package trading.codec

import munit.FunSuite

class DecodeLimitsSuite extends FunSuite:
  test("safe defaults match the documented operational profile"):
    val limits = DecodeLimits.default
    assertEquals(limits.maxPayloadCharacters, 1_000_000)
    assertEquals(limits.maxPayloadUtf8Bytes, 4_000_000)
    assertEquals(limits.maxNestingDepth, 32)
    assertEquals(limits.maxBatchRecords, 10_000)
    assertEquals(limits.maxObjectMembers, 128)
    assertEquals(limits.maxArrayEntries, 10_000)
    assertEquals(limits.maxStringCharacters, 4_096)
    assertEquals(limits.maxIntegerDigits, 4_096)
    assertEquals(limits.maxDimensionFactors, 256)
    assertEquals(limits.maxCatalogCommands, 10_000)
    assertEquals(limits.maxScenarioSlices, 10_000)
    assertEquals(limits.maxMarketConversions, 1_024)

  test("construction accumulates every independently invalid nonpositive setting"):
    val errors = create(
      payloadCharacters = 0,
      payloadBytes = -1,
      depth = 0,
      batch = -2,
      members = 0,
      array = -3,
      string = 0,
      digits = -4,
      dimensions = 0,
      commands = -5,
      slices = 0,
      conversions = -6
    ).left.toOption.get.toVector

    assertEquals(errors.size, DecodeLimit.values.size)
    assertEquals(
      errors.collect { case DecodeLimitConfigurationViolation.NonPositive(limit, _) => limit },
      DecodeLimit.values.toVector
    )

  test("construction accumulates dependent containment mistakes after positive checks"):
    val errors = create(
      payloadCharacters = 20,
      payloadBytes = 10,
      depth = 8,
      batch = 11,
      members = 10,
      array = 10,
      string = 8,
      digits = 9,
      dimensions = 12,
      commands = 13,
      slices = 14,
      conversions = 15
    ).left.toOption.get.toVector

    assertEquals(errors.size, 7)
    assert(errors.forall(_.isInstanceOf[DecodeLimitConfigurationViolation.ExceedsContainer]))

  test("a controlled larger coherent profile remains an ordinary immutable policy"):
    val created = create(
      payloadCharacters = 2_000_000,
      payloadBytes = 8_000_000,
      depth = 64,
      batch = 20_000,
      members = 256,
      array = 20_000,
      string = 8_192,
      digits = 8_192,
      dimensions = 512,
      commands = 20_000,
      slices = 20_000,
      conversions = 2_048
    ).toOption.get

    assertEquals(created.maxIntegerDigits, 8_192)
    assertEquals(created.maxNestingDepth, 64)

  private def create(
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
  ): Either[WireViolations[DecodeLimitConfigurationViolation], DecodeLimits] =
    DecodeLimits.create(
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
end DecodeLimitsSuite
