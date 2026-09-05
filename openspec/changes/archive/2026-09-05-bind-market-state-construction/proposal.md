## Why

Repeated market-state construction currently repeats the instrument and exposes eight pairs of overloads whose only
difference is an empty additional-conversion argument. RFC-0008/S-06 completes the accepted owner-local binding pattern
by making these calls concise while preserving exact conversion mathematics, checked identities, and dependent types.

## What Changes

- Consolidate `quoteSettled`, `baseSettled`, `fromQuoteAnchor`, `fromBaseAnchor`, `fromAnchors`, `fromQuoteRate`,
  `fromBaseRate`, and `fromRates` into one direct operation each with an empty default for additional conversions.
- Add a `MarketState`-owned immutable instrument scope exposing all eight operations and exact local type aliases;
  capture only the assembled instrument and forward each invocation to its canonical checked operation.
- Preserve typed price/rate/conversion inputs, rational results, accumulated `MarketStateViolations`, validation order,
  runtime identity/lineage checks, existing price-grid guarantees, and separate `SettlementConversion` construction.
- Characterize old direct behavior before consolidation; compare all direct/scoped/defaulted forms, add conversion
  properties and concurrent reuse checks, and verify concise positive and precise negative compiled-artifact clients.
- **BREAKING (binary only):** removing redundant overload descriptors is permitted by the accepted Scala-first RFC;
  supported ordinary Scala calls with omitted, explicit-empty, and non-empty conversions must continue compiling.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: consolidate market-state conveniences, add instrument-bound market-state construction,
  and specify behavioral equivalence, exact dependent types, purity, and ownership at that existing boundary.

## Impact

- Source contract: `RFC-0008-simplify-instrument-dependent-apis/S-06-bind-market-state-construction`, AC-022–AC-025,
  all requiring automated evidence; the accepted RFC and its non-goals remain authoritative.
- Primary production owner: `trading-instrument-economics`, principally `Market.scala`. Errors remain owned by
  instrument economics; quantities and immutable reference data remain its only production project dependencies.
- Tests: instrument-economics characterization/property suites and the existing `adversarialBoundary` economics
  compiler harness. All callers remain subject to the aggregate build and completed-artifact checks.
- No new module, dependency, service, registry, codec, runtime interpreter, business policy, or JDK baseline change.
  No market behavior moves onto `Instrument`, and `SettlementConversion` remains independently constructed.
- Delivery is one coherent Task Group containing the constructor consolidation, scope, and matching evidence.
