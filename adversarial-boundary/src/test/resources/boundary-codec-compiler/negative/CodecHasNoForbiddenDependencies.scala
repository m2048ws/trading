package external.codec.negative

import trading.order.Side

object CodecHasNoForbiddenDependencies:
  val ownedInput: Side = Side.Buy

  // OFFENDING-BEGIN
  object MissingFeePolicy:
    import trading.fee.*

  object MissingRisk:
    import trading.risk.*

  object MissingApplication:
    import trading.application.*

  object MissingRuntime:
    import trading.runtime.*

  object MissingEffects:
    import cats.effect.*

  object MissingStreams:
    import fs2.*

  object MissingPersistence:
    import doobie.*

  object MissingTelemetry:
    import io.opentelemetry.api.*

  object MissingDatabind:
    import tools.jackson.databind.*

  object MissingJacksonScala:
    import tools.jackson.module.scala.*

  object MissingCirce:
    import io.circe.*

  object MissingSchemaOracle:
    import com.networknt.schema.*

  object MissingJcsOracle:
    import org.erdtman.jcs.*
  // OFFENDING-END
