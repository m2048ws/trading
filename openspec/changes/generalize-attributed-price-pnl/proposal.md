## Why

Price PnL is currently calculated inside round-trip scenario valuation, which prevents later campaign functionality from reusing the same exact economics for arbitrary finite sequences of attributed position changes. Establishing one instrument-economics operation now preserves today's behavior while providing the honest flat-or-marked endpoint needed by subsequent campaign slices.

## What Changes

- Add a pure instrument-economics capability that calculates exact ending position, ordered settled contributions, total price PnL, and a flat or marked endpoint for any finite sequence of attributed priced position changes.
- Represent invalid endpoint, identity, dimension, grid, and arithmetic conditions with typed domain errors, accumulating independent validation failures deterministically.
- Route existing single- and multi-slice round-trip scenario valuation through the shared calculation while preserving exact long, short, and fee-inclusive results and existing error behavior.
- Verify the generalized calculation with examples and laws covering empty input, open and flat endpoints, three-or-more-change scale-in/scale-out paths, attribution, ordering, permutation, and linear cost.

## Capabilities

### New Capabilities

- `attributed-price-pnl`: Exact finite-change price-PnL calculation with attributed settled contributions and an honest flat-or-marked endpoint.

### Modified Capabilities

None.

## Impact

The change affects the public pure valuation surface and tests in `instrument-economics`, plus the valuation adapter and regression tests in `execution-scenario`. It adds no runtime effects, external dependencies, persistence, or campaign domain types.
