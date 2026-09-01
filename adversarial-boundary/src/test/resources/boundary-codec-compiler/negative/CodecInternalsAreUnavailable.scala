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
  val exactWire = trading.codec.ExactWire
  val envelopeCodec = trading.codec.EnvelopeCodec
  val envelopeHeader: Class[trading.codec.EnvelopeHeader] = classOf[trading.codec.EnvelopeHeader]
  val rationalRecord: Class[trading.codec.CanonicalRationalRecord] =
    classOf[trading.codec.CanonicalRationalRecord]
  // OFFENDING-END
