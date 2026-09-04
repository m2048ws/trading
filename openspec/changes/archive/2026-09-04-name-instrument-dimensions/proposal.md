## Why

`Instrument` already preserves the exact position, base, quote, and settlement dimensions selected during assembly, but downstream Scala APIs must spell those dimensions through deeply nested role projections. Direct instrument-owned aliases make that established type information concise and domain-readable without weakening the assembly boundary or manufacturing new evidence.

## What Changes

- Add `PositionD`, `BaseD`, `QuoteD`, and `SettleD` type members to `Instrument`, each exactly aliasing the corresponding retained role dimension.
- Express existing instrument-dependent members through the direct aliases while preserving their precise dependent types, runtime representation, identity, grids, rates, and construction path.
- Add packaged Scala 3 compiler fixtures proving alias equivalence for valid clients and static rejection for incompatible instruments or roles.
- Reassert that instrument economics remains independent of order, execution, scenario, fee, risk, codec, application, and runtime responsibilities.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `instrument-economics`: Extend the validated `Instrument` surface with exact direct aliases for its four retained role dimensions while preserving all existing economic and dependency guarantees.

## Impact

The change is confined to the `trading-instrument-economics` public Scala API and focused/package-boundary verification. It adds no module, runtime behavior, validation path, error type, dependency, effect, codec or schema change, allocation requirement, or compatibility change to the JDK 25 baseline.
