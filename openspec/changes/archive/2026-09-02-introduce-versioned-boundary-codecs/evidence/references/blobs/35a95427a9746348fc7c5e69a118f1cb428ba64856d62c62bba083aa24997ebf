package trading.codec

import scala.util.Try

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.quantity.DimKey
import trading.reference.GridId
import trading.reference.GridIdentity
import trading.reference.GridKey
import trading.reference.GridVersion

final class BoundaryCodecRobustnessPropertiesSuite extends ScalaCheckSuite:
  private type ParseResult  = Either[WireViolations[WireDecodeViolation], Any]
  private type FamilyParser = (String, DecodeLimits) => ParseResult

  private val familyParsers: Vector[(String, FamilyParser)] = Vector(
    "general-grid-coordinate" -> ((input, limits) => GeneralGridCoordinateRecord.parse(input, limits).map(identity)),
    "asset-grid-coordinate"   -> ((input, limits) => AssetGridCoordinateRecord.parse(input, limits).map(identity)),
    "catalog-journal"         -> ((input, limits) => CatalogJournalEntry.parse(input, limits).map(identity)),
    "instrument-definition"   -> ((input, limits) => InstrumentDefinitionRecord.parse(input, limits).map(identity)),
    "order"                   -> ((input, limits) => OrderRecord.parse(input, limits).map(identity)),
    "order-scenario"          -> ((input, limits) => OrderScenarioRecord.parse(input, limits).map(identity)),
    "round-trip-scenario"     -> ((input, limits) => RoundTripScenarioRecord.parse(input, limits).map(identity))
  )

  private val generalRecord = GeneralGridCoordinateRecord.V1(
    GridIdentity(
      DimKey.one,
      GridKey(GridId.from("robust-grid").toOption.get, GridVersion.from(1).toOption.get)
    ),
    BigInt(42)
  )
  private val generalWire = GeneralGridCoordinateRecord.encode(generalRecord).toOption.get

  property("arbitrary bounded Unicode inputs produce total typed outcomes for every published family"):
    forAll: (input: String) =>
      familyParsers.forall: (_, parse) =>
        Try(parse(input, DecodeLimits.default)).isSuccess

  property("single-code-unit mutations of a valid envelope never escape the typed boundary"):
    val mutation =
      for
        seed        <- Gen.choose(Int.MinValue, Int.MaxValue)
        replacement <- Gen.oneOf('{', '}', '[', ']', '"', '\\', '\u0000', '\uD800', '\uDC00', '\u20ac')
      yield seed -> replacement

    forAll(mutation): (seed, replacement) =>
      val index   = Math.floorMod(seed, generalWire.length)
      val mutated = generalWire.substring(0, index) + replacement + generalWire.substring(index + 1)
      familyParsers.forall: (_, parse) =>
        Try(parse(mutated, DecodeLimits.default)).isSuccess

  test("every published family rejects the payload limit before malformed syntax"):
    val selected = limits(
      payloadCharacters = 16,
      payloadBytes = 64,
      depth = 8,
      batch = 4,
      objectMembers = 8,
      arrayEntries = 8,
      stringCharacters = 16,
      integerDigits = 8,
      dimensionFactors = 4,
      catalogCommands = 4,
      scenarioSlices = 4,
      marketConversions = 4
    )
    val oversizedMalformed = "x" * 17

    familyParsers.foreach: (name, parse) =>
      parse(oversizedMalformed, selected) match
        case Left(errors) =>
          errors.head match
            case WireDecodeViolation.Limit(value) =>
              assertEquals(value.limit, DecodeLimit.PayloadCharacters, name)
              assertEquals(value.actual, 17L, name)
            case other => fail(s"$name reached syntax before its payload limit: $other")
        case Right(value) => fail(s"$name unexpectedly decoded oversized malformed input: $value")

  test("a controlled larger profile preserves exact records while a smaller reader rejects explicitly"):
    val exact = generalRecord.copy(coordinate = BigInt("123456789012345678901234567890"))
    val wire  = GeneralGridCoordinateRecord.encode(exact).toOption.get
    val small = limits(integerDigits = 16)
    val large = limits()

    GeneralGridCoordinateRecord.parse(wire, small).left.toOption.get.head match
      case WireDecodeViolation.Limit(value) =>
        assertEquals(value.limit, DecodeLimit.IntegerDigits)
        assertEquals(value.path.render, "$.payload.coordinate")
      case other => fail(s"expected explicit reader-profile rejection, got $other")

    val decoded = GeneralGridCoordinateRecord.parse(wire, large).toOption.get
    assertEquals(decoded, exact)
    assertEquals(GeneralGridCoordinateRecord.encode(decoded), Right(wire))
    val schema = GeneralGridCoordinateRecord.schema().toOption.get
    assert(!schema.contains("maxLength"))
    assert(!schema.contains("maxItems"))

  test("deep hostile nesting is bounded and a controlled larger depth uses no JVM recursion"):
    val depth = 2_048
    val input = "[" * depth + "0" + "]" * depth

    StrictJson.parse(input).left.toOption.get.head match
      case WireDecodeViolation.Limit(value) =>
        assertEquals(value.limit, DecodeLimit.NestingDepth)
        assertEquals(value.actual, DecodeLimits.default.maxNestingDepth.toLong + 1L)
      case other => fail(s"expected a named nesting limit, got $other")

    val larger = limits(
      payloadCharacters = 10_000,
      payloadBytes = 40_000,
      depth = depth,
      batch = 4,
      objectMembers = 8,
      arrayEntries = 8,
      stringCharacters = 32,
      integerDigits = 32,
      dimensionFactors = 4,
      catalogCommands = 4,
      scenarioSlices = 4,
      marketConversions = 4
    )
    assert(StrictJson.parse(input, larger).isRight)

  test("syntax objects arrays sums Unicode exact strings and mixed independent failures stay typed and ordered"):
    val duplicate =
      """{"payload":{},"payload":{},"recordType":"trading.general-grid-coordinate","schemaVersion":1}"""
    assert(head(GeneralGridCoordinateRecord.parse("{")).isInstanceOf[WireDecodeViolation.Syntax])
    assert(head(GeneralGridCoordinateRecord.parse(duplicate)).isInstanceOf[WireDecodeViolation.Syntax])
    assert(
      head(GeneralGridCoordinateRecord.parse(generalWire.dropRight(1) + ",\"extra\":true}"))
        .isInstanceOf[WireDecodeViolation.UnknownField]
    )
    assert(
      head(GeneralGridCoordinateRecord.parse(generalWire.replace("\"coordinate\":\"42\"", "\"coordinate\":null")))
        .isInstanceOf[WireDecodeViolation.NullRequired]
    )
    assert(
      head(GeneralGridCoordinateRecord.parse(generalWire.replace("robust-grid", "robust-\uD800-grid")))
        .isInstanceOf[WireDecodeViolation.MalformedUnicode]
    )
    assert(
      head(GeneralGridCoordinateRecord.parse(generalWire.replace("\"coordinate\":\"42\"", "\"coordinate\":\"042\"")))
        .isInstanceOf[WireDecodeViolation.ExactNumber]
    )
    assert(
      head(GeneralGridCoordinateRecord.parse(generalWire.replace("\"dimension\":[]", "\"dimension\":[null]")))
        .isInstanceOf[WireDecodeViolation.NullRequired]
    )

    val unknownSum =
      """{"payload":{"activation":{"kind":"surprise"},"execution":{"kind":"market","timeInForce":"immediateOrCancel"},"instrumentId":"robust-order","lotCoordinate":"1","positionEffect":"unrestricted","side":"buy"},"recordType":"trading.order","schemaVersion":1}"""
    assert(head(OrderRecord.parse(unknownSum)).isInstanceOf[WireDecodeViolation.UnknownAlternative])

    val mixed =
      """{"payload":{"coordinate":"01","gridIdentity":{"dimension":[{"atom":"d","power":"0"}],"gridId":"","gridVersion":"0"}},"recordType":"trading.general-grid-coordinate","schemaVersion":1}"""
    val failures = GeneralGridCoordinateRecord.parse(mixed, recordIndex = 13).left.toOption.get.toVector
    assertEquals(failures.size, 4)
    assertEquals(failures.map(_.recordIndex).distinct, Vector(13))
    assertEquals(failures.map(_.path.render), failures.map(_.path.render).sorted)

  private def head[A](result: Either[WireViolations[WireDecodeViolation], A]): WireDecodeViolation =
    result.left.toOption.get.head

  private def limits(
    payloadCharacters: Int = 10_000,
    payloadBytes: Int = 40_000,
    depth: Int = 32,
    batch: Int = 128,
    objectMembers: Int = 128,
    arrayEntries: Int = 128,
    stringCharacters: Int = 128,
    integerDigits: Int = 64,
    dimensionFactors: Int = 64,
    catalogCommands: Int = 128,
    scenarioSlices: Int = 128,
    marketConversions: Int = 128
  ): DecodeLimits =
    DecodeLimits
      .create(
        payloadCharacters,
        payloadBytes,
        depth,
        batch,
        objectMembers,
        arrayEntries,
        stringCharacters,
        integerDigits,
        dimensionFactors,
        catalogCommands,
        scenarioSlices,
        marketConversions
      )
      .toOption
      .get
end BoundaryCodecRobustnessPropertiesSuite
