# position-risk-sizing Specification

## Purpose
Defines exact downside-risk measurement and deterministic maximum-lot selection by evaluating complete fee-inclusive trade scenarios on an instrument's discrete lot grid.
## Requirements
### Requirement: Downside risk from net PnL
A downstream pure risk operation SHALL consume an explicit instrument and fee-inclusive `Pnl` and calculate downside
risk in that instrument's settlement dimension as the exact nonnegative quantity:

```text
max(0, -netPnl)
```

The operation SHALL preserve exact rational semantics, SHALL NOT quantize or convert to floating point, and SHALL
reject a PnL value carrying a different runtime `InstrumentId`. `Instrument` SHALL NOT expose this operation through an
owned `sizing` capability, and the instrument-economics artifact SHALL NOT contain the risk operation.

#### Scenario: Measure a losing scenario
- **WHEN** net PnL is exact `-17/3` in the supplied instrument's settlement asset
- **THEN** downside risk is exact nonnegative `17/3` in that asset

#### Scenario: Clamp profitable risk to zero
- **WHEN** net PnL is zero or positive
- **THEN** downside risk is exact zero

#### Scenario: Reject foreign PnL
- **WHEN** the supplied instrument and PnL carry different runtime `InstrumentId` values
- **THEN** risk evaluation returns a typed instrument-mismatch failure

#### Scenario: Keep risk downstream
- **WHEN** Scala depends only on instrument economics
- **THEN** downside-risk and sizing operations are absent from that artifact

### Requirement: Maximum discrete-lot selection
A downstream risk-sizing boundary SHALL consume an explicit instrument, a nonnegative risk budget in that instrument's
settlement dimension, a positive whole-number lot-count cap, an instrument-bound fee policy, and a total scenario
builder from positive candidate `Lots` to a complete round-trip scenario. It SHALL return the greatest candidate lot
count no larger than the cap whose fee-inclusive downside risk does not exceed the budget, or no result when even one
lot exceeds the budget.

Candidates SHALL be constructed through the pure `Lots` boundary for the supplied instrument and evaluated through the
same downstream scenario, fee-policy, and pure PnL operations available to ordinary callers. Sizing SHALL NOT
manufacture fractional lots, mix instrument paths, compare incompatible settlement dimensions, or omit entry or exit
fees. The instrument and pure economics module SHALL NOT own the search policy.

#### Scenario: Select the greatest affordable lot count
- **WHEN** several positive candidates up to the cap satisfy the exact risk budget
- **THEN** sizing returns the satisfying candidate with the greatest lot coordinate

#### Scenario: Reject every positive candidate
- **WHEN** the complete one-lot scenario already exceeds the risk budget
- **THEN** sizing returns no lot value rather than zero or a fractional lot

#### Scenario: Respect the cap
- **WHEN** every evaluated candidate through the cap satisfies the budget
- **THEN** sizing returns exactly the capped positive lot count

#### Scenario: Include both order legs
- **WHEN** entry and adverse-exit scenarios both produce trading fees
- **THEN** sizing measures risk from exact price PnL plus both settled fee contributions

#### Scenario: Reject cross-layer identity mismatch
- **WHEN** the instrument, fee policy, or scenario builder produces values carrying different runtime instrument
  identities
- **THEN** sizing returns the corresponding typed mismatch instead of evaluating a candidate

### Requirement: Nonlinear fee and rounding correctness
The baseline sizing behavior SHALL evaluate discrete candidate scenarios and SHALL NOT assume that risk or fees are linear per lot. Percentage tiers, minimum fees, maker rebates, per-component quantization, and grid rounding MAY create stepped or non-monotone results. An optimized search SHALL be observationally equivalent to complete candidate evaluation and SHALL require an explicit proven or checked monotonicity condition before discarding candidates based on ordering.

#### Scenario: Cross a minimum-fee threshold
- **WHEN** a minimum fee makes one-lot risk differ from a linear multiple of larger-lot risk
- **THEN** sizing evaluates the actual fee result for each relevant candidate and returns the true greatest satisfying lot count

#### Scenario: Preserve per-candidate fee quantization
- **WHEN** adjacent lot counts quantize to different fee-grid coordinates
- **THEN** each candidate's risk uses its own quantized fee rather than a scaled one-lot fee

#### Scenario: Avoid an unjustified binary search
- **WHEN** no monotonicity capability is supplied for the scenario builder and fee schedule
- **THEN** sizing does not skip candidate lots based on a monotonicity assumption

### Requirement: Sizing failures are explicit
If construction or evaluation of a required candidate fails because of an invalid scenario, missing conversion, fee-policy failure, or another typed economics error, sizing SHALL return that failure rather than treating the candidate as unaffordable, skipping it, or returning a smaller result. Candidate evaluation order SHALL be deterministic and SHALL not change the selected result.

#### Scenario: Propagate a missing conversion
- **WHEN** a candidate produces a fee asset absent from its market state's settlement conversions
- **THEN** sizing fails with the missing-conversion error instead of silently reducing the candidate size

#### Scenario: Propagate an invalid adverse exit
- **WHEN** the scenario builder produces an exit that does not close the candidate entry position
- **THEN** sizing fails with the round-trip validation error

#### Scenario: Reject a scenario for a different candidate count
- **WHEN** the builder is invoked for candidate `n` but returns a round trip whose absolute held-position coordinate is `m != n`
- **THEN** sizing stops at the first mismatch with a typed error preserving `n` and the signed observed held-position coordinate rather than evaluating that scenario

#### Scenario: Return the same maximum under equivalent traversal
- **WHEN** two implementations evaluate all required candidates in different deterministic orders
- **THEN** they return the same greatest satisfying lot count or the same underlying evaluation failure policy

### Requirement: Position-sizing scope is bounded
This capability SHALL size against the supplied complete scenario's exact price and trading-fee downside only. It SHALL NOT imply fill probability, guarantee execution of maker-only or limit orders, calculate margin or liquidation thresholds, forecast funding, or optimize across portfolios. Such concerns require separate explicit inputs and future capabilities.

#### Scenario: Treat maker-only PnL as conditional
- **WHEN** a scenario assumes a completed maker-only entry
- **THEN** sizing calculates the conditional fee-inclusive risk of that completed scenario without asserting that the order will fill

#### Scenario: Exclude liquidation mechanics
- **WHEN** no liquidation or margin capability is supplied
- **THEN** the sizing result expresses only the configured PnL-based risk budget
