# trading-quantities

This Scala 3 module provides exact dimensionful arithmetic, explicit uniform-grid boundaries, runtime identity, and
checked reconstruction.

> A dimension says what is measured.
>
> `Quantity[D]` is an arbitrary exact rational value in that dimension.
>
> `GridQuantity[D, G]` additionally proves membership in one discrete uniform grid.

The primary public vocabulary is `Quantity[D]`, `GridQuantity[D, G]`, `Rate[From, To]`, `Ratio`, `DimRef[D]`, and
`SameDimension[A, B]`.

## Package layout

| Package | Responsibility |
| --- | --- |
| `trading.quantity` | Core exact quantities, dimensions, grids, and identifiers |
| `trading.quantity.grid` | Projection, quantization, encoding, quotient/remainder, and allocation |
| `trading.quantity.refinement` | Checked refinements and refinement-aware operations |
| `trading.quantity.runtime` | Runtime witnesses, registry, heterogeneous values, and logical persistence |
| `trading.quantity.algebra` | Optional Typelevel Algebra integration |

## Static types and runtime authority

Three public capabilities remain deliberately independent:

| Capability | Establishes | Does not establish |
| --- | --- | --- |
| `DimRef[D]` | This inhabited `D` has one authoritative `DimensionKey` | A value, grid, or registry provenance |
| `SameDimension[A, B]` | Controlled retagging between equivalent dimension indices | Runtime identity or construction authority |
| `Quantity[D]` / `GridQuantity[D, G]` | A trusted carrier created by an authoritative or checked path | A `DimRef`, grid witness, or registry ownership |

Static equivalence is interpreted privately by the library. There is no public normalization proof family or canonical
output type member. Reflexive `SameDimension[D, D]` is ordinary Scala type identity; it is not a validity certificate
and supplies no runtime authority. Non-reflexive derivation checks both complete closed expressions with
arbitrary-precision exponent arithmetic.

The closed static language is:

```scala
Atom[K]
Dim[Power[K, E] *: ... *: EmptyTuple]
Times[A, B]
Inverse[A]
Divide[A, B]
One
```

Declared `Power` entries require concrete stable singleton keys and nonzero singleton `Int` exponents. Complete
expressions may accumulate exponents outside the `Int` range before later cancellation because the private interpreter
uses `BigInt`. Duplicate or zero declared powers, unresolved keys, malformed tuple tails, and shapes outside the grammar
are rejected. Runtime `DimensionKey` powers are also arbitrary precision.

Literal construction derives authority from the literal type itself:

```scala
type USD = Atom["asset:USD"]
val usd: DimRef[USD] = DimRef.atom["asset:USD"]
val amount: Quantity[USD] = Quantity(usd, Rational(3, 2))
```

A widened string singleton cannot attach caller-selected runtime text to a static atom. Stable nominal atoms own their
runtime identifier:

```scala
object SettlementKey extends DimRef.NominalAtom(AtomId("asset:SETTLEMENT"))
type Settlement = Atom[SettlementKey.type]
val settlement: DimRef[Settlement] = DimRef.atom(SettlementKey)
```

`DimRef.atomic` creates a generative atom witness, while `DimRef.fresh` creates a generative opaque dimension witness
for a supplied runtime key. The latter never guesses a static factorization.

Construction authority and carrier internals are lexically opaque. Merely declaring `package trading.quantity` does
not grant coefficient, coordinate, `DimRef`, grid, or registry construction authority. Possessing a carrier does not
reveal any of those capabilities.

## Exact construction and arithmetic

`Rational` is the project-owned arbitrary-precision scalar. Exact quantities accept rational coefficients, integers,
decimal text, and checked finite Java decimal values through an authoritative `DimRef[D]`. There is no authoritative
`Float` or `Double` constructor.

```scala
val usd = DimRef.atomic(AtomId("asset:USD"))
val exact: Quantity[usd.D] = Quantity(usd.dimension, "6000.001").toOption.get
assert(exact.coefficient == Rational(6_000_001, 1_000))
```

Addition, subtraction, scaling, comparison, refinement, and other dimension-preserving operations need no proof.
Dimension-changing primitive operations preserve the expression spelling uniformly:

```scala
def multiply[A <: Dimension, B <: Dimension](
  left: Quantity[A],
  right: Quantity[B]
): Quantity[Times[A, B]] =
  left * right

def divide[A <: Dimension, B <: Dimension](
  left: Quantity[A],
  right: NonZero[Quantity[B]]
): Quantity[Divide[A, B]] =
  left.divideBy(right)
```

The same rule applies to grid/exact mixed multiplication and to `DimRef.times`, `DimRef.inverse`, and `DimRef.divide`.
The result retains both its raw expression-typed carrier and, when runtime witnesses are involved, the matching raw
expression witness.

When an API wants a nominated equivalent spelling, select it explicitly with `SameDimension`:

```scala
def notional[A <: Dimension, B <: Dimension, Out <: Dimension](
  amount: Quantity[A],
  price: Quantity[B]
)(using SameDimension[Times[A, B], Out]): Quantity[Out] =
  (amount * price).alignTo[Out]
```

There is no implicit global retagging. Homogeneous generic code simply preserves its input type:

```scala
def total[D <: Dimension](left: Quantity[D], right: Quantity[D]): Quantity[D] =
  left + right
```

Empty construction is authority-bearing. A local `DimRef[D]` is therefore required:

```scala
def empty[D <: Dimension](using dimension: DimRef[D]): Quantity[D] =
  Quantity.zero[D]
```

The same requirement applies to `GridQuantity.zero`, refined nonnegative zeros, and algebra instances that manufacture
an additive identity. Missing, ambiguous, or malformed authority is rejected.

## Rates and ratios

`Rate[From, To]` has mathematical dimension `Divide[To, From]`. Endpoint-oriented operations retain their declared
endpoint types directly, including path-dependent runtime endpoints:

```scala
val btc = DimRef.atomic(AtomId("asset:BTC"))
val eur = DimRef.atomic(AtomId("asset:EUR"))
val usdPerBtc: Rate[btc.D, usd.D] = Rate(btc.dimension, usd.dimension, Rational(60_000))
val eurPerUsd: Rate[usd.D, eur.D] = Rate(usd.dimension, eur.dimension, Rational(9, 10))

val amount = Quantity(btc.dimension, Rational(1, 10))
val eurPerBtc: Rate[btc.D, eur.D] = usdPerBtc.andThen(eurPerUsd)
val euros: Quantity[eur.D] = amount.applyRate(eurPerBtc)
```

Raw multiplication deliberately remains expression-typed:

```scala
val raw: Quantity[Times[btc.D, Divide[usd.D, btc.D]]] = amount * usdPerBtc
val nominated: Quantity[usd.D] = amount.applyRate(usdPerBtc)
```

Checked reciprocal and common-target cross-rate preserve endpoints without casts or contextual proof parameters:

```scala
val nonzero = NonZero(usdPerBtc).toOption.get
val btcPerUsd: Rate[usd.D, btc.D] = nonzero.reciprocalRate

val eth = DimRef.atomic(AtomId("asset:ETH"))
val usdPerEth: Rate[eth.D, usd.D] = Rate(eth.dimension, usd.dimension, Rational(3_000))
val ethPerBtc: Rate[btc.D, eth.D] = usdPerBtc.crossRate(NonZero(usdPerEth).toOption.get)
```

`ratioTo` returns `Ratio` for same-dimension values. Grid helpers delegate through their supplied grid witnesses and
retain the same endpoint semantics. Rate values are explicit values, never ambient givens. This module does not add
instrument, order, position, venue, payoff, or conversion-graph domain models.

## Grids and explicit projection

A grid is zero-anchored and uniform, with any positive rational quantum. An asset or dimension does not impose one
universal grid; grids belong to storage, trading, transfer, or settlement boundaries.

```scala
import trading.quantity.grid.*

val cents = UniformGrid.create(
  GridId("USD-cent"),
  GridVersion(1),
  usd.dimension,
  PositiveRational.exact(1, 100).toOption.get
)
val stored: GridQuantity[usd.D, cents.G] = cents.fromCoordinate(600_000)
val embedded: Quantity[usd.D] = stored.asQuantity(cents)
```

The embedding is explicit and canonical. Returning to a grid is either checked with `narrowExactlyTo` or explicitly
lossy with `quantizeTo` and a named policy. Equal quanta do not imply equal grid identity. Coordinate construction and
inspection remain owned by the matching witness.

## Refinements and algebra

The refinement vocabulary is `NonNegative[A]`, `NonZero[A]`, and `Positive[A]`. Library-owned `Sign[A]` instances check
`Int`, `BigInt`, `Rational`, `Quantity[D]`, and `GridQuantity[D, G]`; callers cannot extend that authority. Refinements
are opaque views and retain only operations whose result is closed under the predicate.

Opt-in Algebra instances require runtime authority only when they need an identity:

```scala
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given

def exactSpace[D <: Dimension](using DimRef[D]) =
  summon[VectorSpace[Quantity[D], Rational]]

def gridModule[D <: Dimension, G](using DimRef[D]) =
  summon[LeftModule[GridQuantity[D, G], BigInt]]
```

Positive quantities expose combine-only semigroups and need no empty-value authority. There is no additive structure
for `NonZero[A]`, no public `Numeric[Quantity[D]]`, and no ring for dimensionful quantities.

## Runtime identity and heterogeneous values

`QuantityRegistry` owns registered dimension and grid provenance. Checked reconstruction rejects foreign registries,
unexpected dimensions, unknown versions, and conflicting definitions. Heterogeneous dimension-changing results retain
the raw expression-typed value together with its matching authoritative `DimRef`; endpoint-oriented operations retain
their declared target witnesses.

`PackedAssetGridQuantity` and `PackedGridQuantity` store logical identity plus an integer coordinate.
`ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, and `ResolvedExactQuantity` package checked results. Their runtime
and logical wire representations are unchanged; arbitrary exact quantities still have no packed wire format.

Two independently obtained witnesses can be reconciled with `SameDimension.between`. Successful checked evidence can
then align a value for homogeneous arithmetic. Reflexive static identity is never used as runtime authority.

## Migration notes

This generation removes the former public normalization trait, companion, associated `Aux` alias, derived evidence,
and canonical-output API. It is a source and JVM-binary break; rebuild downstream clients.

Typical migrations are:

| Before | Now |
| --- | --- |
| Generic product returns an associated canonical output | Return `Quantity[Times[A, B]]` |
| Generic quotient/inverse asks for output evidence | Return `Divide[A, B]` / `Inverse[A]` directly |
| Multiplication silently names a settlement dimension | Keep the raw expression, then `alignTo[Settlement]` with `SameDimension` |
| Ordinary rate multiplication returns the endpoint | Use `applyRate`; raw `*` remains expression-typed |
| Generic zeros derive static validity | Require `using DimRef[D]` |
| Identity-bearing algebra works from static evidence | Put an authoritative `DimRef[D]` in local scope |
| Cross-rate or rate composition forwards proof parameters | Call the endpoint method directly |
| Preserving operations accept explicit evidence arguments | Remove those arguments |

## Supported trust boundary

Guarantees apply to well-typed Scala 3 callers using the public API without casts, reflection, `Unsafe`, hand-written
bytecode, foreign-language ABI calls, or constructor-bypassing deserialization. External data enters through checked
parsers, registries, and decoders.

## Verification

```text
sbt -batch clean test
sbt -batch scalafmtCheckAll scalafmtSbtCheck
sbt -batch quantities/doc
openspec validate --all --strict
```

Executable walkthroughs live in
`quantities/src/test/scala/trading/quantity/examples/FoundationExamplesSuite.scala`.
