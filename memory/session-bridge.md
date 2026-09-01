---
type: memory
updated: 2026-09-01
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0002-architecture-portfolio
- **RFC Revision**: 7b6f7a58f4dcbb8fb4bbdf3a8ba74ba66f222cce
- **Slice**: S-05-versioned-boundary-codecs
- **Issue**: 10 https://github.com/m2048ws/trading/issues/10
- **Change**: introduce-versioned-boundary-codecs
- **Worktree**: /Users/m/src/money/.worktrees/introduce-versioned-boundary-codecs
- **Phase at Checkpoint**: applying
- **Task Group at Checkpoint**: 6
- **Observed Run Revision**: 7
- **Last Verified HEAD**: 3d3dd8aa25343d7efbc86b4b26706222311548dd

The phase and revision above are the required next checkpoint carried by Task Group 6. Live Run Contract authority
remains `applying` at revision 6 until Corgi acknowledges that commit.

## Next Action
- Commit and acknowledge `introduce-versioned-boundary-codecs` Task Group 6, synchronize its draft PR, then implement
  Task Group 7. Verify, Human Review, Human QA, and Archive remain separate gates.

## Blockers
- none

## Uncommitted Work
- Task Group 6's catalog-journal/replay implementation note and this next-state checkpoint, together with its frozen
  V1 codec, pure sequential replay, typed failures, and model/property/compiler/adversarial tests.

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
- The first S-04 local Archive attempt failed before moving the Change because three `MODIFIED` fee-inclusive-PnL
  requirements omitted 13 canonical scenario headings. OpenSpec treats each modified requirement as a complete
  replacement, while the former strict validation and readiness path validated the delta only in isolation.
- The guarded project-local recovery runtime removed only generated untracked Change evidence and the pending
  pre-closeout journal, recorded `failedPhase: archive`, and moved predecessor run
  `run-f72899aa-a667-4f01-8c8c-22d2fc1d784a` to `repair_required`. Successor run
  `run-fe0e73ca-02b9-4f73-a9c9-5b8c5e5f8171` carries acknowledged Groups 1–9 unchanged and admits only Repair Group
  10 from planning baseline `2b8b58f49587885778f5c41bbf207c01281433e4`.
- Repair Group 10 restores all 13 canonical scenario identities across signed fees, exact fee-inclusive PnL, and
  explicit PnL scope while retaining the stronger S-04 policy, attribution, conversion, and error-accumulation
  semantics. Strict OpenSpec validation and deterministic Corgi readiness pass at planning revision
  `sha256:96b5b08c7943f4116de55100e56efd0ca8e65d7e707a1b74c3e51bc7480c7234`.
- A disposable exact-diff OpenSpec Archive at `/private/tmp/s04-archive-rehearsal.UifirS/project` succeeds with three
  added, four modified, and one removed requirement; all 14 resulting canonical specs validate strictly and all 13
  repaired scenario identities remain present. Production Scala, tests, and build configuration are unchanged.
- The automated Repair Group 10 review checked scope, canonical scenario preservation, RFC/source/traceability
  stability, planning-prefix immutability, Archive recovery safety, architecture, performance/security applicability,
  validation evidence, and gate separation with no findings. It changed no file and is not canonical Verify or Human
  Review.
- S-04 Propose was reconciled against delivered S-02/S-03 and current `main` before production edits. It remains
  strict-ready at planning revision `sha256:b2226bdaf8f713cf48bb49f599da6b5cd84462cb96d8f028a6263caed76c4158`
  with source digest `sha256:d7face4865eb2e8e69abb4f305d67720842e19f9d1d4fa3b760b744fb8f06443`, traceability
  digest `sha256:8ef137a161b86ab60b78dadf55c70e91b8727572212054d1d344ec878057f064`, and authoritative
  GitHub Issue #9. The accidental duplicate Issue #31 is tombstoned and closed; named recovery stashes and
  `/private/tmp/corgi-s04-propose-recovery.YHbbq3` remain retained until delivery closeout makes removal safe.
- Fresh Run `run-f72899aa-a667-4f01-8c8c-22d2fc1d784a` uses planning baseline
  `cb1b7109257e7ddf3fa29fb0051862971352f2fd`, whose sole parent is current `origin/main`
  `0b7cc676054f8b9680174fce84f280d863e1a409`. The guarded adapter confirmed the closed native prerequisite graph,
  expected branch, registered worktree, and enabled local-commit/draft-PR authority.
- The reconciled fee-policy baseline passes both formatting gates, clean dependency-order compilation, all 869 tests,
  147 completed-JAR/adversarial boundary tests, and explicit JMH compilation on OpenJDK 26.0.2, SBT 1.12.15, and
  Scala 3.8.4.
- The current inventory keeps fee values and core PnL in instrument economics, moves fee-independent scenario price
  normalization to execution scenario, finalizes policy/directive/assessment/fee-inclusive orchestration in fee policy,
  and leaves pure risk unchanged. No application/runtime fee call site or live policy concern exists.
- Task Group 1 is acknowledged at `e085160202b88a9e2fa4f1589b29eb4260beae93`; draft PR #32 tracks the WIP branch.
- The retained `feePolicy` project now has the accepted explicit quantities, instrument-economics, order-model, and
  execution-scenario production edges; only its tests see risk. Its package root is `trading.fee`, with the provisional
  `trading.fee.policy` API intentionally unchanged until the semantic Task Groups.
- Dedicated completed-JAR classpaths prove quantities, reference data, instrument economics, order model, and execution
  scenario cannot access fee policy. A fee-policy-only client compiles and runs without risk, application, runtime,
  effects, streams, codecs, persistence, telemetry, benchmarks, or the retired aggregate. The focused gate passes 11
  fee-policy tests and all 151 adversarial/compiler tests plus both formatting checks.
- Task Group 2 is acknowledged at `d59b081b4c2fed6e2ee7d6e25fc7707803fa1773`; draft PR #32 is synchronized and Run
  revision 3 admits only Group 3.
- `trading.fee` now owns opaque nominal `FeeRate` and total exact `FeeCalculation` formulas over refined typed
  quantities. Percentage scales the existing `NonNegative[Quantity[D]]`; minimum adjustment preserves rebates, zero,
  and sufficient charges while returning a typed negative minimum only for smaller charges. Core denomination
  construction remains the sole per-component quantization boundary.
- The focused Group 3 gate passes 17 fee-policy example/property/integration tests and all 152 completed-JAR/compiler/
  adversarial tests. External compilation rejects raw bases and raw rates, lower-layer JAR checks reject the full
  `trading.fee` root, and the per-component control preserves exact amount-plus-residual conservation.
- The automated Group 3 review tightened the negative compiler assertion to bind the second rejection explicitly to
  nominal `FeeRate`, then completed scope, behavior, refinement, dimension, architecture, packaged-boundary,
  performance/security, evidence, and checkpoint axes with no findings. It changed no file during the final pass,
  triaged no finding, and is not canonical Verify or Human Review.
- Task Group 3 is acknowledged at `958899c0b40eb3bac8b0d9e96b5d73e976e1f892`; draft PR #32 is synchronized and Run
  revision 4 admits only Group 4.
- The `trading.fee` root now exposes covariant pure `FeePolicy`, domain-owned non-empty `PolicyErrors`, nominal
  nonnegative `SliceIndex`, and existential `FeeDirective`. Checked composition accumulates foreign identities and
  policy failures stably, normalizes nested/no-fee components, and preserves caller-owned typed causes without a global
  monoid, effect parameter, exception, or string erasure.
- The focused Group 4 gate passes 24 fee-policy tests and all 153 completed-JAR/compiler/adversarial tests. Packaged
  guards reject `FeeSchedule`, the old service class, unlawful algebra/effect/error-erasure calls, and arbitrary source
  markets; public policy bytecode contains no validation-library, monoid, effect, stream, or clock reference.
- The automated Group 4 review hardened every JVM-visible private policy implementation constructor and preserved flat
  checked composition across error mapping, then repeated formatting, 24 fee-policy tests, all 153 adversarial tests,
  source/package scans, and bytecode inspection with no remaining findings. The clean final pass changed no file,
  triaged no finding, and is not canonical Verify or Human Review.
- Task Group 4 is acknowledged at `031c233c756d77483286229ad9e7a7f6d9d58d57`; draft PR #32 is synchronized and Run
  revision 5 admits only Group 5. The SSH hardware-backed key refused noninteractive signing during synchronization, so
  the guarded adapter used the already-authorized GitHub CLI credential through invocation-local Git configuration;
  no repository or global Git configuration changed.
- Canonical `FeeAssessment` gates scenario/policy identity before evaluation, retains typed policy failures with stable
  ordinals, and accumulates every directive fee, denomination, and index violation before constructing output.
  `ScenarioFees` owns the target scenario once, and each existential `AssessedFee` retains the actual indexed immutable
  slice selected centrally.
- `ScenarioFees` and the concrete assessed-fee representation have JVM-private constructors. Equal-looking duplicate
  slices retain exact coordinate-selected references, heterogeneous fee assets remain typed, and invalid input suppresses
  policy execution. The provisional PnL bridge now consumes assessment; `FeeLine`, caller-selected markets, reference
  equality reconciliation, and the universal `FeePolicyError` hierarchy are absent.
- The focused Group 5 gate passes 31 fee-policy tests and all 155 completed-JAR/compiler/adversarial tests, including 42
  economics/fee-policy boundary tests. Both formatting gates, source audits, and bytecode constructor inspection pass.
- The automated Group 5 review checked scope, AC-015, staged suppression, stable accumulation, typed-cause provenance,
  honest error products, exact scenario ownership, JVM construction authority, migration, architecture, complexity,
  security applicability, evidence, and checkpoint integrity with no findings. It changed no file, triaged no finding,
  and is not canonical Verify or Human Review.
- Task Group 5 is acknowledged at `a02ef16497214ebb0dc4618be5e03c4078d5ca3d`; draft PR #32 is synchronized and Run
  revision 6 admits only Group 6.
- `ScenarioValuation` now owns fee-independent exact round-trip price normalization in execution scenario. It derives
  typed slice positions through order intent, values every slice at its retained market state, and folds exact typed
  settlement cashflows into core `PricePnl`; the fee-policy bridge delegates instead of duplicating the calculation.
- `RoundTripLeg` closes entry/exit attribution and focused scenario-valuation failures retain leg, index, and typed core
  causes. `Side` exposes no raw sign member to Scala or same-package Java completed-artifact consumers.
- The focused Group 6 gate passes formatting, 7 order-model tests, 16 execution-scenario tests, 31 fee-policy tests, and
  all 158 completed-JAR/compiler/adversarial tests. The 45-test economics boundary suite includes a standalone
  scenario-normalization client plus Scala and same-package Java removed-API guards.
- The automated Group 6 review expanded the packaged purity assertion across every generated scenario-valuation class
  and case-insensitively rejects fee, quantization, catalog, effect, stream, average, and scalar-coefficient symbols.
  The remediated final pass found no findings, changed no file, triaged no finding, and is not canonical Verify or Human
  Review.
- Task Group 6 is acknowledged at `0060e65f5bf4174fab0542a919007a825271d57f`; draft PR #32 is synchronized and Run
  revision 7 admits only Group 7.
- `RoundTripFeePolicies` now records explicit entry/exit policy selections, and `FeeInclusivePnl` owns the pure staged
  identity, scenario price, assessment, selected-slice conversion, and core PnL composition boundary.
- Generic non-empty fee-inclusive violations retain closed identity locations, scenario causes, per-leg assessment
  causes, directive/slice conversion causes, and core causes. A successful leg converts independently beside another
  leg's policy failure, so every eligible independent failure is retained deterministically.
- Successful fee-inclusive results and attributed contribution values have JVM-private constructors. They retain the
  round trip, both exact scenario assessments, entry/exit and slice provenance, original fees, core contributions, and
  one core PnL whose price, fee, and net totals are projected rather than duplicated.
- The focused Group 7 gate passes both formatting checks, 38 fee-policy tests, and all 158 completed-JAR/compiler/
  adversarial tests. The 45-test economics boundary suite compiles and runs fee-inclusive PnL through only the pure
  production graph and rejects caller construction of final attribution and scenario-level PnL.
- The automated Group 7 review checked scope, AC-016, branch eligibility, stable accumulation, typed provenance,
  selected-slice conversion, core-total ownership, replay equality, JVM authority, dependency purity, complexity,
  security applicability, evidence, and checkpoint integrity. After recording the observed evidence matrix, the final
  pass found no findings, changed no file, triaged no finding, and is not canonical Verify or Human Review.
- Task Group 7 is acknowledged at `78146fd80b622f332af825619e5b7228efeb1434`; draft PR #32 is synchronized and Run
  revision 8 admits only Group 8.
- Unit, integration, and completed-JAR clients now call the canonical `trading.fee` surface directly. Risk receives
  only a successful core `Pnl` projection at the caller-owned boundary, so its production API and dependency graph
  remain unchanged.
- The provisional `trading.fee.policy` implementation, `FeeOrchestration`, and universal orchestration errors are
  deleted without aliases. Completed-JAR and negative compiler guards reject the old package plus `FeeSchedule`,
  `FeeLine`, `FeePolicyError`, and `FeeOrchestration` root aliases.
- Module guidance explicitly keeps policy acquisition, clocks, accounts, venue/tier/version selection, audit
  envelopes, and execution reports in later application/runtime or boundary-codec ownership. The focused Group 8 gate
  passes formatting, all 38 fee-policy tests, all 40 unchanged risk tests, and all 158 completed-JAR/compiler/
  adversarial tests.
- The automated Group 8 review checked AC-013/AC-016 scope, canonical migration, accumulated failure semantics, risk
  independence, retired and deferred surfaces, artifact boundaries, code quality, linear behavior, security
  applicability, and evidence with no findings. It changed no file, triaged no finding, and is not canonical Verify or
  Human Review.
- Task Group 8 is acknowledged at `3a8fd512d036462e50c27cd3aad3d10f0e77cd73`; draft PR #32 is synchronized and Run
  revision 9 admits only Group 9.
- Both formatting checks and clean dependency-order compilation pass. The full repository matrix passes 913 tests: 601
  quantities, 13 reference data, 9 application, 18 runtime, 13 instrument economics, 40 risk, 7 order model, 16
  execution scenario, 38 fee policy/integration, and 158 completed-JAR/compiler/adversarial tests. Explicit JMH
  compilation also passes.
- The completed fee-policy JAR has 132 entries and 90 classes, no provisional/retired/reverse package entry, and public
  signatures limited to explicit pure domain inputs. Production scans find no reverse dependencies, effect kind,
  source-market attachment, identity reconciliation, raw-scalar PnL kernel, unlawful policy monoid, or old capability.
- Strict OpenSpec validation and all eleven deterministic Corgi readiness checks pass at planning revision
  `sha256:b2226bdaf8f713cf48bb49f599da6b5cd84462cb96d8f028a6263caed76c4158`. The read-only cross-artifact semantic
  readiness review found no error, warning, or informational drift and changed no file.
- The automated Group 9 review checked Run/Group identity, AC-013 through AC-016 evidence, the full matrix, JMH and
  package/source audits, deterministic and semantic readiness, architecture, performance/security applicability,
  checkpoint integrity, and gate separation with no findings. It changed no file, triaged no finding, and is not
  canonical Verify or Human Review.
- S-05 remains bound to accepted `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`, AC-017 through
  AC-020, source digest `sha256:625809aa552c0c3f4efd1a8e2d9982c76414f82ce405674ce6f91b475807cceb`, and
  traceability digest `sha256:c7545d03bf6039543fee8aab7947aa9c10c46923f8e0ee9cc716a12ab66ac44f`;
  strict readiness passes at planning revision
  `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`.
- Run `run-2b01f872-e634-4491-acd3-b1454c94b59e` uses planning baseline
  `f356ba4bd02cf16bb4f1d1b1e03d9f3395f1286c`; Group 1 records exact integration parent
  `4e0034e7ca16b0f4cc67b045beadf6a3a9b0613c`. The pre-refresh resolved tree remains preserved as recovery commit
  `2cf25761f36c6a01a026855aacea7b9f3adad995`.
- The clean S-05 baseline passes both formatting checks, dependency-order compilation, all 913 tests, all 158
  completed-JAR/compiler/adversarial boundary tests, and explicit JMH compilation on OpenJDK 26.0.2, SBT 1.12.15,
  and Scala 3.8.4.
- The final delivered inventory fixes V1 against stable dimension/asset/grid/instrument identities, immutable
  catalog state/snapshots, canonical instrument assembly, closed order/scenario alternatives, and explicit market
  observers. Retired `Packed*`, `Resolved*`, registry, heterogeneous-runtime, and `GridCoordinateEncoding` names remain
  absent; no portfolio drift requires an RFC or planning amendment.
- The physical `trading-boundary-codecs` artifact now owns the `trading.codec` package/resource root above quantities,
  reference data, instrument economics, order model, and execution scenario. Its production classpath adds only Cats
  Core and Jackson Core 3.2.2; NetworkNT 3.0.7 and Java JSON Canonicalization 1.1 remain test-only.
- Completed-JAR compiler fixtures prove the codec production classpath excludes fee policy, risk, application,
  runtime, effects, streams, persistence/clients, telemetry, Jackson Databind/Scala, Circe, and test oracles, while all
  seven lower/pure artifacts reject reverse `trading.codec` imports.
- The clean Group 2 matrix passes 918 tests: 601 quantities, 13 reference data, 9 application, 18 runtime, 13
  instrument economics, 40 risk, 7 order model, 16 execution scenario, 1 boundary codec, 38 fee policy/integration,
  and 162 completed-JAR/compiler/adversarial tests. Both formatting checks also pass.
- The automated Group 2 review checked AC-017 scope, dependency and configuration containment, completed-JAR/public API
  boundaries, resource/documentation honesty, validation evidence, and performance/security applicability with no
  findings. It changed no file, triaged no finding, and is not canonical Verify or Human Review.
- The codec foundation now owns structured paths and locations, deterministic typed violations, JVM-constructor-private
  non-empty aggregates and `DecodeLimits`, exact pre-parse character/UTF-8 checks, and the documented 12-limit default
  profile without mutable or unbounded policy state.
- One package-private strict Jackson adapter builds an immutable AST while retaining raw number spelling and locations;
  the restricted RFC 8785 renderer agrees with the independent JCS oracle, and one internal algebra drives both
  accumulating codecs and stable-URN local-reference Draft 2020-12 schemas with operational limits omitted.
- The clean Group 3 matrix passes 946 tests: 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument
  economics, 40 risk, 7 order model, 16 execution scenario, 27 boundary codec, 38 fee policy/integration, and 164
  completed-JAR/compiler/adversarial tests. Both formatting checks, dependency inspection, and all 12 strict readiness
  checks pass.
- The automated Group 3 review corrected a misleading invalid-definition diagnostic, then checked identity/scope,
  behavior, architecture, API isolation, parser security/limits, canonicalization, schema agreement, dependency scope,
  and evidence with no findings. It changed no file during the final pass, triaged no finding, and is not canonical
  Verify or Human Review.
- The codec kernel now owns canonical signed/positive decimal strings, reduced rational records, exact stable-ID
  reconstruction, canonical UTF-16-ordered `DimKey` factors, full `GridIdentity`, checked `RecordType`/`SchemaVersion`,
  and family-specific explicit envelope dispatch. It rejects numeric/dimension normalization aliases, malformed
  Unicode, unknown/mismatched type/version pairs, and Java serialization of the new public version values.
- Reordered/whitespace input re-encodes to one canonical envelope, cross-alternative and closed-object fields remain
  structural failures, generated Draft 2020-12 envelopes fix the type/version constants with local references only,
  and `GridVersion` remains an exact payload string distinct from the literal envelope schema version.
- The clean Group 4 matrix passes 962 tests: 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument
  economics, 40 risk, 7 order model, 16 execution scenario, 43 boundary codec, 38 fee policy/integration, and 164
  completed-JAR/compiler/adversarial tests. Both formatting checks and all 12 strict readiness checks pass.
- The automated Group 4 review checked Run/Group identity, AC-017 scope, exact-number/Unicode behavior, owning
  constructors, normalization resistance, dimension/grid semantics, envelope dispatch, version separation, generated
  schemas, API/dependency containment, bounded allocation, and evidence with no findings. It changed no file, triaged
  no finding, and is not canonical Verify or Human Review.
- General and asset-qualified grid-coordinate V1 records now contain only full stable grid identity, exact signed
  coordinate, and asset ID where applicable. Their packers require the exact handle/value relationship and expose no
  arbitrary-quantity, projection, quantization, copied-quantum, registry, `Packed*`, or `Resolved*` surface.
- JVM-constructor-private decoded packages retain dimension/asset, grid, and typed value coherence. General rebuilding
  resolves dimension before the full grid; asset rebuilding resolves asset and rejects dimension drift before the full
  grid. Both families provide direct-snapshot, linear, all-valid-or-indexed-errors batch traversal.
- The clean Group 5 matrix passes 973 tests: 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument
  economics, 40 risk, 7 order model, 16 execution scenario, 52 boundary codec, 38 fee policy/integration, and 166
  completed-JAR/compiler/adversarial tests. Both formatting checks and all 12 strict readiness checks pass.
- The first automated Group 5 review tightened public reconstruction-failure invariants; its final pass checked exact
  scope, behavior, dependent typing, lookup order, batch semantics, architecture, complexity/security applicability,
  packaged/compiler boundaries, and evidence with no findings. It changed no file, triaged no finding, and is not
  canonical Verify or Human Review.
- Frozen catalog-journal V1 entries retain only a positive successor revision and the submitted ordered non-empty
  dimension/asset/grid command batch. Structural history decoding accumulates every independent indexed wire failure
  before replay, while pure sequential replay reuses `CatalogModel.commit` from an explicit fresh revision-zero state
  and returns typed revision, catalog-validation, unchanged, publication-mismatch, and non-fresh failures without a
  partial-state success channel.
- Direct-model, prefix, exact-number, permutation, duplicate, gap/repeat, conflict, no-op, historical-version, and
  cross-lineage tests pass. Completed-JAR fixtures compile the supported published-batch/fresh-state API and reject
  forged entries/results, broader outcomes, encoded authority/state, durability/checkpoint names, and same-package
  Scala construction bypasses.
- The clean Group 6 matrix passes 984 tests: 601 quantities, 13 reference data, 9 application, 18 runtime, 13
  instrument economics, 40 risk, 7 order model, 16 execution scenario, 61 boundary codec, 38 fee policy/integration,
  and 168 completed-JAR/compiler/adversarial tests. Both formatting checks and all 12 strict readiness checks pass.
- The first automated Group 6 review found package-spoof access to package-private entry/result factories. Those
  factories now remain object-private behind cached private method handles and the same-package compiler attack is
  retained. The final pass checked AC-019 scope, behavior, deterministic ordering, exactness, architecture, API and
  dependency confinement, resource/security applicability, and evidence with no findings; it changed no file,
  triaged no finding, and is not canonical Verify or Human Review.

## Promotion Queue
- Review agent-configuration constraints before promoting any item to `memory/MEMORY.md`.
- Keep the S-05 Group 1 baseline and inventory as delivery evidence until Archive proves the complete codec boundary;
  no Task Group 1 discovery is independently eligible for permanent promotion.
- Keep Group 2's module/dependency boundary and test-only oracle containment as delivery evidence until Archive proves
  the complete codec capability; no Group 2 discovery is independently eligible for permanent promotion.
- Keep Group 3's strict JSON, diagnostic/limit, canonical-rendering, and shared schema-algebra foundation as delivery
  evidence until Archive proves the complete codec capability; no Group 3 discovery is independently eligible for
  permanent promotion.
- Keep Group 4's exact primitive, stable-identity, canonical-dimension, and explicit envelope/version-dispatch kernel as
  delivery evidence until Archive proves the complete codec capability; no Group 4 discovery is independently eligible
  for permanent promotion.
- Keep Group 5's exact grid-coordinate records, dependent snapshot reconstruction, and explicit batch semantics as
  delivery evidence until Archive proves the complete codec capability; no Group 5 discovery is independently eligible
  for permanent promotion.
- Keep Group 6's frozen catalog-journal records, pure fresh-lineage replay, typed sequential failures, and authority-
  confinement evidence until Archive proves the complete codec capability; no Group 6 discovery is independently
  eligible for permanent promotion.
