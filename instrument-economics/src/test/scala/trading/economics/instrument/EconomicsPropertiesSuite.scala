package trading.economics.instrument

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*

class EconomicsPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  property("long and short price PnL are exact negations"):
    forAll { (raw: Int, entryRaw: Int, deltaRaw: Int) =>
      val count            = BigInt(raw).abs + 1
      val entryCoefficient = Rational(BigInt(entryRaw).abs + 1)
      val delta            = Rational(BigInt(deltaRaw).abs + 1)
      val entry            = fixture.quoteState(instrument, entryCoefficient)
      val exit             = fixture.quoteState(instrument, entryCoefficient + delta)
      val long             = PricePnl
        .calculate(instrument)(PositionLots.fromCoordinate(instrument)(count), entry, exit)
        .toOption
        .get
      val short = PricePnl
        .calculate(instrument)(PositionLots.fromCoordinate(instrument)(-count), entry, exit)
        .toOption
        .get
      long.quantity + short.quantity == Quantity.zero[instrument.roles.settle.D](
        using instrument.roles.settle.dimension.ref
      )
    }

  property("fee quantization conserves exact signed amounts"):
    forAll { (numerator: Int) =>
      val denomination = FeeDenomination
        .create(instrument)(fixture.usd, fixture.usdCents, trading.quantity.grid.QuantizationPolicy.HalfEven)
        .toOption
        .get
      val fee = Fee
        .create(instrument)(
          denomination,
          FeeKind.from("property").toOption.get,
          Quantity(fixture.usd.dimension.ref, Rational(numerator, 997))
        )
        .toOption
        .get
      fee.amount + fee.residual == fee.unrounded
    }
end EconomicsPropertiesSuite
