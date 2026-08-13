package trading.quantity

import munit.FunSuite

import trading.quantity.refinement.*

class MultiplicativeArithmeticSuite extends FunSuite:
  private val btc =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "BTC-multiplication-suite"
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-multiplication-suite"
  private val eur =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "EUR-multiplication-suite"

  private val btcTenths =
    UniformGrid.create[btc.D](
      GridId:
        "btc-tenths-multiplication-suite"
      ,
      GridVersion(1),
      btc.dimension,
      PositiveRational.exact(1, 10).toOption.get
    )
  private val usdCentsPerBtc =
    UniformGrid.create[Divide[usd.D, btc.D]](
      GridId:
        "usd-cents-per-btc-multiplication-suite"
      ,
      GridVersion(1),
      DimRef.divide(usd.dimension, btc.dimension),
      PositiveRational.exact(1, 100).toOption.get
    )

  test("general grid multiplication returns the exact rational product dimension"):
    val amount = btcTenths.fromCoordinate(1)
    val price  =
      usdCentsPerBtc.fromCoordinate:
        6_000_001
    val product: Quantity[Times[btc.D, Divide[usd.D, btc.D]]] =
      amount.multiplyExact(price, btcTenths, usdCentsPerBtc)

    assertEquals(product.coefficient, Rational(6000001, 1000))

  test("rate application exposes the clean target endpoint"):
    val amount                    = btcTenths.fromCoordinate(1)
    val price                     = Rate(btc.dimension, usd.dimension, Rational(6000001, 100))
    val notional: Quantity[usd.D] = amount.applyRate(price, btcTenths)

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

end MultiplicativeArithmeticSuite
