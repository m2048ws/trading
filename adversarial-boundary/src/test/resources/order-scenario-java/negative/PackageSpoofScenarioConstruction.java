package trading.scenario;

final class PackageSpoofScenarioConstruction {
  static void forge(
      LiquiditySlice slice,
      ScenarioAssumptions assumptions,
      OrderScenario scenario,
      RoundTripScenario roundTrip) {
    new LiquiditySlice(slice.instrumentId(), slice.lots(), slice.market(), slice.role());
    slice.copy(slice.instrumentId(), slice.lots(), slice.market(), slice.role());
    new ScenarioAssumptions(
        assumptions.order(),
        assumptions.activationEvidence(),
        assumptions.pricingResolution(),
        assumptions.matchedSlices());
    new MatchedSlices(assumptions.matchedSlices().head(), assumptions.matchedSlices().tail());
    new OrderScenario(
        scenario.assumptions(),
        scenario.checkedActivation(),
        scenario.effectivePricing(),
        scenario.positionChange());
    new RoundTripScenario(
        roundTrip.instrumentId(),
        roundTrip.entry(),
        roundTrip.exit(),
        roundTrip.heldPosition());
  }
}
