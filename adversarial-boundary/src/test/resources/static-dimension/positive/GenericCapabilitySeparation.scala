package external.fixtures.positive

import trading.quantity.*

object GenericCapabilitySeparation:
  def preservingArithmetic[D <: Dim](
    dimension: DimRef[D],
    left: Quantity[D],
    right: Quantity[D]
  ): (DimKey, Quantity[D], Quantity[D]) =
    (
      dimension.key,
      left + right,
      left * Rational(2)
    )

  type USD = Atom["capability:USD"]

  val usd: DimRef[USD]                                     = DimRef.atom["capability:USD"]
  val result: (DimKey, Quantity[USD], Quantity[USD]) =
    preservingArithmetic(usd, Quantity(usd, 2), Quantity(usd, 3))

end GenericCapabilitySeparation
