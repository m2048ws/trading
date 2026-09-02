package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported

final class CommandStateSuite extends ScalaCheckSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear
  private val target     = required(
    ExecutionTarget.create(
      id(ExecutionSourceId.from("source")),
      id(ExecutionAccountId.from("account"))
    )
  )
  private val order     = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 10)).toOption.get
  private val lifecycle = required(
    ExecutionLifecycle.create(instrument)(
      order,
      id(ExecutionOrderId.from("logical-order")),
      id(OrderLineageId.from("lineage")),
      target
    )
  )

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def commandId(value: String): ApplicationCommandId = id(ApplicationCommandId.from(value))

  private def submit(value: String): SubmitOrderCommand[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(SubmitOrderCommand.create(lifecycle)(commandId(value)))

  private def initial: CommandState[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D
  ] = required(CommandState.initial(lifecycle))

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("submit and cancel commands retain stable application, logical-order, target, order, and lineage identity"):
    val original = submit("submit")
    val cancel   = required(CancelOrderCommand.create(lifecycle)(commandId("cancel"), original.commandId))

    assertEquals(original.commandId, commandId("submit"))
    assertEquals(original.executionOrderId, lifecycle.executionOrderId)
    assertEquals(original.target, lifecycle.target)
    assertEquals(original.lineageId, lifecycle.lineageId)
    assertEquals(original.lifecycle.order, order)
    assertEquals(cancel.originalSubmitCommandId, original.commandId)
    assertEquals(cancel.executionOrderId, original.executionOrderId)
    assertEquals(cancel.lifecycle.order, original.lifecycle.order)

    val commandKinds = List(original, cancel).map:
      case _: SubmitOrderCommand[?, ?, ?] => "submit"
      case _: CancelOrderCommand[?, ?, ?] => "cancel"
    assertEquals(commandKinds, List("submit", "cancel"))

  test("command identity classifies same-body retry and retains a different-body conflict without replacement"):
    val original    = submit("same-id")
    val first       = initial.record(original)
    val retry       = first.state.record(submit("same-id"))
    val cancelReuse = required(CancelOrderCommand.create(lifecycle)(original.commandId, original.commandId))
    val conflict    = retry.state.record(cancelReuse)
    val repeated    = conflict.state.record(cancelReuse)

    assertEquals(first.kind, CommandTransitionKind.Applied)
    assertEquals(retry.kind, CommandTransitionKind.IdempotentDuplicate)
    assertEquals(retry.state, first.state)
    assertEquals(conflict.kind, CommandTransitionKind.ConflictingCommand)
    assertEquals(conflict.state.issuedCommands, Map(original.commandId -> original))
    assertEquals(conflict.state.conflicts.size, 1)
    assertEquals(conflict.state.conflicts.head.original, original)
    assertEquals(conflict.state.conflicts.head.conflicting, cancelReuse)
    assertEquals(repeated.state.conflicts, conflict.state.conflicts)
    assertEquals(conflict.state.cancellationRequests, Vector.empty)

  test("new commands for another logical order are rejected with deterministic scope evidence"):
    val foreignLifecycle = required(
      ExecutionLifecycle.create(instrument)(
        order,
        id(ExecutionOrderId.from("another-logical-order")),
        lifecycle.lineageId,
        lifecycle.target
      )
    )
    val foreign = required(SubmitOrderCommand.create(foreignLifecycle)(commandId("foreign")))
    val result  = initial.record(foreign)

    assertEquals(result.kind, CommandTransitionKind.Rejected)
    assertEquals(
      result.violations.map(_.toVector),
      Some(
        Vector(
          CommandLogicalOrderMismatch(lifecycle.executionOrderId, foreignLifecycle.executionOrderId)
        )
      )
    )
    assertEquals(result.state, initial)

    val otherTarget = required(
      ExecutionTarget.create(
        id(ExecutionSourceId.from("other-source")),
        id(ExecutionAccountId.from("other-account"))
      )
    )
    val foreignScope = required(
      ExecutionLifecycle.create(instrument)(
        order,
        lifecycle.executionOrderId,
        id(OrderLineageId.from("other-lineage")),
        otherTarget
      )
    )
    val scoped = required(SubmitOrderCommand.create(foreignScope)(commandId("foreign-scope")))
    assertEquals(
      initial.record(scoped).violations.map(_.toVector),
      Some(
        Vector(
          CommandLineageMismatch(lifecycle.lineageId, foreignScope.lineageId),
          CommandTargetMismatch(lifecycle.target, foreignScope.target)
        )
      )
    )

    assertEquals(
      CancelOrderCommand.create[trading.quantity.Dim, trading.quantity.Dim, trading.quantity.Dim](null)(
        null,
        null
      ).left.map(_.toVector),
      Left(
        Vector(
          MissingCommandValue(CommandViolationLocation.Lifecycle),
          MissingCommandValue(CommandViolationLocation.CommandIdentity),
          MissingCommandValue(CommandViolationLocation.OriginalSubmit)
        )
      )
    )

  test("cancellation requests remain separate evidence and require an original submit command"):
    val original = submit("submit")
    val unknown  = required(CancelOrderCommand.create(lifecycle)(commandId("cancel-unknown"), commandId("missing")))
    val rejected = initial.record(unknown)

    assertEquals(rejected.kind, CommandTransitionKind.Rejected)
    assertEquals(
      rejected.violations.map(_.toVector),
      Some(Vector(UnknownOriginalSubmit(commandId("missing"))))
    )

    val submitted = initial.record(original).state
    val cancel    = required(CancelOrderCommand.create(lifecycle)(commandId("cancel"), original.commandId))
    val cancelled = submitted.record(cancel)
    val chained   = required(CancelOrderCommand.create(lifecycle)(commandId("cancel-2"), cancel.commandId))
    val invalid   = cancelled.state.record(chained)

    assertEquals(cancelled.kind, CommandTransitionKind.Applied)
    assertEquals(cancelled.state.cancellationRequests, Vector(cancel))
    assertEquals(cancelled.state.dispatchKnowledge, Map.empty)
    assertEquals(
      invalid.violations.map(_.toVector),
      Some(Vector(ReferencedCommandIsNotSubmit(cancel.commandId)))
    )

  test("dispatch observations retain proven non-dispatch and indeterminacy independently on the original submit"):
    val original      = submit("submit")
    val submitted     = initial.record(original).state
    val notDispatched = required(ProvenNotDispatched.forSubmit(original))
    val indeterminate = required(IndeterminateDispatch.forSubmit(original))
    val first         = submitted.observeDispatch(notDispatched)
    val retry         = first.state.observeDispatch(notDispatched)
    val conflict      = retry.state.observeDispatch(indeterminate)

    assertEquals(first.kind, CommandTransitionKind.Applied)
    assertEquals(retry.kind, CommandTransitionKind.IdempotentDuplicate)
    assertEquals(conflict.kind, CommandTransitionKind.ConflictingDispatchEvidence)
    assertEquals(
      conflict.state.dispatchKnowledge,
      Map(original.commandId -> Vector(notDispatched, indeterminate))
    )
    assertEquals(conflict.state.issuedCommands, Map(original.commandId -> original))
    assertEquals(conflict.state.conflicts, Vector.empty)
    assertEquals(conflict.state.cancellationRequests, Vector.empty)

    val recovery = conflict.state.record(submit("submit"))
    assertEquals(recovery.kind, CommandTransitionKind.IdempotentDuplicate)
    assertEquals(recovery.state, conflict.state)

    val evidenceKinds = conflict.state.dispatchKnowledge(original.commandId).map:
      case _: ProvenNotDispatched[?, ?, ?]   => "proven-not-dispatched"
      case _: IndeterminateDispatch[?, ?, ?] => "indeterminate"
    assertEquals(evidenceKinds, Vector("proven-not-dispatched", "indeterminate"))

  test("dispatch evidence rejects unknown and incompatible original submit references"):
    val original = submit("submit")
    val unknown  = initial.observeDispatch(required(IndeterminateDispatch.forSubmit(original)))
    assertEquals(unknown.kind, CommandTransitionKind.Rejected)
    assertEquals(
      unknown.violations.map(_.toVector),
      Some(Vector(UnknownOriginalSubmit(original.commandId)))
    )

    val otherOrder       = Order.market(instrument)(Side.Buy, fixtures.lots(instrument, 11)).toOption.get
    val alteredLifecycle = required(
      ExecutionLifecycle.create(instrument)(
        otherOrder,
        lifecycle.executionOrderId,
        lifecycle.lineageId,
        lifecycle.target
      )
    )
    val incompatible = required(SubmitOrderCommand.create(alteredLifecycle)(original.commandId))
    val observed     = initial
      .record(original)
      .state
      .observeDispatch(required(ProvenNotDispatched.forSubmit(incompatible)))

    assertEquals(observed.kind, CommandTransitionKind.Rejected)
    assertEquals(
      observed.violations.map(_.toVector),
      Some(
        Vector(
          CommandImmutableOrderMismatch(original.commandId),
          DispatchSubmitBodyMismatch(original.commandId)
        )
      )
    )

  property("arbitrarily many redeliveries of one command remain one business command"):
    forAll { (rawAttempts: Int) =>
      val attempts = (BigInt(rawAttempts).abs % 200).toInt + 1
      val original = submit("stable-across-attempts")
      val result   = 0.until(attempts).foldLeft(initial): (state, _) =>
        state.record(original).state

      result.issuedCommands == Map(original.commandId -> original) &&
      result.conflicts.isEmpty && result.cancellationRequests.isEmpty
    }

  test("command representations are final, JVM-private, non-serializable, and expose no native amendment API"):
    val original      = submit("submit")
    val transition    = initial.record(original)
    val notDispatched = required(ProvenNotDispatched.forSubmit(original))
    val observed      = transition.state.observeDispatch(notDispatched)
    val cancelReuse   = required(CancelOrderCommand.create(lifecycle)(original.commandId, original.commandId))
    val conflicted    = observed.state.record(cancelReuse)
    val errors        = SubmitOrderCommand.create(lifecycle)(null).swap.toOption.get

    val representations = List(
      classOf[CommandViolations],
      classOf[SubmitOrderCommand[?, ?, ?]],
      classOf[CancelOrderCommand[?, ?, ?]],
      classOf[ProvenNotDispatched[?, ?, ?]],
      classOf[IndeterminateDispatch[?, ?, ?]],
      classOf[CommandConflict[?, ?, ?]],
      classOf[CommandTransition[?, ?, ?]],
      classOf[CommandState[?, ?, ?]]
    )
    representations.foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} must be final")
      assert(
        representation.getDeclaredConstructors.forall(constructor => Modifier.isPrivate(constructor.getModifiers)),
        s"${representation.getName} exposes a non-private JVM constructor"
      )

    val publicMethodNames =
      (representations :+ classOf[ExecutionCommand[?, ?, ?]] :+ classOf[DispatchEvidence[?, ?, ?]])
        .flatMap(_.getMethods.map(_.getName.toLowerCase))
    List("amend", "cancelreplace", "atomicreplace", "transportattempt", "receiptid").foreach: forbidden =>
      assert(!publicMethodNames.exists(_.contains(forbidden)), clues(forbidden, publicMethodNames))

    val serializable: List[JavaSerializationUnsupported] = List(
      original,
      cancelReuse,
      notDispatched,
      required(IndeterminateDispatch.forSubmit(original)),
      transition.state,
      transition,
      conflicted.state.conflicts.head,
      errors,
      CommandTransitionKind.Applied,
      MissingCommandValue(CommandViolationLocation.Command)
    )
    serializable.foreach(assertSerializationRejected)

end CommandStateSuite
