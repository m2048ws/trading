## Why

The order model already provides safe standard constructors, but every call repeats the same instrument and exposes its deeply nested dependent dimensions in signatures and nearby type annotations. An immutable instrument-bound scope makes repeated order construction concise while retaining the existing checked implementation and exact result refinements.

## What Changes

- Add an order-model-owned immutable scope created from one exact `Instrument`.
- Expose market, limit, stop-market, and stop-limit construction on that scope with the current operation inputs and defaults, except for the already captured instrument.
- Preserve the exact `Order.Aux` activation and execution refinements, runtime identity checks, deterministic accumulated `OrderViolations`, and direct generic checked construction.
- Add focused equivalence, reuse, ordering, concurrency, and completed-artifact compiler evidence for compatible and incompatible inputs.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `order-scenarios`: Add concise instrument-bound standard-order construction without changing the immutable order algebra or canonical validation semantics.

## Impact

The change is confined to the public Scala API and tests of `trading-order-model` plus its test-only completed-artifact boundary. It adds no module, dependency, effect, runtime service, cache, schema, wire-format change, order kind, validation bypass, or change to the JDK 25 baseline.
