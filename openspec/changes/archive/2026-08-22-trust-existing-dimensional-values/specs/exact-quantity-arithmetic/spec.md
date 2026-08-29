## ADDED Requirements

### Requirement: Existing dimensional carriers have validated indices
For supported, well-typed Scala callers, every normally returned `Quantity[D]` and `GridQuantity[D, G]` SHALL have a
valid closed dimension index `D`. The invariant SHALL be established at public construction roots: coefficient-bearing
quantities require an authoritative `DimRef[D]`, nonzero grid coordinates require a matching `GridRef[D]`, type-only
zeros require `Normalize[D]`, checked alignment requires `SameDimension`, dimension-changing results require
normalization of their complete expression, and runtime reconstruction requires checked witness ownership.

Possessing a dimensional value SHALL NOT materialize or permit recovery of `Normalize[D]`, `DimRef[D]`,
`DimKey`, `GridRef[D, G]`, or registered provenance. It SHALL only allow operations that preserve its already
validated dimension index to construct further values at that same index without requesting `Normalize[D]` again.
Refined wrappers over an existing dimensional value SHALL inherit the same dimension-index invariant.

Operation-local rejection of a hypothetical malformed carrier type SHALL NOT be required. A method body that accepts an
otherwise unobtainable `Quantity[Bad]` or `GridQuantity[Bad, G]` parameter MAY type-check for index-preserving
transformations, but supported public APIs SHALL provide no normally returning construction path for such an argument.
The ordinary supported-caller exclusions for casts, reflection, unsafe bytecode, and constructor-bypassing
deserialization remain unchanged; cast-free `null` SHALL NOT inhabit either opaque carrier. Literal `null` supplied as
reference-valued construction authority SHALL fail at the public construction root before a witness or dimensional
carrier is returned. In particular, every retained `Normalize`-authorized root SHALL reject null evidence before
returning a zero, computed carrier, `DimRef`, rate, or identity-bearing algebra capability. A typed null `Rational`
coefficient or `BigInt` coordinate SHALL likewise be rejected at the shared coefficient or coordinate construction
boundary before any carrier is returned; checked grid decoding SHALL reconstruct through the same guarded coordinate
boundary.

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

#### Scenario: Reject null numeric carrier payloads
- **WHEN** supported Scala supplies a typed null `Rational` coefficient or `BigInt` coordinate to witness-backed
  construction, including a coordinate in otherwise valid packed data passed to checked decoding
- **THEN** the shared construction boundary terminates before returning a `Quantity`, `GridQuantity`, or resolved carrier

#### Scenario: Reject null normalization authority
- **WHEN** supported Scala explicitly supplies literal `null` as `Normalize[D]` or a refined `Normalize.Aux` to a
  retained type-only, computed-result, witness, rate, or identity-bearing algebra root
- **THEN** the root terminates before returning any dimensional carrier, witness, rate, or algebra capability

#### Scenario: Reject null grid-construction authority
- **WHEN** supported Scala supplies literal `null` as the `DimRef[D]` authority to uniform-grid construction
- **THEN** construction terminates before returning a `GridRef[D]` capable of attaching coordinates

#### Scenario: Reject null runtime-identity authority
- **WHEN** supported Scala supplies literal `null` as a `DimKey` atom or power component, a fresh key, an atomic or
  nominal atom ID, a grid identity component, or registry identity input
- **THEN** the public construction root terminates before returning a key, dimension or grid witness, registered
  identity, equivalence, or dimensional carrier

## MODIFIED Requirements

### Requirement: Compile-time dimension equivalence
`SameDimension[A, B]` SHALL be derivable at compile time when normalization of the statically visible closed dimension
expressions `A` and `B` produces canonical `Canonical` entries with the same singleton keys and `Int` exponents modulo tuple
permutation. Static derivation SHALL require no runtime `DimRef`, `DimKey`, or total ordering over singleton keys.
The evidence SHALL remain a restricted capability whose construction is unavailable to supported downstream code; it
SHALL authorize controlled explicit quantity- and grid-dimension alignment and equivalence-aware comparison but SHALL
NOT expose unrestricted Scala type equality, a global implicit conversion between arbitrary quantity types, or implicit
alignment inside homogeneous arithmetic.

The public value-level alignment operation SHALL be named `alignTo`. Given `SameDimension[D, E]`, it SHALL retag
`Quantity[D]` as `Quantity[E]` and `GridQuantity[D, G]` as `GridQuantity[E, G]` without changing the exact coefficient,
grid identity, or coordinate. The former `asDimension` operation SHALL no longer be exposed. Concrete arithmetic whose
canonical result is statically determined SHALL expose that result directly; callers SHALL not need a second alignment
evidence family or a routine `alignTo` repair merely to select the named atom, rate endpoint, ratio, or canonical
composite that the operation computes. Explicit `SameDimension`-checked alignment SHALL remain available for checked
runtime equality and for intentionally selecting between distinct but equivalent tuple orders. Reflexive
`SameDimension[D, D]` SHALL mean Scala type identity only; every non-reflexive proof SHALL validate the complete closed
representation. Every operation that computes a new type-level result dimension SHALL independently normalize the
complete result expression.

#### Scenario: Align commuted canonical dimensions
- **WHEN** two canonical `Canonical` values contain the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Align commuted products
- **WHEN** two concrete products normalize to the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Use equivalence in addition
- **WHEN** two quantities have equivalent canonical dimensions in different tuple orders but different Scala dimension
  types
- **THEN** direct addition and subtraction do not compile; the right operand must first use
  `SameDimension[Right, Left]` to `alignTo[Left]`, after which exact-type arithmetic returns `Quantity[Left]`

#### Scenario: Use evidence in addition
- **WHEN** a generic caller selects the left dimension as the result type for equivalent `Quantity[Left]` and
  `Quantity[Right]` operands
- **THEN** it forwards `SameDimension[Right, Left]` to `right.alignTo[Left]` before exact-type addition or subtraction,
  and the arithmetic operation itself does not consume `SameDimension`

#### Scenario: Require explicit alignment before addition
- **WHEN** two quantities have equivalent canonical dimensions represented by different Scala dimension types
- **THEN** direct addition and subtraction do not compile until one operand is explicitly aligned to the other operand's
  dimension type

#### Scenario: Align an exact quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `Quantity[D]`
- **THEN** the result is `Quantity[E]` with exactly the original coefficient

#### Scenario: Align a grid quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `GridQuantity[D, G]`
- **THEN** the result is `GridQuantity[E, G]` with the original grid type and coordinate

#### Scenario: Expose a concrete economic result directly
- **WHEN** concrete quantity or rate arithmetic cancels all intermediate source dimensions and leaves a named target
  dimension
- **THEN** the result is assignable to that target type without caller-supplied alignment or retagging

#### Scenario: Select an economic result type explicitly
- **WHEN** a caller intentionally selects an equivalent canonical composite whose tuple order differs from the computed
  result
- **THEN** `alignTo` exposes the selected type using `SameDimension` without runtime comparison or an unchecked public
  cast

#### Scenario: Reject unequal static dimensions
- **WHEN** normalized dimensions differ by a singleton key or exponent
- **THEN** `SameDimension` is not derivable and `alignTo` cannot cross between the dimension types

#### Scenario: Recover checked runtime equivalence
- **WHEN** two opaque runtime witnesses have equal authoritative `DimKey` values but distinct singleton-key types
- **THEN** successful runtime comparison may issue scoped `SameDimension` evidence for explicit `alignTo`

#### Scenario: Derive evidence from downstream code
- **WHEN** supported downstream Scala requests `SameDimension` for equivalent commuted canonical products with compiler
  warnings treated as errors
- **THEN** the evidence compiles without inaccessible-member diagnostics or access to implementation-only proof rules

#### Scenario: Keep reflexivity separate from canonical certification
- **WHEN** a malformed `Canonical` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity and `alignTo[D]` MAY be available, but they do not certify `D` or provide a normally
  returned carrier; supported construction and complete-result computation reject the malformed representation, while
  an otherwise uncallable index-preserving transformation body MAY type-check

### Requirement: Atomic and canonical static derivation
The public static dimension language SHALL be closed over canonical `Canonical[Entries]`, source expressions `Times[A, B]` and
`Inverse[A]`, and `Divide[A, B]` as quotient syntax. A canonical entry SHALL be
`Power[Key, Exponent]`, where `Key <: Singleton` identifies an atom and `Exponent <: Int` is a singleton integer literal.
`Atom[Key]` SHALL denote `Canonical[Power[Key, 1] *: EmptyTuple]`, and `One` SHALL denote `Canonical[EmptyTuple]`. An arbitrary subtype
of `Dim` SHALL NOT silently become a new atomic identity.

A canonical `Canonical` SHALL contain each singleton key at most once and SHALL store only nonzero literal exponents. Tuple
order SHALL not carry mathematical meaning. A zero exponent, duplicate key, non-`Power` entry, abstract or nonliteral
exponent, unresolved tuple, or key that is not a stable singleton type SHALL make a claimed canonical representation
invalid. Floating-point, decimal, and rational exponent types SHALL not be part of the static dimension language.

`Key <: Singleton` SHALL be necessary but SHALL NOT by itself certify a canonical key. After annotations and transparent
aliases are exposed, automatic normalization SHALL accept only concrete literal `ConstantType` identities, concrete
stable term/module singleton references, and supported generative `ThisType` identities. It SHALL reject `Singleton`,
`Nothing`, `Null`, broad intersections and unions, refinements, bounds, unresolved match or lambda structures,
ordinary non-term `TypeRef` values, abstract/deferred/parameter keys, and unknown wrappers. Rejection of `Nothing` and
`Null` SHALL follow this structural whitelist rather than a permissive subtype test.

`Normalize[D]` SHALL be the sole public associated-output evidence for reducing a dimension expression to a canonical
`Canonical`. Its automatic derivation SHALL parse only the closed grammar, expose definitionally transparent aliases and
annotations, combine equal keys, remove zero results, validate the complete output, and then issue final evidence as one
trusted operation. Public `NormalizedPowers`, `DimensionProduct`, `DimensionInverse`, `DimensionQuotient`,
`DimensionAlignment`, recursive merge rules, guards, and caller-constructible proof tokens SHALL NOT be available.

Automatic derivation SHALL reject unresolved generic dimensions, refinements, intersections, unions, unresolved match
types, and other structures outside the closed grammar with an actionable diagnostic rather than treating them as
atoms. Generic code SHALL accept and forward the applicable final `Normalize` evidence when it manufactures a value
from a type alone or computes a new type-level dimension. A runtime-resolved opaque dimension witness SHALL expose a
concrete atom alias keyed by its own stable singleton identity; its hidden runtime decomposition SHALL not be guessed
statically. Transformations that preserve the index of an existing trusted carrier SHALL not require `Normalize[D]`.

A public literal atom constructor SHALL require `Normalize[Atom[K]]` in addition to `ValueOf[K]`, so a caller-created
`ValueOf[String & Singleton]` cannot select one broad static key for different literal values. A public nominal-object
atom constructor SHALL take one authority-bearing singleton key whose runtime `AtomId` is fixed by that key and SHALL
bind its result key to the supplied stable value's singleton type. It SHALL NOT accept a caller-selected key supertype or
the static singleton key and runtime identifier as independent arguments. At every public static atom construction
boundary, one accepted `Atom[K]` key type SHALL determine exactly one runtime atom identity. Opaque runtime witness
construction used by a registry SHALL be lexically owned or delegated through a safe generative witness;
package-qualified visibility alone SHALL NOT grant that authority to downstream code declaring the library package.

#### Scenario: Declare named atoms with singleton keys
- **WHEN** a caller declares `type USD = Atom["asset:USD"]` and obtains the corresponding authoritative `DimRef`
- **THEN** `USD` is the canonical one-power dimension for that singleton key

#### Scenario: Reject a widened literal constructor key
- **WHEN** a caller creates two legal `ValueOf[String & Singleton]` values containing different strings and explicitly
  requests `DimRef.atom[String & Singleton]`
- **THEN** `Normalize[Atom[String & Singleton]]` is not derivable and each invalid construction is rejected

#### Scenario: Bind nominal construction to the supplied stable identity
- **WHEN** two distinct `NominalAtom` objects are widened to a shared nominal singleton supertype
- **THEN** their constructor results retain distinct stable-value singleton key types and cannot both inhabit one
  caller-selected `DimRef[Atom[K]]` type

#### Scenario: Reject nonconcrete singleton keys
- **WHEN** normalization is requested for a canonical entry keyed by `Singleton`, `Nothing`, `Null`, a broad
  intersection, an abstract bound, or an ordinary non-term type reference
- **THEN** derivation fails with a concrete-stable-singleton diagnostic and no canonical evidence is issued

#### Scenario: Preserve supported concrete keys
- **WHEN** an atom key is a literal, nominal object, stable local/module value, generative atomic witness, fresh runtime
  witness, or transparent alias/annotation exposing to one of those identities
- **THEN** normalization succeeds without a caller manually constructing evidence

#### Scenario: Name an integer-powered canonical dimension
- **WHEN** a caller names `Canonical[Power["length", 2] *: Power["time", -1] *: EmptyTuple]`
- **THEN** normalization accepts the nonzero literal `Int` powers and preserves their exact mathematical values

#### Scenario: Reject malformed canonical entries
- **WHEN** a claimed `Canonical` contains a zero power, duplicate key, non-`Power` entry, abstract exponent, or unresolved tuple
- **THEN** normalization, supported public construction, and complete-result computation reject the representation;
  index-preserving transformations over an otherwise unobtainable carrier need not reject it again

#### Scenario: Reject fractional exponents
- **WHEN** a caller attempts to use a floating, decimal, or rational type as a `Power` exponent
- **THEN** the type does not satisfy the static exponent contract and compilation fails

#### Scenario: Normalize the closed expression grammar
- **WHEN** a concrete expression combines `Canonical`, `Times`, `Inverse`, `Divide`, `Atom`, `One`, transparent aliases, and
  transparent annotations
- **THEN** one `Normalize` derivation produces a validated, unannotated canonical `Canonical`

#### Scenario: Require one contextual operation in generic code
- **WHEN** generic code manufactures a value from an abstract `D <: Dim` or computes a new result dimension whose
  entries are not statically visible
- **THEN** it accepts and forwards the applicable `Normalize[D]` for type-only manufacture or one contextual `Normalize`
  for the complete result expression; preserving an existing carrier's index requires neither

#### Scenario: Reject the legacy proof surface
- **WHEN** supported downstream code names the removed signed-natural exponent types or specialized product, quotient,
  inverse, alignment, or normalized-powers evidence
- **THEN** those APIs are unavailable and the code must use literal `Int` powers and `Normalize`

#### Scenario: Preserve an opaque runtime dimension safely
- **WHEN** a runtime key is resolved without a statically visible decomposition
- **THEN** its witness carries a stable singleton-key atom type that participates safely in subsequent static algebra

#### Scenario: Prevent recursive carrier specialization
- **WHEN** downstream source names removed recursive merge, guard, token, or equivalent carrier APIs in an attempt to
  choose a normalized output
- **THEN** those APIs are unavailable and cannot authorize a malformed canonical result

#### Scenario: Reject malformed exponent magnitudes
- **WHEN** a stored `Power` uses an abstract, unresolved, or nonliteral `Int` exponent
- **THEN** normalization, supported public construction, and complete-result arithmetic evidence boundaries reject the
  representation

#### Scenario: Reject disguised reducible factors
- **WHEN** a claimed canonical key is an intersection, refinement, bound, unresolved wrapper, or non-singleton dimension
  expression disguised behind an alias
- **THEN** automatic derivation rejects it rather than treating it as one concrete singleton identity

#### Scenario: Diagnose unresolved generic derivation cleanly
- **WHEN** a generic method attempts multiplication without contextual `Normalize[Times[A, B]]` evidence
- **THEN** compilation reports actionable contextual-evidence guidance without a macro exception or compiler stack trace

#### Scenario: Reject an alias-hidden generic endpoint
- **WHEN** generic normalization or arithmetic introduces `type X = A` for an unresolved dimension parameter
- **THEN** the alias is followed to `A` and automatic derivation requires contextual `Normalize` evidence

#### Scenario: Expose a concrete associated alias
- **WHEN** a fixed holder defines a transparent `holder.D = Times[A, B]` over concrete dimensions
- **THEN** normalization flattens `holder.D` exactly as `Times[A, B]`

#### Scenario: Preserve a stable abstract associated identity
- **WHEN** a runtime-issued witness exposes its own concrete stable singleton key while another key is rooted in an
  abstract parameter or refinable selection
- **THEN** the witness key remains one opaque atomic identity and the unresolved key is rejected

#### Scenario: Reject rebound generic operation evidence
- **WHEN** generic `Normalize` evidence is copied through stable local values or singleton ascriptions while its input or
  output remains unresolved
- **THEN** rebinding does not create automatic authority and generic code must forward the contextual evidence

#### Scenario: Reuse a concrete operation output
- **WHEN** a stable `Normalize` value exposes an exact canonical output over fully concrete inputs
- **THEN** the output can be reused and is accepted consistently with direct concrete derivation

#### Scenario: Expose concrete operation endpoints transitively
- **WHEN** transparent aliases successively expose a fully concrete normalized operation result
- **THEN** normalization reaches the same canonical `Canonical` as the final concrete endpoint regardless of alias depth

#### Scenario: Reject nested powers after endpoint exposure
- **WHEN** a claimed canonical entry or alias exposes a nested `Canonical`, `Times`, `Inverse`, or other dimension expression
  where a concrete singleton key is required
- **THEN** final validation rejects it instead of certifying the expression as an atomic key

#### Scenario: Reject recursive term paths conservatively
- **WHEN** recursive aliases or witness paths prevent a singleton key or dimension expression from reaching a stable
  semantic form
- **THEN** derivation fails with contextual-evidence guidance without a stack overflow, macro exception, or compiler
  assertion

#### Scenario: Reuse a completed shared term path
- **WHEN** two acyclic branches reference the same completed concrete stable singleton key or normalized expression
- **THEN** both branches derive consistently and produce one canonical identity for that key

#### Scenario: Revalidate exposed final factors
- **WHEN** a transparent alias in a computed or claimed canonical entry exposes an unresolved, nonconcrete, duplicate,
  zero-powered, or otherwise malformed structure
- **THEN** the final normalization boundary rejects it before constructing trusted evidence

#### Scenario: Canonicalize an annotated atom coherently
- **WHEN** a valid stable singleton key or `Atom[K]` is wrapped in a transparent annotation
- **THEN** normalization derives the same unannotated canonical `Canonical` as for the underlying atom

#### Scenario: Normalize an annotated reducible expression
- **WHEN** `Times[A, B]`, `Inverse[A]`, `Divide[A, B]`, a canonical `Canonical`, or a transparent alias is annotated
- **THEN** derivation exposes and reduces the underlying expression normally and stores no annotation wrapper

#### Scenario: Canonicalize annotated natural magnitudes
- **WHEN** a migrated exponent uses a valid annotated singleton `Int` literal in place of the removed natural encoding
- **THEN** normalization emits the corresponding ordinary unannotated singleton `Int` exponent

#### Scenario: Reject invalid annotated underlying structure
- **WHEN** an annotation wraps a nonliteral exponent, malformed `Canonical`, nonconcrete key, unresolved generic expression, or
  structure outside the closed grammar
- **THEN** derivation rejects the exposed underlying structure by the same rule as the unannotated form

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept only quantities with the exact same Scala dimension type `D`, SHALL return
`Quantity[D]`, and SHALL require neither `Normalize[D]` nor `SameDimension`. Multiplication by `Rational` and exact
division by a nonzero whole scalar SHALL preserve `D` without normalization. Multiplying `Quantity[A]` by
`Quantity[B]` SHALL use the single normalization operation and return an exact quantity in a canonical `Canonical`: nested
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

#### Scenario: Reject malformed dimension-preserving arithmetic
- **WHEN** a zero-power or otherwise malformed `D` is selected for supported carrier construction, type-only zero,
  checked decoding, or identity-bearing algebra, while an otherwise uncallable method body over a hypothetical carrier
  performs only index-preserving arithmetic
- **THEN** the supported boundary rejects `D` before returning a malformed carrier or algebraic identity, while the
  otherwise uncallable index-preserving body MAY type-check without `Normalize[D]`

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
- **THEN** the exact result's public dimension is their validated canonical `Canonical` product

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
- **THEN** every use reaches the same canonical `Canonical` and runtime `DimKey` as direct use of the concrete endpoint

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** normalization produces the same canonical output for `holder.D` and `Times[A, B]`, including duplicate-key
  combination, cancellation, inversion, and runtime-key agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, canonical `Canonical`, reducible expression, or transparent alias differs from another input only by
  annotations
- **THEN** normalization produces the same unannotated canonical output and agrees with runtime `DimKey`
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

### Requirement: Static dimension capabilities remain independent
The public static-dimension model SHALL keep validity, runtime inhabitation, equivalence, and exact values as independent
contracts. `Normalize[D]` SHALL certify only that `D` is a valid closed static dimension expression and has one canonical
output. Deriving `Normalize[D]` or `Normalize[Atom[K]]` SHALL NOT assert, synthesize, or otherwise imply that a
`DimRef[D]`, `DimRef[Atom[K]]`, or `DimRef` for the canonical output exists. A concrete stable singleton key MAY
therefore be accepted by static normalization without belonging to the smaller set of atom types inhabitable through
supported public `DimRef` APIs.

`SameDimension[A, B]` SHALL remain controlled evidence that `A` and `B` denote the same dimension. The capability SHALL
NOT independently certify that either expression is valid and SHALL NOT establish runtime inhabitation for either side.
Reflexive evidence MAY exist from Scala type identity alone; it SHALL NOT authorize type-only manufacture or
complete-result computation. Those boundaries SHALL require the applicable `Normalize` evidence, while
index-preserving transformations of existing trusted carriers SHALL not require `Normalize[D]`.

`Quantity[D]` SHALL remain an exact coefficient indexed by `D`, not a runtime identity witness. Possessing a
`Quantity[D]` SHALL NOT provide a `DimRef[D]` or `DimKey`. Dimension-polymorphic zero SHALL remain available for any
normalized `D`; attaching a caller-supplied coefficient SHALL continue to require an authoritative `DimRef[D]`.
Similarly, possession of `DimRef[D]` SHALL NOT implicitly materialize contextual `Normalize` evidence for generic code
that computes a new static result dimension.

#### Scenario: Normalize a key without runtime inhabitation
- **WHEN** a supported concrete stable singleton key `K` admits `Normalize[Atom[K]]` but has no public authority-bearing
  `DimRef` constructor
- **THEN** static normalization succeeds and the evidence alone provides no way to obtain `DimRef[Atom[K]]`

#### Scenario: Construct only static zero without a witness
- **WHEN** `D` has `Normalize[D]` but no `DimRef[D]` is available
- **THEN** `Quantity.zero[D]` is available, while every public constructor that attaches a caller-supplied coefficient
  still requires `DimRef[D]`

#### Scenario: Keep generic runtime and static evidence separate
- **WHEN** generic code receives `DimRef[D]` and computes a new type-level result dimension whose normalization is
  unresolved
- **THEN** it must separately accept and forward `Normalize` for the complete result expression; the runtime witness
  does not satisfy static evidence search

#### Scenario: Keep reflexivity separate from validity
- **WHEN** a malformed `Canonical` representation obtains reflexive `SameDimension[D, D]` through Scala type identity
- **THEN** reflexivity cannot normalize `D` or construct a normally returned carrier; an otherwise uncallable
  index-preserving transformation body MAY still type-check without `Normalize[D]`

#### Scenario: Keep equivalence separate from runtime identity
- **WHEN** `SameDimension[A, B]` is derived from statically equivalent closed expressions without runtime witnesses
- **THEN** it permits only the documented controlled coercions and does not furnish a `DimRef` or `DimKey` for
  either expression
