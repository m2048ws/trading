package trading.codec

import java.nio.charset.StandardCharsets
import scala.util.matching.Regex

import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.StreamReadConstraints
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.TokenStreamContext
import tools.jackson.core.TokenStreamLocation
import tools.jackson.core.exc.StreamConstraintsException
import tools.jackson.core.exc.UnexpectedEndOfInputException
import tools.jackson.core.json.JsonFactory
import tools.jackson.core.json.JsonReadFeature

/** The only Jackson-backed parser adapter. Parser objects and exceptions never leave this object. */
private[codec] object StrictJson:
  private val DuplicateMemberPattern: Regex = "(?i).*duplicate.*(?:property|field)[^\\\"]*\\\"([^\\\"]+)\\\".*".r

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], JsonNode] =
    preflight(input, limits, recordIndex).flatMap: _ =>
      val factory                   = parserFactory(limits)
      var parser: JsonParser | Null = null
      try
        parser = factory.createParser(ObjectReadContext.empty(), input)
        val first = parser.nextToken()
        if first == null then
          Left(WireViolations.one(syntax(SyntaxProblem.EmptyInput, SyntaxLocation.unknown, recordIndex)))
        else
          readValue(parser, first, WirePath.root, depth = 0, limits, recordIndex).flatMap: root =>
            val trailing = parser.nextToken()
            if trailing == null then Right(root)
            else
              Left(
                WireViolations.one(
                  WireDecodeViolation.Syntax(
                    SyntaxProblem.TrailingContent,
                    location(parser.currentTokenLocation()),
                    WirePath.root,
                    recordIndex
                  )
                )
              )
      catch
        case expected: JacksonException =>
          val path = Option(parser).map(value => contextPath(value.streamReadContext())).getOrElse(WirePath.root)
          Left(WireViolations.one(parserFailure(expected, path, recordIndex)))
      finally Option(parser).foreach(_.close())
      end try

  private def preflight(
    input: String,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], Unit] =
    Unicode.malformedIndex(input) match
      case Some(index) =>
        Left(
          WireViolations.one(
            WireDecodeViolation.MalformedUnicode(
              WirePath.root,
              index,
              inputLocation(input, index),
              recordIndex
            )
          )
        )
      case None =>
        val characters = input.codePointCount(0, input.length).toLong
        if characters > limits.maxPayloadCharacters then
          Left(
            WireViolations.one(
              limit(
                DecodeLimit.PayloadCharacters,
                characters,
                limits.maxPayloadCharacters,
                WirePath.root,
                recordIndex
              )
            )
          )
        else
          val bytes = input.getBytes(StandardCharsets.UTF_8).length.toLong
          if bytes > limits.maxPayloadUtf8Bytes then
            Left(
              WireViolations.one(
                limit(
                  DecodeLimit.PayloadUtf8Bytes,
                  bytes,
                  limits.maxPayloadUtf8Bytes,
                  WirePath.root,
                  recordIndex
                )
              )
            )
          else Right(())
        end if

  private def parserFactory(limits: DecodeLimits): JsonFactory =
    val constraints =
      StreamReadConstraints
        .builder()
        // Keep parser constraints beyond the public thresholds so project-owned checks retain exact limit identity.
        .maxNestingDepth(Math.max(limits.maxNestingDepth, 1_024))
        .maxDocumentLength(Math.max(limits.maxPayloadCharacters, limits.maxPayloadUtf8Bytes).toLong)
        .maxStringLength(limits.maxPayloadCharacters)
        .maxNameLength(limits.maxPayloadCharacters)
        .maxNumberLength(limits.maxPayloadCharacters)
        .build()
    val builder =
      JsonFactory
        .builder()
        .streamReadConstraints(constraints)
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
    JsonReadFeature.values().foreach(feature => builder.disable(feature))
    builder.build()

  private def readValue(
    parser: JsonParser,
    token: JsonToken,
    path: WirePath,
    depth: Int,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], JsonNode] =
    val at = location(parser.currentTokenLocation())
    token match
      case JsonToken.START_OBJECT =>
        checkDepth(depth + 1, path, limits, recordIndex).flatMap: _ =>
          readObject(parser, path, depth + 1, at, limits, recordIndex)
      case JsonToken.START_ARRAY =>
        checkDepth(depth + 1, path, limits, recordIndex).flatMap: _ =>
          readArray(parser, path, depth + 1, at, limits, recordIndex)
      case JsonToken.VALUE_STRING =>
        val value = parser.getString()
        checkString(value, path, at, limits, recordIndex).map: _ =>
          JsonNode(JsonValue.JString(value), at)
      case JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT =>
        val raw    = parser.getString()
        val digits = raw.count(_.isDigit).toLong
        if digits > limits.maxIntegerDigits then
          Left(WireViolations.one(limit(DecodeLimit.IntegerDigits, digits, limits.maxIntegerDigits, path, recordIndex)))
        else Right(JsonNode(JsonValue.JNumber(raw), at))
      case JsonToken.VALUE_TRUE  => Right(JsonNode(JsonValue.JBoolean(true), at))
      case JsonToken.VALUE_FALSE => Right(JsonNode(JsonValue.JBoolean(false), at))
      case JsonToken.VALUE_NULL  => Right(JsonNode(JsonValue.JNull, at))
      case _                     =>
        Left(
          WireViolations.one(
            WireDecodeViolation.Syntax(
              SyntaxProblem.MalformedJson(s"unexpected token ${token.toString}"),
              at,
              path,
              recordIndex
            )
          )
        )
    end match
  end readValue

  private def readObject(
    parser: JsonParser,
    path: WirePath,
    depth: Int,
    at: SyntaxLocation,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], JsonNode] =
    val fields = Vector.newBuilder[JsonField]
    var count  = 0
    var token  = parser.nextToken()
    while token != JsonToken.END_OBJECT do
      if token == null then
        return Left(
          WireViolations.one(syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, path))
        )
      if token != JsonToken.PROPERTY_NAME then
        return Left(
          WireViolations.one(
            WireDecodeViolation.Syntax(
              SyntaxProblem.MalformedJson(s"expected property name, found ${token.toString}"),
              location(parser.currentTokenLocation()),
              path,
              recordIndex
            )
          )
        )
      val name         = parser.currentName()
      val nameLocation = location(parser.currentTokenLocation())
      val fieldPath    = path.field(name)
      checkString(name, fieldPath, nameLocation, limits, recordIndex) match
        case Left(errors) => return Left(errors)
        case Right(_)     => ()
      count += 1
      if count > limits.maxObjectMembers then
        return Left(
          WireViolations.one(
            limit(DecodeLimit.ObjectMembers, count.toLong, limits.maxObjectMembers, path, recordIndex)
          )
        )
      val valueToken = parser.nextToken()
      if valueToken == null then
        return Left(WireViolations.one(
          syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, fieldPath)
        ))
      readValue(parser, valueToken, fieldPath, depth, limits, recordIndex) match
        case Left(errors) => return Left(errors)
        case Right(value) => fields += JsonField(name, nameLocation, value)
      token = parser.nextToken()
    end while
    Right(JsonNode(JsonValue.JObject(fields.result()), at))
  end readObject

  private def readArray(
    parser: JsonParser,
    path: WirePath,
    depth: Int,
    at: SyntaxLocation,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], JsonNode] =
    val values = Vector.newBuilder[JsonNode]
    var index  = 0
    var token  = parser.nextToken()
    while token != JsonToken.END_ARRAY do
      if token == null then
        return Left(
          WireViolations.one(syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, path))
        )
      if index >= limits.maxArrayEntries then
        return Left(
          WireViolations.one(
            limit(DecodeLimit.ArrayEntries, index.toLong + 1L, limits.maxArrayEntries, path, recordIndex)
          )
        )
      readValue(parser, token, path.index(index), depth, limits, recordIndex) match
        case Left(errors) => return Left(errors)
        case Right(value) => values += value
      index += 1
      token = parser.nextToken()
    Right(JsonNode(JsonValue.JArray(values.result()), at))
  end readArray

  private def checkDepth(
    depth: Int,
    path: WirePath,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], Unit] =
    Either.cond(
      depth <= limits.maxNestingDepth,
      (),
      WireViolations.one(limit(DecodeLimit.NestingDepth, depth.toLong, limits.maxNestingDepth, path, recordIndex))
    )

  private def checkString(
    value: String,
    path: WirePath,
    at: SyntaxLocation,
    limits: DecodeLimits,
    recordIndex: Int
  ): Either[WireViolations[WireDecodeViolation], Unit] =
    Unicode.malformedIndex(value) match
      case Some(index) =>
        Left(WireViolations.one(WireDecodeViolation.MalformedUnicode(path, index, at, recordIndex)))
      case None =>
        val characters = value.codePointCount(0, value.length).toLong
        Either.cond(
          characters <= limits.maxStringCharacters,
          (),
          WireViolations.one(
            limit(DecodeLimit.StringCharacters, characters, limits.maxStringCharacters, path, recordIndex)
          )
        )

  private def parserFailure(
    failure: JacksonException,
    path: WirePath,
    recordIndex: Int
  ): WireDecodeViolation =
    val detail  = Option(failure.getOriginalMessage()).getOrElse(failure.getClass.getSimpleName)
    val problem =
      failure match
        case _: StreamConstraintsException    => SyntaxProblem.ParserConstraint(detail)
        case _: UnexpectedEndOfInputException => SyntaxProblem.UnexpectedEnd
        case _                                =>
          detail match
            case DuplicateMemberPattern(name) => SyntaxProblem.DuplicateMember(name)
            case _                            => SyntaxProblem.MalformedJson(detail)
    val exactPath =
      problem match
        case SyntaxProblem.DuplicateMember(name)
          if !path.segments.lastOption.flatMap(_.fieldName).contains(name) => path.field(name)
        case _ => path
    WireDecodeViolation.Syntax(problem, location(failure.getLocation()), exactPath, recordIndex)

  private def contextPath(context: TokenStreamContext): WirePath =
    def segments(current: TokenStreamContext | Null): Vector[WirePathSegment] =
      if current == null || current.inRoot() then Vector.empty
      else
        val prefix = segments(current.getParent())
        if current.inObject() && current.hasCurrentName() then prefix :+ WirePathSegment.field(current.currentName())
        else if current.inArray() && current.hasCurrentIndex() then
          prefix :+ WirePathSegment.index(current.getCurrentIndex())
        else prefix
    segments(context).foldLeft(WirePath.root):
      case (path, segment) =>
        segment.fieldName match
          case Some(name) => path.field(name)
          case None       => path.index(segment.arrayIndex.getOrElse(0))

  private def location(value: TokenStreamLocation | Null): SyntaxLocation =
    Option(value).fold(SyntaxLocation.unknown): at =>
      SyntaxLocation(
        nonnegative(at.getCharOffset()),
        nonnegative(at.getByteOffset()),
        positive(at.getLineNr()),
        positive(at.getColumnNr())
      )

  private def inputLocation(input: String, characterIndex: Int): SyntaxLocation =
    val prefix = input.substring(0, characterIndex)
    val line   = prefix.count(_ == '\n') + 1
    val column = prefix.lastIndexOf('\n') match
      case -1    => characterIndex + 1
      case index => characterIndex - index
    SyntaxLocation(Some(characterIndex.toLong), None, Some(line), Some(column))

  private def nonnegative(value: Long): Option[Long] = Option.when(value >= 0L)(value)
  private def positive(value: Int): Option[Int]      = Option.when(value > 0)(value)

  private def limit(
    name: DecodeLimit,
    actual: Long,
    maximum: Int,
    path: WirePath,
    recordIndex: Int
  ): WireDecodeViolation =
    WireDecodeViolation.Limit(WireLimitViolation(name, actual, maximum, path, recordIndex))

  private def syntax(
    problem: SyntaxProblem,
    at: SyntaxLocation,
    recordIndex: Int,
    path: WirePath = WirePath.root
  ): WireDecodeViolation =
    WireDecodeViolation.Syntax(problem, at, path, recordIndex)
end StrictJson
