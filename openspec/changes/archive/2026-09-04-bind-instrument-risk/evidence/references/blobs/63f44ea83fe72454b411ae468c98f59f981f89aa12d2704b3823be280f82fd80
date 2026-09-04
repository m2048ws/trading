package external.risk.negative

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.risk.*

object RiskInstrumentScopeMismatch:
  def rejected[I <: Instrument, J <: Instrument](instrument: I, foreign: J)(
    pnl: instrument.Pnl,
    foreignPnl: foreign.Pnl,
    assessment: LotRiskAssessment[instrument.PositionD, instrument.SettleD],
    foreignAssessment: LotRiskAssessment[foreign.PositionD, foreign.SettleD],
    cap: PositiveWhole,
    loss: Quantity[instrument.SettleD],
    foreignLoss: Quantity[foreign.SettleD],
    marginal: NonNegative[Quantity[instrument.SettleD]],
    foreignMarginal: NonNegative[Quantity[foreign.SettleD]],
    segments: Vector[LossSegment[instrument.SettleD]],
    foreignSegments: Vector[LossSegment[foreign.SettleD]],
    budget: NonNegative[Quantity[instrument.SettleD]],
    foreignBudget: NonNegative[Quantity[foreign.SettleD]],
    evaluate: instrument.Lots => Either[String, instrument.Pnl],
    foreignEvaluate: foreign.Lots => Either[String, foreign.Pnl]
  ): Unit =
    val risk = Risk.forInstrument(instrument)
    val validDownside = risk.downside(pnl)
    val validSingle = risk.single(assessment)
    val checkedForeignSingle = risk.single(foreignAssessment)
    val validAffine = risk.affine(cap, loss, marginal)
    val validPiecewise = risk.piecewise(cap, segments)
    val validExhaustive = risk.selectExhaustively(budget, cap)(evaluate)

    // OFFENDING-BEGIN
    val wrongPnl = risk.downside(foreignPnl)
    val wrongLoss = risk.affine(cap, foreignLoss, foreignMarginal)
    val wrongSegments = risk.piecewise(cap, foreignSegments)
    val wrongBudget = risk.selectExhaustively(foreignBudget, cap)(evaluate)
    val wrongEvaluator = risk.selectExhaustively(budget, cap)(foreignEvaluate)
    // OFFENDING-END

    val _ = (
      validDownside,
      validSingle,
      checkedForeignSingle,
      validAffine,
      validPiecewise,
      validExhaustive
    )
end RiskInstrumentScopeMismatch
