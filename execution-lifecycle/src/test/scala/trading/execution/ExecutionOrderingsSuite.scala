package trading.execution

import munit.FunSuite

import trading.economics.instrument.InstrumentFixtures
import trading.order.Order
import trading.order.Side
import trading.quantity.Rational

final class ExecutionOrderingsSuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val order      = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 10)).toOption.get

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def target(source: String, account: String): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def lifecycle(
    executionTarget: ExecutionTarget,
    logicalOrder: String,
    lineage: String
  ): ExecutionLifecycle[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(
    ExecutionLifecycle.create(instrument)(
      order,
      id(ExecutionOrderId.from(logicalOrder)),
      id(OrderLineageId.from(lineage)),
      executionTarget
    )
  )

  private def event(executionTarget: ExecutionTarget, value: String): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(executionTarget, id(NativeSourceEventId.from(value))))

  private def sourceOrder(executionTarget: ExecutionTarget, value: String): QualifiedSourceOrderId =
    required(QualifiedSourceOrderId.create(executionTarget, id(NativeSourceOrderId.from(value))))

  private def fill(executionTarget: ExecutionTarget, value: String): QualifiedFillId =
    required(QualifiedFillId.create(executionTarget, id(NativeFillId.from(value))))

  private def stream(executionTarget: ExecutionTarget, value: String): QualifiedSourceStreamId =
    required(QualifiedSourceStreamId.create(executionTarget, id(SourceStreamId.from(value))))

  test("typed qualified identifiers preserve component boundaries across former delimiters"):
    val pipeLeft  = target("source|account", "desk")
    val pipeRight = target("source", "account|desk")

    assertEquals(
      s"${pipeLeft.source.value}|${pipeLeft.account.value}",
      s"${pipeRight.source.value}|${pipeRight.account.value}"
    )

    val leftEvent   = event(pipeLeft, "event|part")
    val rightEvent  = event(pipeRight, "event|part")
    val leftOrder   = sourceOrder(pipeLeft, "order|part")
    val rightOrder  = sourceOrder(pipeRight, "order|part")
    val leftFill    = fill(pipeLeft, "fill|part")
    val rightFill   = fill(pipeRight, "fill|part")
    val leftStream  = stream(pipeLeft, "stream|part")
    val rightStream = stream(pipeRight, "stream|part")

    assertNotEquals(ExecutionOrderings.qualifiedSourceEventId.compare(leftEvent, rightEvent), 0)
    assertNotEquals(ExecutionOrderings.qualifiedSourceOrderId.compare(leftOrder, rightOrder), 0)
    assertNotEquals(ExecutionOrderings.qualifiedFillId.compare(leftFill, rightFill), 0)
    assertNotEquals(ExecutionOrderings.qualifiedSourceStreamId.compare(leftStream, rightStream), 0)

    val leftPosition  = required(QualifiedStreamPosition.create(leftStream, id(SourceSequence.from(7))))
    val rightPosition = required(QualifiedStreamPosition.create(rightStream, id(SourceSequence.from(7))))
    assertNotEquals(ExecutionOrderings.qualifiedStreamPosition.compare(leftPosition, rightPosition), 0)

    val dashLeft  = target("source-account", "desk")
    val dashRight = target("source", "account-desk")
    assertEquals(
      s"${dashLeft.source.value}-${dashLeft.account.value}",
      s"${dashRight.source.value}-${dashRight.account.value}"
    )
    assertNotEquals(
      ExecutionOrderings.qualifiedFillId.compare(fill(dashLeft, "fill-part"), fill(dashRight, "fill-part")),
      0
    )

    val dashLifecycleLeft  = lifecycle(dashLeft, "logical", "lineage")
    val dashLifecycleRight = lifecycle(dashRight, "logical", "lineage")
    val leftCorrection     = required(
      FillCorrected.create(dashLifecycleLeft)(
        event(dashLeft, "event-part"),
        dashLifecycleLeft.executionOrderId,
        sourceOrder(dashLeft, "order-part"),
        fill(dashLeft, "fill-part"),
        fixtures.lots(instrument, 2),
        fixtures.price(instrument, Rational.one),
        SourceOrdering.unsequenced
      )
    )
    val rightCorrection = required(
      FillCorrected.create(dashLifecycleRight)(
        event(dashRight, "event-part"),
        dashLifecycleRight.executionOrderId,
        sourceOrder(dashRight, "order-part"),
        fill(dashRight, "fill-part"),
        fixtures.lots(instrument, 2),
        fixtures.price(instrument, Rational.one),
        SourceOrdering.unsequenced
      )
    )
    assertEquals(
      s"${leftCorrection.eventId.target.source.value}-${leftCorrection.eventId.target.account.value}-${leftCorrection.eventId.native.value}",
      s"${rightCorrection.eventId.target.source.value}-${rightCorrection.eventId.target.account.value}-${rightCorrection.eventId.native.value}"
    )
    assertNotEquals(ExecutionOrderings.fillModifier.compare(leftCorrection, rightCorrection), 0)

  test("command and dispatch orderings separate delimiter-bearing command and lineage components"):
    val executionTarget = target("source", "account")
    val leftLifecycle   = lifecycle(executionTarget, "logical|lineage", "left")
    val rightLifecycle  = lifecycle(executionTarget, "logical", "lineage|left")
    val commandId       = id(ApplicationCommandId.from("submit|command"))
    val leftSubmit      = required(SubmitOrderCommand.create(leftLifecycle)(commandId))
    val rightSubmit     = required(SubmitOrderCommand.create(rightLifecycle)(commandId))

    assertNotEquals(ExecutionOrderings.command.compare(leftSubmit, rightSubmit), 0)
    assertNotEquals(
      ExecutionOrderings.dispatchEvidence.compare(
        required(IndeterminateDispatch.forSubmit(leftSubmit)),
        required(IndeterminateDispatch.forSubmit(rightSubmit))
      ),
      0
    )

    val baseLifecycle = lifecycle(executionTarget, "logical", "lineage")
    val leftCancel    = required(
      CancelOrderCommand.create(baseLifecycle)(
        id(ApplicationCommandId.from("cancel|id")),
        id(ApplicationCommandId.from("original"))
      )
    )
    val rightCancel = required(
      CancelOrderCommand.create(baseLifecycle)(
        id(ApplicationCommandId.from("id")),
        id(ApplicationCommandId.from("original|cancel"))
      )
    )
    assertNotEquals(ExecutionOrderings.command.compare(leftCancel, rightCancel), 0)

  test("replay remains identical when formerly colliding evidence is permuted"):
    val baseTarget    = target("base-source", "base-account")
    val baseLifecycle = lifecycle(baseTarget, "logical", "lineage")

    val commandTarget         = target("source", "account")
    val commandLifecycleLeft  = lifecycle(commandTarget, "logical|lineage", "left")
    val commandLifecycleRight = lifecycle(commandTarget, "logical", "lineage|left")
    val commandId             = id(ApplicationCommandId.from("submit|command"))
    val commands              = Vector(
      required(SubmitOrderCommand.create(commandLifecycleLeft)(commandId)),
      required(SubmitOrderCommand.create(commandLifecycleRight)(commandId))
    )

    val factTargetLeft     = target("source|account", "desk")
    val factTargetRight    = target("source", "account|desk")
    val factLifecycleLeft  = lifecycle(factTargetLeft, "logical", "lineage")
    val factLifecycleRight = lifecycle(factTargetRight, "logical", "lineage")
    val facts              = Vector(
      required(
        OrderAccepted.create(factLifecycleLeft)(
          event(factTargetLeft, "event|part"),
          factLifecycleLeft.executionOrderId,
          sourceOrder(factTargetLeft, "order|part"),
          SourceOrdering.unsequenced
        )
      ),
      required(
        OrderAccepted.create(factLifecycleRight)(
          event(factTargetRight, "event|part"),
          factLifecycleRight.executionOrderId,
          sourceOrder(factTargetRight, "order|part"),
          SourceOrdering.unsequenced
        )
      )
    )

    val forward = required(ExecutionState.replay(baseLifecycle)(commands, Vector.empty, facts))
    val reverse = required(ExecutionState.replay(baseLifecycle)(commands.reverse, Vector.empty, facts.reverse))

    assertEquals(forward, reverse)
    assertEquals(forward.state.observation, reverse.state.observation)
    assertEquals(forward.rejections.size, 4)
end ExecutionOrderingsSuite
