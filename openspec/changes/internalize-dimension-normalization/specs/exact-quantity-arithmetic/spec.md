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
- **WHEN** supported code selects a malformed `Dim` as `D` and attempts to call `Quantity.zero[D]`
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
precision and SHALL require no runtime `DimRef`, `DimensionKey`, or total ordering over singleton keys. The evidence
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
- **WHEN** two canonical `Dim` values contain the same singleton-key powers in different tuple orders
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
- **WHEN** two opaque runtime witnesses have equal authoritative `DimensionKey` values but distinct singleton-key types
- **THEN** successful runtime comparison may issue scoped `SameDimension` evidence for explicit `alignTo`

#### Scenario: Keep reflexivity separate from validity
- **WHEN** a malformed `Dim` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity and the no-op `alignTo[D]` MAY be available, but neither creates a dimensional value,
  `DimRef[D]`, or proof that the representation is valid

### Requirement: Atomic and canonical static derivation
The public static dimension language SHALL remain closed over canonical `Dim[Entries]`, expression constructors
`Times[A, B]` and `Inverse[A]`, and `Divide[A, B]` as quotient syntax. A canonical entry SHALL be
`Power[Key, Exponent]`, where `Key <: Singleton` identifies an atom and `Exponent <: Int` is a nonzero singleton integer
literal. `Atom[Key]` SHALL denote `Dim[Power[Key, 1] *: EmptyTuple]`, and `One` SHALL denote `Dim[EmptyTuple]`. An
arbitrary subtype of `Dimension` SHALL NOT silently become a new atomic identity.

A declared canonical `Dim` SHALL contain each singleton key at most once and SHALL store only nonzero literal
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
- **WHEN** a claimed `Dim` contains a zero power, duplicate key, non-`Power` entry, abstract exponent, or unresolved tuple
- **THEN** no authoritative constructor or non-reflexive `SameDimension` derivation accepts the representation

#### Scenario: Preserve expression types in generic code
- **WHEN** generic code multiplies values indexed by abstract `A <: Dimension` and `B <: Dimension`
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

### Requirement: Arbitrary-precision exactness
Quantity coefficients, grid coordinates, rational numerators and denominators, private canonical interpretation, and
runtime `DimensionKey` exponents SHALL retain arbitrary-precision semantics. A declared canonical `Power` exponent
SHALL remain limited to the exact values representable by Scala singleton `Int` literals. Expression-preserving static
arithmetic SHALL not emit a new `Power` literal and therefore SHALL preserve a valid expression even when its
mathematical accumulated exponent lies outside the singleton-`Int` range.

Private equivalence checking and runtime witness algebra SHALL accumulate exponents with `BigInt` and SHALL NOT wrap,
truncate, saturate, or approximate them. Lack of a representable canonical `Dim[Power[K, N]]` spelling for an
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
  `DimensionKey` operation

#### Scenario: Keep runtime-hidden structure opaque
- **WHEN** an opaque runtime dimension's key contains a factor that would cancel a separate static atom
- **THEN** its static path-dependent atom remains opaque and any recovered equivalence requires authoritative runtime
  comparison

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
