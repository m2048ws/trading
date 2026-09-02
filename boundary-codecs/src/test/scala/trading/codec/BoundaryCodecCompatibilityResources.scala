package trading.codec

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import trading.economics.instrument.InstrumentId
import trading.economics.instrument.UnderlyingId
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.quantity.refinement.PositiveRational
import trading.reference.*

/** Deterministic source for the checked-in V1 schemas and compatibility fixtures. */
private[codec] object BoundaryCodecCompatibilityResources:
  final case class Resource(repositoryPath: String, content: String):
    val classpathPath: String = repositoryPath.split("/resources/", 2).last
  end Resource

  private val schemaRoot  = "boundary-codecs/src/main/resources/trading/codec/schema"
  private val goldenRoot  = "boundary-codecs/src/test/resources/trading/codec/golden"
  private val invalidRoot = "boundary-codecs/src/test/resources/trading/codec/invalid"

  private val dimension = DimKey(
    Vector(
      AtomId("compatibility:base")  -> BigInt(1),
      AtomId("compatibility:quote") -> BigInt(-1)
    )
  )
  private val positionDimension = DimKey.atom(AtomId("compatibility:position"))
  private val assetDimension    = DimKey.atom(AtomId("compatibility:asset"))
  private val generalGrid       = GridIdentity(
    dimension,
    GridKey(required(GridId.from("compatibility-general-grid")), required(GridVersion.from(7)))
  )
  private val positionGrid = GridIdentity(
    positionDimension,
    GridKey(required(GridId.from("compatibility-position-grid")), required(GridVersion.from(1)))
  )
  private val assetGrid = GridIdentity(
    assetDimension,
    GridKey(required(GridId.from("compatibility-asset-grid")), required(GridVersion.from(3)))
  )
  private val assetId      = required(AssetId.from("compatibility-asset"))
  private val instrumentId = required(InstrumentId.from("compatibility-primary"))

  val schemaResources: Vector[Resource] = Vector(
    Resource(s"$schemaRoot/general-grid-coordinate-v1.schema.json",
      schemaDocument(required(GeneralGridCoordinateRecord.schema()))),
    Resource(s"$schemaRoot/asset-grid-coordinate-v1.schema.json",
      schemaDocument(required(AssetGridCoordinateRecord.schema()))),
    Resource(s"$schemaRoot/catalog-journal-entry-v1.schema.json",
      schemaDocument(required(CatalogJournalEntry.schema()))),
    Resource(s"$schemaRoot/instrument-definition-v1.schema.json",
      schemaDocument(required(InstrumentDefinitionRecord.schema()))),
    Resource(s"$schemaRoot/order-v1.schema.json", schemaDocument(required(OrderRecord.schema()))),
    Resource(s"$schemaRoot/order-scenario-v1.schema.json", schemaDocument(required(OrderScenarioRecord.schema()))),
    Resource(s"$schemaRoot/round-trip-scenario-v1.schema.json",
      schemaDocument(required(RoundTripScenarioRecord.schema())))
  )

  private val generalWire = required(
    GeneralGridCoordinateRecord.encode(
      GeneralGridCoordinateRecord.V1(generalGrid, BigInt("-12345678901234567890"))
    )
  )
  private val assetWire = required(
    AssetGridCoordinateRecord.encode(AssetGridCoordinateRecord.V1(assetId, assetGrid, BigInt(987654321012345678L)))
  )
  private val journalWires   = catalogHistory()
  private val instrumentWire = required(
    InstrumentDefinitionRecord.encode(
      InstrumentDefinitionRecord.V1(
        InstrumentDefinitionRecord.Identity(
          instrumentId,
          required(UnderlyingId.from("compatibility-underlying"))
        ),
        InstrumentDefinitionRecord.RoleAssetIds(
          required(AssetId.from("compatibility-base")),
          required(AssetId.from("compatibility-quote")),
          required(AssetId.from("compatibility-position")),
          required(AssetId.from("compatibility-settle"))
        ),
        InstrumentDefinitionRecord.Listing(positionGrid, generalGrid),
        InstrumentDefinitionRecord.Payoff(Rational(7, 13), Rational(-19, 23))
      )
    )
  )

  private val immediateMarket = OrderRecord.V1(
    instrumentId,
    OrderRecord.Side.Buy,
    10,
    OrderRecord.PositionEffect.Unrestricted,
    OrderRecord.Activation.Immediate,
    OrderRecord.Execution.Market(OrderRecord.TimeInForce.ImmediateOrCancel)
  )
  private val fixedMarket = OrderRecord.V1(
    instrumentId,
    OrderRecord.Side.Sell,
    10,
    OrderRecord.PositionEffect.ReduceOnly,
    OrderRecord.Activation.Fixed(
      OrderRecord.PriceReference.Mark,
      OrderRecord.TriggerComparison.AtOrAbove,
      100
    ),
    OrderRecord.Execution.Market(OrderRecord.TimeInForce.FillOrKill)
  )
  private val trailingLimit = OrderRecord.V1(
    instrumentId,
    OrderRecord.Side.Buy,
    10,
    OrderRecord.PositionEffect.Unrestricted,
    OrderRecord.Activation.Trailing(
      OrderRecord.PriceReference.Index,
      OrderRecord.TriggerComparison.AtOrBelow,
      3
    ),
    OrderRecord.Execution.Priced(
      OrderRecord.Pricing.Limit(102),
      OrderRecord.TimeInForce.GoodTillCancelled,
      OrderRecord.LiquidityConstraint.Unrestricted,
      OrderRecord.Visibility.Displayed
    )
  )
  private val immediatePegged = OrderRecord.V1(
    instrumentId,
    OrderRecord.Side.Sell,
    10,
    OrderRecord.PositionEffect.ReduceOnly,
    OrderRecord.Activation.Immediate,
    OrderRecord.Execution.Priced(
      OrderRecord.Pricing.Pegged(OrderRecord.PriceReference.Last, -2),
      OrderRecord.TimeInForce.Day,
      OrderRecord.LiquidityConstraint.MakerOnly,
      OrderRecord.Visibility.Hidden
    )
  )
  private val fixedIceberg = OrderRecord.V1(
    instrumentId,
    OrderRecord.Side.Buy,
    10,
    OrderRecord.PositionEffect.Unrestricted,
    OrderRecord.Activation.Fixed(
      OrderRecord.PriceReference.Mark,
      OrderRecord.TriggerComparison.AtOrAbove,
      100
    ),
    OrderRecord.Execution.Priced(
      OrderRecord.Pricing.Limit(102),
      OrderRecord.TimeInForce.GoodTillCancelled,
      OrderRecord.LiquidityConstraint.Unrestricted,
      OrderRecord.Visibility.Iceberg(4)
    )
  )
  private val orderWires =
    Vector(immediateMarket, fixedMarket, trailingLimit, immediatePegged, fixedIceberg)
      .map(record => required(OrderRecord.encode(record)))

  private val tokenConversion = OrderScenarioRecord.AdditionalConversion(
    required(AssetId.from("compatibility-token")),
    Rational(3)
  )
  private def market(price: BigInt): OrderScenarioRecord.Market =
    OrderScenarioRecord.Market(price, Rational(price), Rational(2), Vector(tokenConversion))

  private def scenario(
    order: OrderRecord.V1,
    evidence: OrderScenarioRecord.ActivationEvidence,
    pricing: OrderScenarioRecord.PricingResolution,
    price: BigInt,
    liquidity: OrderScenarioRecord.Liquidity
  ): OrderScenarioRecord.V1 =
    OrderScenarioRecord.V1(
      order,
      evidence,
      pricing,
      Vector(OrderScenarioRecord.Slice(order.lotCoordinate, liquidity, market(price)))
    )

  private val immediateScenario = scenario(
    immediateMarket,
    OrderScenarioRecord.ActivationEvidence.Immediate,
    OrderScenarioRecord.PricingResolution.Direct,
    100,
    OrderScenarioRecord.Liquidity.Taker
  )
  private val fixedScenario = scenario(
    fixedMarket,
    OrderScenarioRecord.ActivationEvidence.Fixed(101),
    OrderScenarioRecord.PricingResolution.Direct,
    101,
    OrderScenarioRecord.Liquidity.Taker
  )
  private val trailingScenario = scenario(
    trailingLimit.copy(execution = OrderRecord.Execution.Priced(
      OrderRecord.Pricing.Pegged(OrderRecord.PriceReference.Mark, 2),
      OrderRecord.TimeInForce.Day,
      OrderRecord.LiquidityConstraint.Unrestricted,
      OrderRecord.Visibility.Displayed
    )),
    OrderScenarioRecord.ActivationEvidence.Trailing(100, 97),
    OrderScenarioRecord.PricingResolution.Pegged(100, 102),
    101,
    OrderScenarioRecord.Liquidity.Maker
  )
  private val exitScenario = scenario(
    immediateMarket.copy(side = OrderRecord.Side.Sell),
    OrderScenarioRecord.ActivationEvidence.Immediate,
    OrderScenarioRecord.PricingResolution.Direct,
    102,
    OrderScenarioRecord.Liquidity.Taker
  )
  private val scenarioWires =
    Vector(immediateScenario, fixedScenario, trailingScenario)
      .map(record => required(OrderScenarioRecord.encode(record)))
  private val roundTripWire = required(
    RoundTripScenarioRecord.encode(RoundTripScenarioRecord.V1(immediateScenario, exitScenario))
  )

  val goldenResources: Vector[Resource] = Vector(
    Resource(s"$goldenRoot/general-grid-coordinate-v1.json", document(generalWire)),
    Resource(s"$goldenRoot/asset-grid-coordinate-v1.json", document(assetWire)),
    Resource(s"$goldenRoot/catalog-journal-history-v1.jsonl", lines(journalWires)),
    Resource(s"$goldenRoot/instrument-definition-v1.json", document(instrumentWire)),
    Resource(s"$goldenRoot/orders-v1.jsonl", lines(orderWires)),
    Resource(s"$goldenRoot/order-scenarios-v1.jsonl", lines(scenarioWires)),
    Resource(s"$goldenRoot/round-trip-scenario-v1.json", document(roundTripWire))
  )

  private val firstGoldenByFamily = Vector(
    "general-grid-coordinate" -> generalWire,
    "asset-grid-coordinate"   -> assetWire,
    "catalog-journal-entry"   -> journalWires.head,
    "instrument-definition"   -> instrumentWire,
    "order"                   -> orderWires.head,
    "order-scenario"          -> scenarioWires.head,
    "round-trip-scenario"     -> roundTripWire
  )

  private val versionInvalidResources: Vector[Resource] = firstGoldenByFamily.map: (name, valid) =>
    Resource(
      s"$invalidRoot/$name-unknown-version.json",
      document(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
    )

  private val structuralInvalidResources: Vector[Resource] = firstGoldenByFamily.map: (name, valid) =>
    Resource(
      s"$invalidRoot/$name-missing-payload.json",
      document(withoutPayload(valid))
    )

  val invalidResources: Vector[Resource] = versionInvalidResources ++ structuralInvalidResources

  val all: Vector[Resource] = schemaResources ++ goldenResources ++ invalidResources

  def writeTo(repositoryRoot: Path): Unit =
    all.foreach: resource =>
      val target = repositoryRoot.resolve(resource.repositoryPath)
      Files.createDirectories(target.getParent())
      Files.writeString(target, resource.content, StandardCharsets.UTF_8)

  private def catalogHistory(): Vector[String] =
    val asset = AssetDefinition(
      required(AssetId.from("compatibility-journal-asset")),
      AtomId("compatibility:journal-asset")
    )
    val assetDimension = DimKey.atom(asset.dimensionAtom)
    val firstGrid      = GridDefinition(
      GridIdentity(
        assetDimension,
        GridKey(required(GridId.from("compatibility-journal-grid")), required(GridVersion.from(1)))
      ),
      required(PositiveRational(Rational(1, 100)))
    )
    val secondGrid = GridDefinition(
      firstGrid.identity.copy(key = firstGrid.identity.key.copy(version = required(GridVersion.from(2)))),
      required(PositiveRational(Rational(1, 1_000)))
    )
    val independent = DimKey(
      Vector(AtomId("compatibility:journal-a") -> BigInt(2), AtomId("compatibility:journal-b") -> BigInt(-1))
    )
    val batches = Vector(
      CatalogBatch.of(CatalogCommand.RegisterAsset(asset), CatalogCommand.RegisterGrid(firstGrid)),
      CatalogBatch.one(CatalogCommand.RegisterDimension(independent)),
      CatalogBatch.one(CatalogCommand.RegisterGrid(secondGrid))
    )
    var state = CatalogRoot.create().initialState
    batches.map: batch =>
      val transition = required(CatalogModel.commit(state, batch))
      state = transition.state
      val published = transition.outcome match
        case value: CatalogCommit.Published => value
        case other => throw new IllegalStateException(s"fixture batch was not published: $other")
      required(CatalogJournalEntry.encode(CatalogJournalEntry.fromPublished(batch, published)))
  end catalogHistory

  private def document(value: String): String       = value + "\n"
  private def lines(values: Vector[String]): String = values.mkString("", "\n", "\n")
  private def schemaDocument(value: String): String =
    pretty(required(StrictJson.parse(value)), 0) + "\n"

  private def withoutPayload(value: String): String =
    required(StrictJson.parse(value)).value match
      case JsonValue.JObject(fields) =>
        required(CanonicalJson.render(JsonNode.obj(
          fields.filterNot(_.name == "payload").map(field => field.name -> field.value)*
        )))
      case other => throw new IllegalArgumentException(s"fixture envelope was not an object: $other")

  private def pretty(node: JsonNode, depth: Int): String =
    val indentation = "  " * depth
    val nested      = "  " * (depth + 1)
    node.value match
      case JsonValue.JObject(fields) if fields.isEmpty => "{}"
      case JsonValue.JObject(fields)                   =>
        fields
          .map(field =>
            s"$nested${required(CanonicalJson.render(JsonNode.string(field.name)))}: ${pretty(field.value, depth + 1)}"
          )
          .mkString("{\n", ",\n", s"\n$indentation}")
      case JsonValue.JArray(values) if values.isEmpty => "[]"
      case JsonValue.JArray(values)                   =>
        values.map(value => s"$nested${pretty(value, depth + 1)}").mkString("[\n", ",\n", s"\n$indentation]")
      case _ => required(CanonicalJson.render(node))

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end BoundaryCodecCompatibilityResources

@main def generateBoundaryCodecCompatibilityResources(repositoryRoot: String): Unit =
  BoundaryCodecCompatibilityResources.writeTo(Paths.get(repositoryRoot).toAbsolutePath.normalize())
