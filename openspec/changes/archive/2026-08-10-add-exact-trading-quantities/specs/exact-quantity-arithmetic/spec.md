## ADDED Requirements

### Requirement: Public exact quantity model
The public unrestricted quantity type SHALL be `Quantity[D]`, where `D <: Dimension`. It SHALL have canonical
`trading.quantity.Rational` coefficient semantics and SHALL NOT expose a representation type parameter, public generic
carrier, or alternate public exact-quantity kind. `GridQuantity[D, G]` SHALL be the separate grid-proven carrier.

#### Scenario: Express an arbitrary exact quantity
- **WHEN** a caller declares a value with coefficient `17/3` in the EUR dimension
- **THEN** the public type is `Quantity[EUR]` and its coefficient is preserved exactly

#### Scenario: Public signatures hide representation choices
- **WHEN** supported external Scala inspects constructors and arithmetic results
- **THEN** exact values are expressed as `Quantity[D]` with no compiler-visible representation parameter

### Requirement: Exact quantity construction
`Quantity` SHALL provide dimension-witnessed construction from `Rational`, `BigInt`, decimal text, and finite
`java.math.BigDecimal`, plus dimension-polymorphic zero. The scalar accessor SHALL return the canonical `Rational`.
No authoritative constructor SHALL accept `Float` or `Double`, and raw opaque reconstruction SHALL remain unavailable
to supported callers, including downstream source that declares `package trading.quantity`. Raw coefficient attachment and
operation-result construction SHALL be lexically private within the `Quantity` opaque owner; package-qualified
visibility SHALL NOT be a construction boundary. Except for polymorphic zero, caller-supplied coefficients SHALL
require an authoritative `DimRef[D]`, and arithmetic results SHALL be derived only from legitimate operands and checked
evidence.

#### Scenario: Construct exact decimal text
- **WHEN** `Quantity.fromDecimal` receives `6000.001` with a USD dimension witness
- **THEN** it returns a `Quantity[USD]` with coefficient `6000001/1000`

#### Scenario: Reject floating construction
- **WHEN** supported Scala attempts to construct an exact quantity from `0.1d` or `0.1f`
- **THEN** the code does not compile

#### Scenario: Reject same-package raw coefficient attachment
- **WHEN** downstream Scala declares `package trading.quantity` and supplies an arbitrary coefficient to a raw or
  operation-result construction helper
- **THEN** lexical privacy prevents construction of a chosen `Quantity[D]`

### Requirement: Rational parsing and finite-decimal bounds
Rational text SHALL use a strict complete integer, decimal, or fraction grammar and canonical normalization. Finite
decimal construction SHALL return `UnsupportedFiniteDecimalScale` before materializing a power of ten when the absolute
scale exceeds `1,000,000` or the scale is `Int.MinValue`.

#### Scenario: Reject malformed rational text
- **WHEN** text contains repeated signs, missing digit components, malformed separators, or a zero denominator
- **THEN** parsing fails without reinterpreting it as another value

#### Scenario: Reject the minimum scale safely
- **WHEN** a Java decimal has scale `Int.MinValue`
- **THEN** exact construction returns `UnsupportedFiniteDecimalScale` without overflowing the scale check

### Requirement: Arbitrary-precision exactness
Quantity coefficients, grid coordinates, rational numerators and denominators, and normalized dimension exponents SHALL
use arbitrary-precision semantics. Arithmetic MUST NOT silently overflow, wrap, truncate, saturate, or approximate.

#### Scenario: Denominator grows during arithmetic
- **WHEN** an exact calculation produces a denominator absent from either input
- **THEN** the normalized rational result is preserved exactly

#### Scenario: Dimension exponent exceeds machine range
- **WHEN** dimension operations produce an exponent outside a machine integer range
- **THEN** the canonical dimension remains correct

### Requirement: Dimension-safe additive and multiplicative arithmetic
`Quantity[D] + Quantity[D]` and subtraction SHALL return `Quantity[D]`. Multiplication by `Rational` SHALL preserve
`D`, while `Quantity[A] * Quantity[B]` SHALL return `Quantity[Times[A, B]]`. Unlike-dimension addition SHALL not be
available without checked dimension evidence.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values

#### Scenario: Multiply dimensions
- **WHEN** exact quantities in dimensions `A` and `B` are multiplied
- **THEN** the exact result has public type `Quantity[Times[A, B]]`

### Requirement: Exact rates and ratios
`Rate[From, To]` SHALL alias `Quantity[Divide[To, From]]`, and `Ratio` SHALL alias `Quantity[One]`. Rate construction,
application, and composition SHALL preserve this orientation. Applying `Rate[From, To]` to `Quantity[From]` SHALL
return `Quantity[To]`, and composing `Rate[A, B]` with `Rate[B, C]` SHALL return `Rate[A, C]`. Identity rates SHALL
require an authoritative `DimRef[D]`; composition SHALL be associative and SHALL preserve left and right identity.

#### Scenario: Apply a mathematical rate
- **WHEN** `0.1 BTC` is acted on by `60000.01 USD/BTC`
- **THEN** the result is exact `6000.001 USD` as `Quantity[USD]`

#### Scenario: Compose rates
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** the result is a `Rate[A, C]` with the exact product coefficient

### Requirement: Checked quantity division
Division by a quantity SHALL require `NonZero[Quantity[B]]`. Dividing `Quantity[A]` by that evidence SHALL return
`Quantity[Divide[A, B]]`; equal-dimension division SHALL expose `Ratio`. A grid divisor SHALL first use its canonical
exact embedding and then the same generic `NonZero.from` check. No separate divisor carrier or validator SHALL exist.

#### Scenario: Produce a ratio
- **WHEN** exact `10 USD` is divided by checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3`

#### Scenario: Reject zero divisor evidence
- **WHEN** `NonZero.from` receives a zero exact quantity or the exact embedding of a zero grid quantity
- **THEN** it fails and division remains unavailable

### Requirement: Scalar division meanings remain distinct
`Quantity[D].exactDivideBy` and `GridQuantity[D, G].exactDivideBy` SHALL return `Quantity[D]`. With an explicit matching
grid witness, grid-only `quotRemBy` SHALL return `trading.quantity.grid.QuotRem[GridQuantity[D, G]]`, and grid-only
`allocateEvenly` SHALL return `trading.quantity.grid.Allocation[GridQuantity[D, G]]`. The two secondary extension
operations SHALL be supplied by `trading.quantity.grid`, not `trading.quantity`. These operations SHALL preserve their
respective exact-division, Euclidean, and ordered-allocation conservation laws without ambiguous shared syntax.

#### Scenario: Exact scalar division leaves a grid
- **WHEN** a grid-proven `10.00 USD` is exact-divided by three
- **THEN** the result is exact `10/3 USD` as `Quantity[USD]`

#### Scenario: Euclidean coordinate division
- **WHEN** coordinate `1000` is quotient/remainder divided by positive whole three
- **THEN** quotient `333` and remainder `1` remain on the same grid and reconstruct the source

#### Scenario: Ordered allocation conserves coordinates
- **WHEN** coordinate `1000` is evenly allocated among three recipients first-to-last
- **THEN** coordinates `334`, `333`, and `333` are returned and sum to `1000`

### Requirement: Checked mathematical refinements
The public refinement lattice SHALL consist only of `NonNegative[A]`, `NonZero[A]`, and `Positive[A]`.
`NonNegative.from`, `NonZero.from`, and `Positive.from` SHALL generically check `Int`, `BigInt`, `Rational`,
`Quantity[D]`, and `GridQuantity[D, G]` using a deterministic exact library-owned `Sign[A]`. `Sign` SHALL be final,
privately constructible, and unavailable for downstream implementation or custom instances, including source in
`trading.quantity` or `trading.quantity.refinement`. Refinement representation, weakening, and operation-specific lawful
closure SHALL be lexically owned; no package-qualified visibility SHALL confer unchecked construction authority. The
canonical predicate failures SHALL be `ExpectedNonNegative`, `ExpectedNonZero`, and `ExpectedPositive`.

The refinements SHALL be zero-allocation opaque views. `Positive[A]` SHALL weaken to `NonNegative[A]` and `NonZero[A]`
without a predicate check, allocation, `Either`, implicit conversion, or public unchecked constructor. When direct
subtyping would make public extension selection ambiguous, explicit `asNonNegative` and `asNonZero` weakenings SHALL
provide this implication. One `unrefined` operation SHALL recover the underlying value.

`PositiveWhole`, `NonZeroWhole`, `PositiveInt`, and `PositiveRational` SHALL be aliases for `Positive[BigInt]`,
`NonZero[BigInt]`, `Positive[Int]`, and `Positive[Rational]`, respectively, with facade constructors delegating to the
generic checks. Widening a `PositiveInt` to `PositiveWhole` SHALL preserve positivity without revalidation. Exact
positive-rational construction SHALL report a zero denominator distinctly from `ExpectedPositive`; floating
constructors SHALL remain unavailable. Runtime numeric values SHALL NOT be replaced by type-level naturals.

Refinement operations SHALL retain a predicate only when algebraic closure guarantees it and SHALL otherwise return an
unrestricted value, a weaker refinement, or a checked result. Lawful closure and weakening SHALL be constructed
lexically without runtime revalidation or production proof-recovery `.toOption.get` calls.

#### Scenario: Construct supported generic refinements
- **WHEN** supported values of each signed carrier are passed to the three generic constructors
- **THEN** their exact sign determines success or the corresponding canonical predicate failure

#### Scenario: Weaken a positive value
- **WHEN** a checked `Positive[A]` is used as nonnegative or nonzero evidence
- **THEN** the same runtime value is returned without rechecking its sign

#### Scenario: Use scalar aliases
- **WHEN** a caller constructs a positive whole, nonzero whole, allocation count, or positive grid quantum
- **THEN** the result is the corresponding generic refinement rather than an independent carrier

#### Scenario: Positive exact division remains positive
- **WHEN** a positive `Quantity[D]` is exact-divided by a positive whole
- **THEN** the result remains `Positive[Quantity[D]]`

#### Scenario: Grid quotient may weaken
- **WHEN** a positive or nonzero grid coordinate is quotient-divided by a larger positive whole
- **THEN** the quotient may be zero and does not retain an invalid stronger refinement

#### Scenario: Subtraction may change sign
- **WHEN** two nonnegative quantities are subtracted without an ordering proof
- **THEN** the result is unrestricted or separately checked

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

#### Scenario: Import exact quantity algebra
- **WHEN** a caller imports `trading.quantity.algebra.exactQuantityAlgebra.given`
- **THEN** exact vector-space operations are available without floating scalar construction

#### Scenario: Reuse the strongest quantity structure
- **WHEN** a caller requests a left module or additive commutative group after importing exact quantity algebra
- **THEN** the production vector-space instance supplies that weaker structure without a competing group instance

#### Scenario: Reciprocal is total after evidence
- **WHEN** `ExactScalarField[Rational].reciprocal` receives `NonZero[Rational]`
- **THEN** it returns the exact reciprocal without a division-by-zero branch

#### Scenario: Multiply nonzero rationals
- **WHEN** two `NonZero[Rational]` values are multiplied or one is reciprocated
- **THEN** the result remains nonzero without rerunning the predicate

#### Scenario: Preserve graded quantity multiplication
- **WHEN** quantities are multiplied associatively or distributively across dimension expressions
- **THEN** coefficients obey the commutative graded-algebra laws and canonical dimension equality reconciles expression shape

#### Scenario: Imports do not change arithmetic meaning
- **WHEN** algebra or rounding-policy values are imported
- **THEN** existing quantity operations keep the same result types and numerical semantics

### Requirement: Supported Scala trust and serialization boundary
Construction guarantees SHALL apply to well-typed supported Scala 3 callers without casts, reflection, unsafe JVM
access, hand-written bytecode, or constructor-bypassing deserialization. Java object serialization SHALL fail closed
through the common project-owned `NotSerializableException` mechanism for invariant-bearing public result and error
records, nominal and logical packed records, and dependent resolved runtime carriers; project-owned checked logical
decoders remain supported.

#### Scenario: Reject Java serialization
- **WHEN** an invariant-bearing identifier, grid definition, result record, error record, or grid-packed record is serialized
- **THEN** Java serialization fails instead of creating an unchecked reconstruction path

#### Scenario: Use checked logical decoding
- **WHEN** a logical grid-packed record is passed directly to its project-owned decoder
- **THEN** runtime identity and provenance checks proceed normally
