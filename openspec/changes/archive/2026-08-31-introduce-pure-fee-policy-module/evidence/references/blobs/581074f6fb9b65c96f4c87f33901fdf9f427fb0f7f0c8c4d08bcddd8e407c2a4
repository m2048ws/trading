package trading.fee

import java.util.Objects

import trading.quantity.*
import trading.quantity.refinement.*

/** Exact fee-policy scalar. A positive quoted rate is a charge; a negative quoted rate is a rebate. */
opaque type FeeRate = Rational

object FeeRate:
  def apply(coefficient: Rational): FeeRate =
    Objects.requireNonNull(coefficient, "fee rate coefficient")

  extension (rate: FeeRate)
    def coefficient: Rational =
      rate
end FeeRate

/** Total exact fee formulas over already-refined typed quantities. */
object FeeCalculation:

  /** Return the account-perspective contribution: a positive quoted rate produces a negative charge. */
  def percentage[D <: Dim](basis: NonNegative[Quantity[D]], rate: FeeRate): Quantity[D] =
    basis.unrefined * -rate.coefficient

  /** Raise only a smaller-magnitude charge to the supplied minimum; preserve rebates, zero, and larger charges. */
  def minimumCharge[D <: Dim](
    contribution: Quantity[D],
    minimum: NonNegative[Quantity[D]]
  ): Quantity[D] =
    val minimumQuantity = minimum.unrefined
    if contribution.coefficient.signum < 0 &&
      contribution.coefficient.abs.compare(minimumQuantity.coefficient) < 0
    then minimumQuantity * -Rational.one
    else contribution
end FeeCalculation
