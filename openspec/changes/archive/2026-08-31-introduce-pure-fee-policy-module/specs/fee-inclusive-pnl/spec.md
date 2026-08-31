## ADDED Requirements

### Requirement: Pure fee-policy and assessment boundary
Contextual fee rules SHALL be delivered by a pure fee-policy artifact depending on instrument economics, the order
model, and execution scenarios. It SHALL NOT be instrument metadata and SHALL NOT be exposed through `Instrument`.
The artifact SHALL contain no catalog lookup, clock, account lookup, policy-loading effect, persistence, network,
stream, transaction, tracing, metrics, or execution-provenance interpreter.

An open `FeePolicy` SHALL be an ordinary deterministic strategy from a complete order scenario to either a domain-owned
non-empty aggregate of the policy's typed failure cause or an immutable vector of fee directives. The typed cause SHALL
be preserved when policy evaluation is composed or wrapped; it SHALL NOT be reduced to an exception or free-form
message. The policy SHALL NOT be parameterized by an effect type. A directive SHALL contain one already calculated core
`Fee` and a requested nonnegative matched-slice index, but SHALL NOT contain a caller-selected market state.

The canonical assessment boundary SHALL consume an explicit instrument, one scenario, and one policy. It SHALL resolve
every directive index against that exact scenario, validate instrument identity and bounds, and produce a
scenario-owned assessment containing the scenario once and zero or more assessed fees. Each assessed fee SHALL retain
the selected immutable slice and its market state by construction. Invalid directives SHALL accumulate in stable
directive order; no later object-reference comparison SHALL be required.

#### Scenario: Extend policy without effects
- **WHEN** downstream code defines venue-, account-, tier-, order-, visibility-, or liquidity-sensitive fee logic
- **THEN** it implements the pure policy strategy and returns typed directives without introducing `F[_]` or an
  interpreter

#### Scenario: Preserve a policy-defined failure
- **WHEN** a custom policy returns its typed failure cause
- **THEN** assessment and fee-inclusive evaluation retain that cause with policy and leg context rather than converting
  it to a string or throwing it

#### Scenario: Assess no fees
- **WHEN** a valid policy emits an empty directive vector
- **THEN** assessment succeeds with the same target scenario and an empty assessed-fee vector

#### Scenario: Select attribution from the actual scenario
- **WHEN** a directive requests matched-slice index `i`
- **THEN** assessment attaches the fee to slice `i` and its market state selected from the supplied scenario

#### Scenario: Reject out-of-range attribution
- **WHEN** one or more directives request indices outside the supplied scenario
- **THEN** assessment reports every invalid directive with its directive ordinal, requested index, and slice count

#### Scenario: Reject foreign fee output
- **WHEN** policy output contains fees or denominations for a different runtime instrument identity
- **THEN** assessment reports typed directive violations instead of constructing scenario fees

#### Scenario: Prevent arbitrary source-market attachment
- **WHEN** a custom policy constructs directives
- **THEN** it has no directive field or supported constructor for supplying a separate source market

### Requirement: Contextual fee-policy composition
A fee policy SHALL retain one stable runtime `InstrumentId` and MAY capture immutable venue, account, tier, effective
version, and other policy inputs. A checked composition boundary SHALL accept zero or more component policies for one
explicit instrument. Zero components SHALL form no-fee identity; multiple components SHALL evaluate independently,
accumulate their policy failures in component order, and concatenate successful directives in component then directive
order.

Composition SHALL be observationally associative and have the no-fee policy as identity within one fixed instrument
context. Because composition is not total across different runtime instrument identities, the artifact SHALL NOT expose
an unconditional global monoid instance that silently combines foreign policies.

#### Scenario: Compose no policies
- **WHEN** a caller composes an empty component vector for one instrument
- **THEN** the resulting policy always emits no directives for a coherent scenario

#### Scenario: Compose several policies stably
- **WHEN** three valid component policies emit zero or more directives
- **THEN** composition preserves component order and each component's directive order

#### Scenario: Accumulate independent component failures
- **WHEN** several component policies independently fail on one scenario
- **THEN** evaluation returns all failures in stable component/error order rather than stopping after the first

#### Scenario: Reject foreign policy composition
- **WHEN** one component carries a different runtime `InstrumentId`
- **THEN** checked composition rejects it before constructing a combined policy

#### Scenario: Satisfy contextual composition laws
- **WHEN** same-instrument policies are regrouped or composed with the no-fee identity
- **THEN** their observable directives and ordered errors are unchanged

## MODIFIED Requirements

### Requirement: Signed grid-constrained fees
A calculated `Fee` SHALL remain a pure instrument-economic value retaining stable `InstrumentId`, semantic kind,
original trusted asset, validated denomination, signed grid amount, exact typed quantization residual, and unrounded
exact typed amount. Fee sign SHALL remain from the account perspective: a charge is negative and a rebate is positive.

The fee-policy artifact SHALL own `FeeRate`, whose quoted convention is positive for a charge and negative for a rebate.
Percentage calculation SHALL accept an existing `NonNegative[Quantity[D]]` basis and return the exact signed typed
contribution `basis × -rate`; it SHALL NOT inspect the basis as a raw scalar to rediscover nonnegativity. Minimum-charge
adjustment SHALL accept an existing nonnegative typed minimum and preserve rebates and sufficiently large charges while
raising a smaller-magnitude charge to the negative minimum. These pure calculations SHALL be total for their refined
inputs.

Policy SHALL pass the resulting exact `Quantity[D]` to the core denomination's fee constructor, which alone applies the
selected grid and quantization policy and preserves exact conservation. Fee policy SHALL NOT infer denomination from
instrument settlement, reconstruct typed quantities from raw arithmetic, or move quantization into schedule logic.

#### Scenario: Calculate a percentage charge
- **WHEN** a positive fee rate is applied to a refined nonnegative typed basis
- **THEN** policy obtains an exact negative typed contribution and core fee construction quantizes it as a charge

#### Scenario: Calculate a percentage rebate
- **WHEN** a negative fee rate is applied to a refined nonnegative typed basis
- **THEN** policy obtains an exact positive typed contribution and core fee construction quantizes it as a rebate

#### Scenario: Reject a negative basis at refinement
- **WHEN** an adapter supplies a negative quantity as a fee basis
- **THEN** it cannot obtain the required nonnegative refined input and percentage calculation is not invoked

#### Scenario: Apply a minimum charge
- **WHEN** an exact negative contribution has magnitude below a refined nonnegative minimum
- **THEN** minimum adjustment returns the exact negative minimum in the same asset dimension

#### Scenario: Do not turn a rebate into a charge
- **WHEN** minimum adjustment receives a zero or positive contribution
- **THEN** it returns that contribution unchanged

#### Scenario: Preserve per-component quantization
- **WHEN** several policy components calculate fees in one denomination
- **THEN** each exact amount crosses the core denomination boundary separately and retains its own grid result and
  residual

#### Scenario: Charge a third asset
- **WHEN** policy uses a denomination in an asset other than base, quote, or settle
- **THEN** the calculated fee retains that asset and grid and later requires an explicit market-state conversion

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

#### Scenario: Keep denomination ownership coherent
- **WHEN** fee construction receives an explicit instrument and denomination carrying different `InstrumentId` values
- **THEN** it returns a typed instrument-mismatch failure before quantization

#### Scenario: Keep fee policy out of the pure artifact
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** it can construct exact fee values but has no fee-rate, schedule-selection, maker/taker, tier, minimum, or
  account-policy API

### Requirement: Exact fee-inclusive PnL breakdown
Fee-inclusive round-trip evaluation SHALL consume one explicit instrument, a complete checked round-trip scenario, and
an explicit entry/exit fee-policy product. The two policies MAY be different so account tier, venue terms, or policy
version can differ between legs; a convenience constructor MAY place the same policy in both fields without erasing the
product.

After one initial runtime-identity stage, evaluation SHALL independently:

- obtain exact `PricePnl` from execution-scenario normalization;
- assess entry policy against the exact entry scenario;
- assess exit policy against the exact exit scenario.

It SHALL convert each assessed fee through the market state retained by its validated matched-slice attribution,
wrapping any conversion failure with entry/exit leg and slice index. Independent policy, directive, and conversion
failures SHALL accumulate in deterministic leg/component/directive order. If every prerequisite succeeds, evaluation
SHALL pass `PricePnl` and the ordered settled contributions to the core `Pnl` constructor.

The successful scenario-level result SHALL retain the round trip, its entry and exit fee assessments, each converted
fee's leg and slice attribution, and the resulting core `Pnl`. This enclosing result preserves scenario provenance and
policy attribution without adding order/scenario types to the core PnL value.

The result SHALL expose exact price PnL, every original fee and settled contribution, exact total fee PnL, and exact net
PnL satisfying:

```text
netPnl = pricePnl + sum(settledFeeContributions)
```

All arithmetic SHALL remain in typed `PositionLots`, `Rate`, `Quantity`, `PricePnl`, and contribution values. Evaluation
SHALL NOT default a missing conversion to identity or parity, choose a different slice/state, consult a live catalog, or
calculate the kernel in raw `Rational` values.

#### Scenario: Use different policies by leg
- **WHEN** entry and exit occur under different immutable fee policies
- **THEN** each scenario is assessed with its explicitly selected policy and both results appear in one PnL breakdown

#### Scenario: Subtract entry and exit charges
- **WHEN** both leg assessments contain negative fees
- **THEN** fee PnL is their exact settlement sum and net PnL is reduced by that amount

#### Scenario: Include a maker rebate
- **WHEN** one assessed fee is positive
- **THEN** its settled contribution increases fee PnL and net PnL exactly

#### Scenario: Convert fees at validated source slices
- **WHEN** two fees share an asset but their attributed slices carry different settle conversions
- **THEN** each fee uses its own selected slice's retained market state before aggregation

#### Scenario: Preserve successful scenario attribution
- **WHEN** fee-inclusive round-trip evaluation succeeds
- **THEN** the enclosing result exposes its round trip, per-leg assessments, leg/slice converted contributions, and core
  PnL without duplicating those concerns inside the core value

#### Scenario: Accumulate missing conversions
- **WHEN** several assessed third-asset fees lack conversions on independent slices or legs
- **THEN** evaluation reports all applicable conversion failures with leg, slice, and asset context

#### Scenario: Reject foreign evaluation inputs once
- **WHEN** the instrument, round trip, or either policy carries a different runtime `InstrumentId`
- **THEN** the initial identity stage reports closed contextual failures and suppresses dependent policy/conversion work

#### Scenario: Preserve an empty fee identity
- **WHEN** both leg policies assess no fees
- **THEN** core PnL contains an empty contribution vector, exact zero fee PnL, and net PnL equal to scenario price PnL

#### Scenario: Preserve deterministic failure ordering
- **WHEN** the same invalid fee-inclusive request is evaluated repeatedly
- **THEN** its non-empty ordered error collection is identical by leg, component, directive, and conversion position

#### Scenario: Compose no fees
- **WHEN** a caller constructs PnL from price PnL and an empty contribution vector
- **THEN** fee PnL is exact zero in settlement and net PnL equals price PnL

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
`Fee`, assessed fee, settled contribution, and `Pnl` SHALL be deterministic exact values for the immutable policy,
scenario, market, conversion, and rounding inputs supplied to their pure boundaries. They SHALL not be named estimated
or reported. Hypothetical status SHALL be conveyed by the enclosing execution scenario; actual execution provenance
belongs to future execution/reporting types.

#### Scenario: Re-evaluate identical inputs
- **WHEN** the same round trip and immutable entry/exit policies are evaluated twice
- **THEN** directives, assessments, fees, residuals, contributions, price PnL, fee PnL, and net PnL are exactly equal

#### Scenario: Change a scenario assumption
- **WHEN** assumed matched price, liquidity allocation, or policy selection changes
- **THEN** calculated fees or PnL may change while immutable instrument and order values remain unchanged

#### Scenario: Avoid execution provenance in planning
- **WHEN** a caller calculates scenario PnL before submission
- **THEN** no execution ID, fill record, venue-reported fee, ledger entry, or effect interpreter is required

### Requirement: PnL scope is explicit
Fee-inclusive scenario evaluation SHALL include exact normalized scenario price PnL and exactly the assessed trading-fee
contributions produced by the supplied leg policies. It SHALL NOT silently include funding, interest, liquidation
penalties, margin effects, settlement events, tax, deposits, withdrawals, or unrelated account cashflows. Future
adjustments SHALL enter through separately specified explicit contribution types or higher orchestration.

#### Scenario: Exclude absent adjustments
- **WHEN** no separately modeled funding, liquidation, or account contribution is supplied
- **THEN** PnL contains normalized scenario price and assessed trading fees only

#### Scenario: Exclude funding from trading-fee PnL
- **WHEN** no funding, liquidation, or other separately modeled contribution is supplied
- **THEN** PnL contains price and supplied trading-fee components only

#### Scenario: Preserve component visibility
- **WHEN** a caller inspects the final core PnL
- **THEN** price PnL, original fees, converted contributions, total fee PnL, and net PnL remain separately observable

## REMOVED Requirements

### Requirement: Contextual instrument-bound fee schedules
**Reason**: `FeeSchedule` combines policy calculation, validated attribution, instrument-owned construction, and
scenario output in one interface and permits arbitrary market attachment that valuation must later distrust.

**Migration**: Replace schedules with pure `FeePolicy` directives, run them through the canonical scenario assessment
boundary, use an explicit entry/exit policy product for round trips, and delegate fee value/contribution/PnL construction
to instrument economics.
