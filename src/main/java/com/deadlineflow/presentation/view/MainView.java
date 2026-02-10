package com.deadlineflow.presentation.view;

import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import com.deadlineflow.presentation.components.GanttChartView;
import com.deadlineflow.presentation.theme.StatusColorManager;
import com.deadlineflow.presentation.theme.ThemeManager;
import com.deadlineflow.presentation.view.sections.BoardSummaryView;
import com.deadlineflow.presentation.view.sections.ProjectsSidebarView;
import com.deadlineflow.presentation.view.sections.TaskInspectorView;
import com.deadlineflow.presentation.view.sections.TopBarView;
import com.deadlineflow.presentation.viewmodel.LanguageManager;
import com.deadlineflow.presentation.viewmodel.MainViewModel;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainView extends BorderPane {
    private static final boolean NEW_TASK_DIALOG_DEBUG = Boolean.getBoolean("deadlineflow.debug.newtaskdialog");

    private final MainViewModel viewModel;
    private final LanguageManager i18n;
    private final ThemeManager themeManager;
    private final StatusColorManager statusColorManager = new StatusColorManager();

    private final TopBarView topBarView = new TopBarView();
    private final ProjectsSidebarView projectsSidebarView = new ProjectsSidebarView();
    private final BoardSummaryView boardSummaryView = new BoardSummaryView();
    private final TaskInspectorView taskInspectorView = new TaskInspectorView();
    private final GanttChartView ganttChartView = new GanttChartView();

    private final VBox centerColumn = new VBox();

    private boolean inspectorUpdating;
    private boolean timelineCreateDialogOpen;
    private boolean ganttDerivedRefreshQueued;
    private final boolean restrictedInteractionMode = true;
    private String inspectorTitleText = "";

    public MainView(MainViewModel viewModel, LanguageManager i18n, ThemeManager themeManager) {
        this.viewModel = viewModel;
        this.i18n = i18n;
        this.themeManager = themeManager;

        getStyleClass().add("main-root");
        setPadding(new Insets(14));

        centerColumn.getStyleClass().add("board-column");
        centerColumn.setSpacing(12);
        centerColumn.getChildren().addAll(boardSummaryView, buildTimelineCard());

        setTop(topBarView);
        setLeft(projectsSidebarView);
        setCenter(centerColumn);
        setRight(taskInspectorView);

        BorderPane.setMargin(topBarView, new Insets(0, 0, 12, 0));
        BorderPane.setMargin(projectsSidebarView, new Insets(0, 12, 0, 0));
        BorderPane.setMargin(taskInspectorView, new Insets(0, 0, 0, 12));

        configureTopBar();
        configureProjectSidebar();
        configureSummaryBoard();
        configureInspector();
        configureThemeBridge();

        wireProjectSelection();
        wireTaskInspector();
        wireGantt();
        wireDerivedState();
        wireLocalization();
        applyInteractionLockdown();
        applyTranslations();
    }

    private Node buildTimelineCard() {
        VBox timelineCard = new VBox(ganttChartView);
        timelineCard.getStyleClass().addAll("panel-card", "timeline-card");
        timelineCard.setPadding(new Insets(0));
        VBox.setVgrow(ganttChartView, Priority.ALWAYS);
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
            if (newValue == TimeScale.DAY) {
                scaleGroup.selectToggle(topBarView.dayButton());
            } else if (newValue == TimeScale.WEEK || newValue == TimeScale.MONTH) {
                scaleGroup.selectToggle(topBarView.weekButton());
            } else {
                scaleGroup.selectToggle(topBarView.yearButton());
            }
        });
        scaleGroup.selectToggle(topBarView.weekButton());

        topBarView.zoomSlider().valueProperty().bindBidirectional(viewModel.zoomProperty());

        topBarView.addTaskButton().setOnAction(event -> createTaskFromToolbar());

        topBarView.languageComboBox().getItems().setAll(LanguageManager.Language.CHINESE, LanguageManager.Language.ENGLISH);
        topBarView.languageComboBox().setValue(i18n.language());
        topBarView.languageComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                i18n.setLanguage(newValue);
            }
        });

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
        projectsSidebarView.projectListView().setItems(viewModel.projects());
        projectsSidebarView.projectListView().setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setGraphic(null);
                    setText(null);
                    setCursor(Cursor.DEFAULT);
                    return;
                }
                Circle dot = new Circle(5, safeColor(project.color()));
                dot.getStyleClass().add("project-dot");
                Label label = new Label(project.name());
                label.getStyleClass().add("project-item-label");
                Label count = new Label(String.valueOf(viewModel.taskCountForProject(project.id())));
                count.getStyleClass().add("project-count-badge");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(8, dot, label, spacer, count);
                row.setAlignment(Pos.CENTER_LEFT);
                setCursor(Cursor.HAND);
                setGraphic(row);
                setText(null);
            }
        });
        viewModel.allTasks().addListener((ListChangeListener<? super Task>) change ->
                projectsSidebarView.projectListView().refresh());

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

    private void configureSummaryBoard() {
        configureSummaryList(boardSummaryView.dueTodayListView(), viewModel.dueToday(), false);
        configureSummaryList(boardSummaryView.dueInSevenListView(), viewModel.dueInSevenDays(), false);
        configureSummaryList(boardSummaryView.overdueListView(), viewModel.overdue(), true);
        configureSummaryList(boardSummaryView.blockedListView(), viewModel.blockedByDependencies(), false);
    }

    private void configureSummaryList(ListView<Task> listView, ObservableList<Task> tasks, boolean pillStyle) {
        listView.setItems(tasks);
        listView.setFocusTraversable(false);
        // Display-only cards should not intercept interaction beyond their own area.
        listView.setMouseTransparent(true);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label itemLabel = new Label(task.title() + " (" + i18n.t("due") + " " + task.dueDate() + ")");
                itemLabel.getStyleClass().add("summary-item-label");
                if (pillStyle) {
                    itemLabel.getStyleClass().add("summary-pill");
                }
                setGraphic(itemLabel);
                setText(null);
            }
        });
    }

    private void configureInspector() {
        taskInspectorView.statusComboBox().setItems(viewModel.statusOptions());

        taskInspectorView.progressSlider().setShowTickMarks(true);
        taskInspectorView.progressSlider().setShowTickLabels(true);
        taskInspectorView.progressSlider().setMajorTickUnit(25);

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
        });

        if (!viewModel.projects().isEmpty()) {
            projectsSidebarView.projectListView().getSelectionModel().select(viewModel.projects().getFirst());
        }
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

        taskInspectorView.progressSlider().valueProperty().addListener((obs, oldValue, newValue) ->
                taskInspectorView.progressValueLabel().setText(newValue.intValue() + "%"));

        taskInspectorView.progressSlider().valueChangingProperty().addListener((obs, oldValue, changing) -> {
            if (!changing) {
                commitProgress();
            }
        });
        taskInspectorView.progressSlider().setOnMouseReleased(event -> commitProgress());

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
    }

    private void wireGantt() {
        ganttChartView.setTasks(viewModel.projectTasks());
        ganttChartView.scaleProperty().bind(viewModel.scaleProperty());
        ganttChartView.zoomProperty().bind(viewModel.zoomProperty());
        ganttChartView.setOnTaskSelected(viewModel::selectTask);
        ganttChartView.setSelectionTextProvider((startDate, dueDate) ->
                i18n.t("drag_create_tooltip").formatted(formatDateForLanguage(startDate), formatDateForLanguage(dueDate)));

        ganttChartView.setTaskColorProvider(task -> viewModel.projects().stream()
                .filter(project -> project.id() == task.projectId())
                .map(Project::color)
                .findFirst()
                .orElse("#2563EB"));

        viewModel.projects().addListener((ListChangeListener<? super Project>) change -> ganttChartView.refresh());
    }

    private void applyInteractionLockdown() {
        // This iteration keeps non-essential controls read-only to reduce accidental edits.
        ganttChartView.setEditingEnabled(false);

        projectsSidebarView.addProjectButton().setDisable(true);
        projectsSidebarView.editProjectButton().setDisable(true);
        projectsSidebarView.deleteProjectButton().setDisable(true);

        taskInspectorView.addDependencyButton().setDisable(true);
        taskInspectorView.removeDependencyButton().setDisable(true);
        taskInspectorView.deleteTaskButton().setDisable(true);
        taskInspectorView.manageStatusesButton().setDisable(true);
        taskInspectorView.dependencyListView().setDisable(true);
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
                    new HashMap<>(viewModel.riskByTaskIdProperty()),
                    new HashMap<>(viewModel.conflictMessageByTaskIdProperty()),
                    new java.util.HashSet<>(viewModel.criticalTaskIdsProperty()),
                    new HashMap<>(viewModel.slackByTaskIdProperty())
            );
        });
    }

    private void wireLocalization() {
        i18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            if (topBarView.languageComboBox().getValue() != newValue) {
                topBarView.languageComboBox().setValue(newValue);
            }
            applyTranslations();
        });
    }

    private void applyTranslations() {
        topBarView.scaleLabel().setText(i18n.t("scale"));
        topBarView.dayButton().setText(i18n.t("day"));
        topBarView.weekButton().setText(i18n.t("week"));
        topBarView.yearButton().setText(i18n.t("year"));
        topBarView.zoomLabel().setText(i18n.t("zoom"));
        topBarView.languageLabel().setText(i18n.t("language"));
        topBarView.themeLabel().setText(i18n.t("theme"));

        topBarView.addTaskButton().setText(i18n.t("add_task"));
        projectsSidebarView.titleLabel().setText(i18n.t("projects"));
        projectsSidebarView.addProjectButton().setText(i18n.t("add_project"));
        projectsSidebarView.editProjectButton().setText(i18n.t("edit"));
        projectsSidebarView.deleteProjectButton().setText(i18n.t("delete"));

        boardSummaryView.dueTodayTitleLabel().setText(i18n.t("due_today"));
        boardSummaryView.dueInSevenTitleLabel().setText(i18n.t("due_7_days"));
        boardSummaryView.overdueTitleLabel().setText(i18n.t("overdue"));
        boardSummaryView.blockedTitleLabel().setText(i18n.t("blocked_dependencies"));

        taskInspectorView.inspectorTitleLabel().setText(i18n.t("task_inspector"));
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

        updateProjectFinishDateLabel();

        if (viewModel.selectedTaskProperty().get() != null) {
            updateSlackLabel(viewModel.selectedTaskProperty().get());
        } else {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
        }

        ThemeManager.ThemeMode selectedTheme = topBarView.themeComboBox().getValue();
        if (selectedTheme != null) {
            topBarView.themeComboBox().setValue(null);
            topBarView.themeComboBox().setValue(selectedTheme);
        }
    }

    private void createTaskFromToolbar() {
        if (viewModel.selectedProjectProperty().get() == null) {
            showValidationError(i18n.t("select_project_before_task"));
            return;
        }

        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = startDate.plusDays(1);

        try {
            Task createdTask = viewModel.createTask(i18n.t("new_task_default_title"), startDate, dueDate);
            viewModel.selectTask(createdTask);
            ganttChartView.focusTask(createdTask.id());

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

        taskInspectorView.titleField().setDisable(disabled);
        taskInspectorView.startDatePicker().setDisable(disabled);
        taskInspectorView.dueDatePicker().setDisable(disabled);
        taskInspectorView.progressSlider().setDisable(disabled);
        taskInspectorView.descriptionArea().setDisable(disabled);
        taskInspectorView.statusComboBox().setDisable(disabled);
        taskInspectorView.manageStatusesButton().setDisable(disabled || restrictedInteractionMode);
        taskInspectorView.dependencyListView().setDisable(disabled || restrictedInteractionMode);

        if (task == null) {
            taskInspectorView.titleField().clear();
            taskInspectorView.startDatePicker().setValue(null);
            taskInspectorView.dueDatePicker().setValue(null);
            taskInspectorView.progressSlider().setValue(0);
            taskInspectorView.progressValueLabel().setText("0%");
            taskInspectorView.descriptionArea().clear();
            taskInspectorView.statusComboBox().setValue(null);
            taskInspectorView.dependencyListView().getItems().clear();
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
        } else {
            taskInspectorView.titleField().setText(task.title());
            taskInspectorView.startDatePicker().setValue(task.startDate());
            taskInspectorView.dueDatePicker().setValue(task.dueDate());
            taskInspectorView.progressSlider().setValue(task.progress());
            taskInspectorView.progressValueLabel().setText(task.progress() + "%");
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
        }

        inspectorUpdating = false;
    }

    private void updateSlackLabel(Task task) {
        Integer slackDays = viewModel.slackDaysForTask(task.id());
        if (slackDays == null) {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": -");
        } else {
            taskInspectorView.slackLabel().setText(i18n.t("slack") + ": " + slackDays + " " + i18n.t("slack_days"));
        }
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
        viewModel.updateSelectedTaskProgress((int) Math.round(taskInspectorView.progressSlider().getValue()));
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
        timelineCreateDialogOpen = true;

        Task createdTask;
        try {
            createdTask = viewModel.createTask(i18n.t("new_task_default_title"), startDate, dueDate);
        } catch (ValidationException ex) {
            timelineCreateDialogOpen = false;
            showValidationError(ex.getMessage());
            return;
        }

        try {
            Optional<String> titleResult = showNewTaskTitleDialog(createdTask.title());
            if (titleResult.isEmpty()) {
                // Cancelled creation should not leave a placeholder task behind.
                Platform.runLater(() -> viewModel.deleteTaskById(createdTask.id()));
                return;
            }

            String title = titleResult.get().trim();
            if (title.isBlank()) {
                Platform.runLater(() -> viewModel.deleteTaskById(createdTask.id()));
                return;
            }

            // Keep submit handler quick and apply title updates after modal close.
            Platform.runLater(() -> viewModel.findTask(createdTask.id()).ifPresent(task -> {
                viewModel.selectTask(task);
                if (!title.equals(task.title())) {
                    viewModel.updateSelectedTaskTitle(title);
                }
                viewModel.findTask(task.id()).ifPresent(updatedTask -> {
                    viewModel.selectTask(updatedTask);
                    ganttChartView.focusTask(updatedTask.id());
                });
            }));
        } finally {
            timelineCreateDialogOpen = false;
        }
    }

    private Optional<String> showNewTaskTitleDialog(String initialTitle) {
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
        dialog.setOnHidden(event -> debugNewTaskDialog("dialog hidden"));
        dialog.setResultConverter(buttonType -> {
            debugNewTaskDialog("result converter: " + buttonType);
            return buttonType == okType ? dialogTitleField.getText() : null;
        });

        return dialog.showAndWait();
    }

    private String formatDateForLanguage(LocalDate date) {
        DateTimeFormatter formatter = i18n.language() == LanguageManager.Language.CHINESE
                ? DateTimeFormatter.ofPattern("yyyy年M月d日")
                : DateTimeFormatter.ofPattern("MMM d, yyyy");
        return formatter.format(date);
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
