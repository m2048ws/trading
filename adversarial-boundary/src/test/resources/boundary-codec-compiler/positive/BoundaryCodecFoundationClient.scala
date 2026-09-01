package external.codec.positive

import trading.codec.*

object BoundaryCodecFoundationClient:
  val defaults: DecodeLimits = DecodeLimits.default
  val configured: Either[WireViolations[DecodeLimitConfigurationViolation], DecodeLimits] =
    DecodeLimits.create(
      maxPayloadCharacters = 100,
      maxPayloadUtf8Bytes = 400,
      maxNestingDepth = 8,
      maxBatchRecords = 10,
      maxObjectMembers = 10,
      maxArrayEntries = 10,
      maxStringCharacters = 10,
      maxIntegerDigits = 10,
      maxDimensionFactors = 10,
      maxCatalogCommands = 10,
      maxScenarioSlices = 10,
      maxMarketConversions = 10
    )
  val root: WirePath = WirePath.root
  val syntaxRank: Int = WireStage.Syntax.rank
