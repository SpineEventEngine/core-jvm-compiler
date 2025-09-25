# About tests in McJava

There are several levels of testing in this project.

1. Regular unit tests.
2. Integration tests based on `io.spine.tools.gradle.testing.GradleProject` API residing under
   the corresponding module.
3. Functional tests based on [`PipelineSetup`][pipeline-setup] API placed in separate
   test-only modules. Please read the section below for details about these tests.
4. Integration tests under the `tests` subproject.
   These tests are [documented](tests/README.md) in the project.

## About `*-tests` modules

A modules with the `-tests` suffix in their names is a test-only module for the module which
has the name before the suffix. For example, `mc-java-signal-tests` is the test module for
`mc-java-signal`

We need to have separate test-only modules because of the following reasons.

 * We want to run code generation tests using the vanilla Protobuf code placed into
resources by [ProtoTap][prototap] using [`PipelineSetup`][pipeline-setup] API.

 * We need this code to be "vanilla" Protobuf — not the one produced by the previous
version of McJava — because the Compiler plugins run on top of such code.

 * We use `testFixtures` source set for storing input proto files for the code generation tests.

 * It is not possible to apply a Gradle plugin to a source set. Plugins are applied to a project.

 * The module `mc-java-signal` needs McJava plugin for generating events and entity states for
the Signal Compiler plugin. Once McJava plugin is applied to a project, it serves all 
the source sets, including `testFixtures`, making the generated code enhanced with Spine features,
which we need to avoid.

Therefore, to have "vanilla" Protobuf code generated for our stub types, we need to have
a separate Gradle project

[prototap]: https://github.com/SpineEventEngine/ProtoTap
[pipeline-setup]: https://github.com/SpineEventEngine/compiler/blob/master/testlib/src/main/kotlin/io/spine/compiler/testing/PipelineSetup.kt
