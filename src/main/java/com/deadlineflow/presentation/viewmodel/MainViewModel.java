package com.deadlineflow.presentation.viewmodel;

import com.deadlineflow.application.services.ConflictService;
import com.deadlineflow.application.services.CriticalPathResult;
import com.deadlineflow.application.services.CriticalPathService;
import com.deadlineflow.application.services.DependencyGraphService;
import com.deadlineflow.application.services.RiskService;
import com.deadlineflow.application.services.SchedulerEngine;
import com.deadlineflow.application.services.TaskService;
import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.data.repository.StatusRepository;
import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Conflict;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.StatusDefinition;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

public class MainViewModel {
    public static final String DONE_STATUS = "DONE";
    private static final boolean MODEL_DEBUG = Boolean.getBoolean("deadlineflow.debug.model");
    private static final String PREF_KEY_TIMELINE_ZOOM = "timeline_zoom";
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;

    private final Preferences preferences = Preferences.userNodeForPackage(MainViewModel.class);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;
    private final StatusRepository statusRepository;

    private final SchedulerEngine schedulerEngine;
    private final TaskService taskService;
    private final ConflictService conflictService;
    private final RiskService riskService;
    private final DependencyGraphService dependencyGraphService;
    private final CriticalPathService criticalPathService;

    private final ObjectProperty<Project> selectedProject = new SimpleObjectProperty<>();
    private final ObjectProperty<Task> selectedTask = new SimpleObjectProperty<>();
    private final ObjectProperty<TimeScale> scale = new SimpleObjectProperty<>(TimeScale.WEEK);
    private final DoubleProperty zoom = new SimpleDoubleProperty(loadZoomPreference());

    private final StringProperty bannerMessage = new SimpleStringProperty("");
    private final StringProperty cpmMessage = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> projectFinishDate = new SimpleObjectProperty<>();

    private final ObservableList<Project> projects;
    private final ObservableList<Task> allTasks;
    private final ObservableList<Dependency> allDependencies;
    private final ObservableList<StatusDefinition> statusDefinitions;
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();
    private final FilteredList<Task> projectTasks;
    private final FilteredList<Dependency> projectDependencies;

    private final ObservableList<Conflict> conflicts = FXCollections.observableArrayList();

    private final MapProperty<String, RiskLevel> riskByTaskId = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final MapProperty<String, String> conflictMessageByTaskId = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final MapProperty<String, Integer> slackByTaskId = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final SetProperty<String> criticalTaskIds = new SimpleSetProperty<>(FXCollections.observableSet(new HashSet<>()));

    private final ObservableList<Task> dueToday = FXCollections.observableArrayList();
    private final ObservableList<Task> dueInSevenDays = FXCollections.observableArrayList();
    private final ObservableList<Task> overdue = FXCollections.observableArrayList();
    private final ObservableList<Task> blockedByDependencies = FXCollections.observableArrayList();

    private boolean normalizingStatuses;
    private boolean handlingTaskListChange;
    private final ExecutorService derivedStateExecutor;
    private final AtomicLong derivedStateGeneration = new AtomicLong();
    private long latestScheduledDerivedStateGeneration;
    private boolean derivedStateComputationInFlight;

    public MainViewModel(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            DependencyRepository dependencyRepository,
            StatusRepository statusRepository,
            TaskService taskService,
            SchedulerEngine schedulerEngine,
            ConflictService conflictService,
            RiskService riskService,
            DependencyGraphService dependencyGraphService,
            CriticalPathService criticalPathService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.statusRepository = statusRepository;
        this.taskService = taskService;
        this.schedulerEngine = schedulerEngine;
        this.conflictService = conflictService;
        this.riskService = riskService;
        this.dependencyGraphService = dependencyGraphService;
        this.criticalPathService = criticalPathService;
        this.derivedStateExecutor = Executors.newSingleThreadExecutor(daemonThreadFactory("deadlineflow-derived-state"));

        this.projects = projectRepository.getAll();
        this.allTasks = taskRepository.getAll();
        this.allDependencies = dependencyRepository.getAll();
        this.statusDefinitions = statusRepository.getAll();
        this.projectTasks = new FilteredList<>(allTasks, task -> false);
        this.projectDependencies = new FilteredList<>(allDependencies, dependency -> false);

        refreshStatusOptions();

        statusDefinitions.addListener((javafx.collections.ListChangeListener<? super StatusDefinition>) change -> {
            debugModel("listener: statusDefinitions changed");
            refreshStatusOptions();
            scheduleDerivedStateRecompute("status-definitions-listener");
        });

        selectedProject.addListener((obs, oldValue, newValue) -> {
            debugModel("listener: selectedProject changed to " + (newValue == null ? "null" : newValue.id()));
            refreshProjectFilters();
            if (selectedTask.get() != null && (newValue == null || selectedTask.get().projectId() != newValue.id())) {
                selectedTask.set(null);
            }
            scheduleDerivedStateRecompute("selected-project-listener");
        });

        allTasks.addListener((javafx.collections.ListChangeListener<? super Task>) change -> {
            if (handlingTaskListChange) {
                debugModel("listener: allTasks re-entrant change coalesced");
                refreshProjectFilters();
                scheduleDerivedStateRecompute("tasks-listener-reentrant");
                return;
            }
            handlingTaskListChange = true;
            debugModel("listener: allTasks changed");
            try {
                if (selectedTask.get() != null && findTask(selectedTask.get().id()).isEmpty()) {
                    selectedTask.set(null);
                }
                refreshProjectFilters();
                scheduleDerivedStateRecompute("tasks-listener");
            } finally {
                handlingTaskListChange = false;
            }
        });
        allDependencies.addListener((javafx.collections.ListChangeListener<? super Dependency>) change -> {
            debugModel("listener: allDependencies changed");
            scheduleDerivedStateRecompute("dependencies-listener");
        });

        zoom.addListener((obs, oldValue, newValue) -> {
            double normalizedZoom = clampZoom(newValue.doubleValue());
            if (Double.compare(normalizedZoom, newValue.doubleValue()) != 0) {
                zoom.set(normalizedZoom);
                return;
            }
            preferences.putDouble(PREF_KEY_TIMELINE_ZOOM, normalizedZoom);
        });

        normalizeUnknownTaskStatuses();

        if (!projects.isEmpty()) {
            selectedProject.set(projects.getFirst());
        } else {
            refreshProjectFilters();
            scheduleDerivedStateRecompute("initial-empty-projects");
        }
    }

    public ObservableList<Project> projects() {
        return projects;
    }

    public FilteredList<Task> projectTasks() {
        return projectTasks;
    }

    public ObservableList<Task> allTasks() {
        return allTasks;
    }

    public ObservableList<Conflict> conflicts() {
        return conflicts;
    }

    public FilteredList<Dependency> projectDependencies() {
        return projectDependencies;
    }

    public ObservableList<Task> dueToday() {
        return dueToday;
    }

    public ObservableList<Task> dueInSevenDays() {
        return dueInSevenDays;
    }

    public ObservableList<Task> overdue() {
        return overdue;
    }

    public ObservableList<Task> blockedByDependencies() {
        return blockedByDependencies;
    }

    public ObservableList<String> statusOptions() {
        return statusOptions;
    }

    public boolean isStatusProtected(String statusName) {
        return statusDefinitions.stream()
                .filter(def -> def.name().equals(statusName))
                .findFirst()
                .map(StatusDefinition::isProtected)
                .orElse(false);
    }

    public ObjectProperty<Project> selectedProjectProperty() {
        return selectedProject;
    }

    public ObjectProperty<Task> selectedTaskProperty() {
        return selectedTask;
    }

    public ObjectProperty<TimeScale> scaleProperty() {
        return scale;
    }

    public DoubleProperty zoomProperty() {
        return zoom;
    }

    public StringProperty bannerMessageProperty() {
        return bannerMessage;
    }

    public StringProperty cpmMessageProperty() {
        return cpmMessage;
    }

    public ObjectProperty<LocalDate> projectFinishDateProperty() {
        return projectFinishDate;
    }

    public MapProperty<String, RiskLevel> riskByTaskIdProperty() {
        return riskByTaskId;
    }

    public MapProperty<String, String> conflictMessageByTaskIdProperty() {
        return conflictMessageByTaskId;
    }

    public SetProperty<String> criticalTaskIdsProperty() {
        return criticalTaskIds;
    }

    public MapProperty<String, Integer> slackByTaskIdProperty() {
        return slackByTaskId;
    }

    public void selectProject(Project project) {
        selectedProject.set(project);
    }

    public void selectTask(Task task) {
        if (task == null) {
            selectedTask.set(null);
            return;
        }
        findTask(task.id()).ifPresent(selectedTask::set);
    }

    public void selectTaskById(String taskId) {
        if (taskId == null) {
            selectedTask.set(null);
            return;
        }
        findTask(taskId).ifPresent(selectedTask::set);
    }

    public int taskCountForProject(long projectId) {
        int count = 0;
        for (Task task : allTasks) {
            if (task.projectId() == projectId) {
                count++;
            }
        }
        return count;
    }

    public void createProject(String name, String color, int priority) {
        try {
            Project created = projectRepository.save(new Project(0, name, color, priority));
            selectedProject.set(projectRepository.findById(created.id()).orElse(created));
            bannerMessage.set("Project created");
            scheduleDerivedStateRecompute("create-project");
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void updateSelectedProject(String name, String color, int priority) {
        Project project = selectedProject.get();
        if (project == null) {
            return;
        }
        try {
            Project updated = new Project(project.id(), name, color, priority);
            projectRepository.save(updated);
            selectedProject.set(projectRepository.findById(project.id()).orElse(updated));
            bannerMessage.set("Project updated");
            scheduleDerivedStateRecompute("update-project");
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void deleteSelectedProject() {
        Project project = selectedProject.get();
        if (project == null) {
            return;
        }
        projectRepository.delete(project.id());
        selectedTask.set(null);
        if (projects.isEmpty()) {
            selectedProject.set(null);
        } else {
            selectedProject.set(projects.getFirst());
        }
        bannerMessage.set("Project deleted");
        scheduleDerivedStateRecompute("delete-project");
    }

    public Task createTask(String title, LocalDate startDate, LocalDate dueDate) {
        long startNanos = System.nanoTime();
        debugModel("createTask ENTRY title=" + title + " start=" + startDate + " due=" + dueDate);
        Project project = selectedProject.get();
        if (project == null) {
            throw new ValidationException("Select a project before creating tasks");
        }

        try {
            Task savedTask = taskService.createTask(project.id(), title, startDate, dueDate, defaultStatus());
            selectTask(savedTask);
            bannerMessage.set("Task created");
            scheduleDerivedStateRecompute("create-task");
            debugModel("createTask EXIT id=" + savedTask.id() + " elapsedNanos=" + (System.nanoTime() - startNanos));
            return savedTask;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public Task createTask(LocalDate startDate, LocalDate dueDate) {
        return createTask("New Task", startDate, dueDate);
    }

    public void deleteSelectedTask() {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        deleteTaskById(task.id());
    }

    public void deleteTaskById(String taskId) {
        Optional<Task> existing = taskRepository.findById(taskId);
        if (existing.isEmpty()) {
            return;
        }
        taskRepository.delete(taskId);
        selectedTask.set(null);
        bannerMessage.set("Task deleted");
        scheduleDerivedStateRecompute("delete-task");
        debugModel("deleteTask " + taskId);
    }

    public void updateSelectedTaskTitle(String title) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        try {
            Task updated = task.withTitle(title);
            taskRepository.save(updated);
            selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
            scheduleDerivedStateRecompute("update-task-title");
            debugModel("updateSelectedTaskTitle " + updated.id() + " -> " + updated.title());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void updateSelectedTaskDescription(String description) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        if (task.description().equals(description)) {
            return;
        }
        // Description is persisted directly on the task record so text survives reload/relaunch.
        Task updated = task.withDescription(description);
        taskRepository.save(updated);
        selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
        scheduleDerivedStateRecompute("update-selected-task-description");
    }

    public void updateTaskDescription(String taskId, String description) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);
        if (maybeTask.isEmpty()) {
            return;
        }
        Task task = maybeTask.get();
        if (task.description().equals(description)) {
            return;
        }
        Task updated = task.withDescription(description);
        taskRepository.save(updated);
        if (selectedTask.get() != null && selectedTask.get().id().equals(taskId)) {
            selectedTask.set(taskRepository.findById(taskId).orElse(updated));
        }
        scheduleDerivedStateRecompute("update-task-description");
    }

    public void updateSelectedTaskDates(LocalDate startDate, LocalDate dueDate) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        try {
            Task updated = task.withDates(startDate, dueDate);
            schedulerEngine.validateTaskDates(updated);
            taskRepository.save(updated);
            selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
            scheduleDerivedStateRecompute("update-selected-task-dates");
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void updateTaskDatesFromGantt(String taskId, LocalDate startDate, LocalDate dueDate) {
        Optional<Task> maybeTask = taskRepository.findById(taskId);
        if (maybeTask.isEmpty()) {
            return;
        }
        try {
            Task updated = maybeTask.get().withDates(startDate, dueDate);
            schedulerEngine.validateTaskDates(updated);
            taskRepository.save(updated);
            if (selectedTask.get() != null && selectedTask.get().id().equals(taskId)) {
                selectedTask.set(taskRepository.findById(taskId).orElse(updated));
            }
            scheduleDerivedStateRecompute("update-task-dates-from-gantt");
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void updateSelectedTaskProgress(int progress) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        Task updated = task.withProgress(progress);
        taskRepository.save(updated);
        selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
        scheduleDerivedStateRecompute("update-task-progress");
    }

    public void updateSelectedTaskStatus(String status) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        Task updated = task.withStatus(status);
        taskRepository.save(updated);
        selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
        scheduleDerivedStateRecompute("update-task-status");
    }

    public void addStatus(String name) {
        statusRepository.add(name);
        bannerMessage.set("Status added");
        scheduleDerivedStateRecompute("add-status");
    }

    public void renameStatus(String oldName, String newName) {
        statusRepository.rename(oldName, newName);
        if (selectedTask.get() != null) {
            findTask(selectedTask.get().id()).ifPresent(selectedTask::set);
        }
        bannerMessage.set("Status renamed");
        scheduleDerivedStateRecompute("rename-status");
    }

    public void deleteStatus(String name) {
        if (statusOptions.size() <= 1) {
            throw new ValidationException("At least one status must remain");
        }
        String fallback = statusOptions.stream()
                .filter(option -> !option.equals(name))
                .findFirst()
                .orElse(defaultStatus());
        statusRepository.delete(name, fallback);
        if (selectedTask.get() != null) {
            findTask(selectedTask.get().id()).ifPresent(selectedTask::set);
        }
        bannerMessage.set("Status deleted");
        scheduleDerivedStateRecompute("delete-status");
    }

    public ObservableList<Dependency> dependenciesForSelectedTask() {
        Task task = selectedTask.get();
        if (task == null) {
            return FXCollections.observableArrayList();
        }
        ObservableList<Dependency> incoming = FXCollections.observableArrayList();
        for (Dependency dependency : projectDependencies) {
            if (dependency.toTaskId().equals(task.id())) {
                incoming.add(dependency);
            }
        }
        return incoming;
    }

    public String taskTitle(String taskId) {
        return findTask(taskId).map(Task::title).orElse(taskId);
    }

    public boolean addDependency(String fromTaskId, String toTaskId) {
        if (fromTaskId == null || toTaskId == null || fromTaskId.equals(toTaskId)) {
            bannerMessage.set("Invalid dependency selection");
            return false;
        }

        boolean duplicate = projectDependencies.stream()
                .anyMatch(existing -> existing.fromTaskId().equals(fromTaskId) && existing.toTaskId().equals(toTaskId));
        if (duplicate) {
            bannerMessage.set("Dependency already exists");
            return false;
        }

        Dependency candidate;
        try {
            candidate = new Dependency(UUID.randomUUID().toString(), fromTaskId, toTaskId, DependencyType.FINISH_START);
        } catch (IllegalArgumentException ex) {
            bannerMessage.set(ex.getMessage());
            return false;
        }
        if (dependencyGraphService.createsCycle(projectTasks, projectDependencies, candidate)) {
            bannerMessage.set("Cannot add dependency because it introduces a cycle");
            return false;
        }

        dependencyRepository.save(candidate);
        bannerMessage.set("Dependency added");
        scheduleDerivedStateRecompute("add-dependency");
        return true;
    }

    public void removeDependency(String dependencyId) {
        dependencyRepository.delete(dependencyId);
        bannerMessage.set("Dependency removed");
        scheduleDerivedStateRecompute("remove-dependency");
    }

    public Optional<Task> findTask(String taskId) {
        return taskRepository.findById(taskId);
    }

    public String dependencyLabel(Dependency dependency) {
        return taskTitle(dependency.fromTaskId()) + " -> " + taskTitle(dependency.toTaskId());
    }

    public Integer slackDaysForTask(String taskId) {
        return slackByTaskId.get(taskId);
    }

    public String slackLabelForTask(String taskId) {
        Integer slack = slackByTaskId.get(taskId);
        if (slack == null) {
            return "Slack: -";
        }
        return "Slack: " + slack + " days";
    }

    private void refreshProjectFilters() {
        Project project = selectedProject.get();
        if (project == null) {
            projectTasks.setPredicate(task -> false);
            projectDependencies.setPredicate(dependency -> false);
            return;
        }

        Set<String> projectTaskIds = new HashSet<>();
        for (Task task : allTasks) {
            if (task.projectId() == project.id()) {
                projectTaskIds.add(task.id());
            }
        }

        projectTasks.setPredicate(task -> task.projectId() == project.id());
        projectDependencies.setPredicate(dependency ->
                projectTaskIds.contains(dependency.fromTaskId()) && projectTaskIds.contains(dependency.toTaskId()));
    }

    private void refreshStatusOptions() {
        List<String> names = statusDefinitions.stream().map(StatusDefinition::name).toList();
        statusOptions.setAll(names);
        if (statusOptions.isEmpty()) {
            statusOptions.add(Task.DEFAULT_STATUS);
        }
    }

    private void normalizeUnknownTaskStatuses() {
        if (normalizingStatuses) {
            return;
        }
        if (allTasks.isEmpty()) {
            return;
        }

        Set<String> knownStatuses = new HashSet<>(statusOptions);
        String fallbackStatus = defaultStatus();

        List<Task> invalidTasks = allTasks.stream()
                .filter(task -> !knownStatuses.contains(task.status()))
                .toList();

        if (invalidTasks.isEmpty()) {
            return;
        }

        normalizingStatuses = true;
        try {
            for (Task task : invalidTasks) {
                taskRepository.save(task.withStatus(fallbackStatus));
            }
        } finally {
            normalizingStatuses = false;
        }
    }

    private String defaultStatus() {
        return statusOptions.isEmpty() ? Task.DEFAULT_STATUS : statusOptions.getFirst();
    }

    private boolean isDone(Task task) {
        return DONE_STATUS.equals(task.status());
    }

    private void scheduleDerivedStateRecompute(String source) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> scheduleDerivedStateRecompute(source));
            return;
        }

        long generation = derivedStateGeneration.incrementAndGet();
        latestScheduledDerivedStateGeneration = generation;
        debugModel("scheduleDerivedState source=" + source + " gen=" + generation);

        if (derivedStateComputationInFlight) {
            return;
        }
        startDerivedStateComputation(generation);
    }

    private void startDerivedStateComputation(long generation) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> startDerivedStateComputation(generation));
            return;
        }

        derivedStateComputationInFlight = true;

        List<Task> activeTasks = snapshotActiveTasks();
        List<Dependency> activeDependencies = snapshotActiveDependencies(activeTasks);
        LocalDate today = LocalDate.now();
        debugModel("derivedState ENTRY gen=" + generation + " tasks=" + activeTasks.size() + " deps=" + activeDependencies.size());

        // Freeze root cause: expensive conflict/risk/CPM recomputation repeatedly ran on the JavaFX thread.
        // Fix: compute derived state off-thread and apply only final coalesced result on the UI thread.
        derivedStateExecutor.submit(() -> {
            try {
                long computeStart = System.nanoTime();
                DerivedStateResult result = computeDerivedState(activeTasks, activeDependencies, today);
                long computeDurationNanos = System.nanoTime() - computeStart;

                Platform.runLater(() -> {
                    try {
                        if (generation == latestScheduledDerivedStateGeneration) {
                            applyDerivedState(result);
                            debugModel("derivedState APPLY gen=" + generation + " computeNanos=" + computeDurationNanos);
                        } else {
                            debugModel("derivedState SKIP stale gen=" + generation + " latest=" + latestScheduledDerivedStateGeneration);
                        }
                    } finally {
                        if (generation < latestScheduledDerivedStateGeneration) {
                            startDerivedStateComputation(latestScheduledDerivedStateGeneration);
                        } else {
                            derivedStateComputationInFlight = false;
                        }
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    derivedStateComputationInFlight = false;
                    debugModel("derivedState ERROR " + ex.getMessage());
                });
            }
        });
    }

    private List<Task> snapshotActiveTasks() {
        Project project = selectedProject.get();
        if (project == null) {
            return List.of();
        }

        List<Task> active = new ArrayList<>(allTasks.size());
        for (Task task : allTasks) {
            if (task.projectId() == project.id()) {
                active.add(task);
            }
        }
        return List.copyOf(active);
    }

    private List<Dependency> snapshotActiveDependencies(Collection<Task> activeTasks) {
        if (activeTasks.isEmpty()) {
            return List.of();
        }
        Set<String> activeTaskIds = new HashSet<>(Math.max(16, activeTasks.size() * 2));
        for (Task task : activeTasks) {
            activeTaskIds.add(task.id());
        }

        List<Dependency> active = new ArrayList<>(Math.min(allDependencies.size(), activeTaskIds.size() * 2));
        for (Dependency dependency : allDependencies) {
            if (activeTaskIds.contains(dependency.fromTaskId()) && activeTaskIds.contains(dependency.toTaskId())) {
                active.add(dependency);
            }
        }
        return List.copyOf(active);
    }

    private DerivedStateResult computeDerivedState(Collection<Task> activeTasks, Collection<Dependency> activeDependencies, LocalDate today) {
        List<Conflict> detectedConflicts = conflictService.detectDependencyConflicts(activeTasks, activeDependencies);

        Map<String, String> conflictMap = new HashMap<>(Math.max(16, detectedConflicts.size() * 2));
        for (Conflict conflict : detectedConflicts) {
            conflictMap.merge(conflict.toTaskId(), conflict.message(), (a, b) -> a + "\n" + b);
        }

        CriticalPathResult cpm = criticalPathService.compute(activeTasks, activeDependencies);
        String cpmBannerText = cpm.hasCycle() ? "Critical path disabled: dependency cycle detected" : "";
        LocalDate finishDate = cpm.hasCycle() ? null : cpm.projectFinishDate();
        Set<String> criticalIds = cpm.hasCycle() ? Set.of() : new HashSet<>(cpm.criticalTaskIds());
        Map<String, Integer> slackMap = cpm.hasCycle() ? Map.of() : new HashMap<>(cpm.slackDays());

        Map<String, Task> taskById = new HashMap<>(Math.max(16, activeTasks.size() * 2));
        Map<String, RiskLevel> riskMap = new HashMap<>(Math.max(16, activeTasks.size() * 2));
        List<Task> dueTodayTasks = new ArrayList<>();
        List<Task> dueInSevenTasks = new ArrayList<>();
        List<Task> overdueTasks = new ArrayList<>();
        LocalDate todayPlusSeven = today.plusDays(7);
        for (Task task : activeTasks) {
            taskById.put(task.id(), task);
            riskMap.put(task.id(), riskService.evaluate(task, today, DONE_STATUS));
            if (isDone(task)) {
                continue;
            }
            LocalDate dueDate = task.dueDate();
            if (dueDate.isEqual(today)) {
                dueTodayTasks.add(task);
            } else if (dueDate.isBefore(today)) {
                overdueTasks.add(task);
            } else if (!dueDate.isAfter(todayPlusSeven)) {
                dueInSevenTasks.add(task);
            }
        }

        Set<String> blockedTaskIds = new HashSet<>();
        for (Dependency dependency : activeDependencies) {
            Task from = taskById.get(dependency.fromTaskId());
            Task to = taskById.get(dependency.toTaskId());
            if (from == null || to == null || isDone(to) || isDone(from)) {
                continue;
            }
            blockedTaskIds.add(to.id());
        }

        List<Task> blockedTasks = new ArrayList<>(blockedTaskIds.size());
        for (String taskId : blockedTaskIds) {
            Task task = taskById.get(taskId);
            if (task != null) {
                blockedTasks.add(task);
            }
        }

        return new DerivedStateResult(
                List.copyOf(detectedConflicts),
                conflictMap,
                riskMap,
                cpmBannerText,
                finishDate,
                criticalIds,
                slackMap,
                sorted(dueTodayTasks),
                sorted(dueInSevenTasks),
                sorted(overdueTasks),
                sorted(blockedTasks)
        );
    }

    private void applyDerivedState(DerivedStateResult result) {
        conflicts.setAll(result.conflicts());
        conflictMessageByTaskId.clear();
        conflictMessageByTaskId.putAll(result.conflictMessageByTaskId());

        riskByTaskId.clear();
        riskByTaskId.putAll(result.riskByTaskId());

        cpmMessage.set(result.cpmMessage());
        projectFinishDate.set(result.projectFinishDate());

        criticalTaskIds.clear();
        criticalTaskIds.addAll(result.criticalTaskIds());

        slackByTaskId.clear();
        slackByTaskId.putAll(result.slackByTaskId());

        dueToday.setAll(result.dueToday());
        dueInSevenDays.setAll(result.dueInSevenDays());
        overdue.setAll(result.overdue());
        blockedByDependencies.setAll(result.blockedByDependencies());
    }

    private List<Task> sorted(List<Task> tasks) {
        List<Task> mutable = new ArrayList<>(tasks);
        mutable.sort(Comparator.comparing(Task::dueDate).thenComparing(Task::startDate).thenComparing(Task::title));
        return mutable;
    }

    public String projectFinishDateText() {
        if (projectFinishDate.get() == null) {
            return "Project Finish Date: -";
        }
        return "Project Finish Date: " + DateTimeFormatter.ISO_LOCAL_DATE.format(projectFinishDate.get());
    }

    public void shutdown() {
        derivedStateExecutor.shutdownNow();
    }

    private ThreadFactory daemonThreadFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
    }

    private double loadZoomPreference() {
        return clampZoom(preferences.getDouble(PREF_KEY_TIMELINE_ZOOM, DEFAULT_ZOOM));
    }

    private double clampZoom(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_ZOOM;
        }
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }

    private record DerivedStateResult(
            List<Conflict> conflicts,
            Map<String, String> conflictMessageByTaskId,
            Map<String, RiskLevel> riskByTaskId,
            String cpmMessage,
            LocalDate projectFinishDate,
            Set<String> criticalTaskIds,
            Map<String, Integer> slackByTaskId,
            List<Task> dueToday,
            List<Task> dueInSevenDays,
            List<Task> overdue,
            List<Task> blockedByDependencies
    ) {
    }

    private void debugModel(String message) {
        if (!MODEL_DEBUG) {
            return;
        }
        System.out.println(System.nanoTime() + " [MainViewModel][" + Thread.currentThread().getName() + "] " + message);
    }
}
