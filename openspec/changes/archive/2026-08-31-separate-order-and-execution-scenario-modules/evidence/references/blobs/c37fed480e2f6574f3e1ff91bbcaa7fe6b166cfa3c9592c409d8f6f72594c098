package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.refinement.PositiveWhole
import trading.scenario.*

object RemovedFlatApi:
  val buyAssumptions = scenarios.assumptionsOne(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  )
  val entry     = scenarios.order(marketOrder, buyAssumptions).toOption.get
  val sell      = Order.market(instrument)(Side.Sell, lots).toOption.get
  val sellSlice = scenarios.slice(lots, state, LiquidityRole.Taker).toOption.get
  val sellAssumptions = scenarios.assumptionsOne(sell)(
    sell.activation.evidence,
    sell.execution.resolution,
    sellSlice
  )
  val exit      = scenarios.order(sell, sellAssumptions).toOption.get
  val roundTrip = scenarios.roundTrip(entry, exit).toOption.get
  val currentPosition = PositionLots.fromCoordinate(instrument)(lots.count.unrefined)

  val _ = price100.ticks
  val _ = Order.market(instrument)(Side.Buy, lots)
  val _ = feePolicy.pnl(roundTrip, feePolicy.none)

  // OFFENDING-BEGIN
  val orders = Orders(instrument)
  val price = instrument.price(1)
  val exactPrice = instrument.priceExactly(price100.rate)
  val market = instrument.marketStateForQuote(price100)
  val order = instrument.marketOrder(Side.Buy, lots)
  val positionValue = instrument.positionValue(currentPosition, state)
  val pnl = instrument.calculatePnl(roundTrip, feePolicy.none)
  val lotCount = instrument.lotCount(lots)
  val kind = marketOrder.kind
  val activationEvidence = entry.activationEvidence
  val sized = instrument.sizePosition(
    Quantity(instrument.roles.settle.dimension.ref, Rational.one),
    PositiveWhole(1).toOption.get,
    feePolicy.none
  )(_ => Right(roundTrip))
  // OFFENDING-END
end RemovedFlatApi
