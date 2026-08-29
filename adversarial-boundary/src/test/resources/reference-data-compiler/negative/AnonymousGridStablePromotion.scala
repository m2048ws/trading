package trading.reference

import trading.quantity.*
import trading.quantity.refinement.*

object AnonymousGridStablePromotion:
  val quantum   = PositiveRational.exact(1, 100).fold(error => throw new AssertionError(error.toString), identity)
  val anonymous = UniformGrid.create(DimRef.one, quantum)

  // OFFENDING-BEGIN
  val promoted: GridHandle[One] = anonymous
  // OFFENDING-END

end AnonymousGridStablePromotion
