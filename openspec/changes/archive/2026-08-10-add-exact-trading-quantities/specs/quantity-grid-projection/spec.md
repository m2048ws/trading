## ADDED Requirements

### Requirement: Grid quantity semantics
Core `GridQuantity`, `GridRef`, and `UniformGrid` vocabulary SHALL belong to `trading.quantity`; grid relationships,
projection, quantization, constrained encoding, and quotient/remainder and allocation extensions SHALL belong to
`trading.quantity.grid`. `GridQuantity[D, G]` SHALL be an opaque integer-coordinate value proving membership in grid
`G` in dimension `D`. A
grid SHALL be zero-anchored with arbitrary positive rational quantum `q`, and a grid quantity's exact value SHALL be
`coordinate × q`. A grid quantum SHALL have type `Positive[Rational]`, exposed by the semantic alias
`PositiveRational`; it SHALL NOT introduce a distinct numeric representation or floating constructor. Grid quantity
backing SHALL remain `BigInt`, not a stored rational coefficient. Raw coordinate attachment and inspection SHALL be
lexically private within the opaque owner. Public nonzero construction and inspection SHALL require the matching grid
witness through `grid.fromCoordinate(coordinate)` and `grid.coordinate(value)`; no package-qualified or static raw
helper SHALL provide equivalent authority to same-package downstream source. Polymorphic grid zero MAY remain
available because it attaches no caller-supplied nonzero coordinate.

#### Scenario: Interpret a coordinate
- **WHEN** coordinate `1000` inhabits a USD grid with quantum `1/100`
- **THEN** it represents exact `10 USD`

#### Scenario: Support a non-reciprocal quantum
- **WHEN** a grid has quantum `3/100`
- **THEN** coordinate `2` represents `0.06`, while `0.01` is not on that grid

#### Scenario: Reject an invalid quantum
- **WHEN** a grid definition supplies zero or a negative quantum
- **THEN** grid construction fails

#### Scenario: Reject same-package raw coordinate attachment
- **WHEN** downstream Scala declares `package trading.quantity` and supplies an arbitrary coordinate while choosing `D`
  and `G` without a matching grid witness
- **THEN** the code does not compile

### Requirement: Dimensions do not impose grids
A dimension or asset SHALL NOT inherently select a grid. Multiple contexts MAY define distinct grids for the same
dimension, including grids with equal quanta but distinct identity. Exact `Quantity[D]` values SHALL remain valid when
they inhabit no registered grid.

#### Scenario: Preserve an off-grid exact value
- **WHEN** inverse calculation produces `2/100001 XBT`
- **THEN** it remains a valid `Quantity[XBT]` even though it is not on the satoshi grid

#### Scenario: Distinguish equal-quantum grids
- **WHEN** two USD grids have the same quantum but different IDs
- **THEN** their grid quantities have distinct grid types and identities

### Requirement: Canonical exact embedding
Every `GridQuantity[D, G]` SHALL have one exact embedding into `Quantity[D]` using an explicit matching grid witness.
The public operation SHALL be named `asQuantity`. The embedding SHALL preserve zero, addition, negation, and integer
scaling, and SHALL be injective. There SHALL be no implicit global grid-to-quantity conversion.

#### Scenario: Embed a satoshi quantity
- **WHEN** coordinate `10,000,000` on a satoshi grid is embedded
- **THEN** the result is `Quantity[BTC]` with coefficient `1/10`

#### Scenario: Require the witness explicitly
- **WHEN** supported Scala uses a grid quantity where an exact quantity is expected without calling the embedding
- **THEN** the code does not compile

### Requirement: Same-grid closure
Same-grid addition and subtraction SHALL return `GridQuantity[D, G]`; integer scaling, negation, zero, ordering, and
coordinate equality SHALL remain coordinate operations on that grid. The optional algebra layer SHALL expose one
production `LeftModule[GridQuantity[D, G], BigInt]`, using the standard exact `Ring[BigInt]`; that strongest instance
SHALL also supply the grid quantity's additive commutative group without a competing group instance. Exact total order
SHALL delegate to primitive coordinate comparison. `Ring[GridQuantity[D, G]]` SHALL NOT be exposed.

#### Scenario: Add same-grid quantities
- **WHEN** two values on the same cent grid are added
- **THEN** the result remains on that grid with the exact coordinate sum

#### Scenario: Scale by an integer
- **WHEN** a grid quantity is multiplied by a `BigInt`
- **THEN** its coordinate is multiplied exactly and its grid type is unchanged

### Requirement: Operations that leave a grid
Cross-grid addition and subtraction SHALL return `Quantity[D]`. Grid-by-grid multiplication SHALL return
`Quantity[Times[A, B]]`; mixed grid/exact multiplication SHALL return the corresponding exact product. Grid quantity
division by `NonZero[Quantity[B]]`, rate application, and exact whole-scalar division SHALL return unrestricted exact
quantities with the mathematically correct dimensions. A grid divisor SHALL be canonically embedded before applying
the generic nonzero check.

#### Scenario: Add distinct grids
- **WHEN** cent and three-cent values in the USD dimension are added
- **THEN** the result is `Quantity[USD]` with the exact coefficient

#### Scenario: Multiply grid values
- **WHEN** grid values in dimensions `A` and `B` are multiplied with their grid witnesses
- **THEN** the result is `Quantity[Times[A, B]]`

#### Scenario: Apply a rate to a grid quantity
- **WHEN** a source grid quantity is acted on by `Rate[From, To]` with its source grid witness
- **THEN** the result is exact `Quantity[To]`

### Requirement: Exact narrowing is checked
`Quantity[D].narrowExactlyTo(target)` SHALL return a target `GridQuantity` only when the exact coefficient is an integer
multiple of the target quantum. Failure SHALL return `NotOnGrid[D]` and MUST NOT round. Grid-to-grid narrowing SHALL
first use the source grid's canonical exact embedding and then reuse the same operation. Exact narrowing SHALL be the
partial inverse of canonical embedding: narrowing every embedded grid value SHALL recover that value, and every
successful narrowing SHALL embed back to the unchanged source.

#### Scenario: Narrow exactly
- **WHEN** exact `0.06 USD` is narrowed to a three-cent grid
- **THEN** narrowing succeeds with coordinate `2`

#### Scenario: Reject off-grid narrowing
- **WHEN** exact `6000.001 USD` is narrowed to cents
- **THEN** narrowing fails without changing the source

### Requirement: Quantization is explicit and residual-bearing
`Quantity[D].quantizeTo(target, policy)` SHALL return `Quantization[D, target.G]` containing a target grid value and
exact `Quantity[D]` residual. Every result SHALL satisfy
`source = result.value.asQuantity(target) + result.residual`. Grid-to-grid quantization SHALL first use canonical exact
embedding and reuse general quantization. Quantization SHALL be an idempotent retraction onto the target grid: embedded
target values SHALL be fixed points with zero residual, and re-quantizing an embedded selected result SHALL preserve
the selected coordinate.

#### Scenario: Quantize sub-cent USD
- **WHEN** exact `6000.001 USD` is quantized to cents with half-even
- **THEN** the selected value is `6000.00 USD`, residual is `0.001 USD`, and conservation holds

#### Scenario: Quantize off-satoshi XBT
- **WHEN** exact `2/100001 XBT` is quantized to satoshis with an explicit policy
- **THEN** the selected grid value and exact residual reconstruct the source

### Requirement: Quantization policy laws
Floor, ceiling, toward-zero, away-from-zero, and deterministic nearest policies SHALL preserve their documented
negative-value tie behavior and residual bounds. For positive quantum `q`, floor residual SHALL be in `[0,q)`, ceiling
in `(-q,0]`, and nearest magnitude at most `q/2`. Floor and ceiling SHALL be monotone and idempotent; floor selection
SHALL not exceed the source and ceiling selection SHALL not be below it. Nearest selection SHALL choose one of the two
coordinate neighbors, including at negative ties.

#### Scenario: Resolve a negative tie
- **WHEN** a negative half-coordinate is quantized with a named half policy
- **THEN** the policy's documented tie direction is applied deterministically

#### Scenario: Quantize on a `0.03` grid
- **WHEN** a value is projected to a grid with quantum `0.03`
- **THEN** coordinate selection and residual bounds use that exact rational quantum

### Requirement: Grid division and allocation conserve coordinates
For positive whole divisor `d`, `value.quotRemBy(d, grid)` SHALL use the matching grid witness and return source-grid
values satisfying
`sourceCoordinate = quotientCoordinate × d + remainderCoordinate`, with
`0 <= remainderCoordinate < d` even for negative sources. `value.allocateEvenly(count, order, grid)` SHALL use the
matching grid witness and return exactly `count` source-grid values whose coordinates sum to the source and whose
maximum and minimum differ by at most one. Both extensions SHALL require `trading.quantity.grid` to be imported; core
grid arithmetic and embedding SHALL remain available from `trading.quantity` alone. Every supported remainder order
SHALL preserve these laws.

#### Scenario: Divide a negative coordinate
- **WHEN** a negative source coordinate is quotient/remainder divided by a positive whole
- **THEN** the Euclidean remainder remains nonnegative and the quotient and remainder reconstruct the source

#### Scenario: Allocate in either remainder order
- **WHEN** a grid coordinate is allocated with either supported `RemainderOrder`
- **THEN** all parts remain on-grid, conserve the source coordinate, and differ by at most one quantum

### Requirement: Grid relationships do not confer registered identity
`trading.quantity.grid.SameGrid`, `SameQuantum`, and `Embedding` SHALL distinguish full equality, numerical
compatibility, and global lattice embedding. Their constructors SHALL remain lexically private, and the
`trading.quantity.grid.GridError` hierarchy SHALL remain closed; declaring the owning package downstream SHALL confer
no evidence or error authority. A generative or numerically derived grid SHALL NOT gain registered identity without
registry construction.

#### Scenario: Embed three-cent values into cents
- **WHEN** source quantum divided by target quantum is whole
- **THEN** checked embedding evidence widens coordinates exactly

#### Scenario: Reject reverse global embedding
- **WHEN** cent values are globally embedded into a three-cent grid
- **THEN** evidence recovery fails because not every cent coordinate is representable

#### Scenario: Reject same-package grid authority
- **WHEN** downstream Scala declares `package trading.quantity.grid` and attempts to invoke a `SameGrid`, `SameQuantum`,
  or `Embedding` constructor or extend `GridError`
- **THEN** lexical constructors and the closed error hierarchy prevent counterfeit authority
