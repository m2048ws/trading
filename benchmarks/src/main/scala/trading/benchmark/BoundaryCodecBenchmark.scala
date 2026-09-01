package trading.benchmark

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import trading.application.LiveCatalog
import trading.codec.*
import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*
import trading.runtime.InMemoryLiveCatalog

private final case class BoundaryCodecBenchmarkInputs(
  catalog: LiveCatalog[IO],
  snapshot: CatalogSnapshot,
  record: GeneralGridCoordinateRecord.V1,
  wire: String,
  records: Vector[GeneralGridCoordinateRecord.V1],
  wires: Vector[String])

@State(Scope.Benchmark)
class BoundaryCodecBenchmarkState:
  @Param(Array("general-grid-coordinate-v1"))
  var payloadShape: String = "general-grid-coordinate-v1"

  @Param(Array("351"))
  var payloadUtf8Bytes: Int = 351

  @Param(Array("128"))
  var coordinateDigits: Int = 128

  @Param(Array("1024"))
  var batchSize: Int = 1024

  private var initialized: Option[BoundaryCodecBenchmarkInputs] = None

  @Setup(Level.Trial)
  def setup(): Unit =
    require(payloadShape == "general-grid-coordinate-v1", s"unsupported benchmark payload shape: $payloadShape")
    val dimension = DimKey.atom(AtomId("codec-benchmark:quantity"))
    val identity  = GridIdentity(
      dimension,
      GridKey(
        required(GridId.from("codec-benchmark-grid")),
        required(GridVersion.from(1))
      )
    )
    val definition = GridDefinition(identity, required(PositiveRational(Rational.one)))
    val bootstrap  = CatalogBatch.of(
      CatalogCommand.RegisterDimension(dimension),
      CatalogCommand.RegisterGrid(definition)
    )
    val catalog = InMemoryLiveCatalog
      .create[IO](Some(bootstrap))
      .unsafeRunSync()
      .fold(errors => throw new IllegalStateException(errors.toString), value => value)

    // Capture once outside every codec operation; timed batch methods receive only this immutable value.
    val snapshot   = catalog.snapshot.unsafeRunSync()
    val grid       = required(snapshot.resolveGrid(identity))
    val coordinate = BigInt("9" * coordinateDigits)
    val record     = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(coordinate))
    val wire       = required(GeneralGridCoordinateRecord.encode(record))
    val actualUtf8 = wire.getBytes(StandardCharsets.UTF_8).length
    require(
      actualUtf8 == payloadUtf8Bytes,
      s"recorded payload size $payloadUtf8Bytes does not match generated UTF-8 size $actualUtf8"
    )
    initialized = Some(
      BoundaryCodecBenchmarkInputs(
        catalog,
        snapshot,
        record,
        wire,
        Vector.fill(batchSize)(record),
        Vector.fill(batchSize)(wire)
      )
    )
  end setup

  def inputs: BoundaryCodecBenchmarkInputs =
    initialized.fold(throw new IllegalStateException("boundary-codec benchmark state was not initialized"))(identity)

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end BoundaryCodecBenchmarkState

/**
 * JMH records the JVM and arguments; explicit parameters record payload shape/UTF-8 size, exact digits, and batch size.
 * One thread keeps JSON, one live capture, pure parsed lookup, and combined decode/reconstruction costs separable.
 */
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Threads(1)
class BoundaryCodecBenchmark:
  @Benchmark
  def parseJson(state: BoundaryCodecBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(GeneralGridCoordinateRecord.parse(state.inputs.wire))

  @Benchmark
  def renderJson(state: BoundaryCodecBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(GeneralGridCoordinateRecord.encode(state.inputs.record))

  @Benchmark
  def captureOneSnapshotOutsideCodec(state: BoundaryCodecBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.catalog.snapshot.unsafeRunSync())

  @Benchmark
  def reconstructParsedBatch(state: BoundaryCodecBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(
      GeneralGridCoordinateRecord.reconstructBatch(state.inputs.records, state.inputs.snapshot)
    )

  @Benchmark
  def decodeAndReconstructBatch(state: BoundaryCodecBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(
      GeneralGridCoordinateRecord.decodeAndReconstructBatch(state.inputs.wires, state.inputs.snapshot)
    )
end BoundaryCodecBenchmark
