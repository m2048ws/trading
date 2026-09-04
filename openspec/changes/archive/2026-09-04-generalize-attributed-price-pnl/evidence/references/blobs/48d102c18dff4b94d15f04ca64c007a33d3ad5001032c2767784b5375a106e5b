package trading.scenario

import trading.economics.instrument.*
import trading.quantity.*

/** Scenario-local association between one shared price contribution and its original round-trip slice. */
private[scenario] final case class RoundTripPriceAttribution(leg: RoundTripLeg, sliceIndex: Int)
  extends JavaSerializationUnsupported

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
    attributedPricePnl(instrument)(roundTrip).map(_.pricePnl)

  private[scenario] def attributedPricePnl[I <: Instrument](
    instrument: I
  )(
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[ScenarioValuationError, instrument.AttributedPricePnl[RoundTripPriceAttribution]] =
    val expected = instrument.identity.id
    if roundTrip.instrumentId != expected then
      Left(ScenarioValuationError.InstrumentMismatch(expected, roundTrip.instrumentId))
    else
      for
        entry  <- attributedChanges(instrument)(RoundTripLeg.Entry, roundTrip.entry)
        exit   <- attributedChanges(instrument)(RoundTripLeg.Exit, roundTrip.exit)
        changes = entry ++ exit
        result <- AttributedPricePnl
                    .calculate(instrument)(changes, PricePnlEndpoint.Flat)
                    .left
                    .map(errors => compatibleFailure(changes.map(_.attribution), errors))
      yield result
  end attributedPricePnl

  private def attributedChanges[I <: Instrument](
    instrument: I
  )(
    leg: RoundTripLeg,
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[ScenarioValuationError, Vector[instrument.AttributedPriceChange[RoundTripPriceAttribution]]] =
    val reversed = scenario.matchedSlices.toVector.zipWithIndex.foldLeft[Either[
      ScenarioValuationError,
      List[instrument.AttributedPriceChange[RoundTripPriceAttribution]]
    ]](Right(List.empty)):
      case (accumulated, (slice, index)) =>
        for
          changes  <- accumulated
          position <- scenario.order.intent
                        .positionChangeFor(instrument)(slice.lots)
                        .left
                        .map(ScenarioValuationError.SlicePosition(leg, index, _))
        yield AttributedPriceChange(RoundTripPriceAttribution(leg, index), position, slice.market) :: changes
    reversed.map(_.reverse.toVector)
  end attributedChanges

  private[scenario] def compatibleFailure(
    attributions: Vector[RoundTripPriceAttribution],
    errors: AttributedPricePnlErrors
  ): ScenarioValuationError =
    def at(index: Int)(located: RoundTripPriceAttribution => ScenarioValuationError): ScenarioValuationError =
      attributions.lift(index).fold[ScenarioValuationError](
        ScenarioValuationError.SharedPricePnl(errors.head)
      )(located)

    errors.head match
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Market),
          expected,
          supplied
        ) =>
        at(index)(value =>
          ScenarioValuationError.SliceValue(
            value.leg,
            value.sliceIndex,
            ValuationInstrumentMismatch("market", expected, supplied)
          )
        )
      case AttributedPricePnlViolation.ValuationFailure(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Value),
          cause
        ) =>
        at(index)(value => ScenarioValuationError.SliceValue(value.leg, value.sliceIndex, cause))
      case AttributedPricePnlViolation.PricePnlConstruction(cause) =>
        ScenarioValuationError.PricePnlConstruction(cause)
      case cause => ScenarioValuationError.SharedPricePnl(cause)
    end match
  end compatibleFailure
end ScenarioValuation
