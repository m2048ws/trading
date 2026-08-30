package external.application.negative

import trading.application.LiveCatalog
import trading.reference.CatalogSnapshot

object ConcreteEffectLeak:
  def generic[F[_]](catalog: LiveCatalog[F]): F[CatalogSnapshot] = catalog.snapshot

  // OFFENDING-BEGIN
  import cats.effect.IO
  def concrete(catalog: LiveCatalog[IO]): IO[CatalogSnapshot] = catalog.snapshot
  // OFFENDING-END
end ConcreteEffectLeak
