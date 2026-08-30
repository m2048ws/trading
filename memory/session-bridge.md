---
type: memory
updated: 2026-08-30
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0002-architecture-portfolio
- **RFC Revision**: 7b6f7a58f4dcbb8fb4bbdf3a8ba74ba66f222cce
- **Slice**: S-02-order-execution-scenarios
- **Issue**: 7 https://github.com/m2048ws/trading/issues/7
- **Change**: separate-order-and-execution-scenario-modules
- **Worktree**: /Users/m/src/money/.worktrees/separate-order-and-execution-scenario-modules
- **Phase at Checkpoint**: applying
- **Task Group at Checkpoint**: 7
- **Observed Run Revision**: 8
- **Last Verified HEAD**: 97bec2609af7e330739bc3d14c0c966d5a00c765

## Next Action
- Apply Task Group 8 (`Verification Evidence and Corgi Handoff`) after Task Group 7 acknowledgement.

## Blockers
- none

## Uncommitted Work
- none

## Discoveries
- The archived `establish-pure-instrument-economics` boundary was reconciled from commit `86613ee` before S-02
  production edits; the clean baseline passed 768 tests plus formatting on OpenJDK 26.0.2.
- Task Group 2 established the physical `trading-order-model` and `trading-execution-scenario` JAR boundaries; the
  clean repository matrix passed 770 tests including isolated completed-JAR ownership and forbidden-import guards.
- Task Group 3 established the dimension-indexed order instruction algebra and instruction-owned evidence/resolution
  constructors; the clean matrix passed 775 tests including exhaustive and impossible-shape compiler coverage.
- Task Group 4 established canonical intent/order construction, non-empty ordered violations, staged accumulating
  validation, and value-oriented convenience constructors; the clean matrix passed 774 tests including intent-forgery
  compiler guards and deterministic accumulating/fail-fast validation coverage.
- Task Group 5 established pure explicit slice construction, domain-owned non-empty matched slices, and one-order
  dependent assumptions; the clean matrix passed 774 tests including associated-shape, untyped-map, duplicate-field,
  empty-reconstruction, and public-signature boundary coverage.
- Task Group 6 replaced the scenario service with one-order branch-sensitive evaluation, closed deterministic
  diagnostics, and retained verified results; the clean matrix passed 778 tests including independent-branch,
  suppression, closed-location, stable-order, and removed-API coverage.
- Task Group 7 implemented checked exact-flat round trips over retained signed positions and completed transitional
  fee/risk consumption; the clean matrix passed 781 tests including long/short closure, unequal signed coordinates,
  cross-instrument legs/positions, downstream equivalence, completed-JAR ownership, and compiler boundaries.

## Promotion Queue
- Review agent-configuration constraints before promoting any item to `memory/MEMORY.md`.
