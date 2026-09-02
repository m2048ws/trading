package external.codec.negative

import trading.codec.DecodeLimits

object CodecInternalsAreUnavailable:
  val publicLimits: DecodeLimits = DecodeLimits.default

  // OFFENDING-BEGIN
  val parser = trading.codec.StrictJson
  val renderer = trading.codec.CanonicalJson
  val ast = trading.codec.JsonNode
  val schema = trading.codec.WireSchema
  val schemaDocument = trading.codec.JsonSchemaDocument
  val contextType: Class[trading.codec.DecodeContext] = classOf[trading.codec.DecodeContext]
  // OFFENDING-END
