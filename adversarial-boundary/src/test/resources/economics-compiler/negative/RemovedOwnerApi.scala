package external.economics.negative

import trading.economics.*

object RemovedOwnerApi:
  def reject(instrument: Instrument, lots: instrument.Lots): Unit =
    val _ = lots.instrumentId

    // OFFENDING-BEGIN
    val _ = instrument.ownerAuthority
    val _: instrument.Owner = ???
    val _: Instrument.OwnerAuthority[String] = ???
    val _: JvmOwnerAuthority = ???
    // OFFENDING-END

end RemovedOwnerApi
