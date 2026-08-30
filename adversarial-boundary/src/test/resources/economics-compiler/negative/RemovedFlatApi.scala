package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.refinement.PositiveWhole
import trading.scenario.*

object RemovedFlatApi:
  val buyAssumptions = ScenarioAssumptions.one(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  ).toOption.get
  val entry     = OrderScenario.evaluate(instrument)(buyAssumptions).toOption.get
  val sell      = Order.market(instrument)(Side.Sell, lots).toOption.get
  val sellSlice = LiquiditySlice.create(instrument)(lots, state, LiquidityRole.Taker).toOption.get
  val sellAssumptions = ScenarioAssumptions.one(sell)(
    sell.activation.evidence,
    sell.execution.resolution,
    sellSlice
  ).toOption.get
  val exit      = OrderScenario.evaluate(instrument)(sellAssumptions).toOption.get
  val roundTrip = RoundTripScenario.create(instrument)(entry, exit).toOption.get
  val currentPosition = PositionLots.fromCoordinate(instrument)(lots.count.unrefined)

  val _ = price100.ticks
  val _ = Order.market(instrument)(Side.Buy, lots)
  val _ = feePolicy.pnl(roundTrip, feePolicy.none)

  // OFFENDING-BEGIN
  val orders = Orders(instrument)
  val scenarios = Scenarios(instrument)
  val price = instrument.price(1)
  val exactPrice = instrument.priceExactly(price100.rate)
  val market = instrument.marketStateForQuote(price100)
  val order = instrument.marketOrder(Side.Buy, lots)
  val positionValue = instrument.positionValue(currentPosition, state)
  val pnl = instrument.calculatePnl(roundTrip, feePolicy.none)
  val lotCount = instrument.lotCount(lots)
  val kind = marketOrder.kind
  val activationEvidence = entry.activationEvidence
  val duplicateAssumptionId = buyAssumptions.instrumentId
  val duplicateTarget       = buyAssumptions.target
  val duplicateEvaluation   = OrderScenario.evaluate(instrument)(marketOrder, buyAssumptions)
  val freeFormLocation = ScenarioViolation.Identity(
    "scenario.order",
    instrument.identity.id,
    instrument.identity.id
  )
  val universalScenarioError = InvalidScenario(ScenarioFailureReason.NoSlices)
  val sized = instrument.sizePosition(
    Quantity(instrument.roles.settle.dimension.ref, Rational.one),
    PositiveWhole(1).toOption.get,
    feePolicy.none
  )(_ => Right(roundTrip))
  // OFFENDING-END
end RemovedFlatApi
