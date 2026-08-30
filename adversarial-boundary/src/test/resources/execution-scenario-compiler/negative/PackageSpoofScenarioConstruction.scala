package trading.scenario

import trading.quantity.Dim

object PackageSpoofScenarioConstruction:
  def forge[D <: Dim, B <: Dim, Q <: Dim, M](
    slice: LiquiditySlice[?, M],
    assumptions: ScenarioAssumptions[D, B, Q, M],
    scenario: OrderScenario[D, B, Q, M],
    roundTrip: RoundTripScenario[D, B, Q, M]
  ): Unit =
    // OFFENDING-BEGIN
    val rawSlice = new LiquiditySlice(
      slice.instrumentId,
      slice.lots,
      slice.market,
      slice.role
    )
    val copiedSlice = slice.copy(role = slice.role)
    val rawAssumptions = new ScenarioAssumptions[D, B, Q, M](assumptions.order)(
      assumptions.activationEvidence,
      assumptions.pricingResolution,
      assumptions.matchedSlices
    )
    val rawScenario = new OrderScenario[D, B, Q, M](
      scenario.assumptions,
      scenario.checkedActivation,
      scenario.effectivePricing,
      scenario.positionChange
    )
    val rawRoundTrip = new RoundTripScenario[D, B, Q, M](
      roundTrip.instrumentId,
      roundTrip.entry,
      roundTrip.exit,
      roundTrip.heldPosition
    )
    // OFFENDING-END
    val _ = slice
end PackageSpoofScenarioConstruction
