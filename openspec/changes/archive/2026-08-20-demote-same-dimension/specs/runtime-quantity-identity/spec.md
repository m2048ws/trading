## MODIFIED Requirements

### Requirement: Checked runtime evidence
`SameDimension[A, B]` SHALL remain one restricted capability for explicit alignment and equivalence-aware comparison
whether it was derived from statically visible equivalent powers or recovered from authoritative runtime witnesses.
Runtime recovery SHALL issue `SameDimension` only after canonical dimension equality and, for registered witnesses,
shared registry ownership. `SameDimension` SHALL expose controlled quantity and grid coercion through `alignTo` and MAY
be consumed as contextual evidence by explicit advanced comparison; it SHALL NOT expose unrestricted Scala type
equality and SHALL NOT be consumed by homogeneous addition or subtraction to align different static dimension types.

`trading.quantity.grid.SameGrid` SHALL remain generic mathematical grid-identity evidence: it MAY be recovered for
matching generative `GridRef` values without registered provenance, and SHALL check ordinary canonical dimension, grid
ID, version, and quantum compatibility. `RuntimeEvidence.sameGrid` SHALL remain the registry-aware operation. It SHALL
first verify that both registered witnesses share registry ownership and, only after that succeeds, SHALL delegate to or
perform the ordinary `SameGrid` compatibility checks. Runtime evidence SHALL remain a scoped success value rather than a
global implicit conversion or an unchecked claim derived from identifiers alone. Reflexive `SameDimension[D, D]` SHALL
remain structural type identity and SHALL NOT certify that `D` is a canonical static power representation. Static
operation-result validation and checked runtime witness recovery remain independent trust boundaries. Transparent Scala
type annotations SHALL NOT alter runtime dimension identity: accepted annotated static inputs SHALL normalize to the
same canonical output and `DimKey` as their unannotated underlying dimensions.

#### Scenario: Coerce an exact quantity
- **WHEN** two registry witnesses have checked-equal canonical dimensions and recovery supplies
  `SameDimension[Source, Target]`
- **THEN** `Quantity[Source].alignTo[Target]` safely changes only the phantom dimension type and preserves the exact
  coefficient

#### Scenario: Coerce a grid quantity
- **WHEN** checked dimension equality supplies `SameDimension[Source, Target]` for a grid quantity
- **THEN** `GridQuantity[Source, G].alignTo[Target]` preserves its grid type and integer coordinate while changing only
  the phantom dimension type

#### Scenario: Align an exact quantity after runtime recovery
- **WHEN** two registry witnesses have checked-equal canonical dimensions
- **THEN** the recovered `SameDimension` permits `alignTo` to change only the phantom dimension type of an exact quantity

#### Scenario: Align a grid quantity after runtime recovery
- **WHEN** checked dimension equality is available for a grid quantity
- **THEN** `alignTo` preserves its grid type and integer coordinate while selecting the target dimension type

#### Scenario: Consume static and runtime evidence uniformly
- **WHEN** a caller receives `SameDimension[A, B]` from either static derivation or successful runtime recovery
- **THEN** the same explicit `alignTo` operation accepts the capability, after which ordinary arithmetic requires exact
  static dimension types

#### Scenario: Reject direct evidence-driven arithmetic
- **WHEN** runtime recovery supplies `SameDimension[A, B]` for distinct static dimension types
- **THEN** addition and subtraction still reject `Quantity[A]` with `Quantity[B]` until the caller explicitly aligns one
  value

#### Scenario: Reject runtime dimension mismatch
- **WHEN** independently resolved witnesses have different canonical dimension keys
- **THEN** runtime recovery returns an explicit mismatch and no `SameDimension` capability is issued

#### Scenario: Recover generic evidence for generative grids
- **WHEN** two generative `GridRef` values have matching canonical dimension, grid ID, version, and quantum
- **THEN** `SameGrid.between` can recover mathematical grid-identity evidence without registry provenance

#### Scenario: Check registry ownership before grid compatibility
- **WHEN** `RuntimeEvidence.sameGrid` compares equal-looking registered grids owned by different registries
- **THEN** it returns a foreign-registry failure before performing ordinary grid compatibility checks

#### Scenario: Ignore transparent annotations in runtime identity
- **WHEN** accepted static arithmetic uses an annotated atom, reducible expression, or transparent alias
- **THEN** its authoritative runtime key equals the key produced from the corresponding unannotated operands

### Requirement: Heterogeneous grid quantities recover evidence before arithmetic
Heterogeneous registered values SHALL be represented as `ResolvedAssetGridQuantity` or `ResolvedGridQuantity`. Same-grid
arithmetic SHALL recover checked grid evidence before retaining a grid result. Exact cross-grid arithmetic SHALL recover
checked dimension evidence, explicitly embed both operands, align one embedded quantity to the selected result
dimension, and only then invoke exact-type homogeneous arithmetic. It SHALL return `ResolvedExactQuantity` containing
`Quantity[D]` indexed by the selected authoritative runtime witness.

#### Scenario: Add heterogeneous same-grid values
- **WHEN** two resolved grid quantities carry the same registered grid identity
- **THEN** their exact coordinate sum is returned as `ResolvedGridQuantity`

#### Scenario: Add heterogeneous distinct-grid values
- **WHEN** two resolved quantities share a runtime dimension but have different grids and static dimension types
- **THEN** checked recovery explicitly aligns one exact embedding before addition returns `ResolvedExactQuantity`

#### Scenario: Reject heterogeneous dimension mismatch
- **WHEN** resolved USD and BTC grid quantities are added without a rate
- **THEN** the operation returns a heterogeneous dimension error without attempting alignment or arithmetic
