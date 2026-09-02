package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.FunSuite

import trading.quantity.JavaSerializationUnsupported

class ExecutionIdentitySuite extends FunSuite:
  private def required[A](value: Either[ExecutionIdentityError, A]): A =
    value.fold(error => fail(s"expected checked identity, received $error"), identity)

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("application and source identities are checked nominal values"):
    val command        = required(ApplicationCommandId.from("command-1"))
    val execution      = required(ExecutionOrderId.from("execution-1"))
    val lineage        = required(OrderLineageId.from("lineage-1"))
    val source         = required(ExecutionSourceId.from("source-1"))
    val account        = required(ExecutionAccountId.from("account-1"))
    val event          = required(NativeSourceEventId.from("event-1"))
    val sourceOrder    = required(NativeSourceOrderId.from("order-1"))
    val fill           = required(NativeFillId.from("fill-1"))
    val stream         = required(SourceStreamId.from("stream-1"))
    val sourceSequence = required(SourceSequence.from(BigInt(0)))

    assertEquals(command, required(ApplicationCommandId.from("command-1")))
    assertEquals(command.hashCode, required(ApplicationCommandId.from("command-1")).hashCode)
    assertNotEquals(command.asInstanceOf[Any], execution.asInstanceOf[Any])
    assertEquals(execution.value, "execution-1")
    assertEquals(lineage.value, "lineage-1")
    assertEquals(source.value, "source-1")
    assertEquals(account.value, "account-1")
    assertEquals(event.value, "event-1")
    assertEquals(sourceOrder.value, "order-1")
    assertEquals(fill.value, "fill-1")
    assertEquals(stream.value, "stream-1")
    assertEquals(sourceSequence.value, BigInt(0))

  test("checked constructors reject missing, blank, and negative representations"):
    assertEquals(
      ApplicationCommandId.from(null),
      Left(MissingExecutionIdentity(ExecutionIdentityKind.ApplicationCommand))
    )
    assertEquals(
      NativeFillId.from("  "),
      Left(BlankExecutionIdentity(ExecutionIdentityKind.NativeFill))
    )
    assertEquals(
      SourceSequence.from(null),
      Left(MissingExecutionIdentity(ExecutionIdentityKind.SourceSequence))
    )
    assertEquals(SourceSequence.from(BigInt(-1)), Left(NegativeSourceSequence(BigInt(-1))))

  test("identity representations are final with JVM-private constructors"):
    val representations = List(
      classOf[ApplicationCommandId],
      classOf[ExecutionOrderId],
      classOf[OrderLineageId],
      classOf[ExecutionSourceId],
      classOf[ExecutionAccountId],
      classOf[NativeSourceEventId],
      classOf[NativeSourceOrderId],
      classOf[NativeFillId],
      classOf[SourceStreamId],
      classOf[SourceSequence]
    )

    representations.foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} must be final")
      assert(representation.getDeclaredConstructors.nonEmpty)
      assert(
        representation.getDeclaredConstructors.forall(constructor => Modifier.isPrivate(constructor.getModifiers)),
        s"${representation.getName} exposes a non-private JVM constructor"
      )

  test("identity companions expose validation but no generation, clock, hash, or receipt authority"):
    val companions = List(
      ApplicationCommandId,
      ExecutionOrderId,
      OrderLineageId,
      ExecutionSourceId,
      ExecutionAccountId,
      NativeSourceEventId,
      NativeSourceOrderId,
      NativeFillId,
      SourceStreamId,
      SourceSequence
    )
    val forbidden = List("generate", "random", "uuid", "now", "timestamp", "receipt", "digest")

    companions.foreach: companion =>
      val names = companion.getClass.getMethods.map(_.getName.toLowerCase).toList
      forbidden.foreach(fragment => assert(!names.exists(_.contains(fragment)), clues(companion, fragment, names)))

  test("all initial identities and their checked errors reject Java object serialization"):
    val values: List[JavaSerializationUnsupported] = List(
      required(ApplicationCommandId.from("command")),
      required(ExecutionOrderId.from("execution")),
      required(OrderLineageId.from("lineage")),
      required(ExecutionSourceId.from("source")),
      required(ExecutionAccountId.from("account")),
      required(NativeSourceEventId.from("event")),
      required(NativeSourceOrderId.from("order")),
      required(NativeFillId.from("fill")),
      required(SourceStreamId.from("stream")),
      required(SourceSequence.from(BigInt(7))),
      MissingExecutionIdentity(ExecutionIdentityKind.NativeFill),
      BlankExecutionIdentity(ExecutionIdentityKind.NativeFill),
      NegativeSourceSequence(BigInt(-1))
    )

    values.foreach(assertSerializationRejected)

end ExecutionIdentitySuite
