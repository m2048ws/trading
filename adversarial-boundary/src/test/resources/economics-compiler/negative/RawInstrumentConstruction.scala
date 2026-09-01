package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*

object RawInstrumentConstruction:
  val assembled = Instrument.fromSpec(spec)

  // OFFENDING-BEGIN
  val raw = Instrument.fromSpec(definition)
  val snapshotValue = Instrument.fromSpec(snapshot)
  val removed = Instrument.create(definition)
  // OFFENDING-END
end RawInstrumentConstruction
