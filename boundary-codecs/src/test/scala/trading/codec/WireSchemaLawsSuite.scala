package trading.codec

import java.lang.reflect.Modifier
import scala.jdk.CollectionConverters.*

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class WireSchemaLawsSuite extends ScalaCheckSuite:
  private final case class Box(value: String)
  private final case class Wrapped(value: Box)
  private final case class Triple(first: String, second: Boolean, third: BigInt)

  private enum Choice:
    case Named(value: String)
    case Count(value: BigInt)

  private val namedRecord  = WireRecord.field("name", WireSchema.text)
  private val countRecord  = WireRecord.field("count", WireSchema.integerLiteral)
  private val choiceSchema =
    WireSchema.tagged[Choice](
      "kind",
      Vector(
        WireCase[Choice, String]("named", namedRecord):
          case Choice.Named(value) => Some(value)
          case _                   =>
            None
        (Choice.Named.apply),
        WireCase[Choice, BigInt]("counted", countRecord):
          case Choice.Count(value) => Some(value)
          case _                   =>
            None
        (Choice.Count.apply)
      )
    )

  property("total invariant mapping obeys identity and composition"):
    forAll: (raw: String) =>
      val value          = raw.filter(character => character >= ' ' && character <= '~').take(100)
      val identitySchema = WireSchema.text.imap(value => value)(value => value)
      val sequential     = WireSchema.text.imap(Box.apply)(_.value).imap(Wrapped.apply)(_.value)
      val composed       = WireSchema.text.imap(value => Wrapped(Box(value)))(_.value.value)

      identitySchema.write(value) == WireSchema.text.write(value) &&
      sequential.write(Wrapped(Box(value))) == composed.write(Wrapped(Box(value))) &&
      sequential.read(sequential.write(Wrapped(Box(value))).toOption.get) == Right(Wrapped(Box(value)))

  property("record products associate under the declared public projection"):
    forAll: (raw: String, flag: Boolean, number: Int) =>
      val value  = Triple(raw.filter(_.isLetterOrDigit).take(40), flag, BigInt(number))
      val first  = WireRecord.field("first", WireSchema.text)
      val second = WireRecord.field("second", WireSchema.boolean)
      val third  = WireRecord.field("third", WireSchema.integerLiteral)
      val left   = WireSchema.record(
        first
          .product(second)
          .product(third)
          .imap(value => Triple(value._1._1, value._1._2, value._2))(value =>
            ((value.first, value.second), value.third)
          )
      )
      val right = WireSchema.record(
        first
          .product(second.product(third))
          .imap(value => Triple(value._1, value._2._1, value._2._2))(value =>
            (value.first, (value.second, value.third))
          )
      )
      val rendered = left.write(value).toOption.get

      rendered == right.write(value).toOption.get && left.read(rendered) == Right(value) &&
      right.read(rendered) == Right(value)

  property("closed tagged sums round-trip without cross-alternative fields"):
    forAll: (raw: String, number: Int) =>
      val values = Vector[Choice](Choice.Named(raw.filter(_.isLetterOrDigit).take(40)), Choice.Count(BigInt(number)))
      values.forall: value =>
        val encoded = choiceSchema.write(value).toOption.get
        choiceSchema.read(encoded) == Right(value) &&
        (value match
          case Choice.Named(_) => !encoded.contains("count") && encoded.contains("name")
          case Choice.Count(_) => !encoded.contains("name") && encoded.contains("count"))

  property("vector traversal preserves value order"):
    forAll: (values: List[Int]) =>
      val bounded = values.take(100).map(BigInt(_)).toVector
      val schema  = WireSchema.vector(WireSchema.integerLiteral)
      schema.read(schema.write(bounded).toOption.get) == Right(bounded)

  test("vector refinement failures accumulate with stable indexed paths"):
    val nonEmpty = WireSchema.text.refine[String]((value, context) =>
      Either.cond(
        value.nonEmpty,
        value,
        WireDecodeViolation.InvalidValue(context.path, "non-empty", context.recordIndex)
      )
    )(identity)
    val errors = WireSchema.vector(nonEmpty).read("""["ok","","fine",""]""").left.toOption.get.toVector

    assertEquals(errors.map(_.path.render), Vector("$[1]", "$[3]"))

  test("generated local-reference Draft 2020-12 schema agrees with the codec"):
    val id         = "urn:trading:codec:schema:choice:v1"
    val schemaText = JsonSchemaDocument.render(id, "ChoiceV1", choiceSchema).toOption.get
    val registry   = SchemaRegistry.withDefaultDialect(
      SpecificationVersion.DRAFT_2020_12,
      builder =>
        val _ = builder.schemas(Map(id -> schemaText).asJava)
        val _ = builder.schemaLoader: loader =>
          val _ = loader.fetchRemoteResources(false)
          ()
    )
    val schema  = registry.getSchema(SchemaLocation.of(id))
    val meta    = registry.getSchema(SchemaLocation.of(SpecificationVersion.DRAFT_2020_12.getDialectId()))
    val valid   = choiceSchema.write(Choice.Named("alpha")).toOption.get
    val invalid = """{"kind":"named","name":"alpha","count":1}"""

    assert(meta.validate(schemaText, InputFormat.JSON).isEmpty,
      meta.validate(schemaText, InputFormat.JSON).asScala.mkString("\n"))
    assert(schema.validate(valid, InputFormat.JSON).isEmpty)
    assert(schema.validate(invalid, InputFormat.JSON).asScala.nonEmpty)
    assert(choiceSchema.read(invalid).isLeft)
    assert(schemaText.contains("\"$ref\":\"#/$defs/ChoiceV1\""))
    assert(!schemaText.matches("(?s).*\\\"\\$ref\\\":\\\"(?!#).*"))
    Vector("maxLength", "maxItems", "maxProperties", "maximum").foreach: operationalKeyword =>
      assert(!schemaText.contains(s"\"$operationalKeyword\""), schemaText)

  test("schema identifiers and local definition names retain their own typed failures"):
    val invalidId = JsonSchemaDocument
      .render("https://example.test/schema", "ChoiceV1", choiceSchema)
      .left
      .toOption
      .get
      .head
    val invalidDefinition = JsonSchemaDocument
      .render("urn:trading:codec:schema:choice:v1", "1 invalid", choiceSchema)
      .left
      .toOption
      .get
      .head

    assertEquals(
      invalidId,
      WireEncodeViolation.InvalidSchemaIdentifier("https://example.test/schema")
    )
    assertEquals(
      invalidDefinition,
      WireEncodeViolation.InvalidSchemaDefinitionName("1 invalid")
    )

  test("public codec foundations expose no Cats, Jackson, validator, or oracle types"):
    val surface = Vector(
      classOf[DecodeLimits],
      classOf[WirePath],
      classOf[WirePathSegment],
      classOf[WireViolations[?]],
      classOf[WireDecodeViolation],
      classOf[WireEncodeViolation],
      classOf[WireLimitViolation],
      classOf[RecordType],
      classOf[SchemaVersion],
      classOf[ExactNumberProblem],
      classOf[StableIdentifierProblem],
      classOf[DimensionProblem],
      classOf[EnvelopeProblem]
    )
    val forbidden  = Vector("cats.", "tools.jackson.", "com.networknt.", "org.erdtman.")
    val signatures = surface.flatMap: clazz =>
      clazz.getDeclaredMethods.toVector.filter(method => Modifier.isPublic(method.getModifiers)).map(_.toGenericString)

    forbidden.foreach(fragment => assert(!signatures.exists(_.contains(fragment)), signatures.mkString("\n")))
end WireSchemaLawsSuite
