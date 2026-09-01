## Why

Order instructions and hypothetical execution evidence currently share one instrument package and are constructed through instrument-owned service views. Separating the immutable instruction from its scenario interpretation gives each algebra a clear owner, removes duplicated target relationships, and creates stable foundations for later fee policy, risk, and live execution interpreters.

## What Changes

- Add a pure `trading-order-model` artifact for immutable order intent, activation, pricing, duration, liquidity constraint, visibility, and their associated evidence shapes.
- Add a pure `trading-execution-scenario` artifact, depending on the order model and instrument economics, for non-empty matched slices, activation/pricing evaluation, complete hypothetical order outcomes, and checked round trips.
- Keep order alternatives algebraic: invalid market/priced, trigger, peg, and visibility combinations remain unrepresentable rather than encoded by flags and optional fields.
- Preserve the dependent relationship between each activation/pricing instruction and the exact evidence or resolution shape it accepts.
- Make a scenario input own its target order exactly once. Construction no longer accepts the same order separately or checks object reference equality to reconcile duplicated claims.
- Use a domain-named non-empty matched-slice value and staged deterministic validation: independent violations accumulate, while checks requiring absent evidence do not run.
- Make order intent own its exact side-directed `PositionLots`; scenario evaluation consumes that result rather than recomputing signed position arithmetic.
- Replace free-form validation path strings and the universal economics error hierarchy with closed order/scenario locations and boundary-owned error ADTs.
- **BREAKING** Remove `Orders`, `Scenarios`, `instrument.orders`, `instrument.scenarios`, duplicated scenario target inputs, and old package-level forwarding paths without compatibility aliases.
- Exclude order lifecycle, venue submission, fills, cancellation, account-state enforcement, market-data acquisition, and all effects from both artifacts.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `order-scenarios`: Split immutable order instructions from hypothetical execution scenarios, remove duplicated target state, and specify the one-way artifact/API dependency between them.

## Impact

- Adds SBT projects/directories `orderModel` / `order-model` and `executionScenario` / `execution-scenario`.
- Moves `Side`, order ADTs, and instruction evidence into `trading.order`; moves liquidity outcomes, scenario assumptions, and round trips into `trading.scenario`.
- Changes public construction paths, scenario input shape, errors, packages, and test imports.
- Removes order/scenario source from the transitional `trading-economics` aggregate; fee policy and risk consume the new artifacts in subsequent proposals.
- Adds module-boundary, negative-compilation, staged-validation, and behavioral-equivalence coverage; introduces no effect-system dependency or I/O.
