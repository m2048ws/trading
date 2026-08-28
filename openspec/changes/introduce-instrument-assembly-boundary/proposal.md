## Why

Instrument construction currently begins with already issued catalog handles, repeats provenance checks inside
economics, and uses a `ValidatedDefinition` whose role relationships are partly recovered with casts. A single pure
snapshot-based assembly boundary can resolve external identity once, retain the resulting proofs in `InstrumentSpec`,
and make final instrument construction total and registry-free.

## What Changes

- Add a pure `instrument-assembly` boundary that consumes one immutable `CatalogSnapshot` and one raw
  `InstrumentDefinition` containing only stable IDs, grid identities, and exact economic coefficients.
- **BREAKING** Replace the current object-linked `Definition`/`Roles`/`ListingRules`/`ContractPayoff` input graph with a
  cohesive raw product that stores instrument identity once and cannot express contradictory component-role owners.
- Resolve base, quote, position, and settle assets plus position and price grids independently from the same snapshot,
  then validate dependent dimension and payoff relationships only after their prerequisites succeed.
- Accumulate independently observable catalog and structural violations in deterministic order and expose a domain-owned
  non-empty `InstrumentAssemblyErrors`; retain an explicit deterministic first-error projection for callers that need
  fail-fast ergonomics.
- Introduce constructor-private `InstrumentSpec` as the trusted proof-carrying result. It retains immutable `Asset` and
  `GridHandle` capabilities, exact typed payoff rates, and the precise role/grid relationships established by assembly.
- Prefer `InstrumentDefinition -> InstrumentSpec -> Instrument`; do not introduce blanket `Unresolved*`/`Resolved*`
  twins when the boundary is already clear from the types and constructors.
- **BREAKING** Remove public `ValidatedDefinition`, `Instrument.validate(Definition)`, and fallible
  `Instrument.create(Definition)`. Constructing `Instrument` from `InstrumentSpec` is total and performs no catalog
  lookup, issuer comparison, grid cast, or repeated definition validation.
- Remove foreign-registry/reference-lineage and catalog lookup failures from ordinary instrument-economic construction
  errors; assembly owns and contextualizes those failures.
- Keep assembly pure: it receives no `LiveCatalog[F]`, registry, parser, codec, clock, market data, or other effect.
- Leave market-state, fee, P&L, order, scenario, and sizing behavior unchanged in this proposal; the following proposals
  narrow those responsibilities around the assembled instrument.

## Capabilities

### New Capabilities

- `instrument-assembly`: Raw stable-ID instrument definitions, coherent snapshot resolution, staged accumulating
  validation, proof-carrying `InstrumentSpec`, assembly errors, and total handoff to instrument construction.

### Modified Capabilities

- `instrument-economics`: Make the final `Instrument` consume a trusted `InstrumentSpec` rather than owning catalog
  resolution and raw-definition validation, and retire the economics-owned `ValidatedDefinition` boundary.

## Impact

- Depends on the active quantity/reference-data and functional-catalog proposals for `Asset`, `GridHandle`,
  `GridIdentity`, `CatalogSnapshot`, canonical lineage, and pure lookup errors.
- Affected economics code: definition components, validation, errors, `Instrument` construction, fixtures, examples, and
  compiler-boundary tests. Exact lot, price, payoff, market, fee, and valuation formulas do not change.
- The assembly code initially lives as a focused package/boundary beside the current economics implementation; the next
  pure-instrument-economics proposal completes the physical artifact narrowing rather than creating a speculative
  assembly service module.
- Boundary adapters will parse venue/configuration/database forms into `InstrumentDefinition`; the definition is an
  in-memory domain command, not a wire schema.
- Successful instruments retain immutable handles and never return to the catalog. Catalog updates and new grid versions
  affect only later assemblies against later snapshots.
