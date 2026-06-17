# Reject implicit-`(required)` ID fields of type `google.protobuf.Empty`

Issue: https://github.com/SpineEventEngine/core-jvm-compiler/issues/87
Branch: `claude/wonderful-galileo-jzz6le`

## Problem

CoreJvm enforces an *implicit* `(required)` rule for ID fields (first field) of
command messages and entity states. When such an ID field is typed as
`google.protobuf.Empty` (singular, `repeated`, or `map<_, Empty>`), the generated
required-check is unsatisfiable at runtime because every `Empty` equals the
default instance. The Validation Compiler already rejects the *explicit*
`(required)` case at compile time (validation#146); this follow-up adds the same
compile-time rejection for the *implicit* ID-field case.

## Reference (validation 2.0.0-SNAPSHOT.446)

`context/.../option/required/RequiredOption.kt`:
- `checkFieldIsNotEmpty(field, file)` → `Compilation.check(!field.type.refersToEmpty(), file, field.span) { msg }`
- private `FieldType.refersToEmpty()` (message / list / map value) + `TypeName.isProtobufEmpty`.

Error message (verbatim from the issue):
> The field `<f>` of type `<t>` cannot be marked as `(required)` because
> `google.protobuf.Empty` has no fields and its instances are always equal to the default value.

## Plan

All implicit-required ID reactions funnel through `RequiredIdReaction.withField`,
so the check lives there once (DRY). The two subclasses just pass `event.file`.

1. `base/.../field/RequiredIdReaction.kt`
   - add `file: File` param to `withField`;
   - after the explicit-`(required)` early return, call `checkFieldIsNotEmpty`;
   - add private `checkFieldIsNotEmpty`, `FieldType.refersToEmpty`, `TypeName.isProtobufEmpty`.
2. `entity/.../EntityStateIdReaction.kt` → `withField(field, event.file, ID_FIELD_MUST_BE_SET)`.
3. `signal/.../CommandTargetReaction.kt` → `withField(field, event.file, TARGET_ENTITY_ID_MUST_BE_SET)`.

## Tests

- base: add singular/repeated/map `Empty` fields to `farm.proto`; 3 unit tests in
  `RequiredIdReactionSpec` asserting `Compilation.Error` (covers the shared logic).
- entity: compile-fail fixture (entity state with `Empty` ID) + test via `acceptingOnly`;
  exclude the bad message from the normal full-pipeline runs.
- signal: compile-fail fixture (command with `Empty` first field) + test via `acceptingOnly`;
  exclude the bad message from the normal full-pipeline runs.

## Verify

Build the affected module tests (`:base:test`, `:entity-tests:test`, `:signal-tests:test`).
