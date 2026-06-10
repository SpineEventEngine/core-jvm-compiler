---
slug: move-message-file-to-core-jvm-compiler
branch: claude/cool-lamport-rnwzpu
owner: claude
status: in-progress
started: 2026-06-10
---

## Goal

Host the `MessageFile` enum (moved out of `io.spine.base` in `base-libraries`,
see [base-libraries#941](https://github.com/SpineEventEngine/base-libraries/issues/941))
in `core-jvm-base` under `io.spine.tools.core.jvm`, converted to Kotlin, with
all in-repo consumers switched to the new type and the ported unit test green.

## Context

- The enum encodes the codegen file-naming convention
  (`commands.proto`, `events.proto`, `rejections.proto`); its only org-wide
  production consumer is `CoreJvmCompilerSettings` in the `base` module here.
- Strategy: **coordinated hard move** (no shim), per the precedent of
  `move-fs-dir-types-to-tool-base` in `base-libraries`.
- This PR is self-contained: the new enum lives in
  `io.spine.tools.core.jvm`, so it does not clash with
  `io.spine.base.MessageFile` still present in the pinned published
  `spine-base`. When the `Base` dependency is bumped past the removal later,
  nothing here will reference the old class anymore.
- The enum's dependency `io.spine.code.proto.FileName` stays in `spine-base`,
  which `core-jvm-base` already has as an `api` dependency.
- Consumers to update (all in this repo):
  - `base`: `CoreJvmCompilerSettings.kt` (production).
  - `plugins`: `CoreJvmOptionsSpec.kt` (test).
  - `signal-tests`: `SignalPluginTestSetup.kt`, `SignalDiscoverySpec.kt` (tests).

## Plan

- [x] Add `base/src/main/kotlin/io/spine/tools/core/jvm/MessageFile.kt` —
      Kotlin conversion of the Java original (`suffix` becomes a `val`,
      `Predicate<FileDescriptorProto>` retained).
- [x] Switch the four consumer files to the new import and the
      `suffix` property.
- [x] Port `MessageFileTest.java` →
      `base/src/test/kotlin/io/spine/tools/core/jvm/MessageFileSpec.kt`
      (Kotest assertions; synthetic `FileDescriptorProto` instead of the
      test-only proto file used in `base-libraries`; added a test pinning
      the conventional suffix values).
- [x] Bump `version.gradle.kts` → `2.0.0-SNAPSHOT.069`; project version
      strings in `docs/dependencies/*` updated to match (no dependency
      changed, so the regenerated content differs only in those strings).
- [ ] Build the affected modules and run the tests — **blocked locally**:
      the remote session's network policy returns 403 for the Spine
      artifact repositories, so Gradle cannot resolve the SDK snapshot
      dependencies. Verification delegated to PR CI.
- [x] Commit, push, draft PR referencing base-libraries#941.

## Log

- 2026-06-10 — drafted; executing (work authorized by the issue assignment).
- 2026-06-10 — sources done; local Gradle build impossible (403 from all
  Spine repos under the session network policy); relying on PR CI.
