package trading.risk

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

class RiskInstrumentScopeSuite extends FunSuite:
  private final case class EvaluationFailure(label: String)

  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val scope      = Risk.forInstrument(instrument)

  test("bound risk exposes exact aliases and delegates every successful operation"):
    val pnl        = pnlWithNet(instrument, Rational(-7, 3))
    val loss       = quantity(Rational(-2))
    val marginal   = nonnegative(Rational(3))
    val checkedCap = cap(3)
    val segments   = Vector(
      LossSegment(BigInt(1), BigInt(2), quantity(1), quantity(1)),
      LossSegment(BigInt(3), BigInt(3), quantity(4), quantity(0))
    )
    val observations: Vector[(Lots[? <: Dim], Pnl[? <: Dim])] = Vector(
      fixtures.lots(instrument, BigInt(1)) -> pnlWithNet(instrument, Rational(-1)),
      fixtures.lots(instrument, BigInt(2)) -> pnlWithNet(instrument, Rational(-2)),
      fixtures.lots(instrument, BigInt(3)) -> pnlWithNet(instrument, Rational(-3))
    )
    val oneLotAssessment = LotRiskAssessment
      .fromPnl(instrument)(
        fixtures.lots(instrument, BigInt(1)),
        pnlWithNet(instrument, Rational(-5))
      )
      .toOption
      .get

    val exactLoss: scope.Loss                                   = loss
    val exactBudget: scope.Budget                               = nonnegative(Rational(2))
    val exactAssessment: scope.Assessment                       = oneLotAssessment
    val scopedDownside: Either[RiskIdentityError, scope.Budget] = scope.downside(pnl)
    assertEquals(scopedDownside, Risk.downside(instrument)(pnl))

    val scopedSingle: Either[ModelViolations[scope.SettleD], scope.Model] =
      scope.single(exactAssessment)
    val directSingle = MonotoneLotRisk.single(instrument)(oneLotAssessment)
    assertModelEquivalent(scopedSingle.toOption.get, directSingle.toOption.get, Vector(1))

    val scopedAffine: scope.Model = scope.affine(checkedCap, exactLoss, marginal)
    val directAffine              = MonotoneLotRisk.affine(instrument)(checkedCap, loss, marginal)
    assertModelEquivalent(scopedAffine, directAffine, Vector(1, 2, 3))

    val scopedPiecewise = scope.piecewise(checkedCap, segments).toOption.get
    val directPiecewise = MonotoneLotRisk.piecewise(instrument)(checkedCap, segments).toOption.get
    assertModelEquivalent(scopedPiecewise, directPiecewise, Vector(1, 2, 3))

    val scopedTable = scope.fromCompleteTable(checkedCap, observations).toOption.get
    val directTable = MonotoneLotRisk.fromCompleteTable(instrument)(checkedCap, observations).toOption.get
    assertModelEquivalent(scopedTable, directTable, Vector(1, 2, 3))

    val evaluator: instrument.Lots => Either[EvaluationFailure, instrument.Pnl] = lots =>
      Right(pnlWithNet(instrument, -Rational(lots.count.unrefined)))
    val scopedDecision: Either[LocatedLotEvaluationFailure[EvaluationFailure], scope.Decision] =
      scope.selectExhaustively(exactBudget, checkedCap)(evaluator)
    val directDecision = ExhaustiveLotSizing.select(instrument)(exactBudget, checkedCap)(evaluator)
    assertEquals(scopedDecision, directDecision)

  test("bound risk preserves identity and every deterministic model validation failure"):
    val foreignPnl = pnlWithNet(fixtures.foreignIdentity, Rational(-999)).asInstanceOf[instrument.Pnl]
    assertEquals(scope.downside(foreignPnl), Risk.downside(instrument)(foreignPnl))
    assertEquals(
      scope.downside(foreignPnl),
      Left(DownsideInstrumentMismatch(instrument.identity.id, fixtures.foreignIdentity.identity.id))
    )

    val foreign           = fixtures.quanto
    val foreignLots       = fixtures.lots(foreign, BigInt(2))
    val foreignAssessment = LotRiskAssessment
      .fromPnl(foreign)(foreignLots, pnlWithNet(foreign, Rational(-2)))
      .toOption
      .get
    val scopedSingle = scope.single(foreignAssessment)
    val directSingle = MonotoneLotRisk.single(instrument)(foreignAssessment)
    assertEquals(scopedSingle, directSingle)
    assertEquals(
      scopedSingle.left.toOption.get.toVector.map(_.productPrefix),
      Vector(
        "ModelInstrumentMismatch",
        "ModelDimensionMismatch",
        "MissingCoordinate",
        "CoordinateOutsideDomain"
      )
    )

    val invalidSegments = Vector(
      LossSegment(BigInt(1), BigInt(2), quantity(10), quantity(-1)),
      LossSegment(BigInt(4), BigInt(5), quantity(8), quantity(0))
    )
    val scopedPiecewise = scope.piecewise(cap(5), invalidSegments)
    val directPiecewise = MonotoneLotRisk.piecewise(instrument)(cap(5), invalidSegments)
    assertEquals(scopedPiecewise, directPiecewise)
    assertEquals(
      scopedPiecewise.left.toOption.get.toVector.map(_.productPrefix),
      Vector("NegativeMarginalLoss", "InvalidBreakpointOrder", "DownwardBoundary")
    )

    val localOne                                            = fixtures.lots(instrument, BigInt(1))
    val foreignIdentityPnl                                  = pnlWithNet(fixtures.foreignIdentity, Rational(-1))
    val quantoLots                                          = fixtures.lots(fixtures.quanto, BigInt(3))
    val quantoPnl                                           = pnlWithNet(fixtures.quanto, Rational(-3))
    val incoherent: Vector[(Lots[? <: Dim], Pnl[? <: Dim])] = Vector(
      localOne   -> pnlWithNet(instrument, Rational(-1)),
      localOne   -> foreignIdentityPnl,
      quantoLots -> quantoPnl
    )
    val scopedTable = scope.fromCompleteTable(cap(3), incoherent)
    val directTable = MonotoneLotRisk.fromCompleteTable(instrument)(cap(3), incoherent)
    assertEquals(scopedTable, directTable)
    assertEquals(
      scopedTable.left.toOption.get.toVector.map(_.productPrefix),
      directTable.left.toOption.get.toVector.map(_.productPrefix)
    )

    val decreasing: Vector[(Lots[? <: Dim], Pnl[? <: Dim])] = Vector(
      fixtures.lots(instrument, BigInt(1)) -> pnlWithNet(instrument, Rational(-2)),
      fixtures.lots(instrument, BigInt(2)) -> pnlWithNet(instrument, Rational(-5)),
      fixtures.lots(instrument, BigInt(3)) -> pnlWithNet(instrument, Rational(-4))
    )
    assertEquals(
      scope.fromCompleteTable(cap(3), decreasing),
      MonotoneLotRisk.fromCompleteTable(instrument)(cap(3), decreasing)
    )

  test("bound exhaustive sizing preserves traversal, decisions, and located caller causes"):
    val risks      = Vector(1, 5, 2, 8, 2)
    val checkedCap = cap(risks.size)
    val budget     = nonnegative(Rational(2))
    val scopedSeen = ArrayBuffer.empty[BigInt]
    val directSeen = ArrayBuffer.empty[BigInt]
    def evaluate(
      observed: ArrayBuffer[BigInt]
    )(
      lots: instrument.Lots
    ): Either[EvaluationFailure, instrument.Pnl] =
      val coordinate = lots.count.unrefined
      observed += coordinate
      Right(pnlWithNet(instrument, -Rational(risks(coordinate.toInt - 1))))

    val scoped = scope.selectExhaustively(budget, checkedCap)(evaluate(scopedSeen))
    val direct = ExhaustiveLotSizing.select(instrument)(budget, checkedCap)(evaluate(directSeen))
    assertEquals(scoped, direct)
    assertEquals(scopedSeen.toVector, Vector(1, 2, 3, 4, 5).map(BigInt(_)))
    assertEquals(scopedSeen.toVector, directSeen.toVector)

    val scopedFailureSeen = ArrayBuffer.empty[BigInt]
    val directFailureSeen = ArrayBuffer.empty[BigInt]
    def failAtThree(
      observed: ArrayBuffer[BigInt]
    )(
      lots: instrument.Lots
    ): Either[EvaluationFailure, instrument.Pnl] =
      val coordinate = lots.count.unrefined
      observed += coordinate
      if coordinate == 3 then Left(EvaluationFailure("failed-three"))
      else Right(pnlWithNet(instrument, Rational(-1)))

    val scopedFailure = scope.selectExhaustively(nonnegative(10), cap(6))(failAtThree(scopedFailureSeen))
    val directFailure = ExhaustiveLotSizing.select(instrument)(nonnegative(10), cap(6))(
      failAtThree(directFailureSeen)
    )
    assertEquals(scopedFailure, directFailure)
    assertEquals(scopedFailureSeen.toVector, Vector(BigInt(1), BigInt(2), BigInt(3)))
    assertEquals(scopedFailureSeen.toVector, directFailureSeen.toVector)
    assertEquals(
      scopedFailure.left.map(failure => (failure.coordinate.unrefined, failure.cause)),
      Left((BigInt(3), ExhaustiveLotEvaluationCause.CallerEvaluation(EvaluationFailure("failed-three"))))
    )

  test("one scope is allocation-independent and safe for interleaved and concurrent reuse"):
    val checkedCap          = cap(4)
    val first               = scope.affine(checkedCap, quantity(-1), nonnegative(2))
    val interleavedDownside = scope.downside(pnlWithNet(instrument, Rational(-3)))
    val second              = scope.affine(checkedCap, quantity(-1), nonnegative(2))

    assert(!first.eq(second))
    assertEquals(interleavedDownside, Risk.downside(instrument)(pnlWithNet(instrument, Rational(-3))))
    assertModelEquivalent(first, second, Vector(1, 2, 3, 4))

    val models = Await.result(
      Future.traverse(1.to(32)): _ =>
        Future(scope.affine(checkedCap, quantity(-1), nonnegative(2))),
      10.seconds
    )
    models.zip(models.drop(1)).foreach: (left, right) =>
      assert(!left.eq(right))
      assertModelEquivalent(left, right, Vector(1, 4))

  test("scoped models retain independent algebra and primary-sizing complexity"):
    val checkedCap = cap(1024)
    val left       = scope.affine(checkedCap, quantity(-1), nonnegative(2))
    val right      = scope.affine(checkedCap, quantity(3), nonnegative(0))
    val combined   = MonotoneLotRisk.maximum(left, right).toOption.get
    val decision   = MaxAffordableLots.select(combined)(nonnegative(513))

    assertEquals(left.constructionCost, CurveConstructionCost(1, 0, 0))
    assertEquals(combined.constructionCost, CurveConstructionCost(3, 0, 0))
    assert(BigInt(decision.observationCount) <= MaxAffordableLots.maximumObservationBound(combined).unrefined)
    assertEquals(combined.instrumentId, instrument.identity.id)
    assertEquals(combined.positionDimension.key, instrument.roles.position.dimension.key)
    assertEquals(combined.settlementDimension.key, instrument.roles.settle.dimension.key)

  private def assertModelEquivalent(
    scoped: scope.Model,
    direct: scope.Model,
    coordinates: Vector[Int]
  ): Unit =
    assertEquals(scoped.instrumentId, direct.instrumentId)
    assertEquals(scoped.positionDimension.key, direct.positionDimension.key)
    assertEquals(scoped.settlementDimension.key, direct.settlementDimension.key)
    assertEquals(scoped.cap, direct.cap)
    assertEquals(scoped.constructionCost, direct.constructionCost)
    coordinates.foreach: coordinate =>
      val checked = PositiveWhole(coordinate).toOption.get
      assertEquals(scoped.assess(checked), direct.assess(checked))

  private def quantity(value: Int): Quantity[instrument.SettleD] =
    quantity(Rational(value))

  private def quantity(value: Rational): Quantity[instrument.SettleD] =
    Quantity(instrument.roles.settle.dimension.ref, value)

  private def nonnegative(value: Int): NonNegative[Quantity[instrument.SettleD]] =
    nonnegative(Rational(value))

  private def nonnegative(value: Rational): NonNegative[Quantity[instrument.SettleD]] =
    NonNegative(quantity(value)).toOption.get

  private def cap(value: Int): PositiveWhole = PositiveWhole(value).toOption.get

  private def pnlWithNet(value: Instrument, coefficient: Rational): value.Pnl =
    val position = PositionLots.fromCoordinate(value)(BigInt(1))
    val zero     = Quantity.zero[value.SettleD](using value.roles.settle.dimension.ref)
    val exit     = Quantity(value.roles.settle.dimension.ref, coefficient)
    val pricePnl = PricePnl.fromValues(value)(position, zero, exit).toOption.get
    Pnl.create(value)(pricePnl, Vector.empty).toOption.get
end RiskInstrumentScopeSuite
