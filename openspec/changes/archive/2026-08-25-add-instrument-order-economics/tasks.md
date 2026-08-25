## 1. Economics Module Boundary

- [x] 1.1 Add the `economics` SBT project in `economics/`, publish it as `trading-economics`, depend on `quantities`, and include it in root aggregation and ordered root testing.
- [x] 1.2 Add the `trading.economics` production/test source layout and only the existing Scala standard, MUnit, and ScalaCheck-style test dependencies needed by the new module.
- [x] 1.3 Add nominal `InstrumentId`, `UnderlyingId`, and `FeeKind` values plus the typed economics error hierarchy, preserving quantity/runtime causes and diagnostic IDs, grids, coefficients, legs, and slice locations.
- [x] 1.4 Add reusable economics test fixtures that create registry-owned assets, composite dimensions, registered lot/price/fee grids, and exact rates exclusively through the public quantities API.

## 2. Instrument-Owned Quantities and Valuation

- [x] 2.1 Implement checked `Instrument` construction for underlying/base/quote/position/settle identities, common registry provenance, position and quote-per-base grids, signed contract terms, and nonempty payoff validation.
- [x] 2.2 Implement instrument-owned positive `Lots`, signed `PositionLots`, and `Side` conversion with exact lot-grid embedding, count access, flat zero support, and compile-time separation between stable instrument paths.
- [x] 2.3 Implement instrument-owned positive grid-valid `Price` with coordinate and exact-value constructors, explicit residual-bearing quantization, and no implicit rounding.
- [x] 2.4 Implement heterogeneous settle-targeted conversion entries and immutable `SettlementConversions`, including exact settlement identity, checked extra fee-asset entries, duplicate rejection, registry/endpoint validation, and type-safe lookup.
- [x] 2.5 Implement coherent `MarketState` constructors for quote settlement, base settlement, third-asset settlement from either anchor, and checked dual anchors satisfying `baseToSettle = price.andThen(quoteToSettle)` exactly.
- [x] 2.6 Implement settle value per position, exact signed position value, and entry/exit price PnL through the universal signed base/quote contract-term formula with lot quantum included.
- [x] 2.7 Add constructor and property tests for positive/invalid lots and prices, foreign or mismatched grids, empty payoffs, conversion identity/coherence failures, long/short symmetry, exact inverse residual values, and explicit additional-asset conversion lookup.
- [x] 2.8 Add representative spot-like, linear, inverse, and quanto-style definitions proving all four use the same valuation path without public product-family flags.

## 3. Orders and Complete Scenarios

- [x] 3.1 Implement the order instruction vocabulary for activation, price instruction, time in force, liquidity constraint, position effect, visibility, price reference, trigger comparison, and maker/taker `LiquidityRole`.
- [x] 3.2 Implement instrument-owned fixed and trailing triggers, signed peg offsets, and grid-aware offset validation without adding observation streams or execution state.
- [x] 3.3 Implement checked immutable `Order` construction and market, limit, stop-market, and stop-limit conveniences, with the universal compatibility matrix for market/maker-only, duration, visibility, and iceberg-lot contradictions.
- [x] 3.4 Implement fixed-trigger, trailing-trigger, and peg-resolution scenario evidence that distinguishes activation/reference prices from matched market states and validates exact tick relationships.
- [x] 3.5 Implement positive `LiquiditySlice` values and checked complete `OrderScenario` construction with nonempty slices, exact lot-total conservation, limit-or-better pricing, peg resolution, trigger satisfaction, and liquidity-role implications.
- [x] 3.6 Implement checked `RoundTripScenario` construction for same-instrument entry and exit scenarios whose signed position changes sum exactly to flat, preserving both legs and the held position.
- [x] 3.7 Add order/scenario tests covering all instruction axes, impossible combinations, stop and peg evidence, all-taker market outcomes, maker-only outcomes, mixed-role multi-price limits, missing trigger evidence, bad limit prices, incomplete totals, and unequal or cross-instrument round trips.

## 4. Contextual Fees

- [x] 4.1 Implement exact signed `FeeRate`, existential grid-constrained `Fee`, and explicit fee quantization so positive policy rates create negative charges, negative rates create positive rebates, and `unrounded = embedded amount + residual` is observable.
- [x] 4.2 Implement checked `FeeLine` attribution that binds each fee to a valid source slice index and captures that slice's exact market state, with no default state for order-wide fees.
- [x] 4.3 Implement the instrument-bound immutable `FeeSchedule` contract and assessment context so policies can inspect the complete order and slices and safely construct zero or more fee lines.
- [x] 4.4 Add percentage-basis and fee-composition helpers sufficient to express base, quote, settle, or explicit third-asset bases; maker/taker rates; flat and minimum components; tier selection; and order-mechanic or visibility-sensitive policies without branching in `Instrument`.
- [x] 4.5 Add fee tests for account-perspective signs, maker rebates, mixed-role rates, account/tier context, order-sensitive policies, flat/minimum and multi-component schedules, third-asset fee grids, per-line rounding, and exact residual conservation.

## 5. Exact Fee-Inclusive PnL

- [x] 5.1 Implement exact multi-slice round-trip price PnL as the negative sum of each slice's signed position change valued at that slice's market state, and prove equivalence to exit-minus-entry valuation for single-state legs.
- [x] 5.2 Assess entry and exit scenarios through the fee schedule and convert every fee's exact embedded grid amount using only its captured source-slice settlement conversion.
- [x] 5.3 Implement converted fee-line and `Pnl` results retaining original fees and residuals, leg/slice attribution, price PnL, exact fee PnL, and exact net PnL, with typed missing-conversion failure.
- [x] 5.4 Add PnL tests for long and short round trips, multiple matched prices, entry and exit charges, maker rebates, distinct per-leg conversion rates, third-asset fees, missing conversions, deterministic reevaluation, and component-sum conservation.
- [x] 5.5 Add API tests confirming the result includes price and trading-fee components only and exposes no funding, margin, liquidation, execution, reported-fee, ledger, or account lifecycle state.

## 6. Downside Risk and Discrete Sizing

- [x] 6.1 Implement exact downside risk as `max(0, -netPnl)` in the instrument settlement dimension without floating-point conversion or quantization.
- [x] 6.2 Implement deterministic ascending exhaustive position sizing from one lot through a positive cap, evaluating each candidate through the ordinary scenario, fee, and PnL path and returning the greatest candidate within a nonnegative risk budget.
- [x] 6.3 Make sizing return `None` when no positive lot fits and propagate the first typed candidate failure in ascending order rather than skipping, treating it as unaffordable, or returning a smaller result.
- [x] 6.4 Add sizing tests for losing/profitable PnL, exact budget boundaries, no-result and capped results, both-leg fees, minimum/tier steps, adjacent fee-grid rounding, deliberately non-monotone risks, missing conversions, invalid exits, and deterministic failure order.
- [x] 6.5 Add property tests comparing sizing with an independently enumerated maximum across generated bounded scenarios, without introducing binary search or a monotonicity assumption.

## 7. Public Artifact and Ownership Boundary

- [x] 7.1 Extend the downstream boundary build wiring to consume completed packaged `trading-economics` and `trading-quantities` artifacts without exposing module classes or private implementation helpers.
- [x] 7.2 Add positive downstream compiler fixtures that construct an instrument, lots, prices, market states, orders, scenarios, a custom fee schedule, PnL, and a sized position through documented public imports.
- [x] 7.3 Add negative downstream compiler fixtures proving instrument constructors/representations remain private, arbitrary fee/market attribution cannot be forged, and equal-looking values from distinct stable instrument paths cannot be mixed in positions, orders, scenarios, PnL, or sizing.
- [x] 7.4 Verify existing quantities-only downstream fixtures still compile and that `trading-quantities` exposes none of the new domain types or construction authority.
- [x] 7.5 Add concise public Scaladoc and an end-to-end example clarifying scenario epistemic status, account-perspective fee signs, explicit conversions, lot inclusion, and the intentionally deferred execution/account concerns.

## 8. Validation

- [x] 8.1 Format production, test, and build sources and run focused economics compilation and test suites with strict warnings.
- [x] 8.2 Run the packaged downstream boundary suite directly, including its positive and negative compiler fixtures under `-Werror` and `-source:future`.
- [x] 8.3 Run the full clean multi-module test command and confirm the pre-existing quantities suite remains green.
- [x] 8.4 Run strict OpenSpec validation and inspect the final diff for accidental quantities-domain changes, untracked lifecycle scope, formatting errors, or generated build artifacts.
- [x] 8.5 Obtain a fresh independent review of the fully staged change and its validation evidence; this task is completed only during finalization after fresh approval and must not be self-certified.
