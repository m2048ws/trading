package trading.economics.instrument

import trading.quantity.*
import trading.reference.*

/** One caller-attributed signed position change valued at one immutable market state. */
final case class AttributedPriceChange[
  A,
  D <: Dim,
  B <: Dim,
  Q <: Dim,
  S <: Dim
](
  attribution: A,
  position: PositionLots[D],
  market: MarketState[B, Q, S])
  extends JavaSerializationUnsupported

/** Requested and retained termination of a finite price-PnL calculation. */
sealed trait PricePnlEndpoint[+M <: MarketState[? <: Dim, ? <: Dim, ? <: Dim]] extends JavaSerializationUnsupported

object PricePnlEndpoint:
  case object Flat                                                                   extends PricePnlEndpoint[Nothing]
  final case class Marked[M <: MarketState[? <: Dim, ? <: Dim, ? <: Dim]](market: M) extends PricePnlEndpoint[M]
end PricePnlEndpoint

/** Exact settlement cashflow contributed by one attributed priced position change. */
final class SettledPriceContribution[
  A,
  D <: Dim,
  B <: Dim,
  Q <: Dim,
  S <: Dim
] private (
  val instrumentId: InstrumentId,
  val settlement: Asset { type D = S },
  val original: AttributedPriceChange[A, D, B, Q, S],
  val quantity: Quantity[S])
  extends JavaSerializationUnsupported:

  def attribution: A                  = original.attribution
  def positionChange: PositionLots[D] = original.position
  def market: MarketState[B, Q, S]    = original.market

  override def equals(other: Any): Boolean =
    other match
      case that: SettledPriceContribution[?, ?, ?, ?, ?] =>
        instrumentId == that.instrumentId &&
        RetainedReferenceEquality.sameAsset(settlement, that.settlement) &&
        original == that.original && quantity.coefficient == that.quantity.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, RetainedReferenceEquality.assetHash(settlement), original, quantity.coefficient).hashCode
end SettledPriceContribution

object SettledPriceContribution:
  private[instrument] def from[
    A,
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    S <: Dim
  ](
    instrumentId: InstrumentId,
    settlement: Asset { type D = S },
    original: AttributedPriceChange[A, D, B, Q, S],
    quantity: Quantity[S]
  ): SettledPriceContribution[A, D, B, Q, S] =
    new SettledPriceContribution(instrumentId, settlement, original, quantity)
end SettledPriceContribution

/** Deterministic successful-path evidence that each change is visited once by each owning phase. */
private[instrument] final case class AttributedPricePnlTraversalCost(
  validationVisits: BigInt,
  positionVisits: BigInt,
  valuationVisits: BigInt,
  aggregationVisits: BigInt)
  extends JavaSerializationUnsupported

/** Exact result of one finite attributed price-PnL calculation. */
final class AttributedPricePnl[
  A,
  D <: Dim,
  B <: Dim,
  Q <: Dim,
  S <: Dim
] private (
  val instrumentId: InstrumentId,
  val settlement: Asset { type D = S },
  val endingPosition: PositionLots[D],
  val settledContributions: Vector[SettledPriceContribution[A, D, B, Q, S]],
  val pricePnl: PricePnl[S],
  val endpoint: PricePnlEndpoint[MarketState[B, Q, S]],
  private[instrument] val traversalCost: AttributedPricePnlTraversalCost)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: AttributedPricePnl[?, ?, ?, ?, ?] =>
        instrumentId == that.instrumentId &&
        RetainedReferenceEquality.sameAsset(settlement, that.settlement) &&
        endingPosition == that.endingPosition && settledContributions == that.settledContributions &&
        pricePnl == that.pricePnl && endpoint == that.endpoint
      case _ => false

  override def hashCode: Int =
    (
      instrumentId,
      RetainedReferenceEquality.assetHash(settlement),
      endingPosition,
      settledContributions,
      pricePnl,
      endpoint
    ).hashCode
end AttributedPricePnl

object AttributedPricePnl:
  def calculate[A](
    instrument: Instrument
  )(
    changes: Vector[AttributedPriceChange[A, ? <: Dim, ? <: Dim, ? <: Dim, ? <: Dim]],
    endpoint: PricePnlEndpoint[?]
  ): Either[AttributedPricePnlErrors, instrument.AttributedPricePnl[A]] =
    val expected = instrument.identity.id

    def instrumentMismatch(
      location: AttributedPricePnlLocation,
      supplied: InstrumentId
    ): Vector[AttributedPricePnlViolation] =
      Option
        .when(supplied != expected)(AttributedPricePnlViolation.InstrumentMismatch(location, expected, supplied))
        .toVector

    def referenceMismatch(
      location: AttributedPricePnlLocation,
      result: Either[ReferenceDataError, ?]
    ): Vector[AttributedPricePnlViolation] =
      result.left.toOption.map(AttributedPricePnlViolation.ReferenceMismatch(location, _)).toVector

    def marketViolations(
      market: MarketState[? <: Dim, ? <: Dim, ? <: Dim],
      location: AttributedPricePnlComponent => AttributedPricePnlLocation
    ): Vector[AttributedPricePnlViolation] =
      instrumentMismatch(location(AttributedPricePnlComponent.Market), market.instrumentId) ++
        referenceMismatch(
          location(AttributedPricePnlComponent.Base),
          Asset.reconcile(market.base, instrument.roles.base)
        ) ++
        referenceMismatch(
          location(AttributedPricePnlComponent.Quote),
          Asset.reconcile(market.quote, instrument.roles.quote)
        ) ++
        referenceMismatch(
          location(AttributedPricePnlComponent.Settlement),
          Asset.reconcile(market.settlement, instrument.roles.settle)
        ) ++
        instrumentMismatch(location(AttributedPricePnlComponent.Price), market.price.instrumentId) ++
        referenceMismatch(
          location(AttributedPricePnlComponent.PriceGrid),
          GridHandle.reconcile(market.price.grid, instrument.priceGrid)
        )

    val validationStart =
      (List.empty[AttributedPricePnlViolation], List.empty[AttributedPricePnlViolation], BigInt(0))
    val (changeViolationsReversed, positionViolationsReversed, validationVisits) =
      changes.iterator.zipWithIndex.foldLeft(validationStart):
        case ((violations, positionViolations, visits), (change, index)) =>
          val at = (component: AttributedPricePnlComponent) => AttributedPricePnlLocation.Change(index, component)
          val currentPositionViolations =
            instrumentMismatch(at(AttributedPricePnlComponent.Position), change.position.instrumentId) ++
              referenceMismatch(
                at(AttributedPricePnlComponent.PositionGrid),
                GridHandle.reconcile(change.position.grid, instrument.positionLotGrid)
              )
          val current = currentPositionViolations ++ marketViolations(change.market, at)
          (
            current.foldLeft(violations)((acc, violation) => violation :: acc),
            currentPositionViolations.foldLeft(positionViolations)((acc, violation) => violation :: acc),
            visits + 1
          )
    val changeViolations   = changeViolationsReversed.reverse.toVector
    val positionViolations = positionViolationsReversed.reverse.toVector

    val endpointReferenceViolations = endpoint match
      case PricePnlEndpoint.Flat           => Vector.empty
      case PricePnlEndpoint.Marked(market) =>
        marketViolations(market, component => AttributedPricePnlLocation.Endpoint(component))

    val independentViolations = changeViolations ++ endpointReferenceViolations
    AttributedPricePnlErrors.from(positionViolations) match
      case Some(positionErrors) =>
        // Market and endpoint-reference failures remain independently knowable even when position evidence is bad.
        Left(AttributedPricePnlErrors.from(independentViolations).getOrElse(positionErrors))
      case None =>
        // Every position identity and retained grid was reconciled above. The immutable collection can therefore be
        // strengthened once and folded through the position owner's lawful combination operation.
        val positionStart: Either[AttributedPricePnlViolation, instrument.PositionLots] =
          Right(PositionLots.flat(instrument))
        val (endingPositionResult, positionVisits) =
          changes.iterator.zipWithIndex.foldLeft((positionStart, BigInt(0))):
            case ((Right(total), visits), (change, index)) =>
              val position = change.position.asInstanceOf[instrument.PositionLots]
              val combined = PositionLots
                .combine(instrument)(total, position)
                .left
                .map:
                  case PositionInstrumentMismatch(_, expected, supplied) =>
                    AttributedPricePnlViolation.InstrumentMismatch(
                      AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Position),
                      expected,
                      supplied
                    )
              (combined, visits + 1)
            case ((failure @ Left(_), visits), _) => (failure, visits)

        endingPositionResult match
          case Left(combinationViolation) =>
            Left(
              AttributedPricePnlErrors
                .from(independentViolations :+ combinationViolation)
                .getOrElse(AttributedPricePnlErrors.one(combinationViolation))
            )
          case Right(endingPosition) =>
            val endpointViolations = endpoint match
              case PricePnlEndpoint.Flat if endingPosition.coordinate != 0 =>
                Vector(AttributedPricePnlViolation.NonFlatPositionRequiresMark(endingPosition.coordinate))
              case PricePnlEndpoint.Marked(_) if endingPosition.coordinate == 0 =>
                Vector(AttributedPricePnlViolation.FlatPositionRejectsMark)
              case _ => Vector.empty

            AttributedPricePnlErrors.from(independentViolations ++ endpointViolations) match
              case Some(errors) => Left(errors)
              case None         =>
                // Every element's identity and retained dimension/grid references were reconciled above. Vector is
                // immutable, so the checked collection can be strengthened without another per-change traversal.
                val checkedChanges = changes.asInstanceOf[Vector[instrument.AttributedPriceChange[A]]]
                val checkedEndpoint: instrument.PricePnlEndpoint = endpoint match
                  case PricePnlEndpoint.Flat           => PricePnlEndpoint.Flat
                  case PricePnlEndpoint.Marked(market) =>
                    // The endpoint market passed the same complete reference reconciliation above.
                    PricePnlEndpoint.Marked(market.asInstanceOf[instrument.MarketState])
                value(instrument)(
                  checkedChanges,
                  checkedEndpoint,
                  endingPosition,
                  validationVisits,
                  positionVisits
                )
        end match
    end match
  end calculate

  private def value[A](
    instrument: Instrument
  )(
    changes: Vector[instrument.AttributedPriceChange[A]],
    endpoint: instrument.PricePnlEndpoint,
    endingPosition: instrument.PositionLots,
    validationVisits: BigInt,
    positionVisits: BigInt
  ): Either[AttributedPricePnlErrors, instrument.AttributedPricePnl[A]] =
    type ValuedChange = (instrument.AttributedPriceChange[A], Quantity[instrument.roles.settle.D])

    val valuationStart =
      (List.empty[ValuedChange], List.empty[AttributedPricePnlViolation], BigInt(0))
    val (valuedChangesReversed, valuationViolationsReversed, valuationVisits) =
      changes.iterator.zipWithIndex.foldLeft(valuationStart):
        case ((valued, violations, visits), (change, index)) =>
          Valuation.positionValue(instrument)(change.position, change.market) match
            case Left(cause) =>
              val violation = AttributedPricePnlViolation.ValuationFailure(
                AttributedPricePnlLocation.Change(index, AttributedPricePnlComponent.Value),
                cause
              )
              (valued, violation :: violations, visits + 1)
            case Right(value) =>
              ((change -> value * Rational(-1)) :: valued, violations, visits + 1)

    val zero          = Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
    val terminalValue = endpoint match
      case PricePnlEndpoint.Flat           => Right(zero)
      case PricePnlEndpoint.Marked(market) =>
        Valuation
          .positionValue(instrument)(endingPosition, market)
          .left
          .map(cause =>
            AttributedPricePnlViolation.ValuationFailure(
              AttributedPricePnlLocation.Endpoint(AttributedPricePnlComponent.Value),
              cause
            )
          )

    val valuationViolations = valuationViolationsReversed.reverse.toVector ++ terminalValue.left.toOption.toVector

    AttributedPricePnlErrors.from(valuationViolations) match
      case Some(errors) => Left(errors)
      case None         =>
        terminalValue match
          case Left(error)        => Left(AttributedPricePnlErrors.one(error))
          case Right(markedValue) =>
            val aggregationStart =
              (List.empty[instrument.SettledPriceContribution[A]], zero, BigInt(0))
            val (contributions, executionCashflow, aggregationVisits) =
              valuedChangesReversed.foldLeft(aggregationStart):
                case ((settled, total, visits), (change, cashflow)) =>
                  val contribution = SettledPriceContribution.from(
                    instrument.identity.id,
                    instrument.roles.settle,
                    change,
                    cashflow
                  )
                  (contribution :: settled, total + cashflow, visits + 1)
            PricePnl
              .fromValues(instrument)(endingPosition, zero, executionCashflow + markedValue)
              .left
              .map(cause =>
                AttributedPricePnlErrors.one(AttributedPricePnlViolation.PricePnlConstruction(cause))
              )
              .map: pricePnl =>
                new AttributedPricePnl(
                  instrument.identity.id,
                  instrument.roles.settle,
                  endingPosition,
                  contributions.toVector,
                  pricePnl,
                  endpoint,
                  AttributedPricePnlTraversalCost(
                    validationVisits,
                    positionVisits,
                    valuationVisits,
                    aggregationVisits
                  )
                )
        end match
    end match
  end value
end AttributedPricePnl
