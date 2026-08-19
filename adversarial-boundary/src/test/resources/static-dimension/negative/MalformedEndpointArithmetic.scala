package external.fixtures.negative

import trading.quantity.*
import trading.quantity.refinement.*

object MalformedEndpointArithmetic:
  type Bad   = Dim[Power["endpoint:bad", 0] *: EmptyTuple]
  type Good  = Atom["endpoint:good"]
  type Other = Atom["endpoint:other"]

  // OFFENDING-BEGIN
  def applied(amount: Quantity[Bad], rate: Rate[Bad, Good]): Quantity[Good] =
    amount.applyRate(rate)

  def composed(first: Rate[Good, Bad], second: Rate[Bad, Other]): Rate[Good, Other] =
    first.andThen(second)

  def ratio(amount: Quantity[Bad], divisor: NonZero[Quantity[Bad]]): Ratio =
    amount.ratioTo(divisor)

  def cross(
    numerator: Rate[Good, Bad],
    denominator: NonZero[Rate[Other, Bad]]
  ): Rate[Good, Other] =
    numerator.crossRate(denominator)
  // OFFENDING-END

end MalformedEndpointArithmetic
