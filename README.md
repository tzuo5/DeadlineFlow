# DeadlineFlow — Visual Deadline Planner

DeadlineFlow is an offline-first JavaFX desktop app for visual project scheduling with a Gantt timeline, task dependencies, conflict/risk detection, and critical path analysis.

## Features Checklist

- [x] Offline-only local app (no accounts, no network calls)
- [x] Java 21 + JavaFX 21
- [x] Multi-project management with project colors and priorities
- [x] Gantt timeline with Day / Week / Month scale and zoom
- [x] Drag task bars to move/resize dates (minimum duration 1 day)
- [x] Task CRUD with progress (0-100), status, and dependency controls
- [x] Finish-Start dependencies with cycle prevention
- [x] Conflict detection + conflict section + tooltips
- [x] Risk rules:
  - [x] Overdue => red accent
  - [x] Due in <48h => yellow accent
- [x] Today dashboard:
  - [x] Due Today
  - [x] Due in 7 days
  - [x] Overdue
  - [x] Blocked by dependencies
- [x] Critical Path Method (CPM):
  - [x] Earliest/latest timings and slack
  - [x] Critical task highlighting
  - [x] Project finish date
  - [x] Cycle detection disables CPM highlight
- [x] SQLite persistence with schema migration + indexes
- [x] Seed sample workspace on first launch (2 projects, 8 tasks)
- [x] JUnit 5 tests for topology/cycle, conflict rule, CPM correctness

## Architecture

```text
com.deadlineflow
├── app
│   ├── DeadlineFlowApp (JavaFX bootstrap)
│   └── AppContext (DI/wiring)
├── presentation
│   ├── view (MainView + dialogs)
│   ├── viewmodel (MainViewModel)
│   └── components (GanttChartView custom control)
├── application
│   └── services
│       ├── SchedulerEngine
│       ├── ConflictService
│       ├── RiskService
│       ├── DependencyGraphService
│       └── CriticalPathService
├── domain
│   ├── model (Project, Task, Dependency, enums, Conflict)
│   └── exceptions
└── data
    ├── repository (interfaces)
    └── sqlite (SQLite DB, migration, seed, repositories, workspace store)
```

Layering:

- Presentation: JavaFX UI and view model state handling.
- Application: pure scheduling/business algorithms (no JavaFX dependency).
- Domain: immutable entities/value-like objects.
- Data: SQLite persistence and observable repository-backed state.

## How to Run

Prerequisites:

- Java 21+

Commands:

```bash
./gradlew run
```

Run tests:

```bash
./gradlew test
```

The SQLite workspace is stored at:

```text
~/.deadlineflow/workspace.db
```

## Screenshots

Add screenshots here:

- `docs/screenshot-main.png`
- `docs/screenshot-critical-path.png`
- `docs/screenshot-conflicts.png`
