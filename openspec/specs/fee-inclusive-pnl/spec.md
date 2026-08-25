# fee-inclusive-pnl Specification

## Purpose
Defines contextual, grid-aware trading fees and exact settlement-denominated PnL that combines universal price PnL with the signed fee contribution of complete order scenarios.
## Requirements
### Requirement: Contextual instrument-bound fee schedules
A fee schedule SHALL be bound to one stable instrument but SHALL remain a separate contextual value rather than an immutable field of that instrument. A schedule MAY capture venue, account, tier, effective version, and other policy inputs. It SHALL calculate zero or more fees from a complete order scenario and SHALL be able to inspect the order mechanics and every liquidity slice instead of assuming that liquidity role alone determines the fee.

The public schedule contract SHALL permit percentage, flat, tiered, minimum, order-type-sensitive, visibility-sensitive, and multi-component policies without product-family branches in `Instrument`. A percentage schedule SHALL define whether its basis is in base, quote, settle, or another explicit asset.

#### Scenario: Apply different maker and taker rates
- **WHEN** a mixed-liquidity order scenario contains maker and taker slices under a schedule with distinct rates
- **THEN** the schedule evaluates the applicable quantities under their respective rates

#### Scenario: Capture account-specific terms outside Instrument
- **WHEN** two accounts have different fee tiers for the same stable instrument
- **THEN** they use distinct fee-schedule values without constructing different instruments

#### Scenario: Inspect order mechanics when required
- **WHEN** a venue policy assigns special treatment to a visibility or order instruction
- **THEN** its schedule can inspect that instruction in addition to the slice liquidity role

### Requirement: Signed grid-constrained fees
A calculated `Fee` SHALL retain its semantic kind, original registered asset, contextual registered grid, signed grid amount, and exact quantization residual. Fee amount sign SHALL be from the account perspective: a charge is negative and a rebate is positive. A quoted percentage `FeeRate` SHALL use the schedule convention that a positive rate is a charge and a negative rate is a rebate, so applying it to a nonnegative basis negates the rate when producing the account contribution.

Every exact fee basis calculation SHALL remain rational until an explicit schedule-selected quantization operation produces the grid amount and residual. The result SHALL satisfy exact conservation between the unrounded source, the grid amount's exact embedding, and the residual. Fee asset or grid SHALL NOT be inferred solely from the instrument settlement asset.

#### Scenario: Calculate a taker charge
- **WHEN** a positive taker rate is applied to a nonnegative fee basis
- **THEN** the resulting fee contribution is negative in the selected fee asset

#### Scenario: Calculate a maker rebate
- **WHEN** a negative maker rate is applied to a nonnegative fee basis
- **THEN** the resulting fee contribution is positive in the selected fee asset

#### Scenario: Preserve fee rounding residual
- **WHEN** an exact fee is not representable on the venue fee grid
- **THEN** explicit quantization returns a grid amount and exact residual that reconstruct the unrounded fee

#### Scenario: Charge a third asset
- **WHEN** a policy denominates a fee in an asset other than base, quote, or settle
- **THEN** the fee retains that asset and its own grid rather than being silently relabeled as settlement currency

### Requirement: Exact fee-inclusive PnL breakdown
PnL for a complete round-trip scenario SHALL contain exact price PnL in the instrument settlement dimension, the original calculated fees, each fee's exact contribution converted to settle using the market state of its corresponding liquidity slice or leg, the exact total fee PnL, and exact net PnL. It SHALL calculate:

```text
netPnl = pricePnl + sum(feeContributionInSettle)
```

Charges therefore reduce net PnL and rebates increase it. Fee conversion SHALL use the fee's original asset and an explicit settle-targeted conversion from the associated market state. Missing or contradictory conversion information SHALL fail PnL calculation rather than defaulting to identity, parity, or another market state.

#### Scenario: Subtract entry and exit charges
- **WHEN** both round-trip legs produce negative fees
- **THEN** fee PnL is their exact settlement-denominated sum and net PnL is below price PnL by that amount

#### Scenario: Include a maker rebate
- **WHEN** one leg produces a positive maker rebate
- **THEN** the rebate increases fee PnL and net PnL exactly

#### Scenario: Convert fees at their corresponding states
- **WHEN** entry and exit fees share an asset but their market states supply different settle conversions
- **THEN** each fee is converted with its own leg or slice state before summation

#### Scenario: Reject a missing fee conversion
- **WHEN** a fee's original asset has no settle-targeted conversion in its associated market state
- **THEN** PnL calculation fails and preserves the missing asset identity for diagnosis

### Requirement: Scenario context carries epistemic status
Fee and PnL values SHALL be deterministic exact results for the order, market, liquidity, fee-policy, conversion, and rounding inputs supplied to them. They SHALL be named `Fee` and `Pnl` rather than asymmetrically labeling the fee as estimated while leaving PnL unqualified. Hypothetical status SHALL be conveyed by the enclosing order or trade scenario. Future execution provenance SHALL NOT be pre-modeled through `ReportedFee`, fill, or execution types in this capability.

#### Scenario: Re-evaluate identical inputs
- **WHEN** the same complete trade scenario and immutable fee schedule are evaluated twice
- **THEN** every fee, residual, price PnL, fee PnL, and net PnL value is exactly equal

#### Scenario: Change a scenario assumption
- **WHEN** the assumed matched price or liquidity allocation changes
- **THEN** the calculated Fee or PnL may change while the immutable Order remains unchanged

#### Scenario: Avoid execution provenance in planning
- **WHEN** a caller calculates scenario PnL before any order is submitted
- **THEN** no execution ID, fill record, venue-reported fee, or ledger entry is required

### Requirement: PnL scope is explicit
The PnL introduced by this capability SHALL cover price PnL and trading fees represented by its supplied complete order scenarios. It SHALL NOT silently include funding, interest, liquidation penalties, margin effects, settlement events, tax, deposits, withdrawals, or unrelated account cashflows. Future signed adjustments MAY compose with the exact result through separately specified capabilities.

#### Scenario: Exclude funding from trading-fee PnL
- **WHEN** a perpetual-position scenario has no separately modeled funding input
- **THEN** its PnL contains price and trading-fee components only

#### Scenario: Preserve component visibility
- **WHEN** a caller inspects a PnL result
- **THEN** price PnL, fee lines, fee PnL, and net PnL remain separately observable
