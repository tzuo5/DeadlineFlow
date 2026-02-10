package com.deadlineflow.app;

import com.deadlineflow.application.services.ConflictService;
import com.deadlineflow.application.services.CriticalPathService;
import com.deadlineflow.application.services.DependencyGraphService;
import com.deadlineflow.application.services.RiskService;
import com.deadlineflow.application.services.SchedulerEngine;
import com.deadlineflow.application.services.TaskService;
import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.data.repository.StatusRepository;
import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.data.sqlite.SampleDataSeeder;
import com.deadlineflow.data.sqlite.SqliteDatabase;
import com.deadlineflow.data.sqlite.SqliteDependencyRepository;
import com.deadlineflow.data.sqlite.SqliteMigration;
import com.deadlineflow.data.sqlite.SqliteProjectRepository;
import com.deadlineflow.data.sqlite.SqliteStatusRepository;
import com.deadlineflow.data.sqlite.SqliteTaskRepository;
import com.deadlineflow.data.sqlite.SqliteWorkspaceStore;
import com.deadlineflow.presentation.theme.ThemeManager;
import com.deadlineflow.presentation.viewmodel.LanguageManager;
import com.deadlineflow.presentation.viewmodel.MainViewModel;

public class AppContext {
    private final SqliteWorkspaceStore workspaceStore;

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;
    private final StatusRepository statusRepository;
    private final LanguageManager languageManager;
    private final ThemeManager themeManager;

    private final MainViewModel mainViewModel;

    public AppContext() {
        SqliteDatabase database = new SqliteDatabase();
        SqliteMigration migration = new SqliteMigration();
        SampleDataSeeder sampleDataSeeder = new SampleDataSeeder();
        workspaceStore = new SqliteWorkspaceStore(database, migration, sampleDataSeeder);
        workspaceStore.initialize();

        projectRepository = new SqliteProjectRepository(database, workspaceStore);
        taskRepository = new SqliteTaskRepository(database, workspaceStore);
        dependencyRepository = new SqliteDependencyRepository(database, workspaceStore);
        statusRepository = new SqliteStatusRepository(database, workspaceStore);
        languageManager = new LanguageManager();
        themeManager = new ThemeManager();

        SchedulerEngine schedulerEngine = new SchedulerEngine();
        TaskService taskService = new TaskService(taskRepository, schedulerEngine);
        ConflictService conflictService = new ConflictService();
        RiskService riskService = new RiskService();
        DependencyGraphService dependencyGraphService = new DependencyGraphService();
        CriticalPathService criticalPathService = new CriticalPathService(dependencyGraphService);

        mainViewModel = new MainViewModel(
                projectRepository,
                taskRepository,
                dependencyRepository,
                statusRepository,
                taskService,
                schedulerEngine,
                conflictService,
                riskService,
                dependencyGraphService,
                criticalPathService
        );
    }

    public MainViewModel mainViewModel() {
        return mainViewModel;
    }

    public LanguageManager languageManager() {
        return languageManager;
    }

    public ThemeManager themeManager() {
        return themeManager;
    }
}
