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
final case class IncompatibleCurveComposition[S <: Dim](reason: CurveCompatibility) extends ModelViolation[S]

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
  private val evaluate: PositiveWhole => LotRiskAssessment[D, S])
  extends JavaSerializationUnsupported:

  @nowarn("msg=unused private member")
  private[this] def assess(count: PositiveWhole): LotRiskAssessment[D, S] =
    if count.unrefined > cap.unrefined then
      throw new IllegalArgumentException(
        s"lot coordinate ${count.unrefined} exceeds model cap ${cap.unrefined}"
      )
    else evaluate(count)
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
          classOf[Function1[?, ?]]
        )
      )

  private def construct[D <: Dim, S <: Dim](
    instrumentId: InstrumentId,
    positionDimension: DimRef[D],
    settlementDimension: DimRef[S],
    cap: PositiveWhole,
    evaluate: PositiveWhole => LotRiskAssessment[D, S]
  ): MonotoneLotRisk[D, S] =
    constructor
      .invoke(instrumentId, positionDimension, settlementDimension, cap, evaluate)
      .asInstanceOf[MonotoneLotRisk[D, S]]

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
        val values = violations.toChain.toVector
        values match
          case head +: tail => constructViolations(head, tail)
          case _            => throw new IllegalStateException("ValidatedNec produced an empty violation chain")
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
          _ => typed
        )
  end single
end MonotoneLotRisk
