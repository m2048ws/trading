package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

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

@nowarn("msg=Ignoring.*qualifier")
final class ApplicationCommandId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ApplicationCommandId => value == that.value
    case _                          => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ApplicationCommandId($value)"

object ApplicationCommandId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ApplicationCommandId], MethodHandles.lookup())
      .findConstructor(classOf[ApplicationCommandId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): ApplicationCommandId =
    constructor.invoke(value).asInstanceOf[ApplicationCommandId]

  def from(value: String): Either[ExecutionIdentityError, ApplicationCommandId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ApplicationCommand, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionOrderId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionOrderId => value == that.value
    case _                      => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionOrderId($value)"

object ExecutionOrderId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionOrderId], MethodHandles.lookup())
      .findConstructor(classOf[ExecutionOrderId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): ExecutionOrderId =
    constructor.invoke(value).asInstanceOf[ExecutionOrderId]

  def from(value: String): Either[ExecutionIdentityError, ExecutionOrderId] =
    IdentityRepresentation.text(ExecutionIdentityKind.LogicalExecutionOrder, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class OrderLineageId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: OrderLineageId => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"OrderLineageId($value)"

object OrderLineageId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[OrderLineageId], MethodHandles.lookup())
      .findConstructor(classOf[OrderLineageId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): OrderLineageId =
    constructor.invoke(value).asInstanceOf[OrderLineageId]

  def from(value: String): Either[ExecutionIdentityError, OrderLineageId] =
    IdentityRepresentation.text(ExecutionIdentityKind.Lineage, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionSourceId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionSourceId => value == that.value
    case _                       => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionSourceId($value)"

object ExecutionSourceId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionSourceId], MethodHandles.lookup())
      .findConstructor(classOf[ExecutionSourceId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): ExecutionSourceId =
    constructor.invoke(value).asInstanceOf[ExecutionSourceId]

  def from(value: String): Either[ExecutionIdentityError, ExecutionSourceId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ExecutionSource, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionAccountId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: ExecutionAccountId => value == that.value
    case _                        => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"ExecutionAccountId($value)"

object ExecutionAccountId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionAccountId], MethodHandles.lookup())
      .findConstructor(classOf[ExecutionAccountId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): ExecutionAccountId =
    constructor.invoke(value).asInstanceOf[ExecutionAccountId]

  def from(value: String): Either[ExecutionIdentityError, ExecutionAccountId] =
    IdentityRepresentation.text(ExecutionIdentityKind.ExecutionAccount, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class NativeSourceEventId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeSourceEventId => value == that.value
    case _                         => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeSourceEventId($value)"

object NativeSourceEventId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[NativeSourceEventId], MethodHandles.lookup())
      .findConstructor(classOf[NativeSourceEventId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): NativeSourceEventId =
    constructor.invoke(value).asInstanceOf[NativeSourceEventId]

  def from(value: String): Either[ExecutionIdentityError, NativeSourceEventId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeSourceEvent, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class NativeSourceOrderId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeSourceOrderId => value == that.value
    case _                         => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeSourceOrderId($value)"

object NativeSourceOrderId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[NativeSourceOrderId], MethodHandles.lookup())
      .findConstructor(classOf[NativeSourceOrderId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): NativeSourceOrderId =
    constructor.invoke(value).asInstanceOf[NativeSourceOrderId]

  def from(value: String): Either[ExecutionIdentityError, NativeSourceOrderId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeSourceOrder, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class NativeFillId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: NativeFillId => value == that.value
    case _                  => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"NativeFillId($value)"

object NativeFillId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[NativeFillId], MethodHandles.lookup())
      .findConstructor(classOf[NativeFillId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): NativeFillId =
    constructor.invoke(value).asInstanceOf[NativeFillId]

  def from(value: String): Either[ExecutionIdentityError, NativeFillId] =
    IdentityRepresentation.text(ExecutionIdentityKind.NativeFill, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class SourceStreamId private[this] (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: SourceStreamId => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"SourceStreamId($value)"

object SourceStreamId:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceStreamId], MethodHandles.lookup())
      .findConstructor(classOf[SourceStreamId], MethodType.methodType(classOf[Unit], classOf[String]))

  private def construct(value: String): SourceStreamId =
    constructor.invoke(value).asInstanceOf[SourceStreamId]

  def from(value: String): Either[ExecutionIdentityError, SourceStreamId] =
    IdentityRepresentation.text(ExecutionIdentityKind.SourceStream, value).map(construct)

@nowarn("msg=Ignoring.*qualifier")
final class SourceSequence private[this] (val value: BigInt) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean = other match
    case that: SourceSequence => value == that.value
    case _                    => false
  override def hashCode(): Int  = 31 * getClass.hashCode + value.hashCode
  override def toString: String = s"SourceSequence($value)"

object SourceSequence:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceSequence], MethodHandles.lookup())
      .findConstructor(classOf[SourceSequence], MethodType.methodType(classOf[Unit], classOf[BigInt]))

  private def construct(value: BigInt): SourceSequence =
    constructor.invoke(value).asInstanceOf[SourceSequence]

  def from(value: BigInt): Either[ExecutionIdentityError, SourceSequence] =
    if value == null then Left(MissingExecutionIdentity(ExecutionIdentityKind.SourceSequence))
    else if value.signum < 0 then Left(NegativeSourceSequence(value))
    else Right(construct(value))
