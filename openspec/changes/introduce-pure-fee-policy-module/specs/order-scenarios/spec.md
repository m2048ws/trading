## ADDED Requirements

### Requirement: Exact round-trip scenario price normalization
The execution-scenario artifact SHALL expose pure price normalization for a complete round-trip scenario and explicit
instrument. For every matched slice, it SHALL derive the slice's signed position change from the order intent's side and
that slice's positive lots through the typed position boundary, value that change at the slice's own market state, and
negate the value as the exact trade cashflow. The exact sum of entry and exit slice cashflows SHALL be returned as one
`PricePnl` in the instrument settlement dimension.

Normalization SHALL preserve typed `PositionLots`, `Rate`, and `Quantity` arithmetic; retain ordinary runtime
instrument coherence; weight every slice by its exact lots; and perform no fee evaluation, quantization, live lookup, or
raw-scalar reconstruction. The execution scenario SHALL own a closed entry/exit leg value for downstream attribution.

#### Scenario: Normalize one slice per leg
- **WHEN** a round trip contains one entry slice and one exit slice
- **THEN** scenario price PnL exactly equals universal exit position value minus entry position value for the held
  position

#### Scenario: Weight several matched prices
- **WHEN** one leg contains slices with different lots and market states
- **THEN** price PnL is the exact sum of each slice's typed signed cashflow rather than an unweighted average price

#### Scenario: Normalize a short round trip
- **WHEN** a sell entry and buy exit close exactly to flat
- **THEN** each slice uses the side-directed signed position change and the resulting PnL is symmetric with the
  corresponding long scenario

#### Scenario: Preserve off-grid exactness
- **WHEN** inverse or third-asset settlement yields a result outside a storage grid
- **THEN** scenario price PnL retains the exact settlement quantity without quantization

#### Scenario: Reject a foreign round trip
- **WHEN** the explicit instrument and round-trip scenario carry different runtime instrument identities
- **THEN** normalization returns a typed scenario-valuation mismatch without performing fee policy

#### Scenario: Keep fee policy out of scenario normalization
- **WHEN** downstream Scala depends on execution scenarios without fee policy
- **THEN** it can calculate exact round-trip `PricePnl` but cannot assess trading fees or construct fee-inclusive PnL
