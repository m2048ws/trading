package trading.risk

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.quantity.*
import trading.quantity.refinement.*

class MaximumAffordablePropertiesSuite extends ScalaCheckSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  property("binary selection agrees with exhaustive reference for every generated monotone affine model"):
    forAll(Gen.choose(1, 100), Gen.choose(-10, 10), Gen.choose(0, 10), Gen.choose(0, 1000)):
      (rawCap, first, marginal, rawBudget) =>
        val model = MonotoneLotRisk.affine(instrument)(
          PositiveWhole(rawCap).toOption.get,
          quantity(first),
          NonNegative(quantity(marginal)).toOption.get
        )
        val checkedBudget = NonNegative(quantity(rawBudget)).toOption.get
        val decision      = MaxAffordableLots.select(model)(checkedBudget)
        val exhaustive    = 1.to(rawCap).toVector
          .map(count => ModelTestAccess.observe(model, BigInt(count)))
          .filter(_.downsideRisk.unrefined.coefficient.compare(Rational(rawBudget)) <= 0)
          .lastOption

        val decisionAgrees = (decision, exhaustive) match
          case (MaxAffordableLots.NoAffordable(firstBoundary, _), None) =>
            firstBoundary.lots.count.unrefined == 1
          case (MaxAffordableLots.Selected(best, AffordableUpperBoundary.AtCap(), _), Some(reference)) =>
            best == reference && best.lots.count.unrefined == rawCap
          case (
              MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _),
              Some(reference)
            ) =>
            best == reference && next.lots.count.unrefined == best.lots.count.unrefined + 1 &&
            next.downsideRisk.unrefined.coefficient.compare(Rational(rawBudget)) > 0
          case _ => false

        val coordinates = decision.observedCoordinates.map(_.unrefined)
        decisionAgrees && coordinates.distinct == coordinates &&
        BigInt(decision.observationCount) <= MaxAffordableLots.maximumObservationBound(model).unrefined

  private def quantity(value: Int): Quantity[instrument.roles.settle.D] =
    Quantity(instrument.roles.settle.dimension.ref, value)
end MaximumAffordablePropertiesSuite
