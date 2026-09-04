## MODIFIED Requirements

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
