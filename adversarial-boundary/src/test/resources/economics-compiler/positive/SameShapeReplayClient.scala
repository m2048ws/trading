package external.economics.positive

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.Price
import trading.order.*
import trading.quantity.Rational

object SameShapeReplayClient:
  val fixed    = FixedActivation(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
  val evidence = fixed.evidence(price100).toOption.get
  assert(fixed.verify(evidence).isRight)
  val changed = FixedActivation(PriceReference.Last, TriggerComparison.AtOrAbove, price100)
  assert(changed.verify(evidence) == Left(ActivationViolation.FixedEvidenceMismatch))
  assert(Price.exact(instrument)(Rational(101)).isRight)
end SameShapeReplayClient
