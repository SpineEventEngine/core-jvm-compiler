# Project: Core JVM Compiler Plugins

## Overview

This repository hosts the plugins to the Spine Compiler that the Core JVM
library offers for building client and server applications. The plugins
enrich the Java/Kotlin code generated from Protobuf definitions with
Core-JVM-specific features: signal interfaces, entity columns and queries,
rejection throwables, UUID values, comparators, message-group markers,
and command/event routing.

## Architecture

**Role**: A collection of Gradle plugins and Spine Compiler plugins,
published under `io.spine.tools` and bundled into a single fat JAR.

The repo is a multi-module Gradle build
(`rootProject.name = "core-jvm-compiler"`) with these modules:

- `plugins` — the entry point: `CoreJvmPlugin` (Gradle plugin
  `io.spine.core-jvm`) applies the Protobuf plugin and wires all feature
  plugins into the Spine Compiler via `CompilerConfigPlugin`; assembles
  the fat JAR.
- `base` — shared infrastructure: compiler settings DSL
  (`CoreJvmCompilerSettings`), Gradle extensions, and `testFixtures`
  used by feature-module tests.
- `annotation` — generates Spine API-level annotations
  (`@Internal`, `@SPI`, `@Beta`, `@Experimental`).
- `entity` — entity state code generation (columns, queries).
- `signal` — events, commands, and rejections; includes the rejection
  throwable codegen (`RThrowablePlugin`).
- `marker` — interfaces for the `is`/`every_is` Protobuf options.
- `message-group` — code generation for message groups.
- `uuid` — conveniences for `UuidValue` messages.
- `comparable` — `Comparable` implementations from the `compare_by`
  option.
- `grpc` — gRPC/Kotlin codegen configuration.
- `ksp` — Kotlin Symbol Processing infrastructure shared by KSP-based
  plugins.
- `routing` — command/event routing via a KSP processor; extends the
  `ksp` infrastructure.
- `*-tests` — paired test-only modules for feature modules. They exist
  because tests need vanilla (non-enhanced) Protobuf code; see
  [`TESTING.md`](../TESTING.md) for the rationale.

The `tests/` directory is a separate Gradle build running integration
tests against locally published artifacts. The `BuildSpeed` submodule
hosts build-performance tests executed on pull requests.

Read [`.agents/guidelines/jvm-project.md`](../.agents/guidelines/jvm-project.md) for build stack,
coding style, tests, and versioning.
