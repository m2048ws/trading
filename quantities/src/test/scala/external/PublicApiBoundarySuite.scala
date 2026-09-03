package external

import munit.FunSuite

import trading.quantity.testkit.CompileAssertions.*

class PublicApiBoundarySuite extends FunSuite:
  test("uniform mathematical grid construction has no stable-identity overload"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      val dimension = DimRef.atomic(AtomId("anonymous-grid-signature"))
      val quantum = PositiveRational.exact(1, 100).toOption.get
      val invalid = UniformGrid.create(dimension.dimension, quantum, 1)
    """

  test("canonical value constructors cannot be bypassed through case-class helpers"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      val nonCanonical = Rational.fromProduct((BigInt(2), BigInt(2)))
    """

    assertDoesNotCompile:
      """
      import trading.quantity.*
      val nonCanonical = DimKey.fromProduct(
        Tuple1(Vector(AtomId("duplicate") -> 1, AtomId("duplicate") -> 1))
      )
    """

  test("client code cannot forge normalized operation results or obtain an unequal conversion"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type Normalized = Canonical[Power["normalized", 1] *: EmptyTuple]
      val forged: Quantity[Normalized] = Rational.one
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      type A = Atom["conversion:A"]
      type B = Atom["conversion:B"]
      summon[Conversion[Quantity[A], Quantity[B]]]
      """

end PublicApiBoundarySuite
