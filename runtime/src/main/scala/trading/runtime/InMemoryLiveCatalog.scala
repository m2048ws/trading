package trading.runtime

import java.util.function.Function as JavaFunction
import java.util.function.Supplier

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
            val snapshotAction = state.get.map(_.snapshot)
            val commitAction   = (batch: CatalogBatch) =>
              state.modify: current =>
                CatalogModel.commit(current, batch) match
                  case Left(errors)      => current          -> Left(errors)
                  case Right(transition) => transition.state -> Right(transition.outcome)

            val snapshotSupplier: Supplier[Object]                 = () => snapshotAction.asInstanceOf[Object]
            val commitFunction: JavaFunction[CatalogBatch, Object] =
              batch => commitAction(batch).asInstanceOf[Object]
            val implementation = LiveCatalogBridge.create(snapshotSupplier, commitFunction)
            // The JVM-private Java bridge verifies this factory as its direct caller and implements the erased
            // LiveCatalog shape. Both actions come from the same F and state, so the cast cannot mix effects or roots.
            Right(implementation.asInstanceOf[LiveCatalog[F]])
end InMemoryLiveCatalog
