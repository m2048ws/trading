## ADDED Requirements

### Requirement: Static dimension capabilities remain independent
The public static-dimension model SHALL keep validity, runtime inhabitation, equivalence, and exact values as independent
contracts. `Normalize[D]` SHALL certify only that `D` is a valid closed static dimension expression and has one canonical
output. Deriving `Normalize[D]` or `Normalize[Atom[K]]` SHALL NOT assert, synthesize, or otherwise imply that a
`DimRef[D]`, `DimRef[Atom[K]]`, or `DimRef` for the canonical output exists. A concrete stable singleton key MAY
therefore be accepted by static normalization without belonging to the smaller set of atom types inhabitable through
supported public `DimRef` APIs.

`SameDimension[A, B]` SHALL remain controlled evidence that `A` and `B` denote the same dimension. The capability SHALL
NOT independently certify that either expression is valid and SHALL NOT establish runtime inhabitation for either side.
Reflexive evidence MAY exist from Scala type identity alone; every arithmetic boundary that requires a valid dimension
SHALL independently require the applicable `Normalize` evidence.

`Quantity[D]` SHALL remain an exact coefficient indexed by `D`, not a runtime identity witness. Possessing a
`Quantity[D]` SHALL NOT provide a `DimRef[D]` or `DimensionKey`. Dimension-polymorphic zero SHALL remain available for any
normalized `D`; attaching a caller-supplied coefficient SHALL continue to require an authoritative `DimRef[D]`.
Similarly, possession of `DimRef[D]` SHALL NOT implicitly materialize contextual `Normalize[D]` evidence for generic
code that performs static arithmetic.

#### Scenario: Normalize a key without runtime inhabitation
- **WHEN** a supported concrete stable singleton key `K` admits `Normalize[Atom[K]]` but has no public authority-bearing
  `DimRef` constructor
- **THEN** static normalization succeeds and the evidence alone provides no way to obtain `DimRef[Atom[K]]`

#### Scenario: Construct only static zero without a witness
- **WHEN** `D` has `Normalize[D]` but no `DimRef[D]` is available
- **THEN** `Quantity.zero[D]` is available, while every public constructor that attaches a caller-supplied coefficient
  still requires `DimRef[D]`

#### Scenario: Keep generic runtime and static evidence separate
- **WHEN** generic code receives `DimRef[D]` and performs dimension-preserving arithmetic over `D`
- **THEN** it must separately accept and forward `Normalize[D]`; the runtime witness does not satisfy static evidence
  search

#### Scenario: Keep reflexivity separate from validity
- **WHEN** a malformed `Dim` representation obtains reflexive `SameDimension[D, D]` through Scala type identity
- **THEN** normalization and arithmetic over `D` remain unavailable because the required `Normalize[D]` cannot be derived

#### Scenario: Keep equivalence separate from runtime identity
- **WHEN** `SameDimension[A, B]` is derived from statically equivalent closed expressions without runtime witnesses
- **THEN** it permits only the documented controlled coercions and does not furnish a `DimRef` or `DimensionKey` for
  either expression
