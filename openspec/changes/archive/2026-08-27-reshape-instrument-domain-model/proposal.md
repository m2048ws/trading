## Why

The first ergonomics refactor made instrument operations easier for callers to discover, but it left `Instrument` as a large ownership scope, factory, validator, service locator, and calculation engine while adding parallel `Plan` and `View` representations. Because the economics artifact is still unreleased, the domain model should represent its invariants and ownership directly before more behavior accumulates around this structure.

## What Changes

- **BREAKING** Replace tag-plus-`Option` representations of activation, pricing, visibility, and scenario evidence with closed, owner-aware domain alternatives whose valid shapes are explicit.
- **BREAKING** Make positive instrument lots and prices genuine refined owned values, keep signed position lots distinct, and move intrinsic observations such as count, exact quantity, ticks, and rate onto the values they describe.
- **BREAKING** Replace wide checked-construction parameter lists with cohesive inputs for instrument definition, order intent/mechanics, scenario assumptions, and validated fee denomination where those inputs own reusable meaning or invariants.
- Introduce a non-forgeable internal instrument ownership kernel that centralizes authoritative grids, witnesses, owner identity, and safe construction without requiring all capability implementations to remain in `Instrument.scala`.
- Remove the parallel scalar `Plan`/`View` models and repeated capability forwarding; capability implementations SHALL operate on the actual domain alternatives and owned values through the kernel.
- Assign validation to explicit boundaries: local numeric invariants at smart construction, structural invariants in domain alternatives, and cross-value coherence at instrument, order, market-state, and scenario aggregate construction.
- Keep one public `Instrument` aggregate, composed from explicit identity, asset-role, listing-rule, and contract-payoff concepts. A separate public `Contract`/`Listing` hierarchy is not introduced without a concrete multiple-listings requirement.
- Preserve exact arithmetic, contextual grids, registry provenance, path-dependent cross-instrument rejection, product-family-neutral valuation, contextual fees, complete-scenario semantics, and exhaustive sizing behavior.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: Reshape instrument construction, owned lots/prices, market values, intrinsic observations, and internal ownership authority around refined values and cohesive domain components.
- `order-scenarios`: Replace optional-field order and scenario shapes with explicit alternatives and cohesive checked-construction inputs while preserving compositional mechanics and complete-scenario validation.
- `fee-inclusive-pnl`: Introduce validated fee-denomination context and value-local observations while preserving contextual schedules, quantization, attribution, and exact PnL.

## Impact

- Affects the public pre-release API and production organization of the `economics` module, its behavioral/property tests, examples, and packaged downstream compiler fixtures.
- Removes superseded capability forwarding and internal `Plan`/`View` representations rather than retaining compatibility aliases.
- Requires new negative and positive compiler coverage for refined lots/prices, owner-aware alternatives, non-forgeable kernel authority, and cross-instrument rejection.
- Does not change the `quantities` public API or artifact, exact formulas, grid semantics, registry identity, venue parsing boundary, execution lifecycle scope, or account/ledger behavior.
