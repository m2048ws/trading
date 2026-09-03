package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import scala.jdk.CollectionConverters.*

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import munit.FunSuite

import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.reference.GridId
import trading.reference.GridIdentity
import trading.reference.GridKey
import trading.reference.GridVersion

class RecordEnvelopeSuite extends FunSuite:
  private final case class Payload(exact: Rational, grid: GridIdentity)

  private enum Choice:
    case Named(name: String)
    case Counted(count: BigInt)

  private val payloadSchema =
    val representation =
      WireRecord
        .field("exact", ExactWire.rational)
        .product(WireRecord.field("grid", ExactWire.gridIdentity))
        .imap(value => Payload(value._1, value._2))(value => (value.exact, value.grid))
    WireSchema.record(representation)

  private val choiceSchema = WireSchema.tagged[Choice](
    "kind",
    Vector(
      WireCase[Choice, String]("named", WireRecord.field("name", WireSchema.text)):
        case Choice.Named(value) => Some(value)
        case _                   =>
          None
      (Choice.Named.apply),
      WireCase[Choice, BigInt]("counted", WireRecord.field("count", WireSchema.integerLiteral)):
        case Choice.Counted(value) => Some(value)
        case _                     =>
          None
      (Choice.Counted.apply)
    )
  )

  private val recordType  = RecordType.from("trading.sample").toOption.get
  private val otherType   = RecordType.from("trading.other").toOption.get
  private val version     = SchemaVersion.one
  private val codec       = EnvelopeCodec(recordType, version, Vector(version -> payloadSchema), Set(otherType))
  private val choiceCodec = EnvelopeCodec(recordType, version, Vector(version -> choiceSchema), Set(otherType))
  private val payload     = Payload(
    Rational(BigInt(-2), BigInt(3)),
    GridIdentity(
      DimKey.atom(AtomId("usd")),
      GridKey(GridId.from("price").toOption.get, GridVersion.from(7).toOption.get)
    )
  )
  private val golden =
    """{"payload":{"exact":{"denominator":"3","numerator":"-2"},"grid":{"dimension":[{"atom":"usd","power":"1"}],"gridId":"price","gridVersion":"7"}},"recordType":"trading.sample","schemaVersion":1}"""

  test("record and schema versions are exact checked values"):
    val huge = BigInt(10).pow(200)

    assertEquals(RecordType.from("trading.sample-v2").map(_.value), Right("trading.sample-v2"))
    Vector("", "Trading.sample", "trading", "trading.sample_2", " trading.sample").foreach: value =>
      assert(RecordType.from(value).isLeft, value)
    assertEquals(SchemaVersion.from(huge).map(_.value), Right(huge))
    assert(SchemaVersion.from(BigInt(0)).isLeft)
    assert(SchemaVersion.from(BigInt(-1)).isLeft)
    rejectSerialization(recordType)
    rejectSerialization(version)

  test("current writer emits one canonical envelope and keeps schemaVersion distinct from gridVersion"):
    assertEquals(codec.write(payload), Right(golden))
    assertEquals(codec.read(golden), Right(payload))
    assert(golden.contains("\"gridVersion\":\"7\""))
    assert(golden.contains("\"schemaVersion\":1"))

  test("member order and insignificant whitespace decode before canonical re-encoding"):
    val reordered =
      """{
        | "schemaVersion" : 1,
        | "recordType" : "trading.sample",
        | "payload" : {
        |   "grid" : {"gridVersion":"7","gridId":"price","dimension":[{"power":"1","atom":"usd"}]},
        |   "exact" : {"numerator":"-2","denominator":"3"}
        | }
        |}""".stripMargin

    val decoded = codec.read(reordered)
    assertEquals(decoded, Right(payload))
    assertEquals(decoded.flatMap(codec.write), Right(golden))

  test("duplicate, unknown, missing, and null envelope fields retain typed paths"):
    val duplicate =
      """{"payload":{},"payload":{},"recordType":"trading.sample","schemaVersion":1}"""
    codec.read(duplicate).left.toOption.get.head match
      case WireDecodeViolation.Syntax(
          SyntaxProblem.DuplicateMember("payload"),
          _,
          path,
          0
        ) => assertEquals(path, WirePath.root.field("payload"))
      case other => fail(s"unexpected duplicate-field failure: $other")

    val unknown =
      """{"payload":{},"recordType":"trading.sample","schemaVersion":1,"extra":true}"""
    assertEquals(unknownField(codec.read(unknown)), ("$.extra", "extra"))

    Vector(
      ("""{"recordType":"trading.sample","schemaVersion":1}""", EnvelopeProblem.MissingPayload),
      ("""{"payload":{},"schemaVersion":1}""", EnvelopeProblem.MissingRecordType),
      ("""{"payload":{},"recordType":"trading.sample"}""", EnvelopeProblem.MissingSchemaVersion)
    ).foreach: (input, expected) =>
      codec.read(input).left.toOption.get.head match
        case WireDecodeViolation.Envelope(_, `expected`, 0) => ()
        case other                                          => fail(s"unexpected missing-field failure: $other")

    Vector(
      "payload"       -> """{"payload":null,"recordType":"trading.sample","schemaVersion":1}""",
      "recordType"    -> """{"payload":{},"recordType":null,"schemaVersion":1}""",
      "schemaVersion" -> """{"payload":{},"recordType":"trading.sample","schemaVersion":null}"""
    ).foreach: (field, input) =>
      assertEquals(codec.read(input).left.toOption.get.head.path.render, s"$$.$field")

  test("dispatch distinguishes malformed, unknown, mismatched, and unsupported envelope headers"):
    val cases = Vector(
      (
        """{"payload":{},"recordType":"Trading.sample","schemaVersion":1}""",
        classOf[EnvelopeProblem.InvalidRecordType]
      ),
      (
        """{"payload":{},"recordType":"trading.unknown","schemaVersion":1}""",
        classOf[EnvelopeProblem.UnknownRecordType]
      ),
      (
        """{"payload":{},"recordType":"trading.other","schemaVersion":1}""",
        classOf[EnvelopeProblem.RecordTypeMismatch]
      ),
      (
        """{"payload":{},"recordType":"trading.sample","schemaVersion":2}""",
        classOf[EnvelopeProblem.UnsupportedSchemaVersion]
      ),
      (
        """{"payload":{},"recordType":"trading.sample","schemaVersion":1.0}""",
        classOf[EnvelopeProblem.InvalidSchemaVersion]
      )
    )

    cases.foreach: (input, expectedClass) =>
      codec.read(input).left.toOption.get.head match
        case WireDecodeViolation.Envelope(path, problem, 0) =>
          assert(expectedClass.isInstance(problem), s"expected ${expectedClass.getName}, obtained $problem")
          assert(path.render == "$.recordType" || path.render == "$.schemaVersion")
        case other => fail(s"unexpected header failure: $other")

    assert(
      codec
        .read("""{"payload":{},"recordType":"trading.\uD800","schemaVersion":1}""")
        .left
        .toOption
        .get
        .head
        .isInstanceOf[WireDecodeViolation.MalformedUnicode]
    )

  test("closed payload alternatives reject cross-alternative fields inside the envelope"):
    val crossed =
      """{"payload":{"kind":"named","name":"alpha","count":1},"recordType":"trading.sample","schemaVersion":1}"""
    assertEquals(unknownField(choiceCodec.read(crossed)), ("$.payload.count", "count"))

  test("generated envelope schema is stable, closed, local, and agrees with offline validation"):
    val id         = "urn:trading:codec:schema:sample:v1"
    val schemaText = codec.schema(id, "SampleV1").toOption.get
    val registry   = SchemaRegistry.withDefaultDialect(
      SpecificationVersion.DRAFT_2020_12,
      builder =>
        val _ = builder.schemas(Map(id -> schemaText).asJava)
        val _ = builder.schemaLoader: loader =>
          val _ = loader.fetchRemoteResources(false)
          ()
    )
    val schema = registry.getSchema(SchemaLocation.of(id))
    val meta   = registry.getSchema(
      SchemaLocation.of(SpecificationVersion.DRAFT_2020_12.getDialectId())
    )
    val invalidVersion = golden.replace("\"schemaVersion\":1", "\"schemaVersion\":2")

    assert(
      meta.validate(schemaText, InputFormat.JSON).isEmpty,
      meta.validate(schemaText, InputFormat.JSON).asScala.mkString("\n")
    )
    assert(schema.validate(golden, InputFormat.JSON).isEmpty)
    assert(schema.validate(invalidVersion, InputFormat.JSON).asScala.nonEmpty)
    assert(schemaText.contains("\"const\":\"trading.sample\""), schemaText)
    assert(schemaText.contains("\"const\":1"), schemaText)
    assert(schemaText.contains("\"additionalProperties\":false"), schemaText)
    assert(schemaText.contains("\"$ref\":\"#/$defs/SampleV1\""), schemaText)
    assert(!schemaText.matches("(?s).*\\\"\\$ref\\\":\\\"(?!#).*"), schemaText)

  private def unknownField[A](result: Either[WireViolations[WireDecodeViolation], A]): (String, String) =
    result.left.toOption.get.head match
      case WireDecodeViolation.UnknownField(path, name, 0) => (path.render, name)
      case other                                           => fail(s"unexpected field failure: $other")

  private def rejectSerialization(value: AnyRef): Unit =
    val bytes  = new ByteArrayOutputStream()
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()
end RecordEnvelopeSuite
