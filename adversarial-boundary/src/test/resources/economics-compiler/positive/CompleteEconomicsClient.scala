package external.economics.positive

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.fee.policy.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.risk.Risk
import trading.scenario.*

object CompleteEconomicsClient:
  def genericLots[I <: Instrument](instrument: I)(count: BigInt): Either[LotError, instrument.Lots] =
    Lots.fromCount(instrument)(count)

  def genericPrice[I <: Instrument](instrument: I)(
    rate: Rate[instrument.roles.base.D, instrument.roles.quote.D]
  ): Either[PriceError, instrument.Price] =
    Price.fromRate(instrument)(rate)

  val typedRate = Rate(
    instrument.roles.base.dimension.ref,
    instrument.roles.quote.dimension.ref,
    Rational(100)
  )
  val genericLotsResult  = genericLots(instrument)(2)
  val genericPriceResult = genericPrice(instrument)(typedRate)
  val canonicalIntent = OrderIntent.create(instrument)(Side.Buy, lots).toOption.get
  val marketExecution = MarketExecution[D, B, Q](NonRestingTimeInForce.ImmediateOrCancel)
  val directOrder = Order.create(instrument)(
    canonicalIntent,
    ImmediateActivation[B, Q](),
    marketExecution
  )
  val positionValue = Valuation
    .positionValue(instrument)(PositionLots.fromCoordinate(instrument)(2), state)
    .toOption
    .get
  val assumptions = scenarios.assumptionsOne(marketOrder)(
    marketOrder.activation.evidence,
    marketOrder.execution.resolution,
    slice
  )
  val entry     = scenarios.order(marketOrder, assumptions).toOption.get
  val sell      = Order.market(instrument)(Side.Sell, lots).toOption.get
  val sellSlice = scenarios.slice(lots, state, LiquidityRole.Taker).toOption.get
  val sellAssumptions = scenarios.assumptionsOne(sell)(
    sell.activation.evidence,
    sell.execution.resolution,
    sellSlice
  )
  val exit = scenarios.order(sell, sellAssumptions).toOption.get
  val trip = scenarios.roundTrip(entry, exit).toOption.get
  val denomination = feePolicy
    .denomination(quote)(quoteGrid, QuantizationPolicy.TowardZero)
    .toOption
    .get
  val fee = feePolicy
    .percentage(
      denomination,
      FeeKind.from("client").toOption.get,
      Quantity(quote.dimension.ref, Rational(10)),
      FeeRate(Rational(1, 1000))
    )
    .toOption
    .get
  val schedule = new feePolicy.Schedule:
    val instrumentId: InstrumentId = instrument.identity.id
    def assess(value: feePolicy.Scenario): Either[FeePolicyError, Vector[FeeLine[? <: Dim, feePolicy.Market]]] =
      feePolicy.line(value, 0, fee).map(Vector(_))
  val pnl  = feePolicy.pnl(trip, schedule).toOption.get
  val risk = Risk.create(instrument)(feePolicy).toOption.get.downsideRisk(pnl).toOption.get

  assert(genericLotsResult.isRight)
  assert(genericPriceResult.isRight)
  assert(directOrder.isRight)
  assert(positionValue.coefficient == Rational(200))
  assert(risk.unrefined.coefficient.compare(Rational.zero) >= 0)
end CompleteEconomicsClient
