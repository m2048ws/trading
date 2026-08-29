## MODIFIED Requirements

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
