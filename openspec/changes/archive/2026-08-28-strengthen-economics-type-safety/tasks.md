## 1. Dependency and Validation Foundations

- [x] 1.1 Add `cats-core` as an economics compile dependency without changing the quantities artifact dependency surface.
- [x] 1.2 Define closed definition, activation, pricing, and scenario violation ADTs plus domain-owned non-empty aggregate errors and mappings to the existing fail-fast `EconomicsError` hierarchy.
- [x] 1.3 Implement small internal `ensure`, indexed-rule, and stage-composition helpers with one deterministic rule order, using applicative accumulation only for independent checks and `Either` for dependent transitions.
- [x] 1.4 Add focused tests proving aggregate ordering, first-error projection, serialization/product behavior of public errors, and absence of Cats error-container types from public signatures.

## 2. Proof-Carrying Instrument Definitions

- [x] 2.1 Add sealed, constructor-private `ValidatedDefinition` with package-private dependent position-grid, price-grid, and payoff-rate evidence tied to its exact raw roles.
- [x] 2.2 Implement accumulating raw-definition validation for component identity, registry provenance, grid dimensions, base/quote distinction, and nonempty payoff, followed by checked evidence narrowing only after its prerequisites succeed.
- [x] 2.3 Add total `Instrument.fromValidated` construction and refactor `Instrument.create(Definition)` to project the same staged rules' first error while preserving its established deterministic behavior.
- [x] 2.4 Remove repeated grid and payoff casts from `Instrument` capability initialization so all later capabilities consume the validated evidence directly.
- [x] 2.5 Add unit/property tests for coherent validation, multiple independent violations, prerequisite-dependent suppression, fail-fast equivalence, and unchanged successful instrument components.
- [x] 2.6 Add downstream positive and negative compiler fixtures proving ordinary validated construction is ergonomic while callers cannot construct `ValidatedDefinition` or extract reusable retagging authority.

## 3. Typed Market Conversion and Valuation

- [x] 3.1 Change `SettlementConversion` and `MarketState` conversion storage to retain dependent source-to-settle rates while preserving a coefficient observer and immutable source ordering.
- [x] 3.2 Refactor quote-, base-, and dual-anchor market constructors to derive rates through typed composition or checked reciprocal/cross-rate operations and keep scalar access only for positivity, identity, and coherence diagnostics.
- [x] 3.3 Refactor heterogeneous settlement lookup to validate asset identity, registry provenance, and runtime dimension equality before aligning the source quantity and applying the stored rate.
- [x] 3.4 Rewrite settle-per-position, position value, price PnL, scenario price PnL, fee conversion totals, net PnL, and downside risk with `Rate.andThen`, `Quantity.applyRate`, typed arithmetic, and authoritative typed zero values.
- [x] 3.5 Add exact property/regression tests comparing typed results with the established rational formulas across linear, inverse, quanto-style, long, short, fee, and off-grid settlement cases.
- [x] 3.6 Extend downstream compiler and runtime boundary coverage for reversed rate endpoints, additional conversion rate observation, foreign registries, checked heterogeneous lookup, and preserved grid-provenance independence.

## 4. Activation and Pricing Evidence

- [x] 4.1 Introduce associated activation evidence types and prototype their stable-path usage in real downstream positive and negative fixtures before migrating the full scenario API.
- [x] 4.2 Implement immediate, fixed, and trailing evidence construction through the corresponding activation values, deriving activation-owned reference/comparison data and retaining typed semantic failures for unsatisfied triggers and invalid thresholds.
- [x] 4.3 Introduce associated pricing resolution types and `EffectivePricing` market/limited alternatives; make direct and pegged resolution construct only their own valid shape and validate peg semantics at the resolution boundary.
- [x] 4.4 Update order and execution representations or focused helper methods as needed to retain ergonomic access to the stable activation/pricing paths without adding activation and pricing indices to every public domain type.
- [x] 4.5 Add behavioral and compiler tests proving missing, extraneous, fixed-versus-trailing, and direct-versus-pegged combinations have no supported construction path while semantic failures remain observable.
- [x] 4.6 Reject same-shape fixed, trailing, and pegged evidence replay when its captured activation or pricing semantics differ from the current value, and retain immutable-JAR public-source replay regressions plus same-instance concrete and generic positive counterparts.

## 5. Non-Empty and Accumulating Scenario Construction

- [x] 5.1 Replace matched-slice `Vector` inputs with `NonEmptyVector`, add concise one/many/adaptation helpers, and migrate scenario, fee, valuation, and public example consumers.
- [x] 5.2 Redesign scenario-assumption construction to consume evidence and resolution associated with the target order's activation and execution shapes, without reintroducing independent assumption ADTs or a parallel scalar view.
- [x] 5.3 Refactor scenario validation into ordered identity, lot-conservation, activation, pricing, and per-slice stages, using `EffectivePricing` rather than `Option[BigInt]` and accumulating independent indexed slice violations.
- [x] 5.4 Add the accumulating scenario diagnostic entry point and derive the existing fail-fast scenario constructor from the same staged rules and first-error mapping.
- [x] 5.5 Remove unreachable mismatch-only scenario error cases and replace correlated optional error context with structured or refined context where this change directly touches it.
- [x] 5.6 Add tests for non-empty construction, exact lot totals, stable multi-slice diagnostics, deterministic fail-fast equivalence, activation/pricing semantics, liquidity constraints, limit quality, and runtime instrument mismatch.

## 6. Selective Functional Cleanup and Sizing

- [x] 6.1 Replace hand-written fail-fast `Vector[Either]` folds in the affected economics paths with `Traverse` while preserving evaluation and failure order.
- [x] 6.2 Use `Chain` for internal fee-line and converted-line accumulation, converting to the established public immutable result collection only at the boundary.
- [x] 6.3 Replace mutable exhaustive sizing and non-local return with a pure tail-recursive or `tailRecM` state transition that still evaluates every candidate in ascending order and stops on the first typed failure.
- [x] 6.4 Add sizing and fee-composition regressions for non-monotone/minimum-fee behavior, per-candidate quantization, component order, first failure propagation, and exact greatest-affordable selection.

## 7. Migration, Verification, and Review Gate

- [x] 7.1 Update all economics tests, end-to-end examples, Scaladoc, and immutable-JAR client fixtures to the validated-definition, associated-evidence, effective-pricing, and non-empty-slice APIs without compatibility aliases.
- [x] 7.2 Run Scala and SBT formatting for every changed source and verify `scalafmtCheckAll`, `scalafmtSbtCheck`, and `git diff --check` pass.
- [x] 7.3 Run focused economics tests and the adversarial/compiler-boundary suites, including every new positive and negative public API fixture.
- [x] 7.4 Run `sbt -batch clean test` and confirm exact quantity, economics, and downstream boundary suites all pass from a clean build.
- [x] 7.5 Run strict OpenSpec validation for `strengthen-economics-type-safety`, inspect the staged scope against the proposal, and stage exactly the intended implementation, tests, build, and active-change artifacts.
- [x] 7.6 Obtain a fresh independent review of the fully staged implementation and validation evidence; only finalization after approval may complete this task, and any remediation SHALL return to another fresh independent review.
