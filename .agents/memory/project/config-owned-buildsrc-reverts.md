---
name: config-owned-buildsrc-reverts
description: "`./config/pull` overwrites `buildSrc/` from the `config` submodule, reverting local edits — never make this repo's build depend on members added to a config-owned dependency object."
metadata:
  type: project
  since: 2026-08-20
---

`./config/pull` re-copies the whole `buildSrc/` tree from the `config`
submodule (except `module.gradle.kts`, which it preserves). Any local edit
to a config-distributed file is silently reverted, on every machine that
runs the pull.

Two grades of damage:

- **Silent** — version constants. A pull rolled `Base` back `.441` → `.440`
  and `ToolBase` `.421` → `.420`, undoing committed branch work. The build
  still runs, just against older artifacts.
- **Hard** — API shape. Members *added* to a config-owned object vanish, so
  every build script calling them fails to compile. On 2026-08-20 a pull
  removed `CoreJvmCompiler.gradlePluginArtifact/gradlePluginLib()/fatJarLib()`
  added for the `plugins` module split, breaking the build outright.

**Why:** the consumer repo legitimately edits dependency versions under
`buildSrc/src/main/kotlin/io/spine/dependency/` (that is what the
`dependency-update` skill does), so those files drift ahead of `config`
until `config` catches up. The drift is expected; depending on *new API*
in those files is not — it converts a routine pull into a broken build.

**How to apply:**

- Never add members to a config-owned dependency object to serve this
  repo's own build. Declare the value locally instead — a module's own
  artifact ID goes in its `build.gradle.kts` as
  `val moduleArtifactId = "core-jvm-…"` (the convention `grpc`, `base`,
  and `gradle-plugin` follow), and a coordinate needed by a test-resource
  build script goes in as a literal string with a comment.
- Publishing a *new* artifact that other Spine repos must consume needs a
  follow-up PR to `config`, adding it to the shared dependency object.
- After running `./config/pull`, always `git status` and diff `buildSrc/`:
  re-apply any version bumps it rolled back before committing.

Related: [[projectbuilder-in-memory-caches]].
