package trading.codec

/** Draft 2020-12 interpreter for the same internal wire-schema shape. */
private[codec] object JsonSchemaDocument:
  val dialect: String = "https://json-schema.org/draft/2020-12/schema"

  def render[A](
    id: String,
    definitionName: String,
    schema: WireSchema[A]
  ): Either[WireViolations[WireEncodeViolation], String] =
    if !validId(id) then
      Left(WireViolations.one(WireEncodeViolation.InvalidSchemaIdentifier(id)))
    else if !validDefinition(definitionName) then
      Left(WireViolations.one(WireEncodeViolation.InvalidSchemaDefinitionName(definitionName)))
    else
      CanonicalJson.render(
        obj(
          "$schema" -> JsonNode.string(dialect),
          "$id"     -> JsonNode.string(id),
          "$ref"    -> JsonNode.string(s"#/$$defs/$definitionName"),
          "$defs"   -> obj(definitionName -> interpret(schema.shape))
        )
      )

  private def interpret(shape: SchemaShape): JsonNode =
    shape match
      case SchemaShape.AnyValue            => obj()
      case SchemaShape.Text                => obj("type" -> JsonNode.string("string"))
      case SchemaShape.TextConstant(value) =>
        obj("type" -> JsonNode.string("string"), "const" -> JsonNode.string(value))
      case SchemaShape.BooleanValue           => obj("type" -> JsonNode.string("boolean"))
      case SchemaShape.IntegerValue           => obj("type" -> JsonNode.string("integer"))
      case SchemaShape.IntegerConstant(value) =>
        obj("type" -> JsonNode.string("integer"), "const" -> JsonNode.number(value.toString))
      case SchemaShape.ArrayOf(element, _) =>
        obj("type" -> JsonNode.string("array"), "items" -> interpret(element))
      case SchemaShape.Record(fields)                 => record(fields)
      case SchemaShape.Tagged(tagField, alternatives) =>
        val cases = alternatives.map: alternative =>
          val tag = SchemaFieldShape(tagField, SchemaShape.Text)
          record(
            tag +: alternative.fields,
            constants = Map(tagField -> alternative.tag)
          )
        obj("oneOf" -> JsonNode(JsonValue.JArray(cases), SyntaxLocation.unknown))

  private def record(
    fields: Vector[SchemaFieldShape],
    constants: Map[String, String] = Map.empty
  ): JsonNode =
    val properties = fields.map: field =>
      val schema = constants.get(field.name) match
        case Some(value) =>
          obj(
            "type"  -> JsonNode.string("string"),
            "const" -> JsonNode.string(value)
          )
        case None => interpret(field.shape)
      field.name -> schema
    obj(
      "type"                 -> JsonNode.string("object"),
      "properties"           -> obj(properties*),
      "required"             -> JsonNode.array(fields.map(field => JsonNode.string(field.name))*),
      "additionalProperties" -> JsonNode.bool(false)
    )

  private def obj(fields: (String, JsonNode)*): JsonNode = JsonNode.obj(fields*)

  private def validId(value: String): Boolean =
    value.matches("urn:trading:codec:schema:[A-Za-z0-9][A-Za-z0-9._:-]*") && Unicode.malformedIndex(value).isEmpty

  private def validDefinition(value: String): Boolean =
    value.matches("[A-Za-z][A-Za-z0-9._-]*")
end JsonSchemaDocument
