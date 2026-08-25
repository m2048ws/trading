## MODIFIED Requirements

### Requirement: Downside risk from net PnL
The instrument-owned `sizing` capability SHALL calculate downside risk for a fee-inclusive PnL value in the instrument's settlement dimension as the exact nonnegative quantity:

```text
max(0, -netPnl)
```

The operation SHALL preserve exact rational semantics and SHALL NOT quantize the risk value or convert it to floating point.

#### Scenario: Measure a losing scenario
- **WHEN** net PnL is exact `-17/3` in the settlement asset
- **THEN** `sizing` returns exact nonnegative downside risk `17/3` in that asset

#### Scenario: Clamp profitable risk to zero
- **WHEN** net PnL is zero or positive
- **THEN** downside risk is exact zero

### Requirement: Maximum discrete-lot selection
The instrument-owned `sizing` capability SHALL expose maximum-lot selection that accepts a nonnegative risk budget in the instrument settlement dimension, a positive whole-number lot-count cap, an instrument-bound fee schedule, and a total scenario builder from positive candidate `Lots` to a complete round-trip scenario. It SHALL return the greatest candidate lot count no larger than the cap whose fee-inclusive downside risk does not exceed the budget, or no result when even one lot exceeds the budget.

Candidates SHALL be constructed through the exact instrument's `Lots` constructor and evaluated using the same `valuation` and `fees` path exposed to ordinary callers. Sizing SHALL NOT manufacture fractional lots, mix instrument paths, compare incompatible settlement dimensions, or omit entry or exit fees.

#### Scenario: Select the greatest affordable lot count
- **WHEN** several positive candidates up to the cap satisfy the exact risk budget
- **THEN** `sizing` returns the satisfying candidate with the greatest lot coordinate

#### Scenario: Reject every positive candidate
- **WHEN** the complete one-lot scenario already exceeds the risk budget
- **THEN** sizing returns no lot value rather than zero or a fractional lot

#### Scenario: Respect the cap
- **WHEN** every evaluated candidate through the cap satisfies the budget
- **THEN** sizing returns exactly the capped positive lot count

#### Scenario: Include both order legs
- **WHEN** entry and adverse-exit scenarios both produce trading fees
- **THEN** sizing measures risk from price PnL plus both exact fee contributions
