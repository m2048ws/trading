package trading.order;

final class PackageSpoofOrderConstruction {
  static void forge(
      OrderIntent intent,
      CheckedActivation checked,
      FixedTriggerEvidence fixed,
      TrailingTriggerEvidence trailing,
      PegResolution peg) {
    new CheckedActivation(checked.observations());
    checked.copy(checked.observations());
    new FixedTriggerEvidence(
        fixed.reference(), fixed.comparison(), fixed.triggerPrice(), fixed.observedPrice());
    new TrailingTriggerEvidence(
        trailing.reference(),
        trailing.comparison(),
        trailing.offsetTicks(),
        trailing.favorableExtreme(),
        trailing.observedPrice());
    new PegResolution(
        peg.reference(), peg.offsetTicks(), peg.referencePrice(), peg.resolvedLimit());
    new OrderIntent(
        intent.instrumentId(),
        intent.side(),
        intent.lots(),
        intent.positionEffect(),
        intent.positionChange());
  }
}
