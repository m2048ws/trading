## MODIFIED Requirements

### Requirement: Checked runtime evidence
`SameDimension[A, B]` SHALL be one restricted capability accepted by dimension-safe operations whether it was derived
from statically visible equivalent powers or recovered from authoritative runtime witnesses. Runtime recovery SHALL issue
`SameDimension` only after canonical dimension equality and, for registered witnesses, shared registry ownership.
`SameDimension` SHALL expose controlled quantity and grid coercion and MAY be consumed as contextual evidence by
dimension-safe operations; it SHALL NOT expose unrestricted Scala type equality. `trading.quantity.grid.SameGrid` SHALL
remain generic mathematical grid-identity evidence: it MAY be recovered for matching generative `GridRef` values without
registered provenance, and SHALL check ordinary canonical dimension, grid ID, version, and quantum compatibility.
`RuntimeEvidence.sameGrid` SHALL remain the registry-aware operation. It SHALL first verify that both registered witnesses
share registry ownership and, only after that succeeds, SHALL delegate to or perform the ordinary `SameGrid` compatibility
checks. Runtime evidence SHALL remain a scoped success value rather than a global implicit conversion or an unchecked
claim derived from identifiers alone. Reflexive `SameDimension[D, D]` SHALL remain structural type identity and SHALL
NOT certify that `D` is a canonical static power representation. Static operation-result validation and checked runtime
witness recovery remain independent trust boundaries.
Transparent Scala type annotations SHALL NOT alter runtime dimension identity: accepted annotated static inputs SHALL
normalize to the same canonical output and `DimensionKey` as their unannotated underlying dimensions.

#### Scenario: Coerce an exact quantity
- **WHEN** two registry witnesses have checked-equal canonical dimensions
- **THEN** `coerceQuantity` safely changes only the phantom dimension type of `Quantity[D]`

#### Scenario: Coerce a grid quantity
- **WHEN** checked dimension equality is available for a grid quantity
- **THEN** `coerceGrid` preserves its grid type and integer coordinate

#### Scenario: Consume static and runtime evidence uniformly
- **WHEN** a dimension-safe operation receives `SameDimension[A, B]` from either static derivation or successful runtime
  recovery
- **THEN** it accepts the same restricted capability without requiring the caller to use a different arithmetic API

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
