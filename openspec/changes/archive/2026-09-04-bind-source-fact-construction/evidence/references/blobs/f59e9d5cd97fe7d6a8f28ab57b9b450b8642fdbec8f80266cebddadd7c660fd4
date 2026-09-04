package trading.execution

import trading.economics.instrument.InstrumentId
import trading.economics.instrument.Lots
import trading.economics.instrument.Price
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported
import trading.reference.GridIdentity

enum SourceFactLocation extends JavaSerializationUnsupported:
  case Lifecycle
  case Event
  case LogicalExecutionOrder
  case SourceOrder
  case Fill
  case Lots
  case Price
  case Ordering
  case CorrectionTarget
  case BustTarget
  case Checkpoint
  case Completeness

sealed abstract class SourceFactViolation extends JavaSerializationUnsupported with Product with Serializable

final case class MissingSourceFactValue(location: SourceFactLocation) extends SourceFactViolation

final case class SourceFactLogicalOrderMismatch(
  expected: ExecutionOrderId,
  supplied: ExecutionOrderId)
  extends SourceFactViolation

final case class SourceFactTargetMismatch(
  location: SourceFactLocation,
  expected: ExecutionTarget,
  supplied: ExecutionTarget)
  extends SourceFactViolation

final case class SourceFactInstrumentMismatch(
  location: SourceFactLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends SourceFactViolation

final case class SourceFactGridMismatch(
  location: SourceFactLocation,
  expected: GridIdentity,
  supplied: GridIdentity)
  extends SourceFactViolation

final class SourceFactViolations private (private val values: Vector[SourceFactViolation])
  extends JavaSerializationUnsupported:

  def head: SourceFactViolation             = values.head
  def toVector: Vector[SourceFactViolation] = values
  def size: Int                             = values.size

  override def equals(other: Any): Boolean = other match
    case that: SourceFactViolations => values == that.toVector
    case _                          => false

  override def hashCode(): Int  = values.hashCode
  override def toString: String = values.mkString("SourceFactViolations(", ",", ")")

object SourceFactViolations:
  private def construct(values: Vector[SourceFactViolation]): SourceFactViolations =
    new SourceFactViolations(values)

  def one(value: SourceFactViolation): SourceFactViolations = construct(Vector(value))

  private[execution] def from(values: Vector[SourceFactViolation]): Option[SourceFactViolations] =
    Option.when(values.nonEmpty)(construct(values))

private object SourceFactValidation:
  def fact[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    value: SourceFact[D, B, Q]
  ): Vector[SourceFactViolation] =
    if value == null then Vector(MissingSourceFactValue(SourceFactLocation.Event))
    else
      val base = common(
        lifecycle,
        value.eventId,
        value.executionOrderId,
        value.sourceOrderId,
        value.ordering
      )
      val specific = value match
        case fill: ExecutionFill[D, B, Q] =>
          qualifiedTarget(lifecycle, SourceFactLocation.Fill, fill.fillId.target) ++
            economics(lifecycle, fill.lots, fill.price)
        case correction: FillCorrected[D, B, Q] =>
          qualifiedTarget(
            lifecycle,
            SourceFactLocation.CorrectionTarget,
            correction.referencedFillId.target
          ) ++ economics(lifecycle, correction.replacementLots, correction.replacementPrice)
        case bust: FillBusted[D, B, Q] =>
          qualifiedTarget(lifecycle, SourceFactLocation.BustTarget, bust.referencedFillId.target)
        case reconciliation: ReconciliationCheckpoint[D, B, Q] =>
          qualifiedTarget(
            lifecycle,
            SourceFactLocation.Checkpoint,
            reconciliation.checkpoint.position.stream.target
          )
        case complete: SourceOrderCompleted[D, B, Q] =>
          qualifiedTarget(
            lifecycle,
            SourceFactLocation.Completeness,
            complete.completeness.completeThrough.stream.target
          )
        case absent: SourceOrderAbsent[D, B, Q] =>
          qualifiedTarget(
            lifecycle,
            SourceFactLocation.Completeness,
            absent.completeness.completeThrough.stream.target
          )
        case _: OrderAccepted[D, B, Q]         => Vector.empty
        case _: OrderRejected[D, B, Q]         => Vector.empty
        case _: CancellationEffective[D, B, Q] => Vector.empty
      base ++ specific

  def common[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): Vector[SourceFactViolation] =
    val missing = Vector(
      Option.when(lifecycle == null)(MissingSourceFactValue(SourceFactLocation.Lifecycle)),
      Option.when(eventId == null)(MissingSourceFactValue(SourceFactLocation.Event)),
      Option.when(executionOrderId == null)(MissingSourceFactValue(SourceFactLocation.LogicalExecutionOrder)),
      Option.when(sourceOrderId == null)(MissingSourceFactValue(SourceFactLocation.SourceOrder)),
      Option.when(ordering == null)(MissingSourceFactValue(SourceFactLocation.Ordering))
    ).flatten
    if lifecycle == null then missing
    else
      missing ++ Vector(
        Option.when(executionOrderId != null && lifecycle.executionOrderId != executionOrderId)(
          SourceFactLogicalOrderMismatch(lifecycle.executionOrderId, executionOrderId)
        ),
        Option.when(eventId != null && lifecycle.target != eventId.target)(
          SourceFactTargetMismatch(SourceFactLocation.Event, lifecycle.target, eventId.target)
        ),
        Option.when(sourceOrderId != null && lifecycle.target != sourceOrderId.target)(
          SourceFactTargetMismatch(SourceFactLocation.SourceOrder, lifecycle.target, sourceOrderId.target)
        ),
        orderingTarget(ordering).filter(_ != lifecycle.target).map: supplied =>
          SourceFactTargetMismatch(SourceFactLocation.Ordering, lifecycle.target, supplied)
      ).flatten
  end common

  def qualifiedTarget[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    location: SourceFactLocation,
    supplied: ExecutionTarget
  ): Vector[SourceFactViolation] =
    if lifecycle == null || supplied == null then Vector.empty
    else
      Vector(
        Option.when(lifecycle.target != supplied)(
          SourceFactTargetMismatch(location, lifecycle.target, supplied)
        )
      ).flatten

  def economics[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    lots: Lots[D],
    price: Price[B, Q]
  ): Vector[SourceFactViolation] =
    if lifecycle == null then Vector.empty
    else
      val expectedInstrument = lifecycle.instrumentId
      Vector(
        Option.when(lots != null && lots.instrumentId != expectedInstrument)(
          SourceFactInstrumentMismatch(SourceFactLocation.Lots, expectedInstrument, lots.instrumentId)
        ),
        Option.when(price != null && price.instrumentId != expectedInstrument)(
          SourceFactInstrumentMismatch(SourceFactLocation.Price, expectedInstrument, price.instrumentId)
        ),
        Option.when(
          lots != null &&
            (lots.grid.identity != lifecycle.positionGrid.identity ||
              lots.grid.quantum != lifecycle.positionGrid.quantum)
        )(
          SourceFactGridMismatch(SourceFactLocation.Lots, lifecycle.positionGrid.identity, lots.grid.identity)
        ),
        Option.when(
          price != null &&
            (price.grid.identity != lifecycle.instrument.priceGrid.identity ||
              price.grid.quantum != lifecycle.instrument.priceGrid.quantum)
        )(
          SourceFactGridMismatch(SourceFactLocation.Price, lifecycle.instrument.priceGrid.identity, price.grid.identity)
        )
      ).flatten

  private def orderingTarget(ordering: SourceOrdering): Option[ExecutionTarget] =
    if ordering == null then None
    else
      ordering match
        case ExplicitlyUnsequenced               => None
        case sequenced: AuthoritativelySequenced => Some(sequenced.position.stream.target)
end SourceFactValidation

sealed abstract class SourceFact[D <: Dim, B <: Dim, Q <: Dim] protected () extends JavaSerializationUnsupported:
  def eventId: QualifiedSourceEventId
  def executionOrderId: ExecutionOrderId
  def sourceOrderId: QualifiedSourceOrderId
  def ordering: SourceOrdering

  final def target: ExecutionTarget                                = eventId.target
  final def authoritativePosition: Option[QualifiedStreamPosition] = ordering match
    case ExplicitlyUnsequenced               => None
    case sequenced: AuthoritativelySequenced => Some(sequenced.position)

object SourceFact:
  final class LifecycleScope[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
    val lifecycle: ExecutionLifecycle[D, B, Q]):

    def accepted(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, OrderAccepted[D, B, Q]] =
      OrderAccepted.create(lifecycle)(eventId, executionOrderId, sourceOrderId, ordering)

    def rejected(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, OrderRejected[D, B, Q]] =
      OrderRejected.create(lifecycle)(eventId, executionOrderId, sourceOrderId, ordering)

    def fill(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      fillId: QualifiedFillId,
      lots: Lots[D],
      price: Price[B, Q],
      ordering: SourceOrdering
    ): Either[SourceFactViolations, ExecutionFill[D, B, Q]] =
      ExecutionFill.create(lifecycle)(eventId, executionOrderId, sourceOrderId, fillId, lots, price, ordering)

    def corrected(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      referencedFillId: QualifiedFillId,
      replacementLots: Lots[D],
      replacementPrice: Price[B, Q],
      ordering: SourceOrdering
    ): Either[SourceFactViolations, FillCorrected[D, B, Q]] =
      FillCorrected.create(lifecycle)(
        eventId,
        executionOrderId,
        sourceOrderId,
        referencedFillId,
        replacementLots,
        replacementPrice,
        ordering
      )

    def busted(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      referencedFillId: QualifiedFillId,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, FillBusted[D, B, Q]] =
      FillBusted.create(lifecycle)(eventId, executionOrderId, sourceOrderId, referencedFillId, ordering)

    def cancellationEffective(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, CancellationEffective[D, B, Q]] =
      CancellationEffective.create(lifecycle)(eventId, executionOrderId, sourceOrderId, ordering)

    def reconciliationCheckpoint(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      checkpoint: SourceCheckpoint,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, ReconciliationCheckpoint[D, B, Q]] =
      ReconciliationCheckpoint.create(lifecycle)(
        eventId,
        executionOrderId,
        sourceOrderId,
        checkpoint,
        ordering
      )

    def sourceOrderCompleted(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      completeness: SourceCompleteness,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, SourceOrderCompleted[D, B, Q]] =
      SourceOrderCompleted.create(lifecycle)(
        eventId,
        executionOrderId,
        sourceOrderId,
        completeness,
        ordering
      )

    def sourceOrderAbsent(
      eventId: QualifiedSourceEventId,
      executionOrderId: ExecutionOrderId,
      sourceOrderId: QualifiedSourceOrderId,
      completeness: SourceCompleteness,
      ordering: SourceOrdering
    ): Either[SourceFactViolations, SourceOrderAbsent[D, B, Q]] =
      SourceOrderAbsent.create(lifecycle)(
        eventId,
        executionOrderId,
        sourceOrderId,
        completeness,
        ordering
      )
  end LifecycleScope

  def forLifecycle[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  ): LifecycleScope[D, B, Q] =
    new LifecycleScope(lifecycle)
end SourceFact

sealed abstract class FillModifier[D <: Dim, B <: Dim, Q <: Dim] protected () extends SourceFact[D, B, Q]():
  def referencedFillId: QualifiedFillId

private object SourceFactEquality:
  def common(left: SourceFact[?, ?, ?], right: SourceFact[?, ?, ?]): Boolean =
    left.eventId == right.eventId && left.executionOrderId == right.executionOrderId &&
      left.sourceOrderId == right.sourceOrderId && left.ordering == right.ordering

  def commonHash(value: SourceFact[?, ?, ?]): Int =
    (value.eventId, value.executionOrderId, value.sourceOrderId, value.ordering).hashCode

  def sameFillBody(left: ExecutionFill[?, ?, ?], right: ExecutionFill[?, ?, ?]): Boolean =
    left.executionOrderId == right.executionOrderId && left.sourceOrderId == right.sourceOrderId &&
      left.fillId == right.fillId && left.lots == right.lots && left.price == right.price &&
      left.ordering == right.ordering

final class OrderAccepted[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: OrderAccepted[?, ?, ?] => SourceFactEquality.common(this, that)
    case _                            => false
  override def hashCode(): Int = ("accepted", SourceFactEquality.commonHash(this)).hashCode

object OrderAccepted:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): OrderAccepted[D, B, Q] =
    new OrderAccepted(eventId, executionOrderId, sourceOrderId, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, OrderAccepted[D, B, Q]] =
    SourceFactViolations
      .from(SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering))
      .toLeft(construct(eventId, executionOrderId, sourceOrderId, ordering))
end OrderAccepted

final class OrderRejected[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: OrderRejected[?, ?, ?] => SourceFactEquality.common(this, that)
    case _                            => false
  override def hashCode(): Int = ("rejected", SourceFactEquality.commonHash(this)).hashCode

object OrderRejected:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): OrderRejected[D, B, Q] =
    new OrderRejected(eventId, executionOrderId, sourceOrderId, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, OrderRejected[D, B, Q]] =
    SourceFactViolations
      .from(SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering))
      .toLeft(construct(eventId, executionOrderId, sourceOrderId, ordering))
end OrderRejected

final class ExecutionFill[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val fillId: QualifiedFillId,
  val lots: Lots[D],
  val price: Price[B, Q],
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ExecutionFill[?, ?, ?] =>
      SourceFactEquality.common(this, that) && fillId == that.fillId && lots == that.lots && price == that.price
    case _ => false
  override def hashCode(): Int =
    ("fill", SourceFactEquality.commonHash(this), fillId, lots, price).hashCode
end ExecutionFill

object ExecutionFill:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    fillId: QualifiedFillId,
    lots: Lots[D],
    price: Price[B, Q],
    ordering: SourceOrdering
  ): ExecutionFill[D, B, Q] =
    new ExecutionFill(eventId, executionOrderId, sourceOrderId, fillId, lots, price, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    fillId: QualifiedFillId,
    lots: Lots[D],
    price: Price[B, Q],
    ordering: SourceOrdering
  ): Either[SourceFactViolations, ExecutionFill[D, B, Q]] =
    val missing = Vector(
      Option.when(fillId == null)(MissingSourceFactValue(SourceFactLocation.Fill)),
      Option.when(lots == null)(MissingSourceFactValue(SourceFactLocation.Lots)),
      Option.when(price == null)(MissingSourceFactValue(SourceFactLocation.Price))
    ).flatten
    val target =
      if fillId == null then Vector.empty
      else SourceFactValidation.qualifiedTarget(lifecycle, SourceFactLocation.Fill, fillId.target)
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++
        missing ++ target ++ SourceFactValidation.economics(lifecycle, lots, price)
    SourceFactViolations.from(violations).toLeft(
      construct(eventId, executionOrderId, sourceOrderId, fillId, lots, price, ordering)
    )
  end create
end ExecutionFill

final class FillCorrected[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val referencedFillId: QualifiedFillId,
  val replacementLots: Lots[D],
  val replacementPrice: Price[B, Q],
  val ordering: SourceOrdering)
  extends FillModifier[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: FillCorrected[?, ?, ?] =>
      SourceFactEquality.common(this, that) && referencedFillId == that.referencedFillId &&
      replacementLots == that.replacementLots && replacementPrice == that.replacementPrice
    case _ => false
  override def hashCode(): Int =
    ("correction", SourceFactEquality.commonHash(this), referencedFillId, replacementLots, replacementPrice).hashCode

object FillCorrected:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    referencedFillId: QualifiedFillId,
    replacementLots: Lots[D],
    replacementPrice: Price[B, Q],
    ordering: SourceOrdering
  ): FillCorrected[D, B, Q] =
    new FillCorrected(eventId, executionOrderId, sourceOrderId, referencedFillId, replacementLots, replacementPrice,
      ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    referencedFillId: QualifiedFillId,
    replacementLots: Lots[D],
    replacementPrice: Price[B, Q],
    ordering: SourceOrdering
  ): Either[SourceFactViolations, FillCorrected[D, B, Q]] =
    val missing = Vector(
      Option.when(referencedFillId == null)(MissingSourceFactValue(SourceFactLocation.CorrectionTarget)),
      Option.when(replacementLots == null)(MissingSourceFactValue(SourceFactLocation.Lots)),
      Option.when(replacementPrice == null)(MissingSourceFactValue(SourceFactLocation.Price))
    ).flatten
    val target =
      if referencedFillId == null then Vector.empty
      else SourceFactValidation.qualifiedTarget(lifecycle, SourceFactLocation.CorrectionTarget, referencedFillId.target)
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++
        missing ++ target ++ SourceFactValidation.economics(lifecycle, replacementLots, replacementPrice)
    SourceFactViolations.from(violations).toLeft(
      construct(
        eventId,
        executionOrderId,
        sourceOrderId,
        referencedFillId,
        replacementLots,
        replacementPrice,
        ordering
      )
    )
  end create
end FillCorrected

final class FillBusted[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val referencedFillId: QualifiedFillId,
  val ordering: SourceOrdering)
  extends FillModifier[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: FillBusted[?, ?, ?] =>
      SourceFactEquality.common(this, that) && referencedFillId == that.referencedFillId
    case _ => false
  override def hashCode(): Int = ("bust", SourceFactEquality.commonHash(this), referencedFillId).hashCode

object FillBusted:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    referencedFillId: QualifiedFillId,
    ordering: SourceOrdering
  ): FillBusted[D, B, Q] =
    new FillBusted(eventId, executionOrderId, sourceOrderId, referencedFillId, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    referencedFillId: QualifiedFillId,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, FillBusted[D, B, Q]] =
    val missing =
      Vector(Option.when(referencedFillId == null)(MissingSourceFactValue(SourceFactLocation.BustTarget))).flatten
    val target =
      if referencedFillId == null then Vector.empty
      else SourceFactValidation.qualifiedTarget(lifecycle, SourceFactLocation.BustTarget, referencedFillId.target)
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++ missing ++ target
    SourceFactViolations.from(violations).toLeft(
      construct(eventId, executionOrderId, sourceOrderId, referencedFillId, ordering)
    )
end FillBusted

final class CancellationEffective[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: CancellationEffective[?, ?, ?] => SourceFactEquality.common(this, that)
    case _                                    => false
  override def hashCode(): Int = ("cancellation-effective", SourceFactEquality.commonHash(this)).hashCode

object CancellationEffective:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): CancellationEffective[D, B, Q] =
    new CancellationEffective(eventId, executionOrderId, sourceOrderId, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, CancellationEffective[D, B, Q]] =
    SourceFactViolations
      .from(SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering))
      .toLeft(construct(eventId, executionOrderId, sourceOrderId, ordering))
end CancellationEffective

final class ReconciliationCheckpoint[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val checkpoint: SourceCheckpoint,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ReconciliationCheckpoint[?, ?, ?] =>
      SourceFactEquality.common(this, that) && checkpoint == that.checkpoint
    case _ => false
  override def hashCode(): Int = ("reconciliation", SourceFactEquality.commonHash(this), checkpoint).hashCode

object ReconciliationCheckpoint:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    checkpoint: SourceCheckpoint,
    ordering: SourceOrdering
  ): ReconciliationCheckpoint[D, B, Q] =
    new ReconciliationCheckpoint(eventId, executionOrderId, sourceOrderId, checkpoint, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    checkpoint: SourceCheckpoint,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, ReconciliationCheckpoint[D, B, Q]] =
    val missing =
      Vector(Option.when(checkpoint == null)(MissingSourceFactValue(SourceFactLocation.Checkpoint))).flatten
    val target =
      if checkpoint == null then Vector.empty
      else
        SourceFactValidation.qualifiedTarget(
          lifecycle,
          SourceFactLocation.Checkpoint,
          checkpoint.position.stream.target
        )
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++ missing ++ target
    SourceFactViolations.from(violations).toLeft(
      construct(eventId, executionOrderId, sourceOrderId, checkpoint, ordering)
    )
  end create
end ReconciliationCheckpoint

final class SourceOrderCompleted[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val completeness: SourceCompleteness,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: SourceOrderCompleted[?, ?, ?] =>
      SourceFactEquality.common(this, that) && completeness == that.completeness
    case _ => false
  override def hashCode(): Int = ("complete", SourceFactEquality.commonHash(this), completeness).hashCode

object SourceOrderCompleted:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    completeness: SourceCompleteness,
    ordering: SourceOrdering
  ): SourceOrderCompleted[D, B, Q] =
    new SourceOrderCompleted(eventId, executionOrderId, sourceOrderId, completeness, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    completeness: SourceCompleteness,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, SourceOrderCompleted[D, B, Q]] =
    val missing =
      Vector(Option.when(completeness == null)(MissingSourceFactValue(SourceFactLocation.Completeness))).flatten
    val target =
      if completeness == null then Vector.empty
      else
        SourceFactValidation.qualifiedTarget(
          lifecycle,
          SourceFactLocation.Completeness,
          completeness.completeThrough.stream.target
        )
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++ missing ++ target
    SourceFactViolations.from(violations).toLeft(
      construct(eventId, executionOrderId, sourceOrderId, completeness, ordering)
    )
  end create
end SourceOrderCompleted

/** An explicit source lookup reporting that the qualified source order is absent through a declared boundary. */
final class SourceOrderAbsent[D <: Dim, B <: Dim, Q <: Dim] private (
  val eventId: QualifiedSourceEventId,
  val executionOrderId: ExecutionOrderId,
  val sourceOrderId: QualifiedSourceOrderId,
  val completeness: SourceCompleteness,
  val ordering: SourceOrdering)
  extends SourceFact[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: SourceOrderAbsent[?, ?, ?] =>
      SourceFactEquality.common(this, that) && completeness == that.completeness
    case _ => false
  override def hashCode(): Int = ("absent", SourceFactEquality.commonHash(this), completeness).hashCode

object SourceOrderAbsent:
  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    completeness: SourceCompleteness,
    ordering: SourceOrdering
  ): SourceOrderAbsent[D, B, Q] =
    new SourceOrderAbsent(eventId, executionOrderId, sourceOrderId, completeness, ordering)

  def create[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    eventId: QualifiedSourceEventId,
    executionOrderId: ExecutionOrderId,
    sourceOrderId: QualifiedSourceOrderId,
    completeness: SourceCompleteness,
    ordering: SourceOrdering
  ): Either[SourceFactViolations, SourceOrderAbsent[D, B, Q]] =
    val missing =
      Vector(Option.when(completeness == null)(MissingSourceFactValue(SourceFactLocation.Completeness))).flatten
    val target =
      if completeness == null then Vector.empty
      else
        SourceFactValidation.qualifiedTarget(
          lifecycle,
          SourceFactLocation.Completeness,
          completeness.completeThrough.stream.target
        )
    val violations =
      SourceFactValidation.common(lifecycle, eventId, executionOrderId, sourceOrderId, ordering) ++ missing ++ target
    SourceFactViolations.from(violations).toLeft(
      construct(eventId, executionOrderId, sourceOrderId, completeness, ordering)
    )
  end create
end SourceOrderAbsent
