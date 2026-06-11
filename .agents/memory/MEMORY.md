# Team memory index

One line per memory. Scan at the start of every session.
See [README.md](README.md) for the format and routing rules.

## Feedback (validated patterns & corrections)

- [copilot-review-request](feedback/copilot-review-request.md) — GraphQL `requestReviews` with `botIds: ["BOT_kgDOCnlnWA"]`; REST endpoint silently no-ops on re-requests.

## Project (durable context & rationale)

- [windows-ci-needs-real-symlinks](project/windows-ci-needs-real-symlinks.md) — `tests/` build needs git symlinks checked out natively; never set `core.symlinks false` in Windows CI.
- [prototap-build-cache](project/prototap-build-cache.md) — Build-cache hits on `*-tests` protoc tasks drop the ProtoTap capture; rerun with `--no-build-cache` when `prototap/CodeGeneratorRequest.binpb` is missing.

## Reference (external systems)

- [cache-warm-window](reference/cache-warm-window.md) — How prompt cache entries are shared between sibling-repo sessions and how to maximise overlap.
- [anthropic-api-caching](reference/anthropic-api-caching.md) — Pattern and pricing for adding prompt caching to any direct Anthropic API call.
