package external.economics.core

import trading.economics.instrument.Instrument
import trading.quantity.Quantity

object InstrumentDimensionAliasMismatch:
  def retain[I <: Instrument](
    instrument: I
  )(value: Quantity[instrument.PositionD]): Quantity[instrument.PositionD] =
    value

  // OFFENDING-BEGIN
  def crossRole[I <: Instrument](
    instrument: I
  )(value: Quantity[instrument.BaseD]): Quantity[instrument.PositionD] =
    value

  def crossInstrument[I <: Instrument, J <: Instrument](
    source: I,
    target: J
  )(value: Quantity[source.PositionD]): Quantity[target.PositionD] =
    value
  // OFFENDING-END
end InstrumentDimensionAliasMismatch
