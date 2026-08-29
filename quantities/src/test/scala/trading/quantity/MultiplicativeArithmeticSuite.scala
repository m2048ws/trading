package trading.quantity

import munit.FunSuite

import trading.quantity.refinement.*

class MultiplicativeArithmeticSuite extends FunSuite:
  private val btc =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "BTC-multiplication-suite"
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "USD-multiplication-suite"
  private val eur =
    trading.quantity.testkit.TestAsset
      .runtime:
        AtomId:
          "EUR-multiplication-suite"

  private val btcTenths =
    UniformGrid.create[btc.D](btc.dimension,
      PositiveRational.exact(1, 10).toOption.get
    )
  private val usdCentsPerBtc =
    UniformGrid.create(DimRef.divide(usd.dimension, btc.dimension),
      PositiveRational.exact(1, 100).toOption.get
    )

  test("general grid multiplication returns the exact rational product dimension"):
    val amount = btcTenths.fromCoordinate(1)
    val price  =
      usdCentsPerBtc.fromCoordinate:
        6_000_001
    type Product = Times[btc.D, Divide[usd.D, btc.D]]
    val product: Quantity[Product]  = amount.multiplyExact(price, btcTenths, usdCentsPerBtc)
    val embedded: Quantity[Product] = btcTenths.asQuantity(amount) * usdCentsPerBtc.asQuantity(price)

    assertEquals(product.coefficient, Rational(6000001, 1000))
    assertEquals(product.coefficient, embedded.coefficient)

  test("rate application exposes the clean target endpoint"):
    val amount                    = btcTenths.fromCoordinate(1)
    val price                     = Rate(btc.dimension, usd.dimension, Rational(6000001, 100))
    val notional: Quantity[usd.D] = amount.applyRate(price, btcTenths)
    val embedded                  = btcTenths.asQuantity(amount) * price

    assertEquals(notional.coefficient, Rational(6000001, 1000))
    assertEquals(notional.coefficient, embedded.coefficient)

  test("ordinary rate multiplication preserves the raw expression"):
    val amount = Quantity(btc.dimension, Rational(1, 10))
    val price  = Rate(btc.dimension, usd.dimension, Rational(6000001, 100))
    val notional: Quantity[Times[btc.D, Divide[usd.D, btc.D]]] = amount * price

    assertEquals(notional.coefficient, Rational(6000001, 1000))

  test("ordinary exact multiplication keeps cancellation explicit"):
    val amount         = Quantity(btc.dimension, Rational(1, 10))
    val priceDimension = DimRef.divide(usd.dimension, btc.dimension)
    val price          = Quantity(priceDimension, Rational(6000001, 100))

    val raw                       = amount * price
    val notional: Quantity[usd.D] = raw.alignTo[usd.D]
    assertEquals(notional.coefficient, Rational(6000001, 1000))

  test("rate composition exposes clean source and target endpoints"):
    val btcToUsd =
      Rate(
        btc.dimension,
        usd.dimension,
        Rational:
          60000
      )
    val usdToEur                     = Rate(usd.dimension, eur.dimension, Rational(9, 10))
    val btcToEur: Rate[btc.D, eur.D] =
      btcToUsd.andThen:
        usdToEur
    val oneBtc                 = Quantity(btc.dimension, 1)
    val euros: Quantity[eur.D] =
      oneBtc.applyRate:
        btcToEur

    assertEquals(
      euros.coefficient,
      Rational:
        54000
    )

  test("ordinary rate multiplication composes through explicit endpoint alignment"):
    val btcToUsd = Rate(btc.dimension, usd.dimension, Rational(60000))
    val usdToEur = Rate(usd.dimension, eur.dimension, Rational(9, 10))

    val multiplied                         = btcToUsd * usdToEur
    val composed: Rate[btc.D, eur.D]       = multiplied.alignTo[Divide[eur.D, btc.D]]
    val viaConvenience: Rate[btc.D, eur.D] = btcToUsd.andThen(usdToEur)

    assertEquals(composed.coefficient, Rational(54000))
    assertEquals(viaConvenience.coefficient, composed.coefficient)

  test("cross-rate division exposes clean source and target endpoints"):
    val usdPerBtc                            = Rate(btc.dimension, usd.dimension, Rational(60000))
    val usdPerEur                            = Rate(eur.dimension, usd.dimension, Rational(6, 5))
    val divisor: NonZero[Rate[eur.D, usd.D]] = NonZero(usdPerEur).toOption.get

    val genericQuotient               = usdPerBtc.divideBy(divisor)
    val eurPerBtc: Rate[btc.D, eur.D] = usdPerBtc.crossRate(divisor)

    assertEquals(eurPerBtc.coefficient, Rational(50000))
    assertEquals(eurPerBtc.coefficient, genericQuotient.coefficient)

  test("identity rates are neutral for declared endpoint composition"):
    val btcToUsd = Rate(btc.dimension, usd.dimension, Rational(60000))

    assertEquals(Rate.identity(btc.dimension).andThen(btcToUsd).coefficient, btcToUsd.coefficient)
    assertEquals(btcToUsd.andThen(Rate.identity(usd.dimension)).coefficient, btcToUsd.coefficient)

  test("exact and grid products remain exact quantities"):
    val exactBtc = Quantity(btc.dimension, Rational(1, 10))
    val exactUsd = Quantity(usd.dimension, Rational(3, 2))
    val gridBtc  = btcTenths.fromCoordinate(2)

    assertEquals(
      (exactBtc * exactUsd).coefficient,
      Rational(3, 20)
    )
    assertEquals(gridBtc.multiplyExact(exactUsd, btcTenths).coefficient, Rational(3, 10))
    assertEquals(exactUsd.multiplyExact(gridBtc, btcTenths).coefficient, Rational(3, 10))

  test("mixed grid and exact multiplication preserves operand expression order"):
    val gridBtc        = btcTenths.fromCoordinate(1)
    val priceDimension = DimRef.divide(usd.dimension, btc.dimension)
    val exactPrice     = Quantity(priceDimension, Rational(6000001, 100))

    val gridFirst: Quantity[Times[btc.D, Divide[usd.D, btc.D]]] =
      gridBtc.multiplyExact(exactPrice, btcTenths)
    val exactFirst: Quantity[Times[Divide[usd.D, btc.D], btc.D]] =
      exactPrice.multiplyExact(gridBtc, btcTenths)

    assertEquals(gridFirst.coefficient, Rational(6000001, 1000))
    assertEquals(exactFirst.coefficient, gridFirst.coefficient)

end MultiplicativeArithmeticSuite
