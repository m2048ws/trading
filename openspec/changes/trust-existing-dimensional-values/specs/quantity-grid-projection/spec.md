## ADDED Requirements

### Requirement: Existing grid values and witness-owned reconstruction are trusted
Every normally returned `GridQuantity[D, G]` SHALL carry a valid dimension index established by `Normalize[D]` for
polymorphic zero, by the matching `GridRef[D]` for nonzero coordinate construction, or by checked grid or registry
evidence that owns the selected target type. Possessing the value SHALL NOT establish that `G` has a public grid witness
or registered identity; exact interpretation, coordinate inspection, packing, and provenance-sensitive operations SHALL
continue to require their matching grid or registry witness.

Same-grid addition, subtraction, integer scaling, negation, quotient/remainder, and allocation SHALL transform existing
trusted grid values without `Normalize[D]`. Exact narrowing, grid constraint, quantization, constrained encoding, and
refined variants SHALL likewise require their existing source values and target grid witnesses but no separate
normalization of the preserved dimension. Checked `SameGrid`, `SameQuantum`, and `Embedding` transitions SHALL rely on
their source and target grid witnesses; selecting the checked target dimension or grid type SHALL not require
`Normalize` for either operand index.

#### Scenario: Move on an abstract grid
- **WHEN** generic code adds, subtracts, scales, or negates existing `GridQuantity[D, G]` values
- **THEN** it preserves `D` and `G` without contextual dimension evidence

#### Scenario: Project through a target witness
- **WHEN** generic code narrows, constrains, quantizes, or encodes an existing `Quantity[D]` through `GridRef[D]`
- **THEN** the target witness owns every constructed coordinate and no `Normalize[D]` is required

#### Scenario: Divide and allocate an existing coordinate
- **WHEN** generic code applies quotient/remainder or even allocation to an existing `GridQuantity[D, G]` with its
  matching grid witness
- **THEN** every result remains on the source grid without `Normalize[D]`

#### Scenario: Convert through checked grid evidence
- **WHEN** checked same-grid, same-quantum, or embedding evidence selects a target grid whose witness has already been
  validated
- **THEN** conversion to the target dimension and grid type requires no additional normalization evidence

#### Scenario: Keep grid zero authoritative
- **WHEN** generic code manufactures `GridQuantity.zero[D, G]` without an existing coordinate or matching grid witness
- **THEN** it must provide `Normalize[D]`

#### Scenario: Do not infer grid provenance from a value
- **WHEN** code possesses `GridQuantity[D, G]`, including a polymorphic zero
- **THEN** it cannot recover a `GridRef[D, G]`, registered witness, quantum, grid key, or packing authority from the value

## MODIFIED Requirements

### Requirement: Operations that leave a grid
Cross-grid addition and subtraction SHALL accept `GridQuantity[D, G]` and `GridQuantity[D, H]` with one exact shared
Scala dimension type `D`, SHALL return `Quantity[D]`, and SHALL require neither `Normalize[D]` nor `SameDimension`.
Operands whose static dimension types differ SHALL first be explicitly aligned. Exact whole-scalar division SHALL
preserve `D` without normalization. Grid-by-grid multiplication and mixed grid/exact multiplication SHALL return
unrestricted exact quantities whose static dimensions are the simplified algebraic products of their operand
dimensions. Grid quantity division by `NonZero[Quantity[B]]` SHALL return the corresponding simplified exact quotient.
Rate application SHALL return an unrestricted exact quantity with the mathematically correct simplified dimension. A
grid divisor SHALL be canonically embedded before applying the generic nonzero check. Operations that compute a new
dimension SHALL use the same complete-expression normalization as ordinary `Quantity` arithmetic and SHALL NOT retain a
separate grid-specific dimension algebra.

#### Scenario: Add distinct grids
- **WHEN** cent and three-cent values with the exact static dimension type USD are added
- **THEN** the result is `Quantity[USD]` with the exact coefficient and no contextual dimension evidence

#### Scenario: Align before cross-grid addition
- **WHEN** values on different grids have equivalent dimensions represented by different Scala dimension types
- **THEN** direct cross-grid addition and subtraction do not compile until one grid quantity is explicitly aligned to the
  selected result dimension

#### Scenario: Exact-divide a grid value by a whole scalar
- **WHEN** an existing grid quantity is canonically embedded and exact-divided by a nonzero whole scalar
- **THEN** the result preserves `D` without `Normalize[D]`

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
