# quantity-grid-projection Specification

## Purpose

Defines anonymous mathematical grids, exact projection and quantization, and numerical grid relationships without
stable asset/grid identity, catalog provenance, or encoding ownership.
## Requirements
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

### Requirement: Dimensions do not impose grids
A dimension or asset SHALL NOT inherently select a grid. Multiple contexts MAY define distinct anonymous mathematical
grids for the same dimension, including grids with equal quanta but distinct generative coordinate namespaces. Exact
`Quantity[D]` values SHALL remain valid when they inhabit no grid. Stable asset and grid identities, when required,
SHALL be composed around these mathematical values by the reference-data layer and SHALL NOT alter this behavior.

#### Scenario: Preserve an off-grid exact value
- **WHEN** inverse calculation produces `2/100001 XBT`
- **THEN** it remains a valid `Quantity[XBT]` even though it is not on the satoshi grid

#### Scenario: Distinguish equal-quantum grids
- **WHEN** two anonymous USD grids are constructed with the same quantum
- **THEN** their grid quantities have distinct generative grid types even though `SameQuantum` may relate them

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
Scala dimension type `D` and SHALL return `Quantity[D]` without `SameDimension` or a validity capability. They SHALL NOT
consume `SameDimension` to align operands whose static dimension types differ. `GridQuantity.alignTo` SHALL retag only
the value's phantom dimension and SHALL NOT retag or manufacture the `GridRef` needed to embed it. Therefore, when the
two original grid witnesses use equivalent but distinct static dimension types, callers SHALL embed each value through
its original grid witness, align one resulting exact `Quantity` to the selected result dimension, and then use
homogeneous `Quantity` addition or subtraction. Exact whole-scalar division SHALL preserve `D` without additional
evidence.

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

#### Scenario: Embed before cross-spelling addition
- **WHEN** values on different grids have equivalent dimensions represented by different Scala dimension types
- **THEN** direct `addExact` and `subtractExact` do not compile, including after aligning only a grid quantity while its
  original grid witness retains the source dimension, and the supported route is to embed through each original grid,
  align one resulting exact quantity, and use homogeneous exact-quantity addition or subtraction

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

#### Scenario: Cancel factors after grid multiplication
- **WHEN** a grid quantity in dimension `Position` is multiplied exactly by a quantity in dimension
  `Divide[Settlement, Position]`
- **THEN** ordinary multiplication retains
  `Quantity[Times[Position, Divide[Settlement, Position]]]`; explicit `SameDimension` alignment may select
  `Quantity[Settlement]`

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
`trading.quantity.grid.SameGrid`, `SameQuantum`, and `Embedding` SHALL describe relationships between anonymous
mathematical grid witnesses. Their constructors SHALL remain lexically private, and the
`trading.quantity.grid.GridError` hierarchy SHALL remain closed; declaring the owning package downstream SHALL confer
no evidence or error authority.

`SameGrid` SHALL require the same private generative grid identity and compatible authoritative definition; equal
dimension and quantum alone SHALL produce at most `SameQuantum`. `Embedding` SHALL depend only on equal canonical
dimension and an exact whole-coordinate quantum ratio. No mathematical relationship SHALL create or compare stable
grid IDs, versions, reference-data handles, or issuer provenance.

#### Scenario: Recover the same anonymous grid
- **WHEN** two retained references denote the same generated mathematical grid
- **THEN** checked `SameGrid` recovery permits exact coordinate retyping between those references

#### Scenario: Keep separately generated equal grids distinct
- **WHEN** two anonymous grids were created separately with the same dimension and quantum
- **THEN** `SameGrid` recovery fails while `SameQuantum` recovery may succeed

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

### Requirement: Existing grid values and witness-owned reconstruction are trusted
Every normally returned `GridQuantity[D, G]` SHALL carry a valid dimension index established by an authoritative
`DimRef[D]` or matching anonymous `GridRef[D]` for zero manufacture, by the matching grid witness for nonzero
coordinate construction, or by checked mathematical evidence that owns the selected target type. Possessing the value
SHALL NOT establish that `G` has a public grid witness or stable reference-data identity; exact interpretation,
coordinate inspection, and identity-sensitive boundary operations SHALL continue to require their corresponding
mathematical witness or downstream trusted handle.

Same-grid addition, subtraction, integer scaling, negation, quotient/remainder, and allocation SHALL transform existing
trusted grid values without a dimension capability. Exact narrowing, grid constraint, quantization, and refined
variants SHALL likewise require their existing source values and target mathematical grid witnesses but no separate
authority for the preserved dimension. Checked `SameGrid`, `SameQuantum`, and `Embedding` transitions SHALL rely on
their source and target mathematical witnesses; selecting the checked target dimension or grid type SHALL not require
a separate dimension capability. Explicit `GridQuantity.alignTo` SHALL preserve the coordinate and grid phantom but
SHALL not alter the original grid witness or confer stable identity.

#### Scenario: Move on an abstract grid
- **WHEN** generic code adds, subtracts, scales, or negates existing `GridQuantity[D, G]` values
- **THEN** it preserves `D` and `G` without contextual dimension evidence

#### Scenario: Project through a target witness
- **WHEN** generic code narrows, constrains, or quantizes an existing `Quantity[D]` through `GridRef[D]`
- **THEN** the target witness owns every constructed coordinate and no separate dimension capability is required

#### Scenario: Divide and allocate an existing coordinate
- **WHEN** generic code applies quotient/remainder or even allocation to an existing `GridQuantity[D, G]` with its
  matching grid witness
- **THEN** every result remains on the source grid without separate dimension authority

#### Scenario: Convert through checked grid evidence
- **WHEN** checked same-grid, same-quantum, or embedding evidence selects a target grid whose mathematical witness has
  already been validated
- **THEN** conversion to the target dimension and grid type requires no additional dimension evidence

#### Scenario: Keep grid zero authoritative
- **WHEN** generic code manufactures `GridQuantity.zero[D, G]` without an existing grid value
- **THEN** it must supply `DimRef[D]` or use a zero operation owned by a matching anonymous grid witness

#### Scenario: Do not infer grid provenance from a value
- **WHEN** generic code possesses only `GridQuantity[D, G]`
- **THEN** it cannot recover a `GridRef[D]`, stable grid handle, quantum, reference-data key, or encoding authority from
  the value

#### Scenario: Reject malformed grid reconstruction
- **WHEN** checked mathematical or downstream reference-data reconstruction cannot establish the requested dimension
  and grid relationship
- **THEN** it returns no `GridQuantity` at the requested indices

### Requirement: Mathematical grid witnesses are anonymous
`GridRef[D]` SHALL describe only an authoritative `DimRef[D]`, a positive exact rational quantum, and a generative
coordinate namespace `G`. Creating a uniform mathematical grid SHALL require the dimension witness and quantum and
SHALL NOT accept, synthesize, or expose a stable grid ID, version, catalog key, asset, or issuer provenance.

Each successful grid construction SHALL create a fresh coordinate namespace even when another grid has the same
dimension and quantum. Stable naming of a mathematical grid SHALL be supplied only by a reference-data handle in a
downstream artifact.

Because `PositiveRational` erases to raw `Rational` for JVM callers, uniform-grid construction SHALL expose a typed
checked raw/JVM entry that returns `ExpectedPositive` for zero or negative input. The statically refined `create` entry
SHALL defensively revalidate its erased argument and terminate before returning a grid if a raw JVM caller violates the
refinement. Ordinary expected raw invalidity SHALL use the checked result boundary rather than exceptions as routine
control flow.

#### Scenario: Construct an anonymous grid
- **WHEN** a caller supplies an authoritative USD dimension and quantum `1/100`
- **THEN** grid construction returns a mathematical witness with a fresh coordinate namespace and no stable identity

#### Scenario: Repeat an equal mathematical definition
- **WHEN** a caller constructs two anonymous grids with the same dimension and quantum
- **THEN** the grids have distinct coordinate namespaces until explicit mathematical evidence relates them

#### Scenario: Exclude stable identity from the factory
- **WHEN** downstream source attempts to pass a grid ID or version to mathematical uniform-grid construction
- **THEN** no such quantity-layer parameter or overload exists

#### Scenario: Reject a nonpositive raw grid quantum
- **WHEN** downstream Scala or Java supplies zero or a negative rational to the checked raw uniform-grid boundary
- **THEN** it receives `ExpectedPositive` and no `GridRef` is returned, while an erased call to the refined entry fails
  closed before returning grid authority

### Requirement: Projection diagnostics remain mathematical
Exact narrowing and quantization failures in the quantity artifact SHALL describe the exact source and target quantum
needed to explain mathematical nonrepresentability. They SHALL NOT contain `AssetId`, `GridId`, `GridVersion`,
`GridKey`, catalog provenance, or another stable reference-data identity.

The quantity artifact SHALL NOT encode an exact value as a stable grid key and coordinate. A boundary that needs that
representation SHALL first retain a trusted reference-data grid handle and use the separately specified codec
capability.

#### Scenario: Reject an off-grid exact value
- **WHEN** exact `6000.001 USD` is narrowed to an anonymous cent grid
- **THEN** the failure reports exact mathematical context without claiming a stable grid identity

#### Scenario: Keep stable coordinate encoding downstream
- **WHEN** a caller has only an anonymous grid and an exact quantity
- **THEN** the quantity artifact offers no operation that emits a stable grid key and coordinate record
