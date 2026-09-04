## MODIFIED Requirements

### Requirement: Stable identity is explicit and dimension-scoped

`AssetId`, `GridId`, `GridVersion`, and `GridKey` SHALL be Scala-owned reference-data value types. `GridKey` SHALL
contain a `GridId` and positive `GridVersion` and SHALL remain local to one canonical dimension. Full stable grid
identity SHALL be the product of canonical `DimKey` and `GridKey`; the reference-data API SHALL represent that full
product explicitly rather than treating an unqualified `GridKey` as globally unique.

Stable identifiers SHALL use immutable values with value equality, hashing, and domain-readable display. Their
documented Scala smart constructors SHALL reject null before returning and SHALL return the precise reference-data-
owned failures `EmptyAssetId`, `EmptyGridId`, and `NonPositiveGridVersion` for ordinary expected invalidity. Product
copying or another documented unchecked path SHALL NOT bypass those factories. Constructor modifiers and deliberate
same-package or JVM bypass are not compatibility or security commitments. Equal quanta SHALL NOT erase distinct asset
IDs, grid IDs, versions, or dimension scopes. Reference data SHALL contain no production Java source for these values.

`GridDefinition` SHALL likewise be an immutable value whose quantum is positive before the definition returns. Its
supported Scala entry SHALL accept an established `PositiveRational` directly without defensive revalidation, and no
raw rational domain factory SHALL remain on `GridDefinition`. A checked external reconstruction boundary MAY accept a
raw `Rational`, but it SHALL establish `PositiveRational` and return `NonPositiveGridQuantum` for zero or negative input
before calling the definition factory or returning a definition or handle. Such reconstruction support SHALL NOT
establish an ordinary-Java domain API contract.

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
- **THEN** no supported Java domain API or production Java identity implementation is present; external values enter
  through an owning checked Scala boundary

#### Scenario: Reject nonpositive stable grid definitions

- **WHEN** an external reconstruction boundary supplies zero or a negative raw quantum
- **THEN** the owning external boundary maps the failed positive refinement to `NonPositiveGridQuantum`, the direct
  definition factory is not called, and no `GridDefinition`, anonymous `GridRef`, or registry-issued `GridHandle` is
  returned
