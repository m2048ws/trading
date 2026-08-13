# trading-quantities

This Scala 3 module provides exact dimensionful arithmetic, explicit uniform-grid boundaries, runtime identity, and
checked reconstruction.

> A dimension says what is measured.
>
> `Quantity[D]` is an arbitrary exact rational value in that dimension.
>
> `GridQuantity[D, G]` additionally proves membership in one discrete uniform grid.

The primary public vocabulary is:

```scala
Quantity[D]
GridQuantity[D, G]
Rate[From, To]
Ratio
```

## Package layout

| Package | Responsibility |
| --- | --- |
| `trading.quantity` | Core exact quantities, dimensions, grid witnesses, and identifiers |
| `trading.quantity.grid` | Grid evidence, projection, quantization, constrained encoding, quotient/remainder, and allocation |
| `trading.quantity.refinement` | Checked refinements, refined numeric aliases, and refinement-aware operations |
| `trading.quantity.runtime` | Runtime witnesses, registry, heterogeneous values, and logical persistence |
| `trading.quantity.algebra` | Optional Typelevel Algebra integration |

`Quantity[D]` is rational-backed and may represent values that are not on any registered grid, including
`6000.001 USD`, `2 / 100001 XBT`, and `17 / 3 EUR`. `GridQuantity[D, G]` is coordinate-backed: its value is an
arbitrary-precision integer coordinate multiplied by grid `G`'s positive rational quantum.

An asset or dimension does not impose one universal grid. Grids belong to the contexts and operations that accept,
store, trade, transfer, or settle values. Exact calculations between those boundaries may be off-grid. Returning to a
grid is therefore either checked with `narrowExactlyTo` or explicitly lossy with `quantizeTo` and a named policy.

## Exact construction and arithmetic

`Rational` is the project-owned arbitrary-precision scalar. Exact quantities accept rational coefficients, integers,
decimal text, and checked finite Java decimal values through an authoritative `DimRef[D]`. There is no authoritative
`Float` or `Double` constructor. Raw coefficient attachment and arithmetic result construction are lexically private
inside the opaque owner; declaring `package trading.quantity` grants no construction authority.

```scala
import trading.quantity.*

val usd = DimRef.atomic(AtomId("asset:USD"))
val exact: Quantity[usd.D] =
  Quantity(usd.dimension, "6000.001").toOption.get

assert(exact.coefficient == Rational(6_000_001, 1_000))
```

Exact addition and subtraction preserve the dimension. Quantity multiplication combines dimensions, division requires
checked nonzero evidence, same-dimension division produces `Ratio`, and a `Rate[From, To]` has mathematical dimension
`To / From`.

```scala
val btc = DimRef.atomic(AtomId("asset:BTC"))
val amount = Quantity(btc.dimension, Rational(1, 10))
val usdPerBtc: Rate[btc.D, usd.D] =
  Rate(btc.dimension, usd.dimension, Rational(6_000_001, 100))

val notional: Quantity[usd.D] = amount.applyRate(usdPerBtc)
assert(notional.coefficient == Rational(6_000_001, 1_000))
```

Rate composition preserves orientation:

```scala
val eur = DimRef.atomic(AtomId("asset:EUR"))
val usdPerBtc = Rate(btc.dimension, usd.dimension, Rational(60_000))
val eurPerUsd = Rate(usd.dimension, eur.dimension, Rational(9, 10))
val eurPerBtc: Rate[btc.D, eur.D] = usdPerBtc.andThen(eurPerUsd)
```

Quantity division uses the same generic nonzero refinement as every other supported carrier:

```scala
import trading.quantity.refinement.*

val tenUsd = Quantity(usd.dimension, 10)
val threeUsd = Quantity(usd.dimension, 3)
val divisor: NonZero[Quantity[usd.D]] = NonZero(threeUsd).toOption.get
val ratio: Ratio = tenUsd.ratioTo(divisor)
```

Scalar division has deliberately distinct meanings:

- `exactDivideBy` returns `Quantity[D]`;
- `quotRemBy` performs Euclidean division on one grid's integer coordinate;
- `allocateEvenly` conserves a grid coordinate and distributes remainder quanta in an explicit order.

The quotient/remainder and allocation extensions require `import trading.quantity.grid.*` and an explicit matching
grid witness, which retains witness-owned coordinate inspection and construction.

## Grids and explicit projection

A grid is zero-anchored and uniform, with any positive rational quantum. Cent, satoshi, and `0.03` grids are exact and
support coordinates beyond machine integer ranges.

```scala
import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

val cents = UniformGrid.create(
  GridId("USD-cent"),
  GridVersion(1),
  usd.dimension,
  PositiveRational.exact(1, 100).toOption.get
)

val stored: GridQuantity[usd.D, cents.G] = cents.fromCoordinate(600_000)
val embedded: Quantity[usd.D] = stored.asQuantity(cents)

assert(cents.coordinate(stored) == BigInt(600_000))
assert(embedded.coefficient == Rational(6_000))
```

The embedding `GridQuantity[D, G] -> Quantity[D]` is canonical and explicit. There is no implicit global conversion and
no automatic reverse conversion. Raw coordinate attachment and inspection are lexically private; the matching grid
witness owns `fromCoordinate` and `coordinate`, including for callers that declare the core package themselves.

```scala
val source = Quantity(usd.dimension, "6000.001").toOption.get
assert(source.narrowExactlyTo(cents).isLeft)

val result = source.quantizeTo(cents, QuantizationPolicy.HalfEven)
val selected = result.value.asQuantity(cents)
assert(selected.coefficient + result.residual.coefficient == source.coefficient)
```

Grid-to-grid narrowing and quantization first use the canonical exact embedding. Equal quanta do not imply equal grid
identity, and numerical grids derived during calculations do not gain registered identity.

An inverse calculation illustrates why the unrestricted space is necessary:

```scala
val xbt = DimRef.atomic(AtomId("asset:XBT"))
val coefficient = Rational.one./(Rational(100_001, 2)).toOption.get
val inverse: Quantity[xbt.D] = Quantity(xbt.dimension, coefficient)

assert(inverse.coefficient == Rational(2, 100_001))
```

This value is exact even though it is not exactly representable on a satoshi grid. Projection remains an explicit
boundary decision.

The opaque `BigInt` coordinate backing and its performance posture are documented in
[performance notes](docs/performance.md).

## Refinements and algebra

`trading.quantity.refinement` provides one small, closed refinement vocabulary for every supported signed carrier:

```scala
NonNegative[A]
NonZero[A]
Positive[A]
```

`NonNegative`, `NonZero`, and `Positive` check `Int`, `BigInt`, `Rational`, `Quantity[D]`, and
`GridQuantity[D, G]` through a library-owned exact `Sign[A]` capability. Callers cannot construct or extend that
capability, including from `trading.quantity` or `trading.quantity.refinement`. Refinement construction and lawful closure are
lexically owned rather than package-qualified. Refinements are opaque views of the original value, so they add no proof
wrapper or runtime allocation.

Positive implies both nonnegative and nonzero. The explicit fallback weakenings preserve the same runtime value and do
not rerun a predicate:

```scala
val positive: Positive[BigInt] = Positive(BigInt(3)).toOption.get
val nonnegative: NonNegative[BigInt] = positive.asNonNegative
val nonzero: NonZero[BigInt] = positive.asNonZero
val raw: BigInt = positive.unrefined
```

Scalar vocabulary is only a set of readable aliases over these generic states:

```scala
type PositiveWhole = Positive[BigInt]
type NonZeroWhole = NonZero[BigInt]
type PositiveInt = Positive[Int]
type PositiveRational = Positive[Rational]
```

There are no independent scalar proof representations. A `PositiveWhole` weakens to `NonZeroWhole` with `asNonZero`,
and `PositiveInt.toPositiveWhole` performs the total sign-preserving `Int`-to-`BigInt` widening. Operations retain a
refinement only when closure proves it: positive exact division by a positive whole remains positive, while a positive
grid quotient and positive quantization result may be zero and therefore weaken to nonnegative.

Typelevel Algebra remains a published module even though its source development moved into the Cats monorepo. This
project therefore depends directly on the Algebra artifact and uses its standard ring hierarchy; that repository move
does not make Algebra part of `cats-core`.

`ExactScalarField[F]` refines Algebra's `CommutativeRing[F]` with reciprocal that requires `NonZero[F]`. The one
production `ExactScalarField[Rational]` is therefore also the supported commutative-ring instance, without exposing
Algebra `Field[Rational]` or any floating construction. `VectorSpace[V, F]` extends the project-owned `LeftModule[V, F]`,
which extends Algebra's additive commutative group. Consequently one strongest instance supplies each carrier's weaker
structures:

```scala
import trading.quantity.algebra.exactScalarAlgebra.given
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given

// ExactScalarField[Rational] and CommutativeRing[Rational]
// VectorSpace[Quantity[D], Rational], LeftModule, and additive group
// LeftModule[GridQuantity[D, G], BigInt] and additive group
```

Additional opt-in production namespaces expose the runtime dimension group, the exact nonzero-rational group, exact
orders, and the closed refined additive structures:

```scala
import trading.quantity.algebra.dimensionAlgebra.given
import trading.quantity.algebra.nonZeroRationalMultiplicative.given
import trading.quantity.algebra.exactOrders.given
import trading.quantity.algebra.refinedAdditive.given
```

`DimensionKey` is a multiplicative commutative group, and `NonZero[Rational]` is a multiplicative commutative group
whose multiplication and reciprocal are closed without predicate revalidation. Cats Kernel `Order` instances are
available for `Rational`, `Quantity[D]`, and `GridQuantity[D, G]`; they delegate to primitive exact comparisons and do
not participate in refinement construction. Nonnegative exact and grid quantities have additive commutative monoids,
while positive exact and grid quantities have additive commutative semigroups. There is deliberately no additive
structure for `NonZero[A]`.

A direct `cats.arrow.Category[Rate]` is not published. Cats' unconstrained `id[A]` cannot obtain an authoritative
`DimRef[A]`, and the bounded `Rate` alias does not conform cleanly to its unconstrained binary type constructor. Rate
identity remains `Rate.identity(dimension)`, requiring the legitimate witness, and rate associativity and identity are
verified through project-owned laws. There is no production `cats-core` dependency.

The test layer uses Discipline MUnit with the standard Algebra and Cats Kernel law suites plus reusable project-owned
laws for exact reciprocal, modules, vector spaces, graded multiplication, rates, canonicality, projection,
quantization, Euclidean division, allocation, and refinements. Law checks establish the sampled algebraic properties;
they do not prove absence of every defect. Focused malformed-input, boundary, provenance, persistence, serialization,
and downstream package-spoofing regressions remain alongside them. There is no public `Numeric[Quantity[D]]`,
`Ring[Quantity[D]]`, or `Ring[GridQuantity[D, G]]`.

## Runtime identity and registered grid quantities

`QuantityRegistry` owns registered dimension and grid provenance through concrete implementations nested in each
registry's lexical scope. Only successful registry operations create those witnesses. Full grid identity is the
canonical dimension plus `GridId` and `GridVersion`, with one immutable exact quantum interpretation. Checked evidence
rejects foreign registries, unexpected dimensions, unknown versions, and conflicting definitions. Plain generative
grids remain distinct from registered grids and cannot be substituted for packing.

Logical packing is intentionally limited to registered grid quantities:

- `PackedAssetGridQuantity` and `PackedGridQuantity` store identity plus an integer coordinate;
- `ResolvedAssetGridQuantity` and `ResolvedGridQuantity` package checked reconstruction results;
- `ResolvedExactQuantity` represents an unrestricted exact result of heterogeneous arithmetic and is not packed.

There is no logical packed format for arbitrary `Quantity[D]` yet. Such a format needs numerator, denominator,
dimension identity, and an explicit schema. Existing packed records are logical in-memory boundary records, not a
wire-stable format. Invariant-bearing public result and error records fail Java object serialization with the common
project-owned `NotSerializableException` policy, including all three resolved runtime carriers.

## Supported trust boundary

Guarantees apply to well-typed Scala 3 callers using the public API without casts, reflection, `Unsafe`, hand-written
bytecode, foreign-language ABI calls, or constructor-bypassing deserialization. External data enters through checked
parsers, registries, and decoders.

## Verification

```text
sbt -batch quantities/clean
sbt -batch quantities/compile
sbt -batch quantities/Test/compile
sbt -batch quantities/test
sbt -batch quantities/doc
sbt -batch quantities/dependencyTree
sbt -batch adversarialBoundary/Test/compile adversarialBoundary/test
sbt -batch clean scalafmtCheckAll scalafmtSbtCheck compile Test/compile test
openspec validate --all --strict
```

Executable walkthroughs live in
`quantities/src/test/scala/trading/quantity/examples/FoundationExamplesSuite.scala`.
