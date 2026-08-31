package trading.order

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*

final class OrderPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  property("intent normalizes every positive lot count to the side-directed signed position"):
    forAll { (raw: Int, buy: Boolean, reduceOnly: Boolean) =>
      val count  = BigInt(raw).abs + 1
      val lots   = Lots.fromCount(instrument)(count).toOption.get
      val side   = if buy then Side.Buy else Side.Sell
      val effect = if reduceOnly then PositionEffect.ReduceOnly else PositionEffect.Unrestricted
      val intent = OrderIntent.create(instrument)(side, lots, effect).toOption.get

      intent.instrumentId == instrument.identity.id &&
      intent.lots == lots &&
      intent.positionEffect == effect &&
      intent.positionChange.coordinate == side.sign * count
    }
end OrderPropertiesSuite
