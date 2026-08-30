## MODIFIED Requirements

### Requirement: Contextual instrument-bound fee schedules
A fee schedule SHALL retain one stable runtime `InstrumentId` and SHALL remain a downstream contextual policy rather
than instrument metadata or a service exposed by `Instrument`. A schedule MAY capture venue, account, tier, effective
version, order mechanics, liquidity role, and other policy inputs. It SHALL consume an explicit `Instrument` and
complete order scenario and calculate zero or more attributed fee lines without introducing policy branches into
instrument economics.

A pure fee-denomination constructor in instrument economics SHALL consume an explicit instrument, trusted asset,
matching trusted grid handle, and quantization policy. A `FeeDenomination` SHALL retain the dependent asset/grid
relationship, validate immutable lineage, dimension coherence, and runtime instrument identity once, and allow exact
fee-value construction without repeating existential grid alignment or catalog resolution.

The downstream fee-policy boundary SHALL own percentage rates, minimums, schedule selection, fee-line attribution,
no-fee policy, and schedule composition. Schedule evaluation and composition SHALL validate ordinary runtime
instrument identity and return zero or more fee lines directly as an immutable vector.

#### Scenario: Reuse a validated denomination
- **WHEN** a schedule charges several components in one asset, grid handle, quantization policy, and instrument identity
- **THEN** it reuses one denomination without repeating dimension or lineage validation and without consulting a catalog

#### Scenario: Reject an incoherent denomination
- **WHEN** a fee asset and grid handle have different dimensions or issuer lineage
- **THEN** denomination construction fails before any fee policy is evaluated

#### Scenario: Apply different maker and taker rates
- **WHEN** a mixed-liquidity scenario is evaluated under a schedule with distinct maker and taker rates
- **THEN** the downstream fee policy evaluates the applicable quantities under their respective rates

#### Scenario: Capture account-specific terms outside Instrument
- **WHEN** two accounts have different fee tiers for the same stable instrument
- **THEN** they use distinct schedules while reusing the same immutable instrument

#### Scenario: Inspect order mechanics when required
- **WHEN** venue policy assigns special treatment to an execution instruction or visibility alternative
- **THEN** the schedule may inspect that alternative while the instrument-economics artifact remains independent of
  order types

#### Scenario: Compose zero or many schedule components directly
- **WHEN** a caller has an immutable vector of component schedules for one instrument
- **THEN** the downstream fee-policy boundary combines their checked fee-line vectors without a pass-through collection
  wrapper

#### Scenario: Reject a foreign schedule or scenario
- **WHEN** schedule composition or evaluation receives an explicit instrument, schedule, or scenario carrying a
  different `InstrumentId`
- **THEN** it returns a typed instrument-mismatch failure

### Requirement: Signed grid-constrained fees
A calculated `Fee` SHALL be a pure instrument-economic value retaining its stable `InstrumentId`, semantic kind,
original trusted asset, validated denomination, signed grid amount, exact typed quantization residual, and unrounded
exact typed amount. Intrinsic observations SHALL be available directly from the value. Fee sign SHALL be from the
account perspective: a charge is negative and a rebate is positive.

The pure denomination boundary SHALL construct a `Fee` by quantizing an exact signed `Quantity` in the denomination
asset and SHALL conserve the unrounded amount exactly as grid amount plus residual. It SHALL NOT accept an unqualified
scalar as the internal fee basis, infer an asset or grid solely from instrument settlement, evaluate a percentage or
minimum policy, inspect an order or scenario, or consult reference data. Percentage-rate sign convention and
calculation SHALL belong to downstream fee policy, which supplies the resulting exact typed amount to this boundary.

Fee value equality SHALL include every retained denomination component: coherent asset/grid issuer provenance, full
stable grid identity and immutable quantum, and quantization policy, as well as kind and exact amount components.
Irreconcilable retained handles SHALL not compare equal. Equal fee values SHALL have equal hash codes; opaque issuer
lineage MAY refine equality without becoming public construction or hashing authority.

#### Scenario: Calculate a taker charge
- **WHEN** fee policy supplies an exact negative amount through a validated denomination
- **THEN** the resulting fee retains a negative grid amount and the exact signed residual in that asset

#### Scenario: Calculate a maker rebate
- **WHEN** fee policy supplies an exact positive amount through a validated denomination
- **THEN** the resulting fee retains a positive grid amount and the exact signed residual in that asset

#### Scenario: Preserve fee rounding residual
- **WHEN** an exact fee amount is not representable on the denomination grid
- **THEN** the fee exposes quantized amount, residual, and unrounded amount satisfying exact typed conservation

#### Scenario: Distinguish irreconcilable fee denominations
- **WHEN** two otherwise identical fees retain the same stable grid identity under different coherent issuer lineage
  or retain different immutable quanta or quantization policy
- **THEN** they do not compare equal, while repeated values from one complete denomination compare equal with equal
  hash codes

#### Scenario: Charge a third asset
- **WHEN** policy supplies a fee amount in an asset other than base, quote, or settle
- **THEN** the fee retains that asset and its checked stable grid handle rather than being relabeled as settlement

#### Scenario: Keep denomination ownership coherent
- **WHEN** fee construction receives an explicit instrument and denomination carrying different `InstrumentId` values
- **THEN** it returns a typed instrument-mismatch failure before quantization

#### Scenario: Keep fee policy out of the pure artifact
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** it can construct exact fee values but has no fee-rate, schedule-selection, maker/taker, tier, minimum, or
  account-policy API

### Requirement: Exact fee-inclusive PnL breakdown
A `SettledFeeContribution` SHALL retain one original `Fee`, the `InstrumentId` and settlement asset for which it was
converted, and the exact settlement `Quantity` produced by an explicit market state. Pure conversion SHALL use the
fee's original trusted asset and the market state's retained settle-targeted typed conversion. A missing conversion or
runtime instrument mismatch SHALL return a typed failure; conversion SHALL NOT default to identity, parity, a live
catalog, or another market state.

A pure `Pnl` constructor SHALL consume one exact `PricePnl` and an immutable vector of already converted
`SettledFeeContribution` values for the same instrument and settlement asset. It SHALL retain the price component,
original fees with their converted contributions, exact total fee PnL, and exact net PnL, calculated as:

```text
netPnl = pricePnl + sum(settledFeeContributions)
```

The empty vector SHALL contribute exact additive identity. Charges SHALL reduce net PnL and rebates SHALL increase it.
The constructor SHALL compose typed quantities directly and SHALL NOT erase them into raw `Rational` arithmetic.

Scenario and fee-policy layers SHALL be responsible for choosing which market state corresponds to each fee line and
for producing the vector of settled contributions. `Pnl` itself SHALL NOT depend on orders, scenarios, schedules,
liquidity roles, or execution provenance.

`PricePnl`, `SettledFeeContribution`, and `Pnl` value equality SHALL preserve coherent settlement-asset provenance.
Contribution and PnL equality SHALL preserve the complete original `Fee` distinction transitively. Equal values SHALL
have equal hash codes; opaque issuer lineage MAY refine equality without becoming public construction or hashing
authority.

#### Scenario: Compose no fees
- **WHEN** a caller constructs PnL from price PnL and an empty contribution vector
- **THEN** fee PnL is exact zero in settlement and net PnL equals price PnL

#### Scenario: Subtract entry and exit charges
- **WHEN** settled contributions contain negative entry and exit charges
- **THEN** fee PnL is their exact typed sum and net PnL is below price PnL by that amount

#### Scenario: Include a maker rebate
- **WHEN** one settled contribution is positive
- **THEN** the rebate increases fee PnL and net PnL exactly

#### Scenario: Convert fees at their corresponding states
- **WHEN** two fees share an asset but downstream orchestration supplies market states with different retained
  settle-targeted rates
- **THEN** each contribution preserves the exact conversion from its explicitly selected state before PnL summation

#### Scenario: Reject a missing fee conversion
- **WHEN** a fee's original asset has no settle-targeted conversion in the supplied market state
- **THEN** contribution construction fails and preserves the missing asset identity for diagnosis

#### Scenario: Reject foreign valuation inputs
- **WHEN** price PnL or any settled fee contribution carries a different instrument or settlement identity
- **THEN** PnL construction returns a typed mismatch rather than combining the quantities

#### Scenario: Preserve component visibility
- **WHEN** a caller inspects a PnL result
- **THEN** price PnL, original fees, converted contributions, total fee PnL, and net PnL remain separately observable

#### Scenario: Preserve retained contribution distinctions in PnL equality
- **WHEN** two numerically identical contributions or PnL values retain irreconcilable settlement assets or original
  fee denominations
- **THEN** they do not compare equal, while repeated values from the same coherent inputs compare equal with equal hash
  codes

### Requirement: Scenario context carries epistemic status
`Fee`, `SettledFeeContribution`, and `Pnl` SHALL be deterministic exact values for the inputs supplied to their pure
construction boundaries. They SHALL be named without speculative or reported qualifiers. Hypothetical or executed
status SHALL be conveyed by a downstream scenario or execution context rather than encoded into the pure economic
values themselves. Instrument economics SHALL NOT pre-model execution provenance through fills, venue reports, or
ledger entries.

#### Scenario: Re-evaluate identical inputs
- **WHEN** the same denomination, exact amount, market state, price PnL, and settled contributions are evaluated twice
- **THEN** every fee, residual, contribution, fee PnL, and net PnL value is exactly equal

#### Scenario: Change a scenario assumption
- **WHEN** a downstream scenario changes an assumed matched price or liquidity allocation
- **THEN** it may produce different inputs and therefore a different `Fee` or `Pnl` while the immutable instrument
  remains unchanged

#### Scenario: Avoid execution provenance in planning
- **WHEN** a caller calculates scenario PnL before any order is submitted
- **THEN** no execution ID, fill record, venue-reported fee, or ledger entry is required by the economic values

### Requirement: PnL scope is explicit
The pure `Pnl` value SHALL cover exact price PnL and exactly the settled fee contributions supplied to it. It SHALL NOT
silently obtain or include funding, interest, liquidation penalties, margin effects, settlement events, tax, deposits,
withdrawals, or unrelated account cashflows. Future signed adjustments MAY compose through separately specified,
explicit contribution types.

#### Scenario: Exclude funding from trading-fee PnL
- **WHEN** no funding, liquidation, or other separately modeled contribution is supplied
- **THEN** PnL contains price and supplied trading-fee components only

#### Scenario: Preserve component visibility
- **WHEN** a caller inspects the PnL inputs and result
- **THEN** every included contribution is represented explicitly and no application service is consulted
