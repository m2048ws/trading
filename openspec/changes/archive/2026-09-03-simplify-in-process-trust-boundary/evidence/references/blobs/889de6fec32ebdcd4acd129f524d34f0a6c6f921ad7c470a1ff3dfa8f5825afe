package trading.execution

import trading.quantity.JavaSerializationUnsupported

sealed abstract class SourceContinuation protected () extends JavaSerializationUnsupported:
  def stream: QualifiedSourceStreamId
  def previous: Option[QualifiedStreamPosition]

object SourceContinuation:
  final class StreamOrigin private[execution] (val stream: QualifiedSourceStreamId) extends SourceContinuation():
    val previous: Option[QualifiedStreamPosition] = None

    override def equals(other: Any): Boolean = other match
      case that: StreamOrigin => stream == that.stream
      case _                  => false

    override def hashCode(): Int  = stream.hashCode
    override def toString: String = s"StreamOrigin($stream)"

  final class ContinuesAfter private[execution] (val value: QualifiedStreamPosition) extends SourceContinuation():
    val stream: QualifiedSourceStreamId           = value.stream
    val previous: Option[QualifiedStreamPosition] = Some(value)

    override def equals(other: Any): Boolean = other match
      case that: ContinuesAfter => value == that.value
      case _                    => false

    override def hashCode(): Int  = value.hashCode
    override def toString: String = s"ContinuesAfter($value)"

  private def constructOrigin(stream: QualifiedSourceStreamId): StreamOrigin =
    new StreamOrigin(stream)

  private def constructContinuation(previous: QualifiedStreamPosition): ContinuesAfter =
    new ContinuesAfter(previous)

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

end SourceContinuation

sealed abstract class SourceOrdering protected () extends JavaSerializationUnsupported

case object ExplicitlyUnsequenced extends SourceOrdering()

final class AuthoritativelySequenced private[execution] (
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
  private def construct(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): AuthoritativelySequenced =
    new AuthoritativelySequenced(position, continuation)

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

end SourceOrdering

final class SourceCheckpoint private (
  val position: QualifiedStreamPosition,
  val continuation: SourceContinuation)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: SourceCheckpoint => position == that.position && continuation == that.continuation
    case _                      => false

  override def hashCode(): Int  = (position, continuation).hashCode
  override def toString: String = s"SourceCheckpoint($position,$continuation)"

object SourceCheckpoint:
  private def construct(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation
  ): SourceCheckpoint =
    new SourceCheckpoint(position, continuation)

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

final class SourceCompleteness private (val completeThrough: QualifiedStreamPosition)
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: SourceCompleteness => completeThrough == that.completeThrough
    case _                        => false

  override def hashCode(): Int  = completeThrough.hashCode
  override def toString: String = s"SourceCompleteness($completeThrough)"

object SourceCompleteness:
  private def construct(completeThrough: QualifiedStreamPosition): SourceCompleteness =
    new SourceCompleteness(completeThrough)

  def create(
    completeThrough: QualifiedStreamPosition
  ): Either[ExecutionConstructionErrors, SourceCompleteness] =
    if completeThrough == null then
      Left(ExecutionConstructionErrors.one(MissingExecutionValue(ExecutionConstructionLocation.Completeness)))
    else Right(construct(completeThrough))
