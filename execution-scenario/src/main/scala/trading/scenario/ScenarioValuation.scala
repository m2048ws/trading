package trading.scenario

import trading.economics.instrument.*
import trading.order.*
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
                    .map(errors => compatibleFailure(instrument)(changes, errors))
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

  private def compatibleFailure[I <: Instrument](
    instrument: I
  )(
    changes: Vector[instrument.AttributedPriceChange[RoundTripPriceAttribution]],
    errors: AttributedPricePnlErrors
  ): ScenarioValuationError =
    val expected = instrument.identity.id

    def invariantFailure(context: String): ScenarioValuationError =
      ScenarioValuationError.PricePnlConstruction(
        ValuationInstrumentMismatch(context, expected, expected)
      )

    def componentName(component: AttributedPricePnlComponent): String =
      component match
        case AttributedPricePnlComponent.Position     => "position"
        case AttributedPricePnlComponent.PositionGrid => "position-grid"
        case AttributedPricePnlComponent.Market       => "market"
        case AttributedPricePnlComponent.Base         => "base"
        case AttributedPricePnlComponent.Quote        => "quote"
        case AttributedPricePnlComponent.Settlement   => "settlement"
        case AttributedPricePnlComponent.Price        => "price"
        case AttributedPricePnlComponent.PriceGrid    => "price-grid"
        case AttributedPricePnlComponent.Value        => "value"

    def at(
      index: Int
    )(
      located: instrument.AttributedPriceChange[RoundTripPriceAttribution] => ScenarioValuationError
    ): ScenarioValuationError =
      changes.lift(index).fold(invariantFailure("attributed price PnL change location"))(located)

    def slicePosition(index: Int, component: OrderComponent, supplied: InstrumentId): ScenarioValuationError =
      at(index): change =>
        ScenarioValuationError.SlicePosition(
          change.attribution.leg,
          change.attribution.sliceIndex,
          OrderViolation.InstrumentMismatch(component, expected, supplied)
        )

    def sliceValue(index: Int, context: String, supplied: InstrumentId): ScenarioValuationError =
      at(index): change =>
        ScenarioValuationError.SliceValue(
          change.attribution.leg,
          change.attribution.sliceIndex,
          ValuationInstrumentMismatch(context, expected, supplied)
        )

    // Scenario valuation historically returned one deterministic failure. Preserve that fail-fast surface while
    // projecting every shared-core head case back into the original four-case algebra and its native slice location.
    errors.head match
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Position),
          _,
          supplied
        ) =>
        slicePosition(index, OrderComponent.Intent, supplied)
      case AttributedPricePnlViolation.ReferenceMismatch(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.PositionGrid),
          _
        ) =>
        val supplied = changes.lift(index).fold(expected)(_.position.instrumentId)
        slicePosition(index, OrderComponent.Lots, supplied)
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Market),
          expected,
          supplied
        ) =>
        sliceValue(index, "market", supplied)
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Price),
          _,
          supplied
        ) =>
        sliceValue(index, "market.price", supplied)
      case AttributedPricePnlViolation.ReferenceMismatch(
          AttributedPricePnlLocation.Change(index, component),
          _
        ) =>
        val supplied = changes.lift(index).fold(expected)(_.market.instrumentId)
        sliceValue(index, s"market.${componentName(component)}.reference", supplied)
      case AttributedPricePnlViolation.PricePnlConstruction(cause) =>
        ScenarioValuationError.PricePnlConstruction(cause)
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Endpoint(component),
          mismatchExpected,
          supplied
        ) =>
        ScenarioValuationError.PricePnlConstruction(
          ValuationInstrumentMismatch(s"flat endpoint ${componentName(component)}", mismatchExpected, supplied)
        )
      case AttributedPricePnlViolation.ReferenceMismatch(AttributedPricePnlLocation.Endpoint(component), _) =>
        invariantFailure(s"flat endpoint ${componentName(component)} reference")
      case AttributedPricePnlViolation.ValuationFailure(AttributedPricePnlLocation.Endpoint(_), cause) =>
        ScenarioValuationError.PricePnlConstruction(cause)
      case AttributedPricePnlViolation.NonFlatPositionRequiresMark(_) =>
        invariantFailure("round-trip flat endpoint")
      case AttributedPricePnlViolation.FlatPositionRejectsMark =>
        invariantFailure("round-trip unexpected marked endpoint")
      case AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(index, component),
          _,
          supplied
        ) =>
        sliceValue(index, s"market.${componentName(component)}", supplied)
      case AttributedPricePnlViolation.ValuationFailure(AttributedPricePnlLocation.Change(index, _), cause) =>
        at(index): change =>
          ScenarioValuationError.SliceValue(change.attribution.leg, change.attribution.sliceIndex, cause)
    end match
  end compatibleFailure
end ScenarioValuation
