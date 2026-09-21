---
name: copyright-header-migration
description: "Copyright headers are migrating `TeamDev` -> `CodeMatters, Lda.` repo by repo; `config` went first, the rest follow via `summit` waves — do not flag it as drift."
metadata:
  type: project
  since: 2026-09-21
---

Source file copyright headers are being changed across the Spine SDK from
`Copyright <year>, TeamDev. All rights reserved.` (with the long BSD-style
redistribution notice) to `Copyright <year> CodeMatters, Lda.` (with the short
standard Apache-2.0 notice). A matching IDEA profile,
`.idea/copyright/CodeMatters_Open_Source.xml`, ships alongside it.

The rollout is gradual and per-repository. `config` was the first repo to adopt
it, so the change reaches consumer repos through `./config/pull` — arriving as a
header rewrite across every config-distributed file at once (~230 files in
`core-jvm-compiler`). The remaining repositories are converted later through the
`summit` repo and its rollout waves.

**Why:** a licensing/ownership header rewrite landing inside a routine dependency
refresh looks exactly like unintended drift, and an agent reviewing the diff will
otherwise flag it or try to restore the old header. It is intentional and staged.

**How to apply:**

- Do not flag the `TeamDev` -> `CodeMatters, Lda.` header change as accidental,
  and do not revert it, in `config` or in any repo a wave has reached.
- Do not hand-migrate headers in a repo a wave has not reached yet — let
  `config`/`summit` deliver it, so every repo converts the same way.
- The `update-copyright` skill already skips config-distributed files in consumer
  repos; that stays correct — those headers are owned by `config`.
- A repo mid-rollout can legitimately show both styles: config-distributed files
  on the new header, repo-owned sources still on the old one.

Related: [[config-owned-buildsrc-reverts]].
