## MODIFIED Requirements

### Requirement: Grid quantity semantics
`GridQuantity[D, G]` SHALL be an opaque integer-coordinate value proving membership in grid `G` in dimension `D`. A
grid SHALL be zero-anchored with arbitrary positive rational quantum `q`, and a grid quantity's exact value SHALL be
`coordinate × q`. A grid quantum SHALL have type `Positive[Rational]`, exposed by the semantic alias
`PositiveRational`; it SHALL NOT introduce a distinct numeric representation or floating constructor. Grid quantity
backing SHALL remain `BigInt`, not a stored rational coefficient.

Raw coordinate attachment and inspection SHALL be lexically private within the opaque owner. Public nonzero
construction and inspection SHALL require the matching grid witness through `grid.fromCoordinate(coordinate)` and
`grid.coordinate(value)`; no package-qualified or static raw helper SHALL provide equivalent authority to same-package
downstream source. A public zero that selects `D` and `G` without an existing grid value SHALL require an authoritative
`DimRef[D]` or a matching `GridRef[D]`; the type argument alone SHALL NOT manufacture a dimensional grid carrier.
Possessing zero SHALL NOT imply that a public grid witness for `G` exists.

#### Scenario: Interpret a coordinate
- **WHEN** coordinate `1000` inhabits a USD grid with quantum `1/100`
- **THEN** it represents exact `10 USD`

#### Scenario: Support a non-reciprocal quantum
- **WHEN** a grid has quantum `3/100`
- **THEN** coordinate `2` represents `0.06`, while `0.01` is not on that grid

#### Scenario: Construct zero with dimension authority
- **WHEN** generic code manufactures `GridQuantity.zero[D, G]` without an existing coordinate
- **THEN** it must supply `DimRef[D]` or use an operation owned by a matching grid witness

#### Scenario: Keep zero independent of grid provenance
- **WHEN** a zero is constructed with `DimRef[D]` for an abstract `G`
- **THEN** the zero carries no recoverable `GridRef[D]`, quantum, grid key, or registered provenance

#### Scenario: Reject an invalid quantum
- **WHEN** a grid definition supplies zero or a negative quantum
- **THEN** grid construction fails

#### Scenario: Reject same-package raw coordinate attachment
- **WHEN** downstream Scala declares `package trading.quantity` and supplies an arbitrary coordinate while choosing `D`
  and `G` without a matching grid witness
- **THEN** the code does not compile

### Requirement: Same-grid closure
Same-grid addition and subtraction SHALL return `GridQuantity[D, G]`; integer scaling, negation, ordering, coordinate
equality, quotient/remainder, and allocation SHALL remain coordinate operations on that grid. Operations that transform
one or more existing `GridQuantity[D, G]` values while preserving both indices SHALL require no dimension-validity or
equivalence capability.

The optional algebra layer SHALL expose one production `LeftModule[GridQuantity[D, G], BigInt]`, using the standard
exact `Ring[BigInt]`; that strongest instance SHALL also supply the grid quantity's additive commutative group without a
competing group instance. Because the module manufactures zero, obtaining it SHALL require `DimRef[D]` or a stronger
matching grid witness. Exact total order SHALL delegate to primitive coordinate comparison. `Ring[GridQuantity[D, G]]`
SHALL NOT be exposed.

#### Scenario: Add same-grid quantities
- **WHEN** two existing values on the same cent grid are added
- **THEN** the result remains on that grid with the exact coordinate sum and no dimension capability

#### Scenario: Scale by an integer
- **WHEN** an existing grid quantity is multiplied by a `BigInt`
- **THEN** its coordinate is multiplied exactly and its dimension and grid types are unchanged

#### Scenario: Allocate an existing coordinate
- **WHEN** an existing grid coordinate is quotient-divided or evenly allocated with its matching grid witness
- **THEN** all returned values remain on the source grid without separate static validation

#### Scenario: Obtain the grid module
- **WHEN** generic code requests the identity-bearing grid module for `GridQuantity[D, G]`
- **THEN** it must provide `DimRef[D]` or the documented stronger matching grid witness

### Requirement: Operations that leave a grid
Cross-grid addition and subtraction SHALL accept `GridQuantity[D, G]` and `GridQuantity[D, H]` with one exact shared
Scala dimension type `D` and SHALL return `Quantity[D]` without `SameDimension` or a validity capability. Operands with
different static dimension spellings SHALL first be explicitly aligned. Exact whole-scalar division SHALL preserve `D`
without additional evidence.

Grid-by-grid and mixed grid/exact multiplication SHALL return unrestricted exact quantities with expression-preserving
dimensions. Operands in `A` and `B` SHALL produce `Quantity[Times[A, B]]`. Grid quantity division by
`NonZero[Quantity[B]]` SHALL return `Quantity[Divide[A, B]]`. A grid divisor SHALL be canonically embedded before the
generic nonzero check. These generic operations SHALL use ordinary `Quantity` expression algebra and SHALL NOT retain a
separate grid-specific normalization or associated-output capability.

Semantic endpoint operations SHALL remain direct. Applying `Rate[From, To]` to a grid quantity in `From` SHALL return
`Quantity[To]`, and a same-dimension `ratioTo` calculation SHALL return `Ratio`. Their coefficients SHALL agree exactly
with canonical embedding followed by the corresponding exact endpoint operation. Ordinary multiplication or division
of the same embedded operands SHALL retain its raw `Times` or `Divide` type until explicitly aligned.

#### Scenario: Add distinct grids
- **WHEN** cent and three-cent values with the exact static dimension type USD are added
- **THEN** the result is `Quantity[USD]` with the exact coefficient and no contextual dimension evidence

#### Scenario: Align before cross-grid addition
- **WHEN** values on different grids have equivalent dimensions represented by different Scala dimension types
- **THEN** direct cross-grid addition and subtraction do not compile until one grid quantity is explicitly aligned to
  the selected result dimension

#### Scenario: Exact-divide a grid value by a whole scalar
- **WHEN** an existing grid quantity is canonically embedded and exact-divided by a nonzero whole scalar
- **THEN** the result preserves `D` without a dimension capability

#### Scenario: Multiply grid values
- **WHEN** grid values in dimensions `A` and `B` are multiplied with their grid witnesses
- **THEN** the result is exact `Quantity[Times[A, B]]`

#### Scenario: Preserve a mixed product
- **WHEN** a grid value in `Position` is multiplied by an exact quantity in `Divide[Settlement, Position]`
- **THEN** ordinary multiplication returns
  `Quantity[Times[Position, Divide[Settlement, Position]]]` and explicit alignment may select `Quantity[Settlement]`

#### Scenario: Preserve a grid quotient
- **WHEN** a grid value in `A` is divided by checked nonzero `Quantity[B]`
- **THEN** the result is exact `Quantity[Divide[A, B]]`

#### Scenario: Apply a rate to a grid quantity
- **WHEN** a source grid quantity is acted on by `Rate[From, To]` with its source grid witness
- **THEN** the endpoint operation returns exact `Quantity[To]` without public normalization or an alignment repair

#### Scenario: Calculate a grid ratio
- **WHEN** an embedded grid quantity in `D` is compared by `ratioTo` with checked nonzero `Quantity[D]`
- **THEN** the endpoint operation returns exact `Ratio`
