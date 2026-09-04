package external.risk.positive

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.risk.*

object RiskInstrumentScopeClient:
  def boundRisk[I <: Instrument](instrument: I)(
    pnl: instrument.Pnl,
    assessment: LotRiskAssessment[instrument.PositionD, instrument.SettleD],
    cap: PositiveWhole,
    firstLotLoss: Quantity[instrument.SettleD],
    additionalLotLoss: NonNegative[Quantity[instrument.SettleD]],
    segments: Vector[LossSegment[instrument.SettleD]],
    observations: Vector[(Lots[? <: Dim], Pnl[? <: Dim])],
    budget: NonNegative[Quantity[instrument.SettleD]],
    evaluate: instrument.Lots => Either[String, instrument.Pnl]
  ): Unit =
    val risk = Risk.forInstrument(instrument)

    val positionIdentity: instrument.PositionD =:= risk.PositionD = summon
    val settlementIdentity: instrument.SettleD =:= risk.SettleD = summon
    val loss: risk.Loss       = firstLotLoss
    val exactBudget: risk.Budget = budget
    val exactAssessment: risk.Assessment = assessment

    val downside: Either[RiskIdentityError, risk.Budget] =
      risk.downside(pnl)
    val single: Either[ModelViolations[risk.SettleD], risk.Model] =
      risk.single(exactAssessment)
    val affine: risk.Model =
      risk.affine(cap, loss, additionalLotLoss)
    val piecewise: Either[ModelViolations[risk.SettleD], risk.Model] =
      risk.piecewise(cap, segments)
    val table: Either[ModelViolations[risk.SettleD], risk.Model] =
      risk.fromCompleteTable(cap, observations)
    val exhaustive: Either[LocatedLotEvaluationFailure[String], risk.Decision] =
      risk.selectExhaustively(exactBudget, cap)(evaluate)

    val combined: Either[ModelViolations[risk.SettleD], risk.Model] =
      MonotoneLotRisk.add(affine, affine)
    val maximum: MaxAffordableLots[risk.PositionD, risk.SettleD] =
      MaxAffordableLots.select(affine)(exactBudget)

    val _ = (
      positionIdentity,
      settlementIdentity,
      downside,
      single,
      piecewise,
      table,
      exhaustive,
      combined,
      maximum
    )

  def run(): Unit =
    assert(classOf[Risk.InstrumentScope[?]].getName.contains("Risk$InstrumentScope"))
end RiskInstrumentScopeClient
