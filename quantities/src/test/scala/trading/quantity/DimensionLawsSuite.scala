package trading.quantity

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.testkit.ExactGenerators.*

class DimensionLawsSuite extends ScalaCheckSuite:
  property("normalization sorts atoms, combines duplicates, and removes zero powers"):
    forAll(dimensionPowers): rawPowers =>
      val normalized =
        DimKey:
          rawPowers
      normalized.powers
        .foreach: (atom, power) =>
          val expected = rawPowers.collect { case (`atom`, contribution) => contribution }.sum
          assertEquals(power, expected)
      assertEquals(
        normalized.powers
          .map:
            _._1.value
        ,
        normalized.powers
          .map:
            _._1.value
          .sorted
      )
      assert:
        normalized.powers
          .forall:
            _._2 != 0

  property("one is the multiplication identity"):
    forAll(dimKey): key =>
      assertEquals(DimKey.multiply(key, DimKey.one), key)
      assertEquals(DimKey.multiply(DimKey.one, key), key)

  property("multiplication is associative and commutative"):
    forAll(dimKey, dimKey, dimKey): (a, b, c) =>
      assertEquals(DimKey.multiply(a, b), DimKey.multiply(b, a))
      assertEquals(
        DimKey.multiply(DimKey.multiply(a, b), c),
        DimKey.multiply(a, DimKey.multiply(b, c))
      )

  property("inverse and cancellation laws hold"):
    forAll(dimKey, dimKey): (a, b) =>
      assertEquals(
        DimKey.inverse:
          DimKey.inverse(a)
        ,
        a
      )
      assertEquals(DimKey.multiply(a, DimKey.inverse(a)), DimKey.one)
      assertEquals(DimKey.multiply(DimKey.multiply(a, b), DimKey.inverse(b)), a)

  property("same-dimension evidence is recovered exactly for equal canonical keys"):
    forAll(dimKey, rational): (key, coefficient) =>
      val left =
        DimRef.fresh:
          key
      val right =
        DimRef.fresh:
          key
      val evidence = SameDimension.between(left.dimension, right.dimension)
      val original = Quantity(left.dimension, coefficient)

      assert:
        evidence.nonEmpty
      val coerced = original.alignTo[right.D](using evidence.get)
      assertEquals(coerced.coefficient, coefficient)

  property("same-dimension evidence is rejected for unequal canonical keys"):
    forAll(dimKey): key =>
      val distinct =
        DimKey.multiply(
          key,
          DimKey.atom:
            AtomId:
              "__distinct_atom__"
        )
      val left  = DimRef.fresh(key)
      val right = DimRef.fresh(distinct)
      assertEquals(SameDimension.between(left.dimension, right.dimension), None)

  test("checked core evidence explicitly aligns before additive arithmetic"):
    val left                      = DimRef.atomic(AtomId("checked-local-addition"))
    val right                     = DimRef.atomic(AtomId("checked-local-addition"))
    val evidence                  = SameDimension.between(right.dimension, left.dimension).get
    val aligned: Quantity[left.D] = Quantity(right.dimension, 3).alignTo[left.D](using evidence)

    val sum: Quantity[left.D] = Quantity(left.dimension, 2) + aligned
    assertEquals(sum.coefficient, Rational(5))

end DimensionLawsSuite
