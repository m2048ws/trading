package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*

object RemovedCapabilityPaths:
  val observed = instrument.identity

  // OFFENDING-BEGIN
  val prices = instrument.prices
  val market = instrument.market
  val orders = instrument.orders
  val scenarios = instrument.scenarios
  val fees = instrument.fees
  val valuation = instrument.valuation
  val sizing = instrument.sizing
  // OFFENDING-END
end RemovedCapabilityPaths
