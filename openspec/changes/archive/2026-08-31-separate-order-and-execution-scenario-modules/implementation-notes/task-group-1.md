# Task Group 1 — Contract, dependency, and baseline gates

## Authority and prerequisite reconciliation

- Delivery: `RFC-0002-architecture-portfolio/S-02-order-execution-scenarios`
- Change: `separate-order-and-execution-scenario-modules`
- Issue: `#7`
- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`
- Source digest: `sha256:5d573cd0d12bbcffacb9b047475ee4beb81069dedabd420018b5d3f32f7acdd0`
- Traceability digest: `sha256:a265f9092baad966ec7ef5f731ab1a889dc3542f8bd9a5e893b1f8338af6de5f`
- Planning baseline commit: `9e126d1ccf930dab074ec421d5d37cae0a906d28`

The externally owned `establish-pure-instrument-economics` delivery is merged on `main` as commit
`86613ee` and archived at
`openspec/changes/archive/2026-08-30-establish-pure-instrument-economics/`. Its exact delivered patch was applied to
this Task Group without an intermediate commit. This establishes `trading-instrument-economics`, removes
instrument-owned capability views, and supplies the transitional `trading.order`, `trading.scenario`, fee-policy, and
risk packages that S-02 must split. No S-02 production redesign was made before this reconciliation.

## Behavioral baseline

The baseline was executed on OpenJDK 26.0.2, which satisfies the repository's JDK 25 minimum, using SBT 1.12.14 and
Scala 3.8.4.

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch scalafmtCheckAll` | pass | All production, test, build, and adversarial Scala sources were clean. |
| `sbt -batch clean test` | pass | 768 tests passed, 0 failed, across quantities, reference data, application, instrument economics, transitional economics, and adversarial boundaries. |
| Instrument economics unit/property boundary | pass | 13 tests passed in `instrumentEconomics`. |
| Transitional order/scenario/fee/risk behavior | pass | 12 tests passed in `DownstreamEconomicsSuite`. |
| Completed-JAR and adversarial compiler boundaries | pass | 122 adversarial tests passed, including all 25 `EconomicsCompilerBoundarySuite` checks. |
| Strict Corgi/OpenSpec readiness | pass | Source, traceability, task structure, capability parity, and source provenance passed before and after reconciliation with unchanged planning/source/traceability digests. |

The JDK emitted only the upstream Scala `sun.misc.Unsafe` terminal-deprecation warning; it did not affect compilation or
tests.

## Production ownership inventory

### Order-model destination

All declarations currently live in `economics/src/main/scala/trading/order/` and move to the final `orderModel`
artifact.

| Current concept | Destination / migration action |
| --- | --- |
| `Side`, `TimeInForce`, `NonRestingTimeInForce` | `trading.order`; retain the sum types and local non-resting refinement. |
| `LiquidityConstraint`, `PositionEffect` | `trading.order`; retain as instructions, without outcome or account-state enforcement. |
| `PriceReference`, `TriggerComparison` | `trading.order`; retain as closed trigger/peg vocabulary. |
| `OrderActivation`, `TriggerActivation`, `ImmediateActivation`, `FixedActivation`, `TrailingActivation` | `trading.order`; retain associated `Evidence` and dimension-index the price shape. |
| `FixedTriggerEvidence`, `TrailingTriggerEvidence`, `CheckedActivation` | `trading.order`; keep construction on the owning activation and semantic same-shape replay checks. |
| `OrderPricing`, `LimitPricing`, `PeggedPricing`, `DirectPricingResolution`, `PegResolution`, `EffectivePricing` | `trading.order`; retain associated `Resolution` and explicit market/effective-limit results. |
| `PricedVisibility`, `DisplayedVisibility`, `HiddenVisibility`, `IcebergVisibility` | `trading.order`; preserve the closed priced-only visibility sum. |
| `OrderExecution`, `MarketExecution`, `PricedExecution` | `trading.order`; preserve market/priced structural exclusion. |
| `OrderIntent`, `Order`, private `ConstructedOrder`, `Order.Aux` | `trading.order`; retain instrument identity and exact typed values, and move signed `PositionLots` construction into intent. |
| `Orders` service | Remove in Task Group 4 after value-oriented constructors and canonical accumulating validation replace it. |

Current order errors are `OrderError`, `OrderFailureReason` (`RestingMarketDuration`, `NonRestingIceberg`,
`IcebergExceedsOrder`, `PositionChangeMismatch`), `InvalidTrailingOffset`, `InvalidOrder`, and
`OrderInstrumentMismatch`. `ActivationViolation` owns five focused semantic cases and `PricingViolation` owns two.
The final artifact replaces string contexts and universal mappings with closed `OrderComponent`, `OrderViolation`, and
non-empty ordered `OrderViolations`; focused activation/pricing causes remain locally owned.

### Execution-scenario destination

All declarations currently live in `economics/src/main/scala/trading/scenario/` and move to the final
`executionScenario` artifact.

| Current concept | Destination / migration action |
| --- | --- |
| `LiquidityRole`, `LiquiditySlice` | `trading.scenario`; retain role as an outcome and construct slices from an explicit instrument. |
| `ScenarioAssumptions` | `trading.scenario`; make it own exactly one order and its associated evidence/resolution; remove duplicate `instrumentId` and target claims. |
| Cats `NonEmptyVector[LiquiditySlice]` | Replace at the public boundary with domain-owned `MatchedSlices` (`one`, head/tail, vector projection, checked reconstruction). |
| `OrderScenario` | `trading.scenario`; retain assumptions, verified effective pricing, slices, and intent's signed position change. |
| `RoundTripScenario`, `ScenarioLeg` | `trading.scenario`; use checked same-instrument position combination and preserve entry/exit details. |
| private `IndexedViolation` / `Validation` | Keep internal staged accumulation semantics, with explicit prerequisites and stable ordinals. |
| `Scenarios` service | Remove in Task Group 6 after explicit constructors and `OrderScenario.evaluate` replace it. |

Current scenario errors comprise 13 `ScenarioFailureReason` cases, seven `ScenarioViolation` alternatives,
`InvalidScenarioDiagnostics`, `ScenarioError`, `ScenarioInstrumentMismatch`, `InvalidScenario`, and `InvalidRoundTrip`.
The final boundary introduces closed `ScenarioLocation`, focused causes, and non-empty ordered `ScenarioViolations`, and
removes free-form contexts, `OrderTargetMismatch`, `AssumptionOrderMismatch`, duplicate universal mappings, and the
second-order/reference-identity path.

### Later Slice owners

| Current concept / consumer | Current dependency on scenarios | Later owner |
| --- | --- | --- |
| `FeeRate`, `FeeLine`, `FeeSchedule`, `FeePolicy` plus eight `FeePolicyError` cases | Consumes `OrderScenario`, `RoundTripScenario`, `ScenarioLeg`, matched-slice vectors, market states, and positions. It currently recomputes per-slice signed positions. | Remains in transitional `economics` for S-02; S-04 fee-policy owns its redesign. S-02 only adapts it to the new scenario projections without changing fee semantics. |
| `Risk` plus six `RiskError` cases | Consumes fee-inclusive P&L and `RoundTripScenario` through `scenarioFor`, checks scenario identity, and compares selected lots with held position. | Remains in transitional `economics`; S-03 pure-risk owns its redesign. S-02 only adapts it without changing traversal or risk semantics. |
| Stable persistence/wire reconstruction | No production codec exists; compiler fixtures exercise adapter-shaped calls. | S-05 boundary codecs. |
| Submission, lifecycle, fills, clocks, effects, streams, clients, telemetry | No current order/scenario production dependency. `DeferredLifecycle.scala` is a negative boundary. | Future application/runtime contracts, outside S-02. |

## Instrument boundary calls

Positive production code uses an explicit `Instrument`; no positive path calls `instrument.orders`,
`instrument.scenarios`, `instrument.fees`, `instrument.risk`, or another instrument-owned capability view.
`RemovedCapabilityPaths.scala` keeps those former paths negative.

- Order construction currently uses `instrument.identity.id`, path-dependent `instrument.Lots` / `instrument.Price`,
  `Lots.fromCount`, and `PositionLots.fromCoordinate` through `Orders(instrument)`.
- Scenario construction currently uses `instrument.identity.id`, `instrument.Lots`, `instrument.Price`,
  `instrument.MarketState`, and `instrument.PositionLots` through `Scenarios(instrument)`.
- Fee policy uses explicit `FeePolicy(instrument)`, `instrument.Pnl`, settlement-role dimensions, valuation/contribution
  operations, and scenario projections.
- Risk uses explicit `Risk.create(instrument)(policy)`, `instrument.Pnl`, settlement-role dimensions, exact lots, and a
  caller-supplied round-trip scenario function.
- Test assembly uses `InstrumentAssembler.assemble`, `Instrument.fromSpec`, `Lots.fromCount`, `Price.exact`, and
  `MarketState.quoteSettled`; these remain instrument-economics construction boundaries rather than order/scenario
  services.

## Test and compiler-fixture migration inventory

| Fixture | Coverage and destination |
| --- | --- |
| `economics/src/test/scala/trading/support/DownstreamFixtures.scala` | Instrument/catalog fixture factory; remain shared test support while order/scenario fixtures migrate to their owning modules. |
| `economics/src/test/scala/trading/DownstreamEconomicsSuite.scala` | 12 behavioral cases covering signed order direction, forged intent rejection, instruction alternatives, evidence/resolution semantics, accumulated scenario diagnostics, exact round trips, fee attribution/P&L, risk, and schedule composition. Split into order/scenario owner suites; retain fee/risk equivalence tests in transitional economics. |
| `economics-compiler/SharedEconomicsSetup.scala` | Packaged external assembly and positive shared values; split into instrument/order/scenario classpaths as module boundaries are introduced. |
| `positive/CompleteEconomicsClient.scala` | Full downstream order/scenario/fee/risk usage; update to the final explicit constructors. |
| `positive/SameShapeReplayClient.scala` | Same-shape activation evidence and peg-resolution replay; move to order-model packaged positive coverage. |
| `negative/AssociatedEvidenceShapes.scala` | Fixed/trailing and direct/pegged mismatch rejection; move to order-model and scenario-assumption negative fixtures. |
| `negative/DeferredLifecycle.scala` | Proves immutable order/scenario values do not expose live lifecycle methods; retain across the split. |
| `negative/RemovedFlatApi.scala` | Proves former flat/instrument-owned order paths stay absent; update old names while retaining the boundary claim. |
| `negative/RemovedCapabilityPaths.scala` and `negative/CoreSideAbsent.scala` | Prove instrument owns no downstream services and instrument economics owns no `Side`/order API; retain and strengthen for the new artifacts. |
| `economics-core-compiler/CoreHasNoDownstream.scala` | Proves instrument economics cannot see order/scenario/fee/risk; retain unchanged as the lower-boundary guard. |
| `external/EconomicsCompilerBoundarySuite.scala` | Orchestrates 25 completed-JAR positive/negative checks; extend with independent order-model and execution-scenario classpaths in Task Group 2. |

This inventory is exhaustive for Scala production and test files containing the delivered `trading.order` or
`trading.scenario` surfaces at the Task Group 1 baseline.
