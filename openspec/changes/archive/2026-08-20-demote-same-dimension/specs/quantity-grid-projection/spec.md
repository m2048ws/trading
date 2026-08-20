## ADDED Requirements

### Requirement: Explicit equivalence-aware grid comparison
Exact comparison across grid types SHALL remain available when the operand dimensions have different static spellings
and `SameDimension[D, E]` is supplied intentionally. `exactlyEquals` SHALL compare the coefficients of the two canonical
exact embeddings, and `compareExact` SHALL return their exact coefficient ordering. These comparison operations SHALL
NOT make alignment implicit in additive arithmetic and SHALL remain separate from arithmetic normalization.

#### Scenario: Compare equivalent dimensions across grids
- **WHEN** grid quantities on different grids have distinct but equivalent static dimension types and the caller supplies
  `SameDimension` evidence
- **THEN** `exactlyEquals` and `compareExact` compare their exact embedded coefficients

#### Scenario: Reject comparison without equivalence
- **WHEN** grid quantities have different static dimension types and no `SameDimension` evidence is available
- **THEN** equivalence-aware exact comparison does not compile

## MODIFIED Requirements

### Requirement: Operations that leave a grid
Cross-grid addition and subtraction SHALL accept `GridQuantity[D, G]` and `GridQuantity[D, H]` with one exact shared
Scala dimension type `D`, SHALL require `Normalize[D]`, and SHALL return `Quantity[D]`. They SHALL NOT consume
`SameDimension` to align operands whose static dimension types differ. `GridQuantity.alignTo` SHALL align only the value's
phantom dimension and SHALL NOT manufacture an aligned `GridRef`. Therefore, when different grid witnesses use
equivalent but distinct static dimension types, callers SHALL embed each value through its original grid witness, align
one resulting exact `Quantity` to the selected result dimension, and use exact-type `Quantity` addition or subtraction
instead of `addExact` or `subtractExact`. Grid-by-grid multiplication and mixed grid/exact multiplication SHALL return
unrestricted exact quantities whose static dimensions are the simplified algebraic products of their operand
dimensions. Grid quantity division by `NonZero[Quantity[B]]` SHALL return the corresponding simplified exact quotient.
Rate application and exact whole-scalar division SHALL return unrestricted exact quantities with the mathematically
correct simplified dimensions. A grid divisor SHALL be canonically embedded before applying the generic nonzero check.
Exact operations that leave a grid SHALL use the same static normalization rules as ordinary `Quantity` arithmetic and
SHALL NOT retain a separate grid-specific dimension algebra.

#### Scenario: Add distinct grids
- **WHEN** cent and three-cent values with the exact static dimension type USD are added
- **THEN** the result is `Quantity[USD]` with the exact coefficient and no `SameDimension` evidence is required

#### Scenario: Embed before cross-spelling addition
- **WHEN** values on different grids have equivalent dimensions represented by different Scala dimension types
- **THEN** direct `addExact` and `subtractExact` do not compile, including after aligning only a grid quantity while its
  original grid witness retains the source dimension, and the supported route is to embed through each original grid,
  align one resulting exact quantity, and use exact-type quantity addition or subtraction

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
