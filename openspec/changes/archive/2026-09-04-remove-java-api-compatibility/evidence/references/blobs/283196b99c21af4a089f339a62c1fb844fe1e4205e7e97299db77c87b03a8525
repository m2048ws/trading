package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*
import scala.util.Using

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import munit.FunSuite
import org.erdtman.jcs.JsonCanonicalizer

import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational
import trading.quantity.refinement.PositiveRational
import trading.reference.*

final class BoundaryCodecCompatibilitySuite extends FunSuite:
  private trait Family:
    def name: String
    def schemaId: String
    def recordType: String
    def schemaResource: String
    def goldenResource: String
    def invalidResource: String
    def structuralInvalidResource: String
    def normalize(input: String): Either[String, String]
    def parseAny(input: String): Either[String, Any]
    def violations(input: String): Vector[WireDecodeViolation]
  end Family

  private def family[A](
    familyName: String,
    id: String,
    recordTypeName: String,
    schemaPath: String,
    goldenPath: String,
    invalidPath: String,
    structuralInvalidPath: String
  )(
    read: String => Either[WireViolations[WireDecodeViolation], A],
    write: A => Either[WireViolations[WireEncodeViolation], String]
  ): Family =
    new Family:
      val name: String                      = familyName
      val schemaId: String                  = id
      val recordType: String                = recordTypeName
      val schemaResource: String            = schemaPath
      val goldenResource: String            = goldenPath
      val invalidResource: String           = invalidPath
      val structuralInvalidResource: String = structuralInvalidPath

      def normalize(input: String): Either[String, String] =
        read(input).left.map(_.toString).flatMap(value => write(value).left.map(_.toString))

      def parseAny(input: String): Either[String, Any] = read(input).left.map(_.toString)

      def violations(input: String): Vector[WireDecodeViolation] =
        read(input).left.toOption.fold(Vector.empty)(_.toVector)
    end new
  end family

  private val families: Vector[Family] = Vector(
    family(
      "general-grid-coordinate",
      "urn:trading:codec:schema:general-grid-coordinate:v1",
      GeneralGridCoordinateRecord.recordType.value,
      "trading/codec/schema/general-grid-coordinate-v1.schema.json",
      "trading/codec/golden/general-grid-coordinate-v1.json",
      "trading/codec/invalid/general-grid-coordinate-unknown-version.json",
      "trading/codec/invalid/general-grid-coordinate-missing-payload.json"
    )(GeneralGridCoordinateRecord.parse(_), GeneralGridCoordinateRecord.encode),
    family(
      "asset-grid-coordinate",
      "urn:trading:codec:schema:asset-grid-coordinate:v1",
      AssetGridCoordinateRecord.recordType.value,
      "trading/codec/schema/asset-grid-coordinate-v1.schema.json",
      "trading/codec/golden/asset-grid-coordinate-v1.json",
      "trading/codec/invalid/asset-grid-coordinate-unknown-version.json",
      "trading/codec/invalid/asset-grid-coordinate-missing-payload.json"
    )(AssetGridCoordinateRecord.parse(_), AssetGridCoordinateRecord.encode),
    family(
      "catalog-journal-entry",
      "urn:trading:codec:schema:catalog-journal-entry:v1",
      CatalogJournalEntry.recordType.value,
      "trading/codec/schema/catalog-journal-entry-v1.schema.json",
      "trading/codec/golden/catalog-journal-history-v1.jsonl",
      "trading/codec/invalid/catalog-journal-entry-unknown-version.json",
      "trading/codec/invalid/catalog-journal-entry-missing-payload.json"
    )(CatalogJournalEntry.parse(_), CatalogJournalEntry.encode),
    family(
      "instrument-definition",
      "urn:trading:codec:schema:instrument-definition:v1",
      InstrumentDefinitionRecord.recordType.value,
      "trading/codec/schema/instrument-definition-v1.schema.json",
      "trading/codec/golden/instrument-definition-v1.json",
      "trading/codec/invalid/instrument-definition-unknown-version.json",
      "trading/codec/invalid/instrument-definition-missing-payload.json"
    )(InstrumentDefinitionRecord.parse(_), InstrumentDefinitionRecord.encode),
    family(
      "order",
      "urn:trading:codec:schema:order:v1",
      OrderRecord.recordType.value,
      "trading/codec/schema/order-v1.schema.json",
      "trading/codec/golden/orders-v1.jsonl",
      "trading/codec/invalid/order-unknown-version.json",
      "trading/codec/invalid/order-missing-payload.json"
    )(OrderRecord.parse(_), OrderRecord.encode),
    family(
      "order-scenario",
      "urn:trading:codec:schema:order-scenario:v1",
      OrderScenarioRecord.recordType.value,
      "trading/codec/schema/order-scenario-v1.schema.json",
      "trading/codec/golden/order-scenarios-v1.jsonl",
      "trading/codec/invalid/order-scenario-unknown-version.json",
      "trading/codec/invalid/order-scenario-missing-payload.json"
    )(OrderScenarioRecord.parse(_), OrderScenarioRecord.encode),
    family(
      "round-trip-scenario",
      "urn:trading:codec:schema:round-trip-scenario:v1",
      RoundTripScenarioRecord.recordType.value,
      "trading/codec/schema/round-trip-scenario-v1.schema.json",
      "trading/codec/golden/round-trip-scenario-v1.json",
      "trading/codec/invalid/round-trip-scenario-unknown-version.json",
      "trading/codec/invalid/round-trip-scenario-missing-payload.json"
    )(RoundTripScenarioRecord.parse(_), RoundTripScenarioRecord.encode)
  )

  test("checked-in schemas and fixtures regenerate byte-for-byte"):
    BoundaryCodecCompatibilityResources.all.foreach: expected =>
      assertEquals(resource(expected.classpathPath), expected.content, expected.repositoryPath)

  test("every V1 schema validates against Draft 2020-12 and independently accepts and rejects offline fixtures"):
    families.foreach: value =>
      val schemaText = resource(value.schemaResource)
      val registry   = SchemaRegistry.withDefaultDialect(
        SpecificationVersion.DRAFT_2020_12,
        builder =>
          val _ = builder.schemas(Map(value.schemaId -> schemaText).asJava)
          val _ = builder.schemaLoader: loader =>
            val _ = loader.fetchRemoteResources(false)
            ()
      )
      val schema = registry.getSchema(SchemaLocation.of(value.schemaId))
      val meta   = registry.getSchema(
        SchemaLocation.of(SpecificationVersion.DRAFT_2020_12.getDialectId())
      )
      val valid   = goldenLines(value)
      val invalid = Vector(value.invalidResource, value.structuralInvalidResource)
        .map(path => resource(path).trim)

      val metaViolations = meta.validate(schemaText, InputFormat.JSON).asScala
      assert(metaViolations.isEmpty, s"${value.name}: ${metaViolations.mkString("\n")}")
      valid.foreach(wire => assert(schema.validate(wire, InputFormat.JSON).isEmpty, value.name))
      invalid.foreach: wire =>
        assert(schema.validate(wire, InputFormat.JSON).asScala.nonEmpty, value.name)
        assert(value.normalize(wire).isLeft, value.name)
      assert(!schemaText.matches("(?s).*\"\\$ref\"\\s*:\\s*\"(?!#).*"), value.name)
  test("canonical goldens cover every family and tagged alternative and agree with the independent JCS oracle"):
    val all = families.flatMap: value =>
      goldenLines(value).map(value -> _)
    all.foreach: (value, wire) =>
      assertEquals(value.normalize(wire), Right(wire), value.name)
      assertEquals(new JsonCanonicalizer(wire).getEncodedString(), wire, value.name)
      assertEquals(new String(wire.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), wire, value.name)
      assertEquals(value.normalize(noncanonical(wire, value.recordType)), Right(wire), value.name)

    val orderText    = resource("trading/codec/golden/orders-v1.jsonl")
    val scenarioText = resource("trading/codec/golden/order-scenarios-v1.jsonl")
    Vector("immediate", "fixed", "trailing", "market", "priced", "limit", "pegged", "displayed", "hidden", "iceberg")
      .foreach(tag => assert(orderText.contains(s"\"kind\":\"$tag\""), tag))
    Vector("immediate", "fixed", "trailing", "direct", "pegged", "maker", "taker")
      .foreach(tag => assert(scenarioText.contains(s"\"$tag\""), tag))
    val journalText = resource("trading/codec/golden/catalog-journal-history-v1.jsonl")
    Vector("register-dimension", "register-asset", "register-grid")
      .foreach(tag => assert(journalText.contains(s"\"kind\":\"$tag\""), tag))

  test("unknown versions fail explicitly and checked-in V1 writers never negotiate another version"):
    families.foreach: value =>
      val invalid = resource(value.invalidResource).trim
      assert(
        value.violations(invalid).exists:
          case WireDecodeViolation.Envelope(
              _,
              EnvelopeProblem.UnsupportedSchemaVersion(recordType, supplied),
              0
            ) => recordType.value == value.recordType && supplied.value == 2
          case _ => false,
        value.name
      )
      goldenLines(value).foreach: wire =>
        assert(wire.endsWith("\"schemaVersion\":1}"), s"${value.name}: $wire")

  test("coherent scenario and round-trip goldens reconstruct with identical retained meaning"):
    val fixture         = OrderRecordTestFixture("compatibility")
    val scenarioRecords = goldenLines(families.find(_.name == "order-scenario").get).map: wire =>
      OrderScenarioRecord.parse(wire).toOption.get
    scenarioRecords.foreach: record =>
      val rebuilt = OrderScenarioRecord
        .reconstruct(record, fixture.instrument, fixture.snapshot)
        .fold(error => fail(s"scenario golden did not reconstruct: $error"), identity)
      assertEquals(OrderScenarioRecord.fromScenario(fixture.instrument)(rebuilt), record)

    val roundTrip = RoundTripScenarioRecord
      .parse(goldenLines(families.find(_.name == "round-trip-scenario").get).head)
      .toOption
      .get
    val rebuilt = RoundTripScenarioRecord
      .reconstruct(roundTrip, fixture.instrument, fixture.snapshot)
      .fold(error => fail(s"round-trip golden did not reconstruct: $error"), identity)
    assertEquals(RoundTripScenarioRecord.fromScenario(fixture.instrument)(rebuilt), roundTrip)

  test("every reachable codec-owned record and decoded dependent package rejects Java serialization"):
    families.foreach: value =>
      goldenLines(value).foreach: wire =>
        val parsed     = value.parseAny(wire).fold(error => fail(s"${value.name}: $error"), identity)
        val codecOwned = collectCodecOwned(parsed)
        assert(codecOwned.nonEmpty, value.name)
        codecOwned.foreach:
          case unsupported: JavaSerializationUnsupported => rejectSerialization(unsupported)
          case other => fail(s"${value.name}: ${other.getClass.getName} does not reject Java serialization")

    val general = GeneralGridCoordinateRecord
      .parse(goldenLines(families.find(_.name == "general-grid-coordinate").get).head)
      .toOption
      .get
    val asset = AssetGridCoordinateRecord
      .parse(goldenLines(families.find(_.name == "asset-grid-coordinate").get).head)
      .toOption
      .get
    val assetAtom = asset.gridIdentity.dimension.powers.head._1
    val snapshot  = CatalogModel
      .commit(
        CatalogRoot.create().initialState,
        CatalogBatch.of(
          CatalogCommand.RegisterDimension(general.gridIdentity.dimension),
          CatalogCommand.RegisterAsset(AssetDefinition(asset.assetId, assetAtom)),
          CatalogCommand.RegisterGrid(GridDefinition(general.gridIdentity, required(PositiveRational(Rational.one)))),
          CatalogCommand.RegisterGrid(GridDefinition(asset.gridIdentity, required(PositiveRational(Rational.one))))
        )
      )
      .toOption
      .get
      .state
      .snapshot
    rejectSerialization(GeneralGridCoordinateRecord.reconstruct(general, snapshot).toOption.get)
    rejectSerialization(AssetGridCoordinateRecord.reconstruct(asset, snapshot).toOption.get)

    val journal = goldenLines(families.find(_.name == "catalog-journal-entry").get)
      .map(value => CatalogJournalEntry.parse(value).toOption.get)
    val replay = CatalogReplay.rebuild(CatalogRoot.create().initialState, journal).toOption.get
    rejectSerialization(replay)

  private def goldenLines(value: Family): Vector[String] =
    resource(value.goldenResource).linesIterator.filter(_.nonEmpty).toVector

  private def resource(path: String): String =
    val stream = Option(getClass.getClassLoader.getResourceAsStream(path)).getOrElse:
      throw new IllegalStateException(s"missing compatibility resource: $path")
    Using.resource(stream)(input => new String(input.readAllBytes(), StandardCharsets.UTF_8))

  private def noncanonical(canonical: String, recordType: String): String =
    val prefix = "{\"payload\":"
    val marker = canonical.lastIndexOf(",\"recordType\":")
    require(canonical.startsWith(prefix) && marker > prefix.length, canonical)
    val payload = canonical.substring(prefix.length, marker)
    s"""{
       |  "schemaVersion" : 1,
       |  "recordType" : "$recordType",
       |  "payload" : $payload
       |} """.stripMargin

  private def collectCodecOwned(value: Any): Vector[AnyRef] =
    val current = value match
      case reference: AnyRef if reference.getClass.getName.startsWith("trading.codec.") => Vector(reference)
      case _                                                                            => Vector.empty
    val nested = value match
      case product: Product    => product.productIterator.toVector.flatMap(collectCodecOwned)
      case values: Iterable[?] => values.iterator.toVector.flatMap(collectCodecOwned)
      case values: Array[?]    => values.toVector.flatMap(collectCodecOwned)
      case _                   => Vector.empty
    current ++ nested

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = ByteArrayOutputStream()
    val output = ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  private def required[E, A](value: Either[E, A]): A =
    value.fold(error => throw new IllegalArgumentException(error.toString), identity)
end BoundaryCodecCompatibilitySuite
