# RFC-0001-project-foundation: Project architecture and functional-design foundation

## Goal

Keep the trading repository evolvable without losing exact domain meaning: every concept has one primary owner,
dependencies remain acyclic and point toward the smallest lower-level responsibility, trusted transitions preserve
semantic evidence in types, and effects or mutable coordination stay outside pure mathematical and domain code.

The normative detailed contracts remain `openspec/specs/repository-architecture/spec.md` and
`openspec/specs/scala-functional-design/spec.md`. `docs/design-principles.md` is their human guide, while
`docs/architecture-charter-audit.md` distinguishes the current tree, transitional exceptions, and proposed target.

## Non-goals

- Implement, publish, or claim the existence of every target responsibility or SBT module.
- Define exchange connectivity, market data, trade persistence, execution, portfolio accounting, or another product
  capability without a separately accepted delivery slice.
- Replace capability specifications with this summary RFC or weaken their scenario-level requirements.
- Migrate the externally owned `establish-pure-instrument-economics` implementation into Corgi while it is being
  completed in a separate repository clone.
- Require abstraction for its own sake, a universal effect algebra, one repository-wide error hierarchy, or speculative
  compatibility aliases.

## Boundary

Quantities own exact mathematics, dimensions, grids, refinements, and lawful structure. Reference data own stable
identities, trusted handles, pure catalog transitions, and immutable snapshots. Instrument economics, order models,
execution scenarios, fee policy, and risk each own their domain vocabulary and errors in that dependency direction.
Application owns effect-polymorphic capabilities and workflows; runtime owns concrete effects, resources, concurrency,
clients, and telemetry. Boundary codecs consume internal values to define durable representations and checked
reconstruction, but no domain layer depends on codecs or runtime.

External data crosses one explicit trust transition: parse with bounded resources, resolve against one coherent
immutable view, accumulate independent structural failures deterministically, sequence evidence-dependent checks, and
construct the strongest useful proof-carrying domain value. Expected absence, invalidity, conflict, and failure remain
typed results rather than `null`, unchecked extraction, sentinels, or ordinary exceptions.

The platform baseline remains Scala 3.8.x and JDK 17 unless an explicit compatibility RFC changes it. Mature
dependencies are admitted for a named mechanism in the narrowest owning layer and configuration; independently
released libraries keep independent version coordinates. Public APIs may use advanced Scala and functional structure
when it prevents invalid states or preserves required evidence, while common calls remain domain-named and readable.

## Slices

### S-01-project-foundation: Enforce the architecture and functional-design contract

- AC-001 [evidence: both]: Every nontrivial delivery identifies one primary owner for each concept and error, states
  allowed and forbidden dependencies, and demonstrates an acyclic dependency direction without presenting a proposed
  module or API as implemented.
- AC-002 [evidence: both]: Every external-to-trusted transition states its boundary representation, coherent resolution
  view, validation stages, retained evidence, and typed failure model; independent failures accumulate deterministically
  while dependent checks run only from prerequisite evidence.
- AC-003 [evidence: automated]: Pure mathematical, reference-data, domain, and economic artifacts remain independent of
  live catalogs, codecs, concrete effects, runtime state, and higher-level policy, with packaged downstream compiler or
  module-boundary checks where ordinary unit tests cannot prove the claim.
- AC-004 [evidence: both]: Algebraic, refinement, type-authority, interpreter, concurrency, complexity, and hot-path
  claims receive evidence proportional to the claim, including laws, negative compiler fixtures, shared interpreter
  contracts, deterministic probe bounds, or representative benchmarks where applicable.
- AC-005 [evidence: human]: Review confirms that advanced Scala or functional abstractions protect real semantics while
  common public calls remain domain-readable, unavoidable unsafe mechanisms are quarantined behind checked invariants,
  and any charter exception is handled as an explicit design decision.

## Risks

- The charter can become diagram-driven ceremony. Mitigation: logical responsibility precedes physical modules, and
  empty or speculative modules are forbidden.
- Type-level rigor can make ordinary use opaque. Mitigation: require small domain-named operations, representative
  examples, and focused inference or negative-compilation evidence.
- Governance summaries can drift from detailed contracts. Mitigation: keep the two canonical capability specs
  normative and validate each delivery against their current revisions.
- A temporary architecture exception can become permanent. Mitigation: record transitional ownership in the charter
  audit and require a separate accepted design decision before changing a settled boundary.
