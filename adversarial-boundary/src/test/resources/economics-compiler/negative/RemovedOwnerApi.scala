package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*

object RemovedOwnerApi:
  val observed = instrument.identity

  // OFFENDING-BEGIN
  val authority = instrument.ownerAuthority
  val owner: instrument.Owner = ???
  val genericAuthority: Instrument.OwnerAuthority = ???
  val jvmAuthority: JvmOwnerAuthority = ???
  // OFFENDING-END
end RemovedOwnerApi
