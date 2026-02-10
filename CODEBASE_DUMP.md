# DeadlineFlow Codebase Dump

```text
/Users/zuotianhao/Desktop/DeadlineFlow
├── .gitignore
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew
├── gradlew.bat
└── src
    src/main/java/com/deadlineflow/app/AppContext.java
    src/main/java/com/deadlineflow/app/DeadlineFlowApp.java
    src/main/java/com/deadlineflow/application/services/ConflictService.java
    src/main/java/com/deadlineflow/application/services/CriticalPathResult.java
    src/main/java/com/deadlineflow/application/services/CriticalPathService.java
    src/main/java/com/deadlineflow/application/services/DependencyGraphService.java
    src/main/java/com/deadlineflow/application/services/RiskService.java
    src/main/java/com/deadlineflow/application/services/SchedulerEngine.java
    src/main/java/com/deadlineflow/data/repository/DependencyRepository.java
    src/main/java/com/deadlineflow/data/repository/ProjectRepository.java
    src/main/java/com/deadlineflow/data/repository/TaskRepository.java
    src/main/java/com/deadlineflow/data/repository/WorkspaceRepository.java
    src/main/java/com/deadlineflow/data/sqlite/SampleDataSeeder.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteDatabase.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteDependencyRepository.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteMigration.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteProjectRepository.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteTaskRepository.java
    src/main/java/com/deadlineflow/data/sqlite/SqliteWorkspaceStore.java
    src/main/java/com/deadlineflow/domain/exceptions/CycleDetectedException.java
    src/main/java/com/deadlineflow/domain/exceptions/ValidationException.java
    src/main/java/com/deadlineflow/domain/model/Conflict.java
    src/main/java/com/deadlineflow/domain/model/Dependency.java
    src/main/java/com/deadlineflow/domain/model/DependencyType.java
    src/main/java/com/deadlineflow/domain/model/Project.java
    src/main/java/com/deadlineflow/domain/model/RiskLevel.java
    src/main/java/com/deadlineflow/domain/model/Task.java
    src/main/java/com/deadlineflow/domain/model/TaskStatus.java
    src/main/java/com/deadlineflow/domain/model/TimeScale.java
    src/main/java/com/deadlineflow/presentation/components/GanttChartView.java
    src/main/java/com/deadlineflow/presentation/view/MainView.java
    src/main/java/com/deadlineflow/presentation/view/ProjectDialog.java
    src/main/java/com/deadlineflow/presentation/view/TaskDialog.java
    src/main/java/com/deadlineflow/presentation/viewmodel/MainViewModel.java
    src/main/resources/com/deadlineflow/presentation/styles/app.css
    src/test/java/com/deadlineflow/application/services/ConflictServiceTest.java
    src/test/java/com/deadlineflow/application/services/CriticalPathServiceTest.java
    src/test/java/com/deadlineflow/application/services/DependencyGraphServiceTest.java
```

## /Users/zuotianhao/Desktop/DeadlineFlow/.gitignore
```text
.gradle/
build/
out/
.DS_Store
*.iml

# IntelliJ
.idea/

# macOS
*.swp

```

## /Users/zuotianhao/Desktop/DeadlineFlow/README.md
```markdown
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

```

## /Users/zuotianhao/Desktop/DeadlineFlow/build.gradle.kts
```kotlin
plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.deadlineflow"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.13")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.deadlineflow.app.DeadlineFlowApp")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/settings.gradle.kts
```kotlin
rootProject.name = "deadlineflow"

```

## /Users/zuotianhao/Desktop/DeadlineFlow/gradle/wrapper/gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

```

## /Users/zuotianhao/Desktop/DeadlineFlow/gradlew
```bash
#!/bin/sh

#
# Copyright © 2015 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

##############################################################################
#
#   Gradle start up script for POSIX generated by Gradle.
#
#   Important for running:
#
#   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
#       noncompliant, but you have some other compliant shell such as ksh or
#       bash, then to run this script, type that shell name before the whole
#       command line, like:
#
#           ksh Gradle
#
#       Busybox and similar reduced shells will NOT work, because this script
#       requires all of these POSIX shell features:
#         * functions;
#         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
#           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
#         * compound commands having a testable exit status, especially «case»;
#         * various built-in commands including «command», «set», and «ulimit».
#
#   Important for patching:
#
#   (2) This script targets any POSIX shell, so it avoids extensions provided
#       by Bash, Ksh, etc; in particular arrays are avoided.
#
#       The "traditional" practice of packing multiple parameters into a
#       space-separated string is a well documented source of bugs and security
#       problems, so this is (mostly) avoided, by progressively accumulating
#       options in "$@", and eventually passing that to Java.
#
#       Where the inherited environment variables (DEFAULT_JVM_OPTS, JAVA_OPTS,
#       and GRADLE_OPTS) rely on word-splitting, this is performed explicitly;
#       see the in-line comments for details.
#
#       There are tweaks for specific operating systems such as AIX, CygWin,
#       Darwin, MinGW, and NonStop.
#
#   (3) This script is generated from the Groovy template
#       https://github.com/gradle/gradle/blob/HEAD/platforms/jvm/plugins-application/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#       within the Gradle project.
#
#       You can find Gradle at https://github.com/gradle/gradle/.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac



# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...appname settings
#   * --module-path (only if needed)
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables.

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # don't mess with options #(
              /?*)  t=${arg#/} t=/${t%%/*}              # looks like a POSIX filepath
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of
        # args, so each arg winds up back in the position where it started, but
        # possibly modified.
        #
        # NB: a `for` loop captures its iteration list before it begins, so
        # changing the positional parameters here affects neither the number of
        # iterations, nor the values presented in `arg`.
        shift                   # remove old arg
        set -- "$@" "$arg"      # push replacement arg
    done
fi


# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and optsEnvironmentVar are not allowed to contain shell fragments,
#     and any embedded shellness will be escaped.
#   * For example: A user cannot expect ${Hostname} to be expanded, as it is an environment variable and will be
#     treated as '${Hostname}' itself on the command line.

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Use "xargs" to parse quoted args.
#
# With -n1 it outputs one arg per line, with the quotes and backslashes removed.
#
# In Bash we could simply go:
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# but POSIX shell has neither arrays nor command substitution, so instead we
# post-process each arg (as a line of input to sed) to backslash-escape any
# character that might be a shell metacharacter, then use eval to reverse
# that process (while maintaining the separation between arguments), and wrap
# the whole thing up as a single "set" statement.
#
# This will of course break if any of these variables contains a newline or
# an unmatched quote.
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"

```

## /Users/zuotianhao/Desktop/DeadlineFlow/gradlew.bat
```bat
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line



@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/app/AppContext.java
```java
package com.deadlineflow.app;

import com.deadlineflow.application.services.ConflictService;
import com.deadlineflow.application.services.CriticalPathService;
import com.deadlineflow.application.services.DependencyGraphService;
import com.deadlineflow.application.services.RiskService;
import com.deadlineflow.application.services.SchedulerEngine;
import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.data.sqlite.SampleDataSeeder;
import com.deadlineflow.data.sqlite.SqliteDatabase;
import com.deadlineflow.data.sqlite.SqliteDependencyRepository;
import com.deadlineflow.data.sqlite.SqliteMigration;
import com.deadlineflow.data.sqlite.SqliteProjectRepository;
import com.deadlineflow.data.sqlite.SqliteTaskRepository;
import com.deadlineflow.data.sqlite.SqliteWorkspaceStore;
import com.deadlineflow.presentation.viewmodel.MainViewModel;

public class AppContext {
    private final SqliteWorkspaceStore workspaceStore;

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;

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

        SchedulerEngine schedulerEngine = new SchedulerEngine();
        ConflictService conflictService = new ConflictService();
        RiskService riskService = new RiskService();
        DependencyGraphService dependencyGraphService = new DependencyGraphService();
        CriticalPathService criticalPathService = new CriticalPathService(dependencyGraphService);

        mainViewModel = new MainViewModel(
                projectRepository,
                taskRepository,
                dependencyRepository,
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
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/app/DeadlineFlowApp.java
```java
package com.deadlineflow.app;

import com.deadlineflow.presentation.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DeadlineFlowApp extends Application {
    @Override
    public void start(Stage stage) {
        AppContext appContext = new AppContext();
        MainView mainView = new MainView(appContext.mainViewModel());

        Scene scene = new Scene(mainView, 1520, 900);
        scene.getStylesheets().add(getClass().getResource("/com/deadlineflow/presentation/styles/app.css").toExternalForm());

        stage.setTitle("DeadlineFlow — Visual Deadline Planner");
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/ConflictService.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Conflict;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictService {
    public static final int FINISH_START_LAG_DAYS = 1;

    public List<Conflict> detectDependencyConflicts(Collection<Task> tasks, Collection<Dependency> dependencies) {
        Map<String, Task> taskById = new HashMap<>();
        for (Task task : tasks) {
            taskById.put(task.id(), task);
        }

        List<Conflict> conflicts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Dependency dependency : dependencies) {
            Task fromTask = taskById.get(dependency.fromTaskId());
            Task toTask = taskById.get(dependency.toTaskId());
            if (fromTask == null || toTask == null) {
                continue;
            }

            if (toTask.startDate().isBefore(fromTask.dueDate().plusDays(FINISH_START_LAG_DAYS))) {
                String message = "Dependency violation: '"
                        + toTask.title()
                        + "' starts "
                        + formatter.format(toTask.startDate())
                        + ", must start on or after "
                        + formatter.format(fromTask.dueDate().plusDays(FINISH_START_LAG_DAYS))
                        + " because it depends on '"
                        + fromTask.title()
                        + "'.";
                conflicts.add(new Conflict(dependency.id(), dependency.fromTaskId(), dependency.toTaskId(), message));
            }
        }
        return conflicts;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/CriticalPathResult.java
```java
package com.deadlineflow.application.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class CriticalPathResult {
    private final boolean hasCycle;
    private final Set<String> cycleTaskIds;
    private final LocalDate projectFinishDate;
    private final Map<String, Integer> earliestStartDays;
    private final Map<String, Integer> earliestFinishDays;
    private final Map<String, Integer> latestStartDays;
    private final Map<String, Integer> latestFinishDays;
    private final Map<String, Integer> slackDays;
    private final Set<String> criticalTaskIds;

    public CriticalPathResult(
            boolean hasCycle,
            Set<String> cycleTaskIds,
            LocalDate projectFinishDate,
            Map<String, Integer> earliestStartDays,
            Map<String, Integer> earliestFinishDays,
            Map<String, Integer> latestStartDays,
            Map<String, Integer> latestFinishDays,
            Map<String, Integer> slackDays,
            Set<String> criticalTaskIds
    ) {
        this.hasCycle = hasCycle;
        this.cycleTaskIds = Collections.unmodifiableSet(cycleTaskIds);
        this.projectFinishDate = projectFinishDate;
        this.earliestStartDays = Collections.unmodifiableMap(earliestStartDays);
        this.earliestFinishDays = Collections.unmodifiableMap(earliestFinishDays);
        this.latestStartDays = Collections.unmodifiableMap(latestStartDays);
        this.latestFinishDays = Collections.unmodifiableMap(latestFinishDays);
        this.slackDays = Collections.unmodifiableMap(slackDays);
        this.criticalTaskIds = Collections.unmodifiableSet(criticalTaskIds);
    }

    public static CriticalPathResult empty() {
        return new CriticalPathResult(false, Set.of(), null, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    }

    public static CriticalPathResult cycle(Set<String> cycleTaskIds) {
        return new CriticalPathResult(true, cycleTaskIds, null, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    }

    public boolean hasCycle() {
        return hasCycle;
    }

    public Set<String> cycleTaskIds() {
        return cycleTaskIds;
    }

    public LocalDate projectFinishDate() {
        return projectFinishDate;
    }

    public Map<String, Integer> earliestStartDays() {
        return earliestStartDays;
    }

    public Map<String, Integer> earliestFinishDays() {
        return earliestFinishDays;
    }

    public Map<String, Integer> latestStartDays() {
        return latestStartDays;
    }

    public Map<String, Integer> latestFinishDays() {
        return latestFinishDays;
    }

    public Map<String, Integer> slackDays() {
        return slackDays;
    }

    public Set<String> criticalTaskIds() {
        return criticalTaskIds;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/CriticalPathService.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CriticalPathService {
    private final DependencyGraphService dependencyGraphService;

    public CriticalPathService(DependencyGraphService dependencyGraphService) {
        this.dependencyGraphService = dependencyGraphService;
    }

    public CriticalPathResult compute(Collection<Task> tasks, Collection<Dependency> dependencies) {
        if (tasks.isEmpty()) {
            return CriticalPathResult.empty();
        }

        DependencyGraphService.TopologyResult topologyResult = dependencyGraphService.topologicalSort(tasks, dependencies);
        if (topologyResult.hasCycle()) {
            return CriticalPathResult.cycle(topologyResult.cycleTaskIds());
        }

        Map<String, Task> taskById = new HashMap<>();
        for (Task task : tasks) {
            taskById.put(task.id(), task);
        }

        LocalDate projectStart = tasks.stream()
                .map(Task::startDate)
                .min(LocalDate::compareTo)
                .orElseThrow();

        Map<String, List<String>> predecessors = new HashMap<>();
        Map<String, List<String>> successors = new HashMap<>();
        for (Task task : tasks) {
            predecessors.put(task.id(), new ArrayList<>());
            successors.put(task.id(), new ArrayList<>());
        }

        for (Dependency dependency : dependencies) {
            if (!taskById.containsKey(dependency.fromTaskId()) || !taskById.containsKey(dependency.toTaskId())) {
                continue;
            }
            predecessors.get(dependency.toTaskId()).add(dependency.fromTaskId());
            successors.get(dependency.fromTaskId()).add(dependency.toTaskId());
        }

        Map<String, Integer> earliestStart = new HashMap<>();
        Map<String, Integer> earliestFinish = new HashMap<>();

        for (String taskId : topologyResult.orderedTaskIds()) {
            Task task = taskById.get(taskId);
            int duration = Math.toIntExact(task.durationDaysInclusive());
            int baseStart = Math.toIntExact(ChronoUnit.DAYS.between(projectStart, task.startDate()));

            int earliest = baseStart;
            for (String predecessorId : predecessors.get(taskId)) {
                earliest = Math.max(earliest, earliestFinish.get(predecessorId) + 1);
            }

            int finish = earliest + duration - 1;
            earliestStart.put(taskId, earliest);
            earliestFinish.put(taskId, finish);
        }

        int projectFinishOffset = earliestFinish.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        Map<String, Integer> latestStart = new HashMap<>();
        Map<String, Integer> latestFinish = new HashMap<>();
        List<String> reverseOrder = new ArrayList<>(topologyResult.orderedTaskIds());
        java.util.Collections.reverse(reverseOrder);

        for (String taskId : reverseOrder) {
            Task task = taskById.get(taskId);
            int duration = Math.toIntExact(task.durationDaysInclusive());

            int lf;
            List<String> next = successors.get(taskId);
            if (next.isEmpty()) {
                lf = projectFinishOffset;
            } else {
                lf = next.stream()
                        .mapToInt(successorId -> latestStart.get(successorId) - 1)
                        .min()
                        .orElse(projectFinishOffset);
            }

            int ls = lf - duration + 1;
            latestFinish.put(taskId, lf);
            latestStart.put(taskId, ls);
        }

        Map<String, Integer> slackDays = new HashMap<>();
        Set<String> criticalTaskIds = new HashSet<>();
        for (String taskId : topologyResult.orderedTaskIds()) {
            int slack = latestStart.get(taskId) - earliestStart.get(taskId);
            slackDays.put(taskId, slack);
            if (slack == 0) {
                criticalTaskIds.add(taskId);
            }
        }

        LocalDate finishDate = projectStart.plusDays(projectFinishOffset);

        return new CriticalPathResult(
                false,
                Set.of(),
                finishDate,
                earliestStart,
                earliestFinish,
                latestStart,
                latestFinish,
                slackDays,
                criticalTaskIds
        );
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/DependencyGraphService.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyGraphService {

    public TopologyResult topologicalSort(Collection<Task> tasks, Collection<Dependency> dependencies) {
        Map<String, Task> taskById = new HashMap<>();
        for (Task task : tasks) {
            taskById.put(task.id(), task);
        }

        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (Task task : tasks) {
            adjacency.put(task.id(), new HashSet<>());
            indegree.put(task.id(), 0);
        }

        for (Dependency dependency : dependencies) {
            if (!taskById.containsKey(dependency.fromTaskId()) || !taskById.containsKey(dependency.toTaskId())) {
                continue;
            }
            Set<String> neighbors = adjacency.get(dependency.fromTaskId());
            if (neighbors.add(dependency.toTaskId())) {
                indegree.put(dependency.toTaskId(), indegree.get(dependency.toTaskId()) + 1);
            }
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            ordered.add(current);

            for (String next : adjacency.getOrDefault(current, Set.of())) {
                int nextIndegree = indegree.get(next) - 1;
                indegree.put(next, nextIndegree);
                if (nextIndegree == 0) {
                    queue.add(next);
                }
            }
        }

        boolean hasCycle = ordered.size() != taskById.size();
        Set<String> cycleNodes = new HashSet<>();
        if (hasCycle) {
            for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
                if (entry.getValue() > 0) {
                    cycleNodes.add(entry.getKey());
                }
            }
        }
        return new TopologyResult(ordered, hasCycle, cycleNodes);
    }

    public boolean createsCycle(Collection<Task> tasks, Collection<Dependency> dependencies, Dependency candidate) {
        List<Dependency> all = new ArrayList<>(dependencies);
        all.add(candidate);
        return topologicalSort(tasks, all).hasCycle();
    }

    public record TopologyResult(List<String> orderedTaskIds, boolean hasCycle, Set<String> cycleTaskIds) {
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/RiskService.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;

import java.time.LocalDate;

public class RiskService {

    public RiskLevel evaluate(Task task, LocalDate today) {
        if (task.status() == TaskStatus.DONE) {
            return RiskLevel.NONE;
        }
        if (task.dueDate().isBefore(today)) {
            return RiskLevel.OVERDUE;
        }
        if (!task.dueDate().isAfter(today.plusDays(1))) {
            return RiskLevel.DUE_SOON;
        }
        return RiskLevel.NONE;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/application/services/SchedulerEngine.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;

public class SchedulerEngine {

    public Task shiftTask(Task task, long deltaDays) {
        LocalDate newStart = task.startDate().plusDays(deltaDays);
        LocalDate newDue = task.dueDate().plusDays(deltaDays);
        return task.withDates(newStart, newDue);
    }

    public Task resizeTaskStart(Task task, LocalDate newStart) {
        LocalDate adjustedStart = newStart;
        if (adjustedStart.isAfter(task.dueDate())) {
            adjustedStart = task.dueDate();
        }
        return task.withDates(adjustedStart, task.dueDate());
    }

    public Task resizeTaskDue(Task task, LocalDate newDue) {
        LocalDate adjustedDue = newDue;
        if (adjustedDue.isBefore(task.startDate())) {
            adjustedDue = task.startDate();
        }
        return task.withDates(task.startDate(), adjustedDue);
    }

    public Task validateTaskDates(Task task) {
        if (task.dueDate().isBefore(task.startDate())) {
            throw new ValidationException("Due date cannot be before start date");
        }
        return task;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/repository/DependencyRepository.java
```java
package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Dependency;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface DependencyRepository {
    ObservableList<Dependency> getAll();

    Optional<Dependency> findById(String dependencyId);

    Dependency save(Dependency dependency);

    void delete(String dependencyId);
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/repository/ProjectRepository.java
```java
package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Project;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface ProjectRepository {
    ObservableList<Project> getAll();

    Optional<Project> findById(long id);

    Project save(Project project);

    void delete(long projectId);
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/repository/TaskRepository.java
```java
package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Task;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface TaskRepository {
    ObservableList<Task> getAll();

    Optional<Task> findById(String taskId);

    Task save(Task task);

    void delete(String taskId);
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/repository/WorkspaceRepository.java
```java
package com.deadlineflow.data.repository;

public interface WorkspaceRepository {
    void initialize();

    void reload();
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SampleDataSeeder.java
```java
package com.deadlineflow.data.sqlite;

import com.deadlineflow.domain.model.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class SampleDataSeeder {

    public void seedIfEmpty(SqliteDatabase database) {
        try (Connection connection = database.openConnection()) {
            if (!isEmpty(connection)) {
                return;
            }

            long projectA = insertProject(connection, "Launch Alpha", "#2B6EF2", 1);
            long projectB = insertProject(connection, "Client Revamp", "#1AA179", 2);

            LocalDate today = LocalDate.now();

            String t1 = "11111111-1111-4111-8111-111111111111";
            String t2 = "22222222-2222-4222-8222-222222222222";
            String t3 = "33333333-3333-4333-8333-333333333333";
            String t4 = "44444444-4444-4444-8444-444444444444";

            String t5 = "55555555-5555-4555-8555-555555555555";
            String t6 = "66666666-6666-4666-8666-666666666666";
            String t7 = "77777777-7777-4777-8777-777777777777";
            String t8 = "88888888-8888-4888-8888-888888888888";

            insertTask(connection, t1, projectA, "Define scope", today.minusDays(8), today.minusDays(5), 100, TaskStatus.DONE);
            insertTask(connection, t2, projectA, "Build prototype", today.minusDays(4), today.plusDays(1), 60, TaskStatus.IN_PROGRESS);
            insertTask(connection, t3, projectA, "QA signoff", today.plusDays(1), today.plusDays(4), 0, TaskStatus.NOT_STARTED);
            insertTask(connection, t4, projectA, "Launch prep", today.plusDays(5), today.plusDays(7), 0, TaskStatus.BLOCKED);

            insertTask(connection, t5, projectB, "Kickoff", today.minusDays(6), today.minusDays(4), 100, TaskStatus.DONE);
            insertTask(connection, t6, projectB, "Design system", today.minusDays(3), today.plusDays(2), 35, TaskStatus.IN_PROGRESS);
            insertTask(connection, t7, projectB, "Implement screens", today.plusDays(3), today.plusDays(8), 0, TaskStatus.NOT_STARTED);
            insertTask(connection, t8, projectB, "Client review", today.plusDays(9), today.plusDays(10), 0, TaskStatus.BLOCKED);

            insertDependency(connection, "a1111111-1111-4111-8111-111111111111", t1, t2);
            insertDependency(connection, "a2222222-2222-4222-8222-222222222222", t2, t3); // intentionally violated on seed data
            insertDependency(connection, "a3333333-3333-4333-8333-333333333333", t3, t4);

            insertDependency(connection, "b1111111-1111-4111-8111-111111111111", t5, t6);
            insertDependency(connection, "b2222222-2222-4222-8222-222222222222", t6, t7);
            insertDependency(connection, "b3333333-3333-4333-8333-333333333333", t7, t8);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed seeding sample workspace data", e);
        }
    }

    private boolean isEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM projects")) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }

    private long insertProject(Connection connection, String name, String color, int priority) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO projects(name, color, priority) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.setInt(3, priority);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert sample project " + name);
    }

    private void insertTask(
            Connection connection,
            String id,
            long projectId,
            String title,
            LocalDate start,
            LocalDate due,
            int progress,
            TaskStatus status
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tasks(id, project_id, title, start_date, due_date, progress, status) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
            statement.setString(1, id);
            statement.setLong(2, projectId);
            statement.setString(3, title);
            statement.setString(4, start.toString());
            statement.setString(5, due.toString());
            statement.setInt(6, progress);
            statement.setString(7, status.name());
            statement.executeUpdate();
        }
    }

    private void insertDependency(Connection connection, String id, String fromTaskId, String toTaskId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dependencies(id, from_task_id, to_task_id, type) VALUES (?, ?, ?, ?)"
        )) {
            statement.setString(1, id);
            statement.setString(2, fromTaskId);
            statement.setString(3, toTaskId);
            statement.setString(4, "FINISH_START");
            statement.executeUpdate();
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteDatabase.java
```java
package com.deadlineflow.data.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteDatabase {
    private final Path dbPath;
    private final String jdbcUrl;

    public SqliteDatabase() {
        this(Paths.get(System.getProperty("user.home"), ".deadlineflow", "workspace.db"));
    }

    public SqliteDatabase(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    public void ensureStorageDirectory() {
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize workspace directory", e);
        }
    }

    public Connection openConnection() {
        ensureStorageDirectory();
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open SQLite connection", e);
        }
    }

    public Path dbPath() {
        return dbPath;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteDependencyRepository.java
```java
package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.domain.model.Dependency;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteDependencyRepository implements DependencyRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteDependencyRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Dependency> getAll() {
        return workspaceStore.dependencies();
    }

    @Override
    public Optional<Dependency> findById(String dependencyId) {
        return workspaceStore.dependencies().stream()
                .filter(dependency -> dependency.id().equals(dependencyId))
                .findFirst();
    }

    @Override
    public Dependency save(Dependency dependency) {
        if (findById(dependency.id()).isPresent()) {
            return update(dependency);
        }
        return insert(dependency);
    }

    @Override
    public void delete(String dependencyId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM dependencies WHERE id = ?")) {
            statement.setString(1, dependencyId);
            statement.executeUpdate();
            workspaceStore.reload();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting dependency", e);
        }
    }

    private Dependency insert(Dependency dependency) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO dependencies(id, from_task_id, to_task_id, type) VALUES (?, ?, ?, ?)"
             )) {
            statement.setString(1, dependency.id());
            statement.setString(2, dependency.fromTaskId());
            statement.setString(3, dependency.toTaskId());
            statement.setString(4, dependency.type().name());
            statement.executeUpdate();
            workspaceStore.reload();
            return dependency;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating dependency", e);
        }
    }

    private Dependency update(Dependency dependency) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dependencies SET from_task_id = ?, to_task_id = ?, type = ? WHERE id = ?"
             )) {
            statement.setString(1, dependency.fromTaskId());
            statement.setString(2, dependency.toTaskId());
            statement.setString(3, dependency.type().name());
            statement.setString(4, dependency.id());
            statement.executeUpdate();
            workspaceStore.reload();
            return dependency;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating dependency", e);
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteMigration.java
```java
package com.deadlineflow.data.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteMigration {

    public void migrate(SqliteDatabase database) {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        priority INTEGER NOT NULL CHECK(priority >= 1 AND priority <= 5)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT PRIMARY KEY,
                        project_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        progress INTEGER NOT NULL CHECK(progress >= 0 AND progress <= 100),
                        status TEXT NOT NULL,
                        FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dependencies (
                        id TEXT PRIMARY KEY,
                        from_task_id TEXT NOT NULL,
                        to_task_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        FOREIGN KEY(from_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(to_task_id) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dependencies_from ON dependencies(from_task_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dependencies_to ON dependencies(to_task_id)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to migrate SQLite schema", e);
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteProjectRepository.java
```java
package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.domain.model.Project;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SqliteProjectRepository implements ProjectRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteProjectRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Project> getAll() {
        return workspaceStore.projects();
    }

    @Override
    public Optional<Project> findById(long id) {
        return workspaceStore.projects().stream().filter(p -> p.id() == id).findFirst();
    }

    @Override
    public Project save(Project project) {
        if (project.id() <= 0) {
            return insert(project);
        }
        return update(project);
    }

    @Override
    public void delete(long projectId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM projects WHERE id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
            workspaceStore.reload();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting project", e);
        }
    }

    private Project insert(Project project) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO projects(name, color, priority) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, project.name());
            statement.setString(2, project.color());
            statement.setInt(3, project.priority());
            statement.executeUpdate();

            long generatedId;
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("No generated key for inserted project");
                }
                generatedId = generatedKeys.getLong(1);
            }

            workspaceStore.reload();
            return project.withId(generatedId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating project", e);
        }
    }

    private Project update(Project project) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE projects SET name = ?, color = ?, priority = ? WHERE id = ?"
             )) {
            statement.setString(1, project.name());
            statement.setString(2, project.color());
            statement.setInt(3, project.priority());
            statement.setLong(4, project.id());
            statement.executeUpdate();
            workspaceStore.reload();
            return project;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating project", e);
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteTaskRepository.java
```java
package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.domain.model.Task;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteTaskRepository implements TaskRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteTaskRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Task> getAll() {
        return workspaceStore.tasks();
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return workspaceStore.tasks().stream().filter(task -> task.id().equals(taskId)).findFirst();
    }

    @Override
    public Task save(Task task) {
        if (findById(task.id()).isPresent()) {
            return update(task);
        }
        return insert(task);
    }

    @Override
    public void delete(String taskId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            statement.setString(1, taskId);
            statement.executeUpdate();
            workspaceStore.reload();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting task", e);
        }
    }

    private Task insert(Task task) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tasks(id, project_id, title, start_date, due_date, progress, status) VALUES (?, ?, ?, ?, ?, ?, ?)"
             )) {
            statement.setString(1, task.id());
            statement.setLong(2, task.projectId());
            statement.setString(3, task.title());
            statement.setString(4, task.startDate().toString());
            statement.setString(5, task.dueDate().toString());
            statement.setInt(6, task.progress());
            statement.setString(7, task.status().name());
            statement.executeUpdate();
            workspaceStore.reload();
            return task;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating task", e);
        }
    }

    private Task update(Task task) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tasks SET project_id = ?, title = ?, start_date = ?, due_date = ?, progress = ?, status = ? WHERE id = ?"
             )) {
            statement.setLong(1, task.projectId());
            statement.setString(2, task.title());
            statement.setString(3, task.startDate().toString());
            statement.setString(4, task.dueDate().toString());
            statement.setInt(5, task.progress());
            statement.setString(6, task.status().name());
            statement.setString(7, task.id());
            statement.executeUpdate();
            workspaceStore.reload();
            return task;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating task", e);
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/data/sqlite/SqliteWorkspaceStore.java
```java
package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.WorkspaceRepository;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteWorkspaceStore implements WorkspaceRepository {
    private final SqliteDatabase database;
    private final SqliteMigration migration;
    private final SampleDataSeeder sampleDataSeeder;

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Dependency> dependencies = FXCollections.observableArrayList();

    public SqliteWorkspaceStore(SqliteDatabase database, SqliteMigration migration, SampleDataSeeder sampleDataSeeder) {
        this.database = database;
        this.migration = migration;
        this.sampleDataSeeder = sampleDataSeeder;
    }

    @Override
    public void initialize() {
        migration.migrate(database);
        sampleDataSeeder.seedIfEmpty(database);
        reload();
    }

    @Override
    public void reload() {
        projects.setAll(loadProjects());
        tasks.setAll(loadTasks());
        dependencies.setAll(loadDependencies());
    }

    public ObservableList<Project> projects() {
        return projects;
    }

    public ObservableList<Task> tasks() {
        return tasks;
    }

    public ObservableList<Dependency> dependencies() {
        return dependencies;
    }

    private ObservableList<Project> loadProjects() {
        ObservableList<Project> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, color, priority FROM projects ORDER BY priority ASC, name ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Project(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("color"),
                        rs.getInt("priority")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading projects", e);
        }
        return result;
    }

    private ObservableList<Task> loadTasks() {
        ObservableList<Task> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, project_id, title, start_date, due_date, progress, status FROM tasks ORDER BY start_date ASC, due_date ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Task(
                        rs.getString("id"),
                        rs.getLong("project_id"),
                        rs.getString("title"),
                        java.time.LocalDate.parse(rs.getString("start_date")),
                        java.time.LocalDate.parse(rs.getString("due_date")),
                        rs.getInt("progress"),
                        TaskStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading tasks", e);
        }
        return result;
    }

    private ObservableList<Dependency> loadDependencies() {
        ObservableList<Dependency> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, from_task_id, to_task_id, type FROM dependencies ORDER BY id ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Dependency(
                        rs.getString("id"),
                        rs.getString("from_task_id"),
                        rs.getString("to_task_id"),
                        DependencyType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading dependencies", e);
        }
        return result;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/exceptions/CycleDetectedException.java
```java
package com.deadlineflow.domain.exceptions;

public class CycleDetectedException extends RuntimeException {
    public CycleDetectedException(String message) {
        super(message);
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/exceptions/ValidationException.java
```java
package com.deadlineflow.domain.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/Conflict.java
```java
package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Conflict {
    private final String dependencyId;
    private final String fromTaskId;
    private final String toTaskId;
    private final String message;

    public Conflict(String dependencyId, String fromTaskId, String toTaskId, String message) {
        this.dependencyId = Objects.requireNonNull(dependencyId, "dependencyId");
        this.fromTaskId = Objects.requireNonNull(fromTaskId, "fromTaskId");
        this.toTaskId = Objects.requireNonNull(toTaskId, "toTaskId");
        this.message = Objects.requireNonNull(message, "message");
    }

    public String dependencyId() {
        return dependencyId;
    }

    public String fromTaskId() {
        return fromTaskId;
    }

    public String toTaskId() {
        return toTaskId;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/Dependency.java
```java
package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Dependency {
    private final String id;
    private final String fromTaskId;
    private final String toTaskId;
    private final DependencyType type;

    public Dependency(String id, String fromTaskId, String toTaskId, DependencyType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Dependency id is required");
        }
        if (fromTaskId == null || fromTaskId.isBlank()) {
            throw new IllegalArgumentException("Dependency fromTaskId is required");
        }
        if (toTaskId == null || toTaskId.isBlank()) {
            throw new IllegalArgumentException("Dependency toTaskId is required");
        }
        if (fromTaskId.equals(toTaskId)) {
            throw new IllegalArgumentException("Dependency cannot target the same task");
        }
        if (type == null) {
            throw new IllegalArgumentException("Dependency type is required");
        }
        this.id = id;
        this.fromTaskId = fromTaskId;
        this.toTaskId = toTaskId;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String fromTaskId() {
        return fromTaskId;
    }

    public String toTaskId() {
        return toTaskId;
    }

    public DependencyType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dependency that)) {
            return false;
        }
        return id.equals(that.id)
                && fromTaskId.equals(that.fromTaskId)
                && toTaskId.equals(that.toTaskId)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromTaskId, toTaskId, type);
    }

    @Override
    public String toString() {
        return fromTaskId + " -> " + toTaskId;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/DependencyType.java
```java
package com.deadlineflow.domain.model;

public enum DependencyType {
    FINISH_START
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/Project.java
```java
package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Project {
    private final long id;
    private final String name;
    private final String color;
    private final int priority;

    public Project(long id, String name, String color, int priority) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Project color is required");
        }
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Project priority must be between 1 and 5");
        }
        this.id = id;
        this.name = name.trim();
        this.color = color.trim();
        this.priority = priority;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String color() {
        return color;
    }

    public int priority() {
        return priority;
    }

    public Project withId(long newId) {
        return new Project(newId, name, color, priority);
    }

    public Project withName(String newName) {
        return new Project(id, newName, color, priority);
    }

    public Project withColor(String newColor) {
        return new Project(id, name, newColor, priority);
    }

    public Project withPriority(int newPriority) {
        return new Project(id, name, color, newPriority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project project)) {
            return false;
        }
        return id == project.id
                && priority == project.priority
                && name.equals(project.name)
                && color.equals(project.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color, priority);
    }

    @Override
    public String toString() {
        return name;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/RiskLevel.java
```java
package com.deadlineflow.domain.model;

public enum RiskLevel {
    NONE,
    DUE_SOON,
    OVERDUE
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/Task.java
```java
package com.deadlineflow.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Task {
    private final String id;
    private final long projectId;
    private final String title;
    private final LocalDate startDate;
    private final LocalDate dueDate;
    private final int progress;
    private final TaskStatus status;

    public Task(
            String id,
            long projectId,
            String title,
            LocalDate startDate,
            LocalDate dueDate,
            int progress,
            TaskStatus status
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task id is required");
        }
        if (projectId <= 0) {
            throw new IllegalArgumentException("Task projectId must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }
        if (startDate == null || dueDate == null) {
            throw new IllegalArgumentException("Task dates are required");
        }
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Task dueDate cannot be before startDate");
        }
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Task progress must be between 0 and 100");
        }
        if (status == null) {
            throw new IllegalArgumentException("Task status is required");
        }
        this.id = id;
        this.projectId = projectId;
        this.title = title.trim();
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.progress = progress;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public long projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public int progress() {
        return progress;
    }

    public TaskStatus status() {
        return status;
    }

    public long durationDaysInclusive() {
        return ChronoUnit.DAYS.between(startDate, dueDate) + 1;
    }

    public Task withTitle(String newTitle) {
        return new Task(id, projectId, newTitle, startDate, dueDate, progress, status);
    }

    public Task withDates(LocalDate newStartDate, LocalDate newDueDate) {
        return new Task(id, projectId, title, newStartDate, newDueDate, progress, status);
    }

    public Task withProgress(int newProgress) {
        return new Task(id, projectId, title, startDate, dueDate, newProgress, status);
    }

    public Task withStatus(TaskStatus newStatus) {
        return new Task(id, projectId, title, startDate, dueDate, progress, newStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Task task)) {
            return false;
        }
        return projectId == task.projectId
                && progress == task.progress
                && id.equals(task.id)
                && title.equals(task.title)
                && startDate.equals(task.startDate)
                && dueDate.equals(task.dueDate)
                && status == task.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId, title, startDate, dueDate, progress, status);
    }

    @Override
    public String toString() {
        return title;
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/TaskStatus.java
```java
package com.deadlineflow.domain.model;

public enum TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    DONE,
    BLOCKED
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/domain/model/TimeScale.java
```java
package com.deadlineflow.domain.model;

public enum TimeScale {
    DAY,
    WEEK,
    MONTH
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/presentation/components/GanttChartView.java
```java
package com.deadlineflow.presentation.components;

import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TimeScale;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    private static final double HEADER_HEIGHT = 38;
    private static final double ROW_HEIGHT = 38;
    private static final double BAR_VERTICAL_PADDING = 7;
    private static final double HANDLE_WIDTH = 7;

    private final Canvas headerCanvas = new Canvas();
    private final Canvas gridCanvas = new Canvas();
    private final Pane barsLayer = new Pane();
    private final StackPane content = new StackPane(gridCanvas, barsLayer);
    private final ScrollPane scrollPane = new ScrollPane(content);

    private final ObjectProperty<TimeScale> scale = new SimpleObjectProperty<>(TimeScale.WEEK);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    private final ObjectProperty<Consumer<Task>> onTaskSelected = new SimpleObjectProperty<>();
    private final ObjectProperty<TaskDateChangeListener> onTaskDateChanged = new SimpleObjectProperty<>();
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

    public GanttChartView() {
        getStyleClass().add("gantt-chart");

        headerCanvas.getStyleClass().add("gantt-header");
        scrollPane.setFitToHeight(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("gantt-scroll");

        barsLayer.setPickOnBounds(false);

        getChildren().addAll(headerCanvas, scrollPane);

        widthProperty().addListener((obs, oldValue, newValue) -> refreshAll());
        heightProperty().addListener((obs, oldValue, newValue) -> refreshAll());
        scale.addListener((obs, oldValue, newValue) -> refreshAll());
        zoom.addListener((obs, oldValue, newValue) -> refreshAll());

        scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> drawHeader());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> drawVisibleBars());
        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> refreshAll());

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

    public void setOnTaskSelected(Consumer<Task> callback) {
        onTaskSelected.set(callback);
    }

    public void setOnTaskDateChanged(TaskDateChangeListener callback) {
        onTaskDateChanged.set(callback);
    }

    public void setTaskColorProvider(Function<Task, String> provider) {
        taskColorProvider.set(provider == null ? task -> "#3A7AFE" : provider);
        drawVisibleBars();
    }

    public void setRiskByTaskId(Map<String, RiskLevel> riskMap) {
        riskByTaskId.clear();
        if (riskMap != null) {
            riskByTaskId.putAll(riskMap);
        }
        drawVisibleBars();
    }

    public void setConflictMessageByTaskId(Map<String, String> conflictMap) {
        conflictMessageByTaskId.clear();
        if (conflictMap != null) {
            conflictMessageByTaskId.putAll(conflictMap);
        }
        drawVisibleBars();
    }

    public void setCriticalTaskIds(Set<String> criticalTasks) {
        criticalTaskIds.clear();
        if (criticalTasks != null) {
            criticalTaskIds.addAll(criticalTasks);
        }
        drawVisibleBars();
    }

    public void setSlackByTaskId(Map<String, Integer> slackMap) {
        slackByTaskId.clear();
        if (slackMap != null) {
            slackByTaskId.putAll(slackMap);
        }
        drawVisibleBars();
    }

    public void setSelectedTaskId(String taskId) {
        selectedTaskId = taskId;
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
        gc.setFill(Color.web("#FAFBFE"));
        gc.fillRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        gc.setStroke(Color.web("#E2E7F1"));
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
                    gc.setStroke(cursor.getDayOfMonth() == 1 ? Color.web("#CBD5E6") : Color.web("#EEF1F7"));
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
                    gc.setStroke(Color.web("#D7DFEC"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate cursor = timelineStart.withDayOfMonth(1);
                while (!cursor.isAfter(timelineEnd)) {
                    double x = xForDate(cursor);
                    gc.setStroke(Color.web("#D7DFEC"));
                    gc.strokeLine(x, 0, x, gridCanvas.getHeight());
                    cursor = cursor.plusMonths(1).withDayOfMonth(1);
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
        gc.setFill(Color.web("#F3F6FD"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#D7DFEC"));
        gc.strokeLine(0, height - 1, width, height - 1);

        double xOffset = horizontalOffset();
        gc.setFont(Font.font("Menlo", FontWeight.SEMI_BOLD, 11));
        gc.setFill(Color.web("#425071"));

        switch (scale.get()) {
            case DAY -> drawDayHeader(gc, width, xOffset);
            case WEEK -> drawWeekHeader(gc, width, xOffset);
            case MONTH -> drawMonthHeader(gc, width, xOffset);
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
                gc.fillText("Week " + cursor.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
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

    private void drawVisibleBars() {
        if (orderedTasks.isEmpty()) {
            barsLayer.getChildren().clear();
            return;
        }

        Bounds viewport = scrollPane.getViewportBounds();
        double viewportHeight = viewport.getHeight();
        double contentHeight = content.getHeight();
        double yOffset = Math.max(0, (contentHeight - viewportHeight) * scrollPane.getVvalue());

        int firstIndex = Math.max(0, (int) Math.floor(yOffset / ROW_HEIGHT) - 1);
        int lastIndex = Math.min(orderedTasks.size() - 1, (int) Math.ceil((yOffset + viewportHeight) / ROW_HEIGHT) + 1);

        List<TaskBar> visibleBars = new ArrayList<>();
        for (int index = firstIndex; index <= lastIndex; index++) {
            visibleBars.add(new TaskBar(orderedTasks.get(index), index));
        }
        barsLayer.getChildren().setAll(visibleBars);
    }

    private double dayWidth() {
        double clampedZoom = Math.max(0.5, Math.min(zoom.get(), 3.0));
        return switch (scale.get()) {
            case DAY -> 30 * clampedZoom;
            case WEEK -> 12 * clampedZoom;
            case MONTH -> 4 * clampedZoom;
        };
    }

    private double xForDate(LocalDate date) {
        return ChronoUnit.DAYS.between(timelineStart, date) * dayWidth();
    }

    private double widthForTask(Task task) {
        long days = ChronoUnit.DAYS.between(task.startDate(), task.dueDate()) + 1;
        return Math.max(dayWidth(), days * dayWidth());
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

            base.setArcWidth(8);
            base.setArcHeight(8);
            accent.setArcWidth(8);
            accent.setArcHeight(8);
            criticalOutline.setArcWidth(8);
            criticalOutline.setArcHeight(8);
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
                double localX = event.getX();
                if (localX <= HANDLE_WIDTH + 2 || localX >= getWidth() - HANDLE_WIDTH - 2) {
                    setCursor(Cursor.H_RESIZE);
                } else {
                    setCursor(Cursor.MOVE);
                }
            });

            setOnMousePressed(event -> {
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
            double width = Math.max(dayWidth(), (ChronoUnit.DAYS.between(startDate, dueDate) + 1) * dayWidth());
            double height = ROW_HEIGHT - (2 * BAR_VERTICAL_PADDING);

            relocate(x, y);
            setPrefSize(width, height);

            base.setWidth(width);
            base.setHeight(height);
            base.setFill(safeColor(taskColorProvider.get().apply(task)));

            RiskLevel risk = riskByTaskId.getOrDefault(task.id(), RiskLevel.NONE);
            Color accentColor = switch (risk) {
                case OVERDUE -> Color.web("#C73636");
                case DUE_SOON -> Color.web("#F0A928");
                default -> Color.TRANSPARENT;
            };
            accent.setWidth(width);
            accent.setHeight(4);
            accent.setFill(accentColor);

            criticalOutline.setVisible(criticalTaskIds.contains(task.id()));
            criticalOutline.setWidth(width);
            criticalOutline.setHeight(height);

            String conflictMessage = conflictMessageByTaskId.get(task.id());
            boolean hasConflict = conflictMessage != null && !conflictMessage.isBlank();

            if (Objects.equals(selectedTaskId, task.id())) {
                base.setStroke(Color.web("#0D1A36"));
                base.setStrokeWidth(2);
            } else if (hasConflict) {
                base.setStroke(Color.web("#C73636"));
                base.setStrokeWidth(2);
            } else {
                base.setStroke(Color.color(0, 0, 0, 0.16));
                base.setStrokeWidth(1);
            }

            leftHandle.setHeight(height);
            rightHandle.setHeight(height);
            leftHandle.setLayoutX(0);
            rightHandle.setLayoutX(width - HANDLE_WIDTH);

            label.setText(task.title() + " (" + task.progress() + "%)");
            label.setLayoutX(8);
            label.setLayoutY(6);
            label.setMaxWidth(Math.max(40, width - 20));

            boolean showBadge = risk != RiskLevel.NONE || hasConflict;
            badge.setVisible(showBadge);
            badge.setLayoutX(Math.max(0, width - 16));
            badge.setLayoutY(2);

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
    }

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE_LEFT,
        RESIZE_RIGHT
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/presentation/view/MainView.java
```java
package com.deadlineflow.presentation.view;

import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Conflict;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import com.deadlineflow.domain.model.TimeScale;
import com.deadlineflow.presentation.components.GanttChartView;
import com.deadlineflow.presentation.viewmodel.MainViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MainView extends BorderPane {
    private final MainViewModel viewModel;
    private final GanttChartView ganttChartView = new GanttChartView();

    private final ListView<Project> projectListView = new ListView<>();
    private final TextField titleField = new TextField();
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker dueDatePicker = new DatePicker();
    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Label progressLabel = new Label("0%");
    private final ComboBox<TaskStatus> statusComboBox = new ComboBox<>();
    private final ListView<Dependency> dependencyListView = new ListView<>();
    private final ListView<Conflict> conflictListView = new ListView<>();
    private final Label slackLabel = new Label("Slack: -");

    private boolean inspectorUpdating;

    public MainView(MainViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("main-root");

        Node topBar = buildTopBar();
        Node leftPanel = buildLeftPanel();
        Node centerPanel = buildCenterPanel();
        Node rightPanel = buildRightPanel();

        setTop(topBar);
        setLeft(leftPanel);
        setCenter(centerPanel);
        setRight(rightPanel);

        setPadding(new Insets(8));

        wireProjectSelection();
        wireTaskInspector();
        wireGantt();
        wireDerivedState();
    }

    private Node buildTopBar() {
        Label bannerLabel = new Label();
        bannerLabel.getStyleClass().add("banner");
        bannerLabel.textProperty().bind(viewModel.bannerMessageProperty());
        bannerLabel.visibleProperty().bind(viewModel.bannerMessageProperty().isNotEmpty());
        bannerLabel.managedProperty().bind(bannerLabel.visibleProperty());

        Label cpmBanner = new Label();
        cpmBanner.getStyleClass().add("banner-error");
        cpmBanner.textProperty().bind(viewModel.cpmMessageProperty());
        cpmBanner.visibleProperty().bind(viewModel.cpmMessageProperty().isNotEmpty());
        cpmBanner.managedProperty().bind(cpmBanner.visibleProperty());

        ToggleGroup scaleGroup = new ToggleGroup();
        ToggleButton dayButton = new ToggleButton("Day");
        ToggleButton weekButton = new ToggleButton("Week");
        ToggleButton monthButton = new ToggleButton("Month");
        dayButton.setToggleGroup(scaleGroup);
        weekButton.setToggleGroup(scaleGroup);
        monthButton.setToggleGroup(scaleGroup);

        scaleGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == dayButton) {
                viewModel.scaleProperty().set(TimeScale.DAY);
            } else if (newValue == weekButton) {
                viewModel.scaleProperty().set(TimeScale.WEEK);
            } else if (newValue == monthButton) {
                viewModel.scaleProperty().set(TimeScale.MONTH);
            }
        });

        viewModel.scaleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == TimeScale.DAY) {
                scaleGroup.selectToggle(dayButton);
            } else if (newValue == TimeScale.WEEK) {
                scaleGroup.selectToggle(weekButton);
            } else {
                scaleGroup.selectToggle(monthButton);
            }
        });
        scaleGroup.selectToggle(weekButton);

        Slider zoomSlider = new Slider(0.5, 3.0, 1.0);
        zoomSlider.valueProperty().bindBidirectional(viewModel.zoomProperty());

        Button addTaskButton = new Button("+ Task");
        addTaskButton.setOnAction(event -> {
            Optional<TaskDialog.Result> result = TaskDialog.show(getWindow());
            result.ifPresent(taskResult -> {
                try {
                    viewModel.createTask(taskResult.title(), taskResult.startDate(), taskResult.dueDate());
                } catch (ValidationException ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        Label finishDateLabel = new Label();
        finishDateLabel.textProperty().bind(Bindings.createStringBinding(
                viewModel::projectFinishDateText,
                viewModel.projectFinishDateProperty()
        ));
        finishDateLabel.getStyleClass().add("project-finish");

        HBox controls = new HBox(10,
                new Label("Scale"), dayButton, weekButton, monthButton,
                new Label("Zoom"), zoomSlider,
                addTaskButton,
                finishDateLabel
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("top-controls");

        VBox top = new VBox(6, bannerLabel, cpmBanner, controls);
        top.setPadding(new Insets(8, 8, 6, 8));
        top.getStyleClass().add("top-wrapper");
        return top;
    }

    private Node buildLeftPanel() {
        projectListView.setItems(viewModel.projects());
        projectListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Circle dot = new Circle(5, safeColor(project.color()));
                Label label = new Label(project.name());
                HBox row = new HBox(8, dot, label);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });

        Button addProjectButton = new Button("+ Project");
        Button editProjectButton = new Button("Edit");
        Button deleteProjectButton = new Button("Delete");

        addProjectButton.setMaxWidth(Double.MAX_VALUE);
        editProjectButton.setMaxWidth(Double.MAX_VALUE);
        deleteProjectButton.setMaxWidth(Double.MAX_VALUE);

        addProjectButton.setOnAction(event -> {
            Optional<ProjectDialog.Result> result = ProjectDialog.show(getWindow(), null);
            result.ifPresent(projectResult -> {
                try {
                    viewModel.createProject(projectResult.name(), projectResult.colorHex(), projectResult.priority());
                } catch (Exception ex) {
                    showValidationError(ex.getMessage());
                }
            });
        });

        editProjectButton.setOnAction(event -> {
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

        deleteProjectButton.setOnAction(event -> {
            Project selected = viewModel.selectedProjectProperty().get();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete project '" + selected.name() + "'?");
            confirm.initOwner(getWindow());
            confirm.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> viewModel.deleteSelectedProject());
        });

        VBox panel = new VBox(8,
                new Label("Projects"),
                projectListView,
                addProjectButton,
                editProjectButton,
                deleteProjectButton
        );
        panel.setPadding(new Insets(8));
        panel.setPrefWidth(230);
        panel.getStyleClass().add("left-panel");
        VBox.setVgrow(projectListView, Priority.ALWAYS);
        return panel;
    }

    private Node buildCenterPanel() {
        Node dashboard = buildDashboard();

        VBox panel = new VBox(8, dashboard, ganttChartView);
        panel.setPadding(new Insets(8));
        panel.getStyleClass().add("center-panel");
        VBox.setVgrow(ganttChartView, Priority.ALWAYS);
        return panel;
    }

    private Node buildDashboard() {
        ListView<Task> dueTodayList = dashboardListView(viewModel.dueToday());
        ListView<Task> dueInSevenList = dashboardListView(viewModel.dueInSevenDays());
        ListView<Task> overdueList = dashboardListView(viewModel.overdue());
        ListView<Task> blockedList = dashboardListView(viewModel.blockedByDependencies());

        VBox dueTodayBox = dashboardBox("Due Today", dueTodayList);
        VBox dueInSevenBox = dashboardBox("Due in 7 days", dueInSevenList);
        VBox overdueBox = dashboardBox("Overdue", overdueList);
        VBox blockedBox = dashboardBox("Blocked by dependencies", blockedList);

        HBox row = new HBox(8, dueTodayBox, dueInSevenBox, overdueBox, blockedBox);
        row.getStyleClass().add("dashboard");
        for (Node child : row.getChildren()) {
            HBox.setHgrow(child, Priority.ALWAYS);
        }
        return row;
    }

    private VBox dashboardBox(String title, ListView<Task> listView) {
        Label label = new Label(title);
        label.getStyleClass().add("dashboard-title");
        VBox box = new VBox(4, label, listView);
        box.getStyleClass().add("dashboard-box");
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }

    private ListView<Task> dashboardListView(ObservableList<Task> tasks) {
        ListView<Task> listView = new ListView<>();
        listView.setItems(tasks);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    return;
                }
                setText(task.title() + " (due " + task.dueDate() + ")");
            }
        });
        listView.setOnMouseClicked(event -> {
            Task selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                viewModel.selectTask(selected);
                ganttChartView.focusTask(selected.id());
            }
        });
        listView.setPrefHeight(120);
        return listView;
    }

    private Node buildRightPanel() {
        statusComboBox.getItems().setAll(TaskStatus.values());

        HBox progressRow = new HBox(8, progressSlider, progressLabel);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        Button addDependencyButton = new Button("Add Dependency");
        Button removeDependencyButton = new Button("Remove Dependency");
        Button deleteTaskButton = new Button("Delete Task");

        addDependencyButton.setOnAction(event -> addDependency());
        removeDependencyButton.setOnAction(event -> {
            Dependency selected = dependencyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                viewModel.removeDependency(selected.id());
                refreshInspector(viewModel.selectedTaskProperty().get());
            }
        });
        deleteTaskButton.setOnAction(event -> viewModel.deleteSelectedTask());

        dependencyListView.setCellFactory(view -> new ListCell<>() {
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

        conflictListView.setItems(viewModel.conflicts());
        conflictListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Conflict conflict, boolean empty) {
                super.updateItem(conflict, empty);
                setText(empty || conflict == null ? null : conflict.message());
            }
        });
        conflictListView.setOnMouseClicked(event -> {
            Conflict selected = conflictListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                viewModel.findTask(selected.toTaskId()).ifPresent(task -> {
                    viewModel.selectTask(task);
                    ganttChartView.focusTask(task.id());
                });
            }
        });

        VBox panel = new VBox(8,
                new Label("Task Inspector"),
                new Label("Title"), titleField,
                new Label("Start Date"), startDatePicker,
                new Label("Due Date"), dueDatePicker,
                new Label("Progress"), progressRow,
                new Label("Status"), statusComboBox,
                slackLabel,
                new Label("Dependencies"), dependencyListView,
                addDependencyButton, removeDependencyButton,
                deleteTaskButton,
                new Label("Conflicts"), conflictListView
        );
        panel.getStyleClass().add("right-panel");
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(8));
        VBox.setVgrow(dependencyListView, Priority.SOMETIMES);
        VBox.setVgrow(conflictListView, Priority.ALWAYS);
        return panel;
    }

    private void wireProjectSelection() {
        projectListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.selectProject(newValue);
            }
        });
        viewModel.selectedProjectProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && projectListView.getSelectionModel().getSelectedItem() != newValue) {
                projectListView.getSelectionModel().select(newValue);
            }
        });

        if (!viewModel.projects().isEmpty()) {
            projectListView.getSelectionModel().select(viewModel.projects().getFirst());
        }
    }

    private void wireTaskInspector() {
        titleField.setOnAction(event -> commitTitle());
        titleField.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitTitle();
            }
        });

        startDatePicker.setOnAction(event -> commitDates());
        dueDatePicker.setOnAction(event -> commitDates());

        progressSlider.setShowTickMarks(true);
        progressSlider.setShowTickLabels(true);
        progressSlider.setMajorTickUnit(25);
        progressSlider.valueProperty().addListener((obs, oldValue, newValue) -> progressLabel.setText(newValue.intValue() + "%"));
        progressSlider.valueChangingProperty().addListener((obs, oldValue, changing) -> {
            if (!changing) {
                commitProgress();
            }
        });
        progressSlider.setOnMouseReleased(event -> commitProgress());

        statusComboBox.setOnAction(event -> {
            if (!inspectorUpdating) {
                TaskStatus status = statusComboBox.getValue();
                if (status != null) {
                    viewModel.updateSelectedTaskStatus(status);
                }
            }
        });

        viewModel.selectedTaskProperty().addListener((obs, oldValue, newValue) -> {
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
    }

    private void wireGantt() {
        ganttChartView.setTasks(viewModel.projectTasks());
        ganttChartView.scaleProperty().bind(viewModel.scaleProperty());
        ganttChartView.zoomProperty().bind(viewModel.zoomProperty());

        ganttChartView.setOnTaskSelected(viewModel::selectTask);
        ganttChartView.setOnTaskDateChanged((taskId, startDate, dueDate) -> {
            try {
                viewModel.updateTaskDatesFromGantt(taskId, startDate, dueDate);
            } catch (ValidationException ex) {
                showValidationError(ex.getMessage());
            }
        });

        ganttChartView.setTaskColorProvider(task -> viewModel.projects().stream()
                .filter(project -> project.id() == task.projectId())
                .map(Project::color)
                .findFirst()
                .orElse("#3A7AFE"));

        viewModel.projects().addListener((ListChangeListener<? super Project>) change -> ganttChartView.refresh());
    }

    private void wireDerivedState() {
        viewModel.riskByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super com.deadlineflow.domain.model.RiskLevel>)
                change -> ganttChartView.setRiskByTaskId(new HashMap<>(viewModel.riskByTaskIdProperty())));
        viewModel.conflictMessageByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super String>)
                change -> ganttChartView.setConflictMessageByTaskId(new HashMap<>(viewModel.conflictMessageByTaskIdProperty())));
        viewModel.criticalTaskIdsProperty().addListener((javafx.collections.SetChangeListener<? super String>)
                change -> ganttChartView.setCriticalTaskIds(new java.util.HashSet<>(viewModel.criticalTaskIdsProperty())));
        viewModel.slackByTaskIdProperty().addListener((javafx.collections.MapChangeListener<? super String, ? super Integer>)
                change -> ganttChartView.setSlackByTaskId(new HashMap<>(viewModel.slackByTaskIdProperty())));

        ganttChartView.setRiskByTaskId(new HashMap<>(viewModel.riskByTaskIdProperty()));
        ganttChartView.setConflictMessageByTaskId(new HashMap<>(viewModel.conflictMessageByTaskIdProperty()));
        ganttChartView.setCriticalTaskIds(new java.util.HashSet<>(viewModel.criticalTaskIdsProperty()));
        ganttChartView.setSlackByTaskId(new HashMap<>(viewModel.slackByTaskIdProperty()));
    }

    private void refreshInspector(Task task) {
        inspectorUpdating = true;
        boolean disabled = task == null;

        titleField.setDisable(disabled);
        startDatePicker.setDisable(disabled);
        dueDatePicker.setDisable(disabled);
        progressSlider.setDisable(disabled);
        statusComboBox.setDisable(disabled);
        dependencyListView.setDisable(disabled);

        if (task == null) {
            titleField.clear();
            startDatePicker.setValue(null);
            dueDatePicker.setValue(null);
            progressSlider.setValue(0);
            progressLabel.setText("0%");
            statusComboBox.setValue(null);
            dependencyListView.getItems().clear();
            slackLabel.setText("Slack: -");
        } else {
            titleField.setText(task.title());
            startDatePicker.setValue(task.startDate());
            dueDatePicker.setValue(task.dueDate());
            progressSlider.setValue(task.progress());
            progressLabel.setText(task.progress() + "%");
            statusComboBox.setValue(task.status());
            dependencyListView.getItems().setAll(viewModel.dependenciesForSelectedTask());
            dependencyListView.getItems().sort(Comparator.comparing(viewModel::dependencyLabel));
            slackLabel.setText(viewModel.slackLabelForTask(task.id()));
        }
        inspectorUpdating = false;
    }

    private void commitTitle() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        try {
            viewModel.updateSelectedTaskTitle(titleField.getText());
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void commitDates() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        try {
            viewModel.updateSelectedTaskDates(startDatePicker.getValue(), dueDatePicker.getValue());
        } catch (ValidationException ex) {
            showValidationError(ex.getMessage());
            refreshInspector(viewModel.selectedTaskProperty().get());
        }
    }

    private void commitProgress() {
        if (inspectorUpdating || viewModel.selectedTaskProperty().get() == null) {
            return;
        }
        viewModel.updateSelectedTaskProgress((int) Math.round(progressSlider.getValue()));
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
                .map(task -> task.title() + " (due " + DateTimeFormatter.ISO_LOCAL_DATE.format(task.dueDate()) + ")")
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.getFirst(), labels);
        dialog.initOwner(getWindow());
        dialog.setTitle("Add Dependency");
        dialog.setHeaderText("Select predecessor task");
        dialog.setContentText("Task");

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

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(getWindow());
        alert.showAndWait();
    }

    private Window getWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private Color safeColor(String value) {
        try {
            return Color.web(value);
        } catch (Exception ignored) {
            return Color.web("#3A7AFE");
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/presentation/view/ProjectDialog.java
```java
package com.deadlineflow.presentation.view;

import com.deadlineflow.domain.model.Project;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

public final class ProjectDialog {
    private ProjectDialog() {
    }

    public record Result(String name, String colorHex, int priority) {
    }

    public static Optional<Result> show(Window owner, Project existing) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create Project" : "Edit Project");
        dialog.initOwner(owner);

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField(existing == null ? "" : existing.name());
        ColorPicker colorPicker = new ColorPicker(existing == null ? Color.web("#3A7AFE") : safeColor(existing.color()));
        Spinner<Integer> prioritySpinner = new Spinner<>(1, 5, existing == null ? 3 : existing.priority());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Color"), 0, 1);
        grid.add(colorPicker, 1, 1);
        grid.add(new Label("Priority"), 0, 2);
        grid.add(prioritySpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButton) {
                return null;
            }
            return new Result(nameField.getText().trim(), toHex(colorPicker.getValue()), prioritySpinner.getValue());
        });

        return dialog.showAndWait();
    }

    private static String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Color safeColor(String value) {
        try {
            return Color.web(value);
        } catch (Exception ignored) {
            return Color.web("#3A7AFE");
        }
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/presentation/view/TaskDialog.java
```java
package com.deadlineflow.presentation.view;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.Optional;

public final class TaskDialog {
    private TaskDialog() {
    }

    public record Result(String title, LocalDate startDate, LocalDate dueDate) {
    }

    public static Optional<Result> show(Window owner) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Task");
        dialog.initOwner(owner);

        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Task title");
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker dueDatePicker = new DatePicker(LocalDate.now().plusDays(2));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Title"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Start"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Due"), 0, 2);
        grid.add(dueDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != createButton) {
                return null;
            }
            return new Result(titleField.getText().trim(), startDatePicker.getValue(), dueDatePicker.getValue());
        });

        return dialog.showAndWait();
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/java/com/deadlineflow/presentation/viewmodel/MainViewModel.java
```java
package com.deadlineflow.presentation.viewmodel;

import com.deadlineflow.application.services.ConflictService;
import com.deadlineflow.application.services.CriticalPathResult;
import com.deadlineflow.application.services.CriticalPathService;
import com.deadlineflow.application.services.DependencyGraphService;
import com.deadlineflow.application.services.RiskService;
import com.deadlineflow.application.services.SchedulerEngine;
import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Conflict;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import com.deadlineflow.domain.model.TimeScale;
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

public class MainViewModel {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DependencyRepository dependencyRepository;

    private final SchedulerEngine schedulerEngine;
    private final ConflictService conflictService;
    private final RiskService riskService;
    private final DependencyGraphService dependencyGraphService;
    private final CriticalPathService criticalPathService;

    private final ObjectProperty<Project> selectedProject = new SimpleObjectProperty<>();
    private final ObjectProperty<Task> selectedTask = new SimpleObjectProperty<>();
    private final ObjectProperty<TimeScale> scale = new SimpleObjectProperty<>(TimeScale.WEEK);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    private final StringProperty bannerMessage = new SimpleStringProperty("");
    private final StringProperty cpmMessage = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> projectFinishDate = new SimpleObjectProperty<>();

    private final ObservableList<Project> projects;
    private final ObservableList<Task> allTasks;
    private final ObservableList<Dependency> allDependencies;
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

    public MainViewModel(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            DependencyRepository dependencyRepository,
            SchedulerEngine schedulerEngine,
            ConflictService conflictService,
            RiskService riskService,
            DependencyGraphService dependencyGraphService,
            CriticalPathService criticalPathService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.schedulerEngine = schedulerEngine;
        this.conflictService = conflictService;
        this.riskService = riskService;
        this.dependencyGraphService = dependencyGraphService;
        this.criticalPathService = criticalPathService;

        this.projects = projectRepository.getAll();
        this.allTasks = taskRepository.getAll();
        this.allDependencies = dependencyRepository.getAll();
        this.projectTasks = new FilteredList<>(allTasks, task -> false);
        this.projectDependencies = new FilteredList<>(allDependencies, dependency -> false);

        selectedProject.addListener((obs, oldValue, newValue) -> {
            refreshProjectFilters();
            if (selectedTask.get() != null && (newValue == null || selectedTask.get().projectId() != newValue.id())) {
                selectedTask.set(null);
            }
            recomputeDerivedState();
        });

        allTasks.addListener((javafx.collections.ListChangeListener<? super Task>) change -> {
            if (selectedTask.get() != null && findTask(selectedTask.get().id()).isEmpty()) {
                selectedTask.set(null);
            }
            recomputeDerivedState();
        });
        allDependencies.addListener((javafx.collections.ListChangeListener<? super Dependency>) change -> recomputeDerivedState());

        if (!projects.isEmpty()) {
            selectedProject.set(projects.getFirst());
        } else {
            refreshProjectFilters();
            recomputeDerivedState();
        }
    }

    public ObservableList<Project> projects() {
        return projects;
    }

    public FilteredList<Task> projectTasks() {
        return projectTasks;
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

    public void createProject(String name, String color, int priority) {
        try {
            Project created = projectRepository.save(new Project(0, name, color, priority));
            selectedProject.set(projectRepository.findById(created.id()).orElse(created));
            bannerMessage.set("Project created");
            recomputeDerivedState();
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
            recomputeDerivedState();
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
        recomputeDerivedState();
    }

    public void createTask(String title, LocalDate startDate, LocalDate dueDate) {
        Project project = selectedProject.get();
        if (project == null) {
            throw new ValidationException("Select a project before creating tasks");
        }

        try {
            Task task = new Task(
                    UUID.randomUUID().toString(),
                    project.id(),
                    title,
                    startDate,
                    dueDate,
                    0,
                    TaskStatus.NOT_STARTED
            );

            schedulerEngine.validateTaskDates(task);
            taskRepository.save(task);
            selectTask(task);
            bannerMessage.set("Task created");
            recomputeDerivedState();
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public void deleteSelectedTask() {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        taskRepository.delete(task.id());
        selectedTask.set(null);
        bannerMessage.set("Task deleted");
        recomputeDerivedState();
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
            recomputeDerivedState();
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
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
            recomputeDerivedState();
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
            recomputeDerivedState();
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
        recomputeDerivedState();
    }

    public void updateSelectedTaskStatus(TaskStatus status) {
        Task task = selectedTask.get();
        if (task == null) {
            return;
        }
        Task updated = task.withStatus(status);
        taskRepository.save(updated);
        selectedTask.set(taskRepository.findById(updated.id()).orElse(updated));
        recomputeDerivedState();
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
        recomputeDerivedState();
        return true;
    }

    public void removeDependency(String dependencyId) {
        dependencyRepository.delete(dependencyId);
        bannerMessage.set("Dependency removed");
        recomputeDerivedState();
    }

    public Optional<Task> findTask(String taskId) {
        return taskRepository.findById(taskId);
    }

    public String dependencyLabel(Dependency dependency) {
        return taskTitle(dependency.fromTaskId()) + " -> " + taskTitle(dependency.toTaskId());
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

        projectTasks.setPredicate(task -> task.projectId() == project.id());
        projectDependencies.setPredicate(dependency -> {
            Optional<Task> fromTask = findTask(dependency.fromTaskId());
            Optional<Task> toTask = findTask(dependency.toTaskId());
            return fromTask.filter(task -> task.projectId() == project.id()).isPresent()
                    && toTask.filter(task -> task.projectId() == project.id()).isPresent();
        });
    }

    private void recomputeDerivedState() {
        refreshProjectFilters();

        Collection<Task> activeTasks = List.copyOf(projectTasks);
        Collection<Dependency> activeDependencies = List.copyOf(projectDependencies);

        List<Conflict> detectedConflicts = conflictService.detectDependencyConflicts(activeTasks, activeDependencies);
        conflicts.setAll(detectedConflicts);

        Map<String, String> conflictMap = new HashMap<>();
        for (Conflict conflict : detectedConflicts) {
            conflictMap.merge(conflict.toTaskId(), conflict.message(), (a, b) -> a + "\n" + b);
        }
        conflictMessageByTaskId.clear();
        conflictMessageByTaskId.putAll(conflictMap);

        LocalDate today = LocalDate.now();
        Map<String, RiskLevel> riskMap = new HashMap<>();
        for (Task task : activeTasks) {
            riskMap.put(task.id(), riskService.evaluate(task, today));
        }
        riskByTaskId.clear();
        riskByTaskId.putAll(riskMap);

        CriticalPathResult cpm = criticalPathService.compute(activeTasks, activeDependencies);
        if (cpm.hasCycle()) {
            cpmMessage.set("Critical path disabled: dependency cycle detected");
            projectFinishDate.set(null);
            criticalTaskIds.clear();
            slackByTaskId.clear();
        } else {
            cpmMessage.set("");
            projectFinishDate.set(cpm.projectFinishDate());
            criticalTaskIds.clear();
            criticalTaskIds.addAll(cpm.criticalTaskIds());
            slackByTaskId.clear();
            slackByTaskId.putAll(cpm.slackDays());
        }

        refreshDashboard(activeTasks, activeDependencies, today);
    }

    private void refreshDashboard(Collection<Task> activeTasks, Collection<Dependency> activeDependencies, LocalDate today) {
        dueToday.setAll(sorted(activeTasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE && task.dueDate().isEqual(today))
                .toList()));

        dueInSevenDays.setAll(sorted(activeTasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE
                        && task.dueDate().isAfter(today)
                        && !task.dueDate().isAfter(today.plusDays(7)))
                .toList()));

        overdue.setAll(sorted(activeTasks.stream()
                .filter(task -> task.status() != TaskStatus.DONE && task.dueDate().isBefore(today))
                .toList()));

        Map<String, Task> taskById = new HashMap<>();
        for (Task task : activeTasks) {
            taskById.put(task.id(), task);
        }

        Set<String> blocked = new HashSet<>();
        for (Dependency dependency : activeDependencies) {
            Task from = taskById.get(dependency.fromTaskId());
            Task to = taskById.get(dependency.toTaskId());
            if (from == null || to == null) {
                continue;
            }
            if (to.status() != TaskStatus.DONE && from.status() != TaskStatus.DONE) {
                blocked.add(to.id());
            }
        }

        List<Task> blockedTasks = new ArrayList<>();
        for (String taskId : blocked) {
            Task task = taskById.get(taskId);
            if (task != null) {
                blockedTasks.add(task);
            }
        }
        blockedByDependencies.setAll(sorted(blockedTasks));
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
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/main/resources/com/deadlineflow/presentation/styles/app.css
```css
.root {
    -fx-font-family: "Avenir Next", "Helvetica Neue", Helvetica, Arial, sans-serif;
    -fx-base: #f5f7fc;
    -fx-background: #f5f7fc;
    -fx-control-inner-background: #ffffff;
}

.main-root {
    -fx-background-color: linear-gradient(to bottom, #f8faff, #edf2fb);
}

.top-wrapper {
    -fx-background-color: rgba(255, 255, 255, 0.9);
    -fx-background-radius: 10;
    -fx-border-color: #dbe4f2;
    -fx-border-radius: 10;
}

.top-controls {
    -fx-padding: 6 4 4 4;
}

.banner {
    -fx-background-color: #ecf5ff;
    -fx-text-fill: #17427a;
    -fx-padding: 8;
    -fx-background-radius: 6;
}

.banner-error {
    -fx-background-color: #ffecec;
    -fx-text-fill: #8b1f1f;
    -fx-padding: 8;
    -fx-background-radius: 6;
}

.project-finish {
    -fx-font-weight: bold;
    -fx-text-fill: #26365d;
}

.left-panel,
.right-panel,
.center-panel {
    -fx-background-color: rgba(255, 255, 255, 0.92);
    -fx-background-radius: 10;
    -fx-border-color: #dbe4f2;
    -fx-border-radius: 10;
}

.left-panel .label,
.right-panel .label {
    -fx-text-fill: #24385f;
}

.dashboard {
    -fx-padding: 0;
}

.dashboard-box {
    -fx-background-color: #f7f9fe;
    -fx-background-radius: 8;
    -fx-border-color: #dbe4f2;
    -fx-border-radius: 8;
    -fx-padding: 6;
}

.dashboard-title {
    -fx-font-size: 12px;
    -fx-font-weight: bold;
    -fx-text-fill: #2b3f67;
}

.gantt-chart {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8;
    -fx-border-color: #dbe4f2;
    -fx-border-radius: 8;
}

.gantt-scroll {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
}

.gantt-bar-label {
    -fx-font-size: 11px;
    -fx-font-weight: bold;
    -fx-text-fill: #f9fbff;
}

.gantt-risk-badge {
    -fx-font-size: 9px;
    -fx-font-weight: bold;
    -fx-text-fill: #ffffff;
    -fx-background-color: #be2f2f;
    -fx-background-radius: 99;
    -fx-padding: 1 4 1 4;
}

.list-view {
    -fx-background-radius: 7;
    -fx-border-radius: 7;
    -fx-border-color: #dbe4f2;
}

.button {
    -fx-background-color: linear-gradient(to bottom, #fdfefe, #e9eef8);
    -fx-text-fill: #24385f;
    -fx-border-color: #c9d6eb;
    -fx-border-radius: 6;
    -fx-background-radius: 6;
}

.button:hover {
    -fx-background-color: linear-gradient(to bottom, #ffffff, #dde7f8);
}

.text-field,
.date-picker,
.combo-box,
.slider {
    -fx-focus-color: #2b6ef2;
    -fx-faint-focus-color: rgba(43, 110, 242, 0.2);
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/test/java/com/deadlineflow/application/services/ConflictServiceTest.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConflictServiceTest {

    private final ConflictService conflictService = new ConflictService();

    @Test
    void finishStartConflictRequiresOneDayLag() {
        Task predecessor = new Task(
                "a",
                1,
                "Predecessor",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10),
                0,
                TaskStatus.NOT_STARTED
        );
        Task dependent = new Task(
                "b",
                1,
                "Dependent",
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 12),
                0,
                TaskStatus.NOT_STARTED
        );

        List<Dependency> dependencies = List.of(new Dependency("d1", "a", "b", DependencyType.FINISH_START));

        assertEquals(1, conflictService.detectDependencyConflicts(List.of(predecessor, dependent), dependencies).size());
    }

    @Test
    void validDependencyProducesNoConflict() {
        Task predecessor = new Task(
                "a",
                1,
                "Predecessor",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10),
                0,
                TaskStatus.NOT_STARTED
        );
        Task dependent = new Task(
                "b",
                1,
                "Dependent",
                LocalDate.of(2026, 2, 11),
                LocalDate.of(2026, 2, 13),
                0,
                TaskStatus.NOT_STARTED
        );

        List<Dependency> dependencies = List.of(new Dependency("d1", "a", "b", DependencyType.FINISH_START));

        assertEquals(0, conflictService.detectDependencyConflicts(List.of(predecessor, dependent), dependencies).size());
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/test/java/com/deadlineflow/application/services/CriticalPathServiceTest.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriticalPathServiceTest {

    private final CriticalPathService criticalPathService = new CriticalPathService(new DependencyGraphService());

    @Test
    void computesEarliestLatestAndSlack() {
        Task a = new Task("a", 1, "A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), 0, TaskStatus.NOT_STARTED); // 3d
        Task b = new Task("b", 1, "B", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, TaskStatus.NOT_STARTED); // 2d
        Task c = new Task("c", 1, "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4), 0, TaskStatus.NOT_STARTED); // 4d
        Task d = new Task("d", 1, "D", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, TaskStatus.NOT_STARTED); // 2d

        List<Dependency> dependencies = List.of(
                new Dependency("d1", "a", "c", DependencyType.FINISH_START),
                new Dependency("d2", "b", "c", DependencyType.FINISH_START),
                new Dependency("d3", "c", "d", DependencyType.FINISH_START)
        );

        CriticalPathResult result = criticalPathService.compute(List.of(a, b, c, d), dependencies);

        assertFalse(result.hasCycle());
        assertEquals(0, result.slackDays().get("a"));
        assertEquals(1, result.slackDays().get("b"));
        assertEquals(0, result.slackDays().get("c"));
        assertEquals(0, result.slackDays().get("d"));

        assertTrue(result.criticalTaskIds().contains("a"));
        assertTrue(result.criticalTaskIds().contains("c"));
        assertTrue(result.criticalTaskIds().contains("d"));
        assertEquals(LocalDate.of(2026, 1, 9), result.projectFinishDate());
    }

    @Test
    void cycleDisablesCriticalPath() {
        Task a = new Task("a", 1, "A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 0, TaskStatus.NOT_STARTED);
        Task b = new Task("b", 1, "B", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 0, TaskStatus.NOT_STARTED);

        List<Dependency> dependencies = List.of(
                new Dependency("d1", "a", "b", DependencyType.FINISH_START),
                new Dependency("d2", "b", "a", DependencyType.FINISH_START)
        );

        CriticalPathResult result = criticalPathService.compute(List.of(a, b), dependencies);

        assertTrue(result.hasCycle());
        assertTrue(result.projectFinishDate() == null);
        assertTrue(result.criticalTaskIds().isEmpty());
    }
}

```

## /Users/zuotianhao/Desktop/DeadlineFlow/src/test/java/com/deadlineflow/application/services/DependencyGraphServiceTest.java
```java
package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import com.deadlineflow.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphServiceTest {

    private final DependencyGraphService graphService = new DependencyGraphService();

    @Test
    void topologicalSortOrdersPredecessorsBeforeSuccessors() {
        Task a = task("a", "A");
        Task b = task("b", "B");
        Task c = task("c", "C");

        List<Dependency> dependencies = List.of(
                dependency("d1", "a", "b"),
                dependency("d2", "b", "c")
        );

        DependencyGraphService.TopologyResult result = graphService.topologicalSort(List.of(a, b, c), dependencies);

        assertFalse(result.hasCycle());
        int idxA = result.orderedTaskIds().indexOf("a");
        int idxB = result.orderedTaskIds().indexOf("b");
        int idxC = result.orderedTaskIds().indexOf("c");
        assertTrue(idxA < idxB);
        assertTrue(idxB < idxC);
    }

    @Test
    void detectsCycle() {
        Task a = task("a", "A");
        Task b = task("b", "B");

        List<Dependency> dependencies = List.of(
                dependency("d1", "a", "b"),
                dependency("d2", "b", "a")
        );

        DependencyGraphService.TopologyResult result = graphService.topologicalSort(List.of(a, b), dependencies);

        assertTrue(result.hasCycle());
        assertTrue(result.cycleTaskIds().contains("a"));
        assertTrue(result.cycleTaskIds().contains("b"));
    }

    private Task task(String id, String title) {
        return new Task(id, 1, title, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, TaskStatus.NOT_STARTED);
    }

    private Dependency dependency(String id, String from, String to) {
        return new Dependency(id, from, to, DependencyType.FINISH_START);
    }
}

```

