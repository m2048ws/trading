package external.application.positive

import cats.effect.IO

import trading.application.LiveCatalog

object RuntimeDependencyClient:
  val delayed: IO[Int]                    = IO.delay(17)
  val unresolved: Option[LiveCatalog[IO]] = None
end RuntimeDependencyClient
