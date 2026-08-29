## MODIFIED Requirements

### Requirement: Exact quantity construction
`Quantity` SHALL provide concise, overloaded, dimension-witnessed `apply` construction from `Rational`, `BigInt`,
`Int`, `Long`, decimal text, and finite `java.math.BigDecimal`, plus authoritative zero construction. `Int` and `Long`
construction SHALL widen exactly through `BigInt`. Overloaded `apply` SHALL be the sole public coefficient-bearing
construction surface: `fromRational`, `fromInteger`, `fromDecimal`, and `fromFiniteDecimal` SHALL NOT be available. The
scalar accessor SHALL return the canonical `Rational`. No authoritative constructor SHALL accept `Float` or `Double`,
and raw opaque reconstruction SHALL remain unavailable to supported callers, including downstream source that declares
`package trading.quantity`.

Raw coefficient attachment and operation-result construction SHALL be lexically private within the `Quantity` opaque
owner; package-qualified visibility SHALL NOT be a construction boundary. Every public operation that manufactures a
`Quantity[D]` without an existing trusted dimensional carrier SHALL require an authoritative `DimRef[D]` or a stronger
witness that owns such a reference. In particular, `Quantity.zero[D]` SHALL require `DimRef[D]`; zero SHALL NOT be
manufactured from the static type argument alone. Arithmetic results SHALL be derived only from legitimate operands,
authoritative witnesses, and checked evidence.

#### Scenario: Construct supported exact coefficients concisely
- **WHEN** `Quantity(dimension, coefficient)` receives a `Rational`, `BigInt`, `Int`, `Long`, decimal `String`, or
  finite `java.math.BigDecimal`
- **THEN** it returns the exact result or validation failure defined for that coefficient type

#### Scenario: Construct primitive integers concisely
- **WHEN** `Quantity(dimension, coefficient)` receives an `Int` or `Long` value or literal
- **THEN** it returns the same exact quantity as widening that coefficient to `BigInt`

#### Scenario: Construct exact decimal text
- **WHEN** `Quantity(usdDimension, "6000.001")` receives a USD dimension witness
- **THEN** it returns a `Quantity[USD]` with coefficient `6000001/1000`

#### Scenario: Manufacture a generic zero
- **WHEN** generic code calls `Quantity.zero[D]` without an existing `Quantity[D]`
- **THEN** it must supply an authoritative `DimRef[D]`

#### Scenario: Reject a type-only malformed zero
- **WHEN** supported code selects a malformed `Canonical` as `D` and attempts to call `Quantity.zero[D]`
- **THEN** no zero is produced because no authoritative `DimRef[D]` can be publicly obtained

#### Scenario: Reject removed named constructors
- **WHEN** supported Scala invokes `fromRational`, `fromInteger`, `fromDecimal`, or `fromFiniteDecimal`
- **THEN** the code does not compile and must use the matching `apply` overload

#### Scenario: Reject floating construction
- **WHEN** supported Scala attempts to construct an exact quantity from `0.1d` or `0.1f`
- **THEN** the code does not compile

#### Scenario: Reject same-package raw coefficient attachment
- **WHEN** downstream Scala declares `package trading.quantity` and supplies an arbitrary coefficient to a raw or
  operation-result construction helper
- **THEN** lexical privacy prevents construction of a chosen `Quantity[D]`

### Requirement: Compile-time dimension equivalence
`SameDimension[A, B]` SHALL be derivable at compile time when the library's private canonical interpreter can validate
both statically visible closed dimension expressions and establish that their singleton-key powers are mathematically
equal modulo expression shape and canonical tuple order. The interpreter SHALL accumulate powers with arbitrary
precision and SHALL require no runtime `DimRef`, `DimKey`, or total ordering over singleton keys. The evidence
SHALL remain a restricted capability whose construction is unavailable to supported downstream code.

`SameDimension` SHALL authorize controlled explicit alignment and equivalence-aware comparison. The public value-level
operation SHALL be `alignTo`: given `SameDimension[D, E]`, it SHALL retag `Quantity[D]` as `Quantity[E]` and
`GridQuantity[D, G]` as `GridQuantity[E, G]` without changing the exact coefficient, grid identity, or coordinate. It
SHALL NOT expose unrestricted Scala type equality, a global implicit conversion, or implicit alignment inside
homogeneous arithmetic.

Reflexive `SameDimension[D, D]` SHALL remain valid from Scala type identity alone and SHALL NOT certify that `D` is a
valid static representation or is publicly runtime-inhabitable. Every non-reflexive statically derived proof SHALL
validate both complete expressions. Runtime recovery SHALL continue to issue the same scoped capability only after
authoritative runtime keys agree. `SameDimension` SHALL NOT synthesize a `DimRef` for either side.

#### Scenario: Align commuted canonical dimensions
- **WHEN** two canonical `Canonical` values contain the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Align commuted expression products
- **WHEN** `Times[A, B]` and `Times[B, A]` have fully visible valid operands
- **THEN** compile-time `SameDimension` evidence is derivable and an existing value can be explicitly aligned

#### Scenario: Require explicit alignment before addition
- **WHEN** two quantities have equivalent dimensions represented by different Scala dimension types
- **THEN** direct addition and subtraction do not compile until one operand is explicitly aligned to the selected type

#### Scenario: Align an exact quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `Quantity[D]`
- **THEN** the result is `Quantity[E]` with exactly the original coefficient

#### Scenario: Nominate a generic output
- **WHEN** downstream generic code chooses `O` and accepts `SameDimension[Times[A, B], O]`
- **THEN** it may explicitly align the expression-preserving product to `Quantity[O]` without a public associated-output
  computation

#### Scenario: Reject unequal static dimensions
- **WHEN** private canonical interpretation finds a different singleton key or accumulated exponent
- **THEN** non-reflexive `SameDimension` is not derivable and `alignTo` cannot cross between the types

#### Scenario: Recover checked runtime equivalence
- **WHEN** two opaque runtime witnesses have equal authoritative `DimKey` values but distinct singleton-key types
- **THEN** successful runtime comparison may issue scoped `SameDimension` evidence for explicit `alignTo`

#### Scenario: Keep reflexivity separate from validity
- **WHEN** a malformed `Canonical` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity and the no-op `alignTo[D]` MAY be available, but neither creates a dimensional value,
  `DimRef[D]`, or proof that the representation is valid

#### Scenario: Align commuted products
- **WHEN** two concrete expression products have the same privately interpreted singleton-key powers in different
  orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Use equivalence in addition
- **WHEN** two quantities have equivalent dimensions in different Scala dimension types
- **THEN** direct addition and subtraction do not compile; the caller explicitly aligns one operand to the selected
  result type before homogeneous arithmetic

#### Scenario: Use evidence in addition
- **WHEN** a generic caller selects the left dimension as the result type for equivalent `Quantity[Left]` and
  `Quantity[Right]` operands
- **THEN** it forwards `SameDimension[Right, Left]` to `right.alignTo[Left]`, after which the arithmetic operation itself
  consumes no equivalence evidence

#### Scenario: Align a grid quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `GridQuantity[D, G]`
- **THEN** the result is `GridQuantity[E, G]` with the original grid type and coordinate

#### Scenario: Expose a concrete economic result directly
- **WHEN** a caller selects a documented endpoint operation whose declared source and target types determine an economic
  result
- **THEN** the endpoint operation returns the named target directly, while ordinary dimension-changing arithmetic
  retains its raw expression type

#### Scenario: Select an economic result type explicitly
- **WHEN** a caller intentionally selects an equivalent result spelling that differs from a raw expression type
- **THEN** `alignTo` exposes the selected type using `SameDimension` without runtime comparison or an unchecked public
  cast

#### Scenario: Derive evidence from downstream code
- **WHEN** supported downstream Scala requests `SameDimension` for equivalent commuted concrete products with compiler
  warnings treated as errors
- **THEN** the evidence compiles without inaccessible-member diagnostics or access to private interpreter machinery

#### Scenario: Keep reflexivity separate from canonical certification
- **WHEN** a malformed `Canonical` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity and `alignTo[D]` MAY be available, but they do not certify `D` or provide a normally
  returned carrier; supported construction rejects the malformed representation, while an otherwise uncallable
  index-preserving transformation body MAY type-check

### Requirement: Atomic and canonical static derivation
The public static dimension language SHALL remain closed over canonical `Canonical[Entries]`, expression constructors
`Times[A, B]` and `Inverse[A]`, and `Divide[A, B]` as quotient syntax. A canonical entry SHALL be
`Power[Key, Exponent]`, where `Key <: Singleton` identifies an atom and `Exponent <: Int` is a nonzero singleton integer
literal. `Atom[Key]` SHALL denote `Canonical[Power[Key, 1] *: EmptyTuple]`, and `One` SHALL denote `Canonical[EmptyTuple]`. An
arbitrary subtype of `Dim` SHALL NOT silently become a new atomic identity.

A declared canonical `Canonical` SHALL contain each singleton key at most once and SHALL store only nonzero literal
exponents. Tuple order SHALL not carry mathematical meaning. A zero exponent, duplicate key, non-`Power` entry,
abstract or nonliteral exponent, unresolved tuple, or key that is not a supported concrete stable singleton identity
SHALL make a claimed canonical representation invalid. Floating-point, decimal, and rational exponent types SHALL not
be part of the static dimension language.

`Key <: Singleton` SHALL be necessary but SHALL NOT by itself certify a canonical key. After annotations and transparent
aliases are exposed, private validation SHALL accept only supported concrete literal identities, concrete stable
term/module singleton references, and supported generative singleton identities. It SHALL reject `Singleton`,
`Nothing`, `Null`, widened intersections, unions, refinements, unresolved bounds or parameters, unresolved match or
lambda structures, ordinary non-concrete type references, and unknown wrappers.

The compiler machinery that validates and canonically interprets this grammar SHALL be library-private. Public
`Normalize[D]`, `Normalize.Aux[D, O]`, associated canonical-output evidence, recursive normalization rules, guards, and
caller-constructible proof tokens SHALL NOT be available. Generic dimension-changing operations SHALL preserve the
public expression type and therefore SHALL NOT require the compiler to materialize a named canonical output. A caller
that needs a chosen equivalent spelling SHALL use `SameDimension` explicitly.

Authority-bearing construction SHALL retain complete validation at its roots. Public literal atom construction SHALL
derive runtime identity from the accepted literal singleton and reject caller-selected widening. Public nominal atom
construction SHALL bind the supplied stable object's exact singleton type to that object's runtime identifier. Fresh
runtime witnesses SHALL bind their path-dependent atom type to their captured runtime key. Accepted static keys need
not all be publicly inhabitable, and private canonical interpretation alone SHALL NOT assert or synthesize a
`DimRef[Atom[K]]`.

#### Scenario: Declare named atoms with singleton keys
- **WHEN** a caller declares `type USD = Atom["asset:USD"]` and obtains the corresponding authoritative `DimRef`
- **THEN** `USD` denotes the one-power dimension for that singleton key and its witness has the matching runtime atom

#### Scenario: Reject a widened literal constructor key
- **WHEN** a caller supplies a `ValueOf[String & Singleton]` and explicitly requests
  `DimRef.atom[String & Singleton]`
- **THEN** construction is rejected by the internal concrete-key check and caller-selected widening establishes no
  runtime identity

#### Scenario: Bind nominal construction to the supplied stable identity
- **WHEN** two distinct `NominalAtom` objects are widened to a shared nominal supertype before construction
- **THEN** their results retain distinct exact singleton key types and cannot inhabit one caller-selected atom type

#### Scenario: Reject nonconcrete singleton keys
- **WHEN** a constructor or non-reflexive equivalence request exposes `Singleton`, `Nothing`, `Null`, a widened
  intersection, an unresolved parameter, or another non-concrete key
- **THEN** validation fails with a concrete-stable-singleton diagnostic and no authority or equivalence is issued

#### Scenario: Preserve supported concrete keys
- **WHEN** a key is a supported literal, nominal object, stable local/module value, generative witness, fresh runtime
  witness, or transparent alias or annotation exposing one of those identities
- **THEN** private interpretation treats the key coherently wherever that boundary is authorized to inspect it

#### Scenario: Reject malformed canonical entries
- **WHEN** a claimed `Canonical` contains a zero power, duplicate key, non-`Power` entry, abstract exponent, or unresolved tuple
- **THEN** no authoritative constructor or non-reflexive `SameDimension` derivation accepts the representation

#### Scenario: Preserve expression types in generic code
- **WHEN** generic code multiplies values indexed by abstract `A <: Dim` and `B <: Dim`
- **THEN** the public result type is `Times[A, B]` without contextual canonical-output evidence

#### Scenario: Reject the public normalization surface
- **WHEN** supported downstream code names `Normalize`, `Normalize.Aux`, or former specialized normalization evidence
- **THEN** the code does not compile and must use expression result types, `SameDimension`, or an authoritative witness

#### Scenario: Preserve an opaque runtime dimension safely
- **WHEN** a runtime key is resolved without a statically visible decomposition
- **THEN** its witness exposes one stable path-dependent atom type whose hidden runtime decomposition is not guessed by
  static equivalence

#### Scenario: Handle aliases, annotations, and cycles coherently
- **WHEN** private interpretation encounters transparent aliases or annotations around valid grammar, or recursive paths
  that cannot reach a stable form
- **THEN** it erases transparent wrappers for valid inputs and rejects non-progressing recursion without a compiler
  assertion, stack overflow, or accidental new atomic identity

#### Scenario: Name an integer-powered canonical dimension
- **WHEN** a caller names `Canonical[Power["length", 2] *: Power["time", -1] *: EmptyTuple]`
- **THEN** private validation accepts the nonzero singleton `Int` powers and preserves their exact mathematical values

#### Scenario: Reject fractional exponents
- **WHEN** a caller attempts to use a floating, decimal, or rational type as a `Power` exponent
- **THEN** the type does not satisfy the static exponent contract and compilation fails

#### Scenario: Normalize the closed expression grammar
- **WHEN** a concrete expression combines `Canonical`, `Times`, `Inverse`, `Divide`, `Atom`, `One`, transparent aliases, and
  transparent annotations
- **THEN** the library-private interpreter validates and mathematically interprets the complete expression without
  exposing public associated-output evidence

#### Scenario: Require one contextual operation in generic code
- **WHEN** generic code manufactures a carrier from an abstract `D` or computes a new dimension from trusted operands
- **THEN** manufacture requires the applicable `DimRef[D]` or stronger matching witness, while dimension-changing
  arithmetic returns its raw expression type without a contextual output computation

#### Scenario: Reject the legacy proof surface
- **WHEN** supported downstream code names the removed normalization family, signed-natural exponents, or specialized
  product, quotient, inverse, alignment, or normalized-powers evidence
- **THEN** those APIs are unavailable and code must use literal `Int` powers, expression result types,
  `SameDimension`, or authoritative witnesses

#### Scenario: Prevent recursive carrier specialization
- **WHEN** downstream source names removed recursive merge, guard, token, or equivalent helper APIs in an attempt to
  choose a static output
- **THEN** those APIs are unavailable and cannot authorize a malformed representation or caller-selected output

#### Scenario: Reject malformed exponent magnitudes
- **WHEN** a stored `Power` uses an abstract, unresolved, or nonliteral `Int` exponent
- **THEN** private validation, supported public construction, and non-reflexive equivalence reject the representation

#### Scenario: Reject disguised reducible factors
- **WHEN** a claimed canonical key is an intersection, refinement, bound, unresolved wrapper, or non-singleton dimension
  expression disguised behind an alias
- **THEN** private validation rejects it rather than treating it as one concrete singleton identity

#### Scenario: Diagnose unresolved generic derivation cleanly
- **WHEN** generic code requests non-reflexive `SameDimension` for unresolved expression parameters
- **THEN** compilation reports actionable contextual-evidence guidance without a macro exception or compiler stack trace,
  while expression-preserving arithmetic over those parameters remains available

#### Scenario: Reject an alias-hidden generic endpoint
- **WHEN** generic static comparison introduces `type X = A` for an unresolved dimension parameter
- **THEN** the alias is followed to `A` and automatic non-reflexive derivation rejects the unresolved endpoint

#### Scenario: Expose a concrete associated alias
- **WHEN** a fixed holder defines a transparent `holder.D = Times[A, B]` over concrete dimensions
- **THEN** private interpretation treats `holder.D` exactly as `Times[A, B]` for validation and equivalence

#### Scenario: Preserve a stable abstract associated identity
- **WHEN** a runtime-issued witness exposes its own concrete stable singleton key while another key is rooted in an
  abstract parameter or refinable selection
- **THEN** the witness key remains one opaque atomic identity and the unresolved key is rejected by private inspection

#### Scenario: Reject rebound generic operation evidence
- **WHEN** generic capabilities or parameter-dependent witnesses are copied through stable local values or singleton
  ascriptions while their dependencies remain unresolved
- **THEN** rebinding creates neither automatic static authority nor a caller-selected canonical output

#### Scenario: Reuse a concrete operation output
- **WHEN** a stable value exposes an exact raw expression result over fully concrete inputs
- **THEN** that expression type can be reused consistently, and an equivalent chosen spelling requires explicit
  `SameDimension`

#### Scenario: Expose concrete operation endpoints transitively
- **WHEN** transparent aliases successively expose a fully concrete expression result
- **THEN** private interpretation reaches the same mathematical powers and runtime-key meaning as the final concrete
  endpoint regardless of alias depth

#### Scenario: Reject nested powers after endpoint exposure
- **WHEN** a claimed canonical entry or alias exposes a nested `Canonical`, `Times`, `Inverse`, or other dimension expression
  where a concrete singleton key is required
- **THEN** final validation rejects it instead of certifying the expression as an atomic key

#### Scenario: Reject recursive term paths conservatively
- **WHEN** recursive aliases or witness paths prevent a singleton key or dimension expression from reaching a stable
  semantic form
- **THEN** private derivation fails with a controlled diagnostic without a stack overflow, macro exception, or compiler
  assertion

#### Scenario: Reuse a completed shared term path
- **WHEN** two acyclic branches reference the same completed concrete stable singleton key or interpreted expression
- **THEN** both branches are accepted consistently and denote one identity for that key

#### Scenario: Revalidate exposed final factors
- **WHEN** a transparent alias in a claimed canonical entry exposes an unresolved, nonconcrete, duplicate, zero-powered,
  or otherwise malformed structure
- **THEN** the final private validation boundary rejects it before issuing equivalence or construction authority

#### Scenario: Canonicalize an annotated atom coherently
- **WHEN** a valid stable singleton key or `Atom[K]` is wrapped in a transparent annotation
- **THEN** private interpretation produces the same mathematical interpretation as for the unannotated atom

#### Scenario: Normalize an annotated reducible expression
- **WHEN** `Times[A, B]`, `Inverse[A]`, `Divide[A, B]`, a canonical `Canonical`, or a transparent alias is annotated
- **THEN** private interpretation exposes and reduces the underlying expression and stores no annotation wrapper

#### Scenario: Canonicalize annotated natural magnitudes
- **WHEN** a declared exponent uses a valid annotated singleton `Int` literal
- **THEN** private interpretation reads the corresponding ordinary unannotated mathematical exponent

#### Scenario: Reject invalid annotated underlying structure
- **WHEN** an annotation wraps a nonliteral exponent, malformed `Canonical`, nonconcrete key, unresolved generic expression, or
  structure outside the closed grammar
- **THEN** private interpretation rejects the exposed underlying structure by the same rule as the unannotated form

### Requirement: Arbitrary-precision exactness
Quantity coefficients, grid coordinates, rational numerators and denominators, private canonical interpretation, and
runtime `DimKey` exponents SHALL retain arbitrary-precision semantics. A declared canonical `Power` exponent
SHALL remain limited to the exact values representable by Scala singleton `Int` literals. Expression-preserving static
arithmetic SHALL not emit a new `Power` literal and therefore SHALL preserve a valid expression even when its
mathematical accumulated exponent lies outside the singleton-`Int` range.

Private equivalence checking and runtime witness algebra SHALL accumulate exponents with `BigInt` and SHALL NOT wrap,
truncate, saturate, or approximate them. Lack of a representable canonical `Canonical[Power[K, N]]` spelling for an
out-of-range `N` SHALL NOT make an expression-preserving arithmetic result numerically or dimensionally incorrect.

#### Scenario: Denominator grows during arithmetic
- **WHEN** an exact calculation produces a denominator absent from either input
- **THEN** the normalized rational result is preserved exactly

#### Scenario: Preserve an in-range declared power
- **WHEN** a caller declares a canonical `Power` with a nonzero singleton `Int` exponent
- **THEN** private validation preserves that exact mathematical exponent

#### Scenario: Reject an unrepresentable declared power
- **WHEN** a caller attempts to declare a canonical `Power` exponent that is not a singleton `Int` literal
- **THEN** the type does not satisfy the canonical representation contract

#### Scenario: Interpret an out-of-range expression exactly
- **WHEN** multiplication or inversion makes the mathematical accumulated power exceed the singleton-`Int` range
- **THEN** the public type remains the source expression and private equivalence and runtime keys retain the exact
  arbitrary-precision exponent

#### Scenario: Cancel large intermediate powers
- **WHEN** two differently shaped expressions accumulate powers outside the singleton-`Int` range before cancelling
- **THEN** non-reflexive equivalence compares their final mathematical powers without machine overflow

#### Scenario: Static exponent fits the literal range
- **WHEN** a caller declares a canonical `Power` with a surviving nonzero exponent in the singleton-`Int` range
- **THEN** private validation preserves the exact singleton literal value

#### Scenario: Static exponent exceeds the literal range
- **WHEN** expression-preserving arithmetic accumulates a surviving mathematical exponent outside the singleton-`Int`
  range
- **THEN** the raw expression remains valid and exact without emitting an out-of-range canonical `Power` literal

#### Scenario: Runtime exponent exceeds the static range
- **WHEN** runtime `DimKey` arithmetic produces an exponent outside the singleton-`Int` range
- **THEN** the runtime key preserves the exact `BigInt` exponent without approximation

#### Scenario: Dimension exponent exceeds machine range
- **WHEN** private or runtime dimension arithmetic produces an exponent outside the singleton-`Int` range
- **THEN** private interpretation and runtime keys preserve the exact mathematical power while the public static result
  remains its source expression

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept only quantities with the exact same Scala dimension type `D` and SHALL return
`Quantity[D]` without `SameDimension` or any validity capability. Multiplication by `Rational`, exact division by a
nonzero whole scalar, negation, ordering, and observation SHALL preserve an existing trusted `Quantity[D]` without
additional dimension evidence.

Generic dimension-changing arithmetic SHALL preserve expression types directly. Multiplying `Quantity[A]` by
`Quantity[B]` SHALL return `Quantity[Times[A, B]]`. Dividing `Quantity[A]` by `NonZero[Quantity[B]]` SHALL return
`Quantity[Divide[A, B]]`. Any public checked reciprocal or inverse operation over a dimensional value SHALL use
`Inverse[A]`. These operations SHALL require no public normalization evidence, SHALL preserve exact coefficients, and
SHALL not silently align the result to a different static spelling.

Dimension witness algebra SHALL follow the same typing: products, inverses, and quotients of authoritative witnesses
SHALL return `DimRef[Times[A, B]]`, `DimRef[Inverse[A]]`, and `DimRef[Divide[A, B]]` with exactly corresponding runtime
keys. Algebraically equal expression shapes SHALL be related by `SameDimension`, not made definitionally equal by
arithmetic.

Supported public construction roots SHALL ensure that operands carry valid dimension indices. Consequently a generic
method that merely transforms hypothetical `Quantity[Bad]` parameters MAY type-check, but supported callers SHALL have
no construction path for such parameters. Dimension-changing arithmetic SHALL NOT use reflexive `SameDimension` as a
substitute for construction authority.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values without contextual dimension evidence

#### Scenario: Write generic homogeneous arithmetic
- **WHEN** a generic operation accepts two `Quantity[D]` operands
- **THEN** it can add, subtract, or scale them without normalization or dimensional-equivalence vocabulary

#### Scenario: Reject implicit cross-spelling arithmetic
- **WHEN** `Quantity[A]` and `Quantity[B]` have different static dimension types even though `SameDimension[A, B]` is
  available
- **THEN** direct addition and subtraction do not compile until one operand is explicitly aligned

#### Scenario: Reject malformed dimension-preserving arithmetic
- **WHEN** a zero-power or otherwise malformed `D` is selected for supported carrier construction, checked decoding,
  or identity-bearing algebra, while an otherwise uncallable method body over a hypothetical carrier performs only
  index-preserving arithmetic
- **THEN** the supported authority boundary rejects `D` before returning a malformed carrier or algebraic identity,
  while the otherwise uncallable index-preserving body MAY type-check without a dimension capability

#### Scenario: Preserve source-expression dimensions
- **WHEN** an index-preserving operation is applied to an existing `Quantity[Divide[T, F]]` or generic `Quantity[D]`
- **THEN** it compiles and retains the original dimension spelling without a dimension capability

#### Scenario: Keep zero construction explicit
- **WHEN** generic code needs `Quantity.zero[D]` without an existing quantity operand
- **THEN** it must provide an authoritative `DimRef[D]`

#### Scenario: Preserve a generic product expression
- **WHEN** generic code multiplies `Quantity[A]` and `Quantity[B]`
- **THEN** the inferred result is `Quantity[Times[A, B]]` without an output type parameter or contextual capability

#### Scenario: Preserve a generic quotient expression
- **WHEN** generic code divides `Quantity[A]` by `NonZero[Quantity[B]]`
- **THEN** the inferred result is `Quantity[Divide[A, B]]`

#### Scenario: Keep concrete cancellation explicit
- **WHEN** `Quantity[Divide[Usd, Btc]]` is multiplied by `Quantity[Btc]`
- **THEN** ordinary multiplication returns `Quantity[Times[Divide[Usd, Btc], Btc]]`, which may be explicitly aligned to
  `Quantity[Usd]`

#### Scenario: Preserve commutative equivalence
- **WHEN** quantities in dimensions `A` and `B` are multiplied in opposite operand orders
- **THEN** the two expression result types remain distinct Scala types and admit `SameDimension` when their operands are
  statically valid and visible

#### Scenario: Lift authoritative witness algebra
- **WHEN** authoritative dimension witnesses are multiplied, inverted, or divided
- **THEN** their result types preserve `Times`, `Inverse`, or `Divide` and their runtime keys perform the matching exact
  `DimKey` operation

#### Scenario: Keep runtime-hidden structure opaque
- **WHEN** an opaque runtime dimension's key contains a factor that would cancel a separate static atom
- **THEN** its static path-dependent atom remains opaque and any recovered equivalence requires authoritative runtime
  comparison

#### Scenario: Multiply concrete dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the exact result has public type `Quantity[Times[A, B]]`

#### Scenario: Multiply dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the result preserves the complete `Times[A, B]` expression rather than emitting a canonical `Canonical`

#### Scenario: Cancel a price denominator
- **WHEN** `Quantity[Position]` is multiplied by a quantity in `Divide[Settlement, Position]`
- **THEN** ordinary multiplication retains `Times[Position, Divide[Settlement, Position]]`; the caller may explicitly
  align it to `Settlement` or choose a documented endpoint operation that returns `Settlement` directly

#### Scenario: Retain a multi-key product
- **WHEN** multiplication has nonzero mathematical powers for more than one singleton key
- **THEN** the public result retains its complete expression and private equivalence interprets every surviving key
  exactly without exposing canonical-output evidence

#### Scenario: Retain a multi-atom product
- **WHEN** multiplication has nonzero mathematical powers for more than one singleton-key atom
- **THEN** the public result retains the raw operand expression and its private interpretation contains the exact
  mathematical power for every surviving atom

#### Scenario: Use one generic normalization context
- **WHEN** a generic function multiplies, dimensionally divides, or otherwise computes a result dimension from abstract
  inputs
- **THEN** its signature requires no normalization context and exposes the corresponding complete `Times`, `Inverse`,
  or `Divide` expression

#### Scenario: Specialize generic multiplication to one dimension
- **WHEN** a generic expression-preserving multiplication is instantiated with both dimension parameters equal to `D`
- **THEN** its result type is `Times[D, D]`, whose private interpretation has the exact mathematical power two

#### Scenario: Specialize generic inversion to a visible product
- **WHEN** generic witness inversion is instantiated with a concrete product `Times[A, B]`
- **THEN** its result type is `Inverse[Times[A, B]]`, its private interpretation negates both powers, and its runtime key
  matches exact `DimKey` inversion

#### Scenario: Reject late alias specialization without contextual evidence
- **WHEN** a generic method requests non-reflexive equivalence for aliases of unresolved dimensions before a caller later
  supplies concrete arguments
- **THEN** automatic derivation is rejected at the generic definition; the method may retain raw expression types or
  accept and forward the exact `SameDimension` relation it needs

#### Scenario: Reject refinable-member inversion without contextual evidence
- **WHEN** static equivalence is requested for an inverse rooted in an abstract member or refinable prefix
- **THEN** private derivation rejects the unresolved structure instead of freezing it as one singleton key, while raw
  expression-preserving inversion remains available

#### Scenario: Reject local aliases over dependent parameters
- **WHEN** generic dimension-changing arithmetic defines aliases over parameter-dependent dimension members before
  requesting static equivalence
- **THEN** the aliases do not hide the unresolved roots or create automatic equivalence authority

#### Scenario: Reject stable local transport of unresolved evidence
- **WHEN** unresolved dimension parameters or dependent witnesses are rebound through stable locals or singleton
  ascriptions
- **THEN** rebinding creates neither an automatic static interpretation result nor authority to choose a canonical
  output; documented `SameDimension` evidence may only be forwarded for its exact relation

#### Scenario: Preserve endpoint-depth coherence
- **WHEN** transparent aliases successively expose a fully concrete expression result
- **THEN** every use has the same private mathematical interpretation and runtime `DimKey` as direct use of the
  concrete endpoint

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** private interpretation establishes the same mathematical powers for `holder.D` and `Times[A, B]`, including
  combination, cancellation, inversion, and runtime-key agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, canonical `Canonical`, reducible expression, or transparent alias differs from another input only by
  annotations
- **THEN** private interpretation establishes the same unannotated mathematical powers and agrees with runtime
  `DimKey` multiplication and inversion

### Requirement: Exact rates and ratios
`Rate[From, To]` SHALL represent an exact coefficient oriented from `From` to `To`, and `Ratio` SHALL denote
`Quantity[One]`. Authoritative rate construction SHALL accept source and target `DimRef` values, work equally for
statically declared and runtime-resolved endpoint types, and return `Rate[From, To]` directly. Identity-rate
construction SHALL require the authoritative endpoint witness.

Endpoint-oriented operations SHALL preserve their semantic result types without public normalization or caller-visible
alignment. Applying `Rate[From, To]` to `Quantity[From]` SHALL return `Quantity[To]`; composing `Rate[A, B]` with
`Rate[B, C]` SHALL return `Rate[A, C]`; reciprocating a checked nonzero `Rate[A, B]` SHALL return `Rate[B, A]`; and
cross-rate division with a shared target SHALL return the declared remaining endpoint orientation. These helpers SHALL
be implemented from trusted existing carriers and endpoint authority, not from reflexive equivalence.

Ordinary quantity multiplication and division SHALL remain expression-preserving even when the operands happen to be
rates. A caller choosing ordinary arithmetic receives the raw expression type and MAY explicitly `alignTo` an equivalent
semantic spelling. Endpoint helpers SHALL remain associative where applicable and preserve identity-rate laws.

#### Scenario: Construct a statically declared rate
- **WHEN** authoritative BTC and USD witnesses and an exact coefficient are supplied
- **THEN** rate construction returns `Rate[BTC, USD]` without `Normalize` or `Normalize.Aux`

#### Scenario: Construct a runtime-resolved rate
- **WHEN** source and target dimensions are obtained as stable path-dependent `DimRef` values from runtime discovery
- **THEN** the same endpoint constructor returns a rate retaining those endpoint types

#### Scenario: Apply a mathematical rate
- **WHEN** `0.1 BTC` is acted on by `60000.01 USD/BTC`
- **THEN** rate application produces exact `6000.001 USD` as `Quantity[USD]`

#### Scenario: Compose rates directly
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** endpoint composition produces the exact product directly as `Rate[A, C]`

#### Scenario: Reciprocal preserves endpoints
- **WHEN** a nonzero `Rate[A, B]` is reciprocated
- **THEN** the exact result is returned directly as `Rate[B, A]`

#### Scenario: Derive a cross rate by division
- **WHEN** `USD/BTC` is divided by nonzero `USD/ETH` through the endpoint-oriented cross-rate operation
- **THEN** it returns `Rate[BTC, ETH]` directly, while ordinary quantity division returns its raw `Divide` expression

#### Scenario: Keep generic instrument conversion endpoint-oriented
- **WHEN** runtime instrument logic supplies a chain of typed rates between base, quote, position, and settlement
  endpoints
- **THEN** the chain can be composed and applied without exposing canonical-output computation or embedding an
  instrument model in the quantity library

#### Scenario: Compose rates without alignment repair
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate through endpoint composition
- **THEN** the semantic operation returns `Rate[A, C]` without caller-supplied alignment, while ordinary multiplication
  retains its raw expression type

#### Scenario: Compose rates
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** endpoint composition produces the exact product directly as `Rate[A, C]`

#### Scenario: Reject malformed endpoint cancellation
- **WHEN** supported code selects a malformed endpoint representation for rate construction, application, composition,
  ratio construction, or cross-rate division
- **THEN** it cannot obtain the authoritative endpoint witnesses or trusted carriers needed to call that boundary, and
  no endpoint-shaped result is normally returned

### Requirement: Checked quantity division
Division by a quantity SHALL require `NonZero[Quantity[B]]`. Generic `Quantity[A].divideBy` SHALL return the exact raw
expression `Quantity[Divide[A, B]]`. A same-type semantic ratio operation `ratioTo` SHALL accept
`NonZero[Quantity[D]]` and return `Ratio` directly. A grid divisor SHALL first use its canonical exact embedding and then
the same generic `NonZero` check. No separate divisor carrier, public normalization capability, or implicit result
alignment SHALL exist.

#### Scenario: Produce a ratio directly
- **WHEN** exact `10 USD` calls `ratioTo` with checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3`

#### Scenario: Preserve a generic quotient
- **WHEN** exact `Quantity[A]` is divided by checked nonzero `Quantity[B]` through generic division
- **THEN** the result has type `Quantity[Divide[A, B]]`

#### Scenario: Keep a quotient of rates as an expression
- **WHEN** exact `USD/BTC` is generically divided by checked nonzero `USD/ETH`
- **THEN** the result retains the complete `Divide[Divide[Usd, Btc], Divide[Usd, Eth]]` spelling until explicitly
  aligned or calculated through `crossRate`

#### Scenario: Reject zero divisor evidence
- **WHEN** `NonZero` receives a zero exact quantity or the exact embedding of a zero grid quantity
- **THEN** it fails and division remains unavailable

#### Scenario: Produce a ratio
- **WHEN** exact `10 USD` calls `ratioTo` with checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3`

#### Scenario: Simplify a quotient of rates
- **WHEN** exact `USD/BTC` is generically divided by checked nonzero `USD/ETH`
- **THEN** the result retains `Divide[Divide[Usd, Btc], Divide[Usd, Eth]]`; callers may relate it to the equivalent
  endpoint spelling with `SameDimension` or use `crossRate` for a direct `Rate[BTC, ETH]`

### Requirement: Exact-only algebra
The optional algebra layer SHALL expose one coherent strongest-instance hierarchy backed by primitive exact
operations. `Rational` SHALL be a commutative ring. The one production `ExactScalarField[Rational]` SHALL extend and
supply that standard commutative-ring structure while adding reciprocal that accepts `NonZero[Rational]`; checked
raw-scalar reciprocal MAY return `Either[ExpectedNonZero.type, Rational]`. It SHALL NOT expose Algebra
`Field[Rational]` or floating scalar construction.

`LeftModule[V, S]` SHALL extend the additive commutative group of `V`, and `VectorSpace[V, F]` SHALL extend
`LeftModule[V, F]`. One production `VectorSpace[Quantity[D], Rational]` SHALL therefore supply the quantity's rational
vector-space, left-module, and additive-commutative-group structures. Quantity multiplication SHALL remain graded by
the dimension-expression group rather than supplying a same-carrier ring. The implementation SHALL NOT expose
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

Every algebra instance that supplies a dimension-typed identity SHALL require authority to manufacture that identity.
The quantity vector space, grid module, and nonnegative quantity and grid monoids SHALL therefore require
`DimRef[D]`, or a stronger matching witness already required by that structure. An algebra instance that only combines
existing trusted dimensional values SHALL require no dimension authority. No competing weaker instance SHALL be
introduced merely to bypass the authority requirement.

#### Scenario: Import exact quantity algebra
- **WHEN** a caller imports exact quantity algebra with an authoritative `DimRef[D]`
- **THEN** exact vector-space operations and their manufactured zero are available without floating scalar construction

#### Scenario: Reuse the strongest quantity structure
- **WHEN** a caller requests a left module or additive commutative group after importing exact quantity algebra
- **THEN** the production vector-space instance supplies that weaker structure without a competing group instance

#### Scenario: Require authority for an algebraic identity
- **WHEN** generic code requests a quantity vector space, grid module, or nonnegative dimensional monoid for abstract `D`
- **THEN** it must provide `DimRef[D]` or the documented stronger matching witness because the structure can manufacture
  zero without an existing carrier

#### Scenario: Combine positive values without authority
- **WHEN** generic code requests the additive semigroup for positive quantity or grid values in abstract `D`
- **THEN** the instance is available without a dimension witness because every operation consumes trusted values

#### Scenario: Reciprocal is total after evidence
- **WHEN** `ExactScalarField[Rational].reciprocal` receives `NonZero[Rational]`
- **THEN** it returns the exact reciprocal without a division-by-zero branch

#### Scenario: Multiply nonzero rationals
- **WHEN** two `NonZero[Rational]` values are multiplied or one is reciprocated
- **THEN** the result remains nonzero without rerunning the predicate

#### Scenario: Preserve graded quantity multiplication
- **WHEN** quantities are multiplied associatively or distributively across dimension expressions
- **THEN** coefficients obey the commutative graded-algebra laws and `SameDimension` reconciles equivalent expression
  shapes when a common spelling is required

#### Scenario: Imports do not change arithmetic meaning
- **WHEN** algebra or rounding-policy values are imported
- **THEN** existing quantity operations keep the same result types and numerical semantics

#### Scenario: Combine positive values without normalization
- **WHEN** generic code requests the additive semigroup for positive quantity or grid values in abstract `D`
- **THEN** the instance is available without dimension authority because every operation consumes trusted values

### Requirement: Static dimension capabilities remain independent
The public static-dimension model SHALL keep private static interpretation, runtime inhabitation, equivalence, and
exact values as independent contracts. The library-private interpreter SHALL validate and mathematically compare closed
static dimension expressions without exposing associated canonical-output evidence. Acceptance by that interpreter
SHALL NOT assert, synthesize, or otherwise imply that a `DimRef[D]`, `DimRef[Atom[K]]`, or witness for an equivalent
spelling exists. A concrete stable singleton key MAY therefore be accepted for private interpretation without belonging
to the smaller set of atom types inhabitable through supported public `DimRef` APIs.

`SameDimension[A, B]` SHALL remain controlled evidence that `A` and `B` denote the same dimension. The capability SHALL
NOT independently certify that either expression is valid and SHALL NOT establish runtime inhabitation for either side.
Reflexive evidence MAY exist from Scala type identity alone; it SHALL NOT authorize manufacture from a type argument.
Non-reflexive derivation SHALL validate both complete expressions, and every use for value retagging SHALL reject null
evidence before returning. Existing-carrier transformations SHALL not require this capability when their static indices
already match exactly.

`DimRef[D]` SHALL remain runtime-inhabitation authority rather than public static-result computation. Possessing one
SHALL authorize documented construction roots and expression-preserving witness algebra, but SHALL NOT expose a
canonical output type or make an unrelated static atom inhabitable. `Quantity[D]` SHALL remain an exact coefficient
indexed by `D`, not a runtime identity witness. Possessing a quantity SHALL NOT provide `DimRef[D]`, `DimKey`,
`SameDimension`, a grid witness, or registered provenance. Manufacturing quantity or grid zero without an existing
trusted carrier SHALL require `DimRef[D]` or a documented stronger matching witness.

#### Scenario: Interpret a key without runtime inhabitation
- **WHEN** private static interpretation accepts a supported concrete stable singleton key `K` for `Atom[K]` but no
  public authority-bearing atom constructor owns that key
- **THEN** interpretation succeeds for its authorized static purpose and provides no way to obtain
  `DimRef[Atom[K]]`

#### Scenario: Require runtime authority for zero
- **WHEN** no `DimRef[D]` or documented stronger matching witness is available
- **THEN** generic code cannot manufacture `Quantity.zero[D]` or `GridQuantity.zero[D, G]` from the type arguments alone

#### Scenario: Keep generic runtime and static capabilities separate
- **WHEN** generic code receives `DimRef[D]` and performs dimension-changing arithmetic with another trusted carrier
- **THEN** the result retains its public `Times`, `Inverse`, or `Divide` expression and the witness does not expose an
  independently selected canonical output type

#### Scenario: Normalize a key without runtime inhabitation
- **WHEN** private static interpretation accepts a supported concrete stable singleton key `K` but no public
  authority-bearing atom constructor owns `K`
- **THEN** the static interpretation succeeds for its authorized purpose and provides no way to obtain
  `DimRef[Atom[K]]`

#### Scenario: Construct only static zero without a witness
- **WHEN** a dimension expression is privately valid but no `DimRef[D]` or documented stronger matching witness is
  available
- **THEN** neither quantity nor grid zero can be manufactured from the static type argument alone

#### Scenario: Keep generic runtime and static evidence separate
- **WHEN** generic code receives `DimRef[D]` and computes a dimension-changing result from trusted operands
- **THEN** it obtains the raw expression result and no public associated-output evidence is materialized from the runtime
  witness

#### Scenario: Keep reflexivity separate from validity
- **WHEN** a malformed `Canonical` representation obtains reflexive `SameDimension[D, D]` through Scala type identity
- **THEN** reflexivity cannot construct a normally returned carrier or runtime witness; an otherwise uncallable
  index-preserving transformation body MAY still type-check without a dimension capability

#### Scenario: Keep equivalence separate from runtime identity
- **WHEN** `SameDimension[A, B]` is derived from statically equivalent closed expressions without runtime witnesses
- **THEN** it permits only documented explicit alignment and comparison and does not furnish a `DimRef` or
  `DimKey` for either expression

### Requirement: Existing dimensional carriers have validated indices
For supported, well-typed Scala callers, every normally returned `Quantity[D]` and `GridQuantity[D, G]` SHALL have a
valid closed dimension index `D`. The invariant SHALL be established at public construction roots: coefficient-bearing
quantities and zero manufacture without an existing carrier require an authoritative `DimRef[D]`; nonzero grid
coordinates require a matching `GridRef[D]`; grid zero requires `DimRef[D]` or a stronger matching grid witness;
non-reflexive alignment requires a non-null `SameDimension`; and runtime reconstruction requires checked witness
ownership. Dimension-changing results derived from existing trusted carriers SHALL preserve their complete `Times`,
`Inverse`, or `Divide` expression rather than require or expose a caller-selected canonical output.

Possessing a dimensional value SHALL NOT materialize or permit recovery of `DimRef[D]`, `DimKey`,
`SameDimension`, `GridRef[D, G]`, private static interpretation, or registered provenance. It SHALL only allow operations
that preserve its already validated dimension index to construct further values at that same index without requesting
dimension authority again. Refined wrappers over an existing dimensional value SHALL inherit the same dimension-index
invariant.

Operation-local rejection of a hypothetical malformed carrier type SHALL NOT be required. A method body that accepts an
otherwise unobtainable `Quantity[Bad]` or `GridQuantity[Bad, G]` parameter MAY type-check for index-preserving
transformations, but supported public APIs SHALL provide no normally returning construction path for such an argument.
The ordinary supported-caller exclusions for casts, reflection, unsafe bytecode, and constructor-bypassing
deserialization remain unchanged; cast-free `null` SHALL NOT inhabit either opaque carrier. Literal `null` supplied as
reference-valued construction or alignment authority SHALL fail at the public boundary before a witness, dimensional
carrier, rate, or identity-bearing algebra capability is returned. A typed null `Rational` coefficient or `BigInt`
coordinate SHALL likewise be rejected at the shared coefficient or coordinate construction boundary; checked grid
decoding SHALL reconstruct through the same guarded coordinate boundary.

#### Scenario: Reduce existing generic quantities
- **WHEN** generic code receives a nonempty collection of `Quantity[D]` values and combines them with homogeneous
  addition
- **THEN** it requires no `DimRef[D]` or dimensional-equivalence evidence

#### Scenario: Transform one existing quantity
- **WHEN** generic code scales or exact-divides an existing `Quantity[D]` while preserving `D`
- **THEN** the result remains `Quantity[D]` without contextual dimension evidence

#### Scenario: Transform existing refined values
- **WHEN** generic code combines or otherwise transforms existing refined quantity or grid values without changing their
  dimension index
- **THEN** refinement closure and result construction require no dimension capability

#### Scenario: Keep value trust non-extractable
- **WHEN** generic code possesses `Quantity[D]` or `GridQuantity[D, G]`
- **THEN** it cannot summon or recover `DimRef[D]`, `SameDimension`, a runtime key, a grid witness, private static
  interpretation, or registry provenance from that value

#### Scenario: Reject malformed carrier construction
- **WHEN** supported code selects a zero-power or otherwise malformed `D` and attempts raw construction, witness-backed
  zero or coefficient construction, non-reflexive alignment from a valid carrier, or checked decoding
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

#### Scenario: Reject null dimensional construction authority
- **WHEN** supported Scala explicitly supplies literal `null` as `DimRef[D]` to quantity, grid, refinement, rate, or
  identity-bearing algebra construction
- **THEN** the root terminates before returning any dimensional carrier, rate, or algebra capability

#### Scenario: Reject null alignment authority
- **WHEN** supported Scala explicitly supplies literal `null` as `SameDimension[D, E]` to quantity or grid `alignTo`
- **THEN** alignment terminates before returning a retagged dimensional carrier

#### Scenario: Reject null normalization authority
- **WHEN** supported Scala attempts the obsolete null normalization-authority construction path
- **THEN** no such public capability or entry point exists; retained `DimRef` construction and `SameDimension`
  alignment boundaries independently reject null before returning a carrier, witness, rate, or algebra capability

#### Scenario: Reject null grid-construction authority
- **WHEN** supported Scala supplies literal `null` as the `DimRef[D]` authority to uniform-grid construction
- **THEN** construction terminates before returning a `GridRef[D]` capable of attaching coordinates

#### Scenario: Reject null runtime-identity authority
- **WHEN** supported Scala supplies literal `null` as a `DimKey` atom or power component, a fresh key, an atomic or
  nominal atom ID, a grid identity component, or registry identity input
- **THEN** the public construction root terminates before returning a key, dimension or grid witness, registered
  identity, equivalence, or dimensional carrier
