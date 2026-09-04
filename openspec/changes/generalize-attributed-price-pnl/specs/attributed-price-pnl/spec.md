## Purpose

Provide one exact, reusable economic calculation for the price PnL of a finite sequence of attributed position changes, whether the resulting exposure is flat or remains open at a supplied mark.

## ADDED Requirements

### Requirement: Finite attributed price calculation

Instrument economics SHALL expose a direct pure calculation over zero or more attributed priced position changes for one instrument. Given valid inputs, the result SHALL contain the exact ending position, exactly one settled contribution for each input change with the same attribution and order, the exact aggregate price PnL, and an endpoint that retains whether the result is flat or marked.

Each settled contribution SHALL equal the negated exact value of its signed position change at its change price. A flat result's price PnL SHALL equal the sum of those contributions; a marked result's price PnL SHALL equal that sum plus the exact value of the ending position at the supplied mark.

#### Scenario: Empty flat calculation

- **WHEN** the calculation receives no changes and a flat endpoint
- **THEN** it returns zero ending position, no settled contributions, exact zero price PnL, and a flat result endpoint

#### Scenario: Three-change marked calculation

- **WHEN** three compatible attributed changes scale into and partially out of one instrument and leave a non-zero ending position with a compatible mark
- **THEN** the calculation returns their exact summed ending position, three correspondingly attributed contributions in input order, and price PnL equal to the exact contribution sum plus ending-position value at the mark

#### Scenario: Multi-change flat calculation

- **WHEN** compatible attributed changes net exactly to zero and the requested endpoint is flat
- **THEN** the calculation returns a flat endpoint and exact price PnL equal to the sum of all settled contributions

### Requirement: Endpoint honesty

The calculation SHALL accept a flat endpoint only when the derived ending position is exactly zero and SHALL require a marked endpoint exactly when the derived ending position is non-zero. A mark SHALL retain and validate the instrument identity, dimensions, and exact grid information needed to value the open position.

#### Scenario: Non-zero exposure declared flat

- **WHEN** valid changes derive a non-zero ending position but the requested endpoint is flat
- **THEN** the calculation returns a typed endpoint mismatch instead of a result

#### Scenario: Zero exposure supplied a mark

- **WHEN** valid changes derive an exactly zero ending position but the requested endpoint is marked
- **THEN** the calculation returns a typed endpoint mismatch instead of discarding or applying the mark

### Requirement: Typed and accumulating validation

Expected input invalidity SHALL be represented in public typed domain results. The calculation SHALL validate instrument identity, quantity and price dimensions, grids, endpoint compatibility, and exact arithmetic without throwing. Independent validation failures SHALL be accumulated deterministically, while checks that depend on a successfully derived ending position SHALL run only from that evidence.

#### Scenario: Multiple independent incompatibilities

- **WHEN** separate changes or endpoint data contain more than one independently detectable identity, dimension, or grid incompatibility
- **THEN** the calculation returns all independently detectable typed failures in deterministic order

#### Scenario: Dependent endpoint validation

- **WHEN** invalid change data prevents a valid ending position from being derived
- **THEN** the calculation reports the change-data failures without inventing an endpoint-consistency result

#### Scenario: Exact arithmetic failure

- **WHEN** an otherwise compatible calculation cannot represent an intermediate or result on its required exact refinement or grid
- **THEN** the calculation returns the owning typed arithmetic or refinement failure without approximation

### Requirement: Round-trip valuation compatibility

Existing round-trip scenario valuation SHALL delegate its price economics to the finite attributed calculation without changing observable results. Single-slice and multi-slice long and short scenarios SHALL retain their exact ending position, contribution ordering and attribution, price PnL, fee contribution, total PnL, and typed failure behavior.

#### Scenario: Existing flat round trip

- **WHEN** any previously valid single-slice or multi-slice long or short round-trip scenario is valued
- **THEN** it produces exactly the same successful valuation as before the shared calculation was introduced

#### Scenario: Existing fee-inclusive round trip

- **WHEN** a previously valid round-trip scenario is valued with settled fees
- **THEN** its price PnL is supplied by the shared calculation and its fees are combined exactly once to produce the same total PnL as before

#### Scenario: Existing invalid round trip

- **WHEN** a round-trip scenario contains an incompatibility previously represented by a typed valuation failure
- **THEN** valuation returns the same public failure semantics after delegation

### Requirement: Exact calculation laws

For compatible inputs, ending position and aggregate price PnL SHALL obey exact algebraic laws: settled contribution is linear in signed position change; the aggregate is invariant under permutation of the same attributed changes and compatible endpoint; and reordering affects only the ordered contribution presentation, never attribution or aggregate economics.

#### Scenario: Permuted compatible changes

- **WHEN** the same compatible attributed changes are supplied in a different order with the same compatible endpoint
- **THEN** ending position and aggregate price PnL are unchanged while each result preserves the order and attribution of its own input

#### Scenario: Linear settled cost

- **WHEN** a compatible signed position change is decomposed into exactly summing changes at the same price
- **THEN** their settled contributions sum exactly to the contribution of the original change
