package trading.quantity

import munit.FunSuite

class DimensionExponentBoundarySuite extends FunSuite:

  test("dimension exponent addition is unbounded beyond Int.MaxValue"):
    val atom = AtomId("dimension:max-boundary")
    val key  = DimKey(List(atom -> BigInt(Int.MaxValue), atom -> BigInt(1)))
    assert(key.powers.head._2 == BigInt(Int.MaxValue) + 1)

  test("dimension exponent addition and inversion are unbounded below Int.MinValue"):
    val atom = AtomId("dimension:min-boundary")
    val key  = DimKey(List(atom -> BigInt(Int.MinValue), atom -> BigInt(-1)))
    assert(key.powers.head._2 == BigInt(Int.MinValue) - 1)
    assert(
      DimKey.inverse(DimKey(List(atom -> BigInt(Int.MinValue)))).powers.head._2 ==
        -BigInt(Int.MinValue)
    )

  test("power composition, inverse, zero, and cancellation laws hold beyond Int limits"):
    val atom     = AtomId("dimension:power-laws")
    val x        = BigInt(Int.MaxValue)
    val y        = BigInt(1)
    val ax       = DimKey(List(atom -> x))
    val ay       = DimKey(List(atom -> y))
    val combined = DimKey(List(atom -> (x + y)))

    assertEquals(DimKey.multiply(ax, ay), combined)
    assertEquals(DimKey.inverse(combined), DimKey(List(atom -> -(x + y))))
    assertEquals(DimKey(List(atom -> BigInt(0))), DimKey.one)
    assertEquals(DimKey.multiply(combined, DimKey.inverse(combined)), DimKey.one)

end DimensionExponentBoundarySuite
