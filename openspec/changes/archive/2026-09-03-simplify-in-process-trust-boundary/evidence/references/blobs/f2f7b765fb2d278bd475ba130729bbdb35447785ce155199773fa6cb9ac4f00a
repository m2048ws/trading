package trading.codec

import java.util.Objects

import trading.quantity.JavaSerializationUnsupported

enum RecordTypeViolation:
  case Empty
  case InvalidAsciiFormat
end RecordTypeViolation

/** Stable lower-ASCII dotted record-family identifier. */
final class RecordType private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean =
    other match
      case that: RecordType => value == that.value
      case _                => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"RecordType($value)"
end RecordType

object RecordType:
  private val Pattern = "[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9-]*)+".r

  def from(value: String): Either[RecordTypeViolation, RecordType] =
    val supplied = Objects.requireNonNull(value, "record type")
    if supplied.isEmpty then Left(RecordTypeViolation.Empty)
    else
      supplied match
        case Pattern() => Right(construct(supplied))
        case _         => Left(RecordTypeViolation.InvalidAsciiFormat)

  private def construct(value: String): RecordType =
    new RecordType(value)
end RecordType

final case class NonPositiveSchemaVersion(value: BigInt)

/** Positive arbitrary-precision version scoped to one record type, distinct from reference-data versions. */
final class SchemaVersion private (val value: BigInt) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean =
    other match
      case that: SchemaVersion => value == that.value
      case _                   => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"SchemaVersion($value)"
end SchemaVersion

object SchemaVersion:
  val one: SchemaVersion = construct(BigInt(1))

  def from(value: BigInt): Either[NonPositiveSchemaVersion, SchemaVersion] =
    val supplied = Objects.requireNonNull(value, "schema version")
    Either.cond(supplied > 0, construct(supplied), NonPositiveSchemaVersion(supplied))

  private def construct(value: BigInt): SchemaVersion =
    new SchemaVersion(value)
end SchemaVersion

private[codec] final case class EnvelopeHeader(
  payload: JsonNode,
  recordType: RecordType,
  schemaVersion: SchemaVersion)

/** One family-specific envelope dispatcher with an explicit supported-version table and one current writer. */
private[codec] final class EnvelopeCodec[A] private (
  val recordType: RecordType,
  val currentVersion: SchemaVersion,
  private val readers: Vector[(SchemaVersion, WireSchema[A])],
  private val knownRecordTypes: Set[RecordType]):

  private val currentSchema = readers.collectFirst:
    case (version, schema) if version == currentVersion => schema
  .getOrElse(throw new IllegalArgumentException("current envelope version requires a writer schema"))

  def write(value: A): Either[WireViolations[WireEncodeViolation], String] =
    envelopeSchema(currentVersion, currentSchema).write(value)

  def read(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], A] =
    StrictJson.parse(input, limits, recordIndex).flatMap(node => decode(node, limits, recordIndex))

  def schema(
    id: String,
    definitionName: String
  ): Either[WireViolations[WireEncodeViolation], String] =
    JsonSchemaDocument.render(id, definitionName, envelopeSchema(currentVersion, currentSchema))

  private def decode(
    node: JsonNode,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], A] =
    val rootContext = DecodeContext(limits, recordIndex, WirePath.root)
    EnvelopeCodec.decodeHeader(node, rootContext).flatMap: header =>
      if header.recordType != recordType then
        val problem =
          if knownRecordTypes.contains(header.recordType) then
            EnvelopeProblem.RecordTypeMismatch(recordType, header.recordType)
          else EnvelopeProblem.UnknownRecordType(header.recordType)
        Left(WireViolations.one(WireDecodeViolation.Envelope(WirePath.root.field("recordType"), problem, recordIndex)))
      else
        readers.collectFirst:
          case (version, schema) if version == header.schemaVersion => schema
        match
          case None =>
            Left(
              WireViolations.one(
                WireDecodeViolation.Envelope(
                  WirePath.root.field("schemaVersion"),
                  EnvelopeProblem.UnsupportedSchemaVersion(recordType, header.schemaVersion),
                  recordIndex
                )
              )
            )
          case Some(schema) =>
            WireSchema.decodeResult(
              schema.decodeAt(DecodeCursor(header.payload, rootContext.field("payload")))
            )
  end decode

  private def envelopeSchema(version: SchemaVersion, payload: WireSchema[A]): WireSchema[A] =
    val representation =
      WireRecord
        .field("payload", payload)
        .product(WireRecord.field("recordType", WireSchema.textConstant(recordType.value)))
        .product(WireRecord.field("schemaVersion", WireSchema.integerConstant(version.value)))
        .imap(value => value._1._1)(value => ((value, ()), ()))
    WireSchema.record(representation)
end EnvelopeCodec

private[codec] object EnvelopeCodec:
  private val recordTypeSchema: WireSchema[RecordType] =
    WireSchema.text.refine[RecordType]((supplied, context) =>
      RecordType.from(supplied).left.map: cause =>
        WireDecodeViolation.Envelope(
          context.path,
          EnvelopeProblem.InvalidRecordType(supplied, cause),
          context.recordIndex
        )
    )(_.value)

  private val schemaVersionSchema: WireSchema[SchemaVersion] =
    WireSchema.integerLiteral.refine[SchemaVersion]((supplied, context) =>
      SchemaVersion.from(supplied).left.map: _ =>
        WireDecodeViolation.Envelope(
          context.path,
          EnvelopeProblem.InvalidSchemaVersion(supplied.toString),
          context.recordIndex
        )
    )(_.value)

  private val headerSchema: WireSchema[EnvelopeHeader] =
    val representation =
      WireRecord
        .field("payload", WireSchema.node)
        .product(WireRecord.field("recordType", recordTypeSchema))
        .product(WireRecord.field("schemaVersion", schemaVersionSchema))
        .imap(value => EnvelopeHeader(value._1._1, value._1._2, value._2))(value =>
          ((value.payload, value.recordType), value.schemaVersion)
        )
    WireSchema.record(representation)

  def apply[A](
    recordType: RecordType,
    currentVersion: SchemaVersion,
    readers: Vector[(SchemaVersion, WireSchema[A])],
    knownRecordTypes: Set[RecordType] = Set.empty
  ): EnvelopeCodec[A] =
    if readers.isEmpty || readers.map(_._1).distinct.size != readers.size then
      throw new IllegalArgumentException("envelope readers require distinct non-empty versions")
    val known = knownRecordTypes + recordType
    new EnvelopeCodec(recordType, currentVersion, readers, known)

  private def decodeHeader(
    node: JsonNode,
    context: DecodeContext
  ): Either[WireViolations[WireDecodeViolation], EnvelopeHeader] =
    val decoded = headerSchema.decodeAt(DecodeCursor(node, context)).leftMap(_.map(mapHeaderViolation))
    WireSchema.decodeResult(decoded)

  private def mapHeaderViolation(violation: WireDecodeViolation): WireDecodeViolation =
    violation match
      case WireDecodeViolation.MissingField(path, "payload", index) =>
        WireDecodeViolation.Envelope(path, EnvelopeProblem.MissingPayload, index)
      case WireDecodeViolation.MissingField(path, "recordType", index) =>
        WireDecodeViolation.Envelope(path, EnvelopeProblem.MissingRecordType, index)
      case WireDecodeViolation.MissingField(path, "schemaVersion", index) =>
        WireDecodeViolation.Envelope(path, EnvelopeProblem.MissingSchemaVersion, index)
      case WireDecodeViolation.ExactNumber(path, problem, index)
        if path == WirePath.root.field("schemaVersion") =>
        val supplied =
          problem match
            case ExactNumberProblem.NonCanonicalInteger(spelling) => spelling
            case other                                            => other.toString
        WireDecodeViolation.Envelope(path, EnvelopeProblem.InvalidSchemaVersion(supplied), index)
      case other => other

end EnvelopeCodec
