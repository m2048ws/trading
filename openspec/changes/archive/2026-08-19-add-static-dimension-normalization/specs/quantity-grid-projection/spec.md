## MODIFIED Requirements

### Requirement: Operations that leave a grid
Cross-grid addition and subtraction SHALL return `Quantity[D]`. Grid-by-grid multiplication and mixed grid/exact
multiplication SHALL return unrestricted exact quantities whose static dimensions are the simplified algebraic products
of their operand dimensions. Grid quantity division by `NonZero[Quantity[B]]` SHALL return the corresponding simplified
exact quotient. Rate application and exact whole-scalar division SHALL return unrestricted exact quantities with the
mathematically correct simplified dimensions. A grid divisor SHALL be canonically embedded before applying the generic
nonzero check. Exact operations that leave a grid SHALL use the same static normalization and `SameDimension` evidence as
ordinary `Quantity` arithmetic and SHALL NOT retain a separate grid-specific dimension algebra.

#### Scenario: Add distinct grids
- **WHEN** cent and three-cent values in the USD dimension are added
- **THEN** the result is `Quantity[USD]` with the exact coefficient

#### Scenario: Multiply grid values
- **WHEN** grid values in dimensions `A` and `B` are multiplied with their grid witnesses
- **THEN** the result is an exact quantity in the simplified product dimension of `A` and `B`

#### Scenario: Cancel factors after grid multiplication
- **WHEN** a grid quantity in dimension `Position` is multiplied exactly by a quantity in dimension
  `Settlement / Position`
- **THEN** the result simplifies to `Quantity[Settlement]`

#### Scenario: Apply a rate to a grid quantity
- **WHEN** a source grid quantity is acted on by `Rate[From, To]` with its source grid witness
- **THEN** the result is exact `Quantity[To]` and agrees with canonical embedding followed by generalized exact
  multiplication
