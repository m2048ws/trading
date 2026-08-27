package trading.quantity

import scala.annotation.StaticAnnotation

import munit.FunSuite

import trading.quantity.refinement.ExpectedNonZero
import trading.quantity.refinement.NonZero
import trading.quantity.testkit.CompileAssertions.*

class StaticDimensionNormalizationSuite extends FunSuite:
  type A = Atom["static:A"]
  type B = Atom["static:B"]
  type C = Atom["static:C"]

  object NominalKey      extends DimRef.NominalAtom(AtomId("static:nominal"))
  object OtherNominalKey extends DimRef.NominalAtom(AtomId("static:other-nominal"))
  type Nominal      = Atom[NominalKey.type]
  type OtherNominal = Atom[OtherNominalKey.type]

  private val a: DimRef[A] = DimRef.atom["static:A"]
  private val b: DimRef[B] = DimRef.atom["static:B"]
  private val c: DimRef[C] = DimRef.atom["static:C"]

  final class Marker extends StaticAnnotation

  test("atoms and identity remain declared canonical Canonical aliases"):
    assertSameType[A, Canonical[Power["static:A", 1] *: EmptyTuple]]
    assertSameType[One, Canonical[EmptyTuple]]

  test("private interpretation proves association, commutation, cancellation, and tuple permutation"):
    type LeftAssociated  = Times[Times[A, B], C]
    type RightAssociated = Times[A, Times[C, B]]
    type Cancelled       = Times[Times[A, B], Inverse[Times[B, A]]]
    type Ordered         = Canonical[Power["static:A", 1] *: Power["static:B", -2] *: EmptyTuple]
    type Permuted        = Canonical[Power["static:B", -2] *: Power["static:A", 1] *: EmptyTuple]

    assert(summon[SameDimension[LeftAssociated, RightAssociated]].ne(null))
    assert(summon[SameDimension[Cancelled, One]].ne(null))
    assert(summon[SameDimension[Ordered, Permuted]].ne(null))

  test("transparent aliases and annotations have the underlying interpretation"):
    type Product   = Times[A, B]
    type Alias     = Product
    type Annotated = Alias @Marker

    assert(summon[SameDimension[Product, Annotated]].ne(null))

  test("private interpretation compares out-of-Int accumulated powers exactly"):
    type Maximum = Canonical[Power["static:boundary", 2147483647] *: EmptyTuple]
    type Left    = Times[Maximum, Atom["static:boundary"]]
    type Right   = Times[Atom["static:boundary"], Maximum]
    type Reduced = Times[Left, Inverse[Maximum]]

    assert(summon[SameDimension[Left, Right]].ne(null))
    assert(summon[SameDimension[Reduced, Atom["static:boundary"]]].ne(null))

  test("DimRef expression algebra agrees exactly with runtime DimKey algebra"):
    val identity: DimRef[One]                = DimRef.one
    val product: DimRef[Times[A, B]]         = DimRef.times(a, b)
    val inverse: DimRef[Inverse[A]]          = DimRef.inverse(a)
    val quotient: DimRef[Divide[A, B]]       = DimRef.divide(a, b)
    val left: DimRef[Times[Times[A, B], C]]  = DimRef.times(product, c)
    val right: DimRef[Times[A, Times[B, C]]] = DimRef.times(a, DimRef.times(b, c))

    assertEquals(identity.key, DimKey.one)
    assertEquals(product.key, DimKey.multiply(a.key, b.key))
    assertEquals(inverse.key, DimKey.inverse(a.key))
    assertEquals(quotient.key, DimKey.multiply(a.key, DimKey.inverse(b.key)))
    assertEquals(left.key, right.key)
    assert(summon[SameDimension[Times[Times[A, B], C], Times[A, Times[B, C]]]].ne(null))

  test("runtime witness algebra preserves arbitrary-precision powers and cancellation"):
    val exponent  = BigInt(Int.MaxValue) + 1
    val runtime   = DimRef.fresh(DimKey(List(AtomId("static:runtime-bigint") -> exponent)))
    val inverse   = DimRef.inverse(runtime.dimension)
    val cancelled = DimRef.times(runtime.dimension, inverse)

    assertEquals(runtime.dimension.key.powers.head._2, exponent)
    assertEquals(inverse.key.powers.head._2, -exponent)
    assertEquals(cancelled.key, DimKey.one)

  test("nominal, generative, and fresh witnesses retain exact stable identities"):
    val first: DimRef[Nominal]      = DimRef.atom(NominalKey)
    val second: DimRef[Nominal]     = DimRef.atom(NominalKey)
    val other: DimRef[OtherNominal] = DimRef.atom(OtherNominalKey)
    val generated                   = DimRef.atomic(AtomId("static:generated"))
    val fresh                       = DimRef.fresh(DimKey.atom(AtomId("static:fresh")))

    assertEquals(first.key, second.key)
    assertNotEquals(first.key, other.key)
    assertEquals(generated.dimension.key, DimKey.atom(generated.atomId))
    assertEquals(fresh.dimension.key, DimKey.atom(AtomId("static:fresh")))

  test("generic expression results need no output evidence and nominated outputs use SameDimension"):
    def multiply[X <: Dim, Y <: Dim](left: Quantity[X], right: Quantity[Y]): Quantity[Times[X, Y]] =
      left * right

    def nominate[X <: Dim, Y <: Dim, O <: Dim](
      left: Quantity[X],
      right: Quantity[Y]
    )(using SameDimension[Times[X, Y], O]
    ): Quantity[O] =
      multiply(left, right).alignTo[O]

    type AB = Canonical[Power["static:A", 1] *: Power["static:B", 1] *: EmptyTuple]
    val raw: Quantity[Times[A, B]] = multiply(Quantity(a, 2), Quantity(b, 3))
    val named: Quantity[AB]        = nominate[A, B, AB](Quantity(a, 2), Quantity(b, 3))

    assertEquals(raw.coefficient, Rational(6))
    assertEquals(named.coefficient, Rational(6))

  test("semantic rate operations retain named endpoints"):
    val amount: Quantity[A]    = Quantity(a, Rational(1, 10))
    val aToB: Rate[A, B]       = Rate(a, b, Rational(60_000))
    val bToC: Rate[B, C]       = Rate(b, c, Rational(9, 10))
    val aToC: Rate[A, C]       = aToB.andThen(bToC)
    val reciprocal: Rate[C, A] = NonZero(aToC).toOption.get.reciprocalRate

    assertEquals(amount.applyRate(aToC).coefficient, Rational(5_400))
    assertEquals(reciprocal.coefficient, Rational(1, 54_000))
    assertEquals(NonZero(Rate(a, c, Rational.zero)), Left(ExpectedNonZero))

  test("malformed declared dimensions and unequal dimensions are rejected non-reflexively"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Bad = Canonical[Power["zero", 0] *: EmptyTuple]
      SameDimension.derived[Bad, One]
      """,
      "zero exponents"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Bad = Canonical[Power["duplicate", 1] *: Power["duplicate", 2] *: EmptyTuple]
      SameDimension.derived[Bad, One]
      """,
      "keys must be unique"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      summon[SameDimension[Atom["left"], Atom["right"]]]
      """,
      "not equivalent"
    )

  test("unresolved generic singleton keys are not interpreted as concrete identities"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      def compare[K <: Singleton] = SameDimension.derived[Atom[K], Atom["known"]]
      """,
      "concrete stable singleton"
    )

  test("reflexive equivalence remains type identity rather than validity"):
    assertCompiles(
      """
      import trading.quantity.*
      type Bad = Canonical[Power["zero", 0] *: EmptyTuple]
      summon[SameDimension[Bad, Bad]]
      """
    )

  test("the removed public normalization family and evidence constructors are unavailable"):
    assertDoesNotCompile(
      """
      import trading.quantity.*
      summon[Normalize[One]]
      """
    )
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type X = Atom["forge:X"]
      val forged = new SameDimension[X, X] {}
      """

end StaticDimensionNormalizationSuite
