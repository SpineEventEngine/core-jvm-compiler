# CoreJvm Compiler Integration tests

This directory (`tests`) contains integration tests for the CoreJvm Compiler.

This is a separate Gradle project, which consumes the artifacts that `core-jvm-compiler`
publishes to Maven Local with its `localPublish` task.

It is deliberately *not* a [composite build][composite-build] of the root project: such
a build would recompile the root modules in their own directories in the middle of the root
build — after its tests and before its coverage report — so the report would analyze classes
other than those the tests ran against. Please see
[`settings.gradle.kts`](settings.gradle.kts) for details.

Tests in this module are run in a separate Gradle process and currently cannot be included in the 
repository code coverage because of Jacoco limitations.

## Execution
These tests are executed as the `integrationTests` task of
the [main build script](../build.gradle.kts).

> **NOTE:** The `check` task of the root project depends on `integrationTests`,<br/>
> so the integration tests are performed automatically during the root build.<br/>
> There is no need to do anything extra about it.

If you need to run only integration tests, while the root project directory is your current,
please run the following command:

```bash
./gradlew integrationTests
```

If your current directory is `tests`, publish the artifacts under test first, and then run
the Gradle build as usual:

```bash
(cd .. && ./gradlew localPublish)
./gradlew clean build
```

Publish again after every change to the CoreJvm Compiler sources. Otherwise, the build tests
the artifacts published earlier. The `integrationTests` task of the root project does this
for you.

## Symlinks

The `tests` directory contains _soft_ symlinks to directories and files from the root project that
this Gradle project needs for execution:

| Symlink       | Target           |
|---------------|------------------|
| `buildSrc/`   | `../buildSrc/`   | 
| `gradle/`     | `../gradle/`     | 
| `.gitignore`  | `../.gitignore`  |
| `gradlew`     | `../gradlew`     |
| `gradlew.bat` | `../gradlew.bat` |

This arrangement ensures that integration tests use the same versions of dependencies as
the production code in the root project.

### Symlinks support in Git

Git handles symlinks according to the [`core.symlinks` option][git-symlinks-option].

Under Unix-like systems symlink support for Git is likely to be turned on.
It is more complicated under Windows.

### Symlink support in Windows

Symlink creation under Windows requires either Administrator privileges or
the [Developer Mode on][developer-mode] turned on. If you develop under Windows you may want
to turn the [Developer Mode on][developer-mode] for your workstation.

The recommended configuration under Windows is:

| Item                                                | Configuration                                                  |
|-----------------------------------------------------|----------------------------------------------------------------| 
| OS                                                  | Windows 10 Creator Update or newer                             |
| File System                                         | NTFS                                                           |
| Ability to handle symlinks<br/>for the Windows user | Developer Mode enabled                                         |
| Git                                                 | Native Client version 2.10.2 or newer                          |
| Git configuration                                   | **Config file path:**<br/>`C:\Program Files\Git\etc\gitconfig` |
|                                                     | **Git global config:**<pre>[core]<br/>symlinks = true</pre>    |

## Configuring IntelliJ IDEA

IntelliJ IDEA treats `tests` as a separate Gradle project. Link it through
*Gradle* → *+* (Add Gradle Project) and pick `tests/build.gradle.kts`, so that the IDE
indexes the integration tests as source code.

[git-symlinks-option]: https://git-scm.com/docs/git-config#Documentation/git-config.txt-coresymlinks
[developer-mode]: https://learn.microsoft.com/en-us/windows/apps/get-started/developer-mode-features-and-debugging
[composite-build]: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions
