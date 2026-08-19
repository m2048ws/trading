package trading.quantity.examples

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class NormalizedArithmeticExamplesSuite extends FunSuite:
  test("spot and inverse linear formulas cancel their source factors"):
    val position   = DimRef.atomic(AtomId("example-position"))
    val settlement = DimRef.atomic(AtomId("example-settlement"))
    val quote      = DimRef.atomic(AtomId("example-quote"))
    val base       = DimRef.atomic(AtomId("example-base"))

    val amount                = Quantity(position.dimension, Rational(3, 2))
    val settlementPerPosition = Quantity(DimRef.divide(settlement.dimension, position.dimension), Rational(20))
    val settled: Quantity[settlement.D] = amount * settlementPerPosition

    val quoteAmount                  = Quantity(quote.dimension, Rational(12))
    val basePerQuote                 = Quantity(DimRef.divide(base.dimension, quote.dimension), Rational(1, 4))
    val baseAmount: Quantity[base.D] = quoteAmount * basePerQuote

    assertEquals(settled.coefficient, Rational(30))
    assertEquals(baseAmount.coefficient, Rational(3))

  test("composed and quotient cross rates expose declared endpoints"):
    val btc = DimRef.atomic(AtomId("example-btc"))
    val usd = DimRef.atomic(AtomId("example-usd"))
    val eur = DimRef.atomic(AtomId("example-eur"))
    val eth = DimRef.atomic(AtomId("example-eth"))

    val usdPerBtc                     = Rate(btc.dimension, usd.dimension, Rational(60000))
    val eurPerUsd                     = Rate(usd.dimension, eur.dimension, Rational(9, 10))
    val ordinaryComposition           = usdPerBtc * eurPerUsd
    val eurPerBtc: Rate[btc.D, eur.D] =
      ordinaryComposition.asDimension[Divide[eur.D, btc.D]]
    val composed: Rate[btc.D, eur.D] = usdPerBtc.andThen(eurPerUsd)

    val usdPerEth                            = Rate(eth.dimension, usd.dimension, Rational(3000))
    val divisor: NonZero[Rate[eth.D, usd.D]] = NonZero(usdPerEth).toOption.get
    val quotient                             = usdPerBtc.divideBy(divisor)
    val ethPerBtc: Rate[btc.D, eth.D]        = usdPerBtc.crossRate(divisor)

    assertEquals(eurPerBtc.coefficient, Rational(54000))
    assertEquals(composed.coefficient, eurPerBtc.coefficient)
    assertEquals(ethPerBtc.coefficient, Rational(20))
    assertEquals(ethPerBtc.coefficient, quotient.coefficient)

  test("dimensionless and rate round trips normalize to their starting endpoints"):
    val btc                               = DimRef.atomic(AtomId("example-round-trip-btc"))
    val usd                               = DimRef.atomic(AtomId("example-round-trip-usd"))
    val amount                            = Quantity(btc.dimension, Rational(2))
    val divisor: NonZero[Quantity[btc.D]] = NonZero(amount).toOption.get
    val ratio: Ratio                      = amount.divideBy(divisor)

    val usdPerBtc                 = Rate(btc.dimension, usd.dimension, Rational(60000))
    val btcPerUsd                 = Rate(usd.dimension, btc.dimension, Rational(1, 60000))
    val restored: Quantity[btc.D] = amount * usdPerBtc * btcPerUsd

    assertEquals(ratio.coefficient, Rational.one)
    assertEquals(restored.coefficient, amount.coefficient)

  test("multi-atom results retain one power entry per surviving factor"):
    val a = DimRef.atomic(AtomId("example-surviving-a"))
    val b = DimRef.atomic(AtomId("example-surviving-b"))
    val c = DimRef.atomic(AtomId("example-surviving-c"))
    type ABC = Dim[
      Power[a.type, 1] *:
        Power[b.type, 1] *:
        Power[c.type, 1] *:
        EmptyTuple
    ]

    val result: Quantity[ABC] =
      Quantity(a.dimension, Rational(2)) * Quantity(b.dimension, Rational(3)) *
        Quantity(c.dimension, Rational(5))

    assertEquals(result.coefficient, Rational(30))

end NormalizedArithmeticExamplesSuite
