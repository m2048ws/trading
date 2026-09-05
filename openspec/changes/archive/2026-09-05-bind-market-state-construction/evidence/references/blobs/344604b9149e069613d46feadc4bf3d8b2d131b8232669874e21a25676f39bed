package trading.benchmark

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

private trait MarketConstructionInputs:
  def direct: Either[MarketStateViolations, MarketState[? <: Dim, ? <: Dim, ? <: Dim]]
  def scoped: Either[MarketStateViolations, MarketState[? <: Dim, ? <: Dim, ? <: Dim]]

@State(Scope.Benchmark)
class MarketStateConstructionBenchmarkState:
  @Param(Array("quote", "third"))
  var settlement: String = "quote"

  @Param(Array("0", "1"))
  var additionalCount: Int = 0

  private var initialized: Option[MarketConstructionInputs] = None

  @Setup(Level.Trial)
  def setup(): Unit =
    val definitions = Vector("base", "quote", "position", "third", "token").map: name =>
      AssetDefinition(AssetId.from(s"market-$name").toOption.get, AtomId(s"market-benchmark:$name"))
    val priceKey = DimKey.multiply(DimKey.atom(definitions(1).dimensionAtom),
      DimKey.inverse(DimKey.atom(definitions(0).dimensionAtom)))
    def grid(name: String, dimension: DimKey): GridDefinition =
      GridDefinition(
        GridIdentity(dimension,
          GridKey(GridId.from(name).toOption.get, GridVersion.from(1).toOption.get)),
        PositiveRational(Rational(1, 2)).toOption.get
      )
    val lots  = grid("market-lots", DimKey.atom(definitions(2).dimensionAtom))
    val ticks = grid("market-price", priceKey)
    val batch = CatalogBatch.from(definitions.map(CatalogCommand.RegisterAsset.apply) ++ Vector(
      CatalogCommand.RegisterDimension(priceKey),
      CatalogCommand.RegisterGrid(lots),
      CatalogCommand.RegisterGrid(ticks)
    )).toOption.get
    val snapshot   = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state.snapshot
    val definition = InstrumentDefinition(
      InstrumentIdentity(InstrumentId.from("market-benchmark").toOption.get,
        UnderlyingId.from("market-underlying").toOption.get),
      AssetRoleIds(definitions(0).id, definitions(1).id, definitions(2).id,
        definitions(if settlement == "quote" then 1 else 3).id),
      ListingDefinition(lots.identity, ticks.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    val instrument = Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)
    val markets    = MarketState.forInstrument(instrument)
    val price      = Price.exact(instrument)(Rational(100)).toOption.get
    val rate       = Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref,
      if settlement == "quote" then Rational.one else Rational(9, 10))
    val token      = snapshot.resolveAsset(definitions(4).id).toOption.get
    val additional = if additionalCount == 0 then Vector.empty
    else
      Vector(SettlementConversion.exact(instrument)(token)(Rational(2, 7)).toOption.get)
    assert(MarketState.fromQuoteRate(instrument)(price, rate, additional).isRight)
    assert(markets.fromQuoteRate(price, rate, additional).isRight)
    initialized = Some(new MarketConstructionInputs:
      def direct: Either[MarketStateViolations, instrument.MarketState] =
        MarketState.fromQuoteRate(instrument)(price, rate, additional)
      def scoped: Either[MarketStateViolations, instrument.MarketState] =
        markets.fromQuoteRate(price, rate, additional)
    )
  end setup

  def inputs: MarketConstructionInputs =
    initialized.getOrElse(throw new IllegalStateException("market construction benchmark is not initialized"))
end MarketStateConstructionBenchmarkState

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class MarketStateConstructionBenchmark:
  @Benchmark
  def direct(state: MarketStateConstructionBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.direct)

  @Benchmark
  def reusedScope(state: MarketStateConstructionBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.scoped)
end MarketStateConstructionBenchmark
