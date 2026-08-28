## 1. Portfolio and Baseline Gates

- [ ] 1.1 Confirm Proposals 0–9 are complete and approved for ordered application and Proposals 0–5 have landed before
  editing fee-policy or scenario-valuation production code.
- [ ] 1.2 Refresh Git/OpenSpec state and record a clean baseline for formatting, compilation, exact/property tests,
  external-artifact compilation, and current fee/PnL behavior.
- [ ] 1.3 Inventory fee value, policy, attribution, scenario-price, PnL, risk, example, and error call sites; map each to
  instrument core, execution scenario, fee policy, or later risk/application ownership.

## 2. Fee-Policy Module Boundary

- [ ] 2.1 Add `feePolicy` in `fee-policy/`, artifact `trading-fee-policy`, package `trading.fee`, with only direct
  quantities/instrument-economics/order-model/execution-scenario plus agreed pure library/test dependencies.
- [ ] 2.2 Wire root aggregation, the completed-JAR external-artifact task, adversarial/compiler-test classpaths, and the
  transitional risk aggregate's downward dependency.
- [ ] 2.3 Add compiler guards proving quantities, reference data, instrument economics, order model, and execution
  scenario cannot access fee policy and that fee policy cannot access risk/application/runtime/effects.

## 3. Refined Fee Mathematics

- [ ] 3.1 Move `FeeRate` to `trading.fee` as a nominal exact policy scalar with documented positive-charge and negative-
  rebate convention.
- [ ] 3.2 Implement percentage contribution from `NonNegative[Quantity[D]]` by typed quantity scaling and return
  `Quantity[D]` without coefficient-first reconstruction.
- [ ] 3.3 Implement total typed minimum-charge adjustment from `Quantity[D]` and `NonNegative[Quantity[D]]`, preserving
  rebates/zero and returning typed negative minimum only for smaller charges.
- [ ] 3.4 Update policy helpers to pass exact typed contributions through core `FeeDenomination` quantization separately
  per component; remove percentage/minimum methods and raw helpers from the denomination/core.
- [ ] 3.5 Add example/property tests for rate sign, negative-basis refinement failure, minimum boundaries, rebate
  preservation, dimension preservation, and fee quantization conservation.

## 4. Pure Policy Strategy and Composition

- [ ] 4.1 Implement covariant typed `FeePolicy[E, ...]` and public domain-owned non-empty `PolicyErrors[E]` without
  exposing a validation-library collection or `F[_]`.
- [ ] 4.2 Add nonnegative `SliceIndex` and existential `FeeDirective` coupling each core fee to only a requested source
  index, with no market/slice/scenario field.
- [ ] 4.3 Implement the total no-fee `FeePolicy[Nothing, ...]` and checked same-instrument composition over zero or more
  policies.
- [ ] 4.4 Normalize nested composition, accumulate all component policy failures in stable order, and concatenate
  directives in component/directive order.
- [ ] 4.5 Preserve custom typed policy causes through composition and support widening distinct implementations into a
  caller-owned sum error type.
- [ ] 4.6 Add contextual identity/associativity law tests and negative compile/API tests proving no unconditional global
  `Monoid[FeePolicy]`, effect parameter, exception/string error erasure, or old `FeeSchedule` path exists.

## 5. Validated Scenario Fee Assessment

- [ ] 5.1 Implement existential `AssessedFee` coupling one directive's typed fee to the actual selected immutable slice,
  index, and market state.
- [ ] 5.2 Implement `ScenarioFees` as one owned `OrderScenario` plus an immutable assessed-fee vector with projected
  identity and no duplicate target fields.
- [ ] 5.3 Add focused policy/assessment location and violation ADTs plus generic non-empty ordered aggregates retaining
  typed policy causes.
- [ ] 5.4 Implement canonical assessment identity and policy-evaluation stages, suppressing dependent work on foreign
  inputs.
- [ ] 5.5 Validate every directive fee/denomination identity and index range, accumulate independent output failures,
  select source slices centrally, and construct `ScenarioFees` only on full success.
- [ ] 5.6 Delete caller-supplied source markets, arbitrary final fee-line constructors, source-market `.eq` checks,
  `FeeLine`, `Fees`, universal fee errors, and compatibility aliases.
- [ ] 5.7 Test empty output, multiple heterogeneous fee assets, duplicate/equal-looking slices, invalid indices,
  multiple invalid directives, foreign fee output, stable order, and inability to attach arbitrary markets.

## 6. Exact Scenario Price Normalization

- [ ] 6.1 Add closed `RoundTripLeg` and focused scenario-valuation errors to the execution-scenario artifact without a
  reverse fee-policy dependency.
- [ ] 6.2 Add a typed order-intent operation for deriving each slice's signed `PositionLots` from side and positive lots
  without exposing raw sign multiplication downstream.
- [ ] 6.3 Implement per-slice exact cashflow as the negated core position value at that slice's own market state and fold
  entry/exit values with settlement-quantity algebra into core `PricePnl`.
- [ ] 6.4 Add one-slice equivalence tests against core exit-minus-entry and multi-slice weighted tests for long/short,
  linear/inverse/quanto, third-asset settlement, and off-grid exactness.
- [ ] 6.5 Add module/API checks proving scenario price normalization works without fee policy and contains no fee,
  quantization, catalog, effect, or raw-scalar average-price shortcut.

## 7. Fee-Inclusive Round-Trip Evaluation

- [ ] 7.1 Implement `RoundTripFeePolicies[E, ...]` with explicit entry/exit fields and a `same` convenience that preserves
  the canonical product.
- [ ] 7.2 Implement the initial instrument/round-trip/policy identity stage with closed leg locations and suppression of
  dependent work on mismatched branches.
- [ ] 7.3 Evaluate scenario price PnL plus entry and exit policy assessments as independent eligible branches and
  accumulate typed failures in stable order.
- [ ] 7.4 Convert every assessed fee through its selected slice market, retaining core settled contributions and
  accumulating missing/invalid conversions by leg, slice, and directive.
- [ ] 7.5 Invoke the core `Pnl` constructor only when price and all conversion prerequisites succeed; use its typed totals
  as the sole price/fee/net source.
- [ ] 7.6 Implement the scenario-level success result retaining round trip, per-leg `ScenarioFees`, attributed converted
  contributions, and the core PnL without duplicating calculated totals.
- [ ] 7.7 Implement generic typed fee-inclusive violation aggregation and optional first-error head projection without
  catch-all exceptions or free-form message causes.
- [ ] 7.8 Test different policies per leg, same-policy convenience, no fees, charges, rebates, third assets, per-slice
  conversions, multiple missing conversions, policy failures on both legs, identity suppression, stable ordering, and
  successful attribution visibility.

## 8. Downstream Migration and Removal

- [ ] 8.1 Move all remaining fee-policy source/tests/examples from the transitional `economics` artifact into
  `feePolicy` and update packages/imports.
- [ ] 8.2 Migrate risk/sizing to generic policy failures, explicit leg policies, and scenario-level PnL results without
  changing search semantics before Proposal 7.
- [ ] 8.3 Remove `instrument.fees`, `instrument.valuation.pnl`, `FeeSchedule`, `FeeLine`, `Fees`, old converted-line
  orchestration, old error mappings, and duplicate forwarders with no compatibility aliases.
- [ ] 8.4 Verify policy acquisition, clock/account/version selection, audit envelopes, and execution reports remain absent
  and explicitly deferred to application/runtime or codec proposals.

## 9. Verification and Steward Handoff

- [ ] 9.1 Format all affected Scala/SBT sources and run clean compilation in dependency order.
- [ ] 9.2 Run complete exact/refinement/unit/property/law suites, scenario and risk dependents, negative compilation,
  external-JAR boundaries, adversarial tests, and the full repository validation matrix.
- [ ] 9.3 Inspect packaged APIs and production imports for forbidden reverse dependencies, `F[_]`, effect libraries,
  arbitrary source markets, object identity checks, raw `Rational` kernels, global unlawful policy instances, and old
  capability names.
- [ ] 9.4 Run strict validation for this and all active OpenSpec changes and reconcile any artifact drift discovered
  during implementation.
- [ ] 9.5 Prepare the validated worktree for fresh independent review without self-certifying, committing, archiving, or
  beginning Proposal 7 outside steward authorization.
