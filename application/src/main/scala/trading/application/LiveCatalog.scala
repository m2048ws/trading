package trading.application

import trading.reference.*

/** Interpreter-neutral capability for one coherent snapshot capture and one atomic catalog transaction. */
trait LiveCatalog[F[_]]:
  def snapshot: F[CatalogSnapshot]

  def commit(batch: CatalogBatch): F[Either[CatalogViolations, CatalogCommit]]
end LiveCatalog
