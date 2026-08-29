## Why

The first instrument-economics API preserves the intended exactness and path-dependent safety, but concentrates too many unrelated operations on `Instrument` and makes ordinary callers restate dimensions the instrument already owns. Because the economics artifact is still unreleased at `0.1.0-SNAPSHOT`, this is the right point to make the public API readable before downstream code depends on the current shape.

## What Changes

- **BREAKING** Reorganize instrument-owned operations into focused capability views for prices, market states, orders, scenarios, fees, valuation, and sizing while keeping identity, asset roles, contract terms, and path-owned types on the instrument.
- **BREAKING** Replace ambiguous price entry points with intent-revealing exact-value, tick-coordinate, typed-rate, and explicit-quantization operations. Ordinary exact price construction accepts a rational coefficient because the instrument already determines quote-per-base dimensions.
- **BREAKING** Replace the `marketStateFor*`, `marketStateFrom*`, and `marketStateChecked` family with names that state the settlement relationship or supplied anchor. Ordinary anchor entry points accept rational coefficients when their source and settlement endpoints are already determined; typed rate entry points remain available for values produced by typed quantity arithmetic.
- **BREAKING** Move order construction, scenario construction, fee calculation, valuation, and position sizing behind their corresponding capability views without weakening instrument-path ownership or checked construction.
- Keep collections as explicit immutable vectors where zero, one, or many values are semantically meaningful, but use role-specific parameter names and validate aggregate invariants at the owning boundary instead of adding pass-through collection wrappers.
- Keep textual number parsing outside the economics core. Venue or application adapters parse text to exact `Rational` values and then call instrument-bound price construction.
- Extract authority-free calculation and validation rules from the concrete instrument implementation where doing so improves local readability; construction of path-owned public values remains under the instrument's private authority.
- Remove the superseded public entry points rather than retaining permanent forwarding aliases.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: Reshape instrument-bound price, market-state, conversion, and valuation entry points around focused capabilities and inputs whose dimensions are already known.
- `order-scenarios`: Expose order and scenario construction through separate instrument-owned capability views while preserving all checked mechanics and path ownership.
- `fee-inclusive-pnl`: Expose fee operations and fee-inclusive valuation through focused capability views while preserving contextual schedules, plural fee results, and exact breakdowns.
- `position-risk-sizing`: Expose downside-risk and discrete sizing operations through a dedicated sizing capability while preserving exhaustive exact evaluation semantics.

## Impact

- Affects the public API and implementation organization in the `economics` module, its tests and examples, and downstream compiler-boundary fixtures.
- Requires source migration for callers of the current flat `Instrument` methods; no compatibility layer is planned before the first release.
- Does not change `quantities` source, public APIs, packaged artifact contents, exact rational semantics, registry/grid provenance, path-dependent cross-instrument protections, product-family normalization, or the economic formulas already specified.
- Makes the `quantities` main-to-test compiler boundary immutable by having its tests consume the completed `Compile / packageBin` JAR instead of the mutable `Compile / classes` directory; this test-only wiring does not enable repository-wide `exportJars` or change dependent-project classpaths.
- Treats one active SBT invocation per checkout's mutable target tree as the supported validation model. Concurrent independent clean and non-clean invocations sharing that tree are unsupported; automation that needs concurrency uses separate checkouts or target trees.
- Does not add retry, delay, filesystem polling, hard-coded target paths, publication, or repository-wide build serialization to compensate for unsupported concurrent invocations.
- Does not add a venue decoder or a core `String`-to-price API; adapters remain responsible for accepted textual grammar and diagnostics.
