package trading.scenario

import trading.economics.instrument.*
import trading.quantity.*

/** Exact fee-independent price normalization for one complete checked round trip. */
object ScenarioValuation:
  def pricePnl[I <: Instrument](
    instrument: I
  )(
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[ScenarioValuationError, instrument.PricePnl] =
    val expected = instrument.identity.id
    if roundTrip.instrumentId != expected then
      Left(ScenarioValuationError.InstrumentMismatch(expected, roundTrip.instrumentId))
    else
      for
        entry  <- cashflow(instrument)(RoundTripLeg.Entry, roundTrip.entry)
        exit   <- cashflow(instrument)(RoundTripLeg.Exit, roundTrip.exit)
        zero    = Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
        result <- PricePnl
                    .fromValues(instrument)(roundTrip.heldPosition, zero, entry + exit)
                    .left
                    .map(ScenarioValuationError.PricePnlConstruction.apply)
      yield result
  end pricePnl

  private def cashflow[I <: Instrument](
    instrument: I
  )(
    leg: RoundTripLeg,
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[ScenarioValuationError, Quantity[instrument.roles.settle.D]] =
    val zero = Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
    scenario.matchedSlices.toVector.zipWithIndex.foldLeft[Either[ScenarioValuationError,
      Quantity[
        instrument.roles.settle.D
      ]]](Right(zero)):
      case (accumulated, (slice, index)) =>
        for
          total    <- accumulated
          position <- scenario.order.intent
                        .positionChangeFor(instrument)(slice.lots)
                        .left
                        .map(ScenarioValuationError.SlicePosition(leg, index, _))
          value <- Valuation
                     .positionValue(instrument)(position, slice.market)
                     .left
                     .map(ScenarioValuationError.SliceValue(leg, index, _))
        yield total + value * Rational(-1)
  end cashflow
end ScenarioValuation
