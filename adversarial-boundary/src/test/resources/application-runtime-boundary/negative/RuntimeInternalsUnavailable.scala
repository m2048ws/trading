package external.application.negative

import cats.effect.IO

import trading.runtime.InMemoryLiveCatalog

object RuntimeInternalsUnavailable:
  val publicFactory = InMemoryLiveCatalog.create[IO](None)

  // OFFENDING-BEGIN
  val internalClass = classOf[trading.runtime.RefBackedLiveCatalog[IO]]
  // OFFENDING-END
end RuntimeInternalsUnavailable
