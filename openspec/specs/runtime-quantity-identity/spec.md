# runtime-quantity-identity Specification

## Purpose

Defines runtime dimensions and assets, registered grid provenance, heterogeneous exact arithmetic, and logical packing
for registered grid quantities.
## Requirements
### Requirement: Runtime dimensions and assets
The runtime layer SHALL create opaque dimension witnesses for runtime identifiers and SHALL normalize compound
dimensions through canonical `DimensionKey` values. `AssetRef` SHALL bind an asset identity to its canonical dimension
without imposing a grid. `DimensionKey` SHALL be the runtime free abelian group of dimensions and SHALL expose
multiplicative identity, multiplication, inverse, and canonical equality through an opt-in production algebra
instance. Those operations SHALL delegate to primitive normalization. Algebraic equality SHALL NOT replace registry
identity, registry ownership, registered provenance, or full grid identity.

#### Scenario: Register an asset
- **WHEN** an asset definition is registered
- **THEN** the registry returns an asset witness with a canonical path-dependent dimension type

#### Scenario: Normalize a compound dimension
- **WHEN** runtime dimension multiplication and inversion cancel factors
- **THEN** the resulting canonical key is equal regardless of expression shape

#### Scenario: Algebraic equality does not confer authority
- **WHEN** two normalized keys or numerical grid definitions compare equal
- **THEN** registered operations still require witnesses created by the owning `QuantityRegistry`

### Requirement: Registered witnesses retain provenance
`RegisteredDimensionRef` and `RegisteredGridRef` SHALL be registry-owned nominal witnesses. A plain generative
dimension or grid witness SHALL NOT satisfy registered provenance, and a witness owned by another registry SHALL be
rejected by runtime evidence recovery. Concrete registered dimension, grid, asset, and dimension-witness
implementations SHALL be lexically private to each `QuantityRegistry`; only successful operations of that registry
SHALL produce them. Package-qualified or top-level privacy SHALL NOT be the provenance boundary.

#### Scenario: Reject a plain grid for registered packing
- **WHEN** a caller attempts to pack through `UniformGrid.create` without registration
- **THEN** the code does not compile against the registered packing API

#### Scenario: Reject foreign registry evidence
- **WHEN** equal-looking witnesses from different registries are compared for trusted evidence
- **THEN** evidence recovery returns an explicit foreign-registry failure

#### Scenario: Reject same-package witness construction
- **WHEN** downstream Scala declares `package trading.quantity.runtime` and attempts to instantiate a concrete registered witness
  or provide an arbitrary registry owner
- **THEN** lexical registry ownership prevents the construction

### Requirement: Full grid identity is dimension-scoped
Full registered grid identity SHALL be `(canonical DimensionKey, GridId, GridVersion)`. A `GridKey` containing only ID
and version SHALL remain dimension-local. Definitions SHALL be immutable per full identity, and equal quanta SHALL NOT
erase distinct IDs or versions. The exact positive rational quantum associated with a registered full identity SHALL
remain immutable and SHALL be obtainable only through the registry-produced witness for that identity.

#### Scenario: Reuse a local grid key under another dimension
- **WHEN** the same `GridId` and `GridVersion` are registered under USD and BTC
- **THEN** the registry stores distinct full grid identities

#### Scenario: Reject a conflicting definition
- **WHEN** the same full identity is registered with a different quantum
- **THEN** registration fails without replacing the canonical definition

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
same canonical output and `DimensionKey` as their unannotated underlying dimensions.

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

### Requirement: Registered grid quantities have unambiguous packed records
The logical packed types SHALL be named `PackedAssetGridQuantity` and `PackedGridQuantity`. They SHALL contain asset or
dimension identity, grid ID, grid version, and integer coordinate. Checked decoding SHALL return
`ResolvedAssetGridQuantity` or `ResolvedGridQuantity` with registry-owned witnesses.
Packing SHALL require true registry-produced `RegisteredGridRef` provenance; a plain or counterfeit witness with an
equal-looking dimension, ID, and version SHALL not satisfy the API.

#### Scenario: Pack and reconstruct an asset grid quantity
- **WHEN** a registered satoshi quantity is packed and decoded through the same definitions
- **THEN** asset, dimension, grid identity, version, and coordinate are reconstructed exactly

#### Scenario: Pack a compound-dimension grid quantity
- **WHEN** a registered grid belongs to a normalized rate dimension
- **THEN** `PackedGridQuantity` stores that canonical dimension key and reconstructs the typed grid value

#### Scenario: Reject counterfeit quantum provenance
- **WHEN** a canonical grid has quantum `1/100` and downstream `trading.quantity.runtime` source attempts to substitute quantum
  `7/13` under the same dimension, ID, and version before packing coordinate `42`
- **THEN** counterfeit registered-witness construction and registered packing do not compile

#### Scenario: Preserve canonical coordinate interpretation
- **WHEN** registry-produced provenance packs and decodes coordinate `42` on the canonical `1/100` grid
- **THEN** the decoded coordinate remains `42` and its exact value remains `21/50`

### Requirement: Decoding verifies dimension before grid
Asset-specialized decoding SHALL verify `expectedDimension` after asset resolution and before grid lookup. General
decoding SHALL resolve the canonical dimension before resolving the dimension-scoped grid identity. Unknown dimensions,
unknown versions, remapped assets, and mismatched grids SHALL fail explicitly.

#### Scenario: Reject a remapped asset
- **WHEN** a packed asset ID resolves to a different canonical dimension than `expectedDimension`
- **THEN** decoding fails before attaching the coordinate to a grid

#### Scenario: Reject an unknown historical version
- **WHEN** the referenced grid version is not registered for the resolved dimension
- **THEN** decoding returns an unknown-grid failure

### Requirement: Arbitrary exact quantities are not packed
The runtime layer SHALL NOT provide logical packing for arbitrary `Quantity[D]`. An exact packed format is deferred
until numerator, denominator, dimension identity, and schema design are specified.

#### Scenario: Exact heterogeneous result remains in memory
- **WHEN** heterogeneous cross-grid arithmetic returns `ResolvedExactQuantity`
- **THEN** it has no automatic conversion to a grid-packed record

#### Scenario: Projection precedes grid packing
- **WHEN** an exact result must enter a registered grid boundary
- **THEN** callers first narrow or quantize explicitly and then pack the resulting registered grid quantity

### Requirement: Logical packing is not a wire schema
Grid-packed case classes SHALL be logical in-memory boundary records, not a stable production wire format. A future wire
format SHALL define a separate schema version and version-dispatching decoder; `GridVersion` SHALL continue to select
only the immutable grid definition and coordinate interpretation.

#### Scenario: Evolve a future record shape
- **WHEN** a future wire record changes fields or codecs
- **THEN** a wire-schema version, not `GridVersion`, selects the decoder

### Requirement: Java serialization fails closed
Java object serialization SHALL NOT reconstruct registry identities, packed records, dependent resolved results, or
invariant-bearing public result and error records. Those records SHALL fail through the common project-owned
`NotSerializableException` mechanism, while supported checked logical decoding remains available. The explicit
fail-closed inventory SHALL include `ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, and `ResolvedExactQuantity`.

#### Scenario: Serialize a grid-packed record
- **WHEN** a caller passes `PackedAssetGridQuantity` or `PackedGridQuantity` to `ObjectOutputStream`
- **THEN** serialization fails without producing a persistence payload

#### Scenario: Decode logical boundary data
- **WHEN** an in-memory packed record is passed directly to its checked decoder
- **THEN** normal registry validation and reconstruction proceed

### Requirement: Public DimRef atom authority is unique
`DimRef[D]` SHALL be the authoritative public association between an inhabited static dimension type `D` and its runtime
`DimensionKey`. For every singleton key `K` whose `Atom[K]` is inhabitable through supported public `DimRef` APIs, any two
publicly obtained values of type `DimRef[Atom[K]]` SHALL have equal `DimensionKey` values. This uniqueness requirement
SHALL apply only to publicly inhabitable atom types; the set of keys accepted by `Normalize[Atom[K]]` MAY be larger, and
normalization alone SHALL NOT make an atom type runtime-inhabitable.

Public atom construction SHALL bind static and runtime identity at one authority-bearing boundary. Literal construction
SHALL derive the runtime atom identifier from the accepted literal singleton. Nominal construction SHALL bind the result
to the supplied stable object's exact singleton type and to the runtime identifier owned by that object. Generative and
fresh runtime witnesses SHALL bind their path-dependent atom type to the identity captured by that same witness. No
supported public constructor SHALL accept a caller-selected static atom type independently from a caller-selected runtime
identity.

The public `DimRef` identity witness SHALL bind `One` to `DimensionKey.one`. Product, inverse, and quotient operations
SHALL preserve the static/runtime association inductively: each SHALL return the canonical static output established by
the complete `Normalize` operation and the corresponding runtime key produced from its authoritative input keys.
Supported downstream code SHALL NOT directly construct or implement `DimRef` to bypass these roots and operations.

#### Scenario: Repeat literal construction
- **WHEN** supported callers construct `DimRef.atom["BTC"]` more than once
- **THEN** every result has type `DimRef[Atom["BTC"]]` and the same runtime dimension key derived from `"BTC"`

#### Scenario: Repeat nominal construction
- **WHEN** a stable `NominalAtom` object is supplied to `DimRef.atom` more than once
- **THEN** every result retains that object's exact singleton atom type and the same object-owned runtime identity

#### Scenario: Reject caller-selected literal widening
- **WHEN** a caller supplies different `ValueOf[String & Singleton]` values and requests the same widened
  `DimRef[Atom[String & Singleton]]` type
- **THEN** construction is rejected because the widened key is not accepted by `Normalize[Atom[K]]`

#### Scenario: Reject caller-selected nominal widening
- **WHEN** distinct nominal objects are widened to a shared nominal singleton supertype before construction
- **THEN** the results cannot both inhabit one caller-selected `DimRef[Atom[K]]` type and retain their distinct runtime
  identities

#### Scenario: Preserve generative authority
- **WHEN** a generative or fresh runtime witness exposes its dimension repeatedly
- **THEN** that witness's exact path-dependent atom type always denotes the runtime identity captured by that witness,
  while a different witness has a distinct path-dependent atom type

#### Scenario: Do not totalize normalized keys
- **WHEN** `Normalize[Atom[K]]` succeeds for a concrete stable key outside the supported public `DimRef` authority sources
- **THEN** no `DimRef[Atom[K]]` constructor or runtime key is inferred from normalization alone

#### Scenario: Preserve authority through witness algebra
- **WHEN** public `DimRef` product, inverse, or quotient combines authoritative input witnesses
- **THEN** the returned `DimRef` has the canonical normalized output type and the exactly corresponding runtime
  `DimensionKey` operation result

#### Scenario: Reject downstream witness forgery
- **WHEN** supported downstream source attempts to implement `DimRef[D]` or invoke an unbound static/runtime constructor
- **THEN** construction is unavailable and no contradictory runtime identity can inhabit the chosen static type
