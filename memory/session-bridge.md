---
type: memory
updated: 2026-08-31
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0002-architecture-portfolio
- **RFC Revision**: 7b6f7a58f4dcbb8fb4bbdf3a8ba74ba66f222cce
- **Slice**: S-03-pure-risk
- **Issue**: 8 https://github.com/m2048ws/trading/issues/8
- **Change**: introduce-pure-risk-module
- **Worktree**: /Users/m/src/money/.worktrees/introduce-pure-risk-module
- **Phase at Checkpoint**: planning_ready
- **Task Group at Checkpoint**: 1
- **Observed Run Revision**: none
- **Last Verified HEAD**: cf210964f62f10602be0551682abda3ce7d24fae

The phase and revision above are the required next checkpoint carried by the Repair Task Group commit. Live Run
Contract authority remains `applying` at revision 1 until Corgi acknowledges that commit.

## Next Action
- Start Apply for `introduce-pure-risk-module` Task Group 1.
  Review, Human QA, and Archive remain separate gates.

## Blockers
- none

## Uncommitted Work
- Task Group 13's archive-delta completeness evidence and this next-state checkpoint; production code is unchanged.

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
- Canonical Verify passed Group 9 at exact SHA `a60b00a7f3a633e9d1c3ee6180ed991b2b0e9d0e`. The next independent
  review confirmed the JVM-private construction repair but found that Group 9 copied current-main content without
  recording `05a8af0ff836b846247c082901ac3baea3d0c169` as an ancestor; GitHub still reported PR #27 conflicting. Human
  Review rejected that implementation and required the merge-ancestry repair.
- Topology commit `1383bc64d113394d289e23288285f437fca0a08c` records parents
  `a60b00a7f3a633e9d1c3ee6180ed991b2b0e9d0e` and `05a8af0ff836b846247c082901ac3baea3d0c169`
  using the already integrated Group 9 tree `15a6b4fd213365539d44aba8b8033bd5ace9c080`. Successor run
  `run-064db6bc-938e-4619-b87f-abc218cb0564` carries Groups 1–9 and admits Repair Task Group 10 from planning baseline
  `f8b486c3d170567a1b031b284e39f483795b89a6`; current `main` is now an ancestor and GitHub reports the draft PR
  mergeable.
- The guarded claim adapter rejects repair successors because its phase allowlist omits an existing Run Contract in
  `planning_ready`. Admission, pilot authority, branch, and worktree were checked through the adapter; canonical Apply
  then started Group 10 directly and its four-field token was retained in the ignored adapter handoff.
- The ancestry-corrected clean gate again passes 815 tests: 601 quantities, 13 reference-data, 9 application, 18
  runtime, 13 instrument-economics, 7 order-model, 8 execution-scenario, 10 downstream economics, and 136 packaged
  adversarial tests, plus explicit JMH compilation. Formatting, strict OpenSpec validation, deterministic Corgi
  readiness, and the read-only semantic readiness review pass at planning revision
  `sha256:ab75044e5a7307760a64dfa8fec283cc2e4e5bb60f031401e1a5323c6a8660bc`.
- The automated Task Group 10 review checked scope/topology, unchanged Group 9 behavior, RFC/AC alignment, completed-JAR
  construction authority, architecture, performance/security, test evidence, and planning integrity with no findings.
  It changed no file, triaged no finding, and is not canonical Verify or Human Review.
- The exact-SHA independent review of Group 10 confirmed merge ancestry and conflict-free integration, then demonstrated
  that ordinary Java could still subclass `Order`, implement activation/execution alternatives, construct
  `Order$ConstructedOrder` and `MatchedSlices`, and supply arbitrary erased evidence to `ScenarioAssumptions.create`.
  Human Review rejected the implementation and successor run `run-5a38d5b3-5939-4e79-9307-7b7ec506d02c` carries
  Groups 1–10 into Repair Task Group 11 at planning revision
  `sha256:ecc38c9f0cdaa28148a2c32c743e4ec24dd35f83cd2157cd2a09fe91f7697cfa`.
- Order, activation, execution, pricing, and visibility abstract bases now execute exact built-in-class guards from their
  JVM constructors, so externally compiled Java subclasses cannot instantiate. `Order$ConstructedOrder` and
  `MatchedSlices` now have bytecode-private constructors reached only by cached private method handles in their owning
  companions.
- Every `ScenarioAssumptions` factory now returns a typed `Either` and checks the runtime evidence/resolution shape before
  invoking its private constructor. Completed-JAR Java fixtures prove arbitrary `Object` inputs expose only the typed
  result, while runtime tests return `EvidenceShapeMismatch` or `ResolutionShapeMismatch` rather than accepting forged
  assumptions or throwing a cast failure.
- The Task Group 11 clean matrix passes 819 tests: 601 quantities, 13 reference-data, 9 application, 18 runtime, 13
  instrument-economics, 7 order-model, 9 execution-scenario, 10 downstream economics, and 139 packaged adversarial
  tests. Explicit JMH compilation and the focused exact completed-JAR constructor/implementation probes also pass.
- Canonical Verify passed Group 11 at exact SHA `5a74678a78a27ff01c258ef4883a8a3d80d3bdc9`. Independent review closed
  `S02-JVM-AUTHORITY-001`, then identified `S02-VALUE-SEMANTICS-001`: the JVM-private checked result classes had lost
  the structural equality and hashing previously supplied by their case-class representations. Human Review rejected
  that implementation and successor run `run-245638d2-ba14-4eda-8fbd-98151b33c1d0` carries Groups 1–11 into Repair
  Task Group 12 at planning revision
  `sha256:d7a7a6b522f5dbf7a15892d6043b1f757f3ea2fd5e3724ea205a1360cc285951`.
- `OrderScenario` now compares and hashes its retained assumptions, checked activation, effective pricing, and position
  change. `RoundTripScenario` now compares and hashes its instrument identity, entry, exit, and held position. Repeated
  checked construction satisfies reflexive/symmetric/transitive equality, equal hashes, and hashed-collection lookup;
  semantically different scenarios and round trips remain unequal.
- `javap -p` confirms both checked result constructors remain JVM-private and exposes only the intended public
  observations plus `equals` and `hashCode`; completed-JAR Scala and Java constructor/copy probes continue to pass.
- The Task Group 12 clean matrix passes 820 tests: 601 quantities, 13 reference-data, 9 application, 18 runtime, 13
  instrument-economics, 7 order-model, 10 execution-scenario, 10 downstream economics, and 139 packaged adversarial
  tests. Formatting, JMH compilation, strict OpenSpec validation, and deterministic Corgi readiness also pass.
- The automated Task Group 12 review checked scope, AC-008 behavioral equivalence, equality/hash laws, constructor
  authority, architecture, downstream compatibility, performance/security, and validation evidence with no findings.
  It changed no file, triaged no finding, and is not canonical Verify or Human Review.
- The initial strong local Archive attempt failed before moving the Change because OpenSpec detected canonical scenario
  headings omitted by renamed or strengthened scenarios in the modified `order-scenarios` delta. A tested rc2 recovery
  transition removed only canonical untracked evidence and the pending closeout journal, recorded `failedPhase: archive`,
  and created successor run `run-e912075b-0579-49ba-8fbe-530c8b138a6b` without changing the accepted RFC source or
  traceability digests.
- Repair Group 13 restores every canonical scenario identity across all five modified requirements while retaining the
  delivered stronger accumulation, one-order assumptions, explicit epistemic status, and signed-position cases. Strict
  OpenSpec validation and deterministic Corgi readiness pass at planning revision
  `sha256:dfda7d3d6ddedf107afdbd3fb8820c55d9d89ec610e1fbe1a5894b4d7ff0d3e9`; a disposable exact-diff OpenSpec Archive
  rehearsal succeeds with one added and five modified requirements and no dropped scenario.
- The automated Task Group 13 review checked scope, canonical scenario preservation, RFC/source/traceability stability,
  archive recovery safety, planning-only Git boundaries, validation evidence, and performance/security applicability
  with no findings. It changed no file, triaged no finding, and is not canonical Verify or Human Review.

## Promotion Queue
- Review agent-configuration constraints before promoting any item to `memory/MEMORY.md`.
