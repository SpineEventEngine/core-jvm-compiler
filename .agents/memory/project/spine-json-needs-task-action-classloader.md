---
name: spine-json-needs-task-action-classloader
description: Spine's `toJson()` fails in a Gradle provider evaluated for task inputs (context class loader is Gradle's, `desc.ref` not found); keep inputs as protos and render JSON in the task action.
metadata:
  type: project
  since: 2026-10-08
---

`io.spine.type.toJson()` initializes `KnownTypes` on first use, and `KnownTypes`
finds the `desc.ref` resources through the **thread context class loader**. Gradle
sets that loader to the plugin's class loader only while running a task action.
When a `provider { }` feeding an `@Input` is evaluated for input fingerprinting
(or for storing the configuration cache), the context class loader is Gradle's
`ant-and-gradle-loader`. `KnownTypes` then fails with `ExceptionInInitializerError`
— "Unable to find `desc.ref` via `ClassLoader` `VisitableURLClassLoader(ant-and-gradle-loader)`"
— and the class stays broken for the rest of that class loader's life.

**Why:** hit on 2026-10-08 while making `WriteCompilerPluginsSettings`
configuration-cache compatible: rendering the settings JSON inside the input
provider broke every TestKit build that ran the task, while unit specs passed,
because their test thread's context class loader can see `desc.ref`.

**How to apply:** for Gradle task inputs, pass the Protobuf messages themselves
(`Property`/`MapProperty` of `Message`; they are `java.io.Serializable`, which input
snapshotting and the configuration cache both handle). Call `toJson()` (or
anything else touching `KnownTypes`/`TypeRegistry`) only inside `@TaskAction`.
Building messages (including `Any` packing) at configuration time is fine.
Trust only TestKit/ig tests to catch this, not `ProjectBuilder` specs.

Related: [[igtest-stale-plugins]]
