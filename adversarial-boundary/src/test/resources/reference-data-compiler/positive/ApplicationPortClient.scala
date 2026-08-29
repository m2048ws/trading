package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*
import trading.application.LiveCatalog
import trading.reference.*

object ApplicationPortClient:
  type Id[A] = A

  final class DeterministicTestCatalog(private var state: CatalogState) extends LiveCatalog[Id]:
    def snapshot: CatalogSnapshot = state.snapshot

    def commit(batch: CatalogBatch): Either[CatalogViolations, CatalogCommit] =
      CatalogModel.commit(state, batch).map: transition =>
        state = transition.state
        transition.outcome

  def capture[F[_]](catalog: LiveCatalog[F]): F[CatalogSnapshot] = catalog.snapshot

  def publish[F[_]](
    catalog: LiveCatalog[F],
    batch: CatalogBatch
  ): F[Either[CatalogViolations, CatalogCommit]] =
    catalog.commit(batch)

  val catalog = new DeterministicTestCatalog(CatalogRoot.create().initialState)
  val result = publish(catalog, CatalogBatch.one(CatalogCommand.RegisterAsset(definition)))
  val captured = capture(catalog)

  assert(result.isRight)
  assert(captured.resolveAsset(definition.id).isRight)
end ApplicationPortClient
