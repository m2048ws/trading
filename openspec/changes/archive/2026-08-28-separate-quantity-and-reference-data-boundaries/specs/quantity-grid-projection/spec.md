## ADDED Requirements

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

## MODIFIED Requirements

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
