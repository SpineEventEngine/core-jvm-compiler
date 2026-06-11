---
name: prototap-build-cache
description: Build-cache hits on protoc tasks of `*-tests` modules drop the ProtoTap capture; rerun with `--no-build-cache` when `prototap/CodeGeneratorRequest.binpb` is missing.
metadata:
  type: project
  since: 2026-06-11
---

In `*-tests` modules (e.g. `annotation-tests`), `PluginTestSetup` loads
`prototap/CodeGeneratorRequest.binpb` from test resources. ProtoTap writes
that capture as a side effect of the protoc run without declaring it as a
task output, so a Gradle build-cache hit on the proto generation task
restores the generated Java but not the capture. Every spec in the module
then fails in `@BeforeAll` with
``Unable to find `prototap/CodeGeneratorRequest.binpb` ``.

**Why:** observed on 2026-06-11: `:annotation-tests:clean
:annotation-tests:build` failed all specs after a cache hit; the identical
build with `--no-build-cache` passed.

**How to apply:** after `clean` of a `*-tests` module — or whenever the
`.binpb` resource error appears — rerun the build with `--no-build-cache`.
The root fix belongs in the ProtoTap plugin: declare the capture files as
task outputs.
