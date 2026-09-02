package trading.codec

private[codec] object CanonicalJson:
  private val CanonicalInteger = "(?:0|-[1-9][0-9]*|[1-9][0-9]*)".r

  def render(node: JsonNode): Either[WireViolations[WireEncodeViolation], String] =
    val rendered = renderAt(node, WirePath.root)
    WireViolations.fromVector(rendered.errors) match
      case Some(errors) => Left(WireViolations.orderedEncode(errors.toVector))
      case None         => Right(rendered.value)

  private final case class Rendered(errors: Vector[WireEncodeViolation], value: String)

  private def renderAt(node: JsonNode, path: WirePath): Rendered =
    node.value match
      case JsonValue.JObject(fields) => renderObject(fields, path)
      case JsonValue.JArray(values)  =>
        combine(values.zipWithIndex.map((value, index) => renderAt(value, path.index(index))), "[", ",", "]")
      case JsonValue.JString(value) =>
        Unicode.malformedIndex(value) match
          case Some(index) => Rendered(Vector(WireEncodeViolation.MalformedUnicode(path, index)), "")
          case None        => Rendered(Vector.empty, quote(value))
      case JsonValue.JNumber(raw) =>
        raw match
          case CanonicalInteger() => Rendered(Vector.empty, raw)
          case _                  => Rendered(Vector(WireEncodeViolation.UnsupportedNumber(path, raw)), "")
      case JsonValue.JBoolean(value) => Rendered(Vector.empty, value.toString)
      case JsonValue.JNull           => Rendered(Vector.empty, "null")

  private def renderObject(fields: Vector[JsonField], path: WirePath): Rendered =
    val duplicates =
      fields
        .groupBy(_.name)
        .collect:
          case (name, occurrences) if occurrences.size > 1 =>
            WireEncodeViolation.DuplicateMember(path.field(name), name)
        .toVector
    val rendered = fields.map: field =>
      val fieldPath  = path.field(field.name)
      val nameErrors = Unicode.malformedIndex(field.name).toVector.map: index =>
        WireEncodeViolation.MalformedUnicode(fieldPath, index)
      val value = renderAt(field.value, fieldPath)
      (field.name, Rendered(nameErrors ++ value.errors, s"${quote(field.name)}:${value.value}"))
    val ordered  = rendered.sortWith((left, right) => left._1.compareTo(right._1) < 0).map(_._2)
    val combined = combine(ordered, "{", ",", "}")
    combined.copy(errors = duplicates ++ combined.errors)

  private def combine(values: Vector[Rendered], prefix: String, separator: String, suffix: String): Rendered =
    Rendered(values.flatMap(_.errors), values.map(_.value).mkString(prefix, separator, suffix))

  private def quote(value: String): String =
    val builder = new StringBuilder(value.length + 2)
    builder.append('"')
    var index = 0
    while index < value.length do
      value.charAt(index) match
        case '"'                            => builder.append("\\\"")
        case '\\'                           => builder.append("\\\\")
        case '\b'                           => builder.append("\\b")
        case '\t'                           => builder.append("\\t")
        case '\n'                           => builder.append("\\n")
        case '\f'                           => builder.append("\\f")
        case '\r'                           => builder.append("\\r")
        case character if character <= 0x1f =>
          builder.append(f"\\u${character.toInt}%04x")
        case character => builder.append(character)
      index += 1
    builder.append('"')
    builder.result()
end CanonicalJson
