---
name: igtest-stale-plugins
description: ApiAnnotationsPluginIgTest can silently run previously published plugins — three staleness layers (artifact meta, build cache, warm daemons) must be cleared to test current code.
metadata:
  type: project
  since: 2026-06-11
---

The IgTest's nested TestKit build does not necessarily run the plugin code
from the working tree. The CoreJvm Compiler Gradle plugin resolves its
companion `core-jvm-plugins` fat jar at the version recorded in the jar's
own `META-INF/io.spine/io.spine.tools_core-jvm-plugins.meta` resource, and
three independent layers can pin that to a previously published version:

1. `writeArtifactMeta` (tool-base plugin) declares no task inputs, so it
   stays `UP-TO-DATE` across version bumps and keeps the old version in
   the meta resource. Remedy: `./gradlew :plugins:writeArtifactMeta --rerun`.
2. The Gradle build cache can restore `launchSpineCompiler` output that was
   generated with older plugins. Remedy: `--no-build-cache`.
3. Warm daemons memoize the lazily loaded meta (`LazyMeta` is a singleton
   `by lazy`), so even a corrected jar keeps resolving the old version
   until the daemon dies. Remedy: `./gradlew --stop` and delete
   `.gradle-test-kit/test-kit-daemon` (TestKit daemons are shared across
   IgTest runs via `withSharedTestKitDirectory()`).

**Why:** on 2026-06-11 a behavioral fix passed the in-JVM
`annotation-tests` suite but failed the IgTest: the nested build ran fat
jar `.070` while the tree was at `.072`. Diagnose by reading
`typedefs/build/spine/compiler/parameters/main.pb.json` in the nested
project — `userClasspath` names the fat jar actually used.

**How to apply:** when an IgTest contradicts the in-JVM suites, check the
`userClasspath` version first; clear the three layers above before
trusting the result. Root fix tracked for tool-base (`WriteArtifactMeta`
inputs). Related: [[prototap-build-cache]].
