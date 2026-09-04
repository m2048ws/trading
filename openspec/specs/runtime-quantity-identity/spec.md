# runtime-quantity-identity Specification

## Purpose

Defines domain-neutral runtime dimension identity and checked mathematical evidence without asset, stable-grid,
registry, or packed-record ownership.

## Requirements

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

### Requirement: Public DimRef atom authority is unique
`DimRef[D]` SHALL be the authoritative public association between an inhabited static dimension type `D` and its
runtime `DimKey`. For every singleton key `K` whose `Atom[K]` is inhabitable through supported public `DimRef`
APIs, any two publicly obtained values of type `DimRef[Atom[K]]` SHALL have equal `DimKey` values. This uniqueness
requirement SHALL apply only to publicly inhabitable atom types. The set of keys accepted by library-private static
interpretation MAY be larger, and private acceptance alone SHALL NOT make an atom type runtime-inhabitable.

Public atom construction SHALL bind static and runtime identity at one authority-bearing boundary. Literal construction
SHALL derive the runtime atom identifier from the accepted literal singleton. Nominal construction SHALL bind the
result to the supplied stable object's exact singleton type and to the runtime identifier owned by that object.
Generative and fresh runtime witnesses SHALL bind their path-dependent atom type to the identity captured by that same
witness. No supported public constructor SHALL accept a caller-selected static atom type independently from a
caller-selected runtime identity.

The public `DimRef` identity witness SHALL bind `One` to `DimKey.one`. Product, inverse, and quotient operations
SHALL preserve the static/runtime association inductively: each SHALL return `DimRef[Times[A, B]]`,
`DimRef[Inverse[A]]`, or `DimRef[Divide[A, B]]` and the exactly corresponding runtime key produced from its
authoritative input keys. Witness algebra SHALL NOT expose a separately selected canonical output. Supported downstream
code SHALL NOT directly construct or implement `DimRef` to bypass these roots and operations.

#### Scenario: Repeat literal construction
- **WHEN** supported callers construct `DimRef.atom["BTC"]` more than once
- **THEN** every result has type `DimRef[Atom["BTC"]]` and the same runtime dimension key derived from `"BTC"`

#### Scenario: Repeat nominal construction
- **WHEN** a stable `NominalAtom` object is supplied to `DimRef.atom` more than once
- **THEN** every result retains that object's exact singleton atom type and the same object-owned runtime identity

#### Scenario: Reject caller-selected literal widening
- **WHEN** a caller supplies different `ValueOf[String & Singleton]` values and requests the same widened
  `DimRef[Atom[String & Singleton]]` type
- **THEN** construction is rejected by the concrete-key authority gate before either value can inhabit that widened
  static atom type

#### Scenario: Reject caller-selected nominal widening
- **WHEN** distinct nominal objects are widened to a shared nominal singleton supertype before construction
- **THEN** the results cannot both inhabit one caller-selected `DimRef[Atom[K]]` type and retain their distinct runtime
  identities

#### Scenario: Preserve generative authority
- **WHEN** a generative or fresh runtime witness exposes its dimension repeatedly
- **THEN** that witness's exact path-dependent atom type always denotes the runtime identity captured by that witness,
  while a different witness has a distinct path-dependent atom type

#### Scenario: Do not totalize privately accepted keys
- **WHEN** private static interpretation accepts a concrete stable `Atom[K]` key outside the supported public `DimRef`
  authority sources
- **THEN** no `DimRef[Atom[K]]` constructor or runtime key is inferred from private interpretation alone

#### Scenario: Do not totalize normalized keys
- **WHEN** a concrete stable `Atom[K]` key is accepted by library-private static interpretation but lies outside the
  supported public `DimRef` authority sources
- **THEN** no `DimRef[Atom[K]]` constructor or runtime key is inferred from that private interpretation

#### Scenario: Preserve authority through witness algebra
- **WHEN** public `DimRef` product, inverse, or quotient combines authoritative input witnesses
- **THEN** the returned witness preserves the corresponding `Times`, `Inverse`, or `Divide` expression type and has the
  exactly matching runtime `DimKey` operation result

#### Scenario: Reject downstream witness forgery
- **WHEN** supported downstream source attempts to implement `DimRef[D]` or invoke an unbound static/runtime constructor
- **THEN** construction is unavailable and no contradictory runtime identity can inhabit the chosen static type

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

### Requirement: Runtime witness authority is checked at supported roots

Runtime dimension witnesses SHALL preserve the one-to-one static/runtime association established by supported literal,
nominal, generative, fresh, algebraic, or checked-recovery roots. Raw keys and caller-selected type arguments SHALL NOT
form a documented unchecked witness-construction path. The supported domain source API SHALL be Scala 3; Java-library
interoperation and checked external reconstruction MAY occur behind an owning Scala boundary without creating an
ordinary-Java domain API promise. An already established positive grid quantum SHALL be trusted as refined input by
runtime grid construction, while a raw external quantum SHALL pass through the owning positive refinement before any
runtime grid witness is returned. This semantic guarantee SHALL NOT claim to defend against deliberate reflection,
unsafe bytecode, casts, or constructor bypass by code already running in the JVM.

#### Scenario: Recover a runtime dimension

- **WHEN** a checked boundary resolves a runtime key through an authoritative witness root
- **THEN** the dependent result retains the matching static type and exact runtime key

#### Scenario: Supply inconsistent external identity

- **WHEN** decoded data cannot establish agreement among its runtime key, dimension, grid, and catalog context
- **THEN** checked reconstruction returns a typed failure and no trusted carrier

#### Scenario: Use only supported in-process APIs

- **WHEN** supported Scala 3 code uses the documented runtime-witness operations
- **THEN** it cannot independently select contradictory static and runtime identities

#### Scenario: Refine a raw grid quantum before witness construction

- **WHEN** an external representation supplies a raw rational quantum
- **THEN** the owning positive refinement rejects nonpositive input before direct runtime grid construction receives
  an established `PositiveRational`
