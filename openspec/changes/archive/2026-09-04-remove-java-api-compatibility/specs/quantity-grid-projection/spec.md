## MODIFIED Requirements

### Requirement: Mathematical grid witnesses are anonymous

`GridRef[D]` SHALL describe only an authoritative `DimRef[D]`, a positive exact rational quantum, and a generative
coordinate namespace `G`. Creating a uniform mathematical grid SHALL require the dimension witness and quantum and
SHALL NOT accept, synthesize, or expose a stable grid ID, version, catalog key, asset, or issuer provenance.

Each successful grid construction SHALL create a fresh coordinate namespace even when another grid has the same
dimension and quantum. Stable naming of a mathematical grid SHALL be supplied only by a reference-data handle in a
downstream artifact.

The supported source API SHALL be Scala 3 and SHALL accept an established `PositiveRational` for refined grid
construction. While a checked raw/JVM entry remains available for external reconstruction, it SHALL return
`ExpectedPositive` for zero or negative input before grid authority is returned; its presence SHALL NOT establish an
ordinary-Java domain API contract.

#### Scenario: Construct an anonymous grid

- **WHEN** a supported Scala caller supplies an authoritative USD dimension and positive quantum `1/100`
- **THEN** grid construction returns a mathematical witness with a fresh coordinate namespace and no stable identity

#### Scenario: Repeat an equal mathematical definition

- **WHEN** a caller constructs two anonymous grids with the same dimension and quantum
- **THEN** the grids have distinct coordinate namespaces until explicit mathematical evidence relates them

#### Scenario: Exclude stable identity from the factory

- **WHEN** downstream source attempts to pass a grid ID or version to mathematical uniform-grid construction
- **THEN** no such quantity-layer parameter or overload exists

#### Scenario: Reject a nonpositive raw grid quantum

- **WHEN** an external reconstruction boundary supplies zero or a negative rational to the checked raw grid entry
- **THEN** it receives `ExpectedPositive` and no `GridRef` is returned
