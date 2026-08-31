package trading.risk

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import cats.data.Validated
import cats.data.ValidatedNec
import cats.syntax.all.*

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

/** One checked positive lot coordinate and its exact downside risk. */
@nowarn("msg=Ignoring.*qualifier")
final class LotRiskAssessment[D <: Dim, S <: Dim] private[this] (
  val lots: Lots[D],
  val downsideRisk: NonNegative[Quantity[S]],
  val positionDimension: DimRef[D],
  val settlementDimension: DimRef[S])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: LotRiskAssessment[?, ?] =>
        lots == that.lots &&
        downsideRisk.unrefined.coefficient == that.downsideRisk.unrefined.coefficient &&
        positionDimension.key == that.positionDimension.key &&
        settlementDimension.key == that.settlementDimension.key
      case _ => false

  override def hashCode: Int =
    (
      lots,
      downsideRisk.unrefined.coefficient,
      positionDimension.key,
      settlementDimension.key
    ).hashCode
end LotRiskAssessment

object LotRiskAssessment:
  private val constructor =
    val owner = classOf[LotRiskAssessment[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[Lots[?]],
          classOf[Rational],
          classOf[DimRef[?]],
          classOf[DimRef[?]]
        )
      )

  private def construct[D <: Dim, S <: Dim](
    lots: Lots[D],
    downsideRisk: NonNegative[Quantity[S]],
    positionDimension: DimRef[D],
    settlementDimension: DimRef[S]
  ): LotRiskAssessment[D, S] =
    constructor
      .invoke(lots, downsideRisk, positionDimension, settlementDimension)
      .asInstanceOf[LotRiskAssessment[D, S]]

  def fromPnl(
    instrument: Instrument
  )(
    lots: instrument.Lots,
    pnl: instrument.Pnl
  ): Either[
    RiskIdentityError,
    LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    if lots.instrumentId != instrument.identity.id then
      Left(
        AssessmentInstrumentMismatch(
          AssessmentInputLocation.Lots,
          instrument.identity.id,
          lots.instrumentId
        )
      )
    else
      Risk
        .downside(instrument)(pnl)
        .left
        .map: mismatch =>
          AssessmentInstrumentMismatch(AssessmentInputLocation.Pnl, mismatch.expected, mismatch.supplied)
        .map: downside =>
          construct(
            lots,
            downside,
            instrument.roles.position.dimension.ref,
            instrument.roles.settle.dimension.ref
          )
end LotRiskAssessment

/** Closed locations for model dimension coherence failures. */
enum ModelDimensionLocation:
  case Position, Settlement

/** Closed reasons that two already-valid monotone curves cannot be composed. */
enum CurveCompatibility:
  case InstrumentIdentity, PositionDimension, SettlementDimension, DomainCap

/** Domain-owned structural failures from library-controlled model construction. */
sealed abstract class ModelViolation[S <: Dim] extends Product with Serializable

final case class ModelInstrumentMismatch[S <: Dim](expected: InstrumentId, supplied: InstrumentId)
  extends ModelViolation[S]
final case class ModelDimensionMismatch[S <: Dim](
  location: ModelDimensionLocation,
  expected: DimKey,
  supplied: DimKey)
  extends ModelViolation[S]
final case class EmptyModelDomain[S <: Dim]()                                       extends ModelViolation[S]
final case class CoordinateOutsideDomain[S <: Dim](coordinate: BigInt, cap: BigInt) extends ModelViolation[S]
final case class DuplicateCoordinate[S <: Dim](coordinate: BigInt)                  extends ModelViolation[S]
final case class MissingCoordinate[S <: Dim](coordinate: BigInt)                    extends ModelViolation[S]
final case class InvalidBreakpointOrder[S <: Dim](segmentIndex: Int, previousEnd: BigInt, nextStart: BigInt)
  extends ModelViolation[S]
final case class NegativeMarginalLoss[S <: Dim](segmentIndex: Int, marginalLoss: Quantity[S]) extends ModelViolation[S]
final case class DownwardBoundary[S <: Dim](
  segmentIndex: Int,
  previousEndLoss: Quantity[S],
  nextStartLoss: Quantity[S])
  extends ModelViolation[S]
final case class IncompatibleCurveComposition[S <: Dim](reason: CurveCompatibility)            extends ModelViolation[S]
final case class InvalidObservationOrder[S <: Dim](index: Int, previous: BigInt, next: BigInt) extends ModelViolation[S]
final case class ObservationInstrumentMismatch[S <: Dim](
  index: Int,
  location: AssessmentInputLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends ModelViolation[S]
final case class ObservationDimensionMismatch[S <: Dim](
  index: Int,
  location: ModelDimensionLocation,
  expected: DimKey,
  supplied: DimKey)
  extends ModelViolation[S]

/** Public non-empty deterministic model-construction failures. */
@nowarn("msg=Ignoring.*qualifier")
final class ModelViolations[S <: Dim] private[this] (private val values: Vector[ModelViolation[S]])
  extends JavaSerializationUnsupported:

  def head: ModelViolation[S]             = values.head
  def toVector: Vector[ModelViolation[S]] = values
  def size: Int                           = values.size

  override def equals(other: Any): Boolean =
    other match
      case that: ModelViolations[?] => values == that.toVector
      case _                        => false

  override def hashCode: Int = values.hashCode
end ModelViolations

/** An immutable library-certified exact nondecreasing lot-risk capability over `1..cap`. */
@nowarn("msg=Ignoring.*qualifier")
final class MonotoneLotRisk[D <: Dim, S <: Dim] private[this] (
  val instrumentId: InstrumentId,
  val positionDimension: DimRef[D],
  val settlementDimension: DimRef[S],
  val cap: PositiveWhole,
  val constructionCost: CurveConstructionCost,
  private val makeLots: PositiveWhole => Lots[D],
  private val formula: LotLossFormula[S])
  extends JavaSerializationUnsupported:

  @nowarn("msg=unused private member")
  private[this] def assess(count: PositiveWhole): LotRiskAssessment[D, S] =
    if count.unrefined > cap.unrefined then
      throw new IllegalArgumentException(
        s"lot coordinate ${count.unrefined} exceeds model cap ${cap.unrefined}"
      )
    else
      val loss     = formula.lossAt(count)
      val downside =
        if loss.coefficient.signum <= 0 then
          NonNegative(Quantity.zero[S](using settlementDimension)).toOption.get
        else NonNegative(loss).toOption.get
      MonotoneLotRisk.constructAssessment(
        makeLots(count),
        downside,
        positionDimension,
        settlementDimension
      )

  @nowarn("msg=unused private member")
  private[this] def lossAt(count: PositiveWhole): Quantity[S] = formula.lossAt(count)
end MonotoneLotRisk

object MonotoneLotRisk:
  private val violationsConstructor =
    val owner = classOf[ModelViolations[?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(owner, MethodType.methodType(java.lang.Void.TYPE, classOf[Vector[?]]))

  private def constructViolations[S <: Dim](
    head: ModelViolation[S],
    tail: Vector[ModelViolation[S]]
  ): ModelViolations[S] =
    violationsConstructor.invoke(head +: tail).asInstanceOf[ModelViolations[S]]

  private def violationsFrom[S <: Dim](values: Vector[ModelViolation[S]]): ModelViolations[S] =
    values match
      case head +: tail => constructViolations(head, tail)
      case _            => throw new IllegalStateException("model violation collection must be non-empty")

  private def check[S <: Dim](condition: Boolean, violation: => ModelViolation[S]) =
    Validated.condNec[ModelViolation[S], Unit](condition, (), violation)

  private def validate[S <: Dim, A](
    checks: Vector[ValidatedNec[ModelViolation[S], Unit]]
  )(
    result: => A
  ): Either[ModelViolations[S], A] =
    checks.sequence_
      .leftMap(violations => violationsFrom(violations.toChain.toVector))
      .toEither
      .map(_ => result)

  private val assessmentConstructor =
    val owner = classOf[LotRiskAssessment[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[Lots[?]],
          classOf[Rational],
          classOf[DimRef[?]],
          classOf[DimRef[?]]
        )
      )

  private def constructAssessment[D <: Dim, S <: Dim](
    lots: Lots[D],
    downsideRisk: NonNegative[Quantity[S]],
    positionDimension: DimRef[D],
    settlementDimension: DimRef[S]
  ): LotRiskAssessment[D, S] =
    assessmentConstructor
      .invoke(lots, downsideRisk, positionDimension, settlementDimension)
      .asInstanceOf[LotRiskAssessment[D, S]]

  private val constructor =
    val owner = classOf[MonotoneLotRisk[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[InstrumentId],
          classOf[DimRef[?]],
          classOf[DimRef[?]],
          classOf[BigInt],
          classOf[CurveConstructionCost],
          classOf[Function1[?, ?]],
          classOf[LotLossFormula[?]]
        )
      )

  private val modelLookup = MethodHandles.privateLookupIn(classOf[MonotoneLotRisk[?, ?]], MethodHandles.lookup())

  private val assessmentObserver =
    modelLookup.findVirtual(
      classOf[MonotoneLotRisk[?, ?]],
      "assess",
      MethodType.methodType(classOf[LotRiskAssessment[?, ?]], classOf[BigInt])
    )

  private val formulaObserver =
    modelLookup.findGetter(
      classOf[MonotoneLotRisk[?, ?]],
      "formula",
      classOf[LotLossFormula[?]]
    )

  private def observe[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    count: PositiveWhole
  ): LotRiskAssessment[D, S] =
    assessmentObserver.invoke(model, count).asInstanceOf[LotRiskAssessment[D, S]]

  private def formulaOf[S <: Dim](model: MonotoneLotRisk[? <: Dim, S]): LotLossFormula[S] =
    formulaObserver.invoke(model).asInstanceOf[LotLossFormula[S]]

  private def construct[D <: Dim, S <: Dim](
    instrumentId: InstrumentId,
    positionDimension: DimRef[D],
    settlementDimension: DimRef[S],
    cap: PositiveWhole,
    constructionCost: CurveConstructionCost,
    makeLots: PositiveWhole => Lots[D],
    formula: LotLossFormula[S]
  ): MonotoneLotRisk[D, S] =
    constructor
      .invoke(
        instrumentId,
        positionDimension,
        settlementDimension,
        cap,
        constructionCost,
        makeLots,
        formula
      )
      .asInstanceOf[MonotoneLotRisk[D, S]]

  private def lotsFromInstrument(instrument: Instrument)(count: PositiveWhole): instrument.Lots =
    Lots
      .fromCount(instrument)(count.unrefined)
      .fold(
        _ => throw new IllegalStateException("positive model coordinate failed instrument lot construction"),
        identity
      )

  /** Trivially lawful one-coordinate model built only from an already checked assessment. */
  def single(
    instrument: Instrument
  )(
    assessment: LotRiskAssessment[? <: Dim,
      ? <: Dim]
  ): Either[
    ModelViolations[instrument.roles.settle.D],
    MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    val expectedPosition   = instrument.roles.position.dimension.ref
    val expectedSettlement = instrument.roles.settle.dimension.ref
    val coordinate         = assessment.lots.count.unrefined

    val identity = Validated.condNec[ModelViolation[instrument.roles.settle.D], Unit](
      assessment.lots.instrumentId == instrument.identity.id,
      (),
      ModelInstrumentMismatch(instrument.identity.id, assessment.lots.instrumentId)
    )
    val position = Validated.condNec[ModelViolation[instrument.roles.settle.D], Unit](
      assessment.positionDimension.key == expectedPosition.key,
      (),
      ModelDimensionMismatch(
        ModelDimensionLocation.Position,
        expectedPosition.key,
        assessment.positionDimension.key
      )
    )
    val settlement = Validated.condNec[ModelViolation[instrument.roles.settle.D], Unit](
      assessment.settlementDimension.key == expectedSettlement.key,
      (),
      ModelDimensionMismatch(
        ModelDimensionLocation.Settlement,
        expectedSettlement.key,
        assessment.settlementDimension.key
      )
    )
    val domainStart = Validated.condNec[ModelViolation[instrument.roles.settle.D], Unit](
      coordinate == 1,
      (),
      MissingCoordinate(BigInt(1))
    )
    val domainEnd = Validated.condNec[ModelViolation[instrument.roles.settle.D], Unit](
      coordinate == 1,
      (),
      CoordinateOutsideDomain(coordinate, BigInt(1))
    )

    val validated: ValidatedNec[ModelViolation[instrument.roles.settle.D], Unit] =
      (identity, position, settlement, domainStart, domainEnd).mapN((_, _, _, _, _) => ())

    validated
      .leftMap: violations =>
        violationsFrom(violations.toChain.toVector)
      .toEither
      .map: _ =>
        val typed = assessment.asInstanceOf[
          LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D]
        ]
        construct(
          instrument.identity.id,
          expectedPosition,
          expectedSettlement,
          typed.lots.count,
          CurveConstructionCost(1, 0, 0),
          _ => typed.lots,
          TableLossFormula(Vector(typed.downsideRisk.unrefined))
        )
  end single

  /** Compact exact affine loss with signed first-lot loss and a refined nonnegative marginal loss. */
  def affine(
    instrument: Instrument
  )(
    cap: PositiveWhole,
    firstLotLoss: Quantity[instrument.roles.settle.D],
    additionalLotLoss: NonNegative[Quantity[instrument.roles.settle.D]]
  ): MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D] =
    construct(
      instrument.identity.id,
      instrument.roles.position.dimension.ref,
      instrument.roles.settle.dimension.ref,
      cap,
      CurveConstructionCost(1, 0, 0),
      count => lotsFromInstrument(instrument)(count),
      AffineLossFormula(firstLotLoss, additionalLotLoss)
    )

  /** Checked contiguous piecewise loss whose validation cost follows only the explicit segment vector. */
  def piecewise(
    instrument: Instrument
  )(
    cap: PositiveWhole,
    segments: Vector[LossSegment[instrument.roles.settle.D]]
  ): Either[
    ModelViolations[instrument.roles.settle.D],
    MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    type S = instrument.roles.settle.D

    val emptyCheck =
      Vector(check[S](segments.nonEmpty, EmptyModelDomain()))

    val segmentChecks = segments.zipWithIndex.flatMap: (segment, index) =>
      Vector(
        check[S](segment.start >= 1 && segment.start <= cap.unrefined,
          CoordinateOutsideDomain(segment.start, cap.unrefined)),
        check[S](segment.end >= 1 && segment.end <= cap.unrefined,
          CoordinateOutsideDomain(segment.end, cap.unrefined)),
        check[S](segment.start <= segment.end,
          InvalidBreakpointOrder(index, segment.start, segment.end)),
        check[S](segment.additionalLotLoss.coefficient.signum >= 0,
          NegativeMarginalLoss(index, segment.additionalLotLoss))
      )

    val startCoverage = segments.headOption.toVector.map: first =>
      check[S](first.start == 1, MissingCoordinate(BigInt(1)))

    val adjacencyChecks = segments.zip(segments.drop(1)).zipWithIndex.flatMap:
      case ((previous, next), offset) =>
        val nextIndex       = offset + 1
        val previousEndLoss =
          previous.startLoss + previous.additionalLotLoss * Rational(previous.end - previous.start)
        Vector(
          check[S](next.start == previous.end + 1,
            InvalidBreakpointOrder(nextIndex, previous.end, next.start)),
          check[S](next.startLoss.coefficient.compare(previousEndLoss.coefficient) >= 0,
            DownwardBoundary(nextIndex, previousEndLoss, next.startLoss))
        )

    val endCoverage = segments.lastOption.toVector.map: last =>
      val missing = if last.end < cap.unrefined then last.end + 1 else cap.unrefined
      check[S](last.end == cap.unrefined, MissingCoordinate(missing))

    validate(emptyCheck ++ segmentChecks ++ startCoverage ++ adjacencyChecks ++ endCoverage):
      val normalized = segments.map: segment =>
        NormalizedLossSegment(
          segment.start,
          segment.end,
          segment.startLoss,
          NonNegative(segment.additionalLotLoss).toOption.get
        )
      construct(
        instrument.identity.id,
        instrument.roles.position.dimension.ref,
        instrument.roles.settle.dimension.ref,
        cap,
        CurveConstructionCost(1, BigInt((segments.size - 1).max(0)), 0),
        count => lotsFromInstrument(instrument)(count),
        PiecewiseLossFormula(normalized)
      )
  end piecewise

  private enum CompositionKind:
    case Add, Minimum, Maximum

  private def combine[D <: Dim, S <: Dim](
    left: MonotoneLotRisk[D, S],
    right: MonotoneLotRisk[? <: Dim, ? <: Dim],
    kind: CompositionKind
  ): Either[ModelViolations[S], MonotoneLotRisk[D, S]] =
    val checks = Vector(
      check[S](left.instrumentId == right.instrumentId,
        IncompatibleCurveComposition(CurveCompatibility.InstrumentIdentity)),
      check[S](left.positionDimension.key == right.positionDimension.key,
        IncompatibleCurveComposition(CurveCompatibility.PositionDimension)),
      check[S](left.settlementDimension.key == right.settlementDimension.key,
        IncompatibleCurveComposition(CurveCompatibility.SettlementDimension)),
      check[S](left.cap == right.cap,
        IncompatibleCurveComposition(CurveCompatibility.DomainCap))
    )

    validate(checks):
      val rightTyped = right.asInstanceOf[MonotoneLotRisk[? <: Dim, S]]
      val formula    =
        kind match
          case CompositionKind.Add     => AddedLossFormula(formulaOf(left), formulaOf(rightTyped))
          case CompositionKind.Minimum => MinimumLossFormula(formulaOf(left), formulaOf(rightTyped))
          case CompositionKind.Maximum => MaximumLossFormula(formulaOf(left), formulaOf(rightTyped))
      construct(
        left.instrumentId,
        left.positionDimension,
        left.settlementDimension,
        left.cap,
        left.constructionCost.combine(right.constructionCost),
        count => observe(left, count).lots,
        formula
      )
  end combine

  /** Pointwise signed-loss addition for compatible certified curves. */
  def add[D <: Dim, S <: Dim](
    left: MonotoneLotRisk[D, S],
    right: MonotoneLotRisk[? <: Dim, ? <: Dim]
  ): Either[ModelViolations[S], MonotoneLotRisk[D, S]] =
    combine(left, right, CompositionKind.Add)

  /** Pointwise signed-loss minimum for compatible certified curves. */
  def minimum[D <: Dim, S <: Dim](
    left: MonotoneLotRisk[D, S],
    right: MonotoneLotRisk[? <: Dim, ? <: Dim]
  ): Either[ModelViolations[S], MonotoneLotRisk[D, S]] =
    combine(left, right, CompositionKind.Minimum)

  /** Pointwise signed-loss maximum for compatible certified curves. */
  def maximum[D <: Dim, S <: Dim](
    left: MonotoneLotRisk[D, S],
    right: MonotoneLotRisk[? <: Dim, ? <: Dim]
  ): Either[ModelViolations[S], MonotoneLotRisk[D, S]] =
    combine(left, right, CompositionKind.Maximum)

  /** Pointwise order-preserving projection onto one compatible uniform settlement grid. */
  def quantized[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S]
  )(
    grid: GridRef[S],
    policy: OrderPreservingQuantization
  ): Either[ModelViolations[S], MonotoneLotRisk[D, S]] =
    validate(
      Vector(
        check[S](
          model.settlementDimension.key == grid.dimension.key,
          ModelDimensionMismatch(
            ModelDimensionLocation.Settlement,
            model.settlementDimension.key,
            grid.dimension.key
          )
        )
      )
    )(constructQuantized(model, grid, policy))

  private def constructQuantized[D <: Dim, S <: Dim](
    model: MonotoneLotRisk[D, S],
    grid: GridRef[S],
    policy: OrderPreservingQuantization
  ): MonotoneLotRisk[D, S] =
    val cost = model.constructionCost.copy(
      expressionNodes = model.constructionCost.expressionNodes + 1
    )
    // format: off
    val formula = LotLossFormula.quantized(formulaOf(model))(grid)(policy)
    // format: on
    construct(
      model.instrumentId,
      model.positionDimension,
      model.settlementDimension,
      model.cap,
      cost,
      count => observe(model, count).lots,
      formula
    )

  // format: off
  // scalafmt 3.10.7 exhausts its search state on this dependent existential table signature.
  /**
   * Validate a complete ordered finite table. This is the deliberate `O(cap)` model-construction route.
   */
  def fromCompleteTable(
    instrument: Instrument
  )(
    cap: PositiveWhole,
    observations: Vector[(Lots[? <: Dim], Pnl[? <: Dim])]
  ): Either[
    ModelViolations[instrument.roles.settle.D],
    MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D]
  ] =
    type S = instrument.roles.settle.D
    val expectedPosition   = instrument.roles.position.dimension.key
    val expectedSettlement = instrument.roles.settle.dimension.key
    val coordinates        = observations.map(_._1.count.unrefined)

    val rowChecks = observations.zipWithIndex.flatMap { case ((lots, pnl), index) =>
      val coordinate = lots.count.unrefined
      Vector(
        check[S](lots.instrumentId == instrument.identity.id,
          ObservationInstrumentMismatch(
            index,
            AssessmentInputLocation.Lots,
            instrument.identity.id,
            lots.instrumentId
          )),
        check[S](pnl.instrumentId == instrument.identity.id,
          ObservationInstrumentMismatch(
            index,
            AssessmentInputLocation.Pnl,
            instrument.identity.id,
            pnl.instrumentId
          )),
        check[S](lots.grid.dimension.key == expectedPosition,
          ObservationDimensionMismatch(
            index,
            ModelDimensionLocation.Position,
            expectedPosition,
            lots.grid.dimension.key
          )),
        check[S](pnl.settlement.dimension.key == expectedSettlement,
          ObservationDimensionMismatch(
            index,
            ModelDimensionLocation.Settlement,
            expectedSettlement,
            pnl.settlement.dimension.key
          )),
        check[S](coordinate >= 1 && coordinate <= cap.unrefined,
          CoordinateOutsideDomain(coordinate, cap.unrefined))
      )
    }

    val orderChecks = coordinates.zip(coordinates.drop(1)).zipWithIndex.map:
      case ((previous, next), index) =>
        check[S](previous < next, InvalidObservationOrder(index + 1, previous, next))

    val duplicateCoordinates =
      coordinates.foldLeft((Set.empty[BigInt], Vector.empty[BigInt])):
        case ((seen, duplicates), coordinate) =>
          if seen(coordinate) then (seen, duplicates :+ coordinate)
          else (seen + coordinate, duplicates)
      ._2
    val duplicateChecks = duplicateCoordinates.map(coordinate =>
      check[S](condition = false, DuplicateCoordinate(coordinate)))

    val coverageChecks = Vector(
      check[S](observations.nonEmpty, EmptyModelDomain())
    ) ++ firstMissingCoordinate(coordinates, cap.unrefined).toVector.map: missing =>
      check[S](condition = false, MissingCoordinate(missing))

    validate(rowChecks ++ orderChecks ++ duplicateChecks ++ coverageChecks):
      val assessments = observations.map { case (lots, pnl) =>
        LotRiskAssessment
          .fromPnl(instrument)(
            lots.asInstanceOf[instrument.Lots],
            pnl.asInstanceOf[instrument.Pnl]
          )
          .fold(
            _ => throw new IllegalStateException("validated table row failed checked assessment construction"),
            identity
          )
      }

      val monotonicityChecks = assessments.zip(assessments.drop(1)).zipWithIndex.map:
        case ((previous, next), index) =>
          check[S](
            previous.downsideRisk.unrefined.coefficient.compare(next.downsideRisk.unrefined.coefficient) <= 0,
            DownwardBoundary(index + 1, previous.downsideRisk.unrefined, next.downsideRisk.unrefined)
          )

      validate(monotonicityChecks):
        construct(
          instrument.identity.id,
          instrument.roles.position.dimension.ref,
          instrument.roles.settle.dimension.ref,
          cap,
          CurveConstructionCost(1, 0, BigInt(observations.size)),
          count => assessments(count.unrefined.toInt - 1).lots,
          TableLossFormula(assessments.map(_.downsideRisk.unrefined))
        )
    .flatten
  end fromCompleteTable

  private def firstMissingCoordinate(coordinates: Vector[BigInt], cap: BigInt): Option[BigInt] =
    val present = coordinates.iterator.filter(value => value >= 1 && value <= cap).toSet

    @scala.annotation.tailrec
    def loop(expected: BigInt): Option[BigInt] =
      if expected > cap then None
      else if present(expected) then loop(expected + 1)
      else Some(expected)

    loop(BigInt(1))
  // format: on
end MonotoneLotRisk
