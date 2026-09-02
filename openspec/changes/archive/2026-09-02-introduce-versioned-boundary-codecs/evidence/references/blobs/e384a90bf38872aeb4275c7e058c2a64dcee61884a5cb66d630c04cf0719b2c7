package trading.codec

import scala.util.control.TailCalls.TailRec
import scala.util.control.TailCalls.done
import scala.util.control.TailCalls.tailcall
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
          readValue(parser, first, WirePath.root, depth = 0, limits, recordIndex).result.flatMap: root =>
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
          val bytes = utf8Length(input)
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

  /** Trampoline nested containers so a larger explicit depth profile never consumes the JVM call stack. */
  private def readValue(
    parser: JsonParser,
    token: JsonToken,
    path: WirePath,
    depth: Int,
    limits: DecodeLimits,
    recordIndex: Int
  ): TailRec[Either[WireViolations[WireDecodeViolation], JsonNode]] =
    val at = location(parser.currentTokenLocation())
    token match
      case JsonToken.START_OBJECT =>
        checkDepth(depth + 1, path, limits, recordIndex) match
          case Left(errors) => done(Left(errors))
          case Right(_)     => tailcall(readObject(parser, path, depth + 1, at, limits, recordIndex))
      case JsonToken.START_ARRAY =>
        checkDepth(depth + 1, path, limits, recordIndex) match
          case Left(errors) => done(Left(errors))
          case Right(_)     => tailcall(readArray(parser, path, depth + 1, at, limits, recordIndex))
      case JsonToken.VALUE_STRING =>
        val value = parser.getString()
        done(checkString(value, path, at, limits, recordIndex).map: _ =>
          JsonNode(JsonValue.JString(value), at))
      case JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT =>
        val raw    = parser.getString()
        val digits = raw.count(_.isDigit).toLong
        if digits > limits.maxIntegerDigits then
          done(Left(WireViolations.one(
            limit(DecodeLimit.IntegerDigits, digits, limits.maxIntegerDigits, path, recordIndex)
          )))
        else done(Right(JsonNode(JsonValue.JNumber(raw), at)))
      case JsonToken.VALUE_TRUE  => done(Right(JsonNode(JsonValue.JBoolean(true), at)))
      case JsonToken.VALUE_FALSE => done(Right(JsonNode(JsonValue.JBoolean(false), at)))
      case JsonToken.VALUE_NULL  => done(Right(JsonNode(JsonValue.JNull, at)))
      case _                     =>
        done(Left(
          WireViolations.one(
            WireDecodeViolation.Syntax(
              SyntaxProblem.MalformedJson(s"unexpected token ${token.toString}"),
              at,
              path,
              recordIndex
            )
          )
        ))
    end match
  end readValue

  private def readObject(
    parser: JsonParser,
    path: WirePath,
    depth: Int,
    at: SyntaxLocation,
    limits: DecodeLimits,
    recordIndex: Int
  ): TailRec[Either[WireViolations[WireDecodeViolation], JsonNode]] =
    val fields = Vector.newBuilder[JsonField]
    def loop(count: Int, token: JsonToken | Null): TailRec[Either[WireViolations[WireDecodeViolation], JsonNode]] =
      if token == JsonToken.END_OBJECT then done(Right(JsonNode(JsonValue.JObject(fields.result()), at)))
      else if token == null then
        done(Left(
          WireViolations.one(syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, path))
        ))
      else if token != JsonToken.PROPERTY_NAME then
        done(Left(
          WireViolations.one(
            WireDecodeViolation.Syntax(
              SyntaxProblem.MalformedJson(s"expected property name, found ${token.toString}"),
              location(parser.currentTokenLocation()),
              path,
              recordIndex
            )
          )
        ))
      else
        val name         = parser.currentName()
        val nameLocation = location(parser.currentTokenLocation())
        val fieldPath    = path.field(name)
        checkString(name, fieldPath, nameLocation, limits, recordIndex) match
          case Left(errors) => done(Left(errors))
          case Right(_)     =>
            val nextCount = count + 1
            if nextCount > limits.maxObjectMembers then
              done(Left(
                WireViolations.one(
                  limit(DecodeLimit.ObjectMembers, nextCount.toLong, limits.maxObjectMembers, path, recordIndex)
                )
              ))
            else
              val valueToken = parser.nextToken()
              if valueToken == null then
                done(Left(WireViolations.one(
                  syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, fieldPath)
                )))
              else
                readValue(parser, valueToken, fieldPath, depth, limits, recordIndex).flatMap:
                  case Left(errors) => done(Left(errors))
                  case Right(value) =>
                    fields += JsonField(name, nameLocation, value)
                    tailcall(loop(nextCount, parser.nextToken()))
        end match
    end loop

    tailcall(loop(0, parser.nextToken()))
  end readObject

  private def readArray(
    parser: JsonParser,
    path: WirePath,
    depth: Int,
    at: SyntaxLocation,
    limits: DecodeLimits,
    recordIndex: Int
  ): TailRec[Either[WireViolations[WireDecodeViolation], JsonNode]] =
    val values = Vector.newBuilder[JsonNode]
    def loop(index: Int, token: JsonToken | Null): TailRec[Either[WireViolations[WireDecodeViolation], JsonNode]] =
      if token == JsonToken.END_ARRAY then done(Right(JsonNode(JsonValue.JArray(values.result()), at)))
      else if token == null then
        done(Left(
          WireViolations.one(syntax(SyntaxProblem.UnexpectedEnd, location(parser.currentLocation()), recordIndex, path))
        ))
      else if index >= limits.maxArrayEntries then
        done(Left(
          WireViolations.one(
            limit(DecodeLimit.ArrayEntries, index.toLong + 1L, limits.maxArrayEntries, path, recordIndex)
          )
        ))
      else
        readValue(parser, token, path.index(index), depth, limits, recordIndex).flatMap:
          case Left(errors) => done(Left(errors))
          case Right(value) =>
            values += value
            tailcall(loop(index + 1, parser.nextToken()))
    end loop

    tailcall(loop(0, parser.nextToken()))
  end readArray

  private def utf8Length(value: String): Long =
    var index = 0
    var bytes = 0L
    while index < value.length do
      val codePoint = value.codePointAt(index)
      bytes += (
        if codePoint <= 0x7f then 1L
        else if codePoint <= 0x7ff then 2L
        else if codePoint <= 0xffff then 3L
        else 4L
      )
      index += Character.charCount(codePoint)
    bytes

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
    val reversed = Vector.newBuilder[WirePathSegment]
    var current  = context
    while current != null && !current.inRoot() do
      if current.inObject() && current.hasCurrentName() then
        reversed += WirePathSegment.field(current.currentName())
      else if current.inArray() && current.hasCurrentIndex() then
        reversed += WirePathSegment.index(current.getCurrentIndex())
      current = current.getParent()
    reversed.result().reverse.foldLeft(WirePath.root):
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
