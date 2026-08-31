package trading.risk

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

object PackageSpoofRiskConstruction:
  // OFFENDING-BEGIN
  def forgeAssessment[D <: Dim, S <: Dim](
    lots: Lots[D],
    downside: NonNegative[Quantity[S]],
    position: DimRef[D],
    settlement: DimRef[S]
  ): LotRiskAssessment[D, S] =
    new LotRiskAssessment(lots, downside, position, settlement)

  def forgeModel[D <: Dim, S <: Dim](
    instrumentId: InstrumentId,
    position: DimRef[D],
    settlement: DimRef[S],
    cap: PositiveWhole,
    evaluate: PositiveWhole => LotRiskAssessment[D, S]
  ): MonotoneLotRisk[D, S] =
    new MonotoneLotRisk(instrumentId, position, settlement, cap, evaluate)

  def copyAssessment[D <: Dim, S <: Dim](assessment: LotRiskAssessment[D, S]) =
    assessment.copy()

  def forgeEmptyViolations[S <: Dim]: ModelViolations[S] =
    new ModelViolations(Vector.empty)

  def exposeAssessment[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    count: PositiveWhole
  ): LotRiskAssessment[D, S] =
    model.assess(count)

  def exposeSignedLoss[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    count: PositiveWhole
  ): Quantity[S] =
    model.lossAt(count)

  final class ForgedModel[D <: Dim, S <: Dim](
    instrumentId: InstrumentId,
    position: DimRef[D],
    settlement: DimRef[S],
    cap: PositiveWhole,
    evaluate: PositiveWhole => LotRiskAssessment[D, S])
    extends MonotoneLotRisk(instrumentId, position, settlement, cap, evaluate)
  // OFFENDING-END
end PackageSpoofRiskConstruction
