---
slug: reject-nested-entity-states
branch: claude/focused-solomon-1f4a72
owner: claude
status: in-progress
started: 2026-11-25
related-memories:
  - one-pipeline-run-per-spec-class
  - copyright-header-migration
---

# Reject entity states nested in other messages

## Goal

An entity state must be a top-level message. If an entity state option, such as `(entity)`,
is applied to a message nested in another one, the compiler reports a compilation error which
names the message, the message it is nested in, and the place in the `.proto` file.

## Context

A nested entity state does not compile today, and the error does not tell why:

```
Outer.java: cannot find symbol, class QueryBuilder, location: class Outer
```

`QueryMethod` adds `query()` to the top-level class of the file, while the `QueryBuilder` class
goes under the class of the nested message.

The initial idea was to make such states compile. The user decided on 2026-11-25 not to support
them, and to report a compilation error instead.

Facts checked before the plan:

- None of the 1,162 entity states in the `.proto` files of the sibling repositories is nested.
  The same is true for the 42 states of the branch `generated-name-clashes`.
- `protoc` records the location of `option (entity).kind = ...` with the number of the `kind`
  field at the end of the path. `Coordinates.forOption()` of the Spine Compiler looks for the path
  without it, so `Option.span` is empty for this notation. The span of the message is always there.
- Descriptor filters of the pipeline apply only to top-level types. So, tests have to accept and
  exclude the messages containing the nested states.

## Plan

- [x] Bump the version -> `2.0.0-SNAPSHOT.109`.
- [x] Add the fixtures `entity-tests/src/testFixtures/proto/given/entities/nesting.proto`:
  - a message with two nested states;
  - a message with a state nested two levels deep;
  - a top-level state with a nested message and a nested enum that are not states.
- [x] Exclude the first two in `EntityPluginTestSetup.defaultExclusions`.
- [x] Add the specs, and see the first two fail:
  - `NestedEntityStateErrorSpec` — the error tells the state, its container, and the place;
  - `DeeplyNestedEntityStateErrorSpec` — the same for two levels. It is a separate class
    because a spec class can run the pipeline only once;
  - `TopLevelEntityStateSpec` — a top-level state with nested types still gets its query API.
- [x] `MessageTypeExts.kt`: add `findEntityOption(settings)`, and use it in `isEntityState()`.
- [x] `EntityDiscovery.kt`: call `Compilation.check(type.isTopLevel, ...)` before emitting
  `EntityStateDiscovered`. Point at the declaration of the message.
- [x] Apply the CodeMatters copyright notice to the changed files in a commit of its own.
- [ ] Build, update the dependency reports, run `pre-pr`, open a draft pull request.

## Log

- 2026-11-25 — drafted, awaiting approval.
- 2026-11-25 — approved, executing.
- 2026-11-25 — `QueryMethod.kt` is not changed. The planned KDoc would be wrong: with
  `java_multiple_files = false` the class of a top-level message is nested in the outer class of
  the file, so `topLevelClass` is not the class of the state. No JVM project among the sibling
  repositories has entity states in such files, only `spine-ts` does.
- 2026-11-25 — checked in a pipeline run: for a top-level state in a file with
  `java_multiple_files = false`, `query()` goes to the outer class of the file, while `QueryBuilder`
  goes to the class of the state. This is the same `javac` error, and it is out of scope here.
- 2026-11-25 — both error specs failed without the check, and pass with it. With `option.span`
  instead of `type.span` the error says `nesting.proto:0:0`, and both specs fail.
