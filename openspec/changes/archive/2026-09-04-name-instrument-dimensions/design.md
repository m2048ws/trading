## Context

See `proposal.md` for motivation. `Instrument` retains one stable `roles` value from `InstrumentSpec`; its grids, payoff rates, and nested economic type members currently refer directly to `roles.position.D`, `roles.base.D`, `roles.quote.D`, and `roles.settle.D`. The accepted Slice requires shorter names for those same path-dependent types while preserving the established assembly proof, public values, and one-way module boundary.

This planning worktree may coexist with another delivery that adds instrument-dependent economic members. Apply must therefore reconcile the then-current `Instrument` surface before its dedicated Task Group commit and preserve every member introduced on the integrated base.

## Goals / Non-Goals

**Goals:**

- Make each retained role dimension directly nameable from one exact `Instrument` value.
- Preserve compile-time equality between every direct alias and its corresponding `roles` projection.
- Keep all existing instrument-dependent member types as precise as their current definitions.
- Prove supported use and nearby incompatible use from a completed instrument-economics artifact.

**Non-Goals:**

- Introduce a new dimension, witness, refinement, runtime field, owner type, or validation step.
- Change `InstrumentSpec`, assembly, role identity, grid identity, economics, serialization, or error behavior.
- Add a bound operation scope; those are owned by later RFC-0008 Slices.
- Promise ordinary Java source or binary compatibility.

## Decisions

### Define transparent aliases from the retained stable roles value

`Instrument` will define `PositionD = roles.position.D`, `BaseD = roles.base.D`, `QuoteD = roles.quote.D`, and `SettleD = roles.settle.D`. Existing fields and nested economic type members will be expressed through those direct aliases where doing so makes the public relationship visible.

This is chosen because `roles` is already the single proof-carrying product retained from assembly. A second set of abstract type members, new phantom dimensions, match types, or separately summoned equality evidence could drift from that product and would force callers or implementations to bridge two representations. A runtime accessor cannot provide the required compile-time relationship.

The aliases add no algebra: they are names for four projections of an existing product. No new law, type class, sum, refinement, validation result, or error owner is warranted.

### Preserve the established trusted transition

The only trusted transition remains raw definition plus immutable catalog snapshot to `InstrumentSpec`, followed by total `Instrument.fromSpec`. Alias availability performs no validation and grants no construction or reconciliation authority. Existing runtime instrument identity, asset lineage, grid membership, exact rates, refinements, and serialization rejection remain owned by their current values and boundaries.

No cast, match type, structural refinement, runtime dimension comparison, implicit conversion, or replacement `Dim` evidence will implement the aliases. There is no expected failure to model because obtaining an alias from an already assembled instrument is total.

### Keep ownership, effects, and dependencies unchanged

The primary owner is `trading-instrument-economics`. Allowed production dependencies remain quantities and immutable reference data plus already admitted pure support. Order model, actual execution, execution scenarios, fee policy, risk, boundary codecs, application, and runtime remain consumers and are forbidden dependencies. No new library coordinate or public library type is introduced; Scala 3.8.x and JDK 25 remain unchanged.

The aliases are compile-time-only and allocate no runtime object, acquire no resource, observe no mutable state, and add no coordination or hot-path work. Codecs and durable schemas are unaffected because no runtime representation changes.

### Verify both equality and rejection at the packaged boundary

Focused instrument-economics checks will establish that existing fields and nested types retain their exact values and relationships. A completed-artifact positive Scala fixture will use the direct aliases interchangeably with the four role projections, including generic helpers and representative grid, rate, quantity, and existing nested-type observations, without explicit dimension arguments or local `D`/`B`/`Q`/`S` aliases.

Nearby negative fixtures will have independently compilable preludes and offending expressions that attempt cross-role and abstract cross-instrument assignments. The compiler suite will require the intended type-mismatch diagnostics and reject compiler-internal failures. Existing completed-JAR/classpath checks will continue to prove the instrument-economics artifact contains no downstream owners, and existing negative surface checks will continue to prove `Instrument` has no downstream operations.

Runtime-only unit tests, reflection, or source-text assertions cannot establish the public type equality and rejection claims, so they are insufficient alternatives to completed-artifact compiler evidence.

## Risks / Trade-offs

- [A seemingly concise declaration widens a path-dependent alias or loses the captured singleton relationship] → Compile positive equality in both directions and reject abstract foreign-instrument and cross-role values from the packaged artifact.
- [Re-expressing existing members accidentally changes a dependent endpoint] → Type-check representative grids, rates, quantities, market state, and P&L members through both direct and role-qualified forms and retain focused runtime value assertions.
- [A concurrently integrated delivery adds more `Instrument` members or edits the compiler harness] → Reconcile the current integration head before implementation, express all then-current instrument-dependent members through the aliases where applicable, and run the combined focused and full boundary suites.
- [Additional public names are mistaken for new runtime authority] → Keep aliases transparent and compile-time-only; add no fields, evidence producers, casts, factories, services, or validation bypasses.

## Migration Plan

1. Integrate any accepted base advancement before the Task Group implementation and inventory the current `Instrument` members.
2. Add the four aliases and re-express applicable existing members without changing their constructors or runtime values.
3. Add focused and completed-artifact compiler evidence, then run module, dependency-boundary, formatting, and full repository checks.
4. Roll back by reverting the dedicated Task Group commit; no data, schema, catalog, wire, or runtime migration is required.
