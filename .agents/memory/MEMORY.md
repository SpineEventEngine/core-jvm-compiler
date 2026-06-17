# Team memory index

One line per memory. Scan at the start of every session.
See [README.md](README.md) for the format and routing rules.

## Feedback (validated patterns & corrections)

- [copilot-review-request](feedback/copilot-review-request.md) — GraphQL `requestReviews` with `botIds: ["BOT_kgDOCnlnWA"]`; REST endpoint silently no-ops on re-requests.
- [no-issue-references-in-code](feedback/no-issue-references-in-code.md) — Don't put issue/PR refs in code (comments, KDoc, fixtures); cross-link the issue and PR instead.

## Project (durable context & rationale)

- [windows-ci-needs-real-symlinks](project/windows-ci-needs-real-symlinks.md) — `tests/` build needs git symlinks checked out natively; never set `core.symlinks false` in Windows CI.
- [prototap-build-cache](project/prototap-build-cache.md) — Build-cache hits on `*-tests` protoc tasks drop the ProtoTap capture; rerun with `--no-build-cache` when `prototap/CodeGeneratorRequest.binpb` is missing.
- [igtest-stale-plugins](project/igtest-stale-plugins.md) — IgTests can silently run previously published plugins; clear stale artifact meta (`writeArtifactMeta --rerun`), build cache, and warm daemons before trusting results.
- [gradle-needs-utf8-locale](project/gradle-needs-utf8-locale.md) — Run Gradle with `LC_ALL=C.UTF-8`; the default POSIX locale makes `sun.jnu.encoding` ASCII and breaks expansion of dependency jars with non-ASCII entries (e.g. KSP).
- [one-pipeline-run-per-spec-class](project/one-pipeline-run-per-spec-class.md) — A `PluginTestSetup` spec can call `runPipeline` only once per class; the backend broker closes after a run, so a second call throws "FilterChain is already closed".

## Reference (external systems)

- [cache-warm-window](reference/cache-warm-window.md) — How prompt cache entries are shared between sibling-repo sessions and how to maximise overlap.
- [anthropic-api-caching](reference/anthropic-api-caching.md) — Pattern and pricing for adding prompt caching to any direct Anthropic API call.
