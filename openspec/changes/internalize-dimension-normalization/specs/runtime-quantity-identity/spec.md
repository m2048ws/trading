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
