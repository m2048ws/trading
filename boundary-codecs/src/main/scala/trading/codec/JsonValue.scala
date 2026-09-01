package trading.codec

private[codec] final case class JsonNode(value: JsonValue, location: SyntaxLocation)

private[codec] final case class JsonField(name: String, nameLocation: SyntaxLocation, value: JsonNode)

private[codec] enum JsonValue:
  case JObject(fields: Vector[JsonField])
  case JArray(values: Vector[JsonNode])
  case JString(value: String)
  case JNumber(raw: String)
  case JBoolean(value: Boolean)
  case JNull

  def kind: JsonKind =
    this match
      case JObject(_)  => JsonKind.Object
      case JArray(_)   => JsonKind.Array
      case JString(_)  => JsonKind.String
      case JNumber(_)  => JsonKind.Number
      case JBoolean(_) => JsonKind.Boolean
      case JNull       => JsonKind.Null
end JsonValue

private[codec] object JsonNode:
  def obj(fields: (String, JsonNode)*): JsonNode =
    JsonNode(
      JsonValue.JObject(fields.toVector.map((name, value) => JsonField(name, SyntaxLocation.unknown, value))),
      SyntaxLocation.unknown
    )

  def array(values: JsonNode*): JsonNode =
    JsonNode(JsonValue.JArray(values.toVector), SyntaxLocation.unknown)

  def string(value: String): JsonNode =
    JsonNode(JsonValue.JString(value), SyntaxLocation.unknown)

  def number(raw: String): JsonNode =
    JsonNode(JsonValue.JNumber(raw), SyntaxLocation.unknown)

  def bool(value: Boolean): JsonNode =
    JsonNode(JsonValue.JBoolean(value), SyntaxLocation.unknown)

  val `null`: JsonNode = JsonNode(JsonValue.JNull, SyntaxLocation.unknown)
end JsonNode

private[codec] object Unicode:
  def malformedIndex(value: String): Option[Int] =
    var index = 0
    while index < value.length do
      val current = value.charAt(index)
      if Character.isHighSurrogate(current) then
        if index + 1 >= value.length || !Character.isLowSurrogate(value.charAt(index + 1)) then return Some(index)
        index += 2
      else if Character.isLowSurrogate(current) then return Some(index)
      else index += 1
    None
end Unicode
