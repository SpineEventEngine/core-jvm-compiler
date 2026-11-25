---
slug: generated-name-clashes
branch: generated-name-clashes
owner: claude
status: in-progress
started: 2026-11-25
---

## Goal

Classes generated for message fields and for routing never get the same name, or the compiler
reports a clear error.

## Context

A review of the names of field classes found types that get the same generated class name.
A field class clash fails in `javac`. A routing class clash fails in KSP with an unclear error, or,
when one entity is in the main code and another in the test code, silently drops one of
the routing classes. No type of the SDK is affected.

## Rules

Field classes. Names that do not clash stay as they are.

1. Types with the same short name from different packages. A type from the package of
   the message keeps the short name: `AddressField`. The others get the last word of their package
   in front: `WebAddressField`. More words are added while the names still clash. If no type is
   from the package of the message, all of them get longer names.
2. Types with the same short name from one package. The least nested type keeps the short name:
   `ShelfBoxField` for `ShelfBox`. The others use underscores: `Shelf_BoxField` for `Shelf.Box`.
   Equally nested types all use underscores.
3. A class named like a class around it gets an underscore before `Field`: `Crate_Field`.
4. A message that is named `Field`, is nested in a type named `Field`, or has its own nested type
   named `Field`: the generated class is `Field_`, and the compiler prints a warning.
5. If the names are still equal, the compiler reports an error.

Routing classes. The names of the outer classes of a nested entity come first, joined with `_`,
always: `A_ScreenEventRouting` for `A.Screen`. Other entities keep the names of their routing
classes. An error is reported if two entities still need the same class.

## Plan

- [x] Field classes: tests, then code
- [x] Routing classes: tests, then code
- [x] Integration test in `tests/entity-queries`
- [x] Full build, copyright notices, dependency reports
- [ ] `pre-pr`, draft pull request

## Not included

- `Column`, `Query`, and `QueryBuilder` are added to entity states in the same way as `Field`, so
  a state with its own nested type of that name should fail the same way. Not tested.
- A routing class is generated twice if route functions of one entity become valid in different
  KSP rounds. Not reproduced.
- Hand-written classes with the name of a generated class.

## Log

- 2026-11-25 — the rules are agreed with the user, the plan is approved
- 2026-11-25 — implemented; each rule was switched off in turn, and every time a test failed;
  the types that did not compile before now compile, and the routing class from the main code is
  loaded along with the one from the test code
