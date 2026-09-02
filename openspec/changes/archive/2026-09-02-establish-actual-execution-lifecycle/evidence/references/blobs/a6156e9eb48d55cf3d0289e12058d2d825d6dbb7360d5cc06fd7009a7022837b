package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.economics.instrument.Instrument
import trading.economics.instrument.InstrumentId
import trading.economics.instrument.Lots
import trading.economics.instrument.PositionLots
import trading.order.ImmediateActivation
import trading.order.Order
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported
import trading.reference.GridHandle

private object LifecycleValueSemantics:
  def sameInstrument(left: Instrument, right: Instrument): Boolean =
    left.identity == right.identity &&
      left.roles.base.id == right.roles.base.id &&
      left.roles.quote.id == right.roles.quote.id &&
      left.roles.position.id == right.roles.position.id &&
      left.roles.settle.id == right.roles.settle.id &&
      left.positionLotGrid.identity == right.positionLotGrid.identity &&
      left.positionLotGrid.quantum == right.positionLotGrid.quantum &&
      left.priceGrid.identity == right.priceGrid.identity &&
      left.priceGrid.quantum == right.priceGrid.quantum &&
      left.basePerPosition.coefficient == right.basePerPosition.coefficient &&
      left.quotePerPosition.coefficient == right.quotePerPosition.coefficient

  def instrumentHash(value: Instrument): Int =
    (
      value.identity,
      value.roles.base.id,
      value.roles.quote.id,
      value.roles.position.id,
      value.roles.settle.id,
      value.positionLotGrid.identity,
      value.positionLotGrid.quantum,
      value.priceGrid.identity,
      value.priceGrid.quantum,
      value.basePerPosition.coefficient,
      value.quotePerPosition.coefficient
    ).hashCode

  def sameOrder(left: Order[?, ?, ?], right: Order[?, ?, ?]): Boolean =
    left.instrumentId == right.instrumentId &&
      left.intent == right.intent &&
      sameActivation(left.activation, right.activation) &&
      left.execution == right.execution

  def orderHash(value: Order[?, ?, ?]): Int =
    (value.instrumentId, value.intent, activationHash(value.activation), value.execution).hashCode

  private def sameActivation(left: Any, right: Any): Boolean = (left, right) match
    case (_: ImmediateActivation[?, ?], _: ImmediateActivation[?, ?]) => true
    case _                                                            => left == right

  private def activationHash(value: Any): Int = value match
    case _: ImmediateActivation[?, ?] => classOf[ImmediateActivation[?, ?]].hashCode
    case other                        => other.hashCode
end LifecycleValueSemantics

/** Trusted configuration for the actual-execution evidence of one immutable order. */
@nowarn("msg=Ignoring.*qualifier")
final class ExecutionLifecycle[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val instrument: Instrument,
  val order: Order[D, B, Q],
  val executionOrderId: ExecutionOrderId,
  val lineageId: OrderLineageId,
  val target: ExecutionTarget,
  val positionGrid: GridHandle[D])
  extends JavaSerializationUnsupported:

  val instrumentId: InstrumentId             = instrument.identity.id
  val orderedLots: Lots[D]                   = order.intent.lots
  val initialPositionChange: PositionLots[D] = order.intent.positionChange

  override def equals(other: Any): Boolean = other match
    case that: ExecutionLifecycle[?, ?, ?] =>
      LifecycleValueSemantics.sameInstrument(instrument, that.instrument) &&
      LifecycleValueSemantics.sameOrder(order, that.order) &&
      executionOrderId == that.executionOrderId && lineageId == that.lineageId &&
      target == that.target && positionGrid.identity == that.positionGrid.identity &&
      positionGrid.quantum == that.positionGrid.quantum
    case _ => false

  override def hashCode(): Int =
    (
      LifecycleValueSemantics.instrumentHash(instrument),
      LifecycleValueSemantics.orderHash(order),
      executionOrderId,
      lineageId,
      target,
      positionGrid.identity,
      positionGrid.quantum
    ).hashCode

  override def toString: String =
    s"ExecutionLifecycle($instrumentId,$executionOrderId,$lineageId,$target)"
end ExecutionLifecycle

object ExecutionLifecycle:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionLifecycle[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ExecutionLifecycle[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[Instrument],
          classOf[Order[?, ?, ?]],
          classOf[ExecutionOrderId],
          classOf[OrderLineageId],
          classOf[ExecutionTarget],
          classOf[GridHandle[?]]
        )
      )

  private def construct[
    I <: Instrument,
    D <: Dim,
    B <: Dim,
    Q <: Dim
  ](
    instrument: I,
    order: Order[D, B, Q],
    executionOrderId: ExecutionOrderId,
    lineageId: OrderLineageId,
    target: ExecutionTarget
  ): ExecutionLifecycle[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] =
    constructor
      .invoke(instrument, order, executionOrderId, lineageId, target, instrument.positionLotGrid)
      .asInstanceOf[
        ExecutionLifecycle[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D
        ]
      ]

  def create[
    I <: Instrument,
    D <: Dim,
    B <: Dim,
    Q <: Dim
  ](
    instrument: I
  )(
    order: Order[D, B, Q],
    executionOrderId: ExecutionOrderId,
    lineageId: OrderLineageId,
    target: ExecutionTarget
  ): Either[
    ExecutionConstructionErrors,
    ExecutionLifecycle[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D
    ]
  ] =
    val violations = Vector.newBuilder[ExecutionConstructionViolation]
    if instrument == null then
      violations += MissingExecutionValue(ExecutionConstructionLocation.Instrument)
    if order == null then violations += MissingExecutionValue(ExecutionConstructionLocation.Order)
    if executionOrderId == null then
      violations += MissingExecutionValue(ExecutionConstructionLocation.LogicalExecutionOrder)
    if lineageId == null then violations += MissingExecutionValue(ExecutionConstructionLocation.Lineage)
    if target == null then violations += MissingExecutionValue(ExecutionConstructionLocation.Target)

    if instrument != null && order != null then
      val expected                                                                     = instrument.identity.id
      def check(location: ExecutionConstructionLocation, supplied: InstrumentId): Unit =
        if supplied != expected then violations += LifecycleInstrumentMismatch(location, expected, supplied)

      check(ExecutionConstructionLocation.Order, order.instrumentId)
      check(ExecutionConstructionLocation.OrderIntent, order.intent.instrumentId)
      check(ExecutionConstructionLocation.OrderLots, order.intent.lots.instrumentId)

    ExecutionConstructionErrors
      .from(violations.result())
      .toLeft(construct(instrument, order, executionOrderId, lineageId, target))
  end create
end ExecutionLifecycle
