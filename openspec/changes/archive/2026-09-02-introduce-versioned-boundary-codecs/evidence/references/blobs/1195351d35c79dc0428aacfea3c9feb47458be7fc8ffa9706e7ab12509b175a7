package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

final class OrderRecordPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = OrderRecordTestFixture("order-property")
  private val instrument = fixture.instrument

  property("generated exact V1 records derive signed position and round-trip stable instruction meaning"):
    forAll { (rawLots: Int, rawPrice: Int, buy: Boolean, reduceOnly: Boolean, market: Boolean, pegged: Boolean) =>
      val lots   = BigInt(rawLots).abs + 1
      val price  = BigInt(rawPrice).abs + 1
      val side   = if buy then OrderRecord.Side.Buy else OrderRecord.Side.Sell
      val effect =
        if reduceOnly then OrderRecord.PositionEffect.ReduceOnly
        else OrderRecord.PositionEffect.Unrestricted
      val activation =
        if pegged then
          OrderRecord.Activation.Trailing(
            OrderRecord.PriceReference.Index,
            OrderRecord.TriggerComparison.AtOrBelow,
            1
          )
        else
          OrderRecord.Activation.Fixed(
            OrderRecord.PriceReference.Mark,
            OrderRecord.TriggerComparison.AtOrAbove,
            price
          )
      val execution =
        if market then OrderRecord.Execution.Market(OrderRecord.TimeInForce.FillOrKill)
        else
          val pricing =
            if pegged then OrderRecord.Pricing.Pegged(OrderRecord.PriceReference.Last, -7)
            else OrderRecord.Pricing.Limit(price)
          OrderRecord.Execution.Priced(
            pricing,
            OrderRecord.TimeInForce.GoodTillCancelled,
            OrderRecord.LiquidityConstraint.MakerOnly,
            OrderRecord.Visibility.Hidden
          )
      val record           = OrderRecord.V1(instrument.identity.id, side, lots, effect, activation, execution)
      val wire             = OrderRecord.encode(record).toOption.get
      val rebuilt          = OrderRecord.decodeAndReconstruct(wire, instrument).toOption.get
      val expectedPosition = if buy then lots else -lots

      OrderRecord.fromOrder(rebuilt) == record &&
      rebuilt.intent.positionChange.coordinate == expectedPosition
    }
end OrderRecordPropertiesSuite
