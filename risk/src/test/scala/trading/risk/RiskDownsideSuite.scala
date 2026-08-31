package trading.risk

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

class RiskDownsideSuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  test("negative PnL produces exact refined downside without quantization"):
    val pnl = pnlWithNet(instrument, Rational(-7, 3))
    assertEquals(
      Risk.downside(instrument)(pnl).map(_.unrefined.coefficient),
      Right(Rational(7, 3))
    )

  test("zero and positive PnL produce the typed refined zero"):
    List(Rational.zero, Rational(11, 5)).foreach: coefficient =>
      val pnl = pnlWithNet(instrument, coefficient)
      assertEquals(
        Risk.downside(instrument)(pnl).map(_.unrefined.coefficient),
        Right(Rational.zero)
      )

  test("ordinary foreign instrument identity is rejected before downside is returned"):
    val foreignPnl = pnlWithNet(fixtures.foreignIdentity, Rational(-999)).asInstanceOf[instrument.Pnl]
    assertEquals(
      Risk.downside(instrument)(foreignPnl),
      Left(
        DownsideInstrumentMismatch(
          instrument.identity.id,
          fixtures.foreignIdentity.identity.id
        )
      )
    )

  test("the supported budget representation rejects raw negative quantities"):
    val negative = Quantity(instrument.roles.settle.dimension.ref, Rational(-1))
    val positive = Quantity(instrument.roles.settle.dimension.ref, Rational(1))
    assertEquals(NonNegative(negative), Left(ExpectedNonNegative))
    assertEquals(NonNegative(positive).map(_.unrefined.coefficient), Right(Rational.one))

  private def pnlWithNet(value: Instrument, coefficient: Rational): value.Pnl =
    val position = PositionLots.fromCoordinate(value)(BigInt(1))
    val zero     = Quantity.zero[value.roles.settle.D](using value.roles.settle.dimension.ref)
    val exit     = Quantity(value.roles.settle.dimension.ref, coefficient)
    val pricePnl = PricePnl.fromValues(value)(position, zero, exit).toOption.get
    Pnl.create(value)(pricePnl, Vector.empty).toOption.get
end RiskDownsideSuite
