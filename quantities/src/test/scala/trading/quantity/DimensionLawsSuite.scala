package trading.quantity

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.testkit.ExactGenerators.*

class DimensionLawsSuite extends ScalaCheckSuite:
  property("normalization sorts atoms, combines duplicates, and removes zero powers"):
    forAll(dimensionPowers): rawPowers =>
      val normalized =
        DimensionKey:
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
    forAll(dimensionKey): key =>
      assertEquals(DimensionKey.multiply(key, DimensionKey.one), key)
      assertEquals(DimensionKey.multiply(DimensionKey.one, key), key)

  property("multiplication is associative and commutative"):
    forAll(dimensionKey, dimensionKey, dimensionKey): (a, b, c) =>
      assertEquals(DimensionKey.multiply(a, b), DimensionKey.multiply(b, a))
      assertEquals(
        DimensionKey.multiply(DimensionKey.multiply(a, b), c),
        DimensionKey.multiply(a, DimensionKey.multiply(b, c))
      )

  property("inverse and cancellation laws hold"):
    forAll(dimensionKey, dimensionKey): (a, b) =>
      assertEquals(
        DimensionKey.inverse:
          DimensionKey.inverse(a)
        ,
        a
      )
      assertEquals(DimensionKey.multiply(a, DimensionKey.inverse(a)), DimensionKey.one)
      assertEquals(DimensionKey.multiply(DimensionKey.multiply(a, b), DimensionKey.inverse(b)), a)

  property("same-dimension evidence is recovered exactly for equal canonical keys"):
    forAll(dimensionKey, rational): (key, coefficient) =>
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
    forAll(dimensionKey): key =>
      val distinct =
        DimensionKey.multiply(
          key,
          DimensionKey.atom:
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
