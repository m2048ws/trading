package trading.runtime

import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.syntax.all.*

import trading.application.LiveCatalog
import trading.reference.*

/** Resource-free concurrent in-memory interpreter for one fresh catalog lineage. */
object InMemoryLiveCatalog:
  /** Create one interpreter, optionally evaluating an explicit bootstrap before exposing the capability. */
  def create[F[_]: Sync](
    bootstrap: Option[CatalogBatch]
  ): F[Either[CatalogViolations, LiveCatalog[F]]] =
    Sync[F]
      .delay:
        val initial = CatalogRoot.create().initialState
        bootstrap match
          case None        => Right(initial)
          case Some(batch) => CatalogModel.commit(initial, batch).map(_.state)
      .flatMap:
        case Left(errors)   => Sync[F].pure(Left(errors))
        case Right(initial) =>
          Ref.of[F, CatalogState](initial).map: state =>
            Right(new RefBackedLiveCatalog[F](state): LiveCatalog[F])
end InMemoryLiveCatalog

private final class RefBackedLiveCatalog[F[_]: Sync](state: Ref[F, CatalogState]) extends LiveCatalog[F]:
  def snapshot: F[CatalogSnapshot] =
    state.get.map(_.snapshot)

  def commit(batch: CatalogBatch): F[Either[CatalogViolations, CatalogCommit]] =
    state.modify: current =>
      CatalogModel.commit(current, batch) match
        case Left(errors)      => current          -> Left(errors)
        case Right(transition) => transition.state -> Right(transition.outcome)
end RefBackedLiveCatalog
