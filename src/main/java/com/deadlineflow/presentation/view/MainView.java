package com.deadlineflow.presentation.view;

import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import com.deadlineflow.presentation.components.GanttChartView;
import com.deadlineflow.presentation.theme.StatusColorManager;
import com.deadlineflow.presentation.theme.ThemeManager;
import com.deadlineflow.presentation.view.sections.ProjectsSidebarView;
import com.deadlineflow.presentation.view.sections.TaskInspectorView;
import com.deadlineflow.presentation.view.sections.TopBarView;
import com.deadlineflow.presentation.viewmodel.LanguageManager;
import com.deadlineflow.presentation.viewmodel.MainViewModel;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MainView extends BorderPane {
    private static final boolean NEW_TASK_DIALOG_DEBUG = Boolean.getBoolean("deadlineflow.debug.newtaskdialog");
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final Duration PROJECTS_SIDEBAR_ANIMATION_DURATION = Duration.millis(200);
    private static final Duration TASK_INSPECTOR_SIDEBAR_ANIMATION_DURATION = Duration.millis(200);
    private static final DateTimeFormatter UI_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final MainViewModel viewModel;
    private final LanguageManager i18n;
    private final ThemeManager themeManager;
    private final StatusColorManager statusColorManager = new StatusColorManager();

    private final TopBarView topBarView = new TopBarView();
    private final ProjectsSidebarView projectsSidebarView = new ProjectsSidebarView();
    private final StackPane projectsSidebarHost = new StackPane(projectsSidebarView);
    private final Rectangle projectsSidebarClip = new Rectangle();
    private final TaskInspectorView taskInspectorView = new TaskInspectorView();
    private final StackPane taskInspectorHost = new StackPane(taskInspectorView);
    private final Rectangle taskInspectorClip = new Rectangle();
    private final GanttChartView ganttChartView = new GanttChartView();
    private final Map<Long, Integer> projectTaskCounts = new HashMap<>();

    private final VBox centerColumn = new VBox();
    private final HBox workspaceRow = new HBox();

    private boolean inspectorUpdating;
    private boolean timelineCreateDialogOpen;
    private boolean ganttDerivedRefreshQueued;
    private boolean projectsSidebarVisible;
    private Timeline projectsSidebarTimeline;
    private boolean taskInspectorVisible;
    private Timeline taskInspectorTimeline;
    private boolean horizontalZoomSyncing;
    private String inspectorTitleText = "";

    public MainView(MainViewModel viewModel, LanguageManager i18n, ThemeManager themeManager) {
        this.viewModel = viewModel;
        this.i18n = i18n;
        this.themeManager = themeManager;

        getStyleClass().add("main-root");
        setPadding(new Insets(14));

        centerColumn.getStyleClass().add("board-column");
        centerColumn.setSpacing(0);
        Node timelineCard = buildTimelineCard();
        centerColumn.getChildren().add(timelineCard);
        VBox.setVgrow(timelineCard, Priority.ALWAYS);

        workspaceRow.getStyleClass().add("workspace-row");
        workspaceRow.setSpacing(0);
        workspaceRow.getChildren().addAll(projectsSidebarHost, centerColumn, taskInspectorHost);
        HBox.setHgrow(centerColumn, Priority.ALWAYS);

        setTop(topBarView);
        setCenter(workspaceRow);

        BorderPane.setMargin(topBarView, new Insets(0, 0, 12, 0));

        configureTopBar();
        configureProjectSidebar();
        configureProjectsDrawer();
        configureTaskInspectorDrawer();
        configureInspector();
        configureThemeBridge();

        wireProjectSelection();
        wireTaskInspector();
        wireGantt();
        wireDerivedState();
        applyTranslations();
        updateProjectActionState(viewModel.selectedProjectProperty().get());
        refreshInspector(viewModel.selectedTaskProperty().get());
    }

    private Node buildTimelineCard() {
        StackPane timelineSurface = new StackPane(ganttChartView);
        VBox.setVgrow(timelineSurface, Priority.ALWAYS);

        VBox timelineCard = new VBox(timelineSurface);
        timelineCard.getStyleClass().addAll("panel-card", "timeline-card");
        timelineCard.setPadding(new Insets(0));
        VBox.setVgrow(timelineSurface, Priority.ALWAYS);
        return timelineCard;
    }

    private void configureTopBar() {
        topBarView.bannerLabel().textProperty().bind(viewModel.bannerMessageProperty());
        topBarView.bannerLabel().visibleProperty().bind(viewModel.bannerMessageProperty().isNotEmpty());
        topBarView.bannerLabel().managedProperty().bind(topBarView.bannerLabel().visibleProperty());

        topBarView.cpmBanner().textProperty().bind(viewModel.cpmMessageProperty());
        topBarView.cpmBanner().visibleProperty().bind(viewModel.cpmMessageProperty().isNotEmpty());
        topBarView.cpmBanner().managedProperty().bind(topBarView.cpmBanner().visibleProperty());

        ToggleGroup scaleGroup = new ToggleGroup();
        topBarView.dayButton().setToggleGroup(scaleGroup);
        topBarView.weekButton().setToggleGroup(scaleGroup);
        topBarView.yearButton().setToggleGroup(scaleGroup);
        topBarView.hourButton().setDisable(true);
        topBarView.hourButton().setFocusTraversable(false);

        scaleGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == topBarView.dayButton()) {
                viewModel.scaleProperty().set(TimeScale.DAY);
            } else if (newValue == topBarView.weekButton()) {
                viewModel.scaleProperty().set(TimeScale.WEEK);
            } else if (newValue == topBarView.yearButton()) {
                viewModel.scaleProperty().set(TimeScale.YEAR);
            }
        });

        viewModel.scaleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == TimeScale.HOUR) {
                scaleGroup.selectToggle(topBarView.weekButton());
                Platform.runLater(() -> viewModel.scaleProperty().set(TimeScale.WEEK));
            } else if (newValue == TimeScale.DAY) {
                scaleGroup.selectToggle(topBarView.dayButton());
            } else if (newValue == TimeScale.WEEK || newValue == TimeScale.MONTH) {
                scaleGroup.selectToggle(topBarView.weekButton());
            } else {
                scaleGroup.selectToggle(topBarView.yearButton());
            }
        });
        scaleGroup.selectToggle(topBarView.weekButton());

        topBarView.zoomField().setOnAction(event -> commitZoomField());
        topBarView.zoomField().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitZoomField();
            }
        });
        viewModel.zoomProperty().addListener((obs, oldValue, newValue) -> {
            if (horizontalZoomSyncing) {
                return;
            }
            horizontalZoomSyncing = true;
            topBarView.zoomField().setText(formatZoomFieldValue(newValue.doubleValue()));
            horizontalZoomSyncing = false;
        });
        String initialZoomValue = formatZoomFieldValue(viewModel.zoomProperty().get());
        horizontalZoomSyncing = true;
        topBarView.zoomField().setText(initialZoomValue);
        horizontalZoomSyncing = false;
        topBarView.projectsToggleButton().setOnAction(event -> toggleProjectsDrawer());
        topBarView.inspectorToggleButton().setOnAction(event -> toggleTaskInspectorDrawer());

        topBarView.addTaskButton().setOnAction(event -> createTaskFromToolbar());

        configureThemeComboBox();
    }

    private void configureThemeComboBox() {
        ComboBox<ThemeManager.ThemeMode> comboBox = topBarView.themeComboBox();
        comboBox.getItems().setAll(ThemeManager.ThemeMode.SYSTEM, ThemeManager.ThemeMode.LIGHT, ThemeManager.ThemeMode.DARK);

        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeManager.ThemeMode object) {
                return themeModeLabel(object);
            }

            @Override
            public ThemeManager.ThemeMode fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ThemeManager.ThemeMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : themeModeLabel(item));
            }
        });

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue != themeManager.themeMode()) {
                themeManager.setThemeMode(newValue);
            }
        });

        themeManager.themeModeProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != comboBox.getValue()) {
                comboBox.setValue(newValue);
            }
        });

        comboBox.setValue(themeManager.themeMode());
    }

    private String themeModeLabel(ThemeManager.ThemeMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case LIGHT -> i18n.t("theme_light");
            case DARK -> i18n.t("theme_dark");
            case SYSTEM -> i18n.t("theme_system");
        };
    }

    private void configureProjectSidebar() {
        rebuildProjectTaskCounts();
        projectsSidebarView.projectListView().setItems(viewModel.projects());
        projectsSidebarView.projectListView().setCellFactory(listView -> new ListCell<>() {
            private final Circle dot = new Circle(5);
            private final Label label = new Label();
            private final Label count = new Label();
            private final Region spacer = new Region();
            private final HBox row = new HBox(8, dot, label, spacer, count);

            {
                dot.getStyleClass().add("project-dot");
                label.getStyleClass().add("project-item-label");
                count.getStyleClass().add("project-count-badge");
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setGraphic(null);
                    setText(null);
                    setCursor(Cursor.DEFAULT);
                    return;
                }
                dot.setFill(safeColor(project.color()));
                label.setText(project.name());
                count.setText(String.valueOf(projectTaskCounts.getOrDefault(project.id(), 0)));
                setCursor(Cursor.HAND);
                setGraphic(row);
                setText(null);
            }
        });
        viewModel.allTasks().addListener((ListChangeListener<? super Task>) change -> {
            rebuildProjectTaskCounts();
            projectsSidebarView.projectListView().refresh();
        });
        viewModel.projects().addListener((ListChangeListener<? super Project>) change -> {
            rebuildProjectTaskCounts();
            projectsSidebarView.projectListView().refresh();
        });

        projectsSidebarView.addProjectButton().setOnAction(event -> {
            Optional<ProjectDialog.Result> result = ProjectDialog.show(getWindow(), null);
            result.ifPresent(projectResult -> {
                try {
                    viewModel.createProject(projectResult.name(), projectResult.colorHex(), projectResult.priority());
                } catch (Exception ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        projectsSidebarView.editProjectButton().setOnAction(event -> {
            Project selected = viewModel.selectedProjectProperty().get();
            if (selected == null) {
                return;
            }
            Optional<ProjectDialog.Result> result = ProjectDialog.show(getWindow(), selected);
            result.ifPresent(projectResult -> {
                try {
                    viewModel.updateSelectedProject(projectResult.name(), projectResult.colorHex(), projectResult.priority());
                } catch (Exception ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        projectsSidebarView.deleteProjectButton().setOnAction(event -> {
            Project selected = viewModel.selectedProjectProperty().get();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    i18n.t("delete_project_confirm").formatted(selected.name()));
            confirm.initOwner(getWindow());
            confirm.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> viewModel.deleteSelectedProject());
        });
    }

    private void configureProjectsDrawer() {
        projectsSidebarHost.getStyleClass().add("projects-sidebar-host");
        projectsSidebarHost.setMinWidth(0);
        projectsSidebarHost.setPrefWidth(0);
        projectsSidebarHost.setMaxWidth(0);
        projectsSidebarHost.setAlignment(Pos.CENTER_LEFT);
        projectsSidebarHost.setClip(projectsSidebarClip);
        projectsSidebarHost.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            projectsSidebarClip.setWidth(newBounds.getWidth());
            projectsSidebarClip.setHeight(newBounds.getHeight());
        });

        double targetWidth = projectsSidebarTargetWidth();
        projectsSidebarView.setTranslateX(-targetWidth);
        projectsSidebarView.setOpacity(0.0);
        projectsSidebarView.setManaged(true);
        projectsSidebarView.setVisible(true);

        projectsSidebarView.prefWidthProperty().addListener((obs, oldValue, newValue) -> {
            if (!projectsSidebarVisible) {
                setSidebarHostWidth(0);
                projectsSidebarView.setTranslateX(-projectsSidebarTargetWidth());
            }
        });
    }

    private void configureTaskInspectorDrawer() {
        taskInspectorHost.getStyleClass().add("task-inspector-host");
        taskInspectorHost.setMinWidth(0);
        taskInspectorHost.setPrefWidth(0);
        taskInspectorHost.setMaxWidth(0);
        taskInspectorHost.setAlignment(Pos.CENTER_RIGHT);
        taskInspectorHost.setClip(taskInspectorClip);
        taskInspectorHost.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            taskInspectorClip.setWidth(newBounds.getWidth());
            taskInspectorClip.setHeight(newBounds.getHeight());
        });

        double targetWidth = taskInspectorTargetWidth();
        taskInspectorView.setTranslateX(targetWidth);
        taskInspectorView.setOpacity(0.0);
        taskInspectorView.setManaged(true);
        taskInspectorView.setVisible(true);

        taskInspectorView.prefWidthProperty().addListener((obs, oldValue, newValue) -> {
            if (!taskInspectorVisible) {
                setTaskInspectorHostWidth(0);
                taskInspectorView.setTranslateX(taskInspectorTargetWidth());
            }
        });
    }

    private void configureInspector() {
        taskInspectorView.statusComboBox().setItems(viewModel.statusOptions());
        taskInspectorView.statusComboBox().setButtonCell(buildStatusCell());
        taskInspectorView.statusComboBox().setCellFactory(listView -> buildStatusCell());

        taskInspectorView.progressField().setPromptText("0-100");

        taskInspectorView.manageStatusesButton().setOnAction(event -> showStatusManagerDialog());

        taskInspectorView.addDependencyButton().setOnAction(event -> addDependency());
        taskInspectorView.removeDependencyButton().setOnAction(event -> {
            Dependency selected = taskInspectorView.dependencyListView().getSelectionModel().getSelectedItem();
            if (selected != null) {
                viewModel.removeDependency(selected.id());
                refreshInspector(viewModel.selectedTaskProperty().get());
            }
        });
        taskInspectorView.deleteTaskButton().setOnAction(event -> viewModel.deleteSelectedTask());

        taskInspectorView.dependencyListView().setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Dependency dependency, boolean empty) {
                super.updateItem(dependency, empty);
                if (empty || dependency == null) {
                    setText(null);
                } else {
                    setText(viewModel.dependencyLabel(dependency));
                }
            }
        });
    }

    private void configureThemeBridge() {
        themeManager.effectiveThemeProperty().addListener((obs, oldValue, newValue) ->
                ganttChartView.setDarkTheme(newValue == ThemeManager.ThemeMode.DARK));
        ganttChartView.setDarkTheme(themeManager.effectiveTheme() == ThemeManager.ThemeMode.DARK);
        ganttChartView.setLocale(i18n.locale());
    }

    private void wireProjectSelection() {
        projectsSidebarView.projectListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.selectProject(newValue);
            }
        });

        viewModel.selectedProjectProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && projectsSidebarView.projectListView().getSelectionModel().getSelectedItem() != newValue) {
                projectsSidebarView.projectListView().getSelectionModel().select(newValue);
            }
            updateProjectActionState(newValue);
            updateInspectorActions(viewModel.selectedTaskProperty().get());
        });

        if (!viewModel.projects().isEmpty()) {
            projectsSidebarView.projectListView().getSelectionModel().select(viewModel.projects().getFirst());
        }
    }

    private void toggleProjectsDrawer() {
        setProjectsSidebarVisible(!projectsSidebarVisible);
    }

    private void toggleTaskInspectorDrawer() {
        setTaskInspectorVisible(!taskInspectorVisible);
    }

    private void setProjectsSidebarVisible(boolean visible) {
        if (projectsSidebarTimeline != null) {
            projectsSidebarTimeline.stop();
        }

        double sidebarTargetWidth = projectsSidebarTargetWidth();
        double targetWidth = visible ? sidebarTargetWidth : 0.0;
        double targetTranslateX = visible ? 0.0 : -sidebarTargetWidth;
        double targetOpacity = visible ? 1.0 : 0.0;

        projectsSidebarTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(projectsSidebarHost.minWidthProperty(), currentSidebarHostWidth()),
                        new KeyValue(projectsSidebarHost.prefWidthProperty(), currentSidebarHostWidth()),
                        new KeyValue(projectsSidebarHost.maxWidthProperty(), currentSidebarHostWidth()),
                        new KeyValue(projectsSidebarView.translateXProperty(), projectsSidebarView.getTranslateX()),
                        new KeyValue(projectsSidebarView.opacityProperty(), projectsSidebarView.getOpacity())
                ),
                new KeyFrame(PROJECTS_SIDEBAR_ANIMATION_DURATION,
                        new KeyValue(projectsSidebarHost.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(projectsSidebarHost.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(projectsSidebarHost.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(projectsSidebarView.translateXProperty(), targetTranslateX, Interpolator.EASE_BOTH),
                        new KeyValue(projectsSidebarView.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)
                )
        );

        projectsSidebarTimeline.setOnFinished(event -> {
            projectsSidebarVisible = visible;
            if (!visible) {
                setSidebarHostWidth(0);
                projectsSidebarView.setTranslateX(-projectsSidebarTargetWidth());
                projectsSidebarView.setOpacity(0.0);
            } else {
                setSidebarHostWidth(sidebarTargetWidth);
                projectsSidebarView.setTranslateX(0.0);
                projectsSidebarView.setOpacity(1.0);
            }
            updateProjectsToggleButtonStyle(visible);
        });

        projectsSidebarVisible = visible;
        updateProjectsToggleButtonStyle(visible);
        projectsSidebarTimeline.playFromStart();
    }

    private void updateProjectsToggleButtonStyle(boolean active) {
        if (active) {
            if (!topBarView.projectsToggleButton().getStyleClass().contains("projects-toggle-active")) {
                topBarView.projectsToggleButton().getStyleClass().add("projects-toggle-active");
            }
        } else {
            topBarView.projectsToggleButton().getStyleClass().remove("projects-toggle-active");
        }
    }

    private void setTaskInspectorVisible(boolean visible) {
        if (taskInspectorTimeline != null) {
            taskInspectorTimeline.stop();
        }

        double inspectorTargetWidth = taskInspectorTargetWidth();
        double targetWidth = visible ? inspectorTargetWidth : 0.0;
        double targetTranslateX = visible ? 0.0 : inspectorTargetWidth;
        double targetOpacity = visible ? 1.0 : 0.0;

        taskInspectorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(taskInspectorHost.minWidthProperty(), currentTaskInspectorHostWidth()),
                        new KeyValue(taskInspectorHost.prefWidthProperty(), currentTaskInspectorHostWidth()),
                        new KeyValue(taskInspectorHost.maxWidthProperty(), currentTaskInspectorHostWidth()),
                        new KeyValue(taskInspectorView.translateXProperty(), taskInspectorView.getTranslateX()),
                        new KeyValue(taskInspectorView.opacityProperty(), taskInspectorView.getOpacity())
                ),
                new KeyFrame(TASK_INSPECTOR_SIDEBAR_ANIMATION_DURATION,
                        new KeyValue(taskInspectorHost.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(taskInspectorHost.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(taskInspectorHost.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(taskInspectorView.translateXProperty(), targetTranslateX, Interpolator.EASE_BOTH),
                        new KeyValue(taskInspectorView.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)
                )
        );

        taskInspectorTimeline.setOnFinished(event -> {
            taskInspectorVisible = visible;
            if (!visible) {
                setTaskInspectorHostWidth(0);
                taskInspectorView.setTranslateX(taskInspectorTargetWidth());
                taskInspectorView.setOpacity(0.0);
            } else {
                setTaskInspectorHostWidth(inspectorTargetWidth);
                taskInspectorView.setTranslateX(0.0);
                taskInspectorView.setOpacity(1.0);
            }
            updateInspectorToggleButtonStyle(visible);
        });

        taskInspectorVisible = visible;
        updateInspectorToggleButtonStyle(visible);
        taskInspectorTimeline.playFromStart();
    }

    private void updateInspectorToggleButtonStyle(boolean active) {
        if (active) {
            if (!topBarView.inspectorToggleButton().getStyleClass().contains("inspector-toggle-active")) {
                topBarView.inspectorToggleButton().getStyleClass().add("inspector-toggle-active");
            }
        } else {
            topBarView.inspectorToggleButton().getStyleClass().remove("inspector-toggle-active");
        }
    }

    private double projectsSidebarTargetWidth() {
        double width = projectsSidebarView.prefWidth(-1);
        if (width <= 0) {
            width = projectsSidebarView.getPrefWidth();
        }
        if (width <= 0) {
            width = 260;
        }
        return width;
    }

    private double currentSidebarHostWidth() {
        double width = projectsSidebarHost.getWidth();
        if (width <= 0) {
            width = projectsSidebarHost.getPrefWidth();
        }
        if (width < 0) {
            width = 0;
        }
        return width;
    }

    private void setSidebarHostWidth(double width) {
        projectsSidebarHost.setMinWidth(width);
        projectsSidebarHost.setPrefWidth(width);
        projectsSidebarHost.setMaxWidth(width);
    }

    private double taskInspectorTargetWidth() {
        double width = taskInspectorView.prefWidth(-1);
        if (width <= 0) {
            width = taskInspectorView.getPrefWidth();
        }
        if (width <= 0) {
            width = 320;
        }
        return width + 12;
    }

    private double currentTaskInspectorHostWidth() {
        double width = taskInspectorHost.getWidth();
        if (width <= 0) {
            width = taskInspectorHost.getPrefWidth();
        }
        if (width < 0) {
            width = 0;
        }
        return width;
    }

    private void setTaskInspectorHostWidth(double width) {
        taskInspectorHost.setMinWidth(width);
        taskInspectorHost.setPrefWidth(width);
        taskInspectorHost.setMaxWidth(width);
    }

    private void wireTaskInspector() {
        taskInspectorView.titleField().setOnAction(event -> commitTitle());
        taskInspectorView.titleField().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitTitle();
            }
        });

        taskInspectorView.startDatePicker().setOnAction(event -> commitDates());
        taskInspectorView.dueDatePicker().setOnAction(event -> commitDates());

        taskInspectorView.progressField().setOnAction(event -> commitProgress());
        taskInspectorView.progressField().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitProgress();
            }
        });

        taskInspectorView.descriptionArea().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitDescriptionForSelectedTask();
            }
        });

        taskInspectorView.statusComboBox().setOnAction(event -> {
            if (!inspectorUpdating) {
                String status = taskInspectorView.statusComboBox().getValue();
                if (status != null) {
                    viewModel.updateSelectedTaskStatus(status);
                }
            }
        });

        viewModel.selectedTaskProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue != null && !inspectorUpdating) {
                commitDescriptionForTask(oldValue.id());
            }
            refreshInspector(newValue);
            if (newValue != null) {
                ganttChartView.setSelectedTaskId(newValue.id());
            } else {
                ganttChartView.setSelectedTaskId(null);
            }
        });

        viewModel.projectDependencies().addListener((ListChangeListener<? super Dependency>) change -> {
            if (viewModel.selectedTaskProperty().get() != null) {
                refreshInspector(viewModel.selectedTaskProperty().get());
            }
        });

        viewModel.statusOptions().addListener((ListChangeListener<? super String>) change -> {
            if (viewModel.selectedTaskProperty().get() != null) {
                refreshInspector(viewModel.selectedTaskProperty().get());
            }
        });

        taskInspectorView.dependencyListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                updateInspectorActions(viewModel.selectedTaskProperty().get()));
        viewModel.projectTasks().addListener((ListChangeListener<? super Task>) change ->
                updateInspectorActions(viewModel.selectedTaskProperty().get()));
    }

    private void wireGantt() {
        ganttChartView.setTasks(viewModel.projectTasks());
        ganttChartView.scaleProperty().bind(viewModel.scaleProperty());
        ganttChartView.zoomProperty().bind(viewModel.zoomProperty());
        ganttChartView.setEditingEnabled(true);
        ganttChartView.setOnTaskSelected(viewModel::selectTask);
        ganttChartView.setOnTaskDateChanged((taskId, startDate, dueDate) -> {
            try {
                viewModel.updateTaskDatesFromGantt(taskId, startDate, dueDate);
            } catch (ValidationException ex) {
                showValidationError(ex.getMessage());
                ganttChartView.refresh();
            }
        });
        ganttChartView.setOnTaskCreateSelected(this::handleTimelineDragCreate);
        ganttChartView.setSelectionTextProvider((startDate, dueDate) ->
                i18n.t("drag_create_tooltip").formatted(formatDateForLanguage(startDate), formatDateForLanguage(dueDate)));
        ganttChartView.setTaskColorProvider(statusColorManager::barColorHex);

        viewModel.projects().addListener((ListChangeListener<? super Project>) change -> ganttChartView.refresh());
    }

    private void wireDerivedState() {
        viewModel.riskByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super com.deadlineflow.domain.model.RiskLevel>)
                change -> queueGanttDerivedRefresh());
        viewModel.conflictMessageByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super String>)
                change -> queueGanttDerivedRefresh());
        viewModel.criticalTaskIdsProperty().addListener((javafx.collections.SetChangeListener<? super String>)
                change -> queueGanttDerivedRefresh());
        viewModel.slackByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super Integer>)
                change -> queueGanttDerivedRefresh());

        viewModel.projectFinishDateProperty().addListener((obs, oldValue, newValue) -> updateProjectFinishDateLabel());

        queueGanttDerivedRefresh();
    }

    private void queueGanttDerivedRefresh() {
        if (ganttDerivedRefreshQueued) {
            return;
        }
        ganttDerivedRefreshQueued = true;
        Platform.runLater(() -> {
            ganttDerivedRefreshQueued = false;
            ganttChartView.setDerivedMetadata(
                    viewModel.riskByTaskIdProperty(),
                    viewModel.conflictMessageByTaskIdProperty(),
                    viewModel.criticalTaskIdsProperty(),
                    viewModel.slackByTaskIdProperty()
            );
        });
    }

    private void applyTranslations() {
        topBarView.scaleLabel().setText(i18n.t("scale"));
        topBarView.hourButton().setText(i18n.t("hour"));
        topBarView.dayButton().setText(i18n.t("day"));
        topBarView.weekButton().setText(i18n.t("week"));
        topBarView.yearButton().setText(i18n.t("year"));
        topBarView.zoomLabel().setText(i18n.t("zoom"));
        topBarView.themeLabel().setText(i18n.t("theme"));
        topBarView.projectsToggleButton().setText(i18n.t("projects"));
        topBarView.inspectorToggleButton().setText(i18n.t("task_inspector"));

        topBarView.addTaskButton().setText(i18n.t("add_task"));
        projectsSidebarView.titleLabel().setText(i18n.t("projects"));
        projectsSidebarView.addProjectButton().setText(i18n.t("add_project"));
        projectsSidebarView.editProjectButton().setText(i18n.t("edit"));
        projectsSidebarView.deleteProjectButton().setText(i18n.t("delete"));

        inspectorTitleText = i18n.t("task_inspector");
        taskInspectorView.inspectorTitleLabel().setText(inspectorTitleText);
        taskInspectorView.titleLabel().setText(i18n.t("title"));
        taskInspectorView.startDateLabel().setText(i18n.t("start_date"));
        taskInspectorView.dueDateLabel().setText(i18n.t("due_date"));
        taskInspectorView.progressLabel().setText(i18n.t("progress"));
        taskInspectorView.descriptionLabel().setText(i18n.t("description"));
        taskInspectorView.statusLabel().setText(i18n.t("status"));
        taskInspectorView.dependenciesLabel().setText(i18n.t("dependencies"));

        taskInspectorView.addDependencyButton().setText(i18n.t("add_dependency"));
        taskInspectorView.removeDependencyButton().setText(i18n.t("remove_dependency"));
        taskInspectorView.deleteTaskButton().setText(i18n.t("delete_task"));
        taskInspectorView.manageStatusesButton().setText(i18n.t("manage"));
        ganttChartView.setEmptyStateText(i18n.t("timeline_empty_title"), i18n.t("timeline_empty_hint"));

        updateProjectFinishDateLabel();
        updateInspectorHeader(viewModel.selectedTaskProperty().get());

        if (viewModel.selectedTaskProperty().get() != null) {
            updateSlackLabel(viewModel.selectedTaskProperty().get());
        } else {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
            taskInspectorView.selectedTaskLabel().setText("-");
            taskInspectorView.selectedTaskLabel().setStyle("");
        }

        ThemeManager.ThemeMode selectedTheme = topBarView.themeComboBox().getValue();
        if (selectedTheme != null) {
            topBarView.themeComboBox().setValue(null);
            topBarView.themeComboBox().setValue(selectedTheme);
        }
    }

    private void createTaskFromToolbar() {
        setTaskInspectorVisible(true);
        if (viewModel.selectedProjectProperty().get() == null) {
            showValidationError(i18n.t("select_project_before_task"));
            return;
        }

        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = startDate.plusDays(1);

        try {
            Task createdTask = viewModel.createTask(i18n.t("new_task_default_title"), startDate, dueDate);
            navigateToTask(createdTask);

            // +Task keeps creation in the inspector: create task first, then focus the title field for immediate editing.
            Platform.runLater(() -> {
                taskInspectorView.titleField().requestFocus();
                taskInspectorView.titleField().selectAll();
            });
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void updateProjectFinishDateLabel() {
        if (viewModel.projectFinishDateProperty().get() == null) {
            topBarView.finishDateLabel().setText(i18n.t("project_finish_date") + ": -");
        } else {
            topBarView.finishDateLabel().setText(i18n.t("project_finish_date") + ": "
                    + DateTimeFormatter.ISO_LOCAL_DATE.format(viewModel.projectFinishDateProperty().get()));
        }
    }

    private void refreshInspector(Task task) {
        inspectorUpdating = true;
        boolean disabled = task == null;
        taskInspectorView.setSelectionActive(!disabled);

        taskInspectorView.titleField().setDisable(disabled);
        taskInspectorView.startDatePicker().setDisable(disabled);
        taskInspectorView.dueDatePicker().setDisable(disabled);
        taskInspectorView.progressField().setDisable(disabled);
        taskInspectorView.descriptionArea().setDisable(disabled);
        taskInspectorView.statusComboBox().setDisable(disabled);

        if (task == null) {
            taskInspectorView.titleField().clear();
            taskInspectorView.startDatePicker().setValue(null);
            taskInspectorView.dueDatePicker().setValue(null);
            taskInspectorView.progressField().setText("0");
            taskInspectorView.descriptionArea().clear();
            taskInspectorView.statusComboBox().setValue(null);
            taskInspectorView.dependencyListView().getItems().clear();
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
            taskInspectorView.selectedTaskLabel().setText("-");
            taskInspectorView.selectedTaskLabel().setStyle("");
        } else {
            taskInspectorView.titleField().setText(task.title());
            taskInspectorView.startDatePicker().setValue(task.startDate());
            taskInspectorView.dueDatePicker().setValue(task.dueDate());
            taskInspectorView.progressField().setText(String.valueOf(task.progress()));
            taskInspectorView.descriptionArea().setText(task.description());

            if (!taskInspectorView.statusComboBox().getItems().contains(task.status())) {
                String fallbackStatus = taskInspectorView.statusComboBox().getItems().isEmpty()
                        ? null
                        : taskInspectorView.statusComboBox().getItems().getFirst();
                if (fallbackStatus != null) {
                    viewModel.updateSelectedTaskStatus(fallbackStatus);
                }
            }
            taskInspectorView.statusComboBox().setValue(task.status());

            taskInspectorView.dependencyListView().getItems().setAll(viewModel.dependenciesForSelectedTask());
            taskInspectorView.dependencyListView().getItems().sort(Comparator.comparing(viewModel::dependencyLabel));
            updateSlackLabel(task);

            StatusColorManager.StatusTone tone = statusColorManager.toneForTask(task);
            taskInspectorView.selectedTaskLabel().setText(tone.label());
            taskInspectorView.selectedTaskLabel().setStyle(statusColorManager.chipStyle(tone));
        }
        updateInspectorHeader(task);
        updateInspectorActions(task);

        inspectorUpdating = false;
    }

    private void updateProjectActionState(Project selectedProject) {
        boolean hasProject = selectedProject != null;
        projectsSidebarView.addProjectButton().setDisable(false);
        projectsSidebarView.editProjectButton().setDisable(!hasProject);
        projectsSidebarView.deleteProjectButton().setDisable(!hasProject);
    }

    private void updateInspectorActions(Task task) {
        boolean hasTask = task != null;
        boolean hasDependencySelection = taskInspectorView.dependencyListView().getSelectionModel().getSelectedItem() != null;
        int projectTaskCount = viewModel.projectTasks().size();

        taskInspectorView.manageStatusesButton().setDisable(false);
        taskInspectorView.dependencyListView().setDisable(!hasTask);
        taskInspectorView.addDependencyButton().setDisable(!hasTask || projectTaskCount <= 1);
        taskInspectorView.removeDependencyButton().setDisable(!hasTask || !hasDependencySelection);
        taskInspectorView.deleteTaskButton().setDisable(!hasTask);
    }

    private void updateSlackLabel(Task task) {
        Integer slackDays = viewModel.slackDaysForTask(task.id());
        if (slackDays == null) {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
        } else {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": " + slackDays + " " + i18n.t("slack_days"));
        }
    }

    private ListCell<String> buildStatusCell() {
        return new ListCell<>() {
            private final Label chip = new Label();

            {
                chip.getStyleClass().add("status-chip");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                StatusColorManager.StatusTone tone = statusColorManager.toneForStatus(status, false);
                chip.setText(statusColorManager.displayStatus(status));
                chip.setStyle(statusColorManager.chipStyle(tone));
                setGraphic(chip);
                setText(null);
            }
        };
    }

    private void rebuildProjectTaskCounts() {
        projectTaskCounts.clear();
        for (Task task : viewModel.allTasks()) {
            projectTaskCounts.merge(task.projectId(), 1, Integer::sum);
        }
    }

    public void dispose() {
        if (projectsSidebarTimeline != null) {
            projectsSidebarTimeline.stop();
        }
        if (taskInspectorTimeline != null) {
            taskInspectorTimeline.stop();
        }
        topBarView.bannerLabel().textProperty().unbind();
        topBarView.bannerLabel().visibleProperty().unbind();
        topBarView.bannerLabel().managedProperty().unbind();
        topBarView.cpmBanner().textProperty().unbind();
        topBarView.cpmBanner().visibleProperty().unbind();
        topBarView.cpmBanner().managedProperty().unbind();
        ganttChartView.scaleProperty().unbind();
        ganttChartView.zoomProperty().unbind();
        ganttChartView.dispose();
    }

    private void navigateToTask(Task task) {
        if (task == null) {
            return;
        }
        viewModel.selectTaskById(task.id());
        ganttChartView.scrollToTask(task);
    }

    private void updateInspectorHeader(Task task) {
        if (task == null) {
            taskInspectorView.inspectorTitleLabel().setText(inspectorTitleText);
            return;
        }
        taskInspectorView.inspectorTitleLabel().setText(inspectorTitleText + " · " + task.title());
    }

    private void commitTitle() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        try {
            viewModel.updateSelectedTaskTitle(taskInspectorView.titleField().getText());
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void commitDates() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        try {
            viewModel.updateSelectedTaskDates(
                    taskInspectorView.startDatePicker().getValue(),
                    taskInspectorView.dueDatePicker().getValue()
            );
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
            refreshInspector(viewModel.selectedTaskProperty().get());
        }
    }

    private void commitProgress() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        Integer progress = parseProgressInput(taskInspectorView.progressField().getText());
        if (progress == null) {
            taskInspectorView.progressField().setText(String.valueOf(viewModel.selectedTaskProperty().get().progress()));
            return;
        }
        viewModel.updateSelectedTaskProgress(progress);
        taskInspectorView.progressField().setText(String.valueOf(progress));
    }

    private void commitZoomField() {
        if (horizontalZoomSyncing) {
            return;
        }
        Double zoomValue = parseZoomInput(topBarView.zoomField().getText());
        if (zoomValue == null) {
            horizontalZoomSyncing = true;
            topBarView.zoomField().setText(formatZoomFieldValue(viewModel.zoomProperty().get()));
            horizontalZoomSyncing = false;
            return;
        }

        viewModel.zoomProperty().set(zoomValue);
        horizontalZoomSyncing = true;
        topBarView.zoomField().setText(formatZoomFieldValue(zoomValue));
        horizontalZoomSyncing = false;
    }

    private void commitDescriptionForSelectedTask() {
        Task selectedTask = viewModel.selectedTaskProperty().get();
        if (inspectorUpdating || selectedTask == null) {
            return;
        }
        commitDescriptionForTask(selectedTask.id());
    }

    private void commitDescriptionForTask(String taskId) {
        if (taskId == null) {
            return;
        }
        try {
            viewModel.updateTaskDescription(taskId, taskInspectorView.descriptionArea().getText());
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void addDependency() {
        Task selectedTask = viewModel.selectedTaskProperty().get();
        if (selectedTask == null) {
            return;
        }

        List<Task> candidates = viewModel.projectTasks().stream()
                .filter(task -> !task.id().equals(selectedTask.id()))
                .sorted(Comparator.comparing(Task::startDate).thenComparing(Task::title))
                .toList();

        if (candidates.isEmpty()) {
            showValidationError("No candidate tasks available for dependency");
            return;
        }

        List<String> labels = candidates.stream()
                .map(task -> task.title() + " (" + i18n.t("due") + " " + DateTimeFormatter.ISO_LOCAL_DATE.format(task.dueDate()) + ")")
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.getFirst(), labels);
        dialog.initOwner(getWindow());
        dialog.setTitle(i18n.t("add_dependency"));
        dialog.setHeaderText(i18n.t("add_dependency"));
        dialog.setContentText(i18n.t("task"));

        dialog.showAndWait().ifPresent(label -> {
            int selectedIndex = labels.indexOf(label);
            if (selectedIndex < 0) {
                return;
            }
            Task fromTask = candidates.get(selectedIndex);
            boolean added = viewModel.addDependency(fromTask.id(), selectedTask.id());
            if (!added) {
                showValidationError(viewModel.bannerMessageProperty().get());
            }
            refreshInspector(viewModel.selectedTaskProperty().get());
        });
    }

    private void handleTimelineDragCreate(LocalDate startDate, LocalDate dueDate) {
        if (timelineCreateDialogOpen) {
            debugNewTaskDialog("ignored re-entrant drag-create request");
            return;
        }

        Task createdTask;
        try {
            createdTask = viewModel.createTask(i18n.t("new_task_default_title"), startDate, dueDate);
            setTaskInspectorVisible(true);
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
            return;
        }

        String createdTaskId = createdTask.id();
        timelineCreateDialogOpen = true;
        // Defer dialog launch until after drag-release event unwinds; avoid nested event loops in drag handlers.
        Platform.runLater(() -> {
            try {
                showNewTaskTitleDialogAsync(createdTask.title(), titleResult ->
                        finalizeTimelineCreatedTask(createdTaskId, titleResult));
            } catch (Exception ex) {
                timelineCreateDialogOpen = false;
                viewModel.deleteTaskById(createdTaskId);
                showValidationError(ex.getMessage());
            }
        });
    }

    private void finalizeTimelineCreatedTask(String taskId, Optional<String> titleResult) {
        try {
            if (titleResult.isEmpty()) {
                viewModel.deleteTaskById(taskId);
                return;
            }

            String title = titleResult.get().trim();
            if (title.isBlank()) {
                viewModel.deleteTaskById(taskId);
                return;
            }

            viewModel.findTask(taskId).ifPresent(task -> {
                viewModel.selectTask(task);
                if (!title.equals(task.title())) {
                    viewModel.updateSelectedTaskTitle(title);
                }
                viewModel.findTask(task.id()).ifPresent(this::navigateToTask);
            });
        } finally {
            timelineCreateDialogOpen = false;
        }
    }

    private void showNewTaskTitleDialogAsync(String initialTitle, Consumer<Optional<String>> completion) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(getWindow());
        dialog.setTitle(i18n.t("new_task_dialog_title"));
        dialog.setHeaderText(i18n.t("new_task_dialog_header"));

        ButtonType okType = new ButtonType(i18n.t("ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(i18n.t("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        TextField dialogTitleField = new TextField(initialTitle);
        dialogTitleField.setPromptText(i18n.t("title"));
        dialog.getDialogPane().setContent(dialogTitleField);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        okButton.setDefaultButton(false);
        AtomicBoolean submitHandled = new AtomicBoolean(false);
        AtomicBoolean completionHandled = new AtomicBoolean(false);

        okButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> dialogTitleField.getText() == null || dialogTitleField.getText().isBlank(),
                dialogTitleField.textProperty()
        ));

        dialogTitleField.setOnAction(event -> {
            debugNewTaskDialog("title field Enter");
            event.consume();
            if (!okButton.isDisable()) {
                okButton.fire();
            }
        });

        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            debugNewTaskDialog("ok action");
            if (!submitHandled.compareAndSet(false, true)) {
                // Prevent duplicate submit when Enter and button action overlap.
                debugNewTaskDialog("duplicate submit ignored");
                event.consume();
            }
        });

        dialog.setOnShown(event -> debugNewTaskDialog("dialog shown"));
        dialog.setOnCloseRequest(event -> debugNewTaskDialog("dialog close request"));
        dialog.setOnHidden(event -> {
            debugNewTaskDialog("dialog hidden");
            if (!completionHandled.compareAndSet(false, true)) {
                return;
            }
            if (completion != null) {
                completion.accept(Optional.ofNullable(dialog.getResult()));
            }
        });
        dialog.setResultConverter(buttonType -> {
            debugNewTaskDialog("result converter: " + buttonType);
            return buttonType == okType ? dialogTitleField.getText() : null;
        });
        dialog.show();
        Platform.runLater(() -> {
            if (dialogTitleField.getScene() != null) {
                dialogTitleField.requestFocus();
                dialogTitleField.selectAll();
            }
        });
    }

    private String formatDateForLanguage(LocalDate date) {
        return UI_DATE_FORMATTER.format(date);
    }

    private String formatZoomFieldValue(double zoom) {
        double clampedZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        return String.valueOf((int) Math.round(clampedZoom * 100));
    }

    private Double parseZoomInput(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().replace("%", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            double percent = Double.parseDouble(normalized);
            double clampedPercent = Math.max(25, Math.min(400, percent));
            return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, clampedPercent / 100.0));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseProgressInput(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().replace("%", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(normalized);
            if (parsed < 0 || parsed > 100) {
                return Math.max(0, Math.min(100, parsed));
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showStatusManagerDialog() {
        // Custom status management UI manipulates the persisted status list used by the status dropdown.
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(getWindow());
        dialog.setTitle(i18n.t("manage_statuses"));
        dialog.getDialogPane().getButtonTypes().add(new ButtonType(i18n.t("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        ListView<String> statusesView = new ListView<>(viewModel.statusOptions());
        statusesView.setPrefHeight(220);

        Button addButton = new Button(i18n.t("add_status"));
        Button renameButton = new Button(i18n.t("rename_status"));
        Button deleteButton = new Button(i18n.t("delete_status"));

        addButton.getStyleClass().add("pill-button");
        renameButton.getStyleClass().add("pill-button");
        deleteButton.getStyleClass().addAll("pill-button", "danger-button");

        addButton.setMaxWidth(Double.MAX_VALUE);
        renameButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);

        addButton.setOnAction(event -> {
            TextInputDialog input = new TextInputDialog();
            input.initOwner(getWindow());
            input.setTitle(i18n.t("add_status"));
            input.setHeaderText(i18n.t("add_status"));
            input.setContentText(i18n.t("new_status"));
            input.showAndWait().ifPresent(name -> {
                try {
                    viewModel.addStatus(name);
                } catch (Exception ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        renameButton.setOnAction(event -> {
            String selected = statusesView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showValidationError(i18n.t("status_required"));
                return;
            }
            if (viewModel.isStatusProtected(selected)) {
                showValidationError(i18n.t("status_protected"));
                return;
            }

            TextInputDialog input = new TextInputDialog(selected);
            input.initOwner(getWindow());
            input.setTitle(i18n.t("rename_status"));
            input.setHeaderText(i18n.t("rename_status"));
            input.setContentText(i18n.t("rename_to"));
            input.showAndWait().ifPresent(newName -> {
                try {
                    viewModel.renameStatus(selected, newName);
                } catch (Exception ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        deleteButton.setOnAction(event -> {
            String selected = statusesView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showValidationError(i18n.t("status_required"));
                return;
            }
            if (viewModel.statusOptions().size() <= 1) {
                showValidationError(i18n.t("status_last"));
                return;
            }
            if (viewModel.isStatusProtected(selected)) {
                showValidationError(i18n.t("status_protected"));
                return;
            }
            try {
                viewModel.deleteStatus(selected);
            } catch (Exception ex) {
                showValidationError(ex.getMessage());
            }
        });

        VBox buttons = new VBox(6, addButton, renameButton, deleteButton);
        buttons.setPrefWidth(140);

        HBox content = new HBox(10, statusesView, buttons);
        HBox.setHgrow(statusesView, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(getWindow());
        alert.setHeaderText(i18n.t("alert_error"));
        alert.showAndWait();
    }

    private Window getWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void debugNewTaskDialog(String message) {
        if (!NEW_TASK_DIALOG_DEBUG) {
            return;
        }
        System.out.println(System.nanoTime()
                + " [NewTaskDialog][" + Thread.currentThread().getName() + "] " + message);
    }

    private Color safeColor(String value) {
        try {
            return Color.web(value);
        } catch (Exception ignored) {
            return Color.web("#2563EB");
        }
    }
}
