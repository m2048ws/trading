## Why

Exact quantity construction is unnecessarily verbose because callers must select a `fromXxx` name even though the
coefficient type already identifies the construction behavior. Overloaded `Quantity.apply` methods make common call
sites shorter while retaining the same exactness and dimension-witness guarantees.

## What Changes

- Add overloaded `Quantity.apply` constructors for `Rational`, `BigInt`, decimal `String`, and finite
  `java.math.BigDecimal` coefficients, plus exact `Int` and `Long` adapters that forward through `BigInt` so primitive
  integer values and literals remain concise.
- **BREAKING** Remove `fromRational`, `fromInteger`, `fromDecimal`, and `fromFiniteDecimal`; callers migrate each use
  directly to the matching `Quantity(dimension, coefficient)` overload.
- Migrate project examples and internal call sites to the concise `Quantity(dimension, coefficient)` form.
- Keep `Float` and `Double` construction unavailable.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Exact quantity construction is consolidated on concise overloaded `apply` entry points
  and exact primitive-integer adapters, removing the parallel named-constructor surface without changing coefficient
  semantics, validation, or construction authority.

## Impact

The public `trading.quantity.Quantity` companion in the `quantities` SBT project and `trading-quantities` artifact
changes incompatibly for source that still calls a removed named constructor. Exact-quantity construction tests,
internal Scala call sites, and the module README examples are affected. No dependency, persistence, runtime
representation, or binary data format changes.
