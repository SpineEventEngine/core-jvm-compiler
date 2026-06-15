---
name: gradle-needs-utf8-locale
description: Run Gradle with `LC_ALL=C.UTF-8`; the default POSIX locale makes `sun.jnu.encoding` ASCII and breaks expansion of dependency jars with non-ASCII entries (e.g. KSP).
metadata:
  type: project
  since: 2026-06-15
---

In container/CI-like environments the locale often defaults to `POSIX`/`C`,
which sets the JVM's `sun.jnu.encoding` to ASCII (`ANSI_X3.4-1968`). Gradle
then cannot write out dependency-jar entries whose names contain non-ASCII
characters, so any `zipTree`-based task fails. Concretely,
`:ksp:extractIncludeProto` (the protobuf-gradle-plugin scanning the proto
path) fails with `Cannot expand ZIP '.../symbol-processing-aa-embeddable-<ver>.jar'`,
caused by `java.nio.file.InvalidPathException: Malformed input or input
contains unmappable characters: ksp/javaslang/λ$Memoized.class`. The jar
itself is fine (`unzip -t` clean, SHA-1 matches the cache path) — only the
extraction to disk fails.

**Why:** Hit on 2026-06-15 while running `publishToMavenLocal` to verify
issue #29. `gradle.properties` already sets `-Dfile.encoding=UTF-8`, but that
does **not** affect file names: `sun.jnu.encoding` is derived from the OS
locale, not from `-D` system properties, so the build still breaks.

**How to apply:** Run Gradle under a UTF-8 locale, e.g.
`LC_ALL=C.UTF-8 LANG=C.UTF-8 ./gradlew …` (`C.utf8` is the same locale). Stop
any daemon started under the old locale first (`./gradlew --stop`) — a daemon
captures `sun.jnu.encoding` at JVM startup, so a lingering POSIX daemon keeps
failing. Affects every module that pulls KSP (e.g. `:ksp`, `:routing`) and any
dependency jar carrying non-ASCII entries; the same applies to the `core-jvm`
consumer build.

Related: [[windows-ci-needs-real-symlinks]]
