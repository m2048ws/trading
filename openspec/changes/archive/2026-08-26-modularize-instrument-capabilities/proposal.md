## Why

`Instrument.scala` remains a 1,405-line trusted compilation unit even after the capability-oriented public API and initial pure-rule extraction. Its public contract, capability wiring, private representations, validation workflows, and exact calculations are still interleaved enough that understanding or changing one capability requires scanning most of the aggregate.

This follow-up should make the implementation navigable by capability while the current API and every ownership, exactness, provenance, and failure guarantee are already fresh and comprehensively covered.

## What Changes

- Make `Instrument.scala` the trusted composition shell: public instrument contract, validated aggregate construction, owner-bound witness casts, sealed-value construction, and stable capability wiring.
- Move the price, market, order, scenario, fee, valuation, and sizing workflows into concern-specific internal source units.
- Keep extracted engines authority-free: they may validate and calculate raw plans or use narrowly typed callbacks, but they do not construct genuine instrument-owned values, cast registered witnesses, or expose public/package-spoofable authority.
- Fold or relocate the current broad rule helpers into the appropriate capability implementation units where that improves locality.
- Preserve the public `Instrument` surface, direct `instrument.Price`/`instrument.Order`-style paths, stable capability values, exact formulas, validation order, typed failures, and runtime behavior.
- Retain downstream positive, cross-instrument negative, private-construction negative, and same-package-spoof compiler coverage; add focused regression coverage for the new internal seams where useful.
- Restore quantities' same-project tests to SBT's ordinary `Compile / classes` wiring instead of routing them through `Compile / packageBin`; retain immutable packaged artifacts only for true downstream consumers.
- Order the existing economics package task after economics compilation so the immutable compiler-boundary artifact is complete on a clean build, without changing module, dependency, or publication topology.

## Capabilities

### New Capabilities

None. This change reorganizes implementation source and introduces no user-facing capability.

### Modified Capabilities

None. Canonical economics requirements and observable public behavior remain unchanged; this change opts out of delta specs with `skip_specs: true`.

## Impact

- Primary code: `economics/src/main/scala/trading/economics/Instrument.scala` and new concern-specific internal implementation files in the same package.
- Related code: the existing `PriceMarketRules`, `OrderScenarioRules`, and `FeeValuationSizingRules` helpers may be replaced or redistributed; economics and packaged compiler-boundary tests may be reorganized or extended.
- Public API: no intended source-level or semantic change, no compatibility aliases, and no new public domain abstractions.
- Build boundary: no new dependency and no change to module, inter-module dependency, or publication topology; quantities tests return to the normal same-project classes directory, downstream consumers retain `quantitiesExternalArtifact`, and `economics / Compile / packageBin` gains an explicit prerequisite on `economics / Compile / compile` while `economicsExternalArtifact` continues to consume that package.
