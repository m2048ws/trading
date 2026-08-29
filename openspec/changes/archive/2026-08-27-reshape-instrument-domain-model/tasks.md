## 1. Owner Encoding and Regression Baseline

- [x] 1.1 Add focused downstream compiler fixtures that characterize current valid generic instrument usage, stable path ownership, and cross-instrument rejection before changing the representation.
- [x] 1.2 Prototype the generative `Owner` type and sealed non-public owner authority in production-shaped Scala, proving that two equal-looking instruments remain distinct through stable rebinding and generic forwarding.
- [x] 1.3 Add negative packaged fixtures proving ordinary and same-package-spoof callers cannot implement, obtain, widen, or substitute owner authority or call trusted carrier constructors.
- [x] 1.4 Retain only an owner encoding whose positive and negative probes compile with the repository's strict downstream settings and whose public diagnostics do not expose callable private authority.

## 2. Refined Owned Primitive Values

- [x] 2.1 Implement owner-indexed lots, signed-position, and price carriers backed by the issuing grids, using existing `Positive[GridQuantity]` refinement for lots and prices and unrestricted grid coordinates for positions.
- [x] 2.2 Require matching owner authority for carrier construction and private representation access, and prevent raw grid arithmetic from returning refined lots or prices without a checked boundary.
- [x] 2.3 Move lot count, exact lot quantity, signed position count, exact position quantity, price ticks, price coefficient, and price rate onto their corresponding owned values.
- [x] 2.4 Migrate lot, position, price, exact-narrowing, and quantization tests to the refined carriers, including positive construction, zero/negative rejection, refinement-losing arithmetic, residual conservation, and cross-owner rejection.

## 3. Cohesive Instrument Definition

- [x] 3.1 Introduce semantic identity, registered-role, listing-rule, and two-leg contract-payoff components rooted in one stable dependent role value.
- [x] 3.2 Implement local component validation and one final validated-definition boundary for registry sharing, role contradiction, lot-grid dimension, quote-per-base price-grid dimension, and nonempty payoff.
- [x] 3.3 Localize all existential grid/rate witness casts to the validated definition and owner-authority creation boundary, with no cast or retagging factory exposed through package visibility.
- [x] 3.4 Replace the wide `Instrument.create` parameter family with the cohesive component-based construction path while retaining one public generative `Instrument` aggregate and no lasting resolved wrapper.
- [x] 3.5 Migrate coherent, foreign-registry, wrong-grid, contradictory-role, non-currency-underlying, empty-payoff, and spot/linear/inverse/quanto construction tests without changing economic terms.

## 4. Explicit Order Domain Alternatives

- [x] 4.1 Replace activation kinds and optional fields with owner-aware immediate, fixed-trigger, and trailing-trigger alternatives, using a positive tick distance for trailing activation.
- [x] 4.2 Replace market/limit/pegged kind records and visibility sentinels with market execution, priced execution, limit/peg pricing, non-resting duration, and displayed/hidden/iceberg alternatives whose local shapes are valid by construction.
- [x] 4.3 Introduce `OrderIntent` for side, positive lots, and position effect, and implement the primary checked order boundary over intent, activation, and execution.
- [x] 4.4 Reimplement market, limit, stop-market, and stop-limit conveniences as construction of the same alternatives followed by the primary boundary.
- [x] 4.5 Replace free-form order failure details with closed reasons for the existing observable incompatibilities and preserve deterministic typed failure behavior.
- [x] 4.6 Add direct and downstream tests for every activation/execution/visibility case, invalid market duration, oversized iceberg display, non-resting iceberg rejection, market absence of priced-only fields, and owner-safe generic inspection.

## 5. Scenario Assumptions and Validation

- [x] 5.1 Replace optional activation evidence with fixed and trailing evidence alternatives and replace optional peg state with direct-pricing and resolved-peg assumptions.
- [x] 5.2 Introduce one owner-indexed `ScenarioAssumptions` input containing activation assumption, pricing assumption, and matched liquidity slices.
- [x] 5.3 Reimplement complete-scenario validation by pattern-matching the actual order and assumption alternatives for trigger satisfaction, peg agreement, exact slice totals, limit quality, and liquidity-role compatibility.
- [x] 5.4 Preserve round-trip closure and signed held-position derivation using the new distinct position carrier.
- [x] 5.5 Remove `ActivationView`, `InstructionView`, `EvidenceView`, `PegView`, `SliceView`, `OrderView`, and generic scenario plans after no production or test caller depends on them.
- [x] 5.6 Add scenario tests for immediate/fixed/trailing and direct/pegged combinations, missing and extraneous evidence, mismatched peg resolution, one/many slices, lot conservation, maker/taker rules, limit quality, and cross-owner rejection.

## 6. Market State and Value-Centric Observations

- [x] 6.1 Migrate market construction to consume refined prices and owner-indexed conversion data while preserving quote-settled, base-settled, single-anchor, dual-anchor, scalar/rate equivalence, and exact coherence behavior.
- [x] 6.2 Move settle conversion onto the owning market state and ensure it uses only that state's checked conversion set, preserving typed missing-conversion and foreign-registry failures.
- [x] 6.3 Remove obsolete root and capability observer/forwarding methods after all production callers use value-local observations.
- [x] 6.4 Preserve market and valuation property tests for identity anchors, third-asset settlement, additional conversions, exact universal payoff valuation, long/short symmetry, and off-grid inverse PnL.

## 7. Validated Fee Denomination and PnL

- [x] 7.1 Implement an owner-indexed `FeeDenomination` retaining one registered fee asset, dimension-matching registered grid, and quantization policy, with registry and grid validation performed at denomination construction.
- [x] 7.2 Move exact quantization and percentage fee construction onto the denomination so schedules supply only kind, correctly typed amount or basis, and rate as applicable.
- [x] 7.3 Move intrinsic fee observations onto `Fee`, preserve checked fee-line attribution and schedule composition, and keep schedules contextual rather than instrument metadata.
- [x] 7.4 Migrate fee schedules, fee-inclusive valuation, converted line attribution, and missing-conversion handling to the new order, scenario, market-state, and denomination values without changing fee signs or exact formulas.
- [x] 7.5 Add tests for denomination reuse, foreign asset/grid rejection, charge/rebate signs, third-asset fees, minimums, tiers, per-component residual conservation, zero/many schedules, and exact net-PnL breakdown.

## 8. Kernel-Based Modularization and API Cleanup

- [x] 8.1 Wire focused price, market, order, scenario, fee, valuation, and sizing implementations with the sealed owner kernel instead of anonymous capability façades forwarding back into one `InstrumentImpl`.
- [x] 8.2 Move concern implementations into responsibility-oriented source files while keeping validated-definition authority issuance in the trusted instrument boundary and avoiding a mechanical one-file-per-capability rule.
- [x] 8.3 Remove `VisibilityPlan`, `ActivationPlan`, `PriceInstructionPlan`, `OrderPlan`, conversion/state plans, PnL plans that only mirror public values, and generic callback helpers that exist solely because domain data was projected away.
- [x] 8.4 Keep small pure arithmetic helpers only where they name reusable formulas or algorithms, and audit remaining methods with large parameter lists for an actual cohesive domain input rather than an arbitrary `Params` wrapper.
- [x] 8.5 Replace remaining caller-relevant free-form validation detail strings with concern-specific reason alternatives and verify stable diagnostic rendering.
- [x] 8.6 Inspect the packaged Scala and JVM economics APIs to confirm Scala owner authority is final and gated, the raw JVM gate cannot be acquired, implemented, or supplied to trusted construction by ordinary or same-package Java, every trusted carrier and aggregate has a JVM-enforced construction boundary, documented Scala aliases remain ergonomic, and no superseded kinds, optional-field observers, root observers, wide checked constructors, or compatibility aliases remain.

## 9. Caller Migration and Semantic Regression Coverage

- [x] 9.1 Migrate economics fixtures and behavioral/property suites to component-based instrument construction, value-local observations, explicit order/scenario alternatives, and validated fee denominations.
- [x] 9.2 Rewrite the end-to-end example around the new ordinary adapter flow from parsed exact rational input through price, market state, order, scenario, fees, valuation, and sizing.
- [x] 9.3 Update the positive packaged client to exercise concrete and generic owner-safe use without private types, casts, manually reconstructed dimensions, or Plan/View representations.
- [x] 9.4 Extend negative packaged fixtures for cross-instrument primitive and aggregate mixing, refinement loss, forged owner/kernel/carrier construction, same-package spoofing, removed optional-field/kind APIs, and removed forwarding paths.
- [x] 9.5 Preserve exhaustive sizing tests and prove that the structural refactor does not change candidate order, non-monotone evaluation, failure propagation, fee inclusion, or selected maximum lots.

## 10. Validation and Independent Review

- [x] 10.1 Format production and test sources and run focused economics compilation, behavioral tests, and property tests under strict warnings.
- [x] 10.2 Run the packaged downstream compiler-boundary suite and inspect positive and negative diagnostics for owner leakage, unrelated failures, or over-restriction.
- [x] 10.3 Run one unretried full clean multi-module test command, quantities/economics artifact audits, formatting checks, strict OpenSpec validation, and Git diff checks using the repository's supported single-invocation build model.
- [x] 10.4 Inspect the complete staged diff for formula drift, quantities API changes, public authority leakage, duplicate representations, compatibility remnants, speculative Contract/Listing or payoff generalization, and unrelated scope.
- [x] 10.5 Obtain a fresh independent review of the fully staged implementation and validation evidence; only finalization after approval may complete this task, and any remediation SHALL return to a new independent review. During finalization, invoke `openspec archive reshape-instrument-domain-model --yes` exactly once, then normalize only the archive-generated canonical specs `openspec/specs/instrument-economics/spec.md`, `openspec/specs/order-scenarios/spec.md`, and `openspec/specs/fee-inclusive-pnl/spec.md` to exactly one terminal newline before post-archive Git validation.
