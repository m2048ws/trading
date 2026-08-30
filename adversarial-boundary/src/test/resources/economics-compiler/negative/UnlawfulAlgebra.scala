package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*

object UnlawfulAlgebra:
  val observed = instrument.identity.id

  // OFFENDING-BEGIN
  val positionGroup = summon[algebra.ring.AdditiveCommutativeGroup[instrument.PositionLots]]
  val pnlMonoid = summon[cats.kernel.Monoid[instrument.Pnl]]
  // OFFENDING-END
end UnlawfulAlgebra
