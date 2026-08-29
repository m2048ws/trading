## ADDED Requirements

### Requirement: Runtime dimension identity is domain-neutral
The quantity runtime layer SHALL create opaque dimension witnesses for runtime identifiers and SHALL interpret compound
dimensions through canonical `DimKey` values. `DimKey` SHALL remain the runtime free abelian group of dimensions and
SHALL expose multiplicative identity, multiplication, inverse, and canonical equality through its opt-in production
algebra instance. Those operations SHALL delegate to primitive arbitrary-precision key arithmetic.

`DimRef[D]` SHALL remain authoritative for the runtime identity inhabited by `D`. Its public algebra SHALL preserve
static expressions: identity returns `DimRef[One]`, product returns `DimRef[Times[A, B]]`, inverse returns
`DimRef[Inverse[A]]`, and quotient returns `DimRef[Divide[A, B]]`. Each operation SHALL compute the exactly
corresponding canonical runtime key and SHALL require no public canonical-output capability. Publicly inhabitable atom
types SHALL retain their one-to-one static/runtime authority; private static interpretation SHALL neither totalize
`DimRef` over all accepted keys nor manufacture a runtime witness.

The quantity runtime layer SHALL NOT define an asset, stable grid identity, catalog issuer, registry, or boundary
record. Algebraic equality of runtime keys SHALL NOT establish downstream asset identity, stable grid identity, or
catalog provenance.

#### Scenario: Construct a fresh runtime dimension
- **WHEN** a trusted runtime boundary creates a dimension witness for one canonical key
- **THEN** the witness retains a path-dependent type and authoritative `DimRef` without becoming an asset or catalog
  handle

#### Scenario: Canonicalize a compound runtime key
- **WHEN** runtime dimension multiplication and inversion cancel factors
- **THEN** the resulting canonical `DimKey` is equal regardless of expression shape

#### Scenario: Preserve a product witness expression
- **WHEN** `DimRef[A]` and `DimRef[B]` are multiplied
- **THEN** the result is `DimRef[Times[A, B]]` and its key is the canonical product of the input keys

#### Scenario: Preserve inverse and quotient witness expressions
- **WHEN** authoritative witnesses are inverted or divided
- **THEN** their result types are `Inverse[A]` or `Divide[A, B]` and their keys are the exact corresponding runtime
  operations

#### Scenario: Preserve public atom authority uniqueness
- **WHEN** two witnesses of the same publicly inhabitable `Atom[K]` type are obtained through supported constructors
- **THEN** they have the same runtime atom key

#### Scenario: Do not infer reference-data authority
- **WHEN** two canonical dimension keys compare equal
- **THEN** the quantity layer supplies no asset, stable grid, or catalog-lineage evidence

## MODIFIED Requirements

### Requirement: Checked runtime evidence
`SameDimension[A, B]` SHALL remain one restricted capability for explicit alignment and equivalence-aware comparison,
whether it was derived by private interpretation of statically visible expressions or recovered from authoritative
`DimRef` witnesses. Runtime recovery in the quantity artifact SHALL issue `SameDimension` only after canonical
`DimKey` equality. It SHALL expose controlled quantity and grid alignment through `alignTo`; it SHALL NOT expose
unrestricted Scala type equality, synthesize `DimRef`, install a global implicit conversion, align different static
types inside homogeneous arithmetic, or claim reference-data issuer provenance.

Reflexive `SameDimension[D, D]` SHALL remain structural Scala type identity and SHALL NOT certify static validity or
runtime inhabitation. Non-reflexive static derivation, authority-bearing construction, and checked runtime recovery
SHALL remain independent trust boundaries. Transparent Scala type annotations SHALL NOT alter static or runtime
dimension identity: accepted annotated inputs SHALL have the same private mathematical interpretation and `DimKey` as
their unannotated underlying dimensions. Stable-handle reconciliation SHALL be a downstream reference-data operation
that may return ordinary `SameDimension` only after its additional lineage checks.

#### Scenario: Align an exact quantity after runtime recovery
- **WHEN** two authoritative dimension witnesses have checked-equal canonical dimensions
- **THEN** recovered `SameDimension` permits `alignTo` to change only the phantom dimension type of an exact quantity

#### Scenario: Align a grid quantity after runtime recovery
- **WHEN** checked dimension equality is available for a grid quantity
- **THEN** `alignTo` preserves its generative grid type and integer coordinate while selecting the target dimension type

#### Scenario: Coerce an exact quantity
- **WHEN** authoritative dimension witnesses have checked-equal canonical dimensions and recovery supplies
  `SameDimension[Source, Target]`
- **THEN** `Quantity[Source].alignTo[Target]` changes only the phantom dimension type and preserves the exact coefficient;
  no separate low-level coercion operation is exposed

#### Scenario: Coerce a grid quantity
- **WHEN** checked dimension equality supplies `SameDimension[Source, Target]` for a grid quantity
- **THEN** `GridQuantity[Source, G].alignTo[Target]` preserves its generative grid type and coordinate while changing only
  the phantom dimension type; no separate low-level coercion operation is exposed

#### Scenario: Consume static and runtime evidence uniformly
- **WHEN** a caller receives `SameDimension[A, B]` from static derivation or successful runtime recovery
- **THEN** the same explicit `alignTo` operation accepts it, after which homogeneous arithmetic uses one exact static
  dimension type

#### Scenario: Reject direct evidence-driven arithmetic
- **WHEN** runtime recovery supplies `SameDimension[A, B]` for distinct static dimension types
- **THEN** addition and subtraction still reject mixed operands until the caller explicitly aligns one value

#### Scenario: Reject runtime dimension mismatch
- **WHEN** independently obtained dimension witnesses have different canonical keys
- **THEN** runtime recovery returns an explicit mismatch and issues no `SameDimension`

#### Scenario: Keep reflexivity separate from authority
- **WHEN** a malformed `Canonical` obtains reflexive `SameDimension[D, D]`
- **THEN** that evidence creates neither a `DimRef[D]` nor a quantity or grid value at `D`

#### Scenario: Recover generic evidence for generative grids
- **WHEN** two retained anonymous `GridRef` values denote the same generated mathematical grid and have coherent
  authoritative definitions
- **THEN** ordinary quantity-layer `SameGrid` recovery may issue mathematical grid evidence without stable
  reference-data identity or issuer provenance

#### Scenario: Check registry ownership before grid compatibility
- **WHEN** equal dimension witnesses are retained by handles from different reference-data lineages
- **THEN** quantity-level equality alone makes no claim that those handles may be reconciled

#### Scenario: Ignore transparent annotations in runtime identity
- **WHEN** an accepted static expression uses an annotated atom, expression, or transparent alias
- **THEN** its authoritative runtime key equals the key produced from the corresponding unannotated operands

### Requirement: Arbitrary exact quantities are not packed
The quantity artifact SHALL NOT provide logical or wire packing for arbitrary `Quantity[D]` or `GridQuantity[D, G]`.
A durable representation belongs to the boundary-codec layer and SHALL specify its own schema version, exact numeric
representation, stable identity fields, and checked reconstruction dependency.

#### Scenario: Exact heterogeneous result remains in memory
- **WHEN** runtime arithmetic produces an exact quantity in a path-dependent dimension
- **THEN** the quantity artifact provides no automatic stable-identity record for it

#### Scenario: Projection precedes grid packing
- **WHEN** an exact result must enter a stable grid-coordinate representation
- **THEN** callers first narrow or quantize explicitly and then let a downstream codec encode the trusted grid handle
  and coordinate

### Requirement: Java serialization fails closed
Java object serialization SHALL NOT reconstruct authoritative runtime dimension witnesses, dependent runtime carriers,
or invariant-bearing quantity result and error records. Those records SHALL fail through the common project-owned
`NotSerializableException` mechanism. Stable reference-data handles and boundary records SHALL define their own
serialization contracts outside the quantity artifact.

#### Scenario: Serialize a grid-packed record
- **WHEN** a caller needs to serialize a stable grid-coordinate record
- **THEN** the quantity artifact exposes no grid-packed record, and a downstream codec owns the durable representation
  and checked reconstruction

#### Scenario: Reject Java serialization of an authority-bearing runtime carrier
- **WHEN** a caller passes an authority-bearing runtime dimension carrier to `ObjectOutputStream`
- **THEN** serialization fails through the project-owned `NotSerializableException` mechanism without producing a
  supported persistence payload

#### Scenario: Decode logical boundary data
- **WHEN** an external record must recover path-dependent dimensions and grids
- **THEN** checked reconstruction occurs through explicit reference-data and codec boundaries rather than Java
  deserialization

### Requirement: Checked runtime reconstruction preserves carrier trust
Checked runtime-dimension construction and operations SHALL return dimensional carriers only through authoritative
`DimRef` and anonymous `GridRef` witnesses. Every normally returned `Quantity[D]` or `GridQuantity[D, G]` inside a
dependent runtime carrier SHALL therefore have a valid dimension index and MAY undergo index-preserving transformations
without a static-dimension capability. A dimension-changing runtime result SHALL retain the matching
expression-preserving `DimRef` produced from its authoritative inputs.

Raw runtime keys, coordinates, and caller-selected type arguments SHALL NOT become dimensional values without the
existing witness-backed construction checks. Possessing a reconstructed value SHALL NOT allow callers to recover or
forge its `DimRef`, runtime key, anonymous grid witness, private static interpretation, stable reference-data handle, or
catalog lineage. Downstream reference-data and codec layers SHALL perform their additional identity checks before
delegating carrier construction to the quantity roots.

#### Scenario: Transform a decoded grid value
- **WHEN** checked runtime construction returns an exact or grid quantity and generic code performs same-index arithmetic
  on it
- **THEN** the arithmetic requires no static dimension or equivalence capability

#### Scenario: Transform a heterogeneous exact result
- **WHEN** runtime arithmetic produces a value in a `Times`, `Inverse`, or `Divide` endpoint
- **THEN** its dependent package retains the corresponding authoritative expression-preserving `DimRef`

#### Scenario: Reject unchecked packed construction
- **WHEN** raw runtime input cannot establish the requested dimension and anonymous grid witness
- **THEN** reconstruction fails before attaching its coordinate to a typed grid carrier

#### Scenario: Keep runtime provenance non-extractable
- **WHEN** code possesses only the dimensional value stored inside a dependent runtime result
- **THEN** the value alone supplies no `DimRef`, `DimKey`, grid witness, reference-data identity, or catalog provenance

### Requirement: Runtime-resolved endpoints support authoritative rates
The public rate constructor SHALL accept authoritative `DimRef[From]` and `DimRef[To]` values and an exact coefficient,
and SHALL return `Rate[From, To]` without requiring statically visible endpoint decomposition or public normalization
evidence. It SHALL work for named static atoms, compound expression witnesses, reference-data path-dependent
dimensions, and fresh runtime dimensions. The constructed rate's runtime dimension key SHALL be exactly
`to.key / from.key`.

Applying a runtime-constructed rate to an existing `Quantity[From]` or canonically embedded
`GridQuantity[From, G]` SHALL return `Quantity[To]` directly. Endpoint composition, checked reciprocal, and cross-rate
operations SHALL retain their declared endpoint types. If independently obtained endpoint types denote equal runtime
keys but are distinct Scala types, the caller SHALL first recover `SameDimension` and explicitly align the relevant
value or endpoint; construction SHALL NOT install a global conversion.

The quantity runtime layer SHALL NOT infer domain metadata such as an asset, instrument base, quote, position,
settlement, or underlying identity from a symbol or another endpoint. A downstream adapter SHALL resolve and supply
every endpoint required by its domain model. This requirement SHALL add no asset, instrument, order, position, payoff,
or venue type to the quantity library.

#### Scenario: Construct a rate from registry dimensions
- **WHEN** authoritative path-dependent source and target dimension witnesses are supplied with an exact coefficient
- **THEN** the constructor returns `Rate[source.D, target.D]` with runtime key `target.key / source.key`

#### Scenario: Apply a runtime rate
- **WHEN** a runtime-constructed `Rate[source.D, target.D]` acts on an existing `Quantity[source.D]`
- **THEN** the result is exact `Quantity[target.D]` without static normalization evidence

#### Scenario: Compose known runtime conversions
- **WHEN** stable runtime rates share the same intermediate path-dependent endpoint type
- **THEN** composition returns a rate between the outer endpoint types directly

#### Scenario: Reconcile independently resolved equal endpoints
- **WHEN** two runtime endpoint witnesses have equal authoritative keys but distinct path-dependent types
- **THEN** checked runtime recovery plus explicit alignment permits their intended composition without a global implicit
  conversion

#### Scenario: Require the adapter to name endpoints
- **WHEN** a venue payload provides symbols, multipliers, or partial currency metadata
- **THEN** the quantity layer does not guess missing asset, position, or settlement endpoints and the adapter must
  resolve them under its own domain rules

## REMOVED Requirements

### Requirement: Runtime dimensions and assets
**Reason**: Runtime dimensions are mathematical quantity identity, while assets are reference data; keeping one requirement for both preserves the coupling this proposal removes.

**Migration**: Use the new domain-neutral runtime-dimension requirement for `DimKey`/`DimRef` and the `reference-data-identity` capability for assets.

### Requirement: Registered witnesses retain provenance
**Reason**: Registry-owned asset and stable-grid witnesses no longer belong to the quantity runtime layer.

**Migration**: Use opaque issuer provenance on `Asset`, `DimensionHandle`, and `GridHandle` in reference data.

### Requirement: Full grid identity is dimension-scoped
**Reason**: The rule remains valid but stable grid identity is now owned by reference data rather than quantity runtime.

**Migration**: Use the corresponding full-identity requirement in `reference-data-identity`.

### Requirement: Heterogeneous grid quantities recover evidence before arithmetic
**Reason**: The existing API combines stable catalog reconciliation, packed reconstruction, and mathematical arithmetic in one quantity-runtime service.

**Migration**: Reconcile immutable handles in reference data, then use the returned quantity evidence and ordinary typed arithmetic; future codecs own decoded dependent packages.

### Requirement: Registered grid quantities have unambiguous packed records
**Reason**: Packed stable-identity records are boundary-codec concerns and the existing logical records are explicitly not a production schema.

**Migration**: Use the separately proposed versioned boundary-codec capability when it is introduced.

### Requirement: Decoding verifies dimension before grid
**Reason**: The ordering is a codec/catalog-snapshot reconstruction invariant, not quantity runtime behavior.

**Migration**: Preserve dimension-first checked reconstruction in the future boundary-codec proposal.

### Requirement: Logical packing is not a wire schema
**Reason**: The quantity-owned logical packing API is removed instead of being promoted into a permanent schema.

**Migration**: Introduce an explicit schema version and decoder under the future boundary-codec capability.
