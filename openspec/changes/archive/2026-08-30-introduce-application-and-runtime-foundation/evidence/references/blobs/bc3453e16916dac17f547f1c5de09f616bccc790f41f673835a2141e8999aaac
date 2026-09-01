package trading.runtime

import cats.effect.IO

object RuntimeInternalsUnavailable:
  val publicFactory = InMemoryLiveCatalog.create[IO](None)

  // OFFENDING-BEGIN
  val internalClass = classOf[LiveCatalogBridge.RefBackedLiveCatalog]
  // OFFENDING-END
end RuntimeInternalsUnavailable
