package external

import munit.FunSuite

import trading.quantity.*
import trading.quantity.algebra.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

class ConstructionAndProvenanceBoundarySuite extends FunSuite:

  private val quantum = PositiveRational.exact(1, 100).toOption.get

  private def nullFailureAtRoot(body: => Any): NullPointerException =
    var returned = false

    val failure = intercept[NullPointerException]:
      val _ = body
      returned = true

    assert(!returned)
    failure

  private def rejectsNullAtRoot(body: => Any): Unit =
    val _ = nullFailureAtRoot(body)

  test("UniformGrid rejects null dimension authority before returning a grid reference"):
    type Bad = Dim[Power["construction-boundary", 0] *: EmptyTuple]

    val witness: DimRef[Bad] = null
    var returned             = false

    val _ = intercept[NullPointerException]:
      val grid = UniformGrid.create(
        GridId("null-dimension-authority"),
        GridVersion(1),
        witness,
        quantum
      )
      returned = true
      val _ = grid.fromCoordinate(7)

    assert(!returned)

  test("UniformGrid keeps valid construction generative and exact"):
    val dimension = DimRef.atomic(AtomId("valid-grid-construction"))
    val grid      = UniformGrid.create(
      GridId("valid-grid-construction"),
      GridVersion(1),
      dimension.dimension,
      quantum
    )
    val value: GridQuantity[dimension.D, grid.G] = grid.fromCoordinate(7)
    val sum: GridQuantity[dimension.D, grid.G]   = value + value

    assertEquals(grid.coordinate(value), BigInt(7))
    assertEquals(grid.coordinate(sum), BigInt(14))
    assertEquals(grid.asQuantity(value), Quantity(dimension.dimension, Rational(7, 100)))

  test("nearby DimRef-taking roots already reject null authority"):
    type Bad = Dim[Power["quantity-construction-boundary", 0] *: EmptyTuple]

    val malformed: DimRef[Bad] = null
    val valid: DimRef[One]     = DimRef.one
    val nullOne: DimRef[One]   = null

    val _ = intercept[NullPointerException](Quantity(malformed, Rational.one))
    val _ = intercept[NullPointerException](DimRef.times(nullOne, valid))
    val _ = intercept[NullPointerException](DimRef.inverse(nullOne))
    val _ = intercept[NullPointerException](DimRef.divide(valid, nullOne))
    intercept[NullPointerException](SameDimension.between(nullOne, valid))

  test("alignment and exact comparison reject null SameDimension before returning"):
    type A   = Atom["null-alignment:a"]
    type B   = Atom["null-alignment:b"]
    type Bad = Dim[Power["null-alignment:bad", 0] *: EmptyTuple]

    val a: DimRef[A]                     = DimRef.atom["null-alignment:a"]
    val b: DimRef[B]                     = DimRef.atom["null-alignment:b"]
    val aGrid                            = UniformGrid.create(GridId("null-alignment:a"), GridVersion(1), a, quantum)
    val bGrid                            = UniformGrid.create(GridId("null-alignment:b"), GridVersion(1), b, quantum)
    val quantity                         = Quantity(a, 7)
    val aValue                           = aGrid.fromCoordinate(7)
    val bValue                           = bGrid.fromCoordinate(7)
    val malformed: SameDimension[A, Bad] = null
    val unequal: SameDimension[A, B]     = null

    val failures = List(
      nullFailureAtRoot(quantity.alignTo[Bad](using malformed)),
      nullFailureAtRoot(aValue.alignTo[Bad](using malformed)),
      nullFailureAtRoot(aValue.exactlyEquals(bValue, aGrid, bGrid)(using unequal)),
      nullFailureAtRoot(aValue.compareExact(bValue, aGrid, bGrid)(using unequal))
    )

    failures.foreach(failure => assertEquals(failure.getMessage, "same dimension evidence"))

  test("derived SameDimension retains exact alignment and comparison"):
    type A          = Atom["valid-alignment:a"]
    type Equivalent = Times[A, One]

    val a: DimRef[A]                   = DimRef.atom["valid-alignment:a"]
    val equivalent: DimRef[Equivalent] = DimRef.times(a, DimRef.one)
    val aGrid                          = UniformGrid.create(GridId("valid-alignment:a"), GridVersion(1), a, quantum)
    val equivalentGrid                 = UniformGrid.create(
      GridId("valid-alignment:equivalent"),
      GridVersion(1),
      equivalent,
      quantum
    )
    val quantity        = Quantity(a, 7)
    val aValue          = aGrid.fromCoordinate(7)
    val equivalentValue = equivalentGrid.fromCoordinate(7)
    val alignedQuantity = quantity.alignTo[Equivalent]
    val alignedGrid     = aValue.alignTo[Equivalent]
    val restoredGrid    = alignedGrid.alignTo[A]

    assertEquals(alignedQuantity.coefficient, Rational(7))
    assertEquals(aGrid.coordinate(restoredGrid), BigInt(7))
    assert(aValue.exactlyEquals(equivalentValue, aGrid, equivalentGrid))
    assertEquals(aValue.compareExact(equivalentValue, aGrid, equivalentGrid), 0)

  test("witness-backed carrier roots reject null numeric payloads before returning"):
    val nullCoefficient: Rational = null
    val nullCoordinate: BigInt    = null
    val registry                  = new QuantityRegistry
    val dimension                 = registry.registerDimension(DimensionKey.one).toOption.get
    val grid                      = registry
      .registerGrid(dimension):
        GridDefinition(
          dimension.dimension.key,
          GridId("null-numeric-payload"),
          GridVersion(1),
          quantum
        )
      .toOption
      .get

    rejectsNullAtRoot(Quantity(DimRef.one, nullCoefficient))
    rejectsNullAtRoot(grid.fromCoordinate(nullCoordinate))
    rejectsNullAtRoot:
      PackedGridQuantity.decode(
        PackedGridQuantity(
          dimension.dimension.key,
          grid.id,
          grid.version,
          nullCoordinate
        ),
        registry
      )

  test("valid numeric payloads retain exact construction and checked decoding"):
    val quantity  = Quantity(DimRef.one, Rational(7, 3))
    val registry  = new QuantityRegistry
    val dimension = registry.registerDimension(DimensionKey.one).toOption.get
    val grid      = registry
      .registerGrid(dimension):
        GridDefinition(
          dimension.dimension.key,
          GridId("valid-numeric-payload"),
          GridVersion(1),
          quantum
        )
      .toOption
      .get
    val coordinate = grid.fromCoordinate(BigInt(7))
    val decoded    = PackedGridQuantity
      .decode(
        PackedGridQuantity(
          dimension.dimension.key,
          grid.id,
          grid.version,
          BigInt(11)
        ),
        registry
      )
      .toOption
      .get

    assertEquals(quantity.coefficient, Rational(7, 3))
    assertEquals(grid.coordinate(coordinate), BigInt(7))
    assertEquals(grid.asQuantity(coordinate).coefficient, Rational(7, 100))
    assertEquals(decoded.grid.coordinate(decoded.value), BigInt(11))

  test("DimRef authority rejects null before manufacturing dimensional identities"):
    type Bad = Dim[Power["null-dimension-authority", 0] *: EmptyTuple]
    type G   = "null-dimension-grid"

    val malformed: DimRef[Bad] = null

    rejectsNullAtRoot(Quantity.zero[Bad](using malformed))
    rejectsNullAtRoot(GridQuantity.zero[Bad, G](using malformed))
    rejectsNullAtRoot(NonNegative.quantityZero[Bad](using malformed))
    rejectsNullAtRoot(NonNegative.gridQuantityZero[Bad, G](using malformed))
    rejectsNullAtRoot(exactQuantityAlgebra.quantityVectorSpace[Bad](using malformed))
    rejectsNullAtRoot(gridQuantityAlgebra.gridModule[Bad, G](using malformed))
    rejectsNullAtRoot(refinedAdditive.nonNegativeQuantityMonoid[Bad](using malformed))
    rejectsNullAtRoot(refinedAdditive.nonNegativeGridQuantityMonoid[Bad, G](using malformed))

  test("authoritative witnesses retain zero, expression, rate, and algebra construction"):
    type G = "authoritative-dimension-grid"

    val quantityZero   = Quantity.zero[One](using DimRef.one)
    val gridZero       = GridQuantity.zero[One, G](using DimRef.one)
    val one            = Quantity(DimRef.one, Rational(2))
    val product        = one * one
    val witness        = DimRef.times(DimRef.one, DimRef.one)
    val rate           = Rate.identity(DimRef.one)
    val vectorSpace    = exactQuantityAlgebra.quantityVectorSpace[One](using DimRef.one)
    val gridModule     = gridQuantityAlgebra.gridModule[One, G](using DimRef.one)
    val quantityMonoid = refinedAdditive.nonNegativeQuantityMonoid[One](using DimRef.one)
    val gridMonoid     = refinedAdditive.nonNegativeGridQuantityMonoid[One, G](using DimRef.one)

    assertEquals(quantityZero.coefficient, Rational.zero)
    assert(gridZero.sameGridEquals(GridQuantity.zero[One, G](using DimRef.one)))
    assertEquals(product.coefficient, Rational(4))
    assertEquals(witness.key, DimensionKey.one)
    assertEquals(rate.coefficient, Rational.one)
    assertEquals(vectorSpace.zero.coefficient, Rational.zero)
    assert(gridModule.zero.sameGridEquals(GridQuantity.zero[One, G](using DimRef.one)))
    assertEquals(quantityMonoid.zero.unrefined.coefficient, Rational.zero)
    assert(gridMonoid.zero.unrefined.sameGridEquals(GridQuantity.zero[One, G](using DimRef.one)))

  test("runtime identity roots reject null before returning authority"):
    val registry = new QuantityRegistry

    rejectsNullAtRoot(DimensionKey.atom(null))
    rejectsNullAtRoot(DimensionKey(Vector((null: AtomId) -> BigInt(1))))
    rejectsNullAtRoot(DimensionKey(Vector((null: AtomId) -> BigInt(0))))
    rejectsNullAtRoot:
      DimensionKey(Vector((null: AtomId) -> BigInt(1), (null: AtomId) -> BigInt(-1)))

    val nullPower: BigInt = null
    rejectsNullAtRoot(DimensionKey(Vector(AtomId("null-dimension-power") -> nullPower)))

    rejectsNullAtRoot(DimRef.fresh(null))
    rejectsNullAtRoot(DimRef.atomic(null))
    rejectsNullAtRoot(new DimRef.NominalAtom(null) {})
    rejectsNullAtRoot(DimensionKey.multiply(null, DimensionKey.one))
    rejectsNullAtRoot(DimensionKey.multiply(DimensionKey.one, null))
    rejectsNullAtRoot(DimensionKey.inverse(null))
    rejectsNullAtRoot(GridKey(null, GridVersion(1)))
    rejectsNullAtRoot(GridKey(GridId("null-grid-version"), null))
    rejectsNullAtRoot(UniformGrid.create(null, GridVersion(1), DimRef.one, quantum))
    rejectsNullAtRoot(UniformGrid.create(GridId("null-grid-version"), null, DimRef.one, quantum))
    rejectsNullAtRoot(registry.registerDimension(null))
    rejectsNullAtRoot:
      registry.registerAsset(AssetDefinition(null, AtomId("null-asset-id")))
    rejectsNullAtRoot:
      registry.registerAsset(AssetDefinition(AssetId("null-asset-atom"), null))

    val dimension = registry.registerDimension(DimensionKey.one).toOption.get
    rejectsNullAtRoot:
      GridDefinition(dimension.dimension.key, null, GridVersion(1), quantum)

  test("valid key, witness, registry, quantity, and grid roots remain exact"):
    val atom        = AtomId("valid-authority-root")
    val key         = DimensionKey(Vector(atom -> BigInt(2), atom -> BigInt(-1)))
    val cancelled   = DimensionKey(Vector(atom -> BigInt(1), atom -> BigInt(-1)))
    val firstFresh  = DimRef.fresh(key)
    val secondFresh = DimRef.fresh(key)
    val atomic      = DimRef.atomic(AtomId("valid-atomic-root"))
    val nominal     = DimRef.atom(ValidBoundaryNominal)
    val quantity    = Quantity(firstFresh.dimension, 1)

    val registry            = new QuantityRegistry
    val registeredDimension = registry.registerDimension(key).toOption.get
    val asset               = registry
      .registerAsset(AssetDefinition(AssetId("valid-registry-asset"), AtomId("valid-registry-atom")))
      .toOption
      .get
    val registeredGrid = registry
      .registerGrid(registeredDimension):
        GridDefinition(
          registeredDimension.dimension.key,
          GridId("valid-registry-grid"),
          GridVersion(1),
          quantum
        )
      .toOption
      .get
    val coordinate = registeredGrid.fromCoordinate(7)

    assertEquals(key, DimensionKey.atom(atom))
    assertEquals(cancelled, DimensionKey.one)
    assertEquals(quantity.coefficient, Rational.one)
    assertEquals(registeredGrid.coordinate(coordinate), BigInt(7))
    assertEquals(registeredGrid.asQuantity(coordinate),
      Quantity(registeredDimension.dimension.asDimensionRef, Rational(7, 100)))
    assert(SameDimension.between(firstFresh.dimension, secondFresh.dimension).nonEmpty)
    assert(atomic.atomId != null)
    assert(nominal.key != null)
    assert(asset.id != null)
    assertEquals(registry.registeredDimensionCount, 2)
    assertEquals(registry.registeredGridCount, 1)

end ConstructionAndProvenanceBoundarySuite

private object ValidBoundaryNominal extends DimRef.NominalAtom(AtomId("valid-nominal-root"))
