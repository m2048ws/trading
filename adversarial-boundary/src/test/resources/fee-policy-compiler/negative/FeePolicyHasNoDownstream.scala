package external.fee.negative

import trading.fee.FeePolicy

object FeePolicyHasNoDownstream:
  val ownedType: Class[FeePolicy[?, ?, ?, ?, ?]] = classOf[FeePolicy[?, ?, ?, ?, ?]]

  // OFFENDING-BEGIN
  object MissingRisk:
    import trading.risk.*

  object MissingApplication:
    import trading.application.*

  object MissingRuntime:
    import trading.runtime.*

  object MissingBoundaryCodecs:
    import trading.codec.*

  object MissingEffects:
    import cats.effect.*

  object MissingStreams:
    import fs2.*

  object MissingCodecs:
    import io.circe.*

  object MissingPersistence:
    import doobie.*

  object MissingTelemetry:
    import io.opentelemetry.api.*

  object MissingBenchmarks:
    import org.openjdk.jmh.annotations.*
  // OFFENDING-END
end FeePolicyHasNoDownstream
