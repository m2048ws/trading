package trading.runtime;

import java.util.function.Function;
import java.util.function.Supplier;
import trading.application.LiveCatalog;
import trading.reference.CatalogBatch;

/** Package-local erased implementation bridge used by InMemoryLiveCatalog. */
final class LiveCatalogBridge {
  private LiveCatalogBridge() {}

  static Object create(Supplier<Object> snapshot, Function<CatalogBatch, Object> commit) {
    return new RefBackedLiveCatalog(snapshot, commit);
  }

  private static final class RefBackedLiveCatalog implements LiveCatalog<Object> {
    private final Supplier<Object> snapshot;
    private final Function<CatalogBatch, Object> commit;

    private RefBackedLiveCatalog(
        Supplier<Object> snapshot, Function<CatalogBatch, Object> commit) {
      this.snapshot = snapshot;
      this.commit = commit;
    }

    @Override
    public Object snapshot() {
      return snapshot.get();
    }

    @Override
    public Object commit(CatalogBatch batch) {
      return commit.apply(batch);
    }
  }
}
