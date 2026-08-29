## Why

The repository is growing from a quantity foundation into instrument economics and, next, effectful trading infrastructure. Without an explicit architecture and Scala/functional-programming charter, locally convenient abstractions can mix mathematical, reference-data, domain, application, and runtime concerns and erase invariants that the type system could have preserved.

This foundational "Proposal 0" establishes the design standard that all later proposals in the architectural shift must follow before any of them are implemented.

## What Changes

- Establish a repository-wide architectural charter based on cohesive responsibilities, one-way dependencies, explicit trust boundaries, and a pure functional core surrounded by an effectful application/runtime shell.
- Require algebra-first modeling: designers must look for meaningful sums, products, refinements, lawful composition, accumulation, traversal, ordering, and state-transition structures before expressing the same behavior as flags, primitives, mutable state, or procedural control flow.
- Require semantic information and established evidence to remain represented in domain types instead of being erased into raw coefficients, identifiers, tuples, or booleans and reconstructed later.
- Define a layer-specific functional-programming profile: pure algebraic quantity and domain code; immutable reference-data transitions and snapshots; effect-polymorphic application capabilities; and concrete Cats Effect/FS2 state, concurrency, resource, and streaming machinery confined to runtime interpreters.
- Establish a dependency-admission policy: prefer mature libraries for general mechanisms when they satisfy the actual
  contract, keep them in the narrowest owning layer, retain project ownership of domain meaning and durable schemas,
  and require an explicit distinct need before introducing a second effect, JSON, refinement, or similar vocabulary.
- Record JDK 17 as the minimum build and runtime baseline already implied by Scala 3.8.4, require dependency majors to
  respect that baseline, and version independently released libraries through independent build coordinates.
- Define validation and error-modeling rules, including applicative accumulation for independent violations, fail-fast sequencing for dependent checks, typed evidence-producing results, deterministic error ordering, and errors owned by the layer that can understand them.
- Define when advanced Scala and functional abstractions are warranted, together with requirements for ergonomic public APIs, explicit laws, focused tests, and isolation of unavoidable casts or unsafe implementation details.
- Establish the target responsibility and dependency direction for quantities, reference data, instrument economics, orders, execution scenarios, fee policy, risk, application workflows, boundary codecs, and runtime interpreters. Later proposals will define their concrete APIs and migrations.
- Add review and proposal criteria that make the charter enforceable rather than aspirational.
- Defer applying this charter and all downstream architectural changes until the complete linked proposal portfolio has been written and checked for cross-proposal coherence.

## Capabilities

### New Capabilities

- `repository-architecture`: Defines responsibility ownership, permitted dependency direction, dependency admission
  and containment, trust and effect boundaries, target module roles, hot-path separation, and architecture-review
  obligations.
- `scala-functional-design`: Defines the expected Scala 3 and functional-programming standard, including algebra-first modeling, semantic type preservation, validation, totality, abstraction selection, API ergonomics, and required verification.

### Modified Capabilities

None. This charter governs future repository design without changing the current behavioral requirements of existing quantity or economics capabilities.

## Impact

- Project design documentation, `AGENTS.md`, stable `.agent` context, review guidance, proposal templates, and module responsibility documentation will be updated when this proposal is eventually applied.
- Future OpenSpec changes will need to identify their owning layer, dependency effects, third-party mechanism and public
  exposure, platform compatibility, algebraic model, trust boundary, validation strategy, error ownership, runtime
  effects, law obligations, and hot-path implications.
- Subsequent proposals will define the actual package/module split and migrations; this change does not move production code, alter public APIs, or select concrete live-catalog, persistence, streaming, database, or telemetry interpreters.
- The proposal introduces no compatibility commitment for unreleased APIs and does not weaken any settled quantity, grid, authority, provenance, or compiler-boundary invariant.
