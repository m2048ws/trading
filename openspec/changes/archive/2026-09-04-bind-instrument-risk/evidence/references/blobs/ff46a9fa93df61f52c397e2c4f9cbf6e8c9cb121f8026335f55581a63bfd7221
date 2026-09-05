package trading.risk

import cats.kernel.Order

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.algebra.exactOrders.given
import trading.quantity.refinement.*

/** Exact downside-risk measurements over already validated pure instrument economics. */
object Risk:

  /** Pure risk operations bound to one exact assembled instrument. */
  final class InstrumentScope[I <: Instrument] private[risk] (val instrument: I):
    type PositionD = instrument.PositionD
    type SettleD   = instrument.SettleD

    type Loss       = Quantity[SettleD]
    type Budget     = NonNegative[Loss]
    type Assessment = LotRiskAssessment[PositionD, SettleD]
    type Model      = MonotoneLotRisk[PositionD, SettleD]
    type Decision   = ExhaustiveLotDecision[PositionD, SettleD]

    def downside(pnl: instrument.Pnl): Either[RiskIdentityError, Budget] =
      Risk.downside(instrument)(pnl)

    def single(
      assessment: LotRiskAssessment[? <: Dim, ? <: Dim]
    ): Either[ModelViolations[SettleD], Model] =
      MonotoneLotRisk.single(instrument)(assessment)

    def affine(
      cap: PositiveWhole,
      firstLotLoss: Loss,
      additionalLotLoss: NonNegative[Loss]
    ): Model =
      MonotoneLotRisk.affine(instrument)(cap, firstLotLoss, additionalLotLoss)

    def piecewise(
      cap: PositiveWhole,
      segments: Vector[LossSegment[SettleD]]
    ): Either[ModelViolations[SettleD], Model] =
      MonotoneLotRisk.piecewise(instrument)(cap, segments)

    def fromCompleteTable(
      cap: PositiveWhole,
      observations: Vector[(Lots[? <: Dim], Pnl[? <: Dim])]
    ): Either[ModelViolations[SettleD], Model] =
      MonotoneLotRisk.fromCompleteTable(instrument)(cap, observations)

    def selectExhaustively[E](
      budget: Budget,
      cap: PositiveWhole
    )(
      evaluate: instrument.Lots => Either[E, instrument.Pnl]
    ): Either[LocatedLotEvaluationFailure[E], Decision] =
      ExhaustiveLotSizing.select(instrument)(budget, cap)(evaluate)
  end InstrumentScope

  def forInstrument[I <: Instrument](instrument: I): InstrumentScope[instrument.type] =
    new InstrumentScope[instrument.type](instrument)

  def downside(
    instrument: Instrument
  )(
    pnl: instrument.Pnl
  ): Either[RiskIdentityError, NonNegative[Quantity[instrument.roles.settle.D]]] =
    if pnl.instrumentId != instrument.identity.id then
      Left(DownsideInstrumentMismatch(instrument.identity.id, pnl.instrumentId))
    else
      Right(
        downsideQuantity(pnl.netPnl)(using instrument.roles.settle.dimension.ref)
      )

  private def downsideQuantity[D <: Dim](
    netPnl: Quantity[D]
  )(using DimRef[D]
  ): NonNegative[Quantity[D]] =
    val zero = Quantity.zero[D]
    if Order[Quantity[D]].lt(netPnl, zero) then
      NonNegative(zero - netPnl).fold(
        _ => throw new IllegalStateException("typed downside negation violated nonnegative closure"),
        identity
      )
    else NonNegative.quantityZero[D]
end Risk
