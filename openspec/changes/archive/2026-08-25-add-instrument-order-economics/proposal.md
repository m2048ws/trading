## Why

The exact-quantity foundation cannot yet describe an instrument, evaluate an order scenario, or calculate fee-inclusive PnL and a risk-constrained lot size. This change introduces the first downstream trading-economics slice while keeping instrument, order, fee, PnL, and risk concepts out of the quantities foundation.

## What Changes

- Add a downstream `trading-economics` module that depends on `trading-quantities` and owns the new domain API.
- Add a validated `Instrument` model with `underlying`, `base`, `quote`, `position`, and `settle` roles; instrument-bound positive `Lots`, signed `PositionLots`, and grid-valid `Price`; and signed base/quote contract terms that normalize spot, linear, inverse, and quanto-style payoff shapes without product-family flags.
- Add coherent market states containing a listing-grid price and explicit settle-targeted conversions, with exact price/conversion agreement and no hidden conversion lookup or rounding.
- Add immutable orders whose activation, price instruction, time in force, liquidity constraint, position effect, and visibility are modeled independently, plus checked order scenarios whose liquidity slices carry assumed price/conversion state and maker/taker classification.
- Add contextual fee schedules and signed, grid-constrained fees in their original assets, including maker rebates and explicit fee quantization residuals.
- Add exact fee-inclusive PnL in the instrument settlement asset and discrete-lot risk sizing that evaluates complete scenarios rather than assuming linear per-lot costs.
- Add public API, behavioral, property, and downstream-consumer tests for the new module.
- Explicitly defer order submission/lifecycle state, fills and partial-fill records, venue-reported fees, accounts, margin/liquidation, funding, ledgers, wallets, and reconciliation.

## Capabilities

### New Capabilities

- `instrument-economics`: Validated instruments, instrument-bound lots/prices/positions, coherent market states, settlement conversions, and universal exact contract valuation.
- `order-scenarios`: Compositional immutable order mechanics and checked hypothetical order outcomes with per-slice price and liquidity classification, without execution lifecycle state.
- `fee-inclusive-pnl`: Contextual fee calculation, explicit fee quantization, conversion of signed fees to settlement value, and exact price/fee/net PnL breakdowns.
- `position-risk-sizing`: Discrete-lot downside-risk evaluation and maximum-lot selection from complete fee-inclusive trade scenarios.

### Modified Capabilities

None.

## Impact

- Adds a new SBT subproject and published artifact downstream of `quantities`; the existing quantities module remains domain-neutral and its public requirements are unchanged.
- Adds new public Scala APIs for instruments, market states, orders, order scenarios, fee schedules, PnL, and risk sizing.
- Adds tests that consume the packaged/public quantities API and exercise representative spot, linear, inverse, quanto-style, mixed-liquidity, third-fee-asset, and nonlinear fee-rounding cases.
- Requires no new external runtime dependency beyond the existing exact-quantity foundation and Scala standard library unless implementation evidence demonstrates a narrowly justified need.
