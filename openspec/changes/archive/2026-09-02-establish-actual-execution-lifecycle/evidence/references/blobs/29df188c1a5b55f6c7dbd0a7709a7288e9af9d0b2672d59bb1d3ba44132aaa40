package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.economics.instrument.InstrumentId
import trading.quantity.JavaSerializationUnsupported

enum ExecutionConstructionLocation extends JavaSerializationUnsupported:
  case Instrument
  case Order
  case OrderIntent
  case OrderLots
  case LogicalExecutionOrder
  case Lineage
  case Target
  case Source
  case Account
  case NativeSourceEvent
  case NativeSourceOrder
  case NativeFill
  case SourceStream
  case SourceSequence
  case Continuation
  case Checkpoint
  case Completeness

sealed abstract class ExecutionConstructionViolation extends JavaSerializationUnsupported with Product with Serializable

final case class MissingExecutionValue(location: ExecutionConstructionLocation) extends ExecutionConstructionViolation

final case class LifecycleInstrumentMismatch(
  location: ExecutionConstructionLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends ExecutionConstructionViolation

final case class StreamScopeMismatch(
  location: ExecutionConstructionLocation,
  expected: QualifiedSourceStreamId,
  supplied: QualifiedSourceStreamId)
  extends ExecutionConstructionViolation

/** Public deterministic non-empty construction failures. */
@nowarn("msg=Ignoring.*qualifier")
final class ExecutionConstructionErrors private[this] (
  private val values: Vector[ExecutionConstructionViolation])
  extends JavaSerializationUnsupported:

  def head: ExecutionConstructionViolation             = values.head
  def toVector: Vector[ExecutionConstructionViolation] = values
  def size: Int                                        = values.size

  override def equals(other: Any): Boolean = other match
    case that: ExecutionConstructionErrors => values == that.toVector
    case _                                 => false

  override def hashCode(): Int  = values.hashCode
  override def toString: String = values.mkString("ExecutionConstructionErrors(", ",", ")")

object ExecutionConstructionErrors:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionConstructionErrors], MethodHandles.lookup())
      .findConstructor(
        classOf[ExecutionConstructionErrors],
        MethodType.methodType(classOf[Unit], classOf[Vector[?]])
      )

  private def construct(values: Vector[ExecutionConstructionViolation]): ExecutionConstructionErrors =
    constructor.invoke(values).asInstanceOf[ExecutionConstructionErrors]

  def one(value: ExecutionConstructionViolation): ExecutionConstructionErrors =
    construct(Vector(value))

  private[execution] def from(
    values: Vector[ExecutionConstructionViolation]
  ): Option[ExecutionConstructionErrors] =
    Option.when(values.nonEmpty)(construct(values))

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionTarget private[this] (
  val source: ExecutionSourceId,
  val account: ExecutionAccountId)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: ExecutionTarget => source == that.source && account == that.account
    case _                     => false

  override def hashCode(): Int  = (source, account).hashCode
  override def toString: String = s"ExecutionTarget($source,$account)"

object ExecutionTarget:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionTarget], MethodHandles.lookup())
      .findConstructor(
        classOf[ExecutionTarget],
        MethodType.methodType(classOf[Unit], classOf[ExecutionSourceId], classOf[ExecutionAccountId])
      )

  private def construct(source: ExecutionSourceId, account: ExecutionAccountId): ExecutionTarget =
    constructor.invoke(source, account).asInstanceOf[ExecutionTarget]

  def create(
    source: ExecutionSourceId,
    account: ExecutionAccountId
  ): Either[ExecutionConstructionErrors, ExecutionTarget] =
    val violations =
      Vector(
        Option.when(source == null)(MissingExecutionValue(ExecutionConstructionLocation.Source)),
        Option.when(account == null)(MissingExecutionValue(ExecutionConstructionLocation.Account))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(source, account))
end ExecutionTarget

@nowarn("msg=Ignoring.*qualifier")
final class QualifiedSourceEventId private[this] (
  val target: ExecutionTarget,
  val native: NativeSourceEventId)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: QualifiedSourceEventId => target == that.target && native == that.native
    case _                            => false

  override def hashCode(): Int  = (target, native).hashCode
  override def toString: String = s"QualifiedSourceEventId($target,$native)"

object QualifiedSourceEventId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[QualifiedSourceEventId], MethodHandles.lookup())
      .findConstructor(
        classOf[QualifiedSourceEventId],
        MethodType.methodType(classOf[Unit], classOf[ExecutionTarget], classOf[NativeSourceEventId])
      )

  private def construct(target: ExecutionTarget, native: NativeSourceEventId): QualifiedSourceEventId =
    constructor.invoke(target, native).asInstanceOf[QualifiedSourceEventId]

  def create(
    target: ExecutionTarget,
    native: NativeSourceEventId
  ): Either[ExecutionConstructionErrors, QualifiedSourceEventId] =
    val violations =
      Vector(
        Option.when(target == null)(MissingExecutionValue(ExecutionConstructionLocation.Target)),
        Option.when(native == null)(MissingExecutionValue(ExecutionConstructionLocation.NativeSourceEvent))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(target, native))
end QualifiedSourceEventId

@nowarn("msg=Ignoring.*qualifier")
final class QualifiedSourceOrderId private[this] (
  val target: ExecutionTarget,
  val native: NativeSourceOrderId)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: QualifiedSourceOrderId => target == that.target && native == that.native
    case _                            => false

  override def hashCode(): Int  = (target, native).hashCode
  override def toString: String = s"QualifiedSourceOrderId($target,$native)"

object QualifiedSourceOrderId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[QualifiedSourceOrderId], MethodHandles.lookup())
      .findConstructor(
        classOf[QualifiedSourceOrderId],
        MethodType.methodType(classOf[Unit], classOf[ExecutionTarget], classOf[NativeSourceOrderId])
      )

  private def construct(target: ExecutionTarget, native: NativeSourceOrderId): QualifiedSourceOrderId =
    constructor.invoke(target, native).asInstanceOf[QualifiedSourceOrderId]

  def create(
    target: ExecutionTarget,
    native: NativeSourceOrderId
  ): Either[ExecutionConstructionErrors, QualifiedSourceOrderId] =
    val violations =
      Vector(
        Option.when(target == null)(MissingExecutionValue(ExecutionConstructionLocation.Target)),
        Option.when(native == null)(MissingExecutionValue(ExecutionConstructionLocation.NativeSourceOrder))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(target, native))
end QualifiedSourceOrderId

@nowarn("msg=Ignoring.*qualifier")
final class QualifiedFillId private[this] (
  val target: ExecutionTarget,
  val native: NativeFillId)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: QualifiedFillId => target == that.target && native == that.native
    case _                     => false

  override def hashCode(): Int  = (target, native).hashCode
  override def toString: String = s"QualifiedFillId($target,$native)"

object QualifiedFillId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[QualifiedFillId], MethodHandles.lookup())
      .findConstructor(
        classOf[QualifiedFillId],
        MethodType.methodType(classOf[Unit], classOf[ExecutionTarget], classOf[NativeFillId])
      )

  private def construct(target: ExecutionTarget, native: NativeFillId): QualifiedFillId =
    constructor.invoke(target, native).asInstanceOf[QualifiedFillId]

  def create(
    target: ExecutionTarget,
    native: NativeFillId
  ): Either[ExecutionConstructionErrors, QualifiedFillId] =
    val violations =
      Vector(
        Option.when(target == null)(MissingExecutionValue(ExecutionConstructionLocation.Target)),
        Option.when(native == null)(MissingExecutionValue(ExecutionConstructionLocation.NativeFill))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(target, native))
end QualifiedFillId

@nowarn("msg=Ignoring.*qualifier")
final class QualifiedSourceStreamId private[this] (
  val target: ExecutionTarget,
  val native: SourceStreamId)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: QualifiedSourceStreamId => target == that.target && native == that.native
    case _                             => false

  override def hashCode(): Int  = (target, native).hashCode
  override def toString: String = s"QualifiedSourceStreamId($target,$native)"

object QualifiedSourceStreamId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[QualifiedSourceStreamId], MethodHandles.lookup())
      .findConstructor(
        classOf[QualifiedSourceStreamId],
        MethodType.methodType(classOf[Unit], classOf[ExecutionTarget], classOf[SourceStreamId])
      )

  private def construct(target: ExecutionTarget, native: SourceStreamId): QualifiedSourceStreamId =
    constructor.invoke(target, native).asInstanceOf[QualifiedSourceStreamId]

  def create(
    target: ExecutionTarget,
    native: SourceStreamId
  ): Either[ExecutionConstructionErrors, QualifiedSourceStreamId] =
    val violations =
      Vector(
        Option.when(target == null)(MissingExecutionValue(ExecutionConstructionLocation.Target)),
        Option.when(native == null)(MissingExecutionValue(ExecutionConstructionLocation.SourceStream))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(target, native))
end QualifiedSourceStreamId

@nowarn("msg=Ignoring.*qualifier")
final class QualifiedStreamPosition private[this] (
  val stream: QualifiedSourceStreamId,
  val sequence: SourceSequence)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: QualifiedStreamPosition => stream == that.stream && sequence == that.sequence
    case _                             => false

  override def hashCode(): Int  = (stream, sequence).hashCode
  override def toString: String = s"QualifiedStreamPosition($stream,$sequence)"

object QualifiedStreamPosition:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[QualifiedStreamPosition], MethodHandles.lookup())
      .findConstructor(
        classOf[QualifiedStreamPosition],
        MethodType.methodType(classOf[Unit], classOf[QualifiedSourceStreamId], classOf[SourceSequence])
      )

  private def construct(
    stream: QualifiedSourceStreamId,
    sequence: SourceSequence
  ): QualifiedStreamPosition =
    constructor.invoke(stream, sequence).asInstanceOf[QualifiedStreamPosition]

  def create(
    stream: QualifiedSourceStreamId,
    sequence: SourceSequence
  ): Either[ExecutionConstructionErrors, QualifiedStreamPosition] =
    val violations =
      Vector(
        Option.when(stream == null)(MissingExecutionValue(ExecutionConstructionLocation.SourceStream)),
        Option.when(sequence == null)(MissingExecutionValue(ExecutionConstructionLocation.SourceSequence))
      ).flatten
    ExecutionConstructionErrors.from(violations).toLeft(construct(stream, sequence))
end QualifiedStreamPosition
