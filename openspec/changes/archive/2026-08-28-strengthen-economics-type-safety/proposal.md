## Why

The economics module already sits on a dimension-safe exact-arithmetic foundation, but several aggregate boundaries discard that type information into raw `Rational`, validate related values independently, and reconstruct coherence with casts and large mismatch matrices. Consolidating those paths around typed composition, proof-carrying validation, and shape-indexed scenario evidence will make invalid states harder to represent while improving diagnostics and preserving exact behavior.

## What Changes

- Keep market conversion, contract valuation, fee aggregation, PnL, and downside-risk calculations in `Rate` and `Quantity` form until an explicit scalar observation is required.
- Split raw instrument input from a proof-carrying validated definition, localizing checked grid narrowing and role-dependent evidence at the validation boundary.
- Add domain-specific definition and scenario violation ADTs plus optional accumulating diagnostic entry points; keep existing `Either[EconomicsError, ...]` construction paths deterministic and fail-fast.
- **BREAKING** Pair activation evidence and pricing resolution with the corresponding activation or pricing shape through associated evidence types and construction methods, removing invalid evidence combinations from supported construction.
- **BREAKING** Represent scenario matched slices as a non-empty collection and replace `Option[BigInt]` effective-price semantics with an explicit `EffectivePricing` ADT.
- Add `cats-core` to the economics module for applicative error accumulation, fail-fast traversal, non-empty collections, and efficient internal accumulation without exposing Cats error collections in the public domain error model.
- Refactor exhaustive sizing into a pure stack-safe state transition while retaining complete deterministic candidate evaluation and existing failure semantics.
- Defer quantity-foundation proof composition, grid-embedding composition, immutable registry extraction, total `Rational`/identifier constructors, affine position modeling, arbitrary payoff-leg generalization, and any reusable/free validation DSL to separate changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: Preserve typed rates and quantities through market conversion and valuation, and add a proof-carrying, accumulating instrument-definition validation boundary while retaining deterministic fail-fast construction.
- `order-scenarios`: Associate activation/pricing evidence with the order shape, make matched slices non-empty by construction, expose effective pricing explicitly, and support accumulated independent scenario diagnostics without changing semantic trigger, pricing, liquidity, or identity checks.

## Impact

- Affected production code: `economics/src/main/scala/trading/economics/instrument/{Definition,Error,Instrument,Market,Order,Scenario,Fee,Valuation,Sizing}.scala`.
- Affected public API: validated-definition and diagnostic types; activation/pricing evidence construction; scenario assumptions and matched-slice collection; explicit effective pricing. Existing fail-fast construction entry points remain available.
- Dependency impact: `cats-core` becomes a compile dependency of `trading-economics`; the quantities artifact and its authority model remain unchanged.
- Tests and downstream fixtures must cover accumulated diagnostics, deleted mismatch combinations, typed endpoint composition, non-empty slices, deterministic fail-fast compatibility, and unchanged exact numerical results.
