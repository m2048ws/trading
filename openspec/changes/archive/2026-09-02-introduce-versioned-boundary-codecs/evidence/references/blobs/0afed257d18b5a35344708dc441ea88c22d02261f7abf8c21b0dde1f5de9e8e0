package trading.codec

import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import cats.syntax.all.*

private[codec] final case class DecodeContext(limits: DecodeLimits, recordIndex: Int, path: WirePath):
  def field(name: String): DecodeContext = copy(path = path.field(name))
  def index(value: Int): DecodeContext   = copy(path = path.index(value))
end DecodeContext

private[codec] final case class DecodeCursor(node: JsonNode, context: DecodeContext)

private[codec] final case class SchemaFieldShape(name: String, shape: SchemaShape)
private[codec] final case class SchemaCaseShape(tag: String, fields: Vector[SchemaFieldShape])

private[codec] enum SchemaShape:
  case Text
  case BooleanValue
  case IntegerValue
  case ArrayOf(element: SchemaShape, limit: DecodeLimit)
  case Record(fields: Vector[SchemaFieldShape])
  case Tagged(tagField: String, alternatives: Vector[SchemaCaseShape])
end SchemaShape

private[codec] type EncodeValidation[A] = ValidatedNec[WireEncodeViolation, A]
private[codec] type DecodeValidation[A] = ValidatedNec[WireDecodeViolation, A]

/** Internal invariant schema with encoding, accumulating decoding, and a shared inspectable shape. */
private[codec] final class WireSchema[A] private[codec] (
  val shape: SchemaShape,
  private val encodeValue: (A, WirePath) => EncodeValidation[JsonNode],
  private val decodeValue: DecodeCursor => DecodeValidation[A]):

  def encode(value: A): Either[WireViolations[WireEncodeViolation], JsonNode] =
    WireSchema.encodeResult(encodeValue(value, WirePath.root))

  def write(value: A): Either[WireViolations[WireEncodeViolation], String] =
    encode(value).flatMap(CanonicalJson.render)

  def decode(
    node: JsonNode,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], A] =
    WireSchema.decodeResult(decodeValue(DecodeCursor(node, DecodeContext(limits, recordIndex, WirePath.root))))

  def read(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], A] =
    StrictJson.parse(input, limits, recordIndex).flatMap(node => decode(node, limits, recordIndex))

  private[codec] def encodeAt(value: A, path: WirePath): EncodeValidation[JsonNode] =
    encodeValue(value, path)

  private[codec] def decodeAt(cursor: DecodeCursor): DecodeValidation[A] =
    decodeValue(cursor)

  def imap[B](forward: A => B)(backward: B => A): WireSchema[B] =
    new WireSchema(
      shape,
      (value, path) => encodeValue(backward(value), path),
      cursor => decodeValue(cursor).map(forward)
    )

  def refine[B](
    forward: (A, DecodeContext) => Either[WireDecodeViolation, B]
  )(
    backward: B => A
  ): WireSchema[B] =
    new WireSchema(
      shape,
      (value, path) => encodeValue(backward(value), path),
      cursor =>
        decodeValue(cursor).andThen: value =>
          forward(value, cursor.context) match
            case Right(refined) => refined.validNec
            case Left(error)    => error.invalidNec
    )
end WireSchema

private[codec] final class WireRecord[A] private[codec] (
  val fields: Vector[SchemaFieldShape],
  private[codec] val encodeFields: (A, WirePath) => EncodeValidation[Vector[JsonField]],
  private[codec] val decodeFields: (Map[String, JsonField], DecodeContext) => DecodeValidation[A]):

  def product[B](other: WireRecord[B]): WireRecord[(A, B)] =
    val duplicate = fields.map(_.name).toSet.intersect(other.fields.map(_.name).toSet)
    if duplicate.nonEmpty then
      throw new IllegalArgumentException(s"duplicate wire record fields: ${duplicate.toVector.sorted.mkString(",")}")
    new WireRecord(
      fields ++ other.fields,
      (value, path) => (encodeFields(value._1, path), other.encodeFields(value._2, path)).mapN(_ ++ _),
      (values, context) => (decodeFields(values, context), other.decodeFields(values, context)).tupled
    )

  def imap[B](forward: A => B)(backward: B => A): WireRecord[B] =
    new WireRecord(
      fields,
      (value, path) => encodeFields(backward(value), path),
      (values, context) => decodeFields(values, context).map(forward)
    )
end WireRecord

private[codec] sealed trait WireCase[A]:
  def tag: String
  def fields: Vector[SchemaFieldShape]
  def encodeSelected(value: A, path: WirePath): Option[EncodeValidation[Vector[JsonField]]]
  def decodeSelected(values: Map[String, JsonField], context: DecodeContext): DecodeValidation[A]
end WireCase

private[codec] object WireCase:
  def apply[A, B](
    caseTag: String,
    record: WireRecord[B]
  )(
    select: A => Option[B]
  )(
    inject: B => A
  ): WireCase[A] =
    new WireCase[A]:
      val tag: String                      = caseTag
      val fields: Vector[SchemaFieldShape] = record.fields

      def encodeSelected(value: A, path: WirePath): Option[EncodeValidation[Vector[JsonField]]] =
        select(value).map(record.encodeFields(_, path))

      def decodeSelected(values: Map[String, JsonField], context: DecodeContext): DecodeValidation[A] =
        record.decodeFields(values, context).map(inject)
end WireCase

private[codec] object WireRecord:
  val unit: WireRecord[Unit] =
    new WireRecord(Vector.empty, (_, _) => Vector.empty.validNec, (_, _) => ().validNec)

  def field[A](name: String, schema: WireSchema[A]): WireRecord[A] =
    new WireRecord(
      Vector(SchemaFieldShape(name, schema.shape)),
      (value, path) =>
        schema.encodeAt(value, path.field(name)).map(node => Vector(JsonField(name, SyntaxLocation.unknown, node))),
      (values, context) =>
        values.get(name) match
          case Some(field) => schema.decodeAt(DecodeCursor(field.value, context.field(name)))
          case None => WireDecodeViolation.MissingField(context.path.field(name), name, context.recordIndex).invalidNec
    )
end WireRecord

private[codec] object WireSchema:
  val text: WireSchema[String] =
    new WireSchema(
      SchemaShape.Text,
      (value, path) =>
        Unicode.malformedIndex(value) match
          case Some(index) => WireEncodeViolation.MalformedUnicode(path, index).invalidNec
          case None        => JsonNode.string(value).validNec,
      cursor =>
        cursor.node.value match
          case JsonValue.JString(value) =>
            Unicode.malformedIndex(value) match
              case Some(index) =>
                WireDecodeViolation
                  .MalformedUnicode(cursor.context.path, index, cursor.node.location, cursor.context.recordIndex)
                  .invalidNec
              case None =>
                val characters = value.codePointCount(0, value.length)
                if characters > cursor.context.limits.maxStringCharacters then
                  limit(
                    DecodeLimit.StringCharacters,
                    characters.toLong,
                    cursor.context.limits.maxStringCharacters,
                    cursor.context
                  ).invalidNec
                else value.validNec
          case JsonValue.JNull =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.String, actual.kind, cursor.context).invalidNec
    )

  val boolean: WireSchema[Boolean] =
    new WireSchema(
      SchemaShape.BooleanValue,
      (value, _) => JsonNode.bool(value).validNec,
      cursor =>
        cursor.node.value match
          case JsonValue.JBoolean(value) => value.validNec
          case JsonValue.JNull           =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.Boolean, actual.kind, cursor.context).invalidNec
    )

  /** JSON integer literals are reserved for bounded framing data such as schema versions. */
  val integerLiteral: WireSchema[BigInt] =
    new WireSchema(
      SchemaShape.IntegerValue,
      (value, _) => JsonNode.number(value.toString).validNec,
      cursor =>
        cursor.node.value match
          case JsonValue.JNumber(raw) =>
            val digits = raw.count(_.isDigit)
            if digits > cursor.context.limits.maxIntegerDigits then
              limit(
                DecodeLimit.IntegerDigits,
                digits.toLong,
                cursor.context.limits.maxIntegerDigits,
                cursor.context
              ).invalidNec
            else if !raw.matches("(?:0|-[1-9][0-9]*|[1-9][0-9]*)") then
              WireDecodeViolation.InvalidValue(
                cursor.context.path,
                "canonical-json-integer-literal",
                cursor.context.recordIndex
              ).invalidNec
            else BigInt(raw).validNec
          case JsonValue.JNull =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.Number, actual.kind, cursor.context).invalidNec
    )

  def constant(value: String): WireSchema[Unit] =
    text.refine[Unit]((supplied, context) =>
      Either.cond(
        supplied == value,
        (),
        WireDecodeViolation.InvalidValue(context.path, s"expected-constant:$value", context.recordIndex)
      )
    )(_ => value)

  def vector[A](element: WireSchema[A], limitName: DecodeLimit = DecodeLimit.ArrayEntries): WireSchema[Vector[A]] =
    new WireSchema(
      SchemaShape.ArrayOf(element.shape, limitName),
      (values, path) =>
        values.zipWithIndex.traverse: (value, index) =>
          element.encodeAt(value, path.index(index))
        .map(values => JsonNode(JsonValue.JArray(values), SyntaxLocation.unknown)),
      cursor =>
        cursor.node.value match
          case JsonValue.JArray(values) =>
            val maximum = cursor.context.limits.maximum(limitName)
            if values.size > maximum then
              limit(limitName, values.size.toLong, maximum, cursor.context).invalidNec
            else
              values.zipWithIndex.traverse: (value, index) =>
                element.decodeAt(DecodeCursor(value, cursor.context.index(index)))
          case JsonValue.JNull =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.Array, actual.kind, cursor.context).invalidNec
    )

  def record[A](record: WireRecord[A]): WireSchema[A] =
    new WireSchema(
      SchemaShape.Record(record.fields),
      (value, path) =>
        record.encodeFields(value, path).map(fields => JsonNode(JsonValue.JObject(fields), SyntaxLocation.unknown)),
      cursor =>
        cursor.node.value match
          case JsonValue.JObject(fields) =>
            val values  = fields.iterator.map(field => field.name -> field).toMap
            val unknown = unknownFields(fields, record.fields.map(_.name).toSet, cursor.context)
            (record.decodeFields(values, cursor.context), unknown).mapN((value, _) => value)
          case JsonValue.JNull =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.Object, actual.kind, cursor.context).invalidNec
    )

  def tagged[A](tagField: String, alternatives: Vector[WireCase[A]]): WireSchema[A] =
    if alternatives.isEmpty || alternatives.map(_.tag).distinct.size != alternatives.size then
      throw new IllegalArgumentException("tagged wire schema requires distinct non-empty alternatives")
    if alternatives.exists(_.fields.exists(_.name == tagField)) then
      throw new IllegalArgumentException(s"tagged alternatives must not redefine '$tagField'")
    new WireSchema(
      SchemaShape.Tagged(
        tagField,
        alternatives.map(alternative => SchemaCaseShape(alternative.tag, alternative.fields))
      ),
      (value, path) =>
        val selected = alternatives.flatMap: alternative =>
          alternative.encodeSelected(value, path).map(fields => alternative.tag -> fields)
        selected match
          case Vector((tag, encoded)) =>
            encoded.map: fields =>
              val tagFieldValue = JsonField(tagField, SyntaxLocation.unknown, JsonNode.string(tag))
              JsonNode(JsonValue.JObject(tagFieldValue +: fields), SyntaxLocation.unknown)
          case _ => WireEncodeViolation.UnmatchedAlternative(path).invalidNec,
      cursor =>
        cursor.node.value match
          case JsonValue.JObject(fields) =>
            val values = fields.iterator.map(field => field.name -> field).toMap
            decodeTag(values, tagField, cursor.context).andThen: tag =>
              alternatives.find(_.tag == tag) match
                case None =>
                  WireDecodeViolation
                    .UnknownAlternative(cursor.context.path.field(tagField), tagField, tag, cursor.context.recordIndex)
                    .invalidNec
                case Some(alternative) =>
                  val allowed = alternative.fields.map(_.name).toSet + tagField
                  val unknown = unknownFields(fields, allowed, cursor.context)
                  (alternative.decodeSelected(values, cursor.context), unknown).mapN((value, _) => value)
          case JsonValue.JNull =>
            WireDecodeViolation.NullRequired(cursor.context.path, cursor.context.recordIndex).invalidNec
          case actual => expected(JsonKind.Object, actual.kind, cursor.context).invalidNec
    )
  end tagged

  private def decodeTag(
    values: Map[String, JsonField],
    tagField: String,
    context: DecodeContext
  ): DecodeValidation[String] =
    values.get(tagField) match
      case None =>
        WireDecodeViolation.MissingField(context.path.field(tagField), tagField, context.recordIndex).invalidNec
      case Some(field) => text.decodeAt(DecodeCursor(field.value, context.field(tagField)))

  private def unknownFields(
    fields: Vector[JsonField],
    allowed: Set[String],
    context: DecodeContext
  ): DecodeValidation[Unit] =
    val errors = fields.collect:
      case field if !allowed.contains(field.name) =>
        WireDecodeViolation.UnknownField(context.path.field(field.name), field.name, context.recordIndex)
    NonEmptyChain.fromSeq(errors) match
      case Some(values) => Validated.Invalid(values)
      case None         => ().validNec

  private def expected(expected: JsonKind, actual: JsonKind, context: DecodeContext): WireDecodeViolation =
    WireDecodeViolation.ExpectedType(context.path, expected, actual, context.recordIndex)

  private def limit(
    name: DecodeLimit,
    actual: Long,
    maximum: Int,
    context: DecodeContext
  ): WireDecodeViolation =
    WireDecodeViolation.Limit(WireLimitViolation(name, actual, maximum, context.path, context.recordIndex))

  private[codec] def encodeResult[A](
    value: EncodeValidation[A]
  ): Either[WireViolations[WireEncodeViolation], A] =
    value.toEither.left.map(errors => WireViolations.orderedEncode(errors.toChain.toList.toVector))

  private[codec] def decodeResult[A](
    value: DecodeValidation[A]
  ): Either[WireViolations[WireDecodeViolation], A] =
    value.toEither.left.map(errors => WireViolations.orderedDecode(errors.toChain.toList.toVector))
end WireSchema
