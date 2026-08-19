package trading.quantity

import scala.annotation.StaticAnnotation

import munit.FunSuite

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

  test("atoms and identity are canonical Dim aliases"):
    assertSameType[A, Dim[Power["static:A", 1] *: EmptyTuple]]
    assertSameType[One, Dim[EmptyTuple]]

    val atomNormalization = Normalize.derived[A]
    val oneNormalization  = Normalize.derived[One]

    assertSameType[atomNormalization.Out, A]
    assertSameType[oneNormalization.Out, One]

  test("product normalization preserves first occurrence order"):
    val normalized = Normalize.derived[Times[A, Times[B, C]]]

    assertSameType[
      normalized.Out,
      Dim[Power["static:A", 1] *: Power["static:B", 1] *: Power["static:C", 1] *: EmptyTuple]
    ]

  test("first-occurrence order is global and independent of Times association"):
    type LeftAssociated  = Times[Times[A, Inverse[A]], Times[B, A]]
    type RightAssociated = Times[A, Times[Inverse[A], Times[B, A]]]
    type Expected        = Dim[Power["static:A", 1] *: Power["static:B", 1] *: EmptyTuple]

    val left: Normalize.Aux[LeftAssociated, Expected]   = Normalize.derived[LeftAssociated]
    val right: Normalize.Aux[RightAssociated, Expected] = Normalize.derived[RightAssociated]

    assertSameType[left.Out, right.Out]

  test("normalization merges powers and removes exact zero"):
    type A2 = Dim[Power["static:A", 2] *: EmptyTuple]

    val reduced   = Normalize.derived[Times[A2, Inverse[A]]]
    val cancelled = Normalize.derived[Times[A2, Inverse[A2]]]

    assertSameType[reduced.Out, A]
    assertSameType[cancelled.Out, One]

  test("inverse negates every literal exponent exactly"):
    type Input    = Dim[Power["static:A", 2] *: Power["static:B", -3] *: EmptyTuple]
    type Expected = Dim[Power["static:A", -2] *: Power["static:B", 3] *: EmptyTuple]

    val normalized = Normalize.derived[Inverse[Input]]
    assertSameType[normalized.Out, Expected]

  test("transparent aliases and annotations do not define identities"):
    type Product   = Times[A, B]
    type Annotated = Product @Marker

    val direct    = Normalize.derived[Product]
    val annotated = Normalize.derived[Annotated]

    assertSameType[direct.Out, annotated.Out]

  test("SameDimension compares canonical entries modulo permutation"):
    val same = summon[SameDimension[Times[A, B], Times[B, A]]]
    assert(same.ne(null))

  test("DimRef identity, product, inverse, quotient, cancellation, and association agree with runtime keys"):
    type AB       = Dim[Power["static:A", 1] *: Power["static:B", 1] *: EmptyTuple]
    type ABC      = Dim[Power["static:A", 1] *: Power["static:B", 1] *: Power["static:C", 1] *: EmptyTuple]
    type AInverse = Dim[Power["static:A", -1] *: EmptyTuple]
    type APerB    = Dim[Power["static:A", 1] *: Power["static:B", -1] *: EmptyTuple]

    val identity: DimRef[One]         = DimRef.one
    val leftIdentity: DimRef[A]       = DimRef.times(identity, a)
    val product: DimRef[AB]           = DimRef.times(a, b)
    val inverse: DimRef[AInverse]     = DimRef.inverse(a)
    val quotient: DimRef[APerB]       = DimRef.divide(a, b)
    val cancelled: DimRef[A]          = DimRef.times(product, DimRef.inverse(b))
    val leftAssociated: DimRef[ABC]   = DimRef.times(DimRef.times(a, b), c)
    val rightAssociated: DimRef[ABC]  = DimRef.times(a, DimRef.times(b, c))
    val quotientIdentity: DimRef[One] = DimRef.divide(a, a)

    assertEquals(identity.key, DimensionKey.one)
    assertEquals(leftIdentity.key, DimensionKey.multiply(DimensionKey.one, a.key))
    assertEquals(product.key, DimensionKey.multiply(a.key, b.key))
    assertEquals(inverse.key, DimensionKey.inverse(a.key))
    assertEquals(quotient.key, DimensionKey.multiply(a.key, DimensionKey.inverse(b.key)))
    assertEquals(cancelled.key, DimensionKey.multiply(product.key, DimensionKey.inverse(b.key)))
    assertEquals(cancelled.key, a.key)
    assertEquals(leftAssociated.key, DimensionKey.multiply(DimensionKey.multiply(a.key, b.key), c.key))
    assertEquals(rightAssociated.key, DimensionKey.multiply(a.key, DimensionKey.multiply(b.key, c.key)))
    assertEquals(leftAssociated.key, rightAssociated.key)
    assertEquals(quotientIdentity.key, DimensionKey.one)

  test("generative and fresh witnesses expose concrete singleton-key atoms"):
    val generated = DimRef.atomic(AtomId("static:generated"))
    val fresh     = DimRef.fresh(DimensionKey.atom(AtomId("static:fresh")))

    type Generated2   = Dim[Power[generated.type, 2] *: EmptyTuple]
    type FreshInverse = Dim[Power[fresh.type, -1] *: EmptyTuple]

    val squared: DimRef[Generated2]    = DimRef.times(generated.dimension, generated.dimension)
    val inverted: DimRef[FreshInverse] = DimRef.inverse(fresh.dimension)

    assertEquals(squared.key, DimensionKey(List(AtomId("static:generated") -> BigInt(2))))
    assertEquals(inverted.key, DimensionKey(List(AtomId("static:fresh") -> BigInt(-1))))

  test("nominal singleton keys own one authoritative runtime atom"):
    val first: DimRef[Nominal]  = DimRef.atom(NominalKey)
    val second: DimRef[Nominal] = DimRef.atom(NominalKey)

    assertEquals(first.key, DimensionKey.atom(NominalKey.atomId))
    assertEquals(second.key, first.key)

  test("every supported repeated static atom key has one runtime identity"):
    val oneFirst: DimRef[One]                = DimRef.one
    val oneSecond: DimRef[One]               = DimRef.one
    val literalFirst: DimRef[A]              = DimRef.atom["static:A"]
    val literalSecond: DimRef[A]             = DimRef.atom["static:A"]
    val nominalFirst: DimRef[Nominal]        = DimRef.atom(NominalKey)
    val nominalSecond: DimRef[Nominal]       = DimRef.atom(NominalKey)
    val generated                            = DimRef.atomic(AtomId("static:authority-generated"))
    val generatedFirst: DimRef[generated.D]  = generated.dimension
    val generatedSecond: DimRef[generated.D] = generated.dimension
    val fresh                                = DimRef.fresh(DimensionKey.atom(AtomId("static:authority-fresh")))
    val freshFirst: DimRef[fresh.D]          = fresh.dimension
    val freshSecond: DimRef[fresh.D]         = fresh.dimension

    assertEquals(oneFirst.key, oneSecond.key)
    assertEquals(literalFirst.key, literalSecond.key)
    assertEquals(nominalFirst.key, nominalSecond.key)
    assertEquals(generatedFirst.key, generatedSecond.key)
    assertEquals(freshFirst.key, freshSecond.key)
    assert(SameDimension.between(oneFirst, oneSecond).nonEmpty)
    assert(SameDimension.between(literalFirst, literalSecond).nonEmpty)
    assert(SameDimension.between(nominalFirst, nominalSecond).nonEmpty)
    assert(SameDimension.between(generatedFirst, generatedSecond).nonEmpty)
    assert(SameDimension.between(freshFirst, freshSecond).nonEmpty)

  test("distinct nominal identities have distinct static paths and runtime keys"):
    val first: DimRef[Nominal]       = DimRef.atom(NominalKey)
    val second: DimRef[OtherNominal] = DimRef.atom(OtherNominalKey)

    assertNotEquals(first.key, second.key)
    assertEquals(SameDimension.between(first, second), None)

  test("opaque runtime witnesses preserve exponents outside the static Int range"):
    val exponent = BigInt(Int.MaxValue) + 1
    val runtime  = DimRef.fresh(DimensionKey(List(AtomId("static:runtime-bigint") -> exponent)))
    val inverse  = DimRef.inverse(runtime.dimension)

    assertEquals(runtime.dimension.key.powers.head._2, exponent)
    assertEquals(inverse.key.powers.head._2, -exponent)

  test("generic code forwards one Normalize.Aux result"):
    def multiply[X <: Dimension, Y <: Dimension, O <: Dimension](
      left: Quantity[X],
      right: Quantity[Y]
    )(using Normalize.Aux[Times[X, Y], O]
    ): Quantity[O] =
      left * right

    type AB = Dim[Power["static:A", 1] *: Power["static:B", 1] *: EmptyTuple]
    val normalization: Normalize.Aux[Times[A, B], AB] = Normalize.derived[Times[A, B]]
    val result: Quantity[AB] = multiply[A, B, AB](Quantity(a, 2), Quantity(b, 3))(using normalization)

    assertEquals(result.coefficient, Rational(6))

  test("generic quotient code forwards one Normalize.Aux result"):
    def quotient[X <: Dimension, Y <: Dimension, O <: Dimension](
      numerator: Quantity[X],
      denominator: trading.quantity.refinement.NonZero[Quantity[Y]]
    )(using Normalize.Aux[Divide[X, Y], O]
    ): Quantity[O] =
      numerator.divideBy(denominator)

    type APerB = Dim[Power["static:A", 1] *: Power["static:B", -1] *: EmptyTuple]
    val normalization: Normalize.Aux[Divide[A, B], APerB] = Normalize.derived[Divide[A, B]]
    val denominator             = trading.quantity.refinement.NonZero(Quantity(b, 2)).toOption.get
    val result: Quantity[APerB] = quotient[A, B, APerB](Quantity(a, 6), denominator)(using normalization)

    assertEquals(result.coefficient, Rational(3))

  test("ordinary rate arithmetic exposes named endpoints"):
    val amount: Quantity[A] = Quantity(a, Rational(1, 10))
    val aToB: Rate[A, B]    = Rate(a, b, Rational(60_000))
    val bToC: Rate[B, C]    = Rate(b, c, Rational(9, 10))

    val inB: Quantity[B] = amount * aToB
    val aToC: Rate[A, C] = aToB.andThen(bToC)
    val inC: Quantity[C] = amount.applyRate(aToC)

    assertEquals(inB.coefficient, Rational(6_000))
    assertEquals(inC.coefficient, Rational(5_400))

  test("malformed canonical Dim claims are rejected"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Bad = Dim[Power["zero", 0] *: EmptyTuple]
      val evidence = Normalize.derived[Bad]
      """,
      "zero exponents"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Bad = Dim[Power["duplicate", 1] *: Power["duplicate", 2] *: EmptyTuple]
      val evidence = Normalize.derived[Bad]
      """,
      "keys must be unique"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Bad = Dim[String *: EmptyTuple]
      val evidence = Normalize.derived[Bad]
      """,
      "must be a Power"
    )

  test("unresolved generic key equality requires contextual Normalize"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      def normalize[K <: Singleton, L <: Singleton] =
        summon[Normalize[Times[Atom[K], Atom[L]]]]
      """,
      "contextual Normalize evidence"
    )

  test("static exponent overflow fails explicitly without wrapping"):
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Maximum = Dim[Power["overflow", 2147483647] *: EmptyTuple]
      val evidence = Normalize.derived[Times[Maximum, Atom["overflow"]]]
      """,
      "outside the singleton Int range"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Minimum = Dim[Power["underflow-addition", -2147483648] *: EmptyTuple]
      val evidence = Normalize.derived[Times[Minimum, Inverse[Atom["underflow-addition"]]]]
      """,
      "static exponent -2147483649 is outside the singleton Int range"
    )
    assertDoesNotCompileContaining(
      """
      import trading.quantity.*
      type Minimum = Dim[Power["underflow", -2147483648] *: EmptyTuple]
      val evidence = Normalize.derived[Inverse[Minimum]]
      """,
      "outside the singleton Int range"
    )

  test("one complete normalization may return an intermediate BigInt sum to the Int range"):
    type Maximum    = Dim[Power["boundary", 2147483647] *: EmptyTuple]
    type Expression = Times[Times[Maximum, Atom["boundary"]], Inverse[Atom["boundary"]]]

    val normalized = Normalize.derived[Expression]
    assertSameType[normalized.Out, Maximum]

  test("fractional exponent types are outside the grammar"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type Bad = Power["fractional", 0.5]
      """

  test("downstream code cannot forge trusted evidence"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type A = Atom["forge:A"]
      val forged = new Normalize[A]:
        type Out = A
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type A = Atom["forge:A"]
      val forged = new SameDimension[A, A] {}
      """

end StaticDimensionNormalizationSuite
