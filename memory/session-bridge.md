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
- **Phase at Checkpoint**: awaiting_verify
- **Task Group at Checkpoint**: 10
- **Observed Run Revision**: 2
- **Last Verified HEAD**: 43db8d83c57fe6f2d2d3bd1c09f3d3b6d5ee1e78

The phase and revision above are the required post-acknowledgement checkpoint for successor Run Contract
`run-133327d8-35ce-46a2-9c8e-c82ab46f82c9`. Live Run Contract state remains execution authority.

## Next Action
- Sync draft PR #28, then rerun the separate canonical whole-change Verify gate. Human Review, Human QA, and Archive
  remain later separate gates.

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
- S-03 Propose remains strict-ready at planning revision
  `sha256:7d49085118e7b0083c8262dd38e09f1e796325df9e738cbe2b5513f352ed33c1`; Issue #8, accepted RFC revision,
  source digest, traceability digest, and the archived instrument-economics prerequisite are current.
- Fresh Run `run-a29691ac-e873-47ae-aa50-f27dff8c0381` uses planning baseline
  `9d9b370f1c55fe8fe8ca2a1eab60a8a423323a1a`, whose sole parent is current `origin/main`
  `cf210964f62f10602be0551682abda3ce7d24fae`. The discarded unacknowledged Run remains recoverable under
  `/private/tmp/s03-run-restart.TLy1JE`.
- The reconciled baseline passes formatting, clean compilation and 820 tests, completed-JAR/adversarial boundaries,
  and explicit JMH compilation on OpenJDK 26.0.2, SBT 1.12.15, and Scala 3.8.4.
- Existing sizing uses divide into fixed affine models, a deliberately non-monotone exhaustive case, and a located
  callback-failure case. No arbitrary callback or fee/scenario builder is eligible for implicit monotone certification.
- Task Group 1 is acknowledged at `a2ad478de1353312bfe56c91a3f16bae9a272ee0`; draft PR #28 tracks the WIP branch.
- The non-empty `trading-risk` artifact now owns focused PnL identity failures and depends only on quantities,
  instrument economics, and pure Cats Core. Its completed-JAR classpath rejects order/scenario/policy/runtime, effects,
  streams, codecs, persistence, telemetry, and JMH; lower-layer classpaths reject risk imports.
- The focused Group 2 gate passes risk compilation and 41 completed-JAR/reverse-dependency tests. Transitional
  `trading-economics` remains until Group 8 moves every production/test surface to an intentional owner.
- Task Group 2 is acknowledged at `d01129c30691a0d978beb36eefe71bd7b9fcb623` and draft PR #28 is synchronized.
- `Risk.downside` validates ordinary instrument identity before typed PnL inspection, then uses exact quantity order,
  zero, subtraction, and nonnegative refinement. Negative fractional, zero, positive, foreign-identity, and static
  dimension-mismatch coverage passes without quantization or floating-point conversion.
- The temporary policy/scenario service is explicitly `TransitionalRisk`; it accepts only refined nonnegative budgets,
  delegates downside to the pure artifact, and no longer exposes `InvalidRiskBudget` or raw sign validation. The focused
  Group 3 gate passes 4 risk, 10 downstream economics, and 40 completed-JAR/compiler tests.
- Task Group 3 is acknowledged at `8bf1ad6da61fd3f91f81aae85ecc543a8603e4a1` and draft PR #28 is synchronized.
- `LotRiskAssessment` derives downside from coherent instrument/lots/PnL inputs, while `MonotoneLotRisk` captures one
  identity, position/settlement dimensions, positive cap, and library-owned total domain evaluator. The initial lawful
  constructor admits only one checked coordinate; Group 5 adds closed curve constructors.
- Independent model identity, dimension, coverage, duplicate/missing coordinate, breakpoint, marginal, boundary, and
  composition failures have domain-owned typed variants and accumulate through `ValidatedNec` into a deterministic
  non-empty public collection. Assessment, model, and violation-collection constructors are JVM-private via cached
  private method handles; the focused gate passes 9 risk and 7 completed-JAR/spoof tests.
- Affine, checked piecewise, compatible addition/minimum/maximum, floor/ceiling quantization, and complete-table
  construction now produce compact exact monotone models. Generated laws cover totality, exactness, identity, and each
  monotone closure; table validation checks every row and adjacent risk in explicit `O(cap)` work.
- Retained construction instrumentation proves algebraic work follows expression nodes and explicit breakpoints rather
  than the declared cap: an exact composed model with cap `10^100` records three nodes, no breakpoints, and no table
  rows. The focused Group 5 gate passes 23 risk tests, 7 completed-JAR/compiler tests, and repository formatting.
- Boundary-certified primary sizing now returns a closed no-affordable/selected decision with retained one-lot,
  selected, cap/next-unaffordable, and complete distinct probe evidence. Generated exhaustive comparison, cap-one,
  plateau, inverse-shaped, and `10^100`-cap tests pass within `2 + ceil(log2(cap))` observations.
- The non-published JMH run at cap 1,024 on JDK 26.0.2 measured roughly 5.02M direct lookups/s, 368K boundary searches/s,
  and 3.75K benchmark-local exhaustive evaluations/s with one fork, three warmups, and five measurements.
- Task Group 6 is acknowledged at `65915625e0783aa51e599dcbd8d11b4339e6c7e6`; draft PR #28 is synchronized.
- The explicitly named exhaustive fallback now traverses arbitrary pure lot-to-PnL evaluations in ascending order,
  retains constant successful state, preserves the exact first failed coordinate and typed cause, and exposes a
  complete-range decision shape with no monotone adjacency or model conversion. The focused Group 7 gate passes 40
  risk tests and 8 completed-JAR/compiler-boundary tests, including unknown Java alternative rejection.
- Task Group 7 is acknowledged at `176654ae34e44b9414aec61824345f5596417587`; draft PR #28 is synchronized.
- The obsolete policy-owned risk wrapper and universal sizing errors are deleted. Fixed fee-inclusive inputs now earn
  checked monotone tables before primary sizing, while arbitrary/non-monotone scenario evaluation deliberately uses
  the located exhaustive fallback. Fee policy moves unchanged to `trading-fee-policy`, risk stays independent, and the
  retired `economics/` directory/artifact is absent. The focused migration gate passes 40 risk, 11 fee-policy
  integration, and 106 completed-JAR/compiler tests plus JMH compilation; the full repository aggregate passes 869
  tests including all 147 adversarial boundary tests.
- Task Group 8 is acknowledged at `c01b94c496f51dd3ce15b4d389fc6e01486d99b2`; draft PR #28 is synchronized.
- The final clean dependency-ordered matrix passes 869 tests: 601 quantities, 13 reference data, 9 application, 18
  runtime, 13 instrument economics, 40 risk, 7 order model, 10 execution scenario, 11 fee-policy/integration, and 147
  completed-JAR/compiler/adversarial tests. Formatting and explicit JMH compilation pass.
- The focused cap-1,024 JMH run records roughly 5.76M direct lookups/s, 346K boundary-certified searches/s, and 3.36K
  exhaustive reference evaluations/s. Packaged API, source import, retired-name, fallback, and dependency audits find
  no boundary drift.
- Strict deterministic Corgi readiness and the read-only cross-artifact semantic review pass with no findings at
  planning revision `sha256:7d49085118e7b0083c8262dd38e09f1e796325df9e738cbe2b5513f352ed33c1`.
- The automated Task Group 9 review checked scope, RFC/AC coverage, validation evidence, packaged boundaries,
  architecture, performance/security applicability, planning integrity, and gate separation with no findings. It
  changed no file during the final pass, human-triaged no finding, and is not canonical Verify or Human Review.
- The first strong local Archive attempt failed because OpenSpec treats a `MODIFIED` requirement as a complete
  replacement and identifies scenarios by heading; renamed and omitted canonical headings therefore appeared to be
  scenario deletions even though the delivered runtime behavior was unchanged. The original strict validation and
  readiness checks validated the delta in isolation and did not simulate its eventual merge into canonical specs.
- The tested rc2 `archive --request-repair` transition removed only the untracked generated evidence and pending
  archive journal, recorded `failedPhase: archive`, and moved predecessor run
  `run-a29691ac-e873-47ae-aa50-f27dff8c0381` to `repair_required`. Its three recovery-specific tests pass; the retained
  pre-recovery loop backup is `/private/tmp/s03-archive-recovery.UMXbFk/introduce-pure-risk-module-loop`.
- Successor run `run-133327d8-35ce-46a2-9c8e-c82ab46f82c9` carries acknowledged Groups 1–9 unchanged and admits only
  Repair Task Group 10 from planning baseline `43db8d83c57fe6f2d2d3bd1c09f3d3b6d5ee1e78`. The accepted RFC source digest
  remains `sha256:1a8cca449f8fdfc009f8ccbe87989a288ea92e7ff41ee58d38aa513a53ed4f2d` and traceability digest remains
  `sha256:caaafeb6cd65026adcb4ea8f5f33d561ffce0ba8e6699d2301b57a5c510c4ab6`.
- Repair Group 10 restores every canonical scenario identity across all three modified `position-risk-sizing`
  requirements while retaining the delivered stronger typed/refined, explicit-failure, deterministic-traversal, and
  bounded-scope semantics. Strict OpenSpec validation and deterministic Corgi readiness pass at planning revision
  `sha256:39af122fd835f55b994cd903dd6dd402e45ddec5b28d8710f9134964e6fde5ae`; a disposable OpenSpec Archive rehearsal at
  `/private/tmp/s03-archive-rehearsal.MqlZNn/repo` succeeds with four added, three modified, and two removed requirements.
- The automated Task Group 10 review checked scope, canonical scenario preservation, RFC/AC alignment, planning
  integrity, architecture, and performance/security applicability with no findings. It changed no file, triaged no
  finding, and is not canonical Verify or Human Review.

## Promotion Queue
- Review agent-configuration constraints before promoting any item to `memory/MEMORY.md`.
