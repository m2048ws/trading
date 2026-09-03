package trading.risk

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

private object ModelTestAccess:
  def observe[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    count: BigInt
  ): LotRiskAssessment[D, S] =
    model.assess(PositiveWhole(count).toOption.get)
end ModelTestAccess

class RiskCurveSuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  test("affine loss is exact, signed at one lot, clamped at zero, and identity preserving"):
    val model = MonotoneLotRisk.affine(instrument)(cap(5), quantity(-5), nonnegative(3))
    assertRisk(model, 1, Rational.zero)
    assertRisk(model, 2, Rational.zero)
    assertRisk(model, 3, Rational.one)
    assertRisk(model, 5, Rational(7))
    val fifth = ModelTestAccess.observe(model, BigInt(5))
    assertEquals(fifth.lots.instrumentId, instrument.identity.id)
    assertEquals(fifth.positionDimension.key, instrument.roles.position.dimension.key)
    assertEquals(fifth.settlementDimension.key, instrument.roles.settle.dimension.key)

  test("piecewise loss supports changing nonnegative slopes and exact upward boundaries"):
    val segments = Vector(
      LossSegment(BigInt(1), BigInt(2), quantity(2), quantity(2)),
      LossSegment(BigInt(3), BigInt(4), quantity(5), quantity(1)),
      LossSegment(BigInt(5), BigInt(6), quantity(8), quantity(3))
    )
    val model = MonotoneLotRisk.piecewise(instrument)(cap(6), segments).toOption.get
    Vector(2, 4, 5, 6, 8, 11).zipWithIndex.foreach: (expected, index) =>
      assertRisk(model, index + 1, Rational(expected))
    assertEquals(model.constructionCost, CurveConstructionCost(1, 2, 0))

  test("piecewise construction accumulates negative slope, coverage, and downward-boundary failures"):
    val invalid = Vector(
      LossSegment(BigInt(1), BigInt(2), quantity(10), quantity(-1)),
      LossSegment(BigInt(4), BigInt(5), quantity(8), quantity(0))
    )
    val failures = MonotoneLotRisk.piecewise(instrument)(cap(5), invalid).left.toOption.get
    assertEquals(
      failures.toVector.map(_.productPrefix),
      Vector("NegativeMarginalLoss", "InvalidBreakpointOrder", "DownwardBoundary")
    )

  test("addition, minimum, and maximum compose signed exact losses without cap expansion"):
    val left  = MonotoneLotRisk.affine(instrument)(cap(3), quantity(-2), nonnegative(3))
    val right = MonotoneLotRisk.affine(instrument)(cap(3), quantity(1), nonnegative(1))
    val added = MonotoneLotRisk.add(left, right).toOption.get
    val min   = MonotoneLotRisk.minimum(left, right).toOption.get
    val max   = MonotoneLotRisk.maximum(left, right).toOption.get

    assertRisks(added, Vector(0, 3, 7))
    assertRisks(min, Vector(0, 1, 3))
    assertRisks(max, Vector(1, 2, 4))
    assertEquals(added.constructionCost, CurveConstructionCost(3, 0, 0))

  test("uniform-grid floor and ceiling quantization preserve exact monotonicity"):
    val source      = MonotoneLotRisk.affine(instrument)(cap(3), quantity(Rational(1, 2)), nonnegative(1))
    val wholeDollar = UniformGrid.create(
      instrument.roles.settle.dimension.ref,
      PositiveRational(Rational.one).toOption.get
    )
    val floor = MonotoneLotRisk
      .quantized(source)(wholeDollar, OrderPreservingQuantization.Floor)
      .toOption
      .get
    val ceiling = MonotoneLotRisk
      .quantized(source)(wholeDollar, OrderPreservingQuantization.Ceiling)
      .toOption
      .get
    assertRisks(floor, Vector(0, 1, 2))
    assertRisks(ceiling, Vector(1, 2, 3))

  test("fixed, proportional, minimum, capped, and inverse-contract-shaped terms remain exact"):
    val fixed        = MonotoneLotRisk.affine(instrument)(cap(4), quantity(2), nonnegative(0))
    val proportional = MonotoneLotRisk.affine(instrument)(cap(4), quantity(1), nonnegative(1))
    val minimumFee   = MonotoneLotRisk.maximum(fixed, proportional).toOption.get
    val cappedFee    = MonotoneLotRisk.minimum(minimumFee,
      MonotoneLotRisk.affine(instrument)(cap(4), quantity(3), nonnegative(0))).toOption.get
    val inverseContractShape =
      MonotoneLotRisk.affine(instrument)(cap(4), quantity(Rational(7, 3)), nonnegative(Rational(7, 3)))

    assertRisks(minimumFee, Vector(2, 2, 3, 4))
    assertRisks(cappedFee, Vector(2, 2, 3, 3))
    assertRationalRisks(inverseContractShape,
      Vector(Rational(7, 3), Rational(14, 3), Rational(7), Rational(28, 3)))

  test("complete table derives exact assessments and records its linear validation cost"):
    val observations = Vector(
      fixtures.lots(instrument, BigInt(1)) -> pnlWithNet(instrument, Rational(-2)),
      fixtures.lots(instrument, BigInt(2)) -> pnlWithNet(instrument, Rational(-3)),
      fixtures.lots(instrument, BigInt(3)) -> pnlWithNet(instrument, Rational(-3))
    )
    val model = MonotoneLotRisk.fromCompleteTable(instrument)(cap(3), observations).toOption.get
    assertRisks(model, Vector(2, 3, 3))
    assertEquals(model.constructionCost, CurveConstructionCost(1, 0, 3))

  test("complete table rejects decreasing risk after coherent structural validation"):
    val observations = Vector(
      fixtures.lots(instrument, BigInt(1)) -> pnlWithNet(instrument, Rational(-2)),
      fixtures.lots(instrument, BigInt(2)) -> pnlWithNet(instrument, Rational(-5)),
      fixtures.lots(instrument, BigInt(3)) -> pnlWithNet(instrument, Rational(-4))
    )
    val failures = MonotoneLotRisk.fromCompleteTable(instrument)(cap(3), observations).left.toOption.get
    assertEquals(failures.toVector.map(_.productPrefix), Vector("DownwardBoundary"))

  test("complete table accumulates mixed identity, dimension, ordering, duplicate, and missing failures"):
    val localOne                                              = fixtures.lots(instrument, BigInt(1))
    val foreignPnl                                            = pnlWithNet(fixtures.foreignIdentity, Rational(-1))
    val quantoLots                                            = fixtures.lots(fixtures.quanto, BigInt(3))
    val quantoPnl                                             = pnlWithNet(fixtures.quanto, Rational(-3))
    val observations: Vector[(Lots[? <: Dim], Pnl[? <: Dim])] = Vector(
      localOne   -> pnlWithNet(instrument, Rational(-1)),
      localOne   -> foreignPnl,
      quantoLots -> quantoPnl
    )
    val failures = MonotoneLotRisk.fromCompleteTable(instrument)(cap(3), observations).left.toOption.get
    val names    = failures.toVector.map(_.productPrefix)
    assert(names.contains("ObservationInstrumentMismatch"))
    assert(names.contains("ObservationDimensionMismatch"))
    assert(names.contains("InvalidObservationOrder"))
    assert(names.contains("DuplicateCoordinate"))
    assert(names.contains("MissingCoordinate"))

  test("composition rejects every independently incompatible model property in stable order"):
    val local   = MonotoneLotRisk.affine(instrument)(cap(3), quantity(1), nonnegative(1))
    val foreign = MonotoneLotRisk.affine(fixtures.quanto)(
      PositiveWhole(4).toOption.get,
      Quantity(fixtures.quanto.roles.settle.dimension.ref, 1),
      NonNegative(Quantity(fixtures.quanto.roles.settle.dimension.ref, 1)).toOption.get
    )
    val failures = MonotoneLotRisk.add(local, foreign).left.toOption.get
    assertEquals(
      failures.toVector,
      Vector(
        IncompatibleCurveComposition(CurveCompatibility.InstrumentIdentity),
        IncompatibleCurveComposition(CurveCompatibility.SettlementDimension),
        IncompatibleCurveComposition(CurveCompatibility.DomainCap)
      )
    )

  test("affine and composed construction cost is independent of an enormous cap"):
    val huge     = PositiveWhole(BigInt(10).pow(100)).toOption.get
    val left     = MonotoneLotRisk.affine(instrument)(huge, quantity(-1), nonnegative(2))
    val right    = MonotoneLotRisk.affine(instrument)(huge, quantity(3), nonnegative(0))
    val composed = MonotoneLotRisk.maximum(left, right).toOption.get
    assertEquals(left.constructionCost, CurveConstructionCost(1, 0, 0))
    assertEquals(composed.constructionCost, CurveConstructionCost(3, 0, 0))

  private def quantity(value: Int): Quantity[instrument.roles.settle.D] =
    quantity(Rational(value))

  private def quantity(value: Rational): Quantity[instrument.roles.settle.D] =
    Quantity(instrument.roles.settle.dimension.ref, value)

  private def nonnegative(value: Int): NonNegative[Quantity[instrument.roles.settle.D]] =
    nonnegative(Rational(value))

  private def nonnegative(value: Rational): NonNegative[Quantity[instrument.roles.settle.D]] =
    NonNegative(quantity(value)).toOption.get

  private def cap(value: Int): PositiveWhole = PositiveWhole(value).toOption.get

  private def assertRisk(
    model: MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D],
    count: BigInt,
    expected: Rational
  ): Unit =
    val assessment = ModelTestAccess.observe(model, count)
    assertEquals(assessment.lots.count.unrefined, count)
    assertEquals(assessment.downsideRisk.unrefined.coefficient, expected)

  private def assertRisks(
    model: MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D],
    expected: Vector[Int]
  ): Unit =
    assertRationalRisks(model, expected.map(value => Rational(value)))

  private def assertRationalRisks(
    model: MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D],
    expected: Vector[Rational]
  ): Unit =
    expected.zipWithIndex.foreach: (risk, index) =>
      assertRisk(model, BigInt(index + 1), risk)

  private def pnlWithNet(value: Instrument, coefficient: Rational): value.Pnl =
    val position = PositionLots.fromCoordinate(value)(BigInt(1))
    val zero     = Quantity.zero[value.roles.settle.D](using value.roles.settle.dimension.ref)
    val exit     = Quantity(value.roles.settle.dimension.ref, coefficient)
    val pricePnl = PricePnl.fromValues(value)(position, zero, exit).toOption.get
    Pnl.create(value)(pricePnl, Vector.empty).toOption.get
end RiskCurveSuite
