package external

import munit.FunSuite

import trading.quantity.*
import trading.quantity.algebra.*
import trading.quantity.refinement.*

class ConstructionAndProvenanceBoundarySuite extends FunSuite:

  private val quantum = PositiveRational.exact(1, 100).toOption.get

  private def rejectsNullAtRoot(body: => Any): Unit =
    var returned = false
    val _        = intercept[NullPointerException]:
      val _ = body
      returned = true
    assert(!returned)

  test("UniformGrid rejects null authority and keeps valid construction generative and exact"):
    type Bad = Canonical[Power["construction-boundary", 0] *: EmptyTuple]
    val malformed: DimRef[Bad] = null
    rejectsNullAtRoot(UniformGrid.create(malformed, quantum))

    val dimension                                = DimRef.atomic(AtomId("valid-grid-construction"))
    val grid                                     = UniformGrid.create(dimension.dimension, quantum)
    val value: GridQuantity[dimension.D, grid.G] = grid.fromCoordinate(7)
    assertEquals(grid.coordinate(value + value), BigInt(14))
    assertEquals(grid.asQuantity(value), Quantity(dimension.dimension, Rational(7, 100)))

  test("witness-backed quantity and grid roots reject null numeric payloads"):
    val nullCoefficient: Rational = null
    val nullCoordinate: BigInt    = null
    val grid                      = UniformGrid.create(DimRef.one, quantum)

    rejectsNullAtRoot(Quantity(DimRef.one, nullCoefficient))
    rejectsNullAtRoot(grid.fromCoordinate(nullCoordinate))

  test("alignment roots reject null SameDimension evidence"):
    type A   = Atom["null-alignment:a"]
    type Bad = Canonical[Power["null-alignment:bad", 0] *: EmptyTuple]
    val a: DimRef[A]                     = DimRef.atom["null-alignment:a"]
    val grid                             = UniformGrid.create(a, quantum)
    val malformed: SameDimension[A, Bad] = null

    rejectsNullAtRoot(Quantity(a, 7).alignTo[Bad](using malformed))
    rejectsNullAtRoot(grid.fromCoordinate(7).alignTo[Bad](using malformed))

  test("generic and concrete anonymous-grid clients retain supported authority"):
    def double[D <: Dim, G](grid: GridRef.Grid[D, G], value: GridQuantity[D, G]): Quantity[D] =
      grid.asQuantity(value + value)

    val grid  = UniformGrid.create(DimRef.one, quantum)
    val value = grid.fromCoordinate(21)
    assertEquals(double(grid, value).coefficient, Rational(42, 100))
    assertEquals(Quantity.zero[One](using DimRef.one).coefficient, Rational.zero)
    assertEquals(
      exactQuantityAlgebra.quantityVectorSpace[One](using DimRef.one).zero.coefficient,
      Rational.zero
    )

end ConstructionAndProvenanceBoundarySuite
