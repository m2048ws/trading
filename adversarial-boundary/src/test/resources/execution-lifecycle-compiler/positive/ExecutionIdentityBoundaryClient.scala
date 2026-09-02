package external.execution.positive

import trading.execution.*

object ExecutionIdentityBoundaryClient:
  private def required[A](value: Either[ExecutionIdentityError, A]): A =
    value.fold(error => throw new AssertionError(error.toString), identity)

  def run(): Unit =
    val command = required(ApplicationCommandId.from("command-1"))
    val order   = required(ExecutionOrderId.from("order-1"))
    val source  = required(ExecutionSourceId.from("source-1"))
    val fill    = required(NativeFillId.from("fill-1"))
    val sequence = required(SourceSequence.from(BigInt(1)))

    assert(command.value == "command-1")
    assert(order.value == "order-1")
    assert(source.value == "source-1")
    assert(fill.value == "fill-1")
    assert(sequence.value == BigInt(1))
