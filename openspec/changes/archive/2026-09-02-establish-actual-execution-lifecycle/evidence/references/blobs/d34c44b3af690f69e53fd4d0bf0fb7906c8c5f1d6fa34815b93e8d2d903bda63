package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.quantity.JavaSerializationUnsupported

sealed abstract class SourceContinuation protected () extends JavaSerializationUnsupported:
  SourceContinuation.requireBuiltin(this)

  def stream: QualifiedSourceStreamId
  def previous: Option[QualifiedStreamPosition]

object SourceContinuation:
  @nowarn("msg=Ignoring.*qualifier")
  final class StreamOrigin private[this] (val stream: QualifiedSourceStreamId) extends SourceContinuation():
    val previous: Option[QualifiedStreamPosition] = None

    override def equals(other: Any): Boolean = other match
      case that: StreamOrigin => stream == that.stream
      case _                  => false

    override def hashCode(): Int  = stream.hashCode
    override def toString: String = s"StreamOrigin($stream)"

  @nowarn("msg=Ignoring.*qualifier")
  final class ContinuesAfter private[this] (val value: QualifiedStreamPosition) extends SourceContinuation():
    val stream: QualifiedSourceStreamId           = value.stream
    val previous: Option[QualifiedStreamPosition] = Some(value)

    override def equals(other: Any): Boolean = other match
      case that: ContinuesAfter => value == that.value
      case _                    => false

    override def hashCode(): Int  = value.hashCode
    override def toString: String = s"ContinuesAfter($value)"

  private val originConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[StreamOrigin], MethodHandles.lookup())
      .findConstructor(
        classOf[StreamOrigin],
        MethodType.methodType(classOf[Unit], classOf[QualifiedSourceStreamId])
      )

  private val continuationConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ContinuesAfter], MethodHandles.lookup())
      .findConstructor(
        classOf[ContinuesAfter],
        MethodType.methodType(classOf[Unit], classOf[QualifiedStreamPosition])
      )

  private def constructOrigin(stream: QualifiedSourceStreamId): StreamOrigin =
    originConstructor.invoke(stream).asInstanceOf[StreamOrigin]

  private def constructContinuation(previous: QualifiedStreamPosition): ContinuesAfter =
    continuationConstructor.invoke(previous).asInstanceOf[ContinuesAfter]

  def origin(
    stream: QualifiedSourceStreamId
  ): Either[ExecutionConstructionErrors, SourceContinuation] =
    if stream == null then
      Left(ExecutionConstructionErrors.one(MissingExecutionValue(ExecutionConstructionLocation.SourceStream)))
    else Right(constructOrigin(stream))

  def after(
    previous: QualifiedStreamPosition
  ): Either[ExecutionConstructionErrors, SourceContinuation] =
    if previous == null then
      Left(ExecutionConstructionErrors.one(MissingExecutionValue(ExecutionConstructionLocation.Continuation)))
    else Right(constructContinuation(previous))

  private[execution] def requireBuiltin(value: SourceContinuation): Unit =
    val runtimeClass = value.getClass
    if runtimeClass != classOf[StreamOrigin] && runtimeClass != classOf[ContinuesAfter] then
      throw new IllegalAccessError(s"unsupported SourceContinuation implementation: ${runtimeClass.getName}")
end SourceContinuation

sealed abstract class SourceOrdering protected () extends JavaSerializationUnsupported:
  SourceOrdering.requireBuiltin(this)

case object ExplicitlyUnsequenced extends SourceOrdering()

@nowarn("msg=Ignoring.*qualifier")
final class AuthoritativelySequenced private[this] (
  val position: QualifiedStreamPosition,
  val continuation: SourceContinuation)
  extends SourceOrdering():

  override def equals(other: Any): Boolean = other match
    case that: AuthoritativelySequenced =>
      position == that.position && continuation == that.continuation
    case _ => false

  override def hashCode(): Int  = (position, continuation).hashCode
  override def toString: String = s"AuthoritativelySequenced($position,$continuation)"

object SourceOrdering:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[AuthoritativelySequenced], MethodHandles.lookup())
      .findConstructor(
        classOf[AuthoritativelySequenced],
        MethodType.methodType(classOf[Unit], classOf[QualifiedStreamPosition], classOf[SourceContinuation])
      )

  private def construct(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): AuthoritativelySequenced =
    constructor.invoke(position, continuation).asInstanceOf[AuthoritativelySequenced]

  val unsequenced: SourceOrdering = ExplicitlyUnsequenced

  def sequenced(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): Either[ExecutionConstructionErrors, AuthoritativelySequenced] =
    val missing = Vector(
      Option.when(position == null)(MissingExecutionValue(ExecutionConstructionLocation.SourceSequence)),
      Option.when(continuation == null)(MissingExecutionValue(ExecutionConstructionLocation.Continuation))
    ).flatten
    val scope =
      Option.when(
        position != null && continuation != null && position.stream != continuation.stream
      )(
        StreamScopeMismatch(
          ExecutionConstructionLocation.Continuation,
          position.stream,
          continuation.stream
        )
      )
    ExecutionConstructionErrors.from(missing ++ scope).toLeft(construct(position, continuation))

  private[execution] def requireBuiltin(value: SourceOrdering): Unit =
    val runtimeClass = value.getClass
    if
      runtimeClass.getName != "trading.execution.ExplicitlyUnsequenced$" &&
      runtimeClass != classOf[AuthoritativelySequenced]
    then throw new IllegalAccessError(s"unsupported SourceOrdering implementation: ${runtimeClass.getName}")
end SourceOrdering

@nowarn("msg=Ignoring.*qualifier")
final class SourceCheckpoint private[this] (
  val position: QualifiedStreamPosition,
  val continuation: SourceContinuation)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: SourceCheckpoint => position == that.position && continuation == that.continuation
    case _                      => false

  override def hashCode(): Int  = (position, continuation).hashCode
  override def toString: String = s"SourceCheckpoint($position,$continuation)"

object SourceCheckpoint:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceCheckpoint], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceCheckpoint],
        MethodType.methodType(classOf[Unit], classOf[QualifiedStreamPosition], classOf[SourceContinuation])
      )

  private def construct(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): SourceCheckpoint =
    constructor.invoke(position, continuation).asInstanceOf[SourceCheckpoint]

  def create(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): Either[ExecutionConstructionErrors, SourceCheckpoint] =
    val missing = Vector(
      Option.when(position == null)(MissingExecutionValue(ExecutionConstructionLocation.Checkpoint)),
      Option.when(continuation == null)(MissingExecutionValue(ExecutionConstructionLocation.Continuation))
    ).flatten
    val scope =
      Option.when(
        position != null && continuation != null && position.stream != continuation.stream
      )(
        StreamScopeMismatch(
          ExecutionConstructionLocation.Checkpoint,
          position.stream,
          continuation.stream
        )
      )
    ExecutionConstructionErrors.from(missing ++ scope).toLeft(construct(position, continuation))
end SourceCheckpoint

@nowarn("msg=Ignoring.*qualifier")
final class SourceCompleteness private[this] (val completeThrough: QualifiedStreamPosition)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: SourceCompleteness => completeThrough == that.completeThrough
    case _                        => false

  override def hashCode(): Int  = completeThrough.hashCode
  override def toString: String = s"SourceCompleteness($completeThrough)"

object SourceCompleteness:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceCompleteness], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceCompleteness],
        MethodType.methodType(classOf[Unit], classOf[QualifiedStreamPosition])
      )

  private def construct(completeThrough: QualifiedStreamPosition): SourceCompleteness =
    constructor.invoke(completeThrough).asInstanceOf[SourceCompleteness]

  def create(
    completeThrough: QualifiedStreamPosition
  ): Either[ExecutionConstructionErrors, SourceCompleteness] =
    if completeThrough == null then
      Left(ExecutionConstructionErrors.one(MissingExecutionValue(ExecutionConstructionLocation.Completeness)))
    else Right(construct(completeThrough))
