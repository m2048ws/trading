## ADDED Requirements

### Requirement: Runtime-resolved endpoints support authoritative rates
The public rate constructor SHALL accept authoritative `DimRef[From]` and `DimRef[To]` values and an exact coefficient,
and SHALL return `Rate[From, To]` without requiring statically visible endpoint decomposition or public normalization
evidence. It SHALL work for named static atoms, compound expression witnesses, registry-resolved path-dependent
dimensions, and fresh runtime dimensions. The constructed rate's runtime dimension key SHALL be exactly
`to.key / from.key`.

Applying a runtime-constructed rate to an existing `Quantity[From]` or canonically embedded
`GridQuantity[From, G]` SHALL return `Quantity[To]` directly. Endpoint composition, checked reciprocal, and cross-rate
operations SHALL retain their declared endpoint types. If independently resolved endpoint types denote equal runtime
keys but are distinct Scala types, the caller SHALL first recover `SameDimension` and explicitly align the relevant
value or endpoint; construction SHALL NOT install a global conversion.

The quantity runtime layer SHALL NOT infer domain metadata such as an instrument's base, quote, position, settlement,
or underlying currency from a symbol or from another endpoint. Runtime adapters SHALL resolve and supply every endpoint
required by their domain model. This requirement SHALL add no instrument, order, position, payoff, or venue type to the
quantity library.

#### Scenario: Construct a rate from registry dimensions
- **WHEN** a registry resolves stable path-dependent source and target dimension witnesses and the caller supplies an
  exact coefficient
- **THEN** the constructor returns `Rate[source.D, target.D]` with runtime key `target.key / source.key`

#### Scenario: Apply a runtime rate
- **WHEN** a runtime-constructed `Rate[source.D, target.D]` acts on an existing `Quantity[source.D]`
- **THEN** the result is exact `Quantity[target.D]` without static normalization evidence

#### Scenario: Compose known runtime conversions
- **WHEN** stable runtime rates share the same intermediate path-dependent endpoint type
- **THEN** composition returns a rate between the outer endpoint types directly

#### Scenario: Reconcile independently resolved equal endpoints
- **WHEN** two stable runtime endpoint witnesses have equal authoritative keys but distinct path-dependent types
- **THEN** checked runtime recovery plus explicit alignment permits their intended composition without a global implicit
  conversion

#### Scenario: Require the adapter to name endpoints
- **WHEN** a venue payload provides symbols, multipliers, or partial currency metadata
- **THEN** the quantity layer does not guess missing position or settlement endpoints and the adapter must resolve them
  under its own domain rules

## MODIFIED Requirements

### Requirement: Runtime dimensions and assets
The runtime layer SHALL create opaque dimension witnesses for runtime identifiers and SHALL interpret compound
dimensions through canonical `DimensionKey` values. `AssetRef` SHALL bind an asset identity to its canonical dimension
without imposing a grid. `DimensionKey` SHALL remain the runtime free abelian group of dimensions and SHALL expose
multiplicative identity, multiplication, inverse, and canonical equality through an opt-in production algebra
instance. Those operations SHALL delegate to primitive arbitrary-precision key arithmetic. Algebraic equality SHALL NOT
replace registry identity, registry ownership, registered provenance, or full grid identity.

`DimRef[D]` SHALL remain authoritative for the runtime identity inhabited by `D`. Its public algebra SHALL preserve
static expressions: identity returns `DimRef[One]`, product returns `DimRef[Times[A, B]]`, inverse returns
`DimRef[Inverse[A]]`, and quotient returns `DimRef[Divide[A, B]]`. Each operation SHALL compute the exactly
corresponding canonical runtime key and SHALL require no public canonical-output capability. Publicly inhabitable atom
types SHALL retain their one-to-one static/runtime authority; private static interpretation SHALL neither totalize
`DimRef` over all accepted keys nor manufacture a runtime witness.

#### Scenario: Register an asset
- **WHEN** an asset definition is registered
- **THEN** the registry returns an asset witness with a canonical path-dependent dimension type

#### Scenario: Canonicalize a compound runtime key
- **WHEN** runtime dimension multiplication and inversion cancel factors
- **THEN** the resulting canonical `DimensionKey` is equal regardless of expression shape

#### Scenario: Normalize a compound dimension
- **WHEN** runtime dimension multiplication and inversion cancel factors
- **THEN** the resulting canonical `DimensionKey` is equal regardless of the expression-preserving static shape

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

#### Scenario: Do not totalize privately accepted keys
- **WHEN** private static interpretation accepts a concrete stable key for equivalence but no public atom authority owns
  that key
- **THEN** no `DimRef[Atom[K]]` is inferred or constructed

#### Scenario: Algebraic equality does not confer authority
- **WHEN** two canonical keys or numerical grid definitions compare equal
- **THEN** registered operations still require witnesses created by the owning `QuantityRegistry`

### Requirement: Checked runtime evidence
`SameDimension[A, B]` SHALL remain one restricted capability for explicit alignment and equivalence-aware comparison,
whether it was derived by private interpretation of statically visible expressions or recovered from authoritative
runtime witnesses. Runtime recovery SHALL issue `SameDimension` only after canonical `DimensionKey` equality and, for
registered witnesses, shared registry ownership. It SHALL expose controlled quantity and grid alignment through
`alignTo`; it SHALL NOT expose unrestricted Scala type equality, synthesize `DimRef`, install a global implicit
conversion, or align different static types inside homogeneous arithmetic.

`trading.quantity.grid.SameGrid` SHALL remain generic mathematical grid-identity evidence: it MAY be recovered for
matching generative `GridRef` values without registered provenance, and SHALL check canonical runtime dimension, grid
ID, version, and quantum compatibility. `RuntimeEvidence.sameGrid` SHALL remain the registry-aware operation. It SHALL
first verify that both registered witnesses share registry ownership and, only after that succeeds, SHALL perform the
ordinary `SameGrid` compatibility checks. Runtime evidence SHALL remain a scoped success value rather than an unchecked
claim derived from identifiers alone.

Reflexive `SameDimension[D, D]` SHALL remain structural Scala type identity and SHALL NOT certify static validity or
runtime inhabitation. Non-reflexive static derivation, authority-bearing construction, and checked runtime recovery
SHALL remain independent trust boundaries. Transparent Scala type annotations SHALL NOT alter static or runtime
dimension identity: accepted annotated inputs SHALL have the same private mathematical interpretation and
`DimensionKey` as their unannotated underlying dimensions.

#### Scenario: Align an exact quantity after runtime recovery
- **WHEN** two registry witnesses have checked-equal canonical dimensions
- **THEN** recovered `SameDimension` permits `alignTo` to change only the phantom dimension type of an exact quantity

#### Scenario: Align a grid quantity after runtime recovery
- **WHEN** checked dimension equality is available for a grid quantity
- **THEN** `alignTo` preserves its grid type and integer coordinate while selecting the target dimension type

#### Scenario: Coerce an exact quantity
- **WHEN** two registry witnesses have checked-equal canonical dimensions and recovery supplies
  `SameDimension[Source, Target]`
- **THEN** `Quantity[Source].alignTo[Target]` changes only the phantom dimension type and preserves the exact coefficient;
  no separate low-level coercion operation is exposed

#### Scenario: Coerce a grid quantity
- **WHEN** checked dimension equality supplies `SameDimension[Source, Target]` for a grid quantity
- **THEN** `GridQuantity[Source, G].alignTo[Target]` preserves its grid type and coordinate while changing only the
  phantom dimension type; no separate low-level coercion operation is exposed

#### Scenario: Consume static and runtime evidence uniformly
- **WHEN** a caller receives `SameDimension[A, B]` from static derivation or successful runtime recovery
- **THEN** the same explicit `alignTo` operation accepts it, after which homogeneous arithmetic uses one exact static
  dimension type

#### Scenario: Reject direct evidence-driven arithmetic
- **WHEN** runtime recovery supplies `SameDimension[A, B]` for distinct static dimension types
- **THEN** addition and subtraction still reject mixed operands until the caller explicitly aligns one value

#### Scenario: Reject runtime dimension mismatch
- **WHEN** independently resolved witnesses have different canonical dimension keys
- **THEN** runtime recovery returns an explicit mismatch and issues no `SameDimension`

#### Scenario: Keep reflexivity separate from authority
- **WHEN** a malformed `Dim` obtains reflexive `SameDimension[D, D]`
- **THEN** that evidence creates neither a `DimRef[D]` nor a quantity or grid value at `D`

#### Scenario: Recover generic evidence for generative grids
- **WHEN** two generative `GridRef` values have matching canonical dimension, grid ID, version, and quantum
- **THEN** `SameGrid.between` can recover mathematical grid-identity evidence without registered provenance

#### Scenario: Check registry ownership before grid compatibility
- **WHEN** `RuntimeEvidence.sameGrid` compares equal-looking registered grids owned by different registries
- **THEN** it returns a foreign-registry failure before ordinary grid compatibility checks

#### Scenario: Ignore transparent annotations in runtime identity
- **WHEN** an accepted static expression uses an annotated atom, expression, or transparent alias
- **THEN** its authoritative runtime key equals the key produced from the corresponding unannotated operands

### Requirement: Heterogeneous grid quantities recover evidence before arithmetic
Heterogeneous registered values SHALL be represented as `ResolvedAssetGridQuantity` or `ResolvedGridQuantity`.
Same-grid arithmetic SHALL recover checked grid evidence, retype one trusted coordinate to the selected grid type, and
then invoke exact-type grid arithmetic without a static validity capability. Exact cross-grid arithmetic SHALL recover
checked dimension evidence, explicitly embed both operands, align one embedded quantity to the selected result
dimension, and then invoke exact-type homogeneous arithmetic. It SHALL return `ResolvedExactQuantity` containing
`Quantity[D]` indexed by the selected authoritative runtime witness.

Dimension-changing heterogeneous operations SHALL preserve raw static expressions unless they invoke a documented
endpoint operation such as rate application. A runtime result package SHALL retain the authoritative witness needed to
interpret its path-dependent result type; it SHALL not expose public normalization evidence.

#### Scenario: Add heterogeneous same-grid values
- **WHEN** two resolved grid quantities carry the same registered grid identity
- **THEN** checked grid recovery permits their exact coordinate sum to be returned as `ResolvedGridQuantity`

#### Scenario: Add heterogeneous distinct-grid values
- **WHEN** two resolved quantities share a runtime dimension but have different grids and static dimension types
- **THEN** checked recovery explicitly aligns one exact embedding before exact-type addition returns
  `ResolvedExactQuantity`

#### Scenario: Multiply heterogeneous values
- **WHEN** checked runtime logic multiplies trusted exact values in path-dependent dimensions `A` and `B`
- **THEN** the typed result preserves `Times[A, B]` together with the authoritative runtime product witness

#### Scenario: Apply a heterogeneous rate
- **WHEN** a resolved value and runtime-constructed rate share the same source endpoint type
- **THEN** rate application returns a resolved exact value at the declared target endpoint type

#### Scenario: Reject heterogeneous dimension mismatch
- **WHEN** resolved USD and BTC grid quantities are added without a rate
- **THEN** the operation returns a heterogeneous dimension error without attempting alignment or arithmetic

### Requirement: Public DimRef atom authority is unique
`DimRef[D]` SHALL be the authoritative public association between an inhabited static dimension type `D` and its
runtime `DimensionKey`. For every singleton key `K` whose `Atom[K]` is inhabitable through supported public `DimRef`
APIs, any two publicly obtained values of type `DimRef[Atom[K]]` SHALL have equal `DimensionKey` values. This uniqueness
requirement SHALL apply only to publicly inhabitable atom types. The set of keys accepted by library-private static
interpretation MAY be larger, and private acceptance alone SHALL NOT make an atom type runtime-inhabitable.

Public atom construction SHALL bind static and runtime identity at one authority-bearing boundary. Literal construction
SHALL derive the runtime atom identifier from the accepted literal singleton. Nominal construction SHALL bind the
result to the supplied stable object's exact singleton type and to the runtime identifier owned by that object.
Generative and fresh runtime witnesses SHALL bind their path-dependent atom type to the identity captured by that same
witness. No supported public constructor SHALL accept a caller-selected static atom type independently from a
caller-selected runtime identity.

The public `DimRef` identity witness SHALL bind `One` to `DimensionKey.one`. Product, inverse, and quotient operations
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
  exactly matching runtime `DimensionKey` operation result

#### Scenario: Reject downstream witness forgery
- **WHEN** supported downstream source attempts to implement `DimRef[D]` or invoke an unbound static/runtime constructor
- **THEN** construction is unavailable and no contradictory runtime identity can inhabit the chosen static type

### Requirement: Checked runtime reconstruction preserves carrier trust
Registry adoption, checked logical decoding, and heterogeneous result construction SHALL return dimensional carriers
only through registry-owned dimension and grid witnesses. Every normally returned `Quantity[D]` or
`GridQuantity[D, G]` inside a resolved runtime carrier SHALL therefore have a valid dimension index and MAY undergo
index-preserving transformations without a static-dimension capability. A dimension-changing heterogeneous result
SHALL retain the matching expression-preserving `DimRef` produced from its authoritative inputs.

Raw packed identities, coordinates, and caller-selected type arguments SHALL NOT become dimensional values without the
existing dimension-first registry checks. Possessing a reconstructed value SHALL NOT allow callers to recover or forge
its registered witness, registry owner, `DimRef`, runtime key, grid provenance, or private static interpretation; those
capabilities remain available only from the resolved dependent package and registry APIs.

#### Scenario: Transform a decoded grid value
- **WHEN** a checked packed record is decoded to a resolved grid quantity and generic code performs same-index grid
  arithmetic on its value
- **THEN** the arithmetic requires no static dimension or equivalence capability

#### Scenario: Transform a heterogeneous exact result
- **WHEN** checked heterogeneous arithmetic returns `ResolvedExactQuantity`
- **THEN** its dependent `Quantity[result.dimension.D]` can undergo index-preserving arithmetic without a static
  dimension capability

#### Scenario: Reject unchecked packed construction
- **WHEN** raw packed data names an unknown, mismatched, foreign, or conflicting dimension or grid identity
- **THEN** decoding fails before attaching its coordinate to a typed grid carrier

#### Scenario: Keep runtime provenance non-extractable
- **WHEN** code possesses only the dimensional value stored inside a resolved runtime result
- **THEN** the value alone does not supply registry ownership, registered grid identity, `DimRef`, `DimensionKey`, or
  private static-interpretation authority
