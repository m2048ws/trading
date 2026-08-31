package trading.risk

import cats.kernel.Order

import trading.economics.instrument.Instrument
import trading.quantity.*
import trading.quantity.algebra.exactOrders.given
import trading.quantity.refinement.*

/** Exact downside-risk measurements over already validated pure instrument economics. */
object Risk:

  def downside(
    instrument: Instrument
  )(
    pnl: instrument.Pnl
  ): Either[RiskIdentityError, NonNegative[Quantity[instrument.roles.settle.D]]] =
    if pnl.instrumentId != instrument.identity.id then
      Left(DownsideInstrumentMismatch(instrument.identity.id, pnl.instrumentId))
    else
      Right(
        downsideQuantity(pnl.netPnl)(using instrument.roles.settle.dimension.ref)
      )

  private def downsideQuantity[D <: Dim](
    netPnl: Quantity[D]
  )(using DimRef[D]
  ): NonNegative[Quantity[D]] =
    val zero = Quantity.zero[D]
    if Order[Quantity[D]].lt(netPnl, zero) then
      NonNegative(zero - netPnl).fold(
        _ => throw new IllegalStateException("typed downside negation violated nonnegative closure"),
        identity
      )
    else NonNegative.quantityZero[D]
end Risk
