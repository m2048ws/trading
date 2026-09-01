package trading.benchmark

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.infra.ThreadParams

import trading.application.LiveCatalog
import trading.quantity.AtomId
import trading.reference.*
import trading.runtime.InMemoryLiveCatalog

@State(Scope.Benchmark)
class SharedLiveCatalogState:
  private var initialized: Option[(LiveCatalog[IO], Vector[AssetId])] = None

  @Setup(Level.Trial)
  def setup(): Unit =
    val definitions = Vector.tabulate(1024): index =>
      AssetDefinition(
        required(AssetId.from(s"live-benchmark-$index")),
        AtomId(s"live-benchmark:$index")
      )
    val bootstrap = required(CatalogBatch.from(definitions.map(CatalogCommand.RegisterAsset.apply)))
    val catalog = InMemoryLiveCatalog
      .create[IO](Some(bootstrap))
      .unsafeRunSync()
      .fold(errors => throw new IllegalStateException(errors.toString), identity)
    initialized = Some(catalog -> definitions.map(_.id))

  def catalog: LiveCatalog[IO] = initializedValue._1
  def ids: Vector[AssetId]     = initializedValue._2

  private def initializedValue: (LiveCatalog[IO], Vector[AssetId]) =
    initialized.fold(throw new IllegalStateException("live benchmark state was not initialized"))(identity)

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end SharedLiveCatalogState

@State(Scope.Thread)
class UncontendedCommitState:
  private var initialized: Option[(LiveCatalog[IO], CatalogBatch)] = None

  @Setup(Level.Invocation)
  def setup(): Unit =
    val definition = AssetDefinition(
      required(AssetId.from("uncontended-publication")),
      AtomId("uncontended:publication")
    )
    val catalog = InMemoryLiveCatalog
      .create[IO](None)
      .unsafeRunSync()
      .fold(errors => throw new IllegalStateException(errors.toString), identity)
    initialized = Some(catalog -> CatalogBatch.one(CatalogCommand.RegisterAsset(definition)))

  def catalog: LiveCatalog[IO] = initializedValue._1
  def batch: CatalogBatch      = initializedValue._2

  private def initializedValue: (LiveCatalog[IO], CatalogBatch) =
    initialized.fold(throw new IllegalStateException("commit benchmark state was not initialized"))(identity)

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end UncontendedCommitState

@State(Scope.Benchmark)
class ContendedLiveCatalogState:
  private var initialized: Option[LiveCatalog[IO]] = None

  @Setup(Level.Iteration)
  def setup(): Unit =
    initialized = Some(
      InMemoryLiveCatalog
        .create[IO](None)
        .unsafeRunSync()
        .fold(errors => throw new IllegalStateException(errors.toString), identity)
    )

  def catalog: LiveCatalog[IO] =
    initialized.fold(throw new IllegalStateException("contended catalog state was not initialized"))(identity)
end ContendedLiveCatalogState

@State(Scope.Thread)
class ContendedCommitState:
  private var threadIndex = 0
  private var sequence    = 0L

  @Setup(Level.Iteration)
  def setup(parameters: ThreadParams): Unit =
    threadIndex = parameters.getThreadIndex
    sequence = 0L

  def nextBatch(): CatalogBatch =
    sequence += 1L
    val name = s"contended-publication-$threadIndex-$sequence"
    val definition = AssetDefinition(
      required(AssetId.from(name)),
      AtomId(s"contended:publication:$threadIndex:$sequence")
    )
    CatalogBatch.one(CatalogCommand.RegisterAsset(definition))

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end ContendedCommitState

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class LiveCatalogRuntimeBenchmark:
  @Benchmark
  def captureSnapshot(state: SharedLiveCatalogState, blackhole: Blackhole): Unit =
    blackhole.consume(state.catalog.snapshot.unsafeRunSync())

  @Benchmark
  def captureOnceAndResolveAll(state: SharedLiveCatalogState, blackhole: Blackhole): Unit =
    val snapshot = state.catalog.snapshot.unsafeRunSync()
    state.ids.foreach(id => blackhole.consume(snapshot.resolveAsset(id)))

  @Benchmark
  def commitUncontended(state: UncontendedCommitState, blackhole: Blackhole): Unit =
    blackhole.consume(state.catalog.commit(state.batch).unsafeRunSync())

  @Benchmark
  @Threads(4)
  def commitContended(
    state: ContendedLiveCatalogState,
    commitState: ContendedCommitState,
    blackhole: Blackhole
  ): Unit =
    state.catalog.commit(commitState.nextBatch()).unsafeRunSync() match
      case published @ Right(_: CatalogCommit.Published) => blackhole.consume(published)
      case other => throw new IllegalStateException(s"contended publication did not publish: $other")
end LiveCatalogRuntimeBenchmark
