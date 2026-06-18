# CoreJvm Compiler Integration tests

This directory (`tests`) contains integration tests for the CoreJvm Compiler.

This is a separate Gradle project and depends on `core-jvm-compiler` via Gradle 
[composite build][composite-build]. Please see [`settings.gradle.kts`](settings.gradle.kts)
for details.

Tests in this module are run in a separate Gradle process and currently cannot be included in the 
repository code coverage because of Jacoco limitations.

## Execution
These tests are executed as the `integrationTest` task of
the [main build script](../build.gradle.kts).

> **NOTE:** The `check` task of the root project depends on `integrationTest`,<br/>
> so the integration tests are performed automatically during the root build.<br/>
> There is no need to do anything extra about it.

If you need to run only integration tests, while the root project directory is your current,
please run the following command:

```bash
./gradlew integrationTest
```

If your current directory is `tests`, just run the Gradle build as usual:

```bash
./gradlew clean build
```

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

To configure IntelliJ IDEA for recognizing `tests` module as the actual source code, please see 
[this page][idea-composite-builds].

[git-symlinks-option]: https://git-scm.com/docs/git-config#Documentation/git-config.txt-coresymlinks
[developer-mode]: https://learn.microsoft.com/en-us/windows/apps/get-started/developer-mode-features-and-debugging
[composite-build]: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions
[idea-composite-builds]: https://blog.jetbrains.com/idea/2016/10/intellij-idea-2016-3-eap-gradle-composite-builds-and-android-studio-2-2/ 
