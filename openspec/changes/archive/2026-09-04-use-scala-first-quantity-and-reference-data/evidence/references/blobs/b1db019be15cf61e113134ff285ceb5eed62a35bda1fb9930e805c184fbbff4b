package trading.quantity

import munit.FunSuite

import trading.quantity.refinement.*

class GridQuantitySuite extends FunSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "USD"
  private val btc =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "BTC"

  private val cents =
    UniformGrid.create[usd.D](usd.dimension,
      PositiveRational
        .decimal:
          "0.01"
        .toOption
        .get
    )

  private val satoshis =
    UniformGrid.create[btc.D](btc.dimension,
      PositiveRational.exact(1, 100_000_000).toOption.get
    )

  private val threeCents =
    UniformGrid.create[usd.D](usd.dimension,
      PositiveRational
        .decimal:
          "0.03"
        .toOption
        .get
    )

  test("USD cents interpret coordinates exactly"):
    val value =
      cents.fromCoordinate:
        1000
    assertEquals(
      cents.coordinate:
        value
      ,
      BigInt:
        1000
    )
    assertEquals(
      cents.asQuantity(value).coefficient,
      Rational:
        10
    )

  test("BTC satoshis interpret coordinates exactly"):
    val value =
      satoshis.fromCoordinate:
        10_000_000
    assertEquals(
      satoshis.coordinate:
        value
      ,
      BigInt:
        10_000_000
    )
    assertEquals(satoshis.asQuantity(value).coefficient, Rational(1, 10))

  test("a three-cent quantum is not confused with the cent lattice"):
    val negative =
      threeCents.fromCoordinate:
        -1
    val zero     = threeCents.fromCoordinate(0)
    val positive = threeCents.fromCoordinate(2)

    assertEquals(threeCents.asQuantity(negative).coefficient, Rational(-3, 100))
    assertEquals(threeCents.asQuantity(zero).coefficient, Rational.zero)
    assertEquals(threeCents.asQuantity(positive).coefficient, Rational(3, 50))
    assert(!Rational(1, 100).divideBy(threeCents.quantum.asNonZero).isWhole)

  test("negative coordinates are preserved"):
    val value =
      cents.fromCoordinate:
        -12345
    assertEquals(
      cents.coordinate:
        value
      ,
      BigInt:
        -12345
    )
    assertEquals(cents.asQuantity(value).coefficient, Rational(-2469, 20))

  test("coordinates beyond Long range round-trip losslessly"):
    val coordinates =
      List(
        BigInt:
          Long.MaxValue
        + 1,
        BigInt:
          Long.MinValue
        - 1,
        BigInt(2).pow:
          256
      )

    coordinates.foreach: coordinate =>
      val value =
        cents.fromCoordinate:
          coordinate
      assertEquals(
        cents.coordinate:
          value
        ,
        coordinate
      )
      assertEquals(cents.asQuantity(value).coefficient, Rational(coordinate, 100))

  test("zero, negative, and undefined grid quanta are rejected"):
    assertEquals(
      PositiveRational.exact(0, 1),
      Left:
        ExpectedPositive
    )
    assertEquals(
      PositiveRational.exact(-1, 100),
      Left:
        ExpectedPositive
    )
    assertEquals(
      PositiveRational.exact(1, 0),
      Left:
        ZeroRationalDenominator
    )
    assert:
      PositiveRational
        .decimal:
          "0"
        .isLeft
    assert:
      PositiveRational
        .decimal:
          "-0.01"
        .isLeft
    assert:
      Rational
        .parse:
          "1/0"
        .isLeft

end GridQuantitySuite
