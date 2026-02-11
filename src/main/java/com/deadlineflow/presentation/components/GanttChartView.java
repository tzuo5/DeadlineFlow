package com.deadlineflow.presentation.components;

import com.sun.management.OperatingSystemMXBean;
import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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

    private static final double HEADER_HEIGHT = 34;
    private static final double BASE_ROW_HEIGHT = 30;
    private static final double BASE_BAR_VERTICAL_PADDING = 4;
    private static final double HANDLE_WIDTH = 7;
    private static final double MIN_VISIBLE_BAR_WIDTH = 44;
    private static final double SELECTION_ANIMATION_WINDOW_MILLIS = 220;
    private static final double MIN_HORIZONTAL_ZOOM = 0.25;
    private static final double MAX_HORIZONTAL_ZOOM = 4.0;
    private static final double MIN_ROW_ZOOM = 0.70;
    private static final double MAX_ROW_ZOOM = 1.80;
    private static final double HOURS_PER_DAY = 24.0;
    private static final Duration HORIZONTAL_SCROLLBAR_FADE_DURATION = Duration.millis(150);
    private static final Duration CURRENT_TIME_INDICATOR_REFRESH_INTERVAL = Duration.seconds(15);
    private static final double TODAY_INDICATOR_ANIMATION_THRESHOLD_PX = 0.75;
    private static final int TEXT_WIDTH_CACHE_LIMIT = 1024;
    private static final int COLOR_CACHE_LIMIT = 512;
    private static final int GRADIENT_CACHE_LIMIT = 256;
    private static final int MAX_REUSABLE_TASK_BARS = 160;
    private static final boolean DRAG_DEBUG = Boolean.getBoolean("deadlineflow.debug.drag");
    // Dev-only telemetry is explicitly opt-in: -Ddeadlineflow.perf.telemetry=true
    private static final boolean PERF_TELEMETRY_ENABLED = Boolean.getBoolean("deadlineflow.perf.telemetry");
    private static final Duration PERF_TELEMETRY_INTERVAL = Duration.seconds(10);
    private static final int PERF_MEMORY_WINDOW_SAMPLES = 6;
    private static final double PERF_MEMORY_GROWTH_WARN_MB = 64;
    private static final double PERF_IDLE_CPU_WARN_PERCENT = 15;
    private static final int PERF_IDLE_CPU_WARN_STREAK = 3;
    private static final List<String> INSTALLED_FONT_FAMILIES = List.copyOf(Font.getFamilies());

    private final Canvas headerCanvas = new Canvas();
    private final Canvas gridCanvas = new Canvas();
    private final BackgroundGridLayer backgroundGridLayer = new BackgroundGridLayer(gridCanvas);
    private final TodayIndicatorLayer todayIndicatorLayer = new TodayIndicatorLayer();
    private final DependencyLayer dependencyLayer = new DependencyLayer();
    private final Pane taskBarsLayer = new Pane();
    private final TaskLayer taskLayer = new TaskLayer(taskBarsLayer);
    private final SelectionLayer selectionLayer = new SelectionLayer();
    private final Rectangle interactionSurface = new Rectangle();
    private final StackPane content = new StackPane(backgroundGridLayer, dependencyLayer, selectionLayer, taskLayer, todayIndicatorLayer);
    private final ScrollPane scrollPane = new ScrollPane(content);
    private final VBox emptyState = new VBox();
    private final Label emptyStateIconLabel = new Label("+");
    private final Label emptyStateTitleLabel = new Label("No tasks yet.");
    private final Label emptyStateHintLabel = new Label("Click \"+ Task\" to create your first item.");

    private final ObjectProperty<TimeScale> scale = new SimpleObjectProperty<>(TimeScale.WEEK);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private final DoubleProperty rowZoom = new SimpleDoubleProperty(1.0);

    private final ObjectProperty<Consumer<Task>> onTaskSelected = new SimpleObjectProperty<>();
    private final ObjectProperty<TaskDateChangeListener> onTaskDateChanged = new SimpleObjectProperty<>();
    private final ObjectProperty<TaskCreateSelectionListener> onTaskCreateSelected = new SimpleObjectProperty<>();
    private final ObjectProperty<SelectionTextProvider> selectionTextProvider = new SimpleObjectProperty<>();
    private final ObjectProperty<Function<Task, String>> taskColorProvider = new SimpleObjectProperty<>(task -> "#3A7AFE");

    private ObservableList<Task> sourceTasks = FXCollections.observableArrayList();
    private final ListChangeListener<Task> sourceTaskListener = change -> onSourceTasksChanged();

    private List<Task> orderedTasks = new ArrayList<>();
    private final Map<String, Integer> taskIndexById = new HashMap<>();

    private final Map<String, RiskLevel> riskByTaskId = new HashMap<>();
    private final Map<String, String> conflictMessageByTaskId = new HashMap<>();
    private final Set<String> criticalTaskIds = new HashSet<>();
    private final Map<String, Integer> slackByTaskId = new HashMap<>();
    private final Set<String> knownTaskIds = new HashSet<>();
    private final Set<String> pendingEntryAnimationTaskIds = new HashSet<>();
    private final Map<String, TaskBar> activeTaskBarsByTaskId = new HashMap<>();
    private final Deque<TaskBar> reusableTaskBars = new ArrayDeque<>();
    private final List<Node> visibleBarsScratch = new ArrayList<>();
    private final Set<String> visibleTaskIdsScratch = new HashSet<>();
    private boolean taskTrackingInitialized;
    private boolean taskOrderDirty = true;
    private boolean timelineBoundsDirty = true;

    private LocalDate timelineStart = LocalDate.now().minusDays(7);
    private LocalDate timelineEnd = LocalDate.now().plusDays(30);
    private String selectedTaskId;
    private long selectionChangeAtMillis;

    private final Rectangle selectionOverlay = new Rectangle();
    private final Tooltip dragSelectionTooltip = new Tooltip();
    private final DragSelectionState dragSelection = new DragSelectionState();
    private boolean dragOverlayUpdateQueued;
    private EventHandler<MouseEvent> sceneDragCaptureHandler;
    private EventHandler<MouseEvent> sceneReleaseCaptureHandler;
    private boolean headerDrawQueued;
    private boolean barsDrawQueued;
    private boolean barsDirty = true;
    private int lastRenderedFirstIndex = -1;
    private int lastRenderedLastIndex = -1;
    private double lastLayoutWidth = -1;
    private double lastLayoutHeight = -1;
    private boolean editingEnabled = true;
    private boolean darkTheme;
    private ScrollBar horizontalScrollBar;
    private FadeTransition horizontalScrollbarFade;
    private boolean timelineHovered;
    private Timeline currentTimeIndicatorRefreshTimeline;
    private Timeline performanceTelemetryTimeline;
    private AnimationTimer pulseTelemetryTimer;
    private boolean refreshQueued;
    private boolean disposed;
    private long layoutPassCounter;
    private long refreshPassCounter;
    private double refreshDurationTotalMillis;
    private long pulseLastNanos = -1;
    private long pulseSampleCount;
    private double pulseSampleTotalMillis;
    private final Deque<Double> processMemorySamplesMb = new ArrayDeque<>();
    private int idleHighCpuSampleStreak;
    private final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private Locale uiLocale = Locale.ENGLISH;
    private String headerFontFamily = "Inter";
    private Font cachedHeaderPrimaryFont;
    private Font cachedHeaderSecondaryFont;
    private Font cachedHeaderYearFont;
    private DateTimeFormatter cachedHeaderDateFormatter;
    private DateTimeFormatter cachedHeaderWeekdayFormatter;
    private DateTimeFormatter cachedHeaderMonthFormatter;
    private DateTimeFormatter cachedHeaderYearFormatter;
    private final Text textMeasureProbe = new Text();
    private final LinkedHashMap<String, Double> textWidthCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
            return size() > TEXT_WIDTH_CACHE_LIMIT;
        }
    };
    private final LinkedHashMap<String, Color> colorCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Color> eldest) {
            return size() > COLOR_CACHE_LIMIT;
        }
    };
    private final LinkedHashMap<String, LinearGradient> gradientCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, LinearGradient> eldest) {
            return size() > GRADIENT_CACHE_LIMIT;
        }
    };

    public GanttChartView() {
        getStyleClass().add("gantt-chart");
        refreshHeaderFontFamily();

        headerCanvas.getStyleClass().add("gantt-header");
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("gantt-scroll");

        taskBarsLayer.setPickOnBounds(false);
        interactionSurface.setManaged(false);
        interactionSurface.setFill(Color.TRANSPARENT);
        interactionSurface.setCursor(Cursor.CROSSHAIR);
        selectionLayer.getChildren().addAll(interactionSurface, selectionOverlay);
        selectionLayer.setPickOnBounds(false);

        selectionOverlay.setVisible(false);
        selectionOverlay.setManaged(false);
        selectionOverlay.setMouseTransparent(true);
        updateSelectionOverlayPaint();
        selectionOverlay.getStrokeDashArray().setAll(6.0, 4.0);
        selectionOverlay.setArcWidth(6);
        selectionOverlay.setArcHeight(6);

        interactionSurface.setOnMousePressed(this::onGridMousePressed);
        interactionSurface.setOnMouseDragged(this::onGridMouseDragged);
        interactionSurface.setOnMouseReleased(this::onGridMouseReleased);
        interactionSurface.setOnMouseExited(event -> {
            if (!dragSelection.active) {
                hideDragSelectionTooltip();
            }
        });
        setOnMouseEntered(event -> {
            timelineHovered = true;
            setHorizontalScrollbarVisible(true);
        });
        setOnMouseExited(event -> {
            timelineHovered = false;
            setHorizontalScrollbarVisible(false);
        });

        emptyState.getStyleClass().add("timeline-empty-state");
        emptyState.setManaged(false);
        emptyState.setMouseTransparent(true);
        emptyState.setSpacing(4);
        emptyState.setAlignment(Pos.CENTER);
        emptyStateIconLabel.getStyleClass().add("timeline-empty-icon");
        emptyStateTitleLabel.getStyleClass().add("timeline-empty-title");
        emptyStateHintLabel.getStyleClass().add("timeline-empty-hint");
        emptyState.getChildren().addAll(emptyStateIconLabel, emptyStateTitleLabel, emptyStateHintLabel);

        getChildren().addAll(headerCanvas, scrollPane, emptyState);

        widthProperty().addListener((obs, oldValue, newValue) -> requestRefreshAll());
        heightProperty().addListener((obs, oldValue, newValue) -> requestRefreshAll());
        scale.addListener((obs, oldValue, newValue) -> requestRefreshAll());
        zoom.addListener((obs, oldValue, newValue) -> requestRefreshAll());
        rowZoom.addListener((obs, oldValue, newValue) -> requestRefreshAll());

        scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> requestDrawHeader());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> requestDrawVisibleBars());
        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> {
            resolveHorizontalScrollbar();
            requestRefreshAll();
        });

        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        sourceTasks.addListener(sourceTaskListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopCurrentTimeIndicatorUpdater();
                stopPerformanceTelemetry();
                hideDragSelectionTooltip();
                removeSceneDragCapture();
                horizontalScrollBar = null;
                return;
            }
            startCurrentTimeIndicatorUpdater();
            startPerformanceTelemetry();
            requestRefreshAll();
            Platform.runLater(() -> {
                if (getScene() != null) {
                    resolveHorizontalScrollbar();
                }
            });
        });
        requestRefreshAll();
    }

    public void setTasks(ObservableList<Task> tasks) {
        if (sourceTasks != null) {
            sourceTasks.removeListener(sourceTaskListener);
        }
        sourceTasks = tasks == null ? FXCollections.observableArrayList() : tasks;
        sourceTasks.addListener(sourceTaskListener);
        taskTrackingInitialized = false;
        taskOrderDirty = true;
        timelineBoundsDirty = true;
        knownTaskIds.clear();
        pendingEntryAnimationTaskIds.clear();
        selectedTaskId = null;
        riskByTaskId.clear();
        conflictMessageByTaskId.clear();
        criticalTaskIds.clear();
        slackByTaskId.clear();
        barsDirty = true;
        lastRenderedFirstIndex = -1;
        lastRenderedLastIndex = -1;
        disposeTaskBars();
        requestRefreshAll();
    }

    private void onSourceTasksChanged() {
        taskOrderDirty = true;
        timelineBoundsDirty = true;
        barsDirty = true;
        requestRefreshAll();
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

    public void setRowZoom(double value) {
        rowZoom.set(clampRowZoom(value));
    }

    public double getRowZoom() {
        return rowZoom.get();
    }

    public DoubleProperty rowZoomProperty() {
        return rowZoom;
    }

    public void setLocale(Locale locale) {
        Locale normalized = locale == null ? Locale.ENGLISH : locale;
        if (Objects.equals(uiLocale, normalized)) {
            return;
        }
        uiLocale = normalized;
        refreshHeaderRenderingCache();
        barsDirty = true;
        requestRefreshAll();
    }

    public void setDarkTheme(boolean enabled) {
        if (darkTheme == enabled) {
            return;
        }
        darkTheme = enabled;
        gradientCache.clear();
        updateSelectionOverlayPaint();
        barsDirty = true;
        requestRefreshAll();
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
        interactionSurface.setCursor(editingEnabled ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        interactionSurface.setMouseTransparent(!editingEnabled);
        if (!editingEnabled) {
            clearSelectionState();
            hideDragSelectionTooltip();
        }
    }

    public void setSelectionTextProvider(SelectionTextProvider provider) {
        selectionTextProvider.set(provider);
    }

    public void setEmptyStateText(String title, String hint) {
        if (title != null && !title.isBlank()) {
            emptyStateTitleLabel.setText(title);
        }
        if (hint != null && !hint.isBlank()) {
            emptyStateHintLabel.setText(hint);
        }
    }

    public void setTaskColorProvider(Function<Task, String> provider) {
        taskColorProvider.set(provider == null ? task -> "#3A7AFE" : provider);
        gradientCache.clear();
        barsDirty = true;
        requestDrawVisibleBars();
    }

    public void setRiskByTaskId(Map<String, RiskLevel> riskMap) {
        riskByTaskId.clear();
        if (riskMap != null) {
            riskByTaskId.putAll(riskMap);
        }
        barsDirty = true;
        requestDrawVisibleBars();
    }

    public void setConflictMessageByTaskId(Map<String, String> conflictMap) {
        conflictMessageByTaskId.clear();
        if (conflictMap != null) {
            conflictMessageByTaskId.putAll(conflictMap);
        }
        barsDirty = true;
        requestDrawVisibleBars();
    }

    public void setCriticalTaskIds(Set<String> criticalTasks) {
        criticalTaskIds.clear();
        if (criticalTasks != null) {
            criticalTaskIds.addAll(criticalTasks);
        }
        barsDirty = true;
        requestDrawVisibleBars();
    }

    public void setSlackByTaskId(Map<String, Integer> slackMap) {
        slackByTaskId.clear();
        if (slackMap != null) {
            slackByTaskId.putAll(slackMap);
        }
        barsDirty = true;
        requestDrawVisibleBars();
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
        requestDrawVisibleBars();
    }

    public void setSelectedTaskId(String taskId) {
        if (!Objects.equals(selectedTaskId, taskId)) {
            selectionChangeAtMillis = System.currentTimeMillis();
        }
        selectedTaskId = taskId;
        barsDirty = true;
        requestDrawVisibleBars();
    }

    public void refresh() {
        requestRefreshAll();
    }

    public void focusTask(String taskId) {
        Integer index = taskIndexById.get(taskId);
        if (index == null) {
            return;
        }
        selectedTaskId = taskId;
        selectionChangeAtMillis = System.currentTimeMillis();
        barsDirty = true;

        Bounds viewport = scrollPane.getViewportBounds();
        double viewportHeight = viewport.getHeight();
        double viewportWidth = viewport.getWidth();
        double contentHeight = content.getHeight();
        double contentWidth = content.getWidth();

        double targetY = Math.max(0, index * rowHeight() - viewportHeight / 2 + rowHeight() / 2);
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

    public void scrollToTask(Task task) {
        if (task != null) {
            focusTask(task.id());
        }
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (sourceTasks != null) {
            sourceTasks.removeListener(sourceTaskListener);
        }
        stopCurrentTimeIndicatorUpdater();
        stopPerformanceTelemetry();
        if (horizontalScrollbarFade != null) {
            horizontalScrollbarFade.stop();
            horizontalScrollbarFade = null;
        }
        hideDragSelectionTooltip();
        clearSelectionState();
        disposeTaskBars();
        orderedTasks = List.of();
        taskIndexById.clear();
        knownTaskIds.clear();
        pendingEntryAnimationTaskIds.clear();
        riskByTaskId.clear();
        conflictMessageByTaskId.clear();
        criticalTaskIds.clear();
        slackByTaskId.clear();
        textWidthCache.clear();
        colorCache.clear();
        gradientCache.clear();
        processMemorySamplesMb.clear();
        visibleBarsScratch.clear();
        visibleTaskIdsScratch.clear();
    }

    private void requestRefreshAll() {
        if (disposed || refreshQueued) {
            return;
        }
        refreshQueued = true;
        Platform.runLater(() -> {
            refreshQueued = false;
            if (!disposed) {
                refreshAll();
            }
        });
    }

    private void requestDrawHeader() {
        if (disposed || headerDrawQueued) {
            return;
        }
        headerDrawQueued = true;
        Platform.runLater(() -> {
            headerDrawQueued = false;
            if (!disposed) {
                drawHeader();
            }
        });
    }

    private void requestDrawVisibleBars() {
        if (disposed || barsDrawQueued) {
            return;
        }
        barsDrawQueued = true;
        Platform.runLater(() -> {
            barsDrawQueued = false;
            if (!disposed) {
                drawVisibleBars();
            }
        });
    }

    private TaskBar borrowTaskBar() {
        TaskBar bar = reusableTaskBars.pollFirst();
        if (bar != null) {
            return bar;
        }
        return new TaskBar();
    }

    private void releaseTaskBar(TaskBar bar) {
        if (bar == null) {
            return;
        }
        bar.prepareForReuse();
        if (reusableTaskBars.size() >= MAX_REUSABLE_TASK_BARS) {
            TaskBar evicted = reusableTaskBars.pollLast();
            if (evicted != null) {
                evicted.dispose();
            }
        }
        reusableTaskBars.offerFirst(bar);
    }

    private void releaseActiveTaskBars() {
        if (activeTaskBarsByTaskId.isEmpty()) {
            return;
        }
        for (TaskBar bar : activeTaskBarsByTaskId.values()) {
            releaseTaskBar(bar);
        }
        activeTaskBarsByTaskId.clear();
        taskBarsLayer.getChildren().clear();
    }

    private void disposeTaskBars() {
        if (!taskBarsLayer.getChildren().isEmpty()) {
            taskBarsLayer.getChildren().clear();
        }
        if (!activeTaskBarsByTaskId.isEmpty()) {
            for (TaskBar bar : activeTaskBarsByTaskId.values()) {
                bar.dispose();
            }
            activeTaskBarsByTaskId.clear();
        }
        while (!reusableTaskBars.isEmpty()) {
            TaskBar bar = reusableTaskBars.pollFirst();
            if (bar != null) {
                bar.dispose();
            }
        }
    }

    private void trimReusableTaskBarPool() {
        int targetSize = Math.min(MAX_REUSABLE_TASK_BARS, Math.max(24, orderedTasks.size()));
        while (reusableTaskBars.size() > targetSize) {
            TaskBar evicted = reusableTaskBars.pollLast();
            if (evicted != null) {
                evicted.dispose();
            }
        }
    }

    private void refreshAll() {
        if (disposed) {
            return;
        }
        long refreshStartedAt = PERF_TELEMETRY_ENABLED ? System.nanoTime() : 0;
        if (taskOrderDirty) {
            rebuildTaskOrder();
            taskOrderDirty = false;
            timelineBoundsDirty = true;
        }
        if (timelineBoundsDirty) {
            rebuildTimelineBounds();
            timelineBoundsDirty = false;
        }
        relayoutContent();
        drawGrid();
        updateTodayIndicator();
        updateEmptyStateVisibility();
        barsDirty = true;
        lastRenderedFirstIndex = -1;
        lastRenderedLastIndex = -1;
        drawVisibleBars();
        drawHeader();
        resolveHorizontalScrollbar();
        if (PERF_TELEMETRY_ENABLED) {
            refreshPassCounter++;
            refreshDurationTotalMillis += (System.nanoTime() - refreshStartedAt) / 1_000_000.0;
        }
    }

    private void rebuildTaskOrder() {
        List<Task> sortedTasks = new ArrayList<>(sourceTasks);
        sortedTasks.sort(Comparator.comparing(Task::startDate)
                .thenComparing(Task::dueDate)
                .thenComparing(Task::title));
        orderedTasks = sortedTasks;

        Set<String> currentTaskIds = new HashSet<>(Math.max(16, orderedTasks.size() * 2));
        for (Task task : orderedTasks) {
            currentTaskIds.add(task.id());
        }

        if (!taskTrackingInitialized) {
            knownTaskIds.clear();
            knownTaskIds.addAll(currentTaskIds);
            taskTrackingInitialized = true;
        } else {
            List<String> addedTaskIds = new ArrayList<>();
            for (String taskId : currentTaskIds) {
                if (!knownTaskIds.contains(taskId)) {
                    addedTaskIds.add(taskId);
                }
            }
            if (addedTaskIds.size() <= 2) {
                pendingEntryAnimationTaskIds.addAll(addedTaskIds);
            }
            knownTaskIds.clear();
            knownTaskIds.addAll(currentTaskIds);
        }
        pendingEntryAnimationTaskIds.retainAll(currentTaskIds);
        trimReusableTaskBarPool();

        if (!activeTaskBarsByTaskId.isEmpty()) {
            Iterator<Map.Entry<String, TaskBar>> iterator = activeTaskBarsByTaskId.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TaskBar> entry = iterator.next();
                if (!currentTaskIds.contains(entry.getKey())) {
                    releaseTaskBar(entry.getValue());
                    iterator.remove();
                }
            }
        }

        taskIndexById.clear();
        for (int i = 0; i < orderedTasks.size(); i++) {
            taskIndexById.put(orderedTasks.get(i).id(), i);
        }
    }

    private void rebuildTimelineBounds() {
        LocalDate today = LocalDate.now();
        if (orderedTasks.isEmpty()) {
            timelineStart = today.minusDays(7);
            timelineEnd = today.plusDays(30);
            return;
        }
        LocalDate minStart = null;
        LocalDate maxDue = null;
        for (Task task : orderedTasks) {
            if (minStart == null || task.startDate().isBefore(minStart)) {
                minStart = task.startDate();
            }
            if (maxDue == null || task.dueDate().isAfter(maxDue)) {
                maxDue = task.dueDate();
            }
        }
        if (minStart == null || maxDue == null) {
            timelineStart = today.minusDays(7);
            timelineEnd = today.plusDays(30);
            return;
        }
        timelineStart = minStart.minusDays(5).isBefore(today.minusDays(6)) ? minStart.minusDays(5) : today.minusDays(6);
        timelineEnd = maxDue.plusDays(12).isAfter(today.plusDays(12)) ? maxDue.plusDays(12) : today.plusDays(12);
    }

    private void relayoutContent() {
        double totalDays = ChronoUnit.DAYS.between(timelineStart, timelineEnd) + 1;
        double width = Math.max(getWidth(), totalDays * dayWidth());
        double height = Math.max(scrollPane.getViewportBounds().getHeight(), Math.max(1, orderedTasks.size()) * rowHeight());

        content.setPrefSize(width, height);
        content.setMinSize(width, height);
        backgroundGridLayer.setPrefSize(width, height);
        taskLayer.setPrefSize(width, height);
        dependencyLayer.setPrefSize(width, height);
        todayIndicatorLayer.setPrefSize(width, height);
        selectionLayer.setPrefSize(width, height);

        gridCanvas.setWidth(width);
        gridCanvas.setHeight(height);
        interactionSurface.setWidth(width);
        interactionSurface.setHeight(height);
        updateSelectionOverlayBounds();
    }

    private void drawGrid() {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.setFill(darkTheme ? Color.web("#0E192B") : Color.web("#EEF2F8"));
        gc.fillRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        gc.setStroke(darkTheme ? Color.web("#2B3951") : Color.web("#D3DCEB"));
        gc.setLineWidth(1);

        for (int i = 0; i <= orderedTasks.size(); i++) {
            double y = i * rowHeight();
            gc.strokeLine(0, y, gridCanvas.getWidth(), y);
        }

        drawVerticalGridLines(gc);
    }

    private void drawVerticalGridLines(GraphicsContext gc) {
        switch (scale.get()) {
            case HOUR -> drawHourGridLines(gc);
            case DAY -> {
                LocalDate cursor = timelineStart;
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    boolean monthStart = cursor.getDayOfMonth() == 1;
                    gc.setLineWidth(monthStart ? 1.35 : 1.0);
                    gc.setStroke(monthStart
                            ? (darkTheme ? Color.web("#4A5F82") : Color.web("#BAC9E1"))
                            : (darkTheme ? Color.web("#2A3A53") : Color.web("#DEE6F4")));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusDays(1);
                }
            }
            case WEEK -> {
                LocalDate cursor = timelineStart.with(DayOfWeek.MONDAY);
                if (cursor.isAfter(timelineStart)) {
                    cursor = cursor.minusWeeks(1);
                }
                gc.setLineWidth(1.5);
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#516382") : Color.web("#B3C0D8"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate cursor = timelineStart.withDayOfMonth(1);
                gc.setLineWidth(1.35);
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#50617F") : Color.web("#B7C4DB"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusMonths(1).withDayOfMonth(1);
                }
            }
            case YEAR -> {
                LocalDate cursor = timelineStart.withDayOfYear(1);
                if (cursor.isAfter(timelineStart)) {
                    cursor = cursor.minusYears(1);
                }
                gc.setLineWidth(1.4);
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(darkTheme ? Color.web("#506180") : Color.web("#B3C2DB"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusYears(1).withDayOfYear(1);
                }
            }
        }
    }

    private void drawHourGridLines(GraphicsContext gc) {
        double pixelsPerHour = Math.max(0.0001, dayWidth() / HOURS_PER_DAY);
        double xOffset = horizontalOffset();
        double viewportWidth = Math.max(0, scrollPane.getViewportBounds().getWidth());
        double visibleStartX = Math.max(0, xOffset - (pixelsPerHour * 3));
        double visibleEndX = Math.min(gridCanvas.getWidth(), xOffset + viewportWidth + (pixelsPerHour * 3));

        long totalHours = Math.max(0, ChronoUnit.HOURS.between(timelineStart.atStartOfDay(), timelineEnd.plusDays(1).atStartOfDay()));
        long firstHour = Math.max(0, (long) Math.floor(visibleStartX / pixelsPerHour));
        long lastHour = Math.min(totalHours, (long) Math.ceil(visibleEndX / pixelsPerHour));

        LocalDateTime cursor = timelineStart.atStartOfDay().plusHours(firstHour);
        for (long hourIndex = firstHour; hourIndex <= lastHour; hourIndex++) {
            double x = hourIndex * pixelsPerHour;
            boolean dayBoundary = cursor.getHour() == 0;
            gc.setLineWidth(dayBoundary ? 1.35 : 0.85);
            gc.setStroke(dayBoundary
                    ? (darkTheme ? Color.web("#4A5F82") : Color.web("#B4C3DD"))
                    : (darkTheme ? Color.web("#2A3A53") : Color.web("#DFE6F3")));
            gc.strokeLine(x, 0, x, gridCanvas.getHeight());
            cursor = cursor.plusHours(1);
        }
    }

    private void drawHeader() {
        double width = getWidth();
        double height = HEADER_HEIGHT;
        headerCanvas.setWidth(width);
        headerCanvas.setHeight(height);

        GraphicsContext gc = headerCanvas.getGraphicsContext2D();
        gc.setFill(darkTheme ? Color.web("#101B30") : Color.web("#E7EEF9"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(darkTheme ? Color.web("#3A4B68") : Color.web("#B7C5DE"));
        gc.strokeLine(0, height - 1, width, height - 1);

        double xOffset = horizontalOffset();
        gc.setFont(headerPrimaryFont());
        gc.setFill(headerPrimaryColor());

        switch (scale.get()) {
            case HOUR -> drawHourHeader(gc, width, xOffset);
            case DAY -> drawDayHeader(gc, width, xOffset);
            case WEEK -> drawWeekHeader(gc, width, xOffset);
            case MONTH -> drawMonthHeader(gc, width, xOffset);
            case YEAR -> drawYearHeader(gc, width, xOffset);
        }
    }

    private void drawHourHeader(GraphicsContext gc, double width, double xOffset) {
        double pixelsPerHour = Math.max(0.0001, dayWidth() / HOURS_PER_DAY);
        double textY = 21;
        int labelStepHours = pixelsPerHour >= 44 ? 1
                : pixelsPerHour >= 26 ? 2
                : pixelsPerHour >= 16 ? 3
                : pixelsPerHour >= 11 ? 4
                : pixelsPerHour >= 8 ? 6 : 12;

        long totalHours = Math.max(0, ChronoUnit.HOURS.between(timelineStart.atStartOfDay(), timelineEnd.plusDays(1).atStartOfDay()));
        long firstHour = Math.max(0, (long) Math.floor(Math.max(0, xOffset - (pixelsPerHour * 2)) / pixelsPerHour));
        long lastHour = Math.min(totalHours, (long) Math.ceil((xOffset + width + (pixelsPerHour * 2)) / pixelsPerHour));

        LocalDateTime cursor = timelineStart.atStartOfDay().plusHours(firstHour);
        for (long hourIndex = firstHour; hourIndex <= lastHour; hourIndex++) {
            double x = (hourIndex * pixelsPerHour) - xOffset;
            if (x < -140 || x > width + 24) {
                cursor = cursor.plusHours(1);
                continue;
            }

            gc.strokeLine(x, 0, x, HEADER_HEIGHT);
            if (cursor.getHour() == 0) {
                drawDateWithWeekdayLabel(gc, cursor.toLocalDate(), x + 4, textY);
            } else if (cursor.getHour() % labelStepHours == 0) {
                gc.setFont(headerSecondaryFont());
                gc.setFill(headerSecondaryColor());
                gc.fillText(String.format(uiLocale, "%02d:00", cursor.getHour()), x + 4, textY);
            }
            cursor = cursor.plusHours(1);
        }
    }

    private void drawDayHeader(GraphicsContext gc, double width, double xOffset) {
        double textY = 21;
        LocalDate cursor = timelineStart;
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -90 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                drawDateWithWeekdayLabel(gc, cursor, x + 4, textY);
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void drawWeekHeader(GraphicsContext gc, double width, double xOffset) {
        double textY = 21;
        LocalDate cursor = timelineStart.with(DayOfWeek.MONDAY);
        if (cursor.isAfter(timelineStart)) {
            cursor = cursor.minusWeeks(1);
        }

        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -120 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                String weekPrefix = "W" + cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + " · ";
                gc.setFont(headerPrimaryFont());
                gc.setFill(headerPrimaryColor());
                gc.fillText(weekPrefix, x + 5, textY);

                double prefixWidth = measureTextWidth(weekPrefix, headerPrimaryFont());
                drawDateWithWeekdayLabel(gc, cursor, x + 5 + prefixWidth, textY);
            }
            cursor = cursor.plusWeeks(1);
        }
    }

    private void drawMonthHeader(GraphicsContext gc, double width, double xOffset) {
        LocalDate cursor = timelineStart.withDayOfMonth(1);
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -120 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.setFont(headerPrimaryFont());
                gc.setFill(headerPrimaryColor());
                gc.fillText(cachedHeaderMonthFormatter.format(cursor), x + 4, 21);
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
    }

    private void drawYearHeader(GraphicsContext gc, double width, double xOffset) {
        gc.setFont(headerYearFont());
        gc.setFill(headerPrimaryColor());
        LocalDate cursor = timelineStart.withDayOfYear(1);
        if (cursor.isAfter(timelineStart)) {
            cursor = cursor.minusYears(1);
        }
        while (!cursor.isAfter(timelineEnd)) {
            double x = xForDate(cursor) - xOffset;
            if (x > -100 && x < width + 20) {
                gc.strokeLine(x, 0, x, HEADER_HEIGHT);
                gc.fillText(cachedHeaderYearFormatter.format(cursor), x + 6, 22);
            }
            cursor = cursor.plusYears(1).withDayOfYear(1);
        }
    }

    private void drawDateWithWeekdayLabel(GraphicsContext gc, LocalDate date, double x, double baselineY) {
        String dateLabel = formatHeaderDateLabel(date);
        String weekdayLabel = " · " + formatHeaderWeekdayLabel(date);

        Font primary = headerPrimaryFont();
        Font secondary = headerSecondaryFont();

        gc.setFont(primary);
        gc.setFill(headerPrimaryColor());
        gc.fillText(dateLabel, x, baselineY);

        gc.setFont(secondary);
        gc.setFill(headerSecondaryColor());
        gc.fillText(weekdayLabel, x + measureTextWidth(dateLabel, primary) + 2, baselineY);
    }

    private String formatHeaderDateLabel(LocalDate date) {
        return cachedHeaderDateFormatter.format(date);
    }

    private String formatHeaderWeekdayLabel(LocalDate date) {
        return cachedHeaderWeekdayFormatter.format(date);
    }

    private Font headerPrimaryFont() {
        return cachedHeaderPrimaryFont;
    }

    private Font headerSecondaryFont() {
        return cachedHeaderSecondaryFont;
    }

    private Font headerYearFont() {
        return cachedHeaderYearFont;
    }

    private Color headerPrimaryColor() {
        return darkTheme ? Color.web("#F1F5F9") : Color.web("#2F3E5E");
    }

    private Color headerSecondaryColor() {
        return darkTheme ? Color.web("#A9BAD5") : Color.web("#6A7894");
    }

    private double measureTextWidth(String text, Font font) {
        String key = font.getFamily() + "|" + font.getStyle() + "|" + font.getSize() + "|" + text;
        Double cached = textWidthCache.get(key);
        if (cached != null) {
            return cached;
        }
        textMeasureProbe.setText(text);
        textMeasureProbe.setFont(font);
        double width = textMeasureProbe.getLayoutBounds().getWidth();
        textWidthCache.put(key, width);
        return width;
    }

    private void drawVisibleBars() {
        debugDrag("drawVisibleBars");
        updateEmptyStateVisibility();
        if (orderedTasks.isEmpty()) {
            releaseActiveTaskBars();
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
        double viewportHeight = Math.max(rowHeight(), viewport.getHeight());
        double contentHeight = Math.max(viewportHeight, content.getHeight());
        double yOffset = Math.max(0, (contentHeight - viewportHeight) * scrollPane.getVvalue());

        int firstIndex = Math.max(0, (int) Math.floor(yOffset / rowHeight()) - 1);
        int lastIndex = Math.min(orderedTasks.size() - 1, (int) Math.ceil((yOffset + viewportHeight) / rowHeight()) + 1);

        boolean visibleRangeChanged = firstIndex != lastRenderedFirstIndex || lastIndex != lastRenderedLastIndex;
        if (!barsDirty && !visibleRangeChanged) {
            if (dragSelection.active) {
                updateSelectionOverlayBounds();
            }
            return;
        }

        visibleTaskIdsScratch.clear();
        for (int index = firstIndex; index <= lastIndex; index++) {
            visibleTaskIdsScratch.add(orderedTasks.get(index).id());
        }
        if (!activeTaskBarsByTaskId.isEmpty()) {
            Iterator<Map.Entry<String, TaskBar>> iterator = activeTaskBarsByTaskId.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TaskBar> entry = iterator.next();
                if (!visibleTaskIdsScratch.contains(entry.getKey())) {
                    releaseTaskBar(entry.getValue());
                    iterator.remove();
                }
            }
        }

        visibleBarsScratch.clear();
        for (int index = firstIndex; index <= lastIndex; index++) {
            Task task = orderedTasks.get(index);
            TaskBar bar = activeTaskBarsByTaskId.get(task.id());
            if (bar == null) {
                bar = borrowTaskBar();
                activeTaskBarsByTaskId.put(task.id(), bar);
            }
            bar.bind(task, index);
            visibleBarsScratch.add(bar);
        }
        boolean compositionChanged = visibleRangeChanged
                || taskBarsLayer.getChildren().size() != visibleBarsScratch.size();
        if (!compositionChanged) {
            for (int i = 0; i < visibleBarsScratch.size(); i++) {
                if (taskBarsLayer.getChildren().get(i) != visibleBarsScratch.get(i)) {
                    compositionChanged = true;
                    break;
                }
            }
        }
        if (compositionChanged) {
            taskBarsLayer.getChildren().setAll(visibleBarsScratch);
        }
        barsDirty = false;
        lastRenderedFirstIndex = firstIndex;
        lastRenderedLastIndex = lastIndex;
        if (dragSelection.active) {
            updateSelectionOverlayBounds();
        }
    }

    private double dayWidth() {
        double clampedZoom = Math.max(MIN_HORIZONTAL_ZOOM, Math.min(zoom.get(), MAX_HORIZONTAL_ZOOM));
        // Keep date->x mapping linear by days and adjust pixels/day per active scale.
        return switch (scale.get()) {
            case HOUR -> 384 * clampedZoom;
            case DAY -> 27 * clampedZoom;
            case WEEK -> 12.2 * clampedZoom;
            case MONTH -> 3.8 * clampedZoom;
            case YEAR -> 0.34 * clampedZoom;
        };
    }

    private double rowHeight() {
        return BASE_ROW_HEIGHT * clampRowZoom(rowZoom.get());
    }

    private double barVerticalPadding() {
        return BASE_BAR_VERTICAL_PADDING * Math.max(0.82, clampRowZoom(rowZoom.get()));
    }

    private double clampRowZoom(double value) {
        return Math.max(MIN_ROW_ZOOM, Math.min(MAX_ROW_ZOOM, value));
    }

    private double xForDate(LocalDate date) {
        return ChronoUnit.DAYS.between(timelineStart, date) * dayWidth();
    }

    private double xForDateTime(LocalDateTime dateTime) {
        LocalDateTime timelineStartDateTime = timelineStart.atStartOfDay();
        long wholeDays = ChronoUnit.DAYS.between(timelineStartDateTime.toLocalDate(), dateTime.toLocalDate());
        double fractionalDay = (dateTime.getHour() / HOURS_PER_DAY)
                + (dateTime.getMinute() / (60.0 * HOURS_PER_DAY))
                + (dateTime.getSecond() / (3600.0 * HOURS_PER_DAY))
                + (dateTime.getNano() / (3_600_000_000_000.0 * HOURS_PER_DAY));
        return (wholeDays + fractionalDay) * dayWidth();
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
        String key = value == null ? "" : value.trim();
        Color cached = colorCache.get(key);
        if (cached != null) {
            return cached;
        }
        Color resolved;
        try {
            resolved = Color.web(key);
        } catch (Exception ignored) {
            resolved = Color.web("#3A7AFE");
        }
        colorCache.put(key, resolved);
        return resolved;
    }

    private LinearGradient resolveTaskGradient(Color projectColor) {
        String key = (darkTheme ? "D:" : "L:") + projectColor.toString();
        LinearGradient gradient = gradientCache.get(key);
        if (gradient != null) {
            return gradient;
        }
        Color topColor = darkTheme
                ? projectColor.deriveColor(0, 1.0, 1.12, 1.0)
                : projectColor.deriveColor(0, 1.0, 1.02, 1.0);
        Color bottomColor = darkTheme
                ? projectColor.deriveColor(0, 1.0, 0.78, 1.0)
                : projectColor.deriveColor(0, 1.0, 0.88, 1.0);
        gradient = new LinearGradient(
                0, 0, 0, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, topColor),
                new Stop(1.0, bottomColor)
        );
        gradientCache.put(key, gradient);
        return gradient;
    }

    private void resolveHorizontalScrollbar() {
        Node candidate = scrollPane.lookup(".scroll-bar:horizontal");
        if (!(candidate instanceof ScrollBar scrollBar)) {
            return;
        }
        if (horizontalScrollBar == scrollBar) {
            return;
        }
        horizontalScrollBar = scrollBar;
        horizontalScrollBar.setOpacity(timelineHovered ? 1.0 : 0.0);
        horizontalScrollBar.setMouseTransparent(!timelineHovered);
    }

    private void setHorizontalScrollbarVisible(boolean visible) {
        resolveHorizontalScrollbar();
        if (horizontalScrollBar == null) {
            return;
        }
        if (horizontalScrollbarFade != null) {
            horizontalScrollbarFade.stop();
        }
        horizontalScrollbarFade = new FadeTransition(HORIZONTAL_SCROLLBAR_FADE_DURATION, horizontalScrollBar);
        horizontalScrollbarFade.setToValue(visible ? 1.0 : 0.0);
        horizontalScrollBar.setMouseTransparent(!visible);
        horizontalScrollbarFade.playFromStart();
    }

    private void startCurrentTimeIndicatorUpdater() {
        if (disposed || getScene() == null) {
            return;
        }
        if (currentTimeIndicatorRefreshTimeline != null) {
            currentTimeIndicatorRefreshTimeline.stop();
        }
        currentTimeIndicatorRefreshTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> updateTodayIndicator()),
                new KeyFrame(CURRENT_TIME_INDICATOR_REFRESH_INTERVAL)
        );
        currentTimeIndicatorRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        currentTimeIndicatorRefreshTimeline.play();
    }

    private void stopCurrentTimeIndicatorUpdater() {
        if (currentTimeIndicatorRefreshTimeline != null) {
            currentTimeIndicatorRefreshTimeline.stop();
            currentTimeIndicatorRefreshTimeline = null;
        }
    }

    private void startPerformanceTelemetry() {
        if (!PERF_TELEMETRY_ENABLED || disposed || getScene() == null) {
            return;
        }
        if (pulseTelemetryTimer == null) {
            pulseTelemetryTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (pulseLastNanos > 0 && now > pulseLastNanos) {
                        pulseSampleTotalMillis += (now - pulseLastNanos) / 1_000_000.0;
                        pulseSampleCount++;
                    }
                    pulseLastNanos = now;
                }
            };
        }
        pulseLastNanos = -1;
        pulseTelemetryTimer.start();

        if (performanceTelemetryTimeline == null) {
            performanceTelemetryTimeline = new Timeline(
                    new KeyFrame(PERF_TELEMETRY_INTERVAL, event -> logPerformanceSnapshot())
            );
            performanceTelemetryTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        performanceTelemetryTimeline.playFromStart();
    }

    private void stopPerformanceTelemetry() {
        if (performanceTelemetryTimeline != null) {
            performanceTelemetryTimeline.stop();
            performanceTelemetryTimeline = null;
        }
        if (pulseTelemetryTimer != null) {
            pulseTelemetryTimer.stop();
        }
        pulseLastNanos = -1;
        pulseSampleCount = 0;
        pulseSampleTotalMillis = 0;
        layoutPassCounter = 0;
        refreshPassCounter = 0;
        refreshDurationTotalMillis = 0;
        processMemorySamplesMb.clear();
        idleHighCpuSampleStreak = 0;
    }

    private void logPerformanceSnapshot() {
        if (!PERF_TELEMETRY_ENABLED || disposed) {
            return;
        }

        double averagePulseMillis = pulseSampleCount == 0 ? 0 : pulseSampleTotalMillis / pulseSampleCount;
        pulseSampleCount = 0;
        pulseSampleTotalMillis = 0;

        long layoutPasses = layoutPassCounter;
        layoutPassCounter = 0;
        long refreshPasses = refreshPassCounter;
        double averageRefreshMillis = refreshPasses == 0 ? 0 : refreshDurationTotalMillis / refreshPasses;
        refreshPassCounter = 0;
        refreshDurationTotalMillis = 0;

        MemorySnapshot memory = captureMemorySnapshot();
        double processCpuPercent = readProcessCpuPercent();
        boolean idle = !dragSelection.active
                && !dragOverlayUpdateQueued
                && !refreshQueued
                && !barsDirty
                && !headerDrawQueued
                && !barsDrawQueued;
        trackTelemetryWarnings(memory.processMemoryMb(), processCpuPercent, idle);

        System.out.printf(Locale.ROOT,
                "[Perf][Gantt] pulseAvgMs=%.2f layoutPasses=%d refreshPasses=%d refreshAvgMs=%.2f activeTaskNodes=%d timelineNodeCount=%d heapUsedMb=%.1f heapCommittedMb=%.1f processMemMb=%.1f cpu=%.1f%%%n",
                averagePulseMillis,
                layoutPasses,
                refreshPasses,
                averageRefreshMillis,
                activeTaskBarsByTaskId.size(),
                countNodes(this),
                memory.heapUsedMb(),
                memory.heapCommittedMb(),
                memory.processMemoryMb(),
                processCpuPercent
        );
    }

    private MemorySnapshot captureMemorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        double totalMb = runtime.totalMemory() / (1024.0 * 1024.0);
        double usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        double maxMb = runtime.maxMemory() / (1024.0 * 1024.0);
        return new MemorySnapshot(usedMb, totalMb, maxMb, readProcessMemoryMb());
    }

    private double readProcessMemoryMb() {
        if (osBean == null) {
            return Double.NaN;
        }
        long bytes = osBean.getCommittedVirtualMemorySize();
        if (bytes <= 0) {
            return Double.NaN;
        }
        // Best-effort RSS proxy from JMX without adding native dependencies.
        return bytes / (1024.0 * 1024.0);
    }

    private double readProcessCpuPercent() {
        if (osBean == null) {
            return Double.NaN;
        }
        double load = osBean.getProcessCpuLoad();
        if (load < 0) {
            return Double.NaN;
        }
        return load * 100.0;
    }

    private void trackTelemetryWarnings(double processMemoryMb, double processCpuPercent, boolean idle) {
        if (!Double.isNaN(processMemoryMb)) {
            processMemorySamplesMb.addLast(processMemoryMb);
            while (processMemorySamplesMb.size() > PERF_MEMORY_WINDOW_SAMPLES) {
                processMemorySamplesMb.removeFirst();
            }
            if (processMemorySamplesMb.size() == PERF_MEMORY_WINDOW_SAMPLES) {
                double memoryGrowth = processMemorySamplesMb.getLast() - processMemorySamplesMb.getFirst();
                if (memoryGrowth >= PERF_MEMORY_GROWTH_WARN_MB) {
                    System.out.printf(Locale.ROOT,
                            "[Perf][Gantt][WARN] sustained process-memory growth detected: +%.1fMB over %d samples%n",
                            memoryGrowth,
                            PERF_MEMORY_WINDOW_SAMPLES
                    );
                    processMemorySamplesMb.clear();
                }
            }
        }

        if (idle && !Double.isNaN(processCpuPercent) && processCpuPercent >= PERF_IDLE_CPU_WARN_PERCENT) {
            idleHighCpuSampleStreak++;
            if (idleHighCpuSampleStreak >= PERF_IDLE_CPU_WARN_STREAK) {
                System.out.printf(Locale.ROOT,
                        "[Perf][Gantt][WARN] idle CPU floor high: %.1f%% for %d consecutive samples%n",
                        processCpuPercent,
                        idleHighCpuSampleStreak
                );
                idleHighCpuSampleStreak = 0;
            }
        } else {
            idleHighCpuSampleStreak = 0;
        }
    }

    private int countNodes(Node node) {
        if (!(node instanceof Parent parent)) {
            return 1;
        }
        int count = 1;
        for (Node child : parent.getChildrenUnmodifiable()) {
            count += countNodes(child);
        }
        return count;
    }

    private void refreshHeaderFontFamily() {
        headerFontFamily = pickAvailableFontFamily(
                "Inter",
                "SF Pro Text",
                "Segoe UI",
                "Helvetica Neue",
                "Arial"
        );
        refreshHeaderRenderingCache();
    }

    private String pickAvailableFontFamily(String... candidates) {
        for (String candidate : candidates) {
            if (INSTALLED_FONT_FAMILIES.contains(candidate)) {
                return candidate;
            }
        }
        return Font.getDefault().getFamily();
    }

    private record MemorySnapshot(
            double heapUsedMb,
            double heapCommittedMb,
            double heapMaxMb,
            double processMemoryMb
    ) {
    }

    private void refreshHeaderRenderingCache() {
        cachedHeaderPrimaryFont = Font.font(headerFontFamily, FontWeight.SEMI_BOLD, 12);
        cachedHeaderSecondaryFont = Font.font(headerFontFamily, FontWeight.MEDIUM, 10);
        cachedHeaderYearFont = Font.font(headerFontFamily, FontWeight.BOLD, 12);

        cachedHeaderDateFormatter = DateTimeFormatter.ofPattern("MMM d", uiLocale);
        cachedHeaderWeekdayFormatter = DateTimeFormatter.ofPattern("EEE", uiLocale);
        cachedHeaderMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", uiLocale);
        cachedHeaderYearFormatter = DateTimeFormatter.ofPattern("yyyy", uiLocale);
        textWidthCache.clear();
    }

    private boolean isSelectionRecentlyChanged() {
        return System.currentTimeMillis() - selectionChangeAtMillis <= SELECTION_ANIMATION_WINDOW_MILLIS;
    }

    @Override
    protected void layoutChildren() {
        layoutPassCounter++;
        double width = getWidth();
        double height = getHeight();

        headerCanvas.resizeRelocate(0, 0, width, HEADER_HEIGHT);
        scrollPane.resizeRelocate(0, HEADER_HEIGHT, width, Math.max(0, height - HEADER_HEIGHT));
        if (Double.compare(lastLayoutWidth, width) != 0 || Double.compare(lastLayoutHeight, height) != 0) {
            lastLayoutWidth = width;
            lastLayoutHeight = height;
            requestRefreshAll();
        } else if (dragSelection.active) {
            updateSelectionOverlayBounds();
        }
        layoutEmptyState(width, height);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 1000;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 640;
    }

    private void updateTodayIndicator() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        boolean visible = !today.isBefore(timelineStart) && !today.isAfter(timelineEnd);
        double x = xForDateTime(now);
        double height = Math.max(rowHeight(), gridCanvas.getHeight());
        todayIndicatorLayer.updateLine(x, height, visible, darkTheme);
    }

    private void updateEmptyStateVisibility() {
        emptyState.setVisible(orderedTasks.isEmpty());
    }

    private void layoutEmptyState(double width, double height) {
        if (!emptyState.isVisible()) {
            return;
        }
        double bodyHeight = Math.max(0, height - HEADER_HEIGHT);
        double contentWidth = Math.min(360, Math.max(240, width - 48));
        double contentHeight = emptyState.prefHeight(contentWidth);
        double x = (width - contentWidth) / 2.0;
        double y = HEADER_HEIGHT + Math.max(0, (bodyHeight - contentHeight) / 2.0) - 10;
        emptyState.resizeRelocate(x, y, contentWidth, contentHeight);
    }

    private final class TaskBar extends Pane {
        private Task task;
        private int rowIndex;

        private final Rectangle base = new Rectangle();
        private final Rectangle accent = new Rectangle();
        private final Rectangle leftHandle = new Rectangle();
        private final Rectangle rightHandle = new Rectangle();
        private final Rectangle criticalOutline = new Rectangle();
        private final Label label = new Label();
        private final Label badge = new Label("!");
        private final DropShadow hoverShadow = new DropShadow();
        private final DropShadow selectionShadow = new DropShadow();
        private Tooltip tooltip;

        private LocalDate initialStart;
        private LocalDate initialDue;
        private LocalDate previewStart;
        private LocalDate previewDue;
        private double dragAnchorSceneX;
        private DragMode dragMode = DragMode.NONE;
        private boolean hovered;
        private boolean selectionAnimationPlayed;

        private TaskBar() {
            setManaged(false);

            base.setArcWidth(8);
            base.setArcHeight(8);
            accent.setArcWidth(8);
            accent.setArcHeight(8);
            criticalOutline.setArcWidth(8);
            criticalOutline.setArcHeight(8);
            criticalOutline.setFill(Color.TRANSPARENT);
            criticalOutline.setStroke(Color.web("#8B5CF6", 0.95));
            criticalOutline.setStrokeWidth(1.8);

            leftHandle.setWidth(HANDLE_WIDTH);
            rightHandle.setWidth(HANDLE_WIDTH);
            leftHandle.setCursor(Cursor.H_RESIZE);
            rightHandle.setCursor(Cursor.H_RESIZE);
            leftHandle.setFill(Color.color(0, 0, 0, 0.15));
            rightHandle.setFill(Color.color(0, 0, 0, 0.15));

            label.getStyleClass().add("gantt-bar-label");
            badge.getStyleClass().add("gantt-risk-badge");

            getChildren().addAll(criticalOutline, base, accent, leftHandle, rightHandle, label, badge);
            wireEvents();
        }

        private void bind(Task task, int rowIndex) {
            boolean taskChanged = this.task == null || !Objects.equals(this.task.id(), task.id());
            this.task = task;
            this.rowIndex = rowIndex;
            if (dragMode == DragMode.NONE || taskChanged) {
                this.initialStart = task.startDate();
                this.initialDue = task.dueDate();
                this.previewStart = task.startDate();
                this.previewDue = task.dueDate();
            }
            if (taskChanged) {
                hovered = false;
                selectionAnimationPlayed = false;
                dragMode = DragMode.NONE;
                setCursor(Cursor.DEFAULT);
            }
            if (pendingEntryAnimationTaskIds.remove(task.id())) {
                playEntryAnimation();
            } else {
                // Reset any transition residue when reusing bars from the pool.
                setOpacity(1.0);
                setTranslateY(0.0);
                setScaleX(1.0);
                setScaleY(1.0);
            }
            updateVisual(previewStart, previewDue);
        }

        private void prepareForReuse() {
            hovered = false;
            selectionAnimationPlayed = false;
            dragMode = DragMode.NONE;
            task = null;
            setCursor(Cursor.DEFAULT);
            setEffect(null);
            setOpacity(1.0);
            setTranslateY(0.0);
            setScaleX(1.0);
            setScaleY(1.0);
            if (tooltip != null) {
                tooltip.hide();
                Tooltip.uninstall(this, tooltip);
                tooltip = null;
            }
        }

        private void dispose() {
            if (tooltip != null) {
                Tooltip.uninstall(this, tooltip);
                tooltip = null;
            }
            setEffect(null);
        }

        private void wireEvents() {
            setOnMouseEntered(event -> {
                hovered = true;
                updateVisual(previewStart, previewDue);
            });

            setOnMouseExited(event -> {
                hovered = false;
                setCursor(Cursor.DEFAULT);
                updateVisual(previewStart, previewDue);
            });

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
                if (task == null) {
                    return;
                }
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
                if (task == null) {
                    return;
                }
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
                if (task == null) {
                    return;
                }
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
                if (task == null) {
                    return;
                }
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
            double y = rowIndex * rowHeight() + barVerticalPadding();
            double width = Math.max(MIN_VISIBLE_BAR_WIDTH, (ChronoUnit.DAYS.between(startDate, dueDate) + 1) * dayWidth());
            double height = rowHeight() - (2 * barVerticalPadding());

            relocate(x, y);
            setPrefSize(width, height);

            base.setWidth(width);
            base.setHeight(height);
            Color projectColor = safeColor(taskColorProvider.get().apply(task));
            base.setFill(resolveTaskGradient(projectColor));

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
            boolean selected = Objects.equals(selectedTaskId, task.id());
            if (selected) {
                base.setStroke(darkTheme ? Color.web("#CFE2FF") : Color.web("#1D4ED8"));
                base.setStrokeWidth(2.2);
                selectionShadow.setRadius(9);
                selectionShadow.setSpread(0.12);
                selectionShadow.setOffsetY(1.4);
                selectionShadow.setColor(darkTheme ? Color.web("#60A5FA", 0.5) : Color.web("#2563EB", 0.34));
                setEffect(selectionShadow);
                if (isSelectionRecentlyChanged() && !selectionAnimationPlayed) {
                    playSelectionAnimation();
                    selectionAnimationPlayed = true;
                }
            } else if (hasConflict) {
                base.setStroke(Color.web("#C73636"));
                base.setStrokeWidth(2);
                setEffect(null);
                selectionAnimationPlayed = false;
            } else if (hovered) {
                base.setStroke(darkTheme ? Color.web("#DAE4F4", 0.58) : Color.web("#334155", 0.28));
                base.setStrokeWidth(1.5);
                hoverShadow.setRadius(5);
                hoverShadow.setOffsetY(1);
                hoverShadow.setSpread(0.04);
                hoverShadow.setColor(darkTheme ? Color.web("#94A3B8", 0.28) : Color.web("#0F172A", 0.16));
                setEffect(hoverShadow);
                selectionAnimationPlayed = false;
            } else {
                base.setStroke(darkTheme ? Color.color(1, 1, 1, 0.22) : Color.color(0, 0, 0, 0.16));
                base.setStrokeWidth(1);
                setEffect(null);
                selectionAnimationPlayed = false;
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
                badge.setLayoutY(1);
            }

            label.setText(compactLabel(task, width, showBadgeVisual));
            label.setVisible(width >= 36);
            label.setLayoutX(8);
            label.setLayoutY(Math.max(3, (height - 14) / 2));
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

        private void playEntryAnimation() {
            setOpacity(0.0);
            setTranslateY(6.0);

            FadeTransition fade = new FadeTransition(Duration.millis(120), this);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(120), this);
            slide.setFromY(6.0);
            slide.setToY(0.0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            fade.play();
            slide.play();
        }

        private void playSelectionAnimation() {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(160), this);
            scaleTransition.setFromX(0.985);
            scaleTransition.setFromY(0.985);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.setInterpolator(Interpolator.EASE_OUT);
            scaleTransition.playFromStart();
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
            if (!dragSelection.active || event.getTarget() == interactionSurface) {
                return;
            }
            Point2D local = interactionSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
            dragSelection.pendingUnitStart = snapDateToScale(mouseXToDate(local.getX()));
            dragSelection.cursorScreenX = event.getScreenX();
            dragSelection.cursorScreenY = event.getScreenY();
            queueDragOverlayUpdate();
        };
        sceneReleaseCaptureHandler = event -> {
            if (!dragSelection.active || event.getTarget() == interactionSurface) {
                return;
            }
            Point2D local = interactionSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
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

        Bounds newRect = new BoundingBox(x, 0, width, Math.max(rowHeight(), gridCanvas.getHeight()));
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
            dragSelectionTooltip.show(interactionSurface, anchorX, anchorY);
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
            case HOUR -> date;
            case DAY -> date;
            case WEEK -> date.with(DayOfWeek.MONDAY);
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private LocalDate unitEndDate(LocalDate unitStart) {
        LocalDate end = switch (scale.get()) {
            case HOUR -> unitStart;
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

    private static final class BackgroundGridLayer extends Pane {
        private BackgroundGridLayer(Canvas canvas) {
            setMouseTransparent(true);
            setPickOnBounds(false);
            getChildren().add(canvas);
        }
    }

    private static final class TaskLayer extends Pane {
        private TaskLayer(Pane bars) {
            setPickOnBounds(false);
            getChildren().add(bars);
        }
    }

    private static final class DependencyLayer extends Pane {
        private DependencyLayer() {
            setMouseTransparent(true);
            setPickOnBounds(false);
        }
    }

    private static final class SelectionLayer extends Pane {
        private SelectionLayer() {
            setPickOnBounds(false);
        }
    }

    private static final class TodayIndicatorLayer extends Pane {
        private final Line glowLine = new Line();
        private final Line coreLine = new Line();
        private Timeline movementTimeline;
        private double currentX = Double.NaN;

        private TodayIndicatorLayer() {
            setMouseTransparent(true);
            setPickOnBounds(false);
            glowLine.setStrokeWidth(3.0);
            coreLine.setStrokeWidth(1.2);
            getChildren().addAll(glowLine, coreLine);
        }

        private void updateLine(double x, double height, boolean visible, boolean darkTheme) {
            if (!visible) {
                if (movementTimeline != null) {
                    movementTimeline.stop();
                    movementTimeline = null;
                }
                currentX = Double.NaN;
                glowLine.setVisible(false);
                coreLine.setVisible(false);
                return;
            }
            Color core = darkTheme ? Color.web("#FB7185") : Color.web("#E11D48");
            Color glow = darkTheme ? Color.web("#FB7185", 0.36) : Color.web("#F43F5E", 0.28);

            glowLine.setStroke(glow);
            coreLine.setStroke(core);
            glowLine.setStartY(0);
            glowLine.setEndY(height);
            coreLine.setStartY(0);
            coreLine.setEndY(height);
            glowLine.setVisible(true);
            coreLine.setVisible(true);

            if (Double.isNaN(currentX)) {
                currentX = x;
                glowLine.setStartX(x);
                glowLine.setEndX(x);
                coreLine.setStartX(x);
                coreLine.setEndX(x);
                return;
            }

            if (Math.abs(currentX - x) <= GanttChartView.TODAY_INDICATOR_ANIMATION_THRESHOLD_PX) {
                currentX = x;
                glowLine.setStartX(x);
                glowLine.setEndX(x);
                coreLine.setStartX(x);
                coreLine.setEndX(x);
                return;
            }

            if (movementTimeline != null) {
                movementTimeline.stop();
            }
            movementTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(glowLine.startXProperty(), glowLine.getStartX()),
                            new KeyValue(glowLine.endXProperty(), glowLine.getEndX()),
                            new KeyValue(coreLine.startXProperty(), coreLine.getStartX()),
                            new KeyValue(coreLine.endXProperty(), coreLine.getEndX())
                    ),
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(glowLine.startXProperty(), x, Interpolator.EASE_BOTH),
                            new KeyValue(glowLine.endXProperty(), x, Interpolator.EASE_BOTH),
                            new KeyValue(coreLine.startXProperty(), x, Interpolator.EASE_BOTH),
                            new KeyValue(coreLine.endXProperty(), x, Interpolator.EASE_BOTH)
                    )
            );
            movementTimeline.setOnFinished(event -> movementTimeline = null);
            currentX = x;
            movementTimeline.playFromStart();
        }
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
