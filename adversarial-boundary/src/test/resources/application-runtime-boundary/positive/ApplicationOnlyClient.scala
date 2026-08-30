package external.application.positive

import trading.application.LiveCatalog
import trading.reference.CatalogSnapshot

object ApplicationOnlyClient:
  def capture[F[_]](catalog: LiveCatalog[F]): F[CatalogSnapshot] = catalog.snapshot
end ApplicationOnlyClient
