package trading.economics.instrument

import trading.quantity.*
import trading.reference.Asset

/** Exact price component retaining instrument and settlement context. */
final class PricePnl[S <: Dim] private (
  val instrumentId: InstrumentId,
  val settlement: Asset { type D = S },
  val quantity: Quantity[S])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: PricePnl[?] =>
        instrumentId == that.instrumentId && RetainedReferenceEquality.sameAsset(settlement, that.settlement) &&
        quantity.coefficient == that.quantity.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, RetainedReferenceEquality.assetHash(settlement), quantity.coefficient).hashCode
end PricePnl

object PricePnl:
  def calculate(
    instrument: Instrument
  )(
    position: instrument.PositionLots,
    entry: instrument.MarketState,
    exit: instrument.MarketState
  ): Either[ValuationError, instrument.PricePnl] =
    for
      _          <- Valuation.checkInstrument(instrument)("position", position.instrumentId)
      _          <- Valuation.checkInstrument(instrument)("entry", entry.instrumentId)
      _          <- Valuation.checkInstrument(instrument)("exit", exit.instrumentId)
      entryValue <- Valuation.positionValue(instrument)(position, entry)
      exitValue  <- Valuation.positionValue(instrument)(position, exit)
      result     <- fromValues(instrument)(position, entryValue, exitValue)
    yield result

  /** Checked typed exit-minus-entry boundary used by downstream scenario aggregation. */
  def fromValues(
    instrument: Instrument
  )(
    position: instrument.PositionLots,
    entryValue: Quantity[instrument.roles.settle.D],
    exitValue: Quantity[instrument.roles.settle.D]
  ): Either[ValuationError, instrument.PricePnl] =
    Valuation.checkInstrument(instrument)("position", position.instrumentId).map: _ =>
      new PricePnl(
        instrument.identity.id,
        instrument.roles.settle,
        exitValue - entryValue
      )
end PricePnl

object Valuation:
  def settlePerPosition(
    instrument: Instrument
  )(
    state: instrument.MarketState
  ): Either[ValuationError, Rate[instrument.roles.position.D, instrument.roles.settle.D]] =
    checkInstrument(instrument)("market", state.instrumentId).map: _ =>
      instrument.basePerPosition.andThen(state.baseToSettle) +
        instrument.quotePerPosition.andThen(state.quoteToSettle)

  def positionValue(
    instrument: Instrument
  )(
    position: instrument.PositionLots,
    state: instrument.MarketState
  ): Either[ValuationError, Quantity[instrument.roles.settle.D]] =
    for
      _           <- checkInstrument(instrument)("position", position.instrumentId)
      _           <- checkInstrument(instrument)("market", state.instrumentId)
      perPosition <- settlePerPosition(instrument)(state)
    yield position.quantity.applyRate(perPosition)

  private[instrument] def checkInstrument(
    instrument: Instrument
  )(
    context: String,
    supplied: InstrumentId
  ): Either[ValuationError, Unit] =
    Either.cond(
      supplied == instrument.identity.id,
      (),
      ValuationInstrumentMismatch(context, instrument.identity.id, supplied)
    )
end Valuation

/** One fee converted through one explicit immutable market state. */
final class SettledFeeContribution[S <: Dim] private (
  val instrumentId: InstrumentId,
  val settlement: Asset { type D = S },
  val original: Fee[? <: Dim],
  val quantity: Quantity[S])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: SettledFeeContribution[?] =>
        instrumentId == that.instrumentId && RetainedReferenceEquality.sameAsset(settlement, that.settlement) &&
        original == that.original &&
        quantity.coefficient == that.quantity.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, RetainedReferenceEquality.assetHash(settlement), original, quantity.coefficient).hashCode
end SettledFeeContribution

object SettledFeeContribution:
  def convert[D <: Dim](
    instrument: Instrument
  )(
    fee: Fee[D],
    market: instrument.MarketState
  ): Either[ContributionError, SettledFeeContribution[instrument.roles.settle.D]] =
    if fee.instrumentId != instrument.identity.id then
      Left(ContributionInstrumentMismatch("fee", instrument.identity.id, fee.instrumentId))
    else if market.instrumentId != instrument.identity.id then
      Left(ContributionInstrumentMismatch("market", instrument.identity.id, market.instrumentId))
    else
      market
        .convertToSettle(fee.asset)(fee.amount)
        .left
        .map(ContributionConversionFailure(_))
        .map: quantity =>
          new SettledFeeContribution(
            instrument.identity.id,
            instrument.roles.settle,
            fee,
            quantity
          )
end SettledFeeContribution

/** Exact price and supplied trading-fee contribution composition. */
final class Pnl[S <: Dim] private (
  val instrumentId: InstrumentId,
  val settlement: Asset { type D = S },
  val pricePnl: PricePnl[S],
  val settledFeeContributions: Vector[SettledFeeContribution[S]],
  val feePnl: Quantity[S],
  val netPnl: Quantity[S])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: Pnl[?] =>
        instrumentId == that.instrumentId && RetainedReferenceEquality.sameAsset(settlement, that.settlement) &&
        pricePnl == that.pricePnl &&
        settledFeeContributions == that.settledFeeContributions && feePnl.coefficient == that.feePnl.coefficient &&
        netPnl.coefficient == that.netPnl.coefficient
      case _ => false

  override def hashCode: Int =
    (
      instrumentId,
      RetainedReferenceEquality.assetHash(settlement),
      pricePnl,
      settledFeeContributions,
      feePnl.coefficient,
      netPnl.coefficient
    ).hashCode
end Pnl

object Pnl:
  def create(
    instrument: Instrument
  )(
    pricePnl: instrument.PricePnl,
    contributions: Vector[SettledFeeContribution[instrument.roles.settle.D]]
  ): Either[PnlError, instrument.Pnl] =
    val expectedId                                                       = instrument.identity.id
    val expectedSettlement: Asset { type D = instrument.roles.settle.D } = instrument.roles.settle
    val identityFailure                                                  =
      if pricePnl.instrumentId != expectedId then
        Some(PnlInstrumentMismatch("pricePnl", expectedId, pricePnl.instrumentId))
      else
        contributions.zipWithIndex.collectFirst:
          case (value, index) if value.instrumentId != expectedId =>
            PnlInstrumentMismatch(s"contributions[$index]", expectedId, value.instrumentId)

    def settlementError(context: String, supplied: Asset): Option[PnlError] =
      if supplied.id != expectedSettlement.id then
        Some(PnlSettlementMismatch(context, expectedSettlement.id, supplied.id))
      else
        Asset
          .reconcile(supplied, expectedSettlement)
          .left
          .toOption
          .map(cause => PnlSettlementReferenceMismatch(context, cause))

    val settlementFailure =
      settlementError("pricePnl", pricePnl.settlement).orElse:
        contributions.zipWithIndex.iterator
          .map((value, index) => settlementError(s"contributions[$index]", value.settlement))
          .collectFirst:
            case Some(error) => error

    identityFailure.orElse(settlementFailure) match
      case Some(error) => Left(error)
      case None        =>
        val feeTotal = contributions.foldLeft(
          Quantity.zero[instrument.roles.settle.D](using expectedSettlement.dimension.ref)
        )((total, contribution) => total + contribution.quantity)
        Right(
          new Pnl(
            expectedId,
            expectedSettlement,
            pricePnl,
            contributions,
            feeTotal,
            pricePnl.quantity + feeTotal
          )
        )
  end create
end Pnl
