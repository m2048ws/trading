package trading.risk

import scala.annotation.tailrec

import cats.kernel.Order

import trading.economics.instrument.*
import trading.fee.policy.FeePolicy
import trading.quantity.*
import trading.quantity.algebra.exactOrders.given
import trading.quantity.refinement.*
import trading.scenario.*

/** Transitional exhaustive fee/scenario service retained only until S-03 downstream migration. */
final class TransitionalRisk[I <: Instrument] private[risk] (val feePolicy: FeePolicy[I]):

  val instrument: feePolicy.instrument.type = feePolicy.instrument

  def downsideRisk(
    pnl: instrument.Pnl
  ): Either[RiskError, NonNegative[Quantity[instrument.roles.settle.D]]] =
    Risk.downside(instrument)(pnl).left.map: mismatch =>
      RiskInstrumentMismatch("pnl", mismatch.expected, mismatch.supplied)

  def maxLots(
    riskBudget: NonNegative[Quantity[instrument.roles.settle.D]],
    cap: PositiveWhole,
    schedule: feePolicy.Schedule
  )(
    scenarioFor: instrument.Lots => Either[RoundTripViolation, feePolicy.RoundTrip]
  ): Either[RiskError, Option[instrument.Lots]] =
    @tailrec
    def loop(
      candidate: BigInt,
      selected: Option[instrument.Lots]
    ): Either[RiskError, Option[instrument.Lots]] =
      if candidate > cap.unrefined then Right(selected)
      else
        val evaluated =
          for
            lots     <- Lots.fromCount(instrument)(candidate).left.map(RiskLotFailure(_))
            scenario <- scenarioFor(lots).left.map(RiskScenarioFailure(_))
            _        <- Either.cond(
                   scenario.instrumentId == instrument.identity.id,
                   (),
                   RiskInstrumentMismatch("scenario", instrument.identity.id, scenario.instrumentId)
                 )
            _ <- Either.cond(
                   scenario.heldPosition.coordinate.abs == candidate,
                   (),
                   SizingScenarioMismatch(candidate, scenario.heldPosition.coordinate)
                 )
            pnl  <- feePolicy.pnl(scenario, schedule).left.map(RiskFeePolicyFailure(_))
            risk <- downsideRisk(pnl)
          yield lots -> risk

        evaluated match
          case Left(error)             => Left(error)
          case Right((lots, measured)) =>
            val next =
              if Order[Quantity[instrument.roles.settle.D]].lteqv(measured.unrefined, riskBudget.unrefined) then
                Some(lots)
              else selected
            loop(candidate + 1, next)
    loop(BigInt(1), None)
  end maxLots
end TransitionalRisk

object TransitionalRisk:
  def create[I <: Instrument](
    instrument: I
  )(
    feePolicy: FeePolicy[instrument.type]
  ): Either[RiskError, TransitionalRisk[instrument.type]] =
    if feePolicy.instrument.identity.id != instrument.identity.id then
      Left(
        RiskInstrumentMismatch(
          "feePolicy",
          instrument.identity.id,
          feePolicy.instrument.identity.id
        )
      )
    else Right(new TransitionalRisk[instrument.type](feePolicy))
