package external.economics.positive

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.quantity.Rational

object SameShapeReplayClient:
  def genericActivation[P](activation: OrderActivation[P])(
    evidence: activation.Evidence
  ): Either[ActivationViolation, CheckedActivation[P]] =
    activation.validate(evidence)

  def genericPricing[P](pricing: OrderPricing[P])(
    resolution: pricing.Resolution
  ): Either[PricingViolation, EffectivePricing[P]] =
    pricing.resolve(resolution)

  assert(fixed.validate(fixedEvidence).isRight)
  assert(genericActivation(fixed)(fixedEvidence).isRight)
  assert(trailing.validate(trailingEvidence).isRight)
  assert(genericActivation(trailing)(trailingEvidence).isRight)
  assert(peg.resolve(pegResolution).isRight)
  assert(genericPricing(peg)(pegResolution).isRight)

  private val price101 = instrument.prices.exact(Rational(101)).toOption.get

  private val fixedTriggerChanged =
    instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, price101)
  private val fixedReferenceChanged =
    instrument.orders.fixedTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, price100)
  private val fixedComparisonChanged =
    instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrBelow, price100)

  assert(fixedTriggerChanged.validate(fixedEvidence) == Left(ActivationViolation.FixedEvidenceMismatch))
  assert(fixedReferenceChanged.validate(fixedEvidence) == Left(ActivationViolation.FixedEvidenceMismatch))
  assert(fixedComparisonChanged.validate(fixedEvidence) == Left(ActivationViolation.FixedEvidenceMismatch))

  private val trailingOffsetChanged = instrument.orders
    .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, 2)
    .toOption
    .get
  private val trailingReferenceChanged = instrument.orders
    .trailingTrigger(PriceReference.Last, TriggerComparison.AtOrAbove, 1)
    .toOption
    .get
  private val trailingComparisonChanged = instrument.orders
    .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrBelow, 1)
    .toOption
    .get

  assert(trailingOffsetChanged.validate(trailingEvidence) == Left(ActivationViolation.TrailingEvidenceMismatch))
  assert(trailingReferenceChanged.validate(trailingEvidence) == Left(ActivationViolation.TrailingEvidenceMismatch))
  assert(trailingComparisonChanged.validate(trailingEvidence) == Left(ActivationViolation.TrailingEvidenceMismatch))

  private val pegOffsetChanged    = instrument.orders.peggedPricing(PriceReference.Mark, 2)
  private val pegReferenceChanged = instrument.orders.peggedPricing(PriceReference.Last, 1)

  assert(pegOffsetChanged.resolve(pegResolution) == Left(PricingViolation.PegResolutionMismatch))
  assert(pegReferenceChanged.resolve(pegResolution) == Left(PricingViolation.PegResolutionMismatch))

end SameShapeReplayClient
