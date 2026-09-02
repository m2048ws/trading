package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentId
import trading.economics.instrument.UnderlyingId
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.reference.AssetId
import trading.reference.GridId
import trading.reference.GridIdentity
import trading.reference.GridKey
import trading.reference.GridVersion

class ExactPrimitivesSuite extends ScalaCheckSuite:
  test("canonical integer strings preserve arbitrary precision and reject aliases"):
    val huge = "9" * 200
    Vector("0", "1", "-1", huge, s"-$huge").foreach: spelling =>
      val decoded = ExactWire.canonicalInteger.read(s"\"$spelling\"")
      assertEquals(decoded, Right(BigInt(spelling)))
      assertEquals(ExactWire.canonicalInteger.write(BigInt(spelling)), Right(s"\"$spelling\""))

    Vector("", "00", "-0", "+1", " 1", "1 ", "1.0", "1e2", "0x10", "NaN").foreach: spelling =>
      ExactWire.canonicalInteger.read(s"\"$spelling\"").left.toOption.get.head match
        case WireDecodeViolation.ExactNumber(_, ExactNumberProblem.NonCanonicalInteger(`spelling`), 0) => ()
        case other => fail(s"$spelling returned $other")

  test("integer digit limits are checked before arbitrary-precision construction"):
    val selected = limits(stringCharacters = 20, integerDigits = 4)
    val error    = ExactWire.canonicalInteger.read("\"12345\"", selected).left.toOption.get.head
    assertEquals(
      error,
      WireDecodeViolation.Limit(
        WireLimitViolation(DecodeLimit.IntegerDigits, 5L, 4, WirePath.root, 0)
      )
    )

  test("positive integer strings reject canonical zero and negative values"):
    assertEquals(ExactWire.positiveInteger.read("\"1\""), Right(BigInt(1)))
    Vector("0", "-1").foreach: spelling =>
      ExactWire.positiveInteger.read(s"\"$spelling\"").left.toOption.get.head match
        case WireDecodeViolation.ExactNumber(_, ExactNumberProblem.NonPositiveInteger(`spelling`), 0) => ()
        case other => fail(s"$spelling returned $other")

  property("reduced rational records round-trip exactly"):
    forAll: (numerator: Int, denominatorSeed: Int) =>
      val denominator = BigInt(denominatorSeed).abs + 1
      val value       = Rational(BigInt(numerator), denominator)
      ExactWire.rational.write(value) match
        case Right(encoded) => ExactWire.rational.read(encoded) == Right(value)
        case Left(_)        => false

  test("rational decoding rejects non-reduced, alternate-zero, signed-denominator, and floating forms"):
    val nonReduced = ExactWire.rational
      .read("""{"numerator":"2","denominator":"4"}""")
      .left
      .toOption
      .get
      .head
    assertEquals(
      nonReduced,
      WireDecodeViolation.ExactNumber(
        WirePath.root,
        ExactNumberProblem.NonReducedRational("2", "4"),
        0
      )
    )

    val alternateZero = ExactWire.rational
      .read("""{"numerator":"0","denominator":"2"}""")
      .left
      .toOption
      .get
      .head
    assertEquals(
      alternateZero,
      WireDecodeViolation.ExactNumber(WirePath.root, ExactNumberProblem.NonCanonicalZero("2"), 0)
    )

    Vector(
      """{"numerator":"1","denominator":"0"}""",
      """{"numerator":"1","denominator":"-2"}"""
    ).foreach: input =>
      assert(
        ExactWire.rational
          .read(input)
          .left
          .toOption
          .get
          .head
          .isInstanceOf[WireDecodeViolation.ExactNumber]
      )
    assert(
      ExactWire.rational
        .read("""{"numerator":1.0,"denominator":"1"}""")
        .left
        .toOption
        .get
        .head
        .isInstanceOf[WireDecodeViolation.ExpectedType]
    )

  test("stable identifier codecs invoke owners and preserve exact Unicode, spacing, normalization, and case"):
    val asset      = AssetId.from(" Ässet ").toOption.get
    val grid       = GridId.from("Grid-A").toOption.get
    val instrument = InstrumentId.from("contract-α").toOption.get
    val underlying = UnderlyingId.from("Underlying").toOption.get
    val composed   = "é"
    val decomposed = "e\u0301"

    assertEquals(ExactWire.assetId.read("\" Ässet \""), Right(asset))
    assertEquals(ExactWire.assetId.write(asset), Right("\" Ässet \""))
    assertEquals(ExactWire.gridId.read("\"Grid-A\""), Right(grid))
    assertEquals(ExactWire.instrumentId.read("\"contract-α\""), Right(instrument))
    assertEquals(ExactWire.underlyingId.read("\"Underlying\""), Right(underlying))
    assertEquals(ExactWire.atomId.read(s"\"$composed\""), Right(AtomId(composed)))
    assertEquals(ExactWire.atomId.read(s"\"$decomposed\""), Right(AtomId(decomposed)))
    assertNotEquals(AtomId(composed), AtomId(decomposed))
    assertNotEquals(InstrumentId.from("Underlying").toOption.get, InstrumentId.from("underlying").toOption.get)

    ExactWire.assetId.read("\"   \"").left.toOption.get.head match
      case WireDecodeViolation.InvalidStableIdentifier(_, StableIdentifierKind.Asset,
          StableIdentifierProblem.Empty, "   ", 0) => ()
      case other => fail(s"unexpected identifier failure: $other")

    assert(
      ExactWire.atomId.write(AtomId("\uD800")).left.toOption.get.head
        .isInstanceOf[WireEncodeViolation.MalformedUnicode]
    )
    assert(
      ExactWire.atomId.read("\"\\uD800\"").left.toOption.get.head
        .isInstanceOf[WireDecodeViolation.MalformedUnicode]
    )

  test("dimension codecs preserve canonical compound powers and reject every indexed normalization alias"):
    val huge      = BigInt(2).pow(100)
    val dimension = DimKey(Vector(AtomId("base") -> -huge, AtomId("quote") -> BigInt(3)))
    val encoded   = ExactWire.dimension.write(dimension).toOption.get

    assertEquals(ExactWire.dimension.read(encoded), Right(dimension))
    assertEquals(ExactWire.dimension.read("[]"), Right(DimKey.one))

    val malformed =
      """[{"atom":"b","power":"0"},{"atom":"a","power":"1"},{"atom":"a","power":"2"}]"""
    val errors = ExactWire.dimension.read(malformed).left.toOption.get.toVector
    assertEquals(errors.map(_.path.render), Vector("$[0].power", "$[1].atom", "$[2].atom"))
    assertEquals(
      errors.collect { case WireDecodeViolation.InvalidDimension(_, problem, _) => problem },
      Vector(
        DimensionProblem.ZeroPower("b"),
        DimensionProblem.AtomOutOfOrder("b", "a"),
        DimensionProblem.DuplicateAtom("a")
      )
    )

  test("dimension factor order follows authoritative UTF-16 code units rather than Unicode code points"):
    val supplementary = "\uD800\uDC00"
    val privateUse    = "\uE000"
    val canonical     =
      s"""[{"atom":"$supplementary","power":"1"},{"atom":"$privateUse","power":"1"}]"""
    val reversed =
      s"""[{"atom":"$privateUse","power":"1"},{"atom":"$supplementary","power":"1"}]"""

    assertEquals(
      ExactWire.dimension.read(canonical),
      Right(DimKey(Vector(AtomId(supplementary) -> BigInt(1), AtomId(privateUse) -> BigInt(1))))
    )
    ExactWire.dimension.read(reversed).left.toOption.get.head match
      case WireDecodeViolation.InvalidDimension(
          _,
          DimensionProblem.AtomOutOfOrder(`privateUse`, `supplementary`),
          0
        ) => ()
      case other => fail(s"unexpected UTF-16 ordering failure: $other")

  test("full grid identity keeps exact dimension, ID, and string grid version distinct from schema versions"):
    val identity = GridIdentity(
      DimKey.atom(AtomId("usd")),
      GridKey(GridId.from("price").toOption.get, GridVersion.from(7).toOption.get)
    )
    val encoded = ExactWire.gridIdentity.write(identity).toOption.get

    assertEquals(
      encoded,
      """{"dimension":[{"atom":"usd","power":"1"}],"gridId":"price","gridVersion":"7"}"""
    )
    assertEquals(ExactWire.gridIdentity.read(encoded), Right(identity))

    val tooLarge = (BigInt(Long.MaxValue) + 1).toString
    ExactWire.gridIdentity
      .read(s"""{"dimension":[],"gridId":"price","gridVersion":"$tooLarge"}""")
      .left
      .toOption
      .get
      .head match
      case WireDecodeViolation.ExactNumber(_, ExactNumberProblem.OutsideTargetRange("GridVersion", `tooLarge`), 0) =>
        ()
      case other => fail(s"unexpected grid version failure: $other")

  private def limits(stringCharacters: Int, integerDigits: Int): DecodeLimits =
    DecodeLimits
      .create(
        maxPayloadCharacters = 1_000,
        maxPayloadUtf8Bytes = 4_000,
        maxNestingDepth = 16,
        maxBatchRecords = 100,
        maxObjectMembers = 100,
        maxArrayEntries = 100,
        maxStringCharacters = stringCharacters,
        maxIntegerDigits = integerDigits,
        maxDimensionFactors = 100,
        maxCatalogCommands = 100,
        maxScenarioSlices = 100,
        maxMarketConversions = 100
      )
      .toOption
      .get
end ExactPrimitivesSuite
