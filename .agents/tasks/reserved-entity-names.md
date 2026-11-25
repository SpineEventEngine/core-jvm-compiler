---
slug: reserved-entity-names
branch: claude/entity-class-name-clashes-dc104b
owner: claude
status: in-progress
started: 2026-11-25
related-memories:
  - one-pipeline-run-per-spec-class
  - copyright-header-migration
---

## Goal

The classes `Column`, `Query`, and `QueryBuilder`, which the compiler nests into the Java class of
an entity state, never clash with the developer's types without a clear error.

## Context

A state with such a name, or with its own nested message or enum with such a name, does not
compile. `kotlinc` reports `Unresolved reference 'newBuilder'` in the generated Kotlin code, and
`javac` reports a duplicate class among many other errors. A top-level type of the same package does
not compile if it is the type of a column or of the ID, because the generated code refers to it by
its simple name, and Java takes it for the generated class. This is true for `Field`, too.

The state `Column` of the Kanban example, which is on Spine 1.x, is affected. It has to be renamed
when the example moves to Spine 2.

## Rules

1. The names `Column`, `Query`, and `QueryBuilder` are reserved. The compiler reports an error if
   an entity state, or a message or an enum nested directly in it, has one of these names. The error
   is not reported if the query support is turned off by `skipQueries()`.
2. The generated code refers to the type of a column or of the ID by its full name if the Java class
   of the state has a nested class with the same simple name. Other references stay as they are.

Giving other names to the generated classes, e.g., `Column_`, was considered, too. For `Column`, it
needs a change of `SpecScanner` in `core-jvm`, which finds the class by its name.

## Plan

- [x] Version, copyright notices
- [x] Reserved names: tests, then code
- [x] References: tests, then code
- [x] Integration test in `tests/entity-queries`
- [x] Full build, dependency reports
- [ ] `pre-pr`, draft pull request

## Not included

- Names that differ from the reserved ones only in case.
- References to the classes of `java.lang`, e.g., if a state has a nested message `String`.
- Top-level types with the names of the classes inherited from the base classes of Protobuf, e.g.,
  `FieldAccessorTable`.
- Entity states nested in other messages are the subject of `reject-nested-entity-states`.

## Log

- 2026-11-25 — the rules are agreed with the user, the plan is approved
- 2026-11-25 — implemented; each rule was switched off in turn, and every time a test failed;
  the code generated for the states without a clash is the same as before
