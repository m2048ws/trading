package trading.quantity

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class AdversarialConstructionBoundarySuite extends FunSuite:

  test("callers cannot pair one static atom tag with unrelated runtime atom IDs"):
    assertDoesNotCompile:
      """
      import trading.quantity.*

      val first = DimRef.atomic(AtomId("atom:first"))
      val second = DimRef.atomic(AtomId("atom:second"))
      val left = Quantity(first.dimension, 1)
      val right = Quantity(second.dimension, 2)
      val invalid = left + right
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*

      val first = DimRef.atomic(AtomId("atom:first"))
      val second = DimRef.atomic(AtomId("atom:second"))
      val third = DimRef.atomic(AtomId("atom:third"))
      val value = Quantity(second.dimension, 1)
      val invalidRate = Rate(first.dimension, third.dimension, Rational.one)
      val invalid = value.applyRate(invalidRate)
    """

  test("separately obtained atom witnesses recover compatibility only through checked evidence"):
    val first     = DimRef.atomic(AtomId("atom:canonical"))
    val second    = DimRef.atomic(AtomId("atom:canonical"))
    val different = DimRef.atomic(AtomId("atom:different"))
    assert(SameDimension.between(first.dimension, second.dimension).nonEmpty)
    assertEquals(SameDimension.between(first.dimension, different.dimension), None)

  test("same-dimension evidence cannot be implemented by downstream source"):
    assertDoesNotCompile:
      """
      import trading.quantity.*

      val forged = new SameDimension[One, One]
    """

  test("private and opaque construction paths are unavailable to package-spoofing source"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val forged = new Sign[Rational](_ => 1)
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      val forged: Quantity[One] = Rational.one
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      sealed trait GridTag
      val forged: GridQuantity[One, GridTag] = BigInt(1)
    """

  test("supported algebra imports do not expose floating Rational construction"):
    assertDoesNotCompile:
      """
      import algebra.ring.Field
      import trading.quantity.algebra.exactQuantityAlgebra.given
      import trading.quantity.Rational

      val forged = summon[Field[Rational]].fromDouble(0.1d)
    """

    assertDoesNotCompile:
      """
      import algebra.ring.Field
      import trading.quantity.algebra.exactQuantityAlgebra.given
      import trading.quantity.Rational

      val forged = summon[Field[Rational]].fromFloat(0.1f)
    """

  test("ephemeral core asset construction is not a public competing authority"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val asset = Asset.runtime(AssetId("ephemeral"))
    """

  test("canonical core values do not advertise default Java serialization"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val serialized: java.io.Serializable = Rational.one
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      val serialized: java.io.Serializable = DimKey.one
    """

end AdversarialConstructionBoundarySuite
