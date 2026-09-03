package trading.codec

import munit.FunSuite

class StrictJsonSuite extends FunSuite:
  test("strict parsing preserves immutable order, raw number spelling, and token locations"):
    val parsed = StrictJson.parse(""" { "second" : 1.2300e+02, "first" : [true, null] } """).toOption.get
    parsed.value match
      case JsonValue.JObject(fields) =>
        assertEquals(fields.map(_.name), Vector("second", "first"))
        assert(fields.forall(_.nameLocation.characterOffset.nonEmpty))
        fields.head.value.value match
          case JsonValue.JNumber(raw) => assertEquals(raw, "1.2300e+02")
          case other                  => fail(s"expected raw number, found $other")
      case other => fail(s"expected object, found $other")

  test("duplicate members fail with their structured field path"):
    val violation = StrictJson.parse("""{"outer":{"value":1,"value":2}}""").left.toOption.get.head
    violation match
      case WireDecodeViolation.Syntax(SyntaxProblem.DuplicateMember("value"), location, path, 0) =>
        assertEquals(path.render, "$.outer.value")
        assert(location.characterOffset.nonEmpty)
      case other => fail(s"unexpected violation: $other")

  test("all permissive non-standard JSON forms remain disabled"):
    val nonStandard = Vector(
      "{/* comment */\"a\":1}",
      "{'a':1}",
      "{a:1}",
      "{\"a\":+1}",
      "{\"a\":01}",
      "{\"a\":1,}",
      "[1,,2]",
      "{\"a\":NaN}"
    )
    nonStandard.foreach: input =>
      val result = StrictJson.parse(input)
      assert(result.isLeft, input)
      assert(result.left.toOption.get.head.stage == WireStage.Syntax, input)

  test("character and UTF-8 limits are checked exactly before parser construction"):
    val characterLimits = limits(
      payloadCharacters = 4,
      payloadBytes = 16,
      depth = 8,
      batch = 4,
      members = 4,
      array = 4,
      string = 4,
      digits = 4,
      dimensions = 4,
      commands = 4,
      slices = 4,
      conversions = 4
    )
    val characterError = StrictJson.parse("[1,2]", characterLimits).left.toOption.get.head
    assertEquals(
      characterError,
      WireDecodeViolation.Limit(
        WireLimitViolation(DecodeLimit.PayloadCharacters, 5L, 4, WirePath.root, 0)
      )
    )

    val byteLimits = limits(
      payloadCharacters = 20,
      payloadBytes = 20,
      depth = 8,
      batch = 20,
      members = 20,
      array = 20,
      string = 20,
      digits = 20,
      dimensions = 20,
      commands = 20,
      slices = 20,
      conversions = 20
    )
    val byteError = StrictJson.parse("\"éééééééééé\"", byteLimits).left.toOption.get.head
    byteError match
      case WireDecodeViolation.Limit(value) =>
        assertEquals(value.limit, DecodeLimit.PayloadUtf8Bytes)
        assertEquals(value.actual, 22L)
      case other => fail(s"unexpected violation: $other")

  test("collection, string, number, and nesting policies return typed limits"):
    val selected = limits(
      payloadCharacters = 100,
      payloadBytes = 400,
      depth = 2,
      members = 2,
      array = 2,
      string = 2,
      digits = 2,
      batch = 2,
      dimensions = 2,
      commands = 2,
      slices = 2,
      conversions = 2
    )
    val cases = Vector(
      "{\"a\":1,\"b\":2,\"c\":3}" -> DecodeLimit.ObjectMembers,
      "[1,2,3]"                   -> DecodeLimit.ArrayEntries,
      "\"abc\""                   -> DecodeLimit.StringCharacters,
      "123"                       -> DecodeLimit.IntegerDigits,
      "[[[]]]"                    -> DecodeLimit.NestingDepth
    )
    cases.foreach: (input, expected) =>
      StrictJson.parse(input, selected).left.toOption.get.head match
        case WireDecodeViolation.Limit(value) => assertEquals(value.limit, expected, input)
        case other                            => fail(s"$input returned $other")

  test("malformed input retains syntax coordinates and never escapes as an expected exception"):
    val corpus = Vector("", "{", "[", "{\"a\":]", "\"\\uD800\"", "true false", "\uD800")
    corpus.foreach: input =>
      val result = StrictJson.parse(input)
      assert(result.isLeft, input)
      result.left.toOption.get.head match
        case WireDecodeViolation.Syntax(_, location, _, _) =>
          if input.nonEmpty then assert(location.line.nonEmpty || location.characterOffset.nonEmpty, input)
        case WireDecodeViolation.MalformedUnicode(_, _, location, _) =>
          assert(location.characterOffset.nonEmpty, input)
        case other => fail(s"$input returned $other")

  private def limits(
    payloadCharacters: Int,
    payloadBytes: Int,
    depth: Int,
    batch: Int,
    members: Int,
    array: Int,
    string: Int,
    digits: Int,
    dimensions: Int,
    commands: Int,
    slices: Int,
    conversions: Int
  ): DecodeLimits =
    DecodeLimits
      .create(
        payloadCharacters,
        payloadBytes,
        depth,
        batch,
        members,
        array,
        string,
        digits,
        dimensions,
        commands,
        slices,
        conversions
      )
      .toOption
      .get
end StrictJsonSuite
