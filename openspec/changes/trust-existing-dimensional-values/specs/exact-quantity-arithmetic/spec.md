## ADDED Requirements

### Requirement: Existing dimensional carriers have validated indices
For supported, well-typed Scala callers, every normally returned `Quantity[D]` and `GridQuantity[D, G]` SHALL have a
valid closed dimension index `D`. The invariant SHALL be established at public construction roots: coefficient-bearing
quantities require an authoritative `DimRef[D]`, nonzero grid coordinates require a matching `GridRef[D]`, type-only
zeros require `Normalize[D]`, checked alignment requires `SameDimension`, dimension-changing results require
normalization of their complete expression, and runtime reconstruction requires checked witness ownership.

Possessing a dimensional value SHALL NOT materialize or permit recovery of `Normalize[D]`, `DimRef[D]`,
`DimensionKey`, `GridRef[D, G]`, or registered provenance. It SHALL only allow operations that preserve its already
validated dimension index to construct further values at that same index without requesting `Normalize[D]` again.
Refined wrappers over an existing dimensional value SHALL inherit the same dimension-index invariant.

Operation-local rejection of a hypothetical malformed carrier type SHALL NOT be required. A method body that accepts an
otherwise unobtainable `Quantity[Bad]` or `GridQuantity[Bad, G]` parameter MAY type-check for index-preserving
transformations, but supported public APIs SHALL provide no normally returning construction path for such an argument.
The ordinary supported-caller exclusions for casts, reflection, unsafe bytecode, and constructor-bypassing
deserialization remain unchanged; cast-free `null` SHALL NOT inhabit either opaque carrier.

#### Scenario: Reduce existing generic quantities
- **WHEN** generic code receives a nonempty collection of `Quantity[D]` values and combines them with homogeneous
  addition
- **THEN** it requires no `Normalize[D]`, `DimRef[D]`, or dimensional-equivalence evidence

#### Scenario: Transform one existing quantity
- **WHEN** generic code scales or exact-divides an existing `Quantity[D]` while preserving `D`
- **THEN** the result remains `Quantity[D]` without contextual dimension evidence

#### Scenario: Transform existing refined values
- **WHEN** generic code combines or otherwise transforms existing refined quantity or grid values without changing their
  dimension index
- **THEN** refinement closure and result construction require no `Normalize[D]`

#### Scenario: Keep value trust non-extractable
- **WHEN** generic code possesses `Quantity[D]` or `GridQuantity[D, G]`
- **THEN** it cannot summon or recover `Normalize[D]`, `DimRef[D]`, a runtime key, a grid witness, or registry provenance
  from that value

#### Scenario: Reject malformed carrier construction
- **WHEN** supported code selects a zero-power or otherwise malformed `D` and attempts raw construction, type-only zero,
  witness-backed construction, alignment from a valid carrier, or checked decoding
- **THEN** no normally returning `Quantity[D]` or `GridQuantity[D, G]` is produced

#### Scenario: Permit an uncallable hypothetical transformation
- **WHEN** a method declares a `Quantity[Bad]` parameter and its body performs only index-preserving arithmetic
- **THEN** the body MAY type-check even though supported code cannot construct an argument that calls it normally

#### Scenario: Reject null carrier inhabitation
- **WHEN** supported Scala assigns literal `null` to `Quantity[D]` or `GridQuantity[D, G]` without a cast
- **THEN** compilation fails at the opaque carrier boundary

## MODIFIED Requirements

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept only quantities with the exact same Scala dimension type `D`, SHALL return
`Quantity[D]`, and SHALL require neither `Normalize[D]` nor `SameDimension`. Multiplication by `Rational` and exact
division by a nonzero whole scalar SHALL preserve `D` without normalization. Multiplying `Quantity[A]` by
`Quantity[B]` SHALL use the single normalization operation and return an exact quantity in a canonical `Dim`: nested
products SHALL be flattened, inverse powers negated, equal singleton keys combined, zero powers removed, and every
surviving key stored exactly once with a nonzero `Int` exponent. Entry order MAY follow operand order and SHALL NOT
affect dimension equivalence.

For fully concrete inputs, the inferred public result SHALL expose the complete canonical dimension without a
specialized product evidence type or caller-visible alignment step. Generic code SHALL state and forward one contextual
`Normalize` computation for the complete multiplication, inversion, quotient, or endpoint-cancellation expression when
its result dimension is unresolved. Instantiating such generic code with concrete dimensions SHALL agree with
normalizing the corresponding concrete expression directly. Hidden decompositions of runtime-resolved opaque
dimensions SHALL remain unavailable to static cancellation until checked runtime equivalence is supplied.

Every operation that preserves the dimension index of one or more existing trusted values SHALL rely on that carrier
invariant and SHALL NOT request `Normalize[D]`. This includes homogeneous exact and grid addition and subtraction,
scalar multiplication and exact scalar division, allocation and quantization, arithmetic grid projection,
refinement-preserving wrappers, and combine-only algebra. An operation that manufactures a dimensional value from the
type alone SHALL require `Normalize[D]`. An operation that computes a new type-level dimension SHALL require
`Normalize` for the complete result expression, rather than redundant operand normalization. Explicit alignment,
equivalence-aware comparison, equality, ordering, sign inspection, authoritative witness-owned construction, and
runtime recovery SHALL remain governed by their own documented boundaries.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values without contextual dimension evidence

#### Scenario: Write generic homogeneous arithmetic
- **WHEN** a generic operation accepts two `Quantity[D]` operands
- **THEN** it can add or subtract them without declaring normalization or dimensional-equivalence vocabulary

#### Scenario: Reject implicit cross-spelling arithmetic
- **WHEN** `Quantity[A]` and `Quantity[B]` have different static dimension types even though `SameDimension[A, B]` is
  available
- **THEN** direct addition and subtraction do not compile, and the caller must explicitly align one operand to the
  chosen result type

#### Scenario: Preserve source-expression dimensions
- **WHEN** an index-preserving operation is applied to an existing `Quantity[Divide[T, F]]` or generic `Quantity[D]`
- **THEN** it compiles and retains the original dimension spelling without `Normalize[D]` or `Normalize.Aux[D, D]`

#### Scenario: Keep zero construction explicit
- **WHEN** generic code needs `Quantity.zero[D]` without an existing quantity operand
- **THEN** it must provide `Normalize[D]`

#### Scenario: Multiply concrete dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the exact result exposes their validated canonical product directly

#### Scenario: Multiply dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the exact result's public dimension is their validated canonical `Dim` product

#### Scenario: Cancel a price denominator
- **WHEN** `Quantity[Position]` is multiplied by a quantity in `Settlement / Position`
- **THEN** the result is directly assignable to `Quantity[Settlement]` without special rate application or alignment

#### Scenario: Retain a multi-key product
- **WHEN** multiplication leaves nonzero powers for more than one singleton key
- **THEN** the result contains one canonical entry for each surviving key and no reducible expression history

#### Scenario: Retain a multi-atom product
- **WHEN** multiplication leaves nonzero powers for more than one singleton-key atom
- **THEN** the result type contains one canonical entry for each surviving atom and no reducible multiplication or
  inversion history

#### Scenario: Preserve commutative equivalence
- **WHEN** quantities in dimensions `A` and `B` are multiplied in opposite operand orders
- **THEN** their result dimensions admit compile-time `SameDimension` evidence

#### Scenario: Use one generic normalization context
- **WHEN** a generic function multiplies, dimensionally divides, or otherwise computes a result dimension from abstract
  inputs
- **THEN** its signature requires only the corresponding complete `Normalize` evidence and forwards that evidence to the
  operation

#### Scenario: Specialize generic multiplication to one dimension
- **WHEN** a generic multiplication operation with contextual `Normalize` evidence is instantiated with both dimension
  parameters equal to `D`
- **THEN** its result contains one `D` key with exponent `2` and agrees with direct concrete multiplication in `D`

#### Scenario: Specialize generic inversion to a visible product
- **WHEN** a generic inversion operation with contextual `Normalize` evidence is instantiated with a concrete product
  `Times[A, B]`
- **THEN** its result flattens to canonical powers `A` to `-1` and `B` to `-1`, matching direct inversion

#### Scenario: Reject late alias specialization without contextual evidence
- **WHEN** a generic method requests normalization for aliases of unresolved dimensions before a caller later supplies
  equal concrete arguments
- **THEN** automatic derivation is rejected at the generic definition and the method must accept the complete contextual
  `Normalize` evidence

#### Scenario: Reject refinable-member inversion without contextual evidence
- **WHEN** inversion is requested for an abstract member rooted in a parameter or refinable prefix
- **THEN** automatic derivation rejects the unresolved structure instead of freezing it as one singleton key, and generic
  code must accept contextual `Normalize` evidence

#### Scenario: Reject local aliases over dependent parameters
- **WHEN** generic dimension-changing arithmetic defines aliases over parameter-dependent dimension members before
  requesting normalization
- **THEN** the aliases do not hide the unresolved roots and contextual `Normalize` evidence remains required

#### Scenario: Reject stable local transport of unresolved evidence
- **WHEN** generic normalization evidence or a parameter-dependent witness is rebound through stable locals, singleton
  ascriptions, or `Normalize.Aux` refinements while dependencies remain unresolved
- **THEN** rebinding does not permit automatic derivation or an invalid duplicate, uncancelled, or nested representation

#### Scenario: Preserve endpoint-depth coherence
- **WHEN** transparent aliases or exact `Normalize.Aux` refinements successively expose a fully concrete operation output
- **THEN** every use reaches the same canonical `Dim` and runtime `DimensionKey` as direct use of the concrete endpoint

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** normalization produces the same canonical output for `holder.D` and `Times[A, B]`, including duplicate-key
  combination, cancellation, inversion, and runtime-key agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, canonical `Dim`, reducible expression, or transparent alias differs from another input only by
  annotations
- **THEN** normalization produces the same unannotated canonical output and agrees with runtime `DimensionKey`
  multiplication and inversion

#### Scenario: Keep runtime-hidden structure opaque
- **WHEN** an opaque runtime dimension's key contains a factor that would cancel a separate static atom
- **THEN** automatic static normalization does not inspect the hidden key, and cancellation requires checked runtime
  equivalence

### Requirement: Exact-only algebra
The optional algebra layer SHALL expose one coherent strongest-instance hierarchy backed by primitive exact
operations. `Rational` SHALL be a commutative ring. The one production `ExactScalarField[Rational]` SHALL extend and
supply that standard commutative-ring structure while adding reciprocal that accepts `NonZero[Rational]`; checked
raw-scalar reciprocal MAY return `Either[ExpectedNonZero.type, Rational]`. It SHALL NOT expose Algebra
`Field[Rational]` or floating scalar construction.

`LeftModule[V, S]` SHALL extend the additive commutative group of `V`, and `VectorSpace[V, F]` SHALL extend
`LeftModule[V, F]`. One production `VectorSpace[Quantity[D], Rational]` SHALL therefore supply the quantity's rational
vector-space, left-module, and additive-commutative-group structures. Quantity multiplication SHALL remain graded by
the dimension group rather than supplying a same-carrier ring. The implementation SHALL NOT expose
`Numeric[Quantity[D]]` or `Ring[Quantity[D]]`, and SHALL NOT reconstruct grid coordinates through the exact-quantity
vector space.

`NonZero[Rational]` SHALL expose a multiplicative commutative group whose identity, multiplication, and reciprocal are
constructed through operation-specific lexical closure without predicate revalidation. Exact total orders SHALL be
available for `Rational` and `Quantity[D]` and SHALL delegate to primitive rational comparison. These orders SHALL NOT
replace or influence the closed `Sign[A]` refinement authority.

Where closed addition is part of the supported public structure, `NonNegative[Quantity[D]]` SHALL expose an additive
commutative monoid and `Positive[Quantity[D]]` SHALL expose an additive commutative semigroup. Zero SHALL belong only
to the nonnegative structure. `NonZero[A]` SHALL NOT receive an additive structure because two nonzero values may sum
to zero. Primitive arithmetic SHALL remain independently usable and SHALL NOT summon an instance that delegates back
to that primitive.

Every algebra instance that supplies a dimension-typed identity SHALL require the authority needed to manufacture that
identity. The quantity vector space, grid module, and nonnegative quantity and grid monoids SHALL therefore retain
`Normalize[D]`. An algebra instance that only combines existing trusted dimensional values SHALL not require
normalization; positive quantity and grid additive semigroups SHALL be available for abstract `D` without contextual
dimension evidence. No competing quantity or grid semigroup instance SHALL be introduced alongside the coherent
strongest identity-bearing instance merely to bypass its construction requirement.

#### Scenario: Import exact quantity algebra
- **WHEN** a caller imports `trading.quantity.algebra.exactQuantityAlgebra.given` with `Normalize[D]`
- **THEN** exact vector-space operations and their manufactured zero are available without floating scalar construction

#### Scenario: Reuse the strongest quantity structure
- **WHEN** a caller requests a left module or additive commutative group after importing exact quantity algebra
- **THEN** the production vector-space instance supplies that weaker structure without a competing group instance

#### Scenario: Require authority for an algebraic identity
- **WHEN** generic code requests a quantity vector space, grid module, or nonnegative dimensional monoid for abstract `D`
- **THEN** it must provide `Normalize[D]` because the structure can manufacture zero without an existing value

#### Scenario: Combine positive values without normalization
- **WHEN** generic code requests the additive semigroup for positive quantity or grid values in abstract `D`
- **THEN** the instance is available without `Normalize[D]` because every operation consumes existing trusted values

#### Scenario: Reciprocal is total after evidence
- **WHEN** `ExactScalarField[Rational].reciprocal` receives `NonZero[Rational]`
- **THEN** it returns the exact reciprocal without a division-by-zero branch

#### Scenario: Multiply nonzero rationals
- **WHEN** two `NonZero[Rational]` values are multiplied or one is reciprocated
- **THEN** the result remains nonzero without rerunning the predicate

#### Scenario: Preserve graded quantity multiplication
- **WHEN** quantities are multiplied associatively or distributively across dimension expressions
- **THEN** coefficients obey the commutative graded-algebra laws and canonical dimension equality reconciles expression
  shape

#### Scenario: Imports do not change arithmetic meaning
- **WHEN** algebra or rounding-policy values are imported
- **THEN** existing quantity operations keep the same result types and numerical semantics
