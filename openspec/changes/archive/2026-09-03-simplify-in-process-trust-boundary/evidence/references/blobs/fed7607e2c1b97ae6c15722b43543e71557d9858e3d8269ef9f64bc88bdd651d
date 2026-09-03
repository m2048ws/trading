package trading.benchmark

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*
import trading.risk.*

private trait RiskBenchmarkInputs:
  def directLookup: LotRiskAssessment[? <: Dim, ? <: Dim]
  def maximumSizing: MaxAffordableLots[? <: Dim, ? <: Dim]
  def exhaustiveReference: Option[LotRiskAssessment[? <: Dim, ? <: Dim]]

@State(Scope.Benchmark)
class RiskSizingBenchmarkState:
  @Param(Array("1024"))
  var cap: Int = 1024

  private var initialized: Option[RiskBenchmarkInputs] = None

  @Setup(Level.Trial)
  def setup(): Unit =
    val instrument = createInstrument()
    val checkedCap = PositiveWhole(cap).toOption.get
    val oneThird   = Quantity(instrument.roles.settle.dimension.ref, Rational(1, 3))
    val model      = MonotoneLotRisk.affine(instrument)(
      checkedCap,
      oneThird,
      NonNegative(oneThird).toOption.get
    )
    val budget = NonNegative(
      Quantity(instrument.roles.settle.dimension.ref, Rational(cap / 6))
    ).toOption.get
    val directCoordinate = PositiveWhole(cap / 2).toOption.get

    initialized = Some(new RiskBenchmarkInputs:
      def directLookup: LotRiskAssessment[? <: Dim, ? <: Dim] =
        model.assess(directCoordinate)

      def maximumSizing: MaxAffordableLots[? <: Dim, ? <: Dim] =
        MaxAffordableLots.select(model)(budget)

      def exhaustiveReference: Option[LotRiskAssessment[? <: Dim, ? <: Dim]] =
        var coordinate                                                                              = BigInt(1)
        var best: Option[LotRiskAssessment[instrument.roles.position.D, instrument.roles.settle.D]] = None
        while coordinate <= checkedCap.unrefined do
          val assessment = model.assess(PositiveWhole(coordinate).toOption.get)
          if assessment.downsideRisk.unrefined.coefficient.compare(budget.unrefined.coefficient) <= 0 then
            best = Some(assessment)
          coordinate += 1
        best
    )
  end setup

  def inputs: RiskBenchmarkInputs =
    initialized.fold(throw new IllegalStateException("risk benchmark state was not initialized"))(identity)

  private def createInstrument(): Instrument =
    val assetDefinitions = Vector("btc", "usd", "contract").map: name =>
      AssetDefinition(AssetId.from(name).toOption.get, AtomId(s"risk-benchmark:$name"))

    def gridDefinition(assetName: String, gridName: String, quantum: Rational): GridDefinition =
      GridDefinition(
        GridIdentity(
          DimKey.atom(AtomId(s"risk-benchmark:$assetName")),
          GridKey(GridId.from(gridName).toOption.get, GridVersion.from(1).toOption.get)
        ),
        PositiveRational(quantum).toOption.get
      )

    val contractGridDefinition = gridDefinition("contract", "risk-contract-lots", Rational.one)
    val baseBatch              = CatalogBatch.from(
      assetDefinitions.map(CatalogCommand.RegisterAsset.apply) :+
        CatalogCommand.RegisterGrid(contractGridDefinition)
    ).toOption.get
    val baseState    = CatalogModel.commit(CatalogRoot.create().initialState, baseBatch).toOption.get.state
    val baseSnapshot = baseState.snapshot
    val btc          = baseSnapshot.resolveAsset(assetDefinitions(0).id).toOption.get
    val usd          = baseSnapshot.resolveAsset(assetDefinitions(1).id).toOption.get
    val contract     = baseSnapshot.resolveAsset(assetDefinitions(2).id).toOption.get
    val positionGrid = baseSnapshot.resolveGrid(contract.dimension)(contractGridDefinition.key).toOption.get

    val priceDimension  = DimRef.divide(usd.dimension.ref, btc.dimension.ref).key
    val priceDefinition = GridDefinition(
      GridIdentity(
        priceDimension,
        GridKey(GridId.from("risk-usd-per-btc").toOption.get, GridVersion.from(1).toOption.get)
      ),
      PositiveRational(Rational(1, 2)).toOption.get
    )
    val completeState = CatalogModel.commit(
      baseState,
      CatalogBatch.of(
        CatalogCommand.RegisterDimension(priceDimension),
        CatalogCommand.RegisterGrid(priceDefinition)
      )
    ).toOption.get.state
    val snapshot   = completeState.snapshot
    val priceGrid  = snapshot.resolveGrid(priceDefinition.identity).toOption.get
    val definition = InstrumentDefinition(
      InstrumentIdentity(
        InstrumentId.from("risk-sizing-benchmark").toOption.get,
        UnderlyingId.from("bitcoin-index").toOption.get
      ),
      AssetRoleIds(btc.id, usd.id, contract.id, usd.id),
      ListingDefinition(positionGrid.identity, priceGrid.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    Instrument.fromSpec(InstrumentAssembler.assemble(definition, snapshot).toOption.get)
  end createInstrument
end RiskSizingBenchmarkState

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class RiskSizingBenchmark:
  @Benchmark
  def directCurveLookup(state: RiskSizingBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.directLookup)

  @Benchmark
  def boundaryCertifiedMaximum(state: RiskSizingBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.maximumSizing)

  @Benchmark
  def exhaustiveReferenceEvaluation(state: RiskSizingBenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(state.inputs.exhaustiveReference)
end RiskSizingBenchmark
