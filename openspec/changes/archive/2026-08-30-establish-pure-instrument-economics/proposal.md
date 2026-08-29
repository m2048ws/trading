## Why

The current economics artifact makes `Instrument` the owner and entry point for orders, execution scenarios, fee policy, valuation, and risk sizing. That shape mixes the immutable economic meaning of an instrument with downstream decisions and orchestration, making future effectful application capabilities harder to add without contaminating the mathematical core.

This change establishes a small, pure `trading-instrument-economics` foundation whose values retain the quantity and reference-data proofs established at assembly, while every policy or workflow layer depends on that foundation in only one direction.

## What Changes

- Add a dedicated `trading-instrument-economics` artifact, depending only on `trading-quantities` and `trading-reference-data`, for assembled instrument specifications and pure economic values.
- Make `Instrument` an immutable value rather than a service locator. Intrinsic lot, position, price, market-state, settlement-conversion, fee-value, and valuation operations remain discoverable through pure companion or contextual APIs.
- Preserve typed quantities, rates, grid membership, asset endpoints, and instrument identity through calculations instead of erasing them into unqualified `Rational` values and reconstructing their meaning later.
- Validate market-state and fee-denomination relationships once at pure construction boundaries; subsequent valuation performs no live catalog lookup, locking, effect execution, or registry-provenance check.
- Define price PnL and fee-inclusive PnL as exact composition over already validated, settlement-denominated contributions.
- **BREAKING** Remove `Instrument.orders`, `Instrument.scenarios`, `Instrument.fees`, `Instrument.valuation`, and `Instrument.sizing` as instrument-owned capability views.
- **BREAKING** Move side-directed position changes, order/scenario factories, fee-schedule evaluation, and risk sizing behind downstream APIs that consume an `Instrument`.
- Keep order/scenario, fee-policy, and risk behavior temporarily buildable from the existing aggregate artifact while Proposals 5–7 move each concern into its final artifact.
- Do not add effect parameters, tagless-final algebras, persistence formats, market-data acquisition, or runtime coordination to the pure economics artifact.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: Narrow the artifact and API to proof-preserving, pure instrument values and valuation, with downstream concerns depending on `Instrument` rather than being owned by it.
- `order-scenarios`: Replace instrument-owned order and scenario capability views with external pure APIs that consume an instrument.
- `fee-inclusive-pnl`: Separate fee denominations, fee values, and exact PnL composition from contextual fee-schedule policy and scenario orchestration.
- `position-risk-sizing`: Replace instrument-owned sizing with a downstream risk API that consumes instrument economics.

## Impact

- Adds the `instrumentEconomics` SBT project and `instrument-economics/` source tree.
- Moves assembled instrument types and the pure valuation kernel out of the broad `economics` project; the existing project becomes a transitional downstream aggregate until Proposals 5–7 complete the split.
- Changes public construction and discovery paths for prices, market states, orders, scenarios, fees, valuation, and sizing.
- Requires compile-time module-boundary tests and behavioral equivalence tests for the moved exact calculations.
- Introduces no concrete effect-system dependency and no production I/O.
