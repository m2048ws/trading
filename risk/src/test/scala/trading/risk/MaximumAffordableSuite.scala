package trading.risk

import munit.FunSuite

import trading.economics.instrument.InstrumentFixtures
import trading.quantity.*
import trading.quantity.refinement.*

class MaximumAffordableSuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  test("one-lot unaffordability returns the assessed lower boundary"):
    val model    = affine(10, Rational.one, Rational.one)
    val decision = MaxAffordableLots.select(model)(budget(Rational.zero))
    decision match
      case MaxAffordableLots.NoAffordable(first, observations) =>
        assertEquals(first.lots.count.unrefined, BigInt(1))
        assertEquals(first.downsideRisk.unrefined.coefficient, Rational.one)
        assertEquals(observations.map(_.lots.count.unrefined), Vector(BigInt(10), BigInt(1)))
      case selected => fail(s"expected no affordable lot, received $selected")
    assertTrace(model, decision)

  test("exact budget equality selects the interior maximum and retains the next unaffordable assessment"):
    val model    = affine(10, Rational.one, Rational.one)
    val decision = MaxAffordableLots.select(model)(budget(Rational(5)))
    decision match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _) =>
        assertEquals(best.lots.count.unrefined, BigInt(5))
        assertEquals(best.downsideRisk.unrefined.coefficient, Rational(5))
        assertEquals(next.lots.count.unrefined, BigInt(6))
        assertEquals(next.downsideRisk.unrefined.coefficient, Rational(6))
      case other => fail(s"expected interior selection, received $other")
    assertTrace(model, decision)

  test("an affordable cap is selected with one observation and explicit cap evidence"):
    val model    = affine(10, Rational.one, Rational.one)
    val decision = MaxAffordableLots.select(model)(budget(Rational(10)))
    decision match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.AtCap(), observations) =>
        assertEquals(best.lots.count.unrefined, BigInt(10))
        assertEquals(observations, Vector(best))
      case other => fail(s"expected cap selection, received $other")
    assertTrace(model, decision)

  test("cap one never observes its sole coordinate twice"):
    val model        = affine(1, Rational(2), Rational.zero)
    val affordable   = MaxAffordableLots.select(model)(budget(Rational(2)))
    val unaffordable = MaxAffordableLots.select(model)(budget(Rational.one))
    assertEquals(affordable.observedCoordinates.map(_.unrefined), Vector(BigInt(1)))
    assertEquals(unaffordable.observedCoordinates.map(_.unrefined), Vector(BigInt(1)))
    assertTrace(model, affordable)
    assertTrace(model, unaffordable)

  test("stepped plateaus select the last affordable coordinate"):
    val segments = Vector(
      LossSegment(BigInt(1), BigInt(3), quantity(1), quantity(0)),
      LossSegment(BigInt(4), BigInt(7), quantity(2), quantity(0)),
      LossSegment(BigInt(8), BigInt(10), quantity(5), quantity(0))
    )
    val model    = MonotoneLotRisk.piecewise(instrument)(cap(10), segments).toOption.get
    val decision = MaxAffordableLots.select(model)(budget(Rational(2)))
    decision match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _) =>
        assertEquals(best.lots.count.unrefined, BigInt(7))
        assertEquals(next.lots.count.unrefined, BigInt(8))
      case other => fail(s"expected stepped interior selection, received $other")
    assertTrace(model, decision)

  test("large inverse-contract-shaped range remains logarithmic"):
    val model    = affine(1000, Rational(1, 3), Rational(1, 3))
    val decision = MaxAffordableLots.select(model)(budget(Rational(100)))
    decision match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _) =>
        assertEquals(best.lots.count.unrefined, BigInt(300))
        assertEquals(best.downsideRisk.unrefined.coefficient, Rational(100))
        assertEquals(next.lots.count.unrefined, BigInt(301))
      case other => fail(s"expected inverse-shaped interior selection, received $other")
    assert(decision.observationCount < 20)
    assertTrace(model, decision)

  test("very large BigInt cap selects an exact interior boundary"):
    val hugeCap  = BigInt(10).pow(100)
    val selected = BigInt(10).pow(50)
    val model    = MonotoneLotRisk.affine(instrument)(
      PositiveWhole(hugeCap).toOption.get,
      quantity(1),
      nonnegative(1)
    )
    val decision = MaxAffordableLots.select(model)(budget(Rational(selected)))
    decision match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), _) =>
        assertEquals(best.lots.count.unrefined, selected)
        assertEquals(next.lots.count.unrefined, selected + 1)
      case other => fail(s"expected huge interior selection, received $other")
    assertTrace(model, decision)

  private def affine(
    rawCap: Int,
    first: Rational,
    marginal: Rational
  ): MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D] =
    MonotoneLotRisk.affine(instrument)(cap(rawCap), quantity(first), nonnegative(marginal))

  private def quantity(value: Int): Quantity[instrument.roles.settle.D] = quantity(Rational(value))

  private def quantity(value: Rational): Quantity[instrument.roles.settle.D] =
    Quantity(instrument.roles.settle.dimension.ref, value)

  private def nonnegative(value: Int): NonNegative[Quantity[instrument.roles.settle.D]] =
    nonnegative(Rational(value))

  private def nonnegative(value: Rational): NonNegative[Quantity[instrument.roles.settle.D]] =
    NonNegative(quantity(value)).toOption.get

  private def budget(value: Rational): NonNegative[Quantity[instrument.roles.settle.D]] = nonnegative(value)

  private def cap(value: Int): PositiveWhole = PositiveWhole(value).toOption.get

  private def assertTrace(
    model: MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D],
    decision: MaxAffordableLots[instrument.roles.position.D, instrument.roles.settle.D]
  ): Unit =
    val coordinates = decision.observedCoordinates.map(_.unrefined)
    assertEquals(coordinates.distinct, coordinates)
    assert(BigInt(decision.observationCount) <= MaxAffordableLots.maximumObservationBound(model).unrefined)
    decision.observations.foreach: assessment =>
      assertEquals(assessment.lots.instrumentId, instrument.identity.id)
      assertEquals(assessment.positionDimension.key, instrument.roles.position.dimension.key)
      assertEquals(assessment.settlementDimension.key, instrument.roles.settle.dimension.key)
end MaximumAffordableSuite
