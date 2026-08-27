## MODIFIED Requirements

### Requirement: Contextual instrument-bound fee schedules
A fee schedule SHALL retain one stable runtime `InstrumentId` and SHALL remain a separate contextual value rather than immutable instrument metadata. Fee schedules, denominations, fee lines, and fees SHALL use ordinary non-owner-parameterized domain types. A schedule MAY capture venue, account, tier, effective version, and other policy inputs. It SHALL calculate zero or more fees from a complete order scenario and SHALL be able to inspect the order's explicit activation and execution alternatives and every liquidity slice.

The instrument's `fees` capability SHALL construct a validated `FeeDenomination` from an explicit registered asset, matching registered grid, and quantization policy. A denomination SHALL retain the dependent relationship between its asset and grid, validate registry and dimension coherence once, retain the runtime instrument identity, and provide subsequent fee construction without requiring callers to repeat or manually align those witnesses. The capability SHALL also provide checked fee-line attribution, the no-fee schedule, and direct schedule composition.

A fee schedule SHALL return its zero, one, or many checked fee lines directly as an immutable vector. Schedule evaluation and composition SHALL validate that schedules, scenarios, denominations, and fee lines carry the same runtime `InstrumentId`. The public schedule contract SHALL continue to permit percentage, flat, tiered, minimum, order-type-sensitive, visibility-sensitive, and multi-component policy without product-family branches in `Instrument`.

#### Scenario: Reuse a validated denomination
- **WHEN** a schedule charges several components in one asset, grid, quantization policy, and runtime instrument identity
- **THEN** it validates that denomination once and uses it for each fee calculation without repeating existential grid parameters

#### Scenario: Reject an incoherent denomination
- **WHEN** a fee asset and grid have different dimensions or registry provenance
- **THEN** denomination construction fails before a fee amount is calculated

#### Scenario: Apply different maker and taker rates
- **WHEN** a mixed-liquidity scenario contains maker and taker slices under a schedule with distinct rates
- **THEN** the schedule evaluates the applicable quantities under their respective rates

#### Scenario: Capture account-specific terms outside Instrument
- **WHEN** two accounts have different fee tiers for the same stable instrument
- **THEN** they use distinct fee schedules and may reuse or select different validated denominations without constructing different instruments

#### Scenario: Inspect order mechanics when required
- **WHEN** venue policy assigns special treatment to an execution instruction or visibility alternative
- **THEN** its schedule can inspect that explicit alternative in addition to slice liquidity role

#### Scenario: Compose zero or many schedule components directly
- **WHEN** a caller has an immutable vector of component fee schedules carrying the same runtime instrument identity
- **THEN** `fees` combines their checked fee-line vectors without a pass-through collection wrapper

#### Scenario: Reject a foreign schedule or scenario
- **WHEN** schedule composition or evaluation receives a schedule or scenario carrying a different runtime `InstrumentId`
- **THEN** the checked boundary returns a typed instrument-mismatch failure

### Requirement: Signed grid-constrained fees
A calculated `Fee` SHALL retain its stable runtime `InstrumentId`, semantic kind, original registered asset, validated fee denomination, signed grid amount, exact quantization residual, and unrounded exact amount. These intrinsic observations SHALL be available directly from the fee value rather than through functions requiring the fee to be passed back to its instrument. Fee amount sign SHALL remain from the account perspective: a charge is negative and a rebate is positive. A quoted percentage `FeeRate` SHALL retain the policy convention that a positive rate is a charge and a negative rate is a rebate.

Every exact fee basis calculation SHALL remain rational until the selected validated denomination explicitly quantizes it. Percentage fee construction SHALL accept the semantic kind, nonnegative basis in the denomination asset, and fee rate; exact quantization SHALL accept kind and unrounded amount in that asset. Both SHALL reuse the denomination's already checked asset, grid, policy, and runtime instrument identity. The result SHALL conserve the exact unrounded amount as grid amount plus residual. A fee asset or grid SHALL NOT be inferred solely from instrument settlement.

#### Scenario: Calculate a taker charge
- **WHEN** a positive taker rate is applied to a nonnegative basis through a validated denomination
- **THEN** the resulting fee contribution is negative in that denomination's asset

#### Scenario: Calculate a maker rebate
- **WHEN** a negative maker rate is applied to a nonnegative basis through a validated denomination
- **THEN** the resulting fee contribution is positive in that denomination's asset

#### Scenario: Preserve fee rounding residual
- **WHEN** an exact fee is not representable on the denomination grid
- **THEN** the fee directly exposes the quantized amount, exact residual, and unrounded amount that satisfy conservation

#### Scenario: Charge a third asset
- **WHEN** a policy constructs a denomination for an asset other than base, quote, or settle
- **THEN** the fee retains that asset and its own checked grid rather than being relabeled as settlement currency

#### Scenario: Keep denomination ownership coherent
- **WHEN** checked fee construction receives a denomination carrying a different runtime `InstrumentId` from its scenario or target instrument
- **THEN** construction returns a typed instrument-mismatch failure

### Requirement: Exact fee-inclusive PnL breakdown
The instrument's `valuation` capability SHALL calculate PnL for a complete round-trip scenario. The round trip, schedules, fees, fee lines, market states, and target instrument SHALL carry one runtime `InstrumentId`, and valuation SHALL reject a mismatch before combining them. The result SHALL contain exact price PnL in the instrument settlement dimension, the original calculated fees, each fee's exact contribution converted to settle using the market state of its corresponding liquidity slice or leg, the exact total fee PnL, and exact net PnL. It SHALL calculate:

```text
netPnl = pricePnl + sum(feeContributionInSettle)
```

Charges therefore reduce net PnL and rebates increase it. Fee conversion SHALL use the fee's original asset and an explicit settle-targeted conversion from the associated market state. Missing or contradictory conversion information SHALL fail PnL calculation rather than defaulting to identity, parity, or another market state.

#### Scenario: Subtract entry and exit charges
- **WHEN** both round-trip legs produce negative fees
- **THEN** `valuation` returns fee PnL as their exact settlement-denominated sum and net PnL below price PnL by that amount

#### Scenario: Include a maker rebate
- **WHEN** one leg produces a positive maker rebate
- **THEN** the rebate increases fee PnL and net PnL exactly

#### Scenario: Convert fees at their corresponding states
- **WHEN** entry and exit fees share an asset but their market states supply different settle conversions
- **THEN** each fee is converted with its own leg or slice state before summation

#### Scenario: Reject a missing fee conversion
- **WHEN** a fee's original asset has no settle-targeted conversion in its associated market state
- **THEN** PnL calculation fails and preserves the missing asset identity for diagnosis

#### Scenario: Reject foreign valuation inputs
- **WHEN** a round trip, schedule, fee line, fee, or market state carries a runtime `InstrumentId` different from the target instrument
- **THEN** valuation returns a typed instrument-mismatch failure rather than combining the values
