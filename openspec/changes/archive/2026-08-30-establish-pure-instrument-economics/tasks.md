## 1. Portfolio and Baseline Gates

- [x] 1.1 Confirm Proposals 0–9 are complete, mutually consistent, independently approved for application, and ordered
  so quantity/reference-data, catalog, and instrument assembly land before this change.
- [x] 1.2 Refresh Git/OpenSpec state and record a clean baseline of formatting, compilation, unit/property/law tests, and
  external-artifact boundary checks before editing production code.
- [x] 1.3 Inventory every public `Instrument` capability path, economics source, test fixture, and downstream import that
  must move or break; classify each as pure core, order, scenario, fee policy, or risk.

## 2. Physical Instrument-Economics Boundary

- [x] 2.1 Add the `instrumentEconomics` SBT project in `instrument-economics/`, artifact
  `trading-instrument-economics`, with direct dependencies only on quantities and reference data plus the agreed pure
  library/test dependencies.
- [x] 2.2 Add completed-JAR external-artifact wiring, root aggregation, and adversarial/compiler-test classpaths for the
  new artifact without weakening existing test/compile isolation.
- [x] 2.3 Make the transitional `economics` project depend on `instrumentEconomics` and add compile guards proving the
  core cannot import order, scenario, fee-policy, risk, application, codec, runtime, or effect-system packages.
- [x] 2.4 Move Proposal 3's instrument definition, assembler, proof-carrying specification, IDs, roles, listing, payoff,
  and total `Instrument` construction into the new artifact without changing their validated behavior.

## 3. Static Instrument and Core Constructors

- [x] 3.1 Remove all service/capability fields from `Instrument` and retain only its assembled immutable semantic data
  and intrinsic observations.
- [x] 3.2 Implement the canonical dependent `Lots.fromCount(instrument)(count)` boundary and direct refined observers,
  with typed rejection of nonpositive counts.
- [x] 3.3 Implement `PositionLots.fromCoordinate(instrument)(coordinate)`, flat position, exact signed quantity
  observation, and checked same-instrument combination using the underlying lawful grid/quantity algebra.
- [x] 3.4 Remove order-side application from instrument economics and update the transitional order constructor to
  derive signed position change from explicit side and lots.
- [x] 3.5 Implement canonical exact/rate/tick `Price` constructors and explicitly named residual-bearing quantization,
  preserving positivity, endpoint type, and listing-grid evidence.
- [x] 3.6 Add compiler tests for dependent return types, reversed price-rate endpoints, refinement loss, old capability
  paths, and absence of `Side` and order types from the core artifact.

## 4. Market State and Typed Conversion

- [x] 4.1 Represent each settlement conversion as an existential package coupling its trusted source asset to its
  endpoint-typed source-to-settle rate; remove scalar-backed conversion reconstruction.
- [x] 4.2 Implement pure quote-settled, base-settled, quote-anchor, base-anchor, and dual-anchor `MarketState` constructors
  with identical scalar/typed validation and exact anchor coherence.
- [x] 4.3 Validate additional conversion target, immutable lineage, positivity, identity truthfulness, and duplicate
  source with staged deterministic accumulation and a shared first-error projection.
- [x] 4.4 Implement pure heterogeneous source lookup and immediate typed rate application using immutable identity/
  dimension evidence, with typed missing-source failure and no catalog or registry access.
- [x] 4.5 Add tests for base/quote/third-asset settlement, identity anchors, inconsistent anchors, duplicate additions,
  reversed endpoints, missing conversions, retained old-snapshot meaning, and lock-free catalog-independent use.

## 5. Proof-Preserving Valuation

- [x] 5.1 Implement settle-per-position through typed `Rate.andThen` and typed rate addition without coefficient-first
  arithmetic or unchecked public casts.
- [x] 5.2 Implement position value by applying the retained position-to-settle rate to exact signed position quantity,
  including lot coordinate and quantum.
- [x] 5.3 Introduce `PricePnl` retaining instrument identity and exact settlement quantity, and implement checked
  exit-minus-entry valuation for one explicit instrument.
- [x] 5.4 Add exact example/property tests for long/short symmetry, flat positions, linear/inverse/quanto normalization,
  and off-grid inverse results.
- [x] 5.5 Add source/API review checks demonstrating that typed payoff and conversion composition is not erased into raw
  `Rational` and reconstructed afterward.

## 6. Pure Fee Values

- [x] 6.1 Move `FeeKind`, quantization policy, `FeeDenomination`, and `Fee` value invariants into instrument economics
  while leaving `FeeRate`, percentage/minimum calculation, schedules, attribution, and selection downstream.
- [x] 6.2 Implement dependent `FeeDenomination` construction from an explicit instrument, trusted asset, and matching
  grid handle with one-time dimension, lineage, and runtime-instrument validation.
- [x] 6.3 Implement exact typed signed-amount quantization into `Fee`, retaining grid amount, residual, and unrounded
  amount with the conservation equation.
- [x] 6.4 Update transitional fee policy to calculate typed quantities and delegate only value construction to the core;
  remove percentage and scenario inspection from core APIs.
- [x] 6.5 Add fee tests for charges, rebates, zero, third assets, every quantization policy, conservation, foreign
  denominations, and absence of policy types from the core JAR.

## 7. Contribution-Based PnL

- [x] 7.1 Introduce `SettledFeeContribution` retaining its original existential fee, target instrument/settlement
  identity, and exact converted settlement quantity.
- [x] 7.2 Implement fee-to-settlement conversion from one explicit market state with typed missing-conversion and
  instrument-mismatch errors and no parity/catalog fallback.
- [x] 7.3 Implement `Pnl` construction from `PricePnl` and a vector of coherent settled fee contributions using typed
  quantity folding, exact empty identity, and retained breakdown.
- [x] 7.4 Move round-trip, fee-schedule evaluation, market-state attribution, and contribution ordering into the
  transitional scenario/fee-policy orchestration path before invoking pure PnL construction.
- [x] 7.5 Add tests for no fees, entry/exit charges, rebates, different per-line conversion states, missing conversions,
  identity mismatch, deterministic equality, and component visibility.
- [x] 7.6 Add algebra-focused tests for settlement-quantity identity/associativity and explicitly verify that no
  unlawful global `Monoid[Pnl]` or cross-instrument `Group[PositionLots]` is exposed.
- [x] 7.7 Preserve complete fee-denomination grid definition and coherent provenance in `Fee`, retained contribution,
  and `Pnl` equality/hash behavior, with a completed-JAR regression for equal and irreconcilable values.

## 8. Downstream Dependency Inversion

- [x] 8.1 Move order sources to `trading.order` in the transitional artifact and replace `instrument.orders` with pure
  constructors receiving an explicit instrument.
- [x] 8.2 Move execution-scenario sources to `trading.scenario` and replace `instrument.scenarios` with accumulating and
  fail-fast constructors receiving an explicit instrument.
- [x] 8.3 Move fee policy and attribution sources to `trading.fee.policy`, replace `instrument.fees`, and preserve the
  existing policy expressiveness through explicit instrument/scenario inputs.
- [x] 8.4 Move downside-risk and sizing sources to `trading.risk`, replace `instrument.sizing`, and consume explicit
  instrument economics plus downstream scenario/fee-policy functions.
- [x] 8.5 Remove `instrument.valuation`; migrate callers to explicit pure valuation, contribution conversion, and PnL
  constructors.
- [x] 8.6 Split the former universal economics error into boundary-owned core and downstream ADTs, retain typed causes
  when wrapping, and remove `ForeignRegistry` and errors no longer producible by the core.
- [x] 8.7 Add no compatibility aliases, deprecated forwarders, duplicate constructors, or generic replacement service
  locator.

## 9. Verification and Steward Handoff

- [x] 9.1 Format all affected Scala/SBT sources and run clean compilation for every module in dependency order.
- [x] 9.2 Run the complete unit, property, law, integration, adversarial-boundary, and external-JAR compiler-test matrix,
  including an executed concrete core-only client, valid-prelude associated-evidence and removed-surface rejections,
  and Java-serialization rejection for pure economics values.
- [x] 9.3 Inspect the packaged `trading-instrument-economics` classpath/API and search production source for forbidden
  higher-layer imports, live catalog access, synchronization, effect kinds, old capability names, public casts, and raw-
  scalar valuation detours.
- [x] 9.4 Run strict validation for this change and all active OpenSpec changes, then reconcile any proposal/spec/design/
  task drift discovered during implementation.
- [x] 9.5 Prepare the validated worktree for fresh independent review without self-certifying, committing, archiving, or
  starting Proposal 5 implementation outside steward authorization.
- [x] 9.6 Obtain fresh independent-review approval of the staged change before archive; only finalization may complete
  this task after that approval.
