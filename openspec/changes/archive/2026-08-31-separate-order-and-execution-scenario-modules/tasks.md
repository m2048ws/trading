## 1. Contract, Dependency, and Baseline Gates

- [ ] 1.1 Confirm the effective RFC/source/traceability bindings, the externally owned instrument-economics prerequisite
  is merged and archived, and this package has been reconciled against its delivered boundary before production edits.
- [ ] 1.2 Refresh Git/source/build/Corgi state and record clean formatting, compilation, unit/property tests, and external-artifact
  compiler checks as the behavioral baseline.
- [ ] 1.3 Inventory every order/scenario type, instrument capability call, error case, test fixture, fee-policy consumer,
  and risk consumer; map each to order model, execution scenario, or a later module.

## 2. Module Boundaries

- [ ] 2.1 Add `orderModel` in `order-model/` with artifact `trading-order-model`, package `trading.order`, and only the
  direct quantities/instrument-economics plus agreed pure test/library dependencies.
- [ ] 2.2 Add `executionScenario` in `execution-scenario/` with artifact `trading-execution-scenario`, package
  `trading.scenario`, and only direct instrument-economics/order-model plus agreed pure test/library dependencies.
- [ ] 2.3 Wire root aggregation, completed-JAR external-artifact tasks, adversarial/compiler-test classpaths, and the
  transitional aggregate's downward dependencies.
- [ ] 2.4 Add compiler guards proving order model cannot access market states/scenario/fee/risk/runtime and execution
  scenario cannot access fee/risk/application/runtime or mutate foundational values.

## 3. Algebraic Order Model

- [ ] 3.1 Move `Side`, time-in-force refinements, liquidity constraint, position effect, price reference, comparison,
  visibility, activation, pricing, execution, intent, and order types to `trading.order`.
- [ ] 3.2 Preserve dimension-indexed `Lots`/`Price` relationships and associated `Evidence`/`Resolution` members without
  `Any`, raw-scalar storage, kind-plus-option records, or public casts.
- [ ] 3.3 Put local smart constructors on trailing activation, non-resting duration, and other refined order values with
  focused order-model errors.
- [ ] 3.4 Implement evidence and peg-resolution construction through the corresponding instruction values, deriving
  owned semantic fields and checking same-shape replay without object identity tokens.
- [ ] 3.5 Add compile-positive/exhaustiveness tests for every valid activation/execution composition and compile-negative
  tests for impossible market/priced, trigger/evidence, peg/resolution, and visibility shapes.

## 4. Intent and Order Validation

- [ ] 4.1 Implement `OrderIntent` construction from an explicit instrument, side, positive lots, and position effect,
  retaining the exact signed `PositionLots` produced by the core boundary.
- [ ] 4.2 Add closed `OrderComponent`, `OrderViolation`, and non-empty ordered `OrderViolations` domain types; remove
  free-form paths and universal economics-error mappings.
- [ ] 4.3 Implement one staged accumulating final-order validator for runtime identity and independent iceberg constraints,
  with deterministic ordinals and fail-fast head projection.
- [ ] 4.4 Rebuild market, limit, stop-market, and stop-limit conveniences as composition into the canonical constructor,
  and remove the `Orders` service class and instrument-owned path.
- [ ] 4.5 Test long/short position changes, reduce-only retention without account enforcement, identity mismatch
  locations, simultaneous iceberg violations, and deterministic accumulating/fail-fast agreement.

## 5. Domain Scenario Inputs

- [ ] 5.1 Move `LiquidityRole` and liquidity-slice types to `trading.scenario` and implement pure slice construction from
  an explicit instrument, positive lots, market state, and role.
- [ ] 5.2 Implement minimal domain-owned `MatchedSlices` with `one`, head/tail, vector projection, and typed vector
  reconstruction that rejects empty input.
- [ ] 5.3 Reshape `ScenarioAssumptions` so it owns one order and its path-dependent activation evidence, pricing
  resolution, and matched slices; remove duplicated instrument ID and target-order claims.
- [ ] 5.4 Provide adapter-friendly constructors for each legal associated evidence shape without accepting untyped maps,
  duplicate references, or mismatched alternatives.
- [ ] 5.5 Add compiler tests proving missing/extraneous and fixed/trailing or direct/pegged evidence pairings cannot use
  the supported assumptions path.

## 6. Staged Scenario Evaluation

- [ ] 6.1 Add closed `ScenarioLocation`, `ScenarioViolation`, focused activation/pricing causes, and non-empty ordered
  `ScenarioViolations`; delete string path diagnostics and target-reference mismatch errors.
- [ ] 6.2 Implement the identity-validation stage across instrument, order components, evidence prices, and indexed slice
  lots/market/price values.
- [ ] 6.3 Implement branch-sensitive lot-total, activation, pricing, market/taker, and maker-only/maker validation with
  explicit prerequisites and stable rule ordinals.
- [ ] 6.4 Implement effective-price-dependent side/limit quality validation without suppressing independent branches or
  running comparisons on foreign/missing evidence.
- [ ] 6.5 Construct `OrderScenario` from the one assumptions value, retaining verified results and intent's signed
  position change; expose accumulating and fail-fast head-projection APIs.
- [ ] 6.6 Remove the `Scenarios` service, duplicate order parameter, `.eq` target comparison, scenario position-sign
  recomputation, parallel scalar `View`, and obsolete universal error mappings.
- [ ] 6.7 Test multi-price slices, non-empty reconstruction, complete lot conservation, activation/peg failures,
  market/maker rules, limit quality, closed error locations, branch suppression, and stable diagnostic ordering.

## 7. Round Trips and Downstream Consumers

- [ ] 7.1 Implement round-trip construction from an explicit instrument and two complete scenarios using checked
  `PositionLots` combination and exact flat identity.
- [ ] 7.2 Preserve entry/exit assumptions and the entry's retained position as held position, with typed signed-coordinate
  and cross-instrument failures and no side/count recomputation.
- [ ] 7.3 Update transitional fee-policy code to consume the new scenario types and slice projections without changing
  fee semantics.
- [ ] 7.4 Update transitional risk/sizing code to consume new round trips without changing candidate traversal or risk
  semantics.
- [ ] 7.5 Remove order/scenario production source and related errors from the transitional `economics` artifact, with no
  compatibility aliases or package forwarders.

## 8. Verification Evidence and Corgi Handoff

- [ ] 8.1 Format all affected Scala/SBT sources and run clean compilation in dependency order.
- [ ] 8.2 Run order/scenario unit and property suites, dependent fee/risk suites, negative compilation, external-JAR
  boundary tests, adversarial tests, and the full repository validation matrix.
- [ ] 8.3 Compare all existing valid and invalid order/scenario fixtures against the new behavior, documenting intentional
  differences from removed duplicate-target and universal-error paths.
- [ ] 8.4 Inspect packaged APIs and production imports for forbidden dependencies, effect kinds, lifecycle state,
  untyped optional encodings, object-identity checks, raw arithmetic detours, and old capability names.
- [ ] 8.5 Confirm the current Slice still satisfies strict planning/source/traceability integrity and reconcile any
  implementation drift before the final Task Group checkpoint.
- [ ] 8.6 Prepare the final acknowledged Task Group commit and evidence for separate canonical Verify, human review,
  human QA, and Archive; do not begin the fee-policy Slice in this delivery.

## 9. Independent Review Repair

- [ ] 9.1 Integrate the current `main` tree into the repair commit without rewriting the acknowledged Task Group prefix,
  preserving the delivered instrument-economics and application/runtime boundaries while resolving all S-02 module,
  compiler-fixture, downstream economics, documentation, and delivery-state conflicts.
- [ ] 9.2 Replace package-qualified order/scenario construction authority with JVM-enforced private or otherwise
  unforgeable representations while retaining the checked factories, associated evidence relationships, immutable
  public observations, and domain-readable call sites required by AC-006 and AC-007.
- [ ] 9.3 Add completed-JAR negative fixtures for same-package Scala and Java construction/copy attempts covering order
  intent, trigger and peg evidence, liquidity slices, assumptions, checked scenarios, and round trips; retain nearby
  positive fixtures for every supported checked construction path.
- [ ] 9.4 Run formatting, clean compilation, unit/property suites, completed-JAR compiler boundaries, downstream
  fee/risk behavioral-equivalence tests, adversarial tests, strict OpenSpec validation, and deterministic Corgi
  readiness on the integrated tree.
- [ ] 9.5 Record the repair rationale, conflict dispositions, API inspection, exact validation counts, and independent
  review findings addressed; prepare one dedicated Repair Task Group commit for acknowledgement, fresh canonical
  Verify, human review, and exact-SHA independent review.

## 10. Merge-Ancestry Verification Repair

- [ ] 10.1 Verify that the repair baseline records `05a8af0ff836b846247c082901ac3baea3d0c169` as an actual ancestor
  without changing the already reviewed Group 9 tree, preserves the acknowledged Task Group prefix, and removes the
  draft PR's integration conflict.
- [ ] 10.2 Rerun formatting, the clean full repository test matrix, JMH compilation, completed-JAR Scala and Java
  construction boundaries, downstream fee/risk behavioral-equivalence coverage, and Git topology/workspace checks on
  the ancestry-corrected history.
- [ ] 10.3 Run strict OpenSpec validation, deterministic Corgi readiness, and the automated Task Group review loop;
  record the rejection reason, topology proof, exact validation counts, remaining risks, and resulting checkpoint in
  the session bridge.
- [ ] 10.4 Prepare one dedicated atomic Repair Task Group commit containing the validation/evidence checkpoint,
  acknowledge it through Corgi, synchronize draft PR #27, rerun canonical Verify, and request a fresh exact-SHA
  independent whole-change review.

## 11. Completed-JAR JVM Construction Authority Repair

- [ ] 11.1 Close the completed `trading-order-model` JAR against external Java subclassing of `Order`, external
  implementation of activation/execution alternatives, and direct construction of concrete order representations,
  while preserving the checked Scala API and exhaustive domain matching required by AC-006.
- [ ] 11.2 Close `MatchedSlices` construction at the JVM boundary and remove, hide, or dynamically validate erased
  `ScenarioAssumptions` entry points so arbitrary Java `Object` evidence and pricing-resolution values produce typed
  rejection rather than forge assumptions accepted by evaluation.
- [ ] 11.3 Extend completed-JAR Java adversarial fixtures to reject every demonstrated subclass, implementation,
  constructor, and erased-factory bypass; retain positive Java/Scala fixtures for the supported checked construction
  paths and inspect emitted class flags and signatures with `javap`.
- [ ] 11.4 Run formatting, clean full repository tests, JMH compilation, completed-JAR compiler boundaries,
  downstream fee/risk behavioral-equivalence tests, strict OpenSpec validation, deterministic Corgi readiness, and
  the automated Task Group review loop.
- [ ] 11.5 Record the exact remediation, API/bytecode evidence, validation counts, and review disposition in the
  session checkpoint; prepare one dedicated Repair Task Group commit for acknowledgement, PR synchronization,
  canonical Verify, human review, and a fresh independent exact-SHA whole-change review.

## 12. Checked Scenario Structural Value Semantics Repair

- [ ] 12.1 Restore structural `equals` and `hashCode` for `OrderScenario` over its retained assumptions, checked
  activation, effective pricing, and position change, preserving the JVM-private constructor and exposing no unchecked
  `copy`, constructor, or product-based construction path.
- [ ] 12.2 Restore structural `equals` and `hashCode` for `RoundTripScenario` over its instrument identity, entry, exit,
  and held position, preserving the JVM-private constructor and checked round-trip creation boundary.
- [ ] 12.3 Add focused laws proving repeated checked evaluation and repeated checked round-trip construction yield equal
  values with equal hashes, while meaningfully different semantic fields remain unequal and completed-JAR construction
  boundaries stay closed.
- [ ] 12.4 Run formatting, the clean full repository test matrix, JMH compilation, completed-JAR Scala and Java boundary
  probes, downstream fee/risk behavioral-equivalence coverage, strict OpenSpec validation, deterministic Corgi
  readiness, and the automated Task Group review loop.
- [ ] 12.5 Record the exact remediation, validation counts, and review disposition in the session checkpoint; prepare
  one dedicated Repair Task Group commit for acknowledgement, PR synchronization, canonical Verify, human review, and
  a fresh independent exact-SHA whole-change review.

## 13. Archive Delta Completeness Repair

- [ ] 13.1 Reconcile the modified compositional-order requirement so every scenario in the current canonical requirement
  remains explicitly represented while retaining the delivered stronger structural and accumulating-validation cases.
- [ ] 13.2 Prove the repaired delta with strict OpenSpec validation and an isolated OpenSpec Archive rehearsal; confirm
  the accepted RFC source contract and the immutable Task Group 1–12 prefix remain unchanged.
- [ ] 13.3 Run deterministic Corgi readiness and the automated Task Group review loop, record the recovery rationale and
  exact evidence in the session checkpoint, and prepare one dedicated repair commit for acknowledgement.
- [ ] 13.4 Rerun canonical Verify, human whole-change Review, Human QA, and the strong Archive transaction against the
  repaired planning revision; do not alter production behavior or begin a later RFC Slice.
