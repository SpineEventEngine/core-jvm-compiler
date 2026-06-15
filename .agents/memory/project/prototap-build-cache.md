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

**How to apply:** the root `build.gradle.kts` now disables build caching
for `GenerateProtoTask`s in modules applying ProtoTap (see the
`subprojects` block), so the capture is always produced. If the error
still appears, check that the module applies the `prototap` plugin and
that the guard is in place.

**Root fix:** implemented in ProtoTap 0.15.0 (branch
`support-gradle-build-cache`, 2026-06-11): the Gradle plugin declares the
tapped files (`CodeGeneratorRequest.binpb`, `CodeGeneratorRequest.pb.json`,
`CompiledProtoFiles.txt`) as `GenerateProtoTask` outputs, so build-cache
hits restore them. Once this repo depends on ProtoTap ≥ 0.15.0, remove
the root-build guard and delete this memory.
