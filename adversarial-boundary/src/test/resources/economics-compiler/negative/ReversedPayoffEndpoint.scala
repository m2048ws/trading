package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.quantity.Rate

object ReversedPayoffEndpoint:
  val coefficient = spec.basePerPosition.coefficient

  // OFFENDING-BEGIN
  val reversed: Rate[spec.roles.base.D, spec.roles.position.D] = spec.basePerPosition
  // OFFENDING-END
end ReversedPayoffEndpoint
