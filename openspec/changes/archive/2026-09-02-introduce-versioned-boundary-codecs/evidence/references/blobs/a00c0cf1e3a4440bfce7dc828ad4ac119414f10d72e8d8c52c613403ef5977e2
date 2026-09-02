# RFC-0002-architecture-portfolio: Convert the planned architecture portfolio into Corgi deliveries

## Goal

Deliver five already-designed architecture changes as independent, traceable Corgi changes without weakening their
existing semantics. Each delivery receives one RFC Slice, GitHub Issue, isolated worktree, `corgi/<change>` branch,
exclusive Run Contract claim, and draft PR. Separate slices may proceed concurrently when their declared dependencies
are closed; Task Groups within one change remain sequential.

The preserved planning source is commit `e6c6f9c4eb4d75087b5a81e307e5ea6bf00a21b1`. Exact source-tree identities,
conversion disposition, and dependency edges are recorded in `tools/corgi/migration-manifest.json`. Agent-assisted
rehydration must retain the original change names and reconcile each proposal, design, delta specification, and task
list into the Corgi source/traceability contract before Apply.

## Non-goals

- Convert or take ownership of `establish-pure-instrument-economics`, which is being implemented in a separate clone
  and will integrate later with its legacy change archived.
- Implement production code, claim a delivery, or manufacture completion while converting its planning artifacts.
- Parallelize Task Groups within one change or split one preserved change into multiple implementation PRs.
- Add market-data, persistence, venue execution, portfolio accounting, or other capabilities excluded by the preserved
  designs.
- Treat proposed target modules or APIs as already implemented.
- Resolve future semantic conflicts by silently changing an accepted Slice; a material boundary change requires an RFC
  Amendment.

## Boundary

The five Slices are planning-authority boundaries, not substitutes for their detailed capability specifications. Every
rehydrated change must be semantically compared with its source Git tree, pass strict planning readiness, and retain
the Foundation RFC obligations for ownership, dependency direction, typed validation, effects, and evidence.

`introduce-application-and-runtime-foundation` is independent of the externally implemented instrument-economics
change. `separate-order-and-execution-scenario-modules` and `introduce-pure-risk-module` are blocked by that external
delivery. `introduce-pure-fee-policy-module` and `introduce-versioned-boundary-codecs` are blocked by the order/scenario
delivery. These edges are projected as native GitHub `blockedBy` relationships after Propose; the claim adapter refuses
Apply until every native dependency is closed.

All five deliveries retain the pre-release breaking-change policy: superseded packages and APIs are removed rather
than kept as speculative aliases. Pure artifacts remain free of concrete effects and live state. External identities
and durable records cross checked reconstruction boundaries against explicit immutable state. New dependencies remain
in the narrowest owning module and configuration and preserve the JDK 17 minimum.

## Slices

### S-01-application-runtime-foundation: Establish effect-polymorphic application and concrete runtime boundaries

- AC-001 [evidence: automated]: `trading-application` remains the minimal effect-polymorphic workflow/port boundary and
  `trading-runtime` owns concrete effects, resources, concurrency, streams, clients, telemetry, and interpreters;
  packaged dependency checks prove pure artifacts do not acquire runtime dependencies.
- AC-002 [evidence: both]: A Cats Effect in-memory `LiveCatalog` interpreter gives one stable lineage and interprets the
  existing pure catalog transition atomically, with coherent snapshot/commit behavior under racing commits,
  cancellation, retry, and contention.
- AC-003 [evidence: human]: Port admission keeps commands, queries, events, expected outcomes, time semantics,
  transaction boundaries, and guaranteed business recording explicit while rejecting speculative capabilities,
  service locators, concrete runtime leakage, and telemetry-as-audit substitutions.
- AC-004 [evidence: automated]: Shared interpreter contracts, deterministic concurrency/cancellation tests, packaged
  boundary fixtures, dependency inspection, and representative benchmarks verify observable semantics and cost.

### S-02-order-execution-scenarios: Separate immutable orders from hypothetical execution evidence

- AC-005 [evidence: automated]: Pure `trading-order-model` and `trading-execution-scenario` artifacts enforce the
  one-way dependency from scenarios to orders and instrument economics; completed-artifact checks reject reverse,
  effect, policy, codec, and runtime dependencies.
- AC-006 [evidence: both]: Immutable order alternatives, activation, pricing, duration, visibility, liquidity, and
  position intent form compositional ADTs whose invalid combinations are unrepresentable and whose associated evidence
  remains tied to the exact instruction alternative.
- AC-007 [evidence: automated]: Scenario construction owns its target order exactly once, requires non-empty matched
  slices, accumulates independent violations deterministically, sequences evidence-dependent checks, and returns
  complete checked outcomes without duplicated identity claims.
- AC-008 [evidence: both]: Checked round trips, downstream fee/risk consumption, behavioral equivalence, negative
  compiler fixtures, and removal of the superseded aggregate order/scenario API demonstrate the new boundary without
  compatibility aliases.

### S-03-pure-risk: Introduce constructive monotone lot-risk sizing

- AC-009 [evidence: automated]: Pure `trading-risk` depends only on quantities and instrument economics, uses refined
  nonnegative typed risk values, owns its errors, and removes the final risk sources plus the now-empty transitional
  economics aggregate without importing scenario, fee, application, codec, or runtime dependencies.
- AC-010 [evidence: both]: Opaque monotone lot-risk models can be constructed only from checked exact curve
  representations and law-preserving combinators; callers cannot certify arbitrary functions through flags, casts,
  tokens, or unchecked evidence.
- AC-011 [evidence: automated]: Maximum-affordable positive lot sizing uses logarithmically many distinct observations
  and returns explicit boundary evidence for the selected maximum or an explicit no-affordable result retaining the
  assessed one-lot boundary.
- AC-012 [evidence: both]: An explicitly linear exhaustive fallback handles genuinely arbitrary finite evaluations,
  while typed failures, algebra/property laws, probe-count checks, packaged boundaries, and representative benchmarks
  verify correctness and bounded scope.

### S-04-fee-policy: Separate contextual fee policy from instrument economics

- AC-013 [evidence: automated]: Pure `trading-fee-policy` depends on instrument economics, order model, and execution
  scenarios in one direction; module and completed-artifact checks reject effects, runtime state, and reverse
  dependencies.
- AC-014 [evidence: both]: Open pure fee strategies express percentage, minimum, tier, maker/taker, account, and schedule
  rules with refined exact quantities; stable composition accumulates independent policy/output failures without
  claiming unlawful cross-instrument algebra.
- AC-015 [evidence: automated]: The scenario-owned assessment boundary validates requested slice attribution against the
  exact immutable scenario and derives source market context rather than accepting caller-forged indices, markets, or
  reference-equality reconciliation.
- AC-016 [evidence: both]: Exact round-trip price normalization and fee-inclusive PnL preserve typed quantities,
  conversion provenance, epistemic scope, and detailed breakdowns while removing superseded fee schedule, line,
  instrument-service, and valuation entry points without aliases.

### S-05-versioned-boundary-codecs: Add canonical durable records and checked reconstruction

- AC-017 [evidence: automated]: Pure `trading-boundary-codecs` sits above the domain, defines independently versioned
  canonical JSON envelopes and exact non-floating primitive encodings, enforces bounded strict parsing, and exposes no
  parser-library, effect, live-catalog, or generic object-mapping types.
- AC-018 [evidence: both]: Grid coordinates, instrument definitions, immutable orders, scenarios, and round trips store
  stable IDs plus exact primitive data and reconstruct proof-carrying values only through explicit snapshots and the
  owning domain assemblers or smart constructors.
- AC-019 [evidence: automated]: Versioned catalog journal entries represent successful publications and replay
  sequentially into a caller-supplied fresh root, rejecting revision gaps, conflicts, no-op entries, reordered history,
  and serialized construction authority deterministically.
- AC-020 [evidence: both]: Independent JSON schemas, canonical golden vectors, round-trip/model properties, malformed
  and adversarial payloads, batch snapshot-coherence tests, compatibility documentation, and dependency inspection
  verify durable behavior and the explicit exclusion of unspecified live or derived records.

## Risks

- The external instrument-economics implementation can change assumptions used by four Slices. Mitigation: keep native
  dependencies closed and reconcile the affected planning artifacts against its merged archived result before claim.
- Mechanical conversion can lose a detailed scenario while preserving only prose intent. Mitigation: compare every
  source tree and requirement heading, retain delta specifications, and require strict Corgi readiness plus semantic
  review before Apply.
- Five concurrent worktrees can produce integration drift. Mitigation: use one exclusive claim and draft PR per Slice,
  fast-forward-only publication, native dependency edges, exact-SHA review, and sequential Task Groups.
- Large breaking changes can leave the repository temporarily unbuildable. Mitigation: preserve the declared delivery
  order and require each Task Group commit to be a validated buildable checkpoint.
- Corgi 4.0.0-rc2 has known bootstrap/skill-verification inconsistencies. Mitigation: keep the exact package pin,
  preserve the migration source and rollback refs, use doctor plus lifecycle-specific checks, and retain the defects as
  evidence for the final adoption decision.
