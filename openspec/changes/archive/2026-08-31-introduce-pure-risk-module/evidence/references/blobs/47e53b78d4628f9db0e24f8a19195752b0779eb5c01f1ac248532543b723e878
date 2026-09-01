package trading.risk

import scala.collection.mutable.ArrayBuffer

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

class ExhaustiveLotSizingSuite extends FunSuite:
  private final case class EvaluationFailure(label: String)

  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  test("non-monotone evaluation continues after an unaffordable coordinate and selects the exact greatest lot"):
    val risks    = Vector(1, 5, 2, 8, 2)
    val decision = select(risks.size, budget = 2): lots =>
      Right(pnlWithRisk(instrument, Rational(risks(lots.count.unrefined.toInt - 1))))

    decision match
      case Right(ExhaustiveLotDecision.Selected(best, evaluatedThrough)) =>
        assertEquals(best.lots.count.unrefined, BigInt(5))
        assertEquals(best.downsideRisk.unrefined.coefficient, Rational(2))
        assertEquals(evaluatedThrough.unrefined, BigInt(5))
      case other => fail(s"expected exhaustive greatest selection, received $other")

  test("an unaffordable first lot does not short-circuit a later affordable lot"):
    val decision = select(cap = 3, budget = 1): lots =>
      val risk = if lots.count.unrefined == 1 then Rational(9) else Rational.one
      Right(pnlWithRisk(instrument, risk))

    decision match
      case Right(ExhaustiveLotDecision.Selected(best, _)) =>
        assertEquals(best.lots.count.unrefined, BigInt(3))
      case other => fail(s"expected a later affordable lot, received $other")

  test("interior decreases do not hide the greatest affordable coordinate"):
    val risks    = Vector(1, 8, 7, 2, 9)
    val decision = select(risks.size, budget = 2): lots =>
      Right(pnlWithRisk(instrument, Rational(risks(lots.count.unrefined.toInt - 1))))

    assertEquals(
      decision.map:
        case ExhaustiveLotDecision.Selected(best, _)  => best.lots.count.unrefined
        case ExhaustiveLotDecision.NoAffordable(_, _) => BigInt(0),
      Right(BigInt(4))
    )

  test("a successful all-unaffordable traversal retains its first assessment and complete range evidence"):
    val decision = select(cap = 4, budget = 0): _ =>
      Right(pnlWithRisk(instrument, Rational.one))

    decision match
      case Right(ExhaustiveLotDecision.NoAffordable(first, evaluatedThrough)) =>
        assertEquals(first.lots.count.unrefined, BigInt(1))
        assertEquals(evaluatedThrough.unrefined, BigInt(4))
      case other => fail(s"expected no affordable lot, received $other")

  test("typed caller failures retain their exact coordinates in separate runs"):
    List(1, 3, 5).foreach: failureAt =>
      val decision = select(cap = 6, budget = 10): lots =>
        if lots.count.unrefined == failureAt then Left(EvaluationFailure(s"failed-$failureAt"))
        else Right(pnlWithRisk(instrument, Rational.one))
      assertEquals(
        decision.left.map(failure => (failure.coordinate.unrefined, failure.cause)),
        Left(
          (
            BigInt(failureAt),
            ExhaustiveLotEvaluationCause.CallerEvaluation(EvaluationFailure(s"failed-$failureAt"))
          )
        )
      )

  test("the first ascending failure wins and later coordinates are not evaluated"):
    val observed = ArrayBuffer.empty[BigInt]
    val decision = select(cap = 6, budget = 10): lots =>
      val count = lots.count.unrefined
      observed += count
      if count == 2 || count == 4 then Left(EvaluationFailure(s"failed-$count"))
      else Right(pnlWithRisk(instrument, Rational.one))

    assertEquals(observed.toVector, Vector(BigInt(1), BigInt(2)))
    assertEquals(decision.left.map(_.coordinate.unrefined), Left(BigInt(2)))

  test("foreign PnL identity is a located assessment failure and returns no partial decision"):
    val foreign  = fixtures.foreignIdentity
    val decision = select(cap = 4, budget = 10): lots =>
      val pnl = pnlWithRisk(foreign, Rational(lots.count.unrefined)).asInstanceOf[instrument.Pnl]
      Right(pnl)

    decision match
      case Left(
          LocatedLotEvaluationFailure(
            coordinate,
            ExhaustiveLotEvaluationCause.RiskAssessment(
              AssessmentInstrumentMismatch(AssessmentInputLocation.Pnl, expected, supplied)
            )
          )
        ) =>
        assertEquals(coordinate.unrefined, BigInt(1))
        assertEquals(expected, instrument.identity.id)
        assertEquals(supplied, foreign.identity.id)
      case other => fail(s"expected located PnL identity failure, received $other")

  test("a large successful cap is stack safe and retains constant-size decision evidence"):
    val decision = select(cap = 20000, budget = 20000): lots =>
      Right(pnlWithRisk(instrument, Rational(lots.count.unrefined)))

    decision match
      case Right(ExhaustiveLotDecision.Selected(best, evaluatedThrough)) =>
        assertEquals(best.lots.count, evaluatedThrough)
        assertEquals(evaluatedThrough.unrefined, BigInt(20000))
        assertEquals(best.downsideRisk.unrefined.coefficient, Rational(20000))
      case other => fail(s"expected large-cap selection, received $other")

  test("primary and exhaustive agree on monotone data while exposing different evidence contracts"):
    val checkedCap    = PositiveWhole(32).toOption.get
    val checkedBudget = nonnegative(17)
    val model         = MonotoneLotRisk.affine(instrument)(checkedCap, quantity(1), nonnegative(1))
    val primary       = MaxAffordableLots.select(model)(checkedBudget)
    val exhaustive    = ExhaustiveLotSizing.select(instrument)(checkedBudget, checkedCap): lots =>
      Right(pnlWithRisk(instrument, Rational(lots.count.unrefined)))

    val primaryBest = primary match
      case MaxAffordableLots.Selected(best, AffordableUpperBoundary.NextUnaffordable(next), observations) =>
        assertEquals(next.lots.count.unrefined, best.lots.count.unrefined + 1)
        assert(observations.size < checkedCap.unrefined)
        best.lots.count.unrefined
      case other => fail(s"expected primary interior decision, received $other")
    val exhaustiveBest = exhaustive match
      case Right(decision @ ExhaustiveLotDecision.Selected(best, evaluatedThrough)) =>
        assertEquals(evaluatedThrough, checkedCap)
        assert(!decision.getClass.getMethods.exists(_.getName == "observations"))
        assert(!decision.getClass.getMethods.exists(_.getName == "upper"))
        best.lots.count.unrefined
      case other => fail(s"expected exhaustive interior decision, received $other")

    assertEquals(exhaustiveBest, primaryBest)

  private def select[E](
    cap: Int,
    budget: Int
  )(
    evaluate: instrument.Lots => Either[E, instrument.Pnl]
  ) =
    ExhaustiveLotSizing.select(instrument)(nonnegative(budget), PositiveWhole(cap).toOption.get)(evaluate)

  private def pnlWithRisk(value: Instrument, risk: Rational): value.Pnl =
    val position = PositionLots.fromCoordinate(value)(BigInt(1))
    val zero     = Quantity.zero[value.roles.settle.D](using value.roles.settle.dimension.ref)
    val exit     = Quantity(value.roles.settle.dimension.ref, -risk)
    val pricePnl = PricePnl.fromValues(value)(position, zero, exit).toOption.get
    Pnl.create(value)(pricePnl, Vector.empty).toOption.get

  private def quantity(value: Int): Quantity[instrument.roles.settle.D] =
    Quantity(instrument.roles.settle.dimension.ref, value)

  private def nonnegative(value: Int): NonNegative[Quantity[instrument.roles.settle.D]] =
    NonNegative(quantity(value)).toOption.get
end ExhaustiveLotSizingSuite
