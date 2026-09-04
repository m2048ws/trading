## Why

Price PnL is currently calculated inside round-trip scenario valuation, which prevents later campaign functionality from reusing the same exact economics for arbitrary finite sequences of attributed position changes. Establishing one instrument-economics operation now preserves reference-coherent behavior while enforcing retained reference-data identity and providing the honest flat-or-marked endpoint needed by subsequent campaign slices.

## What Changes

- Add a pure instrument-economics capability that calculates exact ending position, ordered settled contributions, total price PnL, and a flat or marked endpoint for any finite sequence of attributed priced position changes.
- Represent invalid endpoint, identity, retained-reference, dimension, and grid conditions with typed domain errors, accumulating independent validation failures deterministically; compatible finite `BigInt`/`Rational` calculations are total.
- Route existing single- and multi-slice round-trip scenario valuation through the shared calculation while preserving reference-coherent long, short, and fee-inclusive results. Reject same-ID foreign-lineage inputs at their original leg and slice with the retained `ReferenceDataError` instead of fabricating an instrument mismatch.
- Verify the generalized calculation with examples and laws covering empty input, open and flat endpoints, three-or-more-change scale-in/scale-out paths, attribution, ordering, permutation, and linear cost.

## Capabilities

### New Capabilities

- `attributed-price-pnl`: Exact finite-change price-PnL calculation with attributed settled contributions and an honest flat-or-marked endpoint.

### Modified Capabilities

None.

## Impact

The change affects the public pure valuation surface and tests in `instrument-economics`, plus the valuation adapter and regression tests in `execution-scenario`. It adds one truthful `ValuationError` case, so exhaustive matches over that nested error algebra must handle the new case. It adds no runtime effects, external dependencies, persistence, or campaign domain types.
