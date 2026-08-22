## ADDED Requirements

### Requirement: Checked runtime reconstruction preserves carrier trust
Registry adoption, checked logical decoding, and heterogeneous result construction SHALL return dimensional carriers
only through registry-owned dimension and grid witnesses. Every normally returned `Quantity[D]` or
`GridQuantity[D, G]` inside a resolved runtime carrier SHALL therefore have a valid dimension index and MAY undergo
index-preserving transformations without `Normalize[D]`.

Raw packed identities, coordinates, and caller-selected type arguments SHALL NOT become dimensional values without the
existing dimension-first registry checks. Possessing a reconstructed value SHALL NOT allow callers to recover or forge
its registered witness, registry owner, `DimRef`, runtime key, or grid provenance; those capabilities remain available
only from the resolved dependent package and registry APIs.

#### Scenario: Transform a decoded grid value
- **WHEN** a checked packed record is decoded to a resolved grid quantity and generic code performs same-index grid
  arithmetic on its value
- **THEN** the arithmetic requires no static normalization evidence

#### Scenario: Transform a heterogeneous exact result
- **WHEN** checked heterogeneous arithmetic returns `ResolvedExactQuantity`
- **THEN** its dependent `Quantity[result.dimension.D]` can undergo index-preserving arithmetic without
  `Normalize[result.dimension.D]`

#### Scenario: Reject unchecked packed construction
- **WHEN** raw packed data names an unknown, mismatched, foreign, or conflicting dimension or grid identity
- **THEN** decoding fails before attaching its coordinate to a typed grid carrier

#### Scenario: Keep runtime provenance non-extractable
- **WHEN** code possesses only the dimensional value stored inside a resolved runtime result
- **THEN** the value alone does not supply registry ownership, registered grid identity, `DimRef`, or `DimensionKey`
  authority

## MODIFIED Requirements

### Requirement: Heterogeneous grid quantities recover evidence before arithmetic
Heterogeneous registered values SHALL be represented as `ResolvedAssetGridQuantity` or `ResolvedGridQuantity`. Same-grid
arithmetic SHALL recover checked grid evidence, retype one trusted coordinate to the selected grid type, and then invoke
exact-type grid arithmetic without `Normalize`. Exact cross-grid arithmetic SHALL recover checked dimension evidence,
explicitly embed both operands, align one embedded quantity to the selected result dimension, and then invoke exact-type
homogeneous arithmetic without static normalization evidence. It SHALL return `ResolvedExactQuantity` containing
`Quantity[D]` indexed by the selected authoritative runtime witness.

#### Scenario: Add heterogeneous same-grid values
- **WHEN** two resolved grid quantities carry the same registered grid identity
- **THEN** checked grid recovery permits their exact coordinate sum to be returned as a trusted
  `ResolvedGridQuantity` without `Normalize`

#### Scenario: Add heterogeneous distinct-grid values
- **WHEN** two resolved quantities share a runtime dimension but have different grids and static dimension types
- **THEN** checked recovery explicitly aligns one exact embedding before proof-free homogeneous addition returns a
  trusted `ResolvedExactQuantity`

#### Scenario: Reject heterogeneous dimension mismatch
- **WHEN** resolved USD and BTC grid quantities are added without a rate
- **THEN** the operation returns a heterogeneous dimension error without attempting alignment or arithmetic
