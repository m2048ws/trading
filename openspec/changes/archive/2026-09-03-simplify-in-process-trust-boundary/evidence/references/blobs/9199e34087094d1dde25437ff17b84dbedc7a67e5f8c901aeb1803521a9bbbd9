package trading.codec

import java.util.Objects

/** One immutable component of a wire location. */
sealed trait WirePathSegment:
  def fieldName: Option[String]
  def arrayIndex: Option[Int]
end WirePathSegment

object WirePathSegment:
  private[codec] final class Field private[codec] (val name: String) extends WirePathSegment:
    def fieldName: Option[String] = Some(name)
    def arrayIndex: Option[Int]   = None

    override def equals(other: Any): Boolean =
      other match
        case that: Field => name == that.name
        case _           => false

    override def hashCode: Int    = name.hashCode
    override def toString: String = s"Field($name)"
  end Field

  private[codec] final class Index private[codec] (val value: Int) extends WirePathSegment:
    def fieldName: Option[String] = None
    def arrayIndex: Option[Int]   = Some(value)

    override def equals(other: Any): Boolean =
      other match
        case that: Index => value == that.value
        case _           => false

    override def hashCode: Int    = value.hashCode
    override def toString: String = s"Index($value)"
  end Index

  private[codec] def field(name: String): WirePathSegment =
    new Field(Objects.requireNonNull(name, "wire field name"))

  private[codec] def index(value: Int): WirePathSegment =
    if value < 0 then throw new IllegalArgumentException("wire array index must be nonnegative")
    new Index(value)
end WirePathSegment

/** Structured field/index location owned by the codec boundary. */
final class WirePath private (val segments: Vector[WirePathSegment]):
  private[codec] def field(name: String): WirePath =
    WirePath.construct(segments :+ WirePathSegment.field(name))

  private[codec] def index(value: Int): WirePath =
    WirePath.construct(segments :+ WirePathSegment.index(value))

  def render: String =
    segments.foldLeft("$"):
      case (prefix, segment) =>
        segment.fieldName match
          case Some(name) if name.matches("[A-Za-z_][A-Za-z0-9_]*") => s"$prefix.$name"
          case Some(name)                                           =>
            val escaped = name.replace("\\", "\\\\").replace("'", "\\'")
            s"$prefix['$escaped']"
          case None => s"$prefix[${segment.arrayIndex.getOrElse(0)}]"

  override def equals(other: Any): Boolean =
    other match
      case that: WirePath => segments == that.segments
      case _              => false

  override def hashCode: Int    = segments.hashCode
  override def toString: String = render
end WirePath

object WirePath:
  val root: WirePath = construct(Vector.empty)

  private[codec] val ordering: Ordering[WirePath] =
    new Ordering[WirePath]:
      def compare(left: WirePath, right: WirePath): Int =
        val common = Math.min(left.segments.size, right.segments.size)
        var index  = 0
        while index < common do
          val compared = compareSegment(left.segments(index), right.segments(index))
          if compared != 0 then return compared
          index += 1
        Integer.compare(left.segments.size, right.segments.size)

      private def compareSegment(left: WirePathSegment, right: WirePathSegment): Int =
        (left.fieldName, right.fieldName) match
          case (Some(leftName), Some(rightName)) => leftName.compareTo(rightName)
          case (Some(_), None)                   => -1
          case (None, Some(_))                   => 1
          case (None, None) => Integer.compare(left.arrayIndex.getOrElse(0), right.arrayIndex.getOrElse(0))

  private def construct(segments: Vector[WirePathSegment]): WirePath =
    new WirePath(segments)
end WirePath

/** Source coordinates retained when the parser reports them. Line and column values are one-based. */
final case class SyntaxLocation(
  characterOffset: Option[Long],
  byteOffset: Option[Long],
  line: Option[Int],
  column: Option[Int])

object SyntaxLocation:
  val unknown: SyntaxLocation = SyntaxLocation(None, None, None, None)
end SyntaxLocation

/** Stable ordering of validation phases at the wire boundary. */
enum WireStage(val rank: Int):
  case InputLimit extends WireStage(0)
  case Syntax     extends WireStage(1)
  case Structure  extends WireStage(2)
  case Refinement extends WireStage(3)
  case Catalog    extends WireStage(4)
  case Assembly   extends WireStage(5)
  case Domain     extends WireStage(6)
end WireStage

/** Named operational limits; these names do not become JSON Schema restrictions. */
enum DecodeLimit:
  case PayloadCharacters
  case PayloadUtf8Bytes
  case NestingDepth
  case BatchRecords
  case ObjectMembers
  case ArrayEntries
  case StringCharacters
  case IntegerDigits
  case DimensionFactors
  case CatalogCommands
  case ScenarioSlices
  case MarketConversions
end DecodeLimit

/** Invalid immutable limit-policy construction. */
enum DecodeLimitConfigurationViolation:
  case NonPositive(limit: DecodeLimit, supplied: Int)
  case ExceedsContainer(limit: DecodeLimit, supplied: Int, container: DecodeLimit, containerMaximum: Int)
end DecodeLimitConfigurationViolation

/** A concrete payload or collection exceeded one selected operational limit. */
final case class WireLimitViolation(
  limit: DecodeLimit,
  actual: Long,
  maximum: Int,
  path: WirePath,
  recordIndex: Int)

enum JsonKind:
  case Object, Array, String, Number, Boolean, Null
end JsonKind

enum SyntaxProblem:
  case EmptyInput
  case DuplicateMember(name: String)
  case TrailingContent
  case UnexpectedEnd
  case ParserConstraint(detail: String)
  case MalformedJson(detail: String)
end SyntaxProblem

/** Exact-number failures retain the wire spelling that was rejected. */
enum ExactNumberProblem:
  case NonCanonicalInteger(spelling: String)
  case NonPositiveInteger(spelling: String)
  case OutsideTargetRange(target: String, spelling: String)
  case NonReducedRational(numerator: String, denominator: String)
  case NonCanonicalZero(denominator: String)
  case RationalProjectionMismatch(numerator: String, denominator: String)
end ExactNumberProblem

/** Stable identifier owner whose checked constructor rejected one exact source string. */
enum StableIdentifierKind:
  case Asset, Grid, Instrument, Underlying, DimensionAtom
end StableIdentifierKind

enum StableIdentifierProblem:
  case Empty
end StableIdentifierProblem

/** Canonical dimension-array failures detected before the normalizing domain constructor is invoked. */
enum DimensionProblem:
  case ZeroPower(atom: String)
  case DuplicateAtom(atom: String)
  case AtomOutOfOrder(previous: String, supplied: String)
  case NormalizationMismatch
end DimensionProblem

/** Failures owned by the exact record envelope and its type/version dispatch. */
enum EnvelopeProblem:
  case MissingPayload
  case MissingRecordType
  case MissingSchemaVersion
  case InvalidRecordType(supplied: String, cause: RecordTypeViolation)
  case InvalidSchemaVersion(supplied: String)
  case UnknownRecordType(supplied: RecordType)
  case RecordTypeMismatch(expected: RecordType, supplied: RecordType)
  case UnsupportedSchemaVersion(recordType: RecordType, supplied: SchemaVersion)
end EnvelopeProblem

/** Expected encoding failures from codec-owned immutable values. */
enum WireEncodeViolation:
  case MalformedUnicode(atPath: WirePath, characterIndex: Int)
  case DuplicateMember(atPath: WirePath, name: String)
  case UnsupportedNumber(atPath: WirePath, spelling: String)
  case UnmatchedAlternative(atPath: WirePath)
  case InvalidSchemaIdentifier(value: String)
  case InvalidSchemaDefinitionName(value: String)

  private[codec] def path: WirePath =
    this match
      case MalformedUnicode(path, _)      => path
      case DuplicateMember(path, _)       => path
      case UnsupportedNumber(path, _)     => path
      case UnmatchedAlternative(path)     => path
      case InvalidSchemaIdentifier(_)     => WirePath.root
      case InvalidSchemaDefinitionName(_) => WirePath.root

  private[codec] def detailOrder: Int = ordinal
end WireEncodeViolation

/** Syntax and structural failures returned without exposing parser or validation-library types. */
enum WireDecodeViolation:
  case Limit(value: WireLimitViolation)
  case Syntax(problem: SyntaxProblem, location: SyntaxLocation, atPath: WirePath, atRecordIndex: Int)
  case MalformedUnicode(
    atPath: WirePath,
    characterIndex: Int,
    location: SyntaxLocation,
    atRecordIndex: Int)
  case ExpectedType(atPath: WirePath, expected: JsonKind, actual: JsonKind, atRecordIndex: Int)
  case MissingField(atPath: WirePath, name: String, atRecordIndex: Int)
  case UnknownField(atPath: WirePath, name: String, atRecordIndex: Int)
  case NullRequired(atPath: WirePath, atRecordIndex: Int)
  case InvalidValue(atPath: WirePath, code: String, atRecordIndex: Int)
  case ExactNumber(atPath: WirePath, problem: ExactNumberProblem, atRecordIndex: Int)
  case InvalidStableIdentifier(
    atPath: WirePath,
    kind: StableIdentifierKind,
    problem: StableIdentifierProblem,
    supplied: String,
    atRecordIndex: Int)
  case InvalidDimension(atPath: WirePath, problem: DimensionProblem, atRecordIndex: Int)
  case Envelope(atPath: WirePath, problem: EnvelopeProblem, atRecordIndex: Int)
  case UnknownAlternative(atPath: WirePath, tagField: String, supplied: String, atRecordIndex: Int)

  def stage: WireStage =
    this match
      case Limit(_)                               => WireStage.InputLimit
      case Syntax(_, _, _, _)                     => WireStage.Syntax
      case MalformedUnicode(_, _, _, _)           => WireStage.Syntax
      case ExpectedType(_, _, _, _)               => WireStage.Structure
      case MissingField(_, _, _)                  => WireStage.Structure
      case UnknownField(_, _, _)                  => WireStage.Structure
      case NullRequired(_, _)                     => WireStage.Structure
      case InvalidValue(_, _, _)                  => WireStage.Refinement
      case ExactNumber(_, _, _)                   => WireStage.Refinement
      case InvalidStableIdentifier(_, _, _, _, _) => WireStage.Refinement
      case InvalidDimension(_, _, _)              => WireStage.Refinement
      case Envelope(_, _, _)                      => WireStage.Structure
      case UnknownAlternative(_, _, _, _)         => WireStage.Structure

  def path: WirePath =
    this match
      case Limit(value)                              => value.path
      case Syntax(_, _, path, _)                     => path
      case MalformedUnicode(path, _, _, _)           => path
      case ExpectedType(path, _, _, _)               => path
      case MissingField(path, _, _)                  => path
      case UnknownField(path, _, _)                  => path
      case NullRequired(path, _)                     => path
      case InvalidValue(path, _, _)                  => path
      case ExactNumber(path, _, _)                   => path
      case InvalidStableIdentifier(path, _, _, _, _) => path
      case InvalidDimension(path, _, _)              => path
      case Envelope(path, _, _)                      => path
      case UnknownAlternative(path, _, _, _)         => path

  def recordIndex: Int =
    this match
      case Limit(value)                               => value.recordIndex
      case Syntax(_, _, _, recordIndex)               => recordIndex
      case MalformedUnicode(_, _, _, index)           => index
      case ExpectedType(_, _, _, index)               => index
      case MissingField(_, _, index)                  => index
      case UnknownField(_, _, index)                  => index
      case NullRequired(_, index)                     => index
      case InvalidValue(_, _, index)                  => index
      case ExactNumber(_, _, index)                   => index
      case InvalidStableIdentifier(_, _, _, _, index) => index
      case InvalidDimension(_, _, index)              => index
      case Envelope(_, _, index)                      => index
      case UnknownAlternative(_, _, _, index)         => index

  private[codec] def detailOrder: Int = ordinal
end WireDecodeViolation

/** Domain-owned non-empty ordered aggregate used at codec boundaries. */
final class WireViolations[+E] private (val head: E, val tail: Vector[E]):
  def toVector: Vector[E] = head +: tail

  def map[E2](f: E => E2): WireViolations[E2] =
    WireViolations.unsafe(f(head), tail.map(f))

  def concat[E2 >: E](other: WireViolations[E2]): WireViolations[E2] =
    WireViolations.unsafe(head, tail ++ other.toVector)

  override def equals(other: Any): Boolean =
    other match
      case that: WireViolations[?] => toVector == that.toVector
      case _                       => false

  override def hashCode: Int    = toVector.hashCode
  override def toString: String = toVector.mkString("WireViolations(", ",", ")")
end WireViolations

object WireViolations:
  def one[E](head: E): WireViolations[E] =
    unsafe(head, Vector.empty)

  def fromVector[E](values: Vector[E]): Option[WireViolations[E]] =
    values.headOption.map(head => unsafe(head, values.tail))

  private[codec] def orderedDecode(values: Vector[WireDecodeViolation]): WireViolations[WireDecodeViolation] =
    val sorted = values.sortWith: (left, right) =>
      val record = Integer.compare(left.recordIndex, right.recordIndex)
      if record != 0 then record < 0
      else
        val stage = Integer.compare(left.stage.rank, right.stage.rank)
        if stage != 0 then stage < 0
        else
          val path = WirePath.ordering.compare(left.path, right.path)
          if path != 0 then path < 0 else left.detailOrder < right.detailOrder
    fromVector(sorted).getOrElse(throw new IllegalArgumentException("ordered decode violations must be non-empty"))

  private[codec] def orderedEncode(values: Vector[WireEncodeViolation]): WireViolations[WireEncodeViolation] =
    val sorted = values.sortWith: (left, right) =>
      val path = WirePath.ordering.compare(left.path, right.path)
      if path != 0 then path < 0 else left.detailOrder < right.detailOrder
    fromVector(sorted).getOrElse(throw new IllegalArgumentException("ordered encode violations must be non-empty"))

  private[codec] def unsafe[E](head: E, tail: Vector[E]): WireViolations[E] =
    new WireViolations(head, tail)
end WireViolations
