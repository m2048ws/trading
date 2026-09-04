package external.execution.negative

import trading.execution.ApplicationCommandId

object ExecutionLifecycleHasNoDownstream:
  val owned = ApplicationCommandId.from("command")

  // OFFENDING-BEGIN
  val scenario: trading.scenario.OrderScenario[?, ?, ?] = ???
  val fee: trading.fee.FeePolicy = ???
  val risk: trading.risk.RiskDecision = ???
  val application: trading.application.CapabilityProgram[?] = ???
  val runtime: trading.runtime.CapabilityRuntime[?] = ???
  val codec: io.circe.Codec[String] = ???
  val effect: cats.effect.IO[Unit] = ???
  val stream: fs2.Stream[cats.effect.IO, Unit] = ???
  val persistence: doobie.ConnectionIO[Unit] = ???
  val telemetry: io.opentelemetry.api.trace.Tracer = ???
  val venueSdk: com.binance.connector.client.SpotClient = ???
  // OFFENDING-END
