---
name: no-issue-references-in-code
description: Don't put issue/PR references in code (comments, KDoc, fixtures); cross-link the issue and PR to each other instead.
metadata:
  type: feedback
  since: 2026-06-16
---

Source code — including comments, KDoc/Javadoc, and test fixtures — must not
carry bare issue or PR references such as `See issue #33`. Describe the
rationale inline so the code reads as self-contained.

**Why:** Issue numbers embedded in code rot over time and force readers to
leave the codebase to understand intent. Traceability already lives in the
issue ↔ PR link: let the issue reference the PR, or the PR reference the
issue — but not the code. Raised by @alexander-yevsyukov in review of PR #96.

**How to apply:** When documenting why a piece of code or a test fixture
exists, explain it in prose in place. Keep issue/PR cross-references in the PR
description or the issue thread, never in source files.
