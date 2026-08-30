package trading.runtime;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import trading.application.LiveCatalog;
import trading.reference.CatalogBatch;

/** JVM-private implementation bridge; construction remains owned by InMemoryLiveCatalog. */
final class LiveCatalogBridge {
  private static final StackWalker CALLERS =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  private static final Class<?> FACTORY_CLASS = loadFactoryClass();

  private LiveCatalogBridge() {}

  static Object create(Supplier<Object> snapshot, Function<CatalogBatch, Object> commit) {
    Class<?> caller = CALLERS.getCallerClass();
    if (caller != FACTORY_CLASS) {
      throw new SecurityException("live-catalog construction is owned by InMemoryLiveCatalog");
    }
    return new RefBackedLiveCatalog(snapshot, commit);
  }

  private static Class<?> loadFactoryClass() {
    try {
      Class<?> factory =
          Class.forName(
              "trading.runtime.InMemoryLiveCatalog$", false, LiveCatalogBridge.class.getClassLoader());
      if (!Objects.equals(
          factory.getProtectionDomain().getCodeSource(),
          LiveCatalogBridge.class.getProtectionDomain().getCodeSource())) {
        throw new ExceptionInInitializerError("live-catalog factory must share the bridge code source");
      }
      return factory;
    } catch (ClassNotFoundException error) {
      throw new ExceptionInInitializerError(error);
    }
  }

  private static final class RefBackedLiveCatalog implements LiveCatalog<Object> {
    private final Supplier<Object> snapshot;
    private final Function<CatalogBatch, Object> commit;

    private RefBackedLiveCatalog(
        Supplier<Object> snapshot, Function<CatalogBatch, Object> commit) {
      if (CALLERS.getCallerClass() != LiveCatalogBridge.class) {
        throw new SecurityException("live-catalog implementation construction is bridge-owned");
      }
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
