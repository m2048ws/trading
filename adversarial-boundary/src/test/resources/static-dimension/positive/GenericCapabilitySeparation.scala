package external.fixtures.positive

import trading.quantity.*

object GenericCapabilitySeparation:
  def preservingArithmetic[D <: Dimension](
    dimension: DimRef[D],
    left: Quantity[D],
    right: Quantity[D]
  )(using Normalize[D]
  ): (DimensionKey, Quantity[D], Quantity[D], Quantity[D]) =
    (
      dimension.key,
      Quantity.zero[D],
      left + right,
      left * Rational(2)
    )

  type USD = Atom["capability:USD"]

  val usd: DimRef[USD]                                                    = DimRef.atom["capability:USD"]
  val result: (DimensionKey, Quantity[USD], Quantity[USD], Quantity[USD]) =
    preservingArithmetic(usd, Quantity(usd, 2), Quantity(usd, 3))

end GenericCapabilitySeparation
