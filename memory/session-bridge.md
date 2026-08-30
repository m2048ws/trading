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
- **Phase at Checkpoint**: planning_ready
- **Task Group at Checkpoint**: 10
- **Observed Run Revision**: none
- **Last Verified HEAD**: 1383bc64d113394d289e23288285f437fca0a08c

The phase and revision above are the required next checkpoint carried by the Repair Task Group commit. Live Run
Contract authority remains `applying` at revision 1 until Corgi acknowledges that commit.

## Next Action
- Start Apply for `separate-order-and-execution-scenario-modules` Task Group 10.
  Verify remains a separate gate.

## Blockers
- The current-main integration, JVM construction-boundary repair, completed-JAR Scala/Java fixtures, documentation,
  validation evidence, and this Task Group 9 checkpoint.

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
- Task Group 8 completed dependency-order compilation, baseline fixture comparison, packaged API/import inspection,
  strict planning/source/traceability validation, and order/scenario property laws; the final clean matrix passed 785
  tests and the remediated automated Task Group review returned no findings.
- The rejected predecessor run's independent review identified JVM-public Scala package-private constructors and a
  19-path conflict with current `main`; successor run `run-416e6504-0ba5-4cf9-9978-962b120ecb7f` carries Groups 1–8
  and admits Repair Task Group 9 without rewriting their acknowledged commits.
- Repair Group 9 squash-integrates current `main` at `05a8af0ff836b846247c082901ac3baea3d0c169`, preserving S-01's
  application/runtime, JDK 25, dependency, workflow-retirement, and archived-delivery changes while retaining S-02's
  order/scenario modules and migrated fee/risk consumers; all 19 conflicts were resolved by responsibility owner.
- `OrderIntent`, checked activation and trigger/peg evidence, `LiquiditySlice`, `ScenarioAssumptions`, `OrderScenario`,
  and `RoundTripScenario` now have JVM-private constructors. Their typed factories retain construction authority via
  cached private method handles, and value semantics remain explicit where case-class generation was removed.
- Completed-order/scenario-JAR fixtures reject same-package Scala and Java constructor/copy bypasses while the existing
  positive instruction, assumptions, scenario, round-trip, fee, and risk paths continue to compile and execute.
- The final clean dependency-ordered gate passes 815 tests: 601 quantities, 13 reference-data, 9 application, 18
  runtime, 13 instrument-economics, 7 order-model, 8 execution-scenario, 10 downstream economics, and 136 packaged
  adversarial tests. Explicit JMH compilation, formatting, strict OpenSpec validation, and deterministic Corgi
  readiness at planning revision `sha256:316fa4786876ba4cb9a506ebd3bdf711f938bdee3ee87ac4c6a1f7efbb4e97b6`
  also pass.
- The automated Repair Task Group review caught and remediated lost `CheckedActivation` value equality, then completed
  its scope, behavior, architecture, artifact-boundary, performance/security, and evidence axes with no findings. It
  changed no file during the final pass, triaged no finding, and is not canonical Verify or Human Review.

## Promotion Queue
- Review agent-configuration constraints before promoting any item to `memory/MEMORY.md`.
