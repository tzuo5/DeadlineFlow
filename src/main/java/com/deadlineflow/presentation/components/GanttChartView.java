package com.deadlineflow.presentation.components;

import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Scene;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class GanttChartView extends Region {

    @FunctionalInterface
    public interface TaskDateChangeListener {
        void onTaskDateChanged(String taskId, LocalDate startDate, LocalDate dueDate);
    }

    @FunctionalInterface
    public interface TaskCreateSelectionListener {
        void onTaskCreateSelected(LocalDate startDate, LocalDate dueDate);
    }

    @FunctionalInterface
    public interface SelectionTextProvider {
        String format(LocalDate startDate, LocalDate dueDate);
    }

    private static final double HEADER_HEIGHT = 38;
    private static final double ROW_HEIGHT = 38;
    private static final double BAR_VERTICAL_PADDING = 7;
    private static final double HANDLE_WIDTH = 7;
    private static final double MIN_VISIBLE_BAR_WIDTH = 44;
    private static final boolean DRAG_DEBUG = Boolean.getBoolean("deadlineflow.debug.drag");

    private final Canvas headerCanvas = new Canvas();
    private final Canvas gridCanvas = new Canvas();
    private final Pane taskBarsLayer = new Pane();
    private final Pane dragOverlayLayer = new Pane();
    private final StackPane content = new StackPane(gridCanvas, taskBarsLayer, dragOverlayLayer);
    private final ScrollPane scrollPane = new ScrollPane(content);

    private final ObjectProperty<TimeScale> scale = new SimpleObjectProperty<>(TimeScale.WEEK);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    private final ObjectProperty<Consumer<Task>> onTaskSelected = new SimpleObjectProperty<>();
    private final ObjectProperty<TaskDateChangeListener> onTaskDateChanged = new SimpleObjectProperty<>();
    private final ObjectProperty<TaskCreateSelectionListener> onTaskCreateSelected = new SimpleObjectProperty<>();
    private final ObjectProperty<SelectionTextProvider> selectionTextProvider = new SimpleObjectProperty<>();
    private final ObjectProperty<Function<Task, String>> taskColorProvider = new SimpleObjectProperty<>(task -> "#3A7AFE");

    private ObservableList<Task> sourceTasks = FXCollections.observableArrayList();
    private final ListChangeListener<Task> sourceTaskListener = change -> refreshAll();

    private List<Task> orderedTasks = new ArrayList<>();
    private final Map<String, Integer> taskIndexById = new HashMap<>();

    private final Map<String, RiskLevel> riskByTaskId = new HashMap<>();
    private final Map<String, String> conflictMessageByTaskId = new HashMap<>();
    private final Set<String> criticalTaskIds = new HashSet<>();
    private final Map<String, Integer> slackByTaskId = new HashMap<>();

    private LocalDate timelineStart = LocalDate.now().minusDays(7);
    private LocalDate timelineEnd = LocalDate.now().plusDays(30);
    private String selectedTaskId;

    private final Rectangle selectionOverlay = new Rectangle();
    private final Tooltip dragSelectionTooltip = new Tooltip();
    private final DragSelectionState dragSelection = new DragSelectionState();
    private boolean dragOverlayUpdateQueued;
    private EventHandler<MouseEvent> sceneDragCaptureHandler;
    private EventHandler<MouseEvent> sceneReleaseCaptureHandler;
    private boolean barsDirty = true;
    private int lastRenderedFirstIndex = -1;
    private int lastRenderedLastIndex = -1;
    private boolean editingEnabled = true;
    private boolean darkTheme;

    public GanttChartView() {
        getStyleClass().add("gantt-chart");

        headerCanvas.getStyleClass().add("gantt-header");
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("gantt-scroll");

        taskBarsLayer.setPickOnBounds(false);
        dragOverlayLayer.setPickOnBounds(false);
        dragOverlayLayer.setMouseTransparent(true);
        dragOverlayLayer.getChildren().add(selectionOverlay);

        selectionOverlay.setVisible(false);
        selectionOverlay.setManaged(false);
        selectionOverlay.setMouseTransparent(true);
        updateSelectionOverlayPaint();
        selectionOverlay.getStrokeDashArray().setAll(6.0, 4.0);
        selectionOverlay.setArcWidth(6);
        selectionOverlay.setArcHeight(6);

        gridCanvas.setCursor(Cursor.CROSSHAIR);
        gridCanvas.setOnMousePressed(this::onGridMousePressed);
        gridCanvas.setOnMouseDragged(this::onGridMouseDragged);
        gridCanvas.setOnMouseReleased(this::onGridMouseReleased);
        gridCanvas.setOnMouseExited(event -> {
            if (!dragSelection.active) {
                hideDragSelectionTooltip();
            }
        });

        getChildren().addAll(headerCanvas, scrollPane);

        widthProperty().addListener((obs, oldValue, newValue) -> refreshAll());
        heightProperty().addListener((obs, oldValue, newValue) -> refreshAll());
        scale.addListener((obs, oldValue, newValue) -> refreshAll());
        zoom.addListener((obs, oldValue, newValue) -> refreshAll());

        scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> drawHeader());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> drawVisibleBars());
        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> refreshAll());

        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        setTasks(sourceTasks);
    }

    public void setTasks(ObservableList<Task> tasks) {
        if (sourceTasks != null) {
            sourceTasks.removeListener(sourceTaskListener);
        }
        sourceTasks = tasks == null ? FXCollections.observableArrayList() : tasks;
        sourceTasks.addListener(sourceTaskListener);
        refreshAll();
    }

    public void setScale(TimeScale scale) {
        this.scale.set(scale);
    }

    public TimeScale getScale() {
        return scale.get();
    }

    public ObjectProperty<TimeScale> scaleProperty() {
        return scale;
    }

    public void setZoom(double zoom) {
        this.zoom.set(zoom);
    }

    public double getZoom() {
        return zoom.get();
    }

    public DoubleProperty zoomProperty() {
        return zoom;
    }

    public void setDarkTheme(boolean enabled) {
        if (darkTheme == enabled) {
            return;
        }
        darkTheme = enabled;
        updateSelectionOverlayPaint();
        barsDirty = true;
        refreshAll();
    }

    public void setOnTaskSelected(Consumer<Task> callback) {
        onTaskSelected.set(callback);
    }

    public void setOnTaskDateChanged(TaskDateChangeListener callback) {
        onTaskDateChanged.set(callback);
    }

    public void setOnTaskCreateSelected(TaskCreateSelectionListener callback) {
        onTaskCreateSelected.set(callback);
    }

    public void setEditingEnabled(boolean enabled) {
        if (editingEnabled == enabled) {
            return;
        }
        editingEnabled = enabled;
        gridCanvas.setCursor(editingEnabled ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        if (!editingEnabled) {
            clearSelectionState();
            hideDragSelectionTooltip();
        }
    }

    public void setSelectionTextProvider(SelectionTextProvider provider) {
        selectionTextProvider.set(provider);
    }

    public void setTaskColorProvider(Function<Task, String> provider) {
        taskColorProvider.set(provider == null ? task -> "#3A7AFE" : provider);
        barsDirty = true;
        drawVisibleBars();
    }

    public void setRiskByTaskId(Map<String, RiskLevel> riskMap) {
        riskByTaskId.clear();
        if (riskMap != null) {
            riskByTaskId.putAll(riskMap);
        }
        barsDirty = true;
        drawVisibleBars();
    }

    public void setConflictMessageByTaskId(Map<String, String> conflictMap) {
        conflictMessageByTaskId.clear();
        if (conflictMap != null) {
            conflictMessageByTaskId.putAll(conflictMap);
        }
        barsDirty = true;
        drawVisibleBars();
    }

    public void setCriticalTaskIds(Set<String> criticalTasks) {
        criticalTaskIds.clear();
        if (criticalTasks != null) {
            criticalTaskIds.addAll(criticalTasks);
        }
        barsDirty = true;
        drawVisibleBars();
    }

    public void setSlackByTaskId(Map<String, Integer> slackMap) {
        slackByTaskId.clear();
        if (slackMap != null) {
            slackByTaskId.putAll(slackMap);
        }
        barsDirty = true;
        drawVisibleBars();
    }

    public void setDerivedMetadata(
            Map<String, RiskLevel> riskMap,
            Map<String, String> conflictMap,
            Set<String> criticalTasks,
            Map<String, Integer> slackMap
    ) {
        // Apply all derived decorations in one batch to avoid redraw storms from per-map listeners.
        riskByTaskId.clear();
        if (riskMap != null) {
            riskByTaskId.putAll(riskMap);
        }

        conflictMessageByTaskId.clear();
        if (conflictMap != null) {
            conflictMessageByTaskId.putAll(conflictMap);
        }

        criticalTaskIds.clear();
        if (criticalTasks != null) {
            criticalTaskIds.addAll(criticalTasks);
        }

        slackByTaskId.clear();
        if (slackMap != null) {
            slackByTaskId.putAll(slackMap);
        }

        barsDirty = true;
        drawVisibleBars();
    }

    public void setSelectedTaskId(String taskId) {
        selectedTaskId = taskId;
        barsDirty = true;
        drawVisibleBars();
    }

    public void refresh() {
        refreshAll();
    }

    public void focusTask(String taskId) {
        Integer index = taskIndexById.get(taskId);
        if (index == null) {
            return;
        }
        selectedTaskId = taskId;
        barsDirty = true;

        Bounds viewport = scrollPane.getViewportBounds();
        double viewportHeight = viewport.getHeight();
        double viewportWidth = viewport.getWidth();
        double contentHeight = content.getHeight();
        double contentWidth = content.getWidth();

        double targetY = Math.max(0, index * ROW_HEIGHT - viewportHeight / 2 + ROW_HEIGHT / 2);
        double maxY = Math.max(0, contentHeight - viewportHeight);
        double vValue = maxY == 0 ? 0 : Math.min(1, targetY / maxY);

        Task task = orderedTasks.get(index);
        double targetX = xForDate(task.startDate()) - viewportWidth / 3;
        double maxX = Math.max(0, contentWidth - viewportWidth);
        double hValue = maxX == 0 ? 0 : Math.min(1, Math.max(0, targetX) / maxX);

        scrollPane.setVvalue(vValue);
        scrollPane.setHvalue(hValue);
        drawVisibleBars();
        drawHeader();
    }

    private void refreshAll() {
        rebuildTaskOrder();
        rebuildTimelineBounds();
        relayoutContent();
        drawGrid();
        barsDirty = true;
        lastRenderedFirstIndex = -1;
        lastRenderedLastIndex = -1;
        drawVisibleBars();
        drawHeader();
    }

    private void rebuildTaskOrder() {
        orderedTasks = sourceTasks.stream()
                .sorted(Comparator.comparing(Task::startDate)
                        .thenComparing(Task::dueDate)
                        .thenComparing(Task::title))
                .toList();

        taskIndexById.clear();
        for (int i = 0; i < orderedTasks.size(); i++) {
            taskIndexById.put(orderedTasks.get(i).id(), i);
        }
    }

    private void rebuildTimelineBounds() {
        if (orderedTasks.isEmpty()) {
            timelineStart = LocalDate.now().minusDays(7);
            timelineEnd = LocalDate.now().plusDays(30);
            return;
        }
        LocalDate minStart = orderedTasks.stream().map(Task::startDate).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDue = orderedTasks.stream().map(Task::dueDate).max(LocalDate::compareTo).orElse(LocalDate.now().plusDays(30));
        timelineStart = minStart.minusDays(5);
        timelineEnd = maxDue.plusDays(12);
    }

    private void relayoutContent() {
        double totalDays = ChronoUnit.DAYS.between(timelineStart, timelineEnd) + 1;
        double width = Math.max(getWidth(), totalDays * dayWidth());
        double height = Math.max(scrollPane.getViewportBounds().getHeight(), orderedTasks.size() * ROW_HEIGHT);

        content.setPrefSize(width, height);
        content.setMinSize(width, height);

        gridCanvas.setWidth(width);
        gridCanvas.setHeight(height);
    }

    private void drawGrid() {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.setFill(darkTheme ? Color.web("#111C2E") : Color.web("#FAFBFE"));
        gc.fillRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        gc.setStroke(darkTheme ? Color.web("#273247") : Color.web("#E2E7F1"));
        gc.setLineWidth(1);

        for (int i = 0; i <= orderedTasks.size(); i++) {
            double y = i * ROW_HEIGHT;
            gc.strokeLine(0, y, gridCanvas.getWidth(), y);
        }

        drawVerticalGridLines(gc);
    }

    private void drawVerticalGridLines(GraphicsContext gc) {
        switch (scale.get()) {
            case DAY -> {
                LocalDate cursor = timelineStart;
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(cursor.getDayOfMonth() == 1
                            ? (darkTheme ? Color.web("#3B4A67") : Color.web("#CBD5E6"))
                            : (darkTheme ? Color.web("#253147") : Color.web("#EEF1F7")));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusDays(1);
                }
            }
            case WEEK -> {
                LocalDate cursor = timelineStart.with(DayOfWeek.MONDAY);
                if (cursor.isAfter(timelineStart)) {
                    cursor = cursor.minusWeeks(1);
                }
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#32405A") : Color.web("#D7DFEC"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate cursor = timelineStart.withDayOfMonth(1);
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#32405A") : Color.web("#D7DFEC"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusMonths(1).withDayOfMonth(1);
                }
            }
            case YEAR -> {
                LocalDate cursor = timelineStart.withDayOfYear(1);
                if (cursor.isAfter(timelineStart)) {
                    cursor = cursor.minusYears(1);
                }
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#3B4A67") : Color.web("#C9D4E8"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusYears(1).withDayOfYear(1);
                }
            }
        }
    }

    private void drawHeader() {
        double width = getWidth();
        double height = HEADER_HEIGHT;
        headerCanvas.setWidth(width);
        headerCanvas.setHeight(height);

        GraphicsContext gc = headerCanvas.getGraphicsContext2D();
        gc.setFill(darkTheme ? Color.web("#0F172A") : Color.web("#F3F6FD"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(darkTheme ? Color.web("#2C3A56") : Color.web("#D7DFEC"));
        gc.strokeLine(0, height - 1, width, height - 1);

        double xOffset = horizontalOffset();
        gc.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 11));
        gc.setFill(darkTheme ? Color.web("#E5E7EB") : Color.web("#425071"));

        switch (scale.get()) {
            case DAY -> drawDayHeader(gc, width, xOffset);
            case WEEK -> drawWeekHeader(gc, width, xOffset);
            case MONTH -> drawMonthHeader(gc, width, xOffset);
            case YEAR -> drawYearHeader(gc, width, xOffset);
        }
    }

    private void drawDayHeader(GraphicsContext gc, double width, double xOffset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        LocalDate cursor = timelineStart;
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -90 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.fillText(formatter.format(cursor), x + 4, 22);
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void drawWeekHeader(GraphicsContext gc, double width, double xOffset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        LocalDate cursor = timelineStart.with(DayOfWeek.MONDAY);
        if (cursor.isAfter(timelineStart)) {
            cursor = cursor.minusWeeks(1);
        }

        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -120 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.fillText("Week " + cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                        + " (" + formatter.format(cursor) + ")", x + 4, 22);
            }
            cursor = cursor.plusWeeks(1);
        }
    }

    private void drawMonthHeader(GraphicsContext gc, double width, double xOffset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        LocalDate cursor = timelineStart.withDayOfMonth(1);
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -120 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.fillText(formatter.format(cursor), x + 4, 22);
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
    }

    private void drawYearHeader(GraphicsContext gc, double width, double xOffset) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        gc.setFont(Font.font("Inter", FontWeight.BOLD, 12));
        LocalDate cursor = timelineStart.withDayOfYear(1);
        if (cursor.isAfter(timelineStart)) {
            cursor = cursor.minusYears(1);
        }
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -100 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.fillText(formatter.format(cursor), x + 6, 24);
            }
            cursor = cursor.plusYears(1).withDayOfYear(1);
        }
    }

    private void drawVisibleBars() {
        debugDrag("drawVisibleBars");
        if (orderedTasks.isEmpty()) {
            if (!taskBarsLayer.getChildren().isEmpty()) {
                taskBarsLayer.getChildren().clear();
            }
            barsDirty = false;
            lastRenderedFirstIndex = -1;
            lastRenderedLastIndex = -1;
            if (dragSelection.active) {
                updateSelectionOverlayBounds();
            }
            return;
        }

        Bounds viewport = scrollPane.getViewportBounds();
        double viewportHeight = Math.max(ROW_HEIGHT, viewport.getHeight());
        double contentHeight = Math.max(viewportHeight, content.getHeight());
        double yOffset = Math.max(0, (contentHeight - viewportHeight) * scrollPane.getVvalue());

        int firstIndex = Math.max(0, (int) Math.floor(yOffset / ROW_HEIGHT) - 1);
        int lastIndex = Math.min(orderedTasks.size() - 1, (int) Math.ceil((yOffset + viewportHeight) / ROW_HEIGHT) + 1);

        boolean visibleRangeChanged = firstIndex != lastRenderedFirstIndex || lastIndex != lastRenderedLastIndex;
        if (!barsDirty && !visibleRangeChanged) {
            if (dragSelection.active) {
                updateSelectionOverlayBounds();
            }
            return;
        }

        // Freeze root cause: rebuilding bar nodes inside repeated layout passes caused layout/repaint storms.
        // Fix: only rebuild task bar nodes when either visible range or task-derived styling actually changes.
        List<Node> nodes = new ArrayList<>(Math.max(0, lastIndex - firstIndex + 1));
        for (int index = firstIndex; index <= lastIndex; index++) {
            nodes.add(new TaskBar(orderedTasks.get(index), index));
        }
        taskBarsLayer.getChildren().setAll(nodes);
        barsDirty = false;
        lastRenderedFirstIndex = firstIndex;
        lastRenderedLastIndex = lastIndex;
        if (dragSelection.active) {
            updateSelectionOverlayBounds();
        }
    }

    private double dayWidth() {
        double clampedZoom = Math.max(0.5, Math.min(zoom.get(), 3.0));
        // Keep date->x mapping linear by days and adjust pixels/day per active scale.
        return switch (scale.get()) {
            case DAY -> 30 * clampedZoom;
            case WEEK -> 14 * clampedZoom;
            case MONTH -> 4.2 * clampedZoom;
            case YEAR -> 0.38 * clampedZoom;
        };
    }

    private double xForDate(LocalDate date) {
        return ChronoUnit.DAYS.between(timelineStart, date) * dayWidth();
    }

    private double widthForTask(Task task) {
        long days = ChronoUnit.DAYS.between(task.startDate(), task.dueDate()) + 1;
        return Math.max(MIN_VISIBLE_BAR_WIDTH, days * dayWidth());
    }

    private double horizontalOffset() {
        Bounds viewport = scrollPane.getViewportBounds();
        double maxOffset = Math.max(0, content.getWidth() - viewport.getWidth());
        return maxOffset * scrollPane.getHvalue();
    }

    private Color safeColor(String value) {
        try {
            return Color.web(value);
        } catch (Exception ignored) {
            return Color.web("#3A7AFE");
        }
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();

        headerCanvas.resizeRelocate(0, 0, width, HEADER_HEIGHT);
        scrollPane.resizeRelocate(0, HEADER_HEIGHT, width, Math.max(0, height - HEADER_HEIGHT));
        relayoutContent();
        drawGrid();
        drawVisibleBars();
        drawHeader();
    }

    @Override
    protected double computePrefWidth(double height) {
        return 1000;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 640;
    }

    private final class TaskBar extends Pane {
        private final Task task;
        private final int rowIndex;

        private final Rectangle base = new Rectangle();
        private final Rectangle accent = new Rectangle();
        private final Rectangle leftHandle = new Rectangle();
        private final Rectangle rightHandle = new Rectangle();
        private final Rectangle criticalOutline = new Rectangle();
        private final javafx.scene.control.Label label = new javafx.scene.control.Label();
        private final javafx.scene.control.Label badge = new javafx.scene.control.Label("!");
        private Tooltip tooltip;

        private LocalDate initialStart;
        private LocalDate initialDue;
        private LocalDate previewStart;
        private LocalDate previewDue;
        private double dragAnchorSceneX;
        private DragMode dragMode = DragMode.NONE;

        private TaskBar(Task task, int rowIndex) {
            this.task = task;
            this.rowIndex = rowIndex;
            this.initialStart = task.startDate();
            this.initialDue = task.dueDate();
            this.previewStart = task.startDate();
            this.previewDue = task.dueDate();

            setManaged(false);

            base.setArcWidth(10);
            base.setArcHeight(10);
            accent.setArcWidth(10);
            accent.setArcHeight(10);
            criticalOutline.setArcWidth(10);
            criticalOutline.setArcHeight(10);
            criticalOutline.setFill(Color.TRANSPARENT);
            criticalOutline.setStroke(Color.web("#7E59D9"));
            criticalOutline.setStrokeWidth(2);

            leftHandle.setWidth(HANDLE_WIDTH);
            rightHandle.setWidth(HANDLE_WIDTH);
            leftHandle.setCursor(Cursor.H_RESIZE);
            rightHandle.setCursor(Cursor.H_RESIZE);
            leftHandle.setFill(Color.color(0, 0, 0, 0.15));
            rightHandle.setFill(Color.color(0, 0, 0, 0.15));

            label.getStyleClass().add("gantt-bar-label");
            badge.getStyleClass().add("gantt-risk-badge");

            getChildren().addAll(criticalOutline, base, accent, leftHandle, rightHandle, label, badge);
            updateVisual(previewStart, previewDue);
            wireEvents();
        }

        private void wireEvents() {
            setOnMouseMoved(event -> {
                if (!editingEnabled) {
                    setCursor(Cursor.HAND);
                    return;
                }
                double localX = event.getX();
                if (localX <= HANDLE_WIDTH + 2 || localX >= getWidth() - HANDLE_WIDTH - 2) {
                    setCursor(Cursor.H_RESIZE);
                } else {
                    setCursor(Cursor.MOVE);
                }
            });

            setOnMousePressed(event -> {
                if (!editingEnabled) {
                    if (onTaskSelected.get() != null) {
                        onTaskSelected.get().accept(task);
                    }
                    event.consume();
                    return;
                }
                dragAnchorSceneX = event.getSceneX();
                initialStart = previewStart;
                initialDue = previewDue;
                dragMode = resolveDragMode(event.getX());
                if (onTaskSelected.get() != null) {
                    onTaskSelected.get().accept(task);
                }
                event.consume();
            });

            setOnMouseDragged(event -> {
                if (!editingEnabled) {
                    return;
                }
                long deltaDays = Math.round((event.getSceneX() - dragAnchorSceneX) / dayWidth());

                LocalDate nextStart = initialStart;
                LocalDate nextDue = initialDue;

                if (dragMode == DragMode.MOVE) {
                    nextStart = initialStart.plusDays(deltaDays);
                    nextDue = initialDue.plusDays(deltaDays);
                } else if (dragMode == DragMode.RESIZE_LEFT) {
                    nextStart = initialStart.plusDays(deltaDays);
                    if (nextStart.isAfter(nextDue)) {
                        nextStart = nextDue;
                    }
                } else if (dragMode == DragMode.RESIZE_RIGHT) {
                    nextDue = initialDue.plusDays(deltaDays);
                    if (nextDue.isBefore(nextStart)) {
                        nextDue = nextStart;
                    }
                }

                previewStart = nextStart;
                previewDue = nextDue;
                updateVisual(previewStart, previewDue);
                event.consume();
            });

            setOnMouseReleased(event -> {
                if (!editingEnabled) {
                    return;
                }
                if (dragMode != DragMode.NONE && onTaskDateChanged.get() != null
                        && (!previewStart.equals(task.startDate()) || !previewDue.equals(task.dueDate()))) {
                    onTaskDateChanged.get().onTaskDateChanged(task.id(), previewStart, previewDue);
                }
                dragMode = DragMode.NONE;
                event.consume();
            });

            setOnMouseClicked(event -> {
                if (onTaskSelected.get() != null) {
                    onTaskSelected.get().accept(task);
                }
                event.consume();
            });
        }

        private DragMode resolveDragMode(double localX) {
            if (localX <= HANDLE_WIDTH + 2) {
                return DragMode.RESIZE_LEFT;
            }
            if (localX >= getWidth() - HANDLE_WIDTH - 2) {
                return DragMode.RESIZE_RIGHT;
            }
            return DragMode.MOVE;
        }

        private void updateVisual(LocalDate startDate, LocalDate dueDate) {
            double x = xForDate(startDate);
            double y = rowIndex * ROW_HEIGHT + BAR_VERTICAL_PADDING;
            double width = Math.max(MIN_VISIBLE_BAR_WIDTH, (ChronoUnit.DAYS.between(startDate, dueDate) + 1) * dayWidth());
            double height = ROW_HEIGHT - (2 * BAR_VERTICAL_PADDING);

            relocate(x, y);
            setPrefSize(width, height);

            base.setWidth(width);
            base.setHeight(height);
            Color projectColor = safeColor(taskColorProvider.get().apply(task));
            Color topColor = darkTheme
                    ? projectColor.deriveColor(0, 1.0, 1.12, 1.0)
                    : projectColor.deriveColor(0, 1.0, 1.02, 1.0);
            Color bottomColor = darkTheme
                    ? projectColor.deriveColor(0, 1.0, 0.78, 1.0)
                    : projectColor.deriveColor(0, 1.0, 0.88, 1.0);
            base.setFill(new LinearGradient(
                    0, 0, 0, 1,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0.0, topColor),
                    new Stop(1.0, bottomColor)
            ));

            RiskLevel risk = riskByTaskId.getOrDefault(task.id(), RiskLevel.NONE);
            Color accentColor = switch (risk) {
                case OVERDUE -> Color.web("#C73636");
                case DUE_SOON -> Color.web("#F0A928");
                default -> Color.TRANSPARENT;
            };
            accent.setWidth(width);
            accent.setHeight(3);
            accent.setFill(accentColor);

            criticalOutline.setVisible(criticalTaskIds.contains(task.id()));
            criticalOutline.setWidth(width);
            criticalOutline.setHeight(height);

            String conflictMessage = conflictMessageByTaskId.get(task.id());
            boolean hasConflict = conflictMessage != null && !conflictMessage.isBlank();

            if (Objects.equals(selectedTaskId, task.id())) {
                base.setStroke(darkTheme ? Color.web("#93C5FD") : Color.web("#0D1A36"));
                base.setStrokeWidth(2);
            } else if (hasConflict) {
                base.setStroke(Color.web("#C73636"));
                base.setStrokeWidth(2);
            } else {
                base.setStroke(darkTheme ? Color.color(1, 1, 1, 0.22) : Color.color(0, 0, 0, 0.16));
                base.setStrokeWidth(1);
            }

            boolean showHandles = editingEnabled;
            leftHandle.setVisible(showHandles);
            rightHandle.setVisible(showHandles);
            leftHandle.setManaged(showHandles);
            rightHandle.setManaged(showHandles);
            leftHandle.setHeight(height);
            rightHandle.setHeight(height);
            leftHandle.setLayoutX(0);
            rightHandle.setLayoutX(width - HANDLE_WIDTH);

            boolean showBadge = risk != RiskLevel.NONE || hasConflict;
            boolean showBadgeVisual = showBadge && width >= 56;
            badge.setVisible(showBadgeVisual);
            if (showBadgeVisual) {
                badge.setLayoutX(Math.max(0, width - 16));
                badge.setLayoutY(2);
            }

            label.setText(compactLabel(task, width, showBadgeVisual));
            label.setVisible(width >= 36);
            label.setLayoutX(8);
            label.setLayoutY(Math.max(4, (height - 14) / 2));
            label.setMaxWidth(Math.max(18, width - (showBadgeVisual ? 26 : 12)));

            StringBuilder tooltipText = new StringBuilder();
            Integer slack = slackByTaskId.get(task.id());
            if (slack != null) {
                tooltipText.append("Slack: ").append(slack).append(" days");
            }
            if (risk == RiskLevel.OVERDUE) {
                appendTooltipLine(tooltipText, "Overdue");
            } else if (risk == RiskLevel.DUE_SOON) {
                appendTooltipLine(tooltipText, "Due in less than 48 hours");
            }
            if (hasConflict) {
                appendTooltipLine(tooltipText, conflictMessage);
            }

            if (tooltipText.length() == 0) {
                if (tooltip != null) {
                    Tooltip.uninstall(this, tooltip);
                    tooltip = null;
                }
            } else {
                if (tooltip == null) {
                    tooltip = new Tooltip();
                    Tooltip.install(this, tooltip);
                }
                tooltip.setText(tooltipText.toString());
            }
        }

        private void appendTooltipLine(StringBuilder builder, String line) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }

        private String compactLabel(Task task, double barWidth, boolean hasBadge) {
            String withProgress = task.title() + " (" + task.progress() + "%)";
            String titleOnly = task.title();
            String preferred = barWidth >= 92 ? withProgress : titleOnly;

            double reserved = hasBadge ? 28 : 14;
            int maxChars = Math.max(4, (int) Math.floor((barWidth - reserved) / 6.5));
            if (preferred.length() <= maxChars) {
                return preferred;
            }
            return preferred.substring(0, Math.max(1, maxChars - 1)) + "...";
        }
    }

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE_LEFT,
        RESIZE_RIGHT
    }

    private void onGridMousePressed(MouseEvent event) {
        if (!editingEnabled) {
            return;
        }
        if (!event.isPrimaryButtonDown()) {
            return;
        }

        requestFocus();
        LocalDate snappedUnitStart = snapDateToScale(mouseXToDate(event.getX()));
        dragSelection.active = true;
        dragSelection.anchorUnitStart = snappedUnitStart;
        dragSelection.currentUnitStart = snappedUnitStart;
        dragSelection.pendingUnitStart = snappedUnitStart;
        dragSelection.cursorScreenX = event.getScreenX();
        dragSelection.cursorScreenY = event.getScreenY();
        debugDrag("mousePressed " + snappedUnitStart);
        installSceneDragCapture();

        // Drag-selection overlay tracks the snapped date range while creating a new task.
        applyPendingDragSelectionUpdate();
        event.consume();
    }

    private void onGridMouseDragged(MouseEvent event) {
        if (!editingEnabled) {
            return;
        }
        if (!dragSelection.active) {
            return;
        }
        dragSelection.pendingUnitStart = snapDateToScale(mouseXToDate(event.getX()));
        dragSelection.cursorScreenX = event.getScreenX();
        dragSelection.cursorScreenY = event.getScreenY();
        debugDrag("mouseDragged " + dragSelection.pendingUnitStart);
        queueDragOverlayUpdate();
        event.consume();
    }

    private void onGridMouseReleased(MouseEvent event) {
        if (!editingEnabled) {
            return;
        }
        if (!dragSelection.active) {
            return;
        }
        finalizeSelectionFromMouse(event.getX(), event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE && dragSelection.active) {
            clearSelectionState();
            hideDragSelectionTooltip();
            debugDrag("cancelled by ESC");
            event.consume();
        }
    }

    private void finalizeSelectionFromMouse(double localX, double screenX, double screenY) {
        dragSelection.pendingUnitStart = snapDateToScale(mouseXToDate(localX));
        dragSelection.cursorScreenX = screenX;
        dragSelection.cursorScreenY = screenY;
        applyPendingDragSelectionUpdate();
        hideDragSelectionTooltip();

        LocalDate start = dragSelection.startDate;
        LocalDate end = dragSelection.endDate;
        debugDrag("mouseReleased " + start + " -> " + end);
        clearSelectionState();

        if (start != null && end != null && onTaskCreateSelected.get() != null) {
            onTaskCreateSelected.get().onTaskCreateSelected(start, end);
        }
    }

    private void queueDragOverlayUpdate() {
        if (dragOverlayUpdateQueued) {
            return;
        }
        dragOverlayUpdateQueued = true;
        // Freeze root cause: mouseDragged rebuilt all visible task nodes every event.
        // Fix: coalesce drag updates on FX pulse and update only lightweight overlay state.
        Platform.runLater(() -> {
            dragOverlayUpdateQueued = false;
            applyPendingDragSelectionUpdate();
        });
    }

    private void installSceneDragCapture() {
        if (dragSelection.captureInstalled) {
            return;
        }
        Scene scene = getScene();
        if (scene == null) {
            return;
        }
        sceneDragCaptureHandler = event -> {
            if (!dragSelection.active || event.getTarget() == gridCanvas) {
                return;
            }
            Point2D local = gridCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
            dragSelection.pendingUnitStart = snapDateToScale(mouseXToDate(local.getX()));
            dragSelection.cursorScreenX = event.getScreenX();
            dragSelection.cursorScreenY = event.getScreenY();
            queueDragOverlayUpdate();
        };
        sceneReleaseCaptureHandler = event -> {
            if (!dragSelection.active || event.getTarget() == gridCanvas) {
                return;
            }
            Point2D local = gridCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
            finalizeSelectionFromMouse(local.getX(), event.getScreenX(), event.getScreenY());
        };
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, sceneDragCaptureHandler);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, sceneReleaseCaptureHandler);
        dragSelection.captureInstalled = true;
    }

    private void removeSceneDragCapture() {
        if (!dragSelection.captureInstalled) {
            return;
        }
        Scene scene = getScene();
        if (scene != null) {
            if (sceneDragCaptureHandler != null) {
                scene.removeEventFilter(MouseEvent.MOUSE_DRAGGED, sceneDragCaptureHandler);
            }
            if (sceneReleaseCaptureHandler != null) {
                scene.removeEventFilter(MouseEvent.MOUSE_RELEASED, sceneReleaseCaptureHandler);
            }
        }
        dragSelection.captureInstalled = false;
        sceneDragCaptureHandler = null;
        sceneReleaseCaptureHandler = null;
    }

    private void applyPendingDragSelectionUpdate() {
        if (!dragSelection.active || dragSelection.pendingUnitStart == null) {
            return;
        }

        LocalDate previousStart = dragSelection.startDate;
        LocalDate previousEnd = dragSelection.endDate;

        dragSelection.currentUnitStart = dragSelection.pendingUnitStart;
        updateSelectionRange();

        if (!Objects.equals(previousStart, dragSelection.startDate)
                || !Objects.equals(previousEnd, dragSelection.endDate)
                || !selectionOverlay.isVisible()) {
            updateSelectionOverlayBounds();
        }
        updateDragSelectionTooltip();
    }

    private void updateSelectionRange() {
        if (dragSelection.anchorUnitStart == null || dragSelection.currentUnitStart == null) {
            return;
        }

        LocalDate minUnitStart = dragSelection.anchorUnitStart.isBefore(dragSelection.currentUnitStart)
                ? dragSelection.anchorUnitStart
                : dragSelection.currentUnitStart;
        LocalDate maxUnitStart = dragSelection.anchorUnitStart.isAfter(dragSelection.currentUnitStart)
                ? dragSelection.anchorUnitStart
                : dragSelection.currentUnitStart;

        LocalDate start = clampDate(minUnitStart);
        LocalDate end = clampDate(unitEndDate(maxUnitStart));
        if (end.isBefore(start)) {
            end = start;
        }
        dragSelection.startDate = start;
        dragSelection.endDate = end;
    }

    private void updateSelectionOverlayBounds() {
        if (dragSelection.startDate == null || dragSelection.endDate == null) {
            selectionOverlay.setVisible(false);
            dragSelection.lastRect = null;
            return;
        }
        double x = Math.max(0, xForDate(dragSelection.startDate));
        double endX = Math.max(x, xForDate(dragSelection.endDate.plusDays(1)));
        double width = Math.max(1, endX - x);

        Bounds newRect = new BoundingBox(x, 0, width, Math.max(ROW_HEIGHT, gridCanvas.getHeight()));
        if (dragSelection.lastRect != null
                && dragSelection.lastRect.getMinX() == newRect.getMinX()
                && dragSelection.lastRect.getWidth() == newRect.getWidth()
                && dragSelection.lastRect.getHeight() == newRect.getHeight()) {
            return;
        }

        dragSelection.lastRect = newRect;
        selectionOverlay.setX(newRect.getMinX());
        selectionOverlay.setY(newRect.getMinY());
        selectionOverlay.setWidth(newRect.getWidth());
        selectionOverlay.setHeight(newRect.getHeight());
        selectionOverlay.setVisible(true);
    }

    private void updateDragSelectionTooltip() {
        if (!dragSelection.active || dragSelection.startDate == null || dragSelection.endDate == null) {
            hideDragSelectionTooltip();
            return;
        }

        String text;
        if (selectionTextProvider.get() != null) {
            text = selectionTextProvider.get().format(dragSelection.startDate, dragSelection.endDate);
        } else {
            text = dragSelection.startDate + " -> " + dragSelection.endDate;
        }
        dragSelectionTooltip.setText(text);
        double anchorX = dragSelection.cursorScreenX + 12;
        double anchorY = dragSelection.cursorScreenY + 12;
        if (!dragSelectionTooltip.isShowing()) {
            dragSelectionTooltip.show(gridCanvas, anchorX, anchorY);
        } else {
            dragSelectionTooltip.setAnchorX(anchorX);
            dragSelectionTooltip.setAnchorY(anchorY);
        }
    }

    private void hideDragSelectionTooltip() {
        if (dragSelectionTooltip.isShowing()) {
            dragSelectionTooltip.hide();
        }
    }

    private void updateSelectionOverlayPaint() {
        selectionOverlay.setFill(darkTheme ? Color.web("#60A5FA", 0.28) : Color.web("#2B6EF2", 0.2));
        selectionOverlay.setStroke(darkTheme ? Color.web("#93C5FD", 0.95) : Color.web("#2B6EF2", 0.9));
    }

    private void clearSelectionState() {
        dragSelection.active = false;
        dragSelection.anchorUnitStart = null;
        dragSelection.currentUnitStart = null;
        dragSelection.pendingUnitStart = null;
        dragSelection.startDate = null;
        dragSelection.endDate = null;
        dragSelection.lastRect = null;
        selectionOverlay.setVisible(false);
        removeSceneDragCapture();
    }

    private LocalDate mouseXToDate(double localX) {
        double clampedX = Math.max(0, Math.min(localX, gridCanvas.getWidth() - 1));
        long dayOffset = (long) Math.floor(clampedX / Math.max(0.0001, dayWidth()));
        return clampDate(timelineStart.plusDays(dayOffset));
    }

    private LocalDate snapDateToScale(LocalDate date) {
        // Snap mouse-selected date to unit boundaries based on current scale.
        return switch (scale.get()) {
            case DAY -> date;
            case WEEK -> date.with(DayOfWeek.MONDAY);
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private LocalDate unitEndDate(LocalDate unitStart) {
        LocalDate end = switch (scale.get()) {
            case DAY -> unitStart;
            case WEEK -> unitStart.plusWeeks(1).minusDays(1);
            case MONTH -> unitStart.plusMonths(1).minusDays(1);
            case YEAR -> unitStart.plusYears(1).minusDays(1);
        };
        return clampDate(end);
    }

    private LocalDate clampDate(LocalDate date) {
        if (date.isBefore(timelineStart)) {
            return timelineStart;
        }
        if (date.isAfter(timelineEnd)) {
            return timelineEnd;
        }
        return date;
    }

    private void debugDrag(String message) {
        if (!DRAG_DEBUG) {
            return;
        }
        System.out.println(System.currentTimeMillis() + " [DragCreate][" + Thread.currentThread().getName() + "] " + message);
    }

    private static final class DragSelectionState {
        private boolean active;
        private boolean captureInstalled;
        private LocalDate anchorUnitStart;
        private LocalDate currentUnitStart;
        private LocalDate pendingUnitStart;
        private LocalDate startDate;
        private LocalDate endDate;
        private double cursorScreenX;
        private double cursorScreenY;
        private Bounds lastRect;
    }
}
