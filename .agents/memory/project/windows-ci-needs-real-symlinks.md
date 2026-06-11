---
name: windows-ci-needs-real-symlinks
description: The `tests/` build requires git to check out real symlinks; never set `core.symlinks false` in Windows CI.
metadata:
  type: project
  since: 2026-06-11
---

The integration-tests build under `tests/` reaches build logic through
git-tracked symlinks (`tests/buildSrc -> ../buildSrc`, plus `gradle/`,
`gradle.properties`, `gradlew`, `gradlew.bat`). Windows CI works only
because GitHub Windows runners check these out as real symlinks by
default. Disabling that (e.g. `git config core.symlinks false` before
checkout) turns `tests/buildSrc` into a placeholder text file, and
`:integrationTests` fails compiling `tests/build.gradle.kts` with
`Unresolved reference 'standardSpineSdkRepositories'` / `'io'`.

**Why:** Incident on 2026-06-10: a `config` sync added a
"Configure Git for Windows symlink compatibility" step
(`core.symlinks false`) to `build-on-windows.yml`, breaking every
Windows run at `:integrationTests`. Reverted in `config@6cbaefdd`
("Restore using symlinks under Windows") and here in `ea50b5045`.

**How to apply:** Do not add `core.symlinks false` (or equivalent) to
Windows workflows. If `:integrationTests` fails on Windows with
unresolved references in `tests/build.gradle.kts`, first check that
`tests/buildSrc` was checked out as a directory symlink, not a file.
The canonical workflow lives in the `config` repo under
`.github-workflows/` — fix it there and sync, or the next
"Update `config`" will reintroduce the problem.
