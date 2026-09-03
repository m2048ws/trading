package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.economics.instrument.InstrumentFixtures
import trading.economics.instrument.Lots
import trading.order.Order
import trading.order.Side
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported
import trading.reference.GridHandle

final class ExecutionAuthoritySuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val foreign    = fixtures.foreignIdentity

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def target(source: String = "source", account: String = "account"): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def stream(
    executionTarget: ExecutionTarget = target(),
    value: String = "stream"
  ): QualifiedSourceStreamId =
    required(QualifiedSourceStreamId.create(executionTarget, id(SourceStreamId.from(value))))

  private def position(
    sourceStream: QualifiedSourceStreamId,
    value: BigInt
  ): QualifiedStreamPosition =
    required(QualifiedStreamPosition.create(sourceStream, id(SourceSequence.from(value))))

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("qualified references use source and account as nominal identity scope"):
    val targetA = target("source-a", "account-a")
    val targetB = target("source-a", "account-b")
    val eventA  = required(
      QualifiedSourceEventId.create(targetA, id(NativeSourceEventId.from("native-1")))
    )
    val replay = required(
      QualifiedSourceEventId.create(targetA, id(NativeSourceEventId.from("native-1")))
    )
    val otherAccount = required(
      QualifiedSourceEventId.create(targetB, id(NativeSourceEventId.from("native-1")))
    )
    val sourceOrder = required(
      QualifiedSourceOrderId.create(targetA, id(NativeSourceOrderId.from("native-order")))
    )
    val fill = required(QualifiedFillId.create(targetA, id(NativeFillId.from("native-fill"))))

    assertEquals(eventA, replay)
    assertEquals(eventA.hashCode, replay.hashCode)
    assertNotEquals(eventA, otherAccount)
    assertEquals(sourceOrder.target, targetA)
    assertEquals(fill.target, targetA)

  test("ordering retains explicit origin, continuation, gaps, rewinds, checkpoint, and completeness evidence"):
    val sourceStream = stream()
    val atTwo        = position(sourceStream, 2)
    val atFive       = position(sourceStream, 5)
    val atSeven      = position(sourceStream, 7)
    val origin       = required(SourceContinuation.origin(sourceStream))
    val afterTwo     = required(SourceContinuation.after(atTwo))
    val afterSeven   = required(SourceContinuation.after(atSeven))

    val first      = required(SourceOrdering.sequenced(position(sourceStream, 0), origin))
    val acrossGap  = required(SourceOrdering.sequenced(atFive, afterTwo))
    val checkpoint = required(SourceCheckpoint.create(atFive, afterSeven))
    val complete   = required(SourceCompleteness.create(atFive))

    assertEquals(SourceOrdering.unsequenced, ExplicitlyUnsequenced)
    assertEquals(first.position.sequence.value, BigInt(0))
    assertEquals(acrossGap.continuation.previous, Some(atTwo))
    assertEquals(checkpoint.continuation.previous, Some(atSeven))
    assertEquals(complete.completeThrough, atFive)

    val foreignStream = stream(target("other-source", "account"), "stream")
    val mismatch      = SourceOrdering.sequenced(atFive, required(SourceContinuation.origin(foreignStream)))
    assertEquals(
      mismatch.left.map(_.toVector),
      Left(
        Vector(
          StreamScopeMismatch(
            ExecutionConstructionLocation.Continuation,
            sourceStream,
            foreignStream
          )
        )
      )
    )

  test("lifecycle construction retains one trusted typed order, instrument grid, and target"):
    val lots            = fixtures.lots(instrument, 10)
    val order           = Order.market(instrument)(Side.Buy, lots).toOption.get
    val orderId         = id(ExecutionOrderId.from("logical-order"))
    val lineage         = id(OrderLineageId.from("lineage"))
    val executionTarget = target()
    val lifecycle       = required(
      ExecutionLifecycle.create(instrument)(order, orderId, lineage, executionTarget)
    )
    val replay = required(
      ExecutionLifecycle.create(instrument)(order, orderId, lineage, executionTarget)
    )

    def requireTyped[D <: Dim, B <: Dim, Q <: Dim](
      value: ExecutionLifecycle[D, B, Q],
      expectedGrid: GridHandle[D],
      expectedLots: Lots[D]
    ): Unit =
      assertEquals(value.positionGrid.identity, expectedGrid.identity)
      assertEquals(value.orderedLots, expectedLots)

    requireTyped(lifecycle, instrument.positionLotGrid, lots)
    assertEquals(lifecycle.instrumentId, instrument.identity.id)
    assertEquals(lifecycle.initialPositionChange.coordinate, BigInt(10))
    assertEquals(lifecycle, replay)
    assertEquals(lifecycle.hashCode, replay.hashCode)

  test("lifecycle construction accumulates independent missing identities and instrument mismatches stably"):
    val foreignOrder =
      Order.market(foreign)(Side.Sell, fixtures.lots(foreign, 3)).toOption.get
    val result = ExecutionLifecycle.create(instrument)(
      foreignOrder,
      null,
      null,
      target()
    )
    val expected = instrument.identity.id
    val supplied = foreign.identity.id
    assertEquals(
      result.left.map(_.toVector),
      Left(
        Vector(
          MissingExecutionValue(ExecutionConstructionLocation.LogicalExecutionOrder),
          MissingExecutionValue(ExecutionConstructionLocation.Lineage),
          LifecycleInstrumentMismatch(ExecutionConstructionLocation.Order, expected, supplied),
          LifecycleInstrumentMismatch(ExecutionConstructionLocation.OrderIntent, expected, supplied),
          LifecycleInstrumentMismatch(ExecutionConstructionLocation.OrderLots, expected, supplied)
        )
      )
    )

    assertEquals(
      ExecutionTarget.create(null, null).left.map(_.toVector),
      Left(
        Vector(
          MissingExecutionValue(ExecutionConstructionLocation.Source),
          MissingExecutionValue(ExecutionConstructionLocation.Account)
        )
      )
    )

  test("qualified authority, ordering, lifecycle, and construction errors reject Java serialization"):
    val executionTarget = target()
    val sourceStream    = stream(executionTarget)
    val atOne           = position(sourceStream, 1)
    val continuation    = required(SourceContinuation.origin(sourceStream))
    val order           = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 1)).toOption.get
    val lifecycle       = required(
      ExecutionLifecycle.create(instrument)(
        order,
        id(ExecutionOrderId.from("logical")),
        id(OrderLineageId.from("lineage")),
        executionTarget
      )
    )
    val errors = ExecutionTarget.create(null, null).swap.toOption.get

    val values: List[JavaSerializationUnsupported] = List(
      executionTarget,
      required(QualifiedSourceEventId.create(executionTarget, id(NativeSourceEventId.from("event")))),
      required(QualifiedSourceOrderId.create(executionTarget, id(NativeSourceOrderId.from("order")))),
      required(QualifiedFillId.create(executionTarget, id(NativeFillId.from("fill")))),
      sourceStream,
      atOne,
      continuation,
      required(SourceOrdering.sequenced(atOne, continuation)),
      SourceOrdering.unsequenced,
      required(SourceCheckpoint.create(atOne, continuation)),
      required(SourceCompleteness.create(atOne)),
      lifecycle,
      errors
    )

    values.foreach(assertSerializationRejected)

end ExecutionAuthoritySuite
