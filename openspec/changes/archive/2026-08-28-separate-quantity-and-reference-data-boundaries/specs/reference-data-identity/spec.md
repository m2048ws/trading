## Purpose

Defines immutable asset and stable-grid identities and the trusted handles that connect those reference-data concepts
to the anonymous dimensions and mathematical grids supplied by the quantity foundation.

## ADDED Requirements

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

Stable identifiers SHALL use privately constructed non-case immutable values with value equality, hashing, and
domain-readable display. Their public smart constructors SHALL reject null before returning and SHALL return the
precise reference-data-owned failures `EmptyAssetId`, `EmptyGridId`, and `NonPositiveGridVersion` for ordinary expected
invalidity. No public constructor, `apply`, `copy`, product-construction path, or same-package source authority SHALL
bypass those smart constructors. This construction restriction SHALL be enforced by JVM constructor visibility in the
completed artifact, not only by Scala/TASTy visibility. Empty asset and grid identifiers and nonpositive grid versions
SHALL therefore produce no normally returned identity value from downstream Scala or Java. Equal quanta SHALL NOT
erase distinct asset IDs, grid IDs, versions, or dimension scopes.

`GridDefinition` SHALL likewise be a non-product immutable value whose quantum is positive before the definition
returns. Its ordinary Scala entry SHALL accept `PositiveRational`, its typed raw/JVM factory SHALL return
`NonPositiveGridQuantum` for zero or negative `Rational`, and its erased refined entry SHALL defensively revalidate.
No public two-field constructor, generated `copy`, or `fromProduct` path SHALL attach a nonpositive quantum, and a
registry SHALL therefore never issue a `GridHandle` backed by such a definition.

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
- **WHEN** downstream or same-package Scala attempts direct construction, case-class `apply`/`copy`, or product
  reconstruction of a stable identifier
- **THEN** compilation fails while supported smart construction remains available

#### Scenario: Reject Java stable-identity construction bypasses
- **WHEN** downstream Java compiles against the completed reference-data JAR and attempts to invoke a stable-identity
  constructor directly
- **THEN** JVM-private constructors reject the source while the typed public smart constructors remain available

#### Scenario: Reject nonpositive stable grid definitions
- **WHEN** supported Scala or Java supplies zero or a negative raw quantum through definition construction, product
  reconstruction, copying, or an erased refined entry
- **THEN** the typed raw boundary returns `NonPositiveGridQuantum` or the unsupported erased path fails closed before a
  `GridDefinition`, anonymous `GridRef`, or registry-issued `GridHandle` is returned

### Requirement: Trusted dimension and asset handles retain authority
A trusted `DimensionHandle[D]` SHALL retain one authoritative `DimRef[D]`, its equal canonical `DimKey`, and opaque
issuer lineage. A trusted `Asset` SHALL retain an `AssetId` and one such dimension handle through a path-dependent
dimension type. Neither handle SHALL expose lookup, registration, mutable state, locking, I/O, or a public construction
path that lets callers pair an arbitrary stable identity with an independently chosen dimension witness or issuer.
The public handle representations SHALL be final values with Scala-private construction, SHALL retain no public
implementation hierarchy, and SHALL check registry-private issuance authority before retaining lineage or domain
fields. Any JVM-callable constructor SHALL terminate before returning a value without that authority; Scala/TASTy
privacy alone SHALL NOT establish this JVM boundary.

Reference-data resolution is signified by controlled construction and by the trusted handle type itself. The public
names SHALL be `Asset` and `DimensionHandle`; the API SHALL NOT require parallel `UnresolvedAsset` types or a blanket
`Resolved` prefix when raw definitions and identifiers already make the trust boundary unambiguous.

#### Scenario: Retain an asset's dependent dimension
- **WHEN** trusted reference data supplies an `Asset`
- **THEN** the asset's path-dependent dimension, dimension handle, `DimRef`, and canonical key agree

#### Scenario: Reject an arbitrary asset binding
- **WHEN** downstream Scala attempts to pair an `AssetId` with a caller-selected `DimRef` as a trusted `Asset`
- **THEN** no supported public construction path is available

#### Scenario: Reject Java handle implementation construction
- **WHEN** downstream Java attempts to implement or extend a public handle representation or invokes its JVM constructor
  without registry issuance authority, including with lineage observed from a legitimate handle
- **THEN** compilation or construction terminates before a trusted handle is returned

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
