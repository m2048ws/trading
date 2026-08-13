package trading.quantity.testkit

import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.scalacheck.Gen

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

/** Reusable generators whose inputs and outputs remain exact and use only checked public construction. */
object ExactGenerators:
  private def checked[E, A](r: Either[E, A]): Gen[A] =
    r match
      case Right(value) => Gen.const(value)
      case Left(error)  => throw new IllegalStateException(s"lawful generator violated checked construction: $error")

  val bigInt: Gen[BigInt] =
    Gen.chooseNum(-1_000_000_000L, 1_000_000_000L).map(BigInt(_))

  val positiveBigInt: Gen[BigInt] =
    Gen.chooseNum(1L, 1_000_000_000L).map(BigInt(_))

  val nonNegativeBigInt: Gen[BigInt] =
    Gen.frequency(1 -> Gen.const(BigInt(0)), 9 -> positiveBigInt)

  val nonZeroBigInt: Gen[BigInt] =
    for
      magnitude <- positiveBigInt
      sign      <- Gen.oneOf(BigInt(-1), BigInt(1))
    yield magnitude * sign

  private val properFraction: Gen[Rational] =
    for
      denominator <- Gen.chooseNum(2L, 1_000_000L).map(BigInt(_))
      numerator   <- Gen.chooseNum(1L, 999_999L).map(BigInt(_)).map(_ % denominator)
      sign        <- Gen.oneOf(BigInt(-1), BigInt(1))
    yield Rational(numerator * sign, denominator)

  private val improperFraction: Gen[Rational] =
    for
      denominator     <- positiveBigInt
      extra           <- positiveBigInt
      sign            <- Gen.oneOf(BigInt(-1), BigInt(1))
      denominatorSign <- Gen.oneOf(BigInt(-1), BigInt(1))
    yield Rational((denominator + extra) * sign, denominator * denominatorSign)

  val rational: Gen[Rational] =
    Gen.frequency(
      2 -> Gen.const(Rational.zero),
      3 -> properFraction,
      3 -> improperFraction,
      4 ->
        (for
          numerator   <- bigInt
          denominator <- nonZeroBigInt
        yield Rational(numerator, denominator))
    )

  val nonZeroRational: Gen[Rational] =
    for
      numerator   <- nonZeroBigInt
      denominator <- nonZeroBigInt
    yield Rational(numerator, denominator)

  val nonNegativeRational: Gen[Rational] =
    for
      numerator   <- nonNegativeBigInt
      denominator <- positiveBigInt
    yield Rational(numerator, denominator)

  val positiveRationalValue: Gen[Rational] =
    for
      numerator   <- positiveBigInt
      denominator <- positiveBigInt
    yield Rational(numerator, denominator)

  val nonZeroRationalRefined: Gen[NonZero[Rational]] =
    nonZeroRational.flatMap(value => checked(NonZero(value)))

  val positiveRational: Gen[PositiveRational] =
    positiveRationalValue.flatMap(value => checked(PositiveRational(value)))

  val positiveWhole: Gen[PositiveWhole] =
    positiveBigInt.flatMap(value => checked(PositiveWhole(value)))

  val nonZeroWhole: Gen[NonZeroWhole] =
    nonZeroBigInt.flatMap(value => checked(NonZeroWhole(value)))

  val positiveInt: Gen[PositiveInt] =
    Gen.chooseNum(1, 1_000_000).flatMap(value => checked(PositiveInt(value)))

  val allocationCount: Gen[PositiveInt] =
    Gen.chooseNum(1, 100).flatMap(value => checked(PositiveInt(value)))

  val coordinate: Gen[BigInt] = bigInt

  def quantity[D <: Dimension](d: DimRef[D]): Gen[Quantity[D]] =
    rational.map(Quantity(d, _))

  def nonNegativeQuantity[D <: Dimension](d: DimRef[D]): Gen[NonNegative[Quantity[D]]] =
    nonNegativeRational.flatMap(v => checked(NonNegative(Quantity(d, v))))

  def positiveQuantity[D <: Dimension](d: DimRef[D]): Gen[Positive[Quantity[D]]] =
    positiveRationalValue.flatMap(v => checked(Positive(Quantity(d, v))))

  def nonZeroQuantity[D <: Dimension](d: DimRef[D]): Gen[NonZero[Quantity[D]]] =
    nonZeroRational.flatMap(v => checked(NonZero(Quantity(d, v))))

  def gridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Gen[GridQuantity[D, G]] =
    coordinate.map(g.fromCoordinate)

  def nonNegativeGridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Gen[NonNegative[GridQuantity[D, G]]] =
    nonNegativeBigInt.flatMap(v => checked(NonNegative(g.fromCoordinate(v))))

  def positiveGridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Gen[Positive[GridQuantity[D, G]]] =
    positiveBigInt.flatMap(v => checked(Positive(g.fromCoordinate(v))))

  def nonZeroGridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Gen[NonZero[GridQuantity[D, G]]] =
    nonZeroBigInt.flatMap(v => checked(NonZero(g.fromCoordinate(v))))

  private val atomName: Gen[String] =
    for
      first <- Gen.alphaLowerChar
      rest  <- Gen.listOf(Gen.alphaNumChar)
    yield (first :: rest).mkString

  val atomId: Gen[AtomId] = atomName.map(AtomId(_))

  val dimensionExponent: Gen[BigInt] =
    Gen.chooseNum(-8, 8).map(BigInt(_))

  /** Raw powers may contain duplicates and zeroes so normalization laws are exercised. */
  val dimensionPowers: Gen[List[(AtomId, BigInt)]] =
    Gen.listOf:
      for
        atom  <- atomId
        power <- dimensionExponent
      yield atom -> power

  val dimensionKey: Gen[DimensionKey] = dimensionPowers.map(powers => DimensionKey(powers))

  val quantizationPolicy: Gen[QuantizationPolicy] =
    Gen.oneOf(
      QuantizationPolicy.Floor,
      QuantizationPolicy.Ceiling,
      QuantizationPolicy.TowardZero,
      QuantizationPolicy.AwayFromZero,
      QuantizationPolicy.HalfEven,
      QuantizationPolicy.HalfOdd,
      QuantizationPolicy.HalfUp,
      QuantizationPolicy.HalfDown,
      QuantizationPolicy.HalfTowardZero,
      QuantizationPolicy.HalfAwayFromZero
    )

  val nearestQuantizationPolicy: Gen[QuantizationPolicy.NearestPolicy] =
    Gen.oneOf(
      QuantizationPolicy.HalfEven,
      QuantizationPolicy.HalfOdd,
      QuantizationPolicy.HalfUp,
      QuantizationPolicy.HalfDown,
      QuantizationPolicy.HalfTowardZero,
      QuantizationPolicy.HalfAwayFromZero
    )

  val remainderOrder: Gen[RemainderOrder] = Gen.oneOf(RemainderOrder.values.toIndexedSeq)

  given Arbitrary[Rational]          = Arbitrary(rational)
  given Arbitrary[NonZero[Rational]] = Arbitrary(nonZeroRationalRefined)
  given Arbitrary[PositiveRational]  = Arbitrary(positiveRational)
  given Arbitrary[DimensionKey]      = Arbitrary(dimensionKey)
  given Cogen[Rational]              = Cogen[String].contramap(_.toString)
  given Cogen[DimensionKey]          = Cogen[String].contramap(_.powers.mkString("|"))

  def arbitraryQuantity[D <: Dimension](d: DimRef[D]): Arbitrary[Quantity[D]] =
    Arbitrary(quantity(d))

  def cogenQuantity[D <: Dimension]: Cogen[Quantity[D]] =
    Cogen[String].contramap(_.coefficient.toString)

  def arbitraryGridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Arbitrary[GridQuantity[D, G]] =
    Arbitrary(gridQuantity(g))

  def cogenGridQuantity[D <: Dimension, G](g: GridRef.Grid[D, G]): Cogen[GridQuantity[D, G]] =
    Cogen[String].contramap(v => g.coordinate(v).toString)

end ExactGenerators
