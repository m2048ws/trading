package external.risk.negative

import trading.risk.RiskIdentityError

object RiskHasNoDownstream:
  val ownedType: Class[RiskIdentityError] = classOf[RiskIdentityError]

  // OFFENDING-BEGIN
  object MissingOrder:
    import trading.order.*

  object MissingScenario:
    import trading.scenario.*

  object MissingFeePolicy:
    import trading.fee.*

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
end RiskHasNoDownstream
