package trading.codec

import munit.FunSuite

class WireDiagnosticsSuite extends FunSuite:
  test("paths are immutable structured field/index products"):
    val root   = WirePath.root
    val nested = root.field("orders").index(3).field("odd key")

    assertEquals(root.segments, Vector.empty)
    assertEquals(nested.render, "$.orders[3]['odd key']")
    assertEquals(nested.segments.flatMap(_.fieldName), Vector("orders", "odd key"))
    assertEquals(nested.segments.flatMap(_.arrayIndex), Vector(3))

  test("decode violations sort by record, stage, structured path, then typed branch"):
    val laterRecord = WireDecodeViolation.InvalidValue(WirePath.root.field("a"), "later", 2)
    val laterStage  = WireDecodeViolation.InvalidValue(WirePath.root.field("a"), "refinement", 0)
    val laterPath   = WireDecodeViolation.UnknownField(WirePath.root.field("z"), "z", 0)
    val earlierPath = WireDecodeViolation.UnknownField(WirePath.root.field("a"), "a", 0)
    val inputLimit  = WireDecodeViolation.Limit(
      WireLimitViolation(DecodeLimit.PayloadCharacters, 11, 10, WirePath.root, 0)
    )

    val ordered =
      WireViolations.orderedDecode(Vector(laterRecord, laterStage, laterPath, earlierPath, inputLimit)).toVector
    assertEquals(ordered, Vector(inputLimit, earlierPath, laterPath, laterStage, laterRecord))

  test("non-empty aggregates preserve explicit order through mapping and concatenation"):
    val first  = WireViolations.one(1)
    val second = WireViolations.fromVector(Vector(2, 3)).get

    assertEquals(first.concat(second).map(_.toString).toVector, Vector("1", "2", "3"))
    assertEquals(WireViolations.fromVector(Vector.empty[Int]), None)

end WireDiagnosticsSuite
