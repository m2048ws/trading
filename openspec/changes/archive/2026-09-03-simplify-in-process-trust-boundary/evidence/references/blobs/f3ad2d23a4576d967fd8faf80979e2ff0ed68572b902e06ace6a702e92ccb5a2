package trading.risk

import scala.annotation.tailrec

import cats.kernel.Order

import trading.quantity.*
import trading.quantity.algebra.exactOrders.given
import trading.quantity.refinement.*

/** Evidence for why a selected affordable assessment is maximal in its declared model domain. */
sealed abstract class AffordableUpperBoundary[D <: Dim, S <: Dim] protected () extends JavaSerializationUnsupported

object AffordableUpperBoundary:
  final case class AtCap[D <: Dim, S <: Dim]() extends AffordableUpperBoundary[D, S]()
  final case class NextUnaffordable[D <: Dim, S <: Dim](next: LotRiskAssessment[D, S])
    extends AffordableUpperBoundary[D, S]()

/** Closed maximum-affordable decision with every distinct model observation retained in probe order. */
sealed abstract class MaxAffordableLots[D <: Dim, S <: Dim] protected () extends JavaSerializationUnsupported:
  def observations: Vector[LotRiskAssessment[D, S]]

  final def observationCount: Int = observations.size

  final def observedCoordinates: Vector[PositiveWhole] = observations.map(_.lots.count)
end MaxAffordableLots

/** Exact boundary-certified maximum sizing over an already validated monotone model. */
object MaxAffordableLots:
  final case class NoAffordable[D <: Dim, S <: Dim](
    first: LotRiskAssessment[D, S],
    observations: Vector[LotRiskAssessment[D, S]])
    extends MaxAffordableLots[D, S]()

  final case class Selected[D <: Dim, S <: Dim](
    best: LotRiskAssessment[D, S],
    upper: AffordableUpperBoundary[D, S],
    observations: Vector[LotRiskAssessment[D, S]])
    extends MaxAffordableLots[D, S]()

  private def observe[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    count: PositiveWhole
  ): LotRiskAssessment[D, S] =
    model.assess(count)

  private def affordable[S <: Dim](
    assessment: LotRiskAssessment[? <: Dim, S],
    budget: NonNegative[Quantity[S]]
  ): Boolean =
    Order[Quantity[S]].lteqv(assessment.downsideRisk.unrefined, budget.unrefined)

  /** Select the exact greatest affordable lot coordinate with at most `2 + ceil(log2(cap))` distinct observations. */
  def select[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S]
  )(
    budget: NonNegative[Quantity[S]]
  ): MaxAffordableLots[D, S] =
    val capAssessment = observe(model, model.cap)
    val capProbes     = Vector(capAssessment)

    if affordable(capAssessment, budget) then
      MaxAffordableLots.Selected(
        capAssessment,
        AffordableUpperBoundary.AtCap(),
        capProbes
      )
    else if model.cap.unrefined == 1 then
      MaxAffordableLots.NoAffordable(capAssessment, capProbes)
    else
      val one       = PositiveWhole(BigInt(1)).toOption.get
      val first     = observe(model, one)
      val endpoints = capProbes :+ first
      if !affordable(first, budget) then MaxAffordableLots.NoAffordable(first, endpoints)
      else search(model, budget, first, capAssessment, endpoints)
  end select

  @tailrec
  private def search[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    budget: NonNegative[Quantity[S]],
    lower: LotRiskAssessment[D, S],
    upper: LotRiskAssessment[D, S],
    observations: Vector[LotRiskAssessment[D, S]]
  ): MaxAffordableLots[D, S] =
    val lowerCount = lower.lots.count.unrefined
    val upperCount = upper.lots.count.unrefined
    if upperCount == lowerCount + 1 then
      MaxAffordableLots.Selected(
        lower,
        AffordableUpperBoundary.NextUnaffordable(upper),
        observations
      )
    else
      val midpoint   = PositiveWhole(lowerCount + (upperCount - lowerCount) / 2).toOption.get
      val assessment = observe(model, midpoint)
      val nextProbes = observations :+ assessment
      if affordable(assessment, budget) then
        search(model, budget, assessment, upper, nextProbes)
      else search(model, budget, lower, assessment, nextProbes)
  end search

  /** Contractual upper bound for one model's distinct primary-sizing observations. */
  def maximumObservationBound[D <: Dim, S <: Dim](model: MonotoneLotRisk[D, S]): PositiveWhole =
    val ceilingLog2 =
      if model.cap.unrefined <= 1 then BigInt(0)
      else BigInt((model.cap.unrefined - 1).bitLength)
    PositiveWhole(BigInt(2) + ceilingLog2).toOption.get
end MaxAffordableLots
