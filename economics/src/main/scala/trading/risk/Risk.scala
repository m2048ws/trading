package trading.risk

import scala.annotation.tailrec

import trading.economics.instrument.*
import trading.fee.policy.FeePolicy
import trading.quantity.*
import trading.quantity.refinement.*
import trading.scenario.*

/** Transitional pure risk boundary consuming explicit instrument economics and fee policy. */
final class Risk[I <: Instrument] private[risk] (val feePolicy: FeePolicy[I]):

  val instrument: feePolicy.instrument.type = feePolicy.instrument

  def downsideRisk(
    pnl: instrument.Pnl
  ): Either[RiskError, NonNegative[Quantity[instrument.roles.settle.D]]] =
    if pnl.instrumentId != instrument.identity.id then
      Left(RiskInstrumentMismatch("pnl", instrument.identity.id, pnl.instrumentId))
    else
      val quantity =
        if pnl.netPnl.coefficient.signum < 0 then pnl.netPnl * Rational(-1)
        else
          Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
      NonNegative(quantity) match
        case Right(nonnegative) => Right(nonnegative)
        case Left(_)            => Left(InvalidRiskBudget(quantity.coefficient))

  def maxLots(
    riskBudget: Quantity[instrument.roles.settle.D],
    cap: PositiveWhole,
    schedule: feePolicy.Schedule
  )(
    scenarioFor: instrument.Lots => Either[ScenarioError, feePolicy.RoundTrip]
  ): Either[RiskError, Option[instrument.Lots]] =
    if riskBudget.coefficient.signum < 0 then Left(InvalidRiskBudget(riskBudget.coefficient))
    else
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
                if measured.unrefined.coefficient.compare(riskBudget.coefficient) <= 0 then Some(lots)
                else selected
              loop(candidate + 1, next)
      loop(BigInt(1), None)
end Risk

object Risk:
  def create[I <: Instrument](
    instrument: I
  )(
    feePolicy: FeePolicy[instrument.type]
  ): Either[RiskError, Risk[instrument.type]] =
    if feePolicy.instrument.identity.id != instrument.identity.id then
      Left(
        RiskInstrumentMismatch(
          "feePolicy",
          instrument.identity.id,
          feePolicy.instrument.identity.id
        )
      )
    else Right(new Risk[instrument.type](feePolicy))
