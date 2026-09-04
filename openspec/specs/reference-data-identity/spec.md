# reference-data-identity Specification

## Purpose
Defines immutable asset and stable-grid identities and the trusted handles that connect those reference-data concepts
to the anonymous dimensions and mathematical grids supplied by the quantity foundation.
## Requirements
### Requirement: Reference data is a one-way layer above quantities
Stable asset identity, stable grid identity and version, reference-data definitions, and trusted identity-bearing
handles SHALL be delivered by a `trading-reference-data` artifact that depends on `trading-quantities`. The quantities
artifact SHALL remain independently usable and SHALL NOT expose asset identifiers, stable grid identifiers or versions,
catalog provenance, registration, lookup, or persistence records.

Reference-data values MAY expose the public quantity types needed to describe their dimensions, grids, quanta, and
coordinates. No quantity API SHALL require a reference-data type in order to perform exact arithmetic, construct an
anonymous grid, project or quantize a value, or use mathematical grid evidence.

#### Scenario: Use quantities without reference data
- **WHEN** downstream Scala depends only on `trading-quantities`
- **THEN** it can construct and use exact quantities and anonymous grids, while asset and stable-grid identity types are
  absent

#### Scenario: Use reference data over quantities
- **WHEN** downstream Scala depends on `trading-reference-data`
- **THEN** it can relate stable assets and grid versions to public quantity dimensions and anonymous mathematical grids

#### Scenario: Prevent a reverse dependency
- **WHEN** the reference-data artifact is removed from the quantity compile classpath
- **THEN** the quantity artifact still compiles and its public mathematical behavior remains available

### Requirement: Stable identity is explicit and dimension-scoped

`AssetId`, `GridId`, `GridVersion`, and `GridKey` SHALL be reference-data value types. `GridKey` SHALL contain a
`GridId` and positive `GridVersion` and SHALL remain local to one canonical dimension. Full stable grid identity SHALL
be the product of canonical `DimKey` and `GridKey`; the reference-data API SHALL represent that full product explicitly
rather than treating an unqualified `GridKey` as globally unique.

Stable identifiers SHALL use immutable values with value equality, hashing, and domain-readable display. Their
documented Scala smart constructors SHALL reject null before returning and SHALL return the precise reference-data-
owned failures `EmptyAssetId`, `EmptyGridId`, and `NonPositiveGridVersion` for ordinary expected invalidity. Product
copying or another documented unchecked path SHALL NOT bypass those factories. Constructor modifiers and deliberate
same-package or JVM bypass are not compatibility or security commitments. Equal quanta SHALL NOT erase distinct asset
IDs, grid IDs, versions, or dimension scopes.

`GridDefinition` SHALL likewise be an immutable value whose quantum is positive before the definition returns. Its
supported Scala entry SHALL accept `PositiveRational`; a checked external reconstruction boundary MAY accept raw
`Rational` and SHALL return `NonPositiveGridQuantum` for zero or negative input before a definition or handle is
returned. Such reconstruction support SHALL NOT establish an ordinary-Java domain API contract.

#### Scenario: Reuse a local grid key across dimensions

- **WHEN** the same grid ID and version are qualified once by USD and once by BTC
- **THEN** the two full grid identities are distinct

#### Scenario: Distinguish versions with equal definitions

- **WHEN** two versions of one grid ID have the same exact quantum
- **THEN** they remain distinct stable identities even though their mathematical grids are numerically compatible

#### Scenario: Reject invalid stable identifiers

- **WHEN** a caller supplies an empty stable identifier or a nonpositive grid version
- **THEN** the smart constructor returns the corresponding typed reference-data failure without throwing for that
  expected invalidity

#### Scenario: Reject stable-identity construction bypasses

- **WHEN** supported Scala attempts direct, product-copy, or reconstruction syntax instead of the documented stable-
  identity smart constructor
- **THEN** no documented unchecked path returns a stable identity, while constructor modifiers remain an unsupported
  implementation detail rather than a security boundary

#### Scenario: Reject Java stable-identity construction bypasses

- **WHEN** ordinary Java source attempts to construct a stable identity through domain implementation details
- **THEN** no supported Java domain API is promised; external values enter through an owning checked Scala boundary,
  and direct JVM constructor availability or privacy remains outside the compatibility and anti-forgery contract

#### Scenario: Reject nonpositive stable grid definitions

- **WHEN** supported Scala or an external reconstruction boundary supplies zero or a negative raw quantum
- **THEN** the checked boundary returns `NonPositiveGridQuantum` before a `GridDefinition`, anonymous `GridRef`, or
  registry-issued `GridHandle` is returned

### Requirement: Trusted dimension and asset handles retain authority

A trusted `DimensionHandle[D]` SHALL retain one authoritative `DimRef[D]`, its equal canonical `DimKey`, and opaque
issuer lineage. A trusted `Asset` SHALL retain an `AssetId` and one such dimension handle through a path-dependent
dimension type. Neither handle SHALL expose lookup, registration, mutable state, locking, or I/O. The documented
construction path SHALL be catalog issuance, which alone combines stable identity, dimension witness, and issuer
lineage; possessing visible component values SHALL not provide an ordinary unchecked operation for manufacturing that
combination.

Reference-data resolution is signified by catalog construction and by the trusted handle type itself. Constructor
secrecy SHALL NOT be described as protection from deliberately hostile in-process code. Operations that compare,
retype, decode, or otherwise strengthen supplied handles SHALL continue to establish issuer-lineage, stable-identity,
dimension, and definition agreement through checked reconciliation. Supported source-level construction and
observation SHALL be Scala 3; ordinary-Java handle construction is outside the supported domain contract.

#### Scenario: Retain an asset's dependent dimension

- **WHEN** trusted reference data supplies an `Asset`
- **THEN** the asset's path-dependent dimension, dimension handle, `DimRef`, and canonical key agree

#### Scenario: Reject an arbitrary asset binding

- **WHEN** ordinary downstream code has an `AssetId` and an independently selected `DimRef`
- **THEN** the documented API provides no unchecked operation that promotes that pair to a catalog-issued `Asset`

#### Scenario: Obtain trusted handles through the catalog

- **WHEN** supported Scala code needs a trusted dimension, asset, or grid handle
- **THEN** it obtains the handle through catalog resolution rather than a caller-defined implementation or unchecked
  constructor

#### Scenario: Reject Java handle implementation construction

- **WHEN** ordinary Java source attempts to manufacture a trusted dimension, asset, or grid handle
- **THEN** no supported Java handle-construction API is promised, and trusted authority still arises only through
  catalog resolution and checked reconciliation

#### Scenario: Reconcile supplied handles semantically

- **WHEN** a consumer must combine or retype independently obtained handles
- **THEN** checked reconciliation validates lineage and identity agreement before issuing stronger evidence

#### Scenario: Calculate without returning to reference data

- **WHEN** domain code receives a trusted asset handle and values already indexed by its dimension
- **THEN** it may retain and calculate with those immutable values without performing a lookup or registration

### Requirement: Grid handles compose stable and mathematical identity
A trusted `GridHandle[D]` SHALL retain a full stable grid identity, a matching trusted `DimensionHandle[D]`, and one
underlying anonymous mathematical `GridRef[D]` whose associated coordinate type `G`, authoritative dimension, and
positive exact quantum are preserved. Its public coordinate construction, coordinate observation, and exact embedding
SHALL delegate to that underlying grid and SHALL return the same `GridQuantity[D, G]` and `Quantity[D]` results.

The handle SHALL carry opaque issuer lineage and SHALL have no public constructor that can attach a stable identity to
an arbitrary anonymous grid. A plain `GridRef[D]`, including one with equal dimension and quantum, SHALL NOT satisfy an
API requiring a trusted `GridHandle[D]`. Possessing a grid handle SHALL confer no catalog lookup or mutation capability.

#### Scenario: Use a stable handle as a grid witness
- **WHEN** a caller constructs a coordinate or embeds it through a trusted grid handle
- **THEN** the operation has exactly the coordinate type, dimension, quantum, and exact value of the handle's underlying
  mathematical grid

#### Scenario: Keep equal-quantum identities distinct
- **WHEN** two grid handles share a dimension and quantum but have different IDs or versions
- **THEN** their stable identities and coordinate namespaces remain distinct

#### Scenario: Reject promotion of an anonymous grid
- **WHEN** downstream Scala constructs an anonymous `GridRef` and chooses a stable key with matching numerical fields
- **THEN** it cannot manufacture a trusted `GridHandle` or catalog provenance

### Requirement: Stable-handle reconciliation is checked separately
Reference data SHALL provide checked reconciliation for independently obtained dimension, asset, and grid handles.
Stable-grid reconciliation SHALL verify shared opaque issuer lineage, equal full grid identity, equal canonical
dimension, and one immutable mathematical definition before returning evidence that can retype a coordinate between
the two handles. Stable-dimension reconciliation SHALL verify issuer lineage and canonical dimension before returning
ordinary `SameDimension` evidence.

Failures SHALL distinguish foreign issuer lineage, different stable identities, dimension mismatch, and conflicting
immutable definitions. Numerical `SameQuantum` or `Embedding` evidence from the quantity layer SHALL NOT establish
stable identity or issuer provenance, and stable-handle reconciliation SHALL NOT perform a live lookup.

#### Scenario: Reconcile two canonical views of one handle
- **WHEN** independently retained handles come from the same issuer lineage and name the same immutable full grid
  identity
- **THEN** checked reconciliation permits their matching coordinates to be combined through ordinary same-grid
  arithmetic

#### Scenario: Reject equal-looking foreign handles
- **WHEN** two handles have equal visible IDs, dimensions, versions, and quanta but different issuer lineages
- **THEN** reconciliation returns a typed foreign-lineage failure and issues no stable-grid evidence

#### Scenario: Keep numerical compatibility weaker than identity
- **WHEN** two different stable grids admit `SameQuantum` or an exact `Embedding`
- **THEN** that mathematical evidence permits only its documented numerical conversion and does not make the handles
  stably identical

### Requirement: Reference-data values have an explicit persistence boundary
Stable identifiers and definitions SHALL remain ordinary immutable in-memory values. Stable identifiers, definitions,
reference-data errors, trusted handles, and their dependent values SHALL NOT be advertised as a Java-serialization or
production wire format; Java object serialization of those invariant-bearing values SHALL fail through the
project-owned unsupported-serialization mechanism. Durable encoding and checked reconstruction SHALL require the
separately specified boundary-codec capability.

#### Scenario: Reject Java serialization of reference-data values
- **WHEN** a caller passes a stable identifier, definition, error, asset, dimension, or grid handle to
  `ObjectOutputStream`
- **THEN** serialization fails without creating a supported persistence payload

#### Scenario: Defer durable reconstruction
- **WHEN** a database or wire adapter needs to reconstruct a trusted handle from stable IDs
- **THEN** it uses the future explicit codec and catalog-snapshot boundary rather than Java serialization or a quantity
  constructor

