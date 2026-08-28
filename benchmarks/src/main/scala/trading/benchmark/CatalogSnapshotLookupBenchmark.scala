package trading.benchmark

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import trading.quantity.*
import trading.reference.*

@State(Scope.Benchmark)
class CatalogBenchmarkState:
  @Param(Array("1024"))
  var catalogSize: Int = 1024

  private var initialized: Option[(CatalogSnapshot, Vector[AssetId])] = None

  @Setup(Level.Trial)
  def setup(): Unit =
    val definitions = Vector.tabulate(catalogSize): index =>
      AssetDefinition(
        AssetId.from(s"benchmark-asset-$index").toOption.get,
        AtomId(s"benchmark:asset:$index")
      )
    val batch      = CatalogBatch.from(definitions.map(CatalogCommand.RegisterAsset.apply)).toOption.get
    val transition = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get
    initialized = Some(transition.state.snapshot -> definitions.map(_.id))

  def snapshot: CatalogSnapshot =
    initialized.fold(throw new IllegalStateException("benchmark state was not initialized"))(_._1)

  def ids: Vector[AssetId] =
    initialized.fold(throw new IllegalStateException("benchmark state was not initialized"))(_._2)
end CatalogBenchmarkState

@State(Scope.Thread)
class CatalogReaderState:
  private var cursor: Int = 0

  def next(size: Int): Int =
    val current = cursor
    cursor = if current + 1 == size then 0 else current + 1
    current
end CatalogReaderState

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class CatalogSnapshotLookupBenchmark:
  @Benchmark
  def resolveAsset(
    catalog: CatalogBenchmarkState,
    reader: CatalogReaderState,
    blackhole: Blackhole
  ): Unit =
    val ids = catalog.ids
    blackhole.consume(catalog.snapshot.resolveAsset(ids(reader.next(ids.size))))
end CatalogSnapshotLookupBenchmark
