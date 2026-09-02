package trading.codec

import munit.FunSuite
import org.erdtman.jcs.JsonCanonicalizer

class CanonicalJsonSuite extends FunSuite:
  test("restricted canonical rendering agrees with the independent JCS oracle"):
    val input  = """{"z":"€","a":[3,true,null],"emoji":"😀","escaped":"line\nfeed"}"""
    val parsed = StrictJson.parse(input).toOption.get
    val actual = CanonicalJson.render(parsed).toOption.get
    val oracle = new JsonCanonicalizer(input).getEncodedString()

    assertEquals(actual, oracle)
    assertEquals(actual, """{"a":[3,true,null],"emoji":"😀","escaped":"line\nfeed","z":"€"}""")

  test("object members use unsigned UTF-16 code-unit ordering"):
    val keys        = Vector("דּ", "1", "😀", "\r", "€", "ö", "\u0080")
    val node        = JsonNode.obj(keys.map(key => key -> JsonNode.string(key))*)
    val rendered    = CanonicalJson.render(node).toOption.get
    val oracleInput = keys.reverse.map(key => s"${quoteForInput(key)}:${quoteForInput(key)}").mkString("{", ",", "}")

    assertEquals(rendered, new JsonCanonicalizer(oracleInput).getEncodedString())
    val positions = Vector("\\r", "1", "\u0080", "ö", "€", "😀", "דּ").map(rendered.indexOf)
    assertEquals(positions, positions.sorted)

  test("JCS escaping is minimal, lowercase, and whitespace-free"):
    val value    = "\u0000\b\t\n\f\r\"\\/"
    val rendered = CanonicalJson.render(JsonNode.string(value)).toOption.get
    assertEquals(rendered, "\"\\u0000\\b\\t\\n\\f\\r\\\"\\\\/\"")

  test("malformed Unicode, duplicate members, and unsupported number spellings are typed"):
    val malformed = CanonicalJson.render(JsonNode.string("\uD800")).left.toOption.get.head
    assert(malformed.isInstanceOf[WireEncodeViolation.MalformedUnicode])

    val duplicate = CanonicalJson
      .render(JsonNode.obj("a" -> JsonNode.bool(true), "a" -> JsonNode.bool(false)))
      .left
      .toOption
      .get
      .head
    assertEquals(duplicate, WireEncodeViolation.DuplicateMember(WirePath.root.field("a"), "a"))

    val number = CanonicalJson.render(JsonNode.number("1.0")).left.toOption.get.head
    assertEquals(number, WireEncodeViolation.UnsupportedNumber(WirePath.root, "1.0"))

  private def quoteForInput(value: String): String =
    value.flatMap:
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\r' => "\\r"
      case char => char.toString
    .prepended('"')
      .appended('"')
end CanonicalJsonSuite
