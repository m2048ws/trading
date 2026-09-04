package trading.execution

import trading.quantity.JavaSerializationUnsupported

enum ExecutionIdentityKind extends JavaSerializationUnsupported:
  case ApplicationCommand
  case LogicalExecutionOrder
  case Lineage
  case ExecutionSource
  case ExecutionAccount
  case NativeSourceEvent
  case NativeSourceOrder
  case NativeFill
  case SourceStream
  case SourceSequence

sealed abstract class ExecutionIdentityError extends JavaSerializationUnsupported with Product with Serializable

final case class MissingExecutionIdentity(kind: ExecutionIdentityKind) extends ExecutionIdentityError

final case class BlankExecutionIdentity(kind: ExecutionIdentityKind) extends ExecutionIdentityError

final case class NegativeSourceSequence(value: BigInt) extends ExecutionIdentityError

private object IdentityRepresentation:
  def text(kind: ExecutionIdentityKind, value: String): Either[ExecutionIdentityError, String] =
    if value == null then Left(MissingExecutionIdentity(kind))
    else if value.trim.isEmpty then Left(BlankExecutionIdentity(kind))
    else Right(value)

final class ApplicationCommandId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ApplicationCommandId => value == that.value
    case _                          => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ApplicationCommandId($value)"

object ApplicationCommandId:
  private def construct(value: String): ApplicationCommandId =
    new ApplicationCommandId(value)

  def from(value: String): Either[ExecutionIdentityError, ApplicationCommandId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ApplicationCommand, value).map(construct)

final class ExecutionOrderId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionOrderId => value == that.value
    case _                      => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionOrderId($value)"

object ExecutionOrderId:
  private def construct(value: String): ExecutionOrderId =
    new ExecutionOrderId(value)

  def from(value: String): Either[ExecutionIdentityError, ExecutionOrderId] =
    IdentityRepresentation.text(ExecutionIdentityKind.LogicalExecutionOrder, value).map(construct)

final class OrderLineageId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: OrderLineageId => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"OrderLineageId($value)"

object OrderLineageId:
  private def construct(value: String): OrderLineageId =
    new OrderLineageId(value)

  def from(value: String): Either[ExecutionIdentityError, OrderLineageId] =
    IdentityRepresentation.text(ExecutionIdentityKind.Lineage, value).map(construct)

final class ExecutionSourceId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionSourceId => value == that.value
    case _                       => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionSourceId($value)"

object ExecutionSourceId:
  private def construct(value: String): ExecutionSourceId =
    new ExecutionSourceId(value)

  def from(value: String): Either[ExecutionIdentityError, ExecutionSourceId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ExecutionSource, value).map(construct)

final class ExecutionAccountId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionAccountId => value == that.value
    case _                        => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionAccountId($value)"

object ExecutionAccountId:
  private def construct(value: String): ExecutionAccountId =
    new ExecutionAccountId(value)

  def from(value: String): Either[ExecutionIdentityError, ExecutionAccountId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ExecutionAccount, value).map(construct)

final class NativeSourceEventId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeSourceEventId => value == that.value
    case _                         => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeSourceEventId($value)"

object NativeSourceEventId:
  private def construct(value: String): NativeSourceEventId =
    new NativeSourceEventId(value)

  def from(value: String): Either[ExecutionIdentityError, NativeSourceEventId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeSourceEvent, value).map(construct)

final class NativeSourceOrderId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeSourceOrderId => value == that.value
    case _                         => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeSourceOrderId($value)"

object NativeSourceOrderId:
  private def construct(value: String): NativeSourceOrderId =
    new NativeSourceOrderId(value)

  def from(value: String): Either[ExecutionIdentityError, NativeSourceOrderId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeSourceOrder, value).map(construct)

final class NativeFillId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeFillId => value == that.value
    case _                  => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeFillId($value)"

object NativeFillId:
  private def construct(value: String): NativeFillId =
    new NativeFillId(value)

  def from(value: String): Either[ExecutionIdentityError, NativeFillId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeFill, value).map(construct)

final class SourceStreamId private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: SourceStreamId => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"SourceStreamId($value)"

object SourceStreamId:
  private def construct(value: String): SourceStreamId =
    new SourceStreamId(value)

  def from(value: String): Either[ExecutionIdentityError, SourceStreamId] =
    IdentityRepresentation.text(ExecutionIdentityKind.SourceStream, value).map(construct)

final class SourceSequence private (val value: BigInt) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: SourceSequence => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"SourceSequence($value)"

object SourceSequence:
  private def construct(value: BigInt): SourceSequence =
    new SourceSequence(value)

  def from(value: BigInt): Either[ExecutionIdentityError, SourceSequence] =
    if value == null then Left(MissingExecutionIdentity(ExecutionIdentityKind.SourceSequence))
    else if value.signum < 0 then Left(NegativeSourceSequence(value))
    else Right(construct(value))
