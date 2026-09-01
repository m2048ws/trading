package trading.risk

import scala.annotation.tailrec

import cats.kernel.Order

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.algebra.exactOrders.given
import trading.quantity.refinement.*

/** Closed reasons why an arbitrary exhaustive observation could not be completed. */
sealed abstract class ExhaustiveLotEvaluationCause[+E] protected () extends JavaSerializationUnsupported:
  ExhaustiveLotEvaluationCause.requireBuiltin(this)

object ExhaustiveLotEvaluationCause:
  final case class CallerEvaluation[E](cause: E)            extends ExhaustiveLotEvaluationCause[E]()
  final case class LotConstruction(cause: LotError)         extends ExhaustiveLotEvaluationCause[Nothing]()
  final case class RiskAssessment(cause: RiskIdentityError) extends ExhaustiveLotEvaluationCause[Nothing]()

  private def requireBuiltin(value: ExhaustiveLotEvaluationCause[?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[CallerEvaluation[?]] ||
        runtimeClass == classOf[LotConstruction] ||
        runtimeClass == classOf[RiskAssessment]
    if !supported then
      throw new IllegalAccessError(s"unsupported ExhaustiveLotEvaluationCause implementation: ${runtimeClass.getName}")
end ExhaustiveLotEvaluationCause

/** The exact positive coordinate and typed cause of the first failed ascending observation. */
final case class LocatedLotEvaluationFailure[+E](
  coordinate: PositiveWhole,
  cause: ExhaustiveLotEvaluationCause[E])

/**
 * Exhaustive maximum decision justified by successful evaluation of every coordinate through `evaluatedThrough`.
 *
 * This evidence is intentionally distinct from the adjacent-boundary evidence of [[MaxAffordableLots]].
 */
sealed abstract class ExhaustiveLotDecision[D <: Dim, S <: Dim] protected () extends JavaSerializationUnsupported:
  ExhaustiveLotDecision.requireBuiltin(this)

  def evaluatedThrough: PositiveWhole
end ExhaustiveLotDecision

object ExhaustiveLotDecision:
  final case class NoAffordable[D <: Dim, S <: Dim](
    first: LotRiskAssessment[D, S],
    evaluatedThrough: PositiveWhole)
    extends ExhaustiveLotDecision[D, S]()

  final case class Selected[D <: Dim, S <: Dim](
    best: LotRiskAssessment[D, S],
    evaluatedThrough: PositiveWhole)
    extends ExhaustiveLotDecision[D, S]()

  private def requireBuiltin(value: ExhaustiveLotDecision[?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[NoAffordable[?, ?]] || runtimeClass == classOf[Selected[?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported ExhaustiveLotDecision implementation: ${runtimeClass.getName}")
end ExhaustiveLotDecision

/** Explicit `O(cap)` sizing for genuinely arbitrary deterministic pure lot-to-PnL evaluation. */
object ExhaustiveLotSizing:
  private val firstCoordinate =
    PositiveWhole(BigInt(1)).fold(
      _ => throw new IllegalStateException("positive one failed PositiveWhole construction"),
      identity
    )

  /**
   * Evaluate every positive lot coordinate in ascending order and retain the exact greatest affordable assessment.
   *
   * The first failed observation terminates with its coordinate and typed cause; no partial affordability decision is
   * returned. A successful run performs exactly `cap` observations and retains constant state.
   */
  // format: off
  def select[E](
    instrument: Instrument
  )(
    budget: NonNegative[Quantity[instrument.roles.settle.D]],
    cap: PositiveWhole
  )(
    evaluate: instrument.Lots => Either[E, instrument.Pnl]
  ): Either[
    LocatedLotEvaluationFailure[E],
    ExhaustiveLotDecision[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    // format: on
    observe(instrument)(firstCoordinate)(evaluate).flatMap: first =>
      val initialBest = Option.when(affordable(first, budget))(first)
      traverse(instrument)(budget, cap, evaluate, BigInt(2), first, initialBest)
  end select

  @tailrec
  // format: off
  private def traverse[E](
    instrument: Instrument
  )(
    budget: NonNegative[Quantity[instrument.roles.settle.D]],
    cap: PositiveWhole,
    evaluate: instrument.Lots => Either[E, instrument.Pnl],
    nextCount: BigInt,
    first: LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D],
    best: Option[LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D]]
  ): Either[
    LocatedLotEvaluationFailure[E],
    ExhaustiveLotDecision[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    // format: on
    if nextCount > cap.unrefined then
      Right(
        best.fold[ExhaustiveLotDecision[instrument.roles.position.D, instrument.roles.settle.D]](
          ExhaustiveLotDecision.NoAffordable(first, cap)
        )(selected => ExhaustiveLotDecision.Selected(selected, cap))
      )
    else
      val coordinate = PositiveWhole(nextCount).fold(
        _ => throw new IllegalStateException(s"ascending positive coordinate became invalid: $nextCount"),
        identity
      )
      observe(instrument)(coordinate)(evaluate) match
        case Left(failure)     => Left(failure)
        case Right(assessment) =>
          val nextBest = if affordable(assessment, budget) then Some(assessment) else best
          traverse(instrument)(budget, cap, evaluate, nextCount + 1, first, nextBest)
  end traverse

  // format: off
  private def observe[E](
    instrument: Instrument
  )(
    coordinate: PositiveWhole
  )(
    evaluate: instrument.Lots => Either[E, instrument.Pnl]
  ): Either[
    LocatedLotEvaluationFailure[E],
    LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    // format: on
    Lots
      .fromCount(instrument)(coordinate.unrefined)
      .left
      .map(cause => LocatedLotEvaluationFailure(coordinate, ExhaustiveLotEvaluationCause.LotConstruction(cause)))
      .flatMap: lots =>
        evaluate(lots)
          .left
          .map(cause => LocatedLotEvaluationFailure(coordinate, ExhaustiveLotEvaluationCause.CallerEvaluation(cause)))
          .flatMap: pnl =>
            LotRiskAssessment
              .fromPnl(instrument)(lots, pnl)
              .left
              .map(cause =>
                LocatedLotEvaluationFailure(coordinate, ExhaustiveLotEvaluationCause.RiskAssessment(cause))
              )
  end observe

  private def affordable[S <: Dim](
    assessment: LotRiskAssessment[? <: Dim, S],
    budget: NonNegative[Quantity[S]]
  ): Boolean =
    Order[Quantity[S]].lteqv(assessment.downsideRisk.unrefined, budget.unrefined)
end ExhaustiveLotSizing
