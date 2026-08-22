package external.fixtures.negative

import algebra.ring.AdditiveCommutativeMonoid

import trading.quantity.*
import trading.quantity.algebra.*
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.gridQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.grid.*
import trading.quantity.refinement.*

object MalformedCarrierConstruction:
  type Bad = Dim[Power["construction:bad", 0] *: EmptyTuple]
  sealed trait G

  val validQuantity: Quantity[One] = Quantity(DimRef.one, 1)
  val validGrid                    = UniformGrid.create(
    GridId("construction-valid"),
    GridVersion(1),
    DimRef.one,
    PositiveRational.exact(1, 100).toOption.get
  )

  // OFFENDING-BEGIN
  val quantityZero: Quantity[Bad]                                   = Quantity.zero[Bad]
  val gridZero: GridQuantity[Bad, G]                                = GridQuantity.zero[Bad, G]
  val refinedQuantityZero: NonNegative[Quantity[Bad]]               = NonNegative.quantityZero[Bad]
  val refinedGridZero: NonNegative[GridQuantity[Bad, G]]            = NonNegative.gridQuantityZero[Bad, G]
  val rawQuantity: Quantity[Bad]                                    = Rational.one
  val rawCoordinate: GridQuantity[Bad, G]                           = BigInt(1)
  val dimensionWitness: DimRef[Bad]                                 = summon
  val alignedQuantity: Quantity[Bad]                                = validQuantity.alignTo[Bad]
  val alignedGrid: GridQuantity[Bad, validGrid.G]                   = validGrid.fromCoordinate(1).alignTo[Bad]
  val badGridWitness: GridRef[Bad]                                  = validGrid
  val refinedQuantity: NonNegative[Quantity[Bad]]                   = NonNegative(validQuantity).toOption.get
  val vector: VectorSpace[Quantity[Bad], Rational]                  = summon
  val module: LeftModule[GridQuantity[Bad, G], BigInt]              = summon
  val monoid: AdditiveCommutativeMonoid[NonNegative[Quantity[Bad]]] = summon
  // OFFENDING-END

end MalformedCarrierConstruction
