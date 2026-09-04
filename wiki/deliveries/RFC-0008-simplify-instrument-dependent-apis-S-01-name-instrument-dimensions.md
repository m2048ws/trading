---
type: delivery
updated: 2026-09-04
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-01-name-instrument-dimensions
change: name-instrument-dimensions
status: archived
archived: 2026-09-04
evidence_manifest: sha256:f51be78a9c5b67c22ac29adfeebbb80bdbddd712d4c29f51933da3fbad945e3f
source_digest: sha256:31c852e3a0a7b1b0448b20053d68fe292aa8996aed9992653f3a7d6e7895ce73
---

# RFC-0008-simplify-instrument-dependent-apis/S-01-name-instrument-dimensions

## Outcome
Make common, already well-typed Scala calls concise when several operations share one assembled instrument, execution
lifecycle, or immutable catalog snapshot. Each owning module shall provide a small immutable scope that captures its
stable context once, retains the exact path-dependent relationships established by that context, and exposes
domain-named operations without requiring callers to repeat deeply nested role-dimension projections.

The pattern shall cover the current high-value surfaces: standard order construction, lifecycle-bound source facts,
scenario-record encoding and reconstruction, instrument-specific risk construction and exhaustive sizing, and market
state construction. `Instrument` shall expose direct aliases for its position, base, quote, and settlement dimensions
so owner-local signatures can name those established types without reopening the roles representation.

Conciseness must not weaken semantics. The new scopes shall share the existing checked implementation, preserve precise
result and error types, retain instrument identity and grid validation, and keep every successful result as
specifically typed as the current direct operation. Ordinary callers shall be able to reuse a bound scope without
explicit dimension type arguments, unchecked casts, structural refinements, or duplicated local aliases.

## Boundary Delivered
This RFC introduces an owner-local binding pattern, not a new architectural layer. A bound scope is an immutable value
created by the companion or facade that already owns the operations. It captures one stable trusted value, or the
smallest coherent product of stable values the operations genuinely share. Its methods retain the captured value's
singleton relationship so Scala can infer dependent parameter and result types. The scope neither changes the captured
value nor acquires resources, performs I/O, coordinates shared state, or memoizes results.

`trading-instrument-economics` shall add direct `PositionD`, `BaseD`, `QuoteD`, and `SettleD` type members to
`Instrument`. Each is exactly an alias of the corresponding role dimension; it creates no new dimension, evidence, or
runtime representation. Existing `Lots`, `PositionLots`, `Price`, `MarketState`, `PricePnl`, and `Pnl` members retain
their meanings and may use the direct aliases internally.

`trading-order-model` shall own an instrument-bound standard-order construction scope. It captures one exact
instrument and provides the existing market, limit, stop-market, and stop-limit conveniences with the same defaults,
validation, activation/execution refinements, and accumulated `OrderViolations`. Generic checked construction that
accepts independently supplied intent, activation, and execution values remains available; binding the conveniences
shall not narrow or bypass that validation surface.

`trading-execution-lifecycle` shall own a lifecycle-bound source-fact construction scope. It captures one exact
`ExecutionLifecycle` and provides construction for accepted, rejected, fill, correction, bust, cancellation-effective,
reconciliation-checkpoint, source-order-completed, and source-order-absent facts. It reuses the existing fact owners and
`SourceFactViolations`, including logical-order, target, instrument, grid, ordering, modifier-reference, checkpoint,
and completeness checks. Commands, dispatch, state initialization, replay, observation, and effective-fill derivation
remain outside this scope because they do not share the same construction context or ownership.

`trading-boundary-codecs` shall distinguish two immutable scenario-record contexts. An encoder captures only the exact
instrument needed to turn typed order and round-trip scenarios into records and canonical wire forms. A decoder
captures that exact instrument plus one immutable `CatalogSnapshot` used coherently for reconstruction. Record parsing,
record-only encoding, and schemas remain context-free. Decode limits and record indices remain explicit operation
inputs. Both scenario families preserve their current wire versions, canonicalization, locations, accumulated errors,
batch ordering, and exact reconstructed dependent types.

`trading-risk` shall own an instrument-bound risk scope for downside measurement, monotone lot-risk construction, and
arbitrary exhaustive lot sizing. It may expose local aliases for the captured instrument's position and settlement
dimensions, loss, budget, model, assessment, and decision types. `single` and complete-table construction shall retain
their deliberately broad existential inputs and runtime identity/dimension validation. Model-to-model combinators and
`MaxAffordableLots.select` remain model-bound operations because their semantics do not require an instrument scope.

`trading-instrument-economics` shall consolidate each pair of market-state overloads into one operation whose
additional conversions default to the empty vector, and shall expose the same eight construction modes through an
instrument-bound market-state scope: quote-settled, base-settled, quote anchor, base anchor, both anchors, quote rate,
base rate, and both rates. Settlement-conversion construction stays separately owned rather than expanding the scope
into a general economics facade.

Current companion entry points may remain as thin delegates where source compatibility is intentionally retained, but
there shall be one implementation of each validation or calculation. Moving a check into scope creation is allowed only
when it depends solely on captured context and preserves observable failure and null behavior; otherwise validation
remains at its current operation. Every scope remains safe to reuse concurrently because it is pure and immutable.

Each Slice shall include completed-artifact Scala compiler fixtures demonstrating the concise supported call and nearby
invalid calls. Positive fixtures shall not spell explicit dimension type arguments or
`instrument.roles.<role>.D` at the call site. Negative fixtures shall prove that binding does not permit values from
incompatible dimensions or contexts. Focused tests shall compare scoped and characterized current operations for exact
success, failure ordering, and result refinement.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-001 | automated | file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FInstrument.scala#sha256:275ca8d2efef5a821083cdc64502e8c589c65ff430278b914f26bff982b39a7d; file:instrument-economics%2Fsrc%2Ftest%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FPureInstrumentEconomicsSuite.scala#sha256:89db5fd7ce25934d22280b9d86cefd3f304e47200bf0549c1a34fda444e93c51 | PASS |
| AC-002 | automated | file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-core-compiler%2FPureCoreClient.scala#sha256:65a38e67240c02ac126092a6a77319bd5ec5bd13083528dcc87fa836a7c67ffc; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-core-compiler%2FInstrumentDimensionAliasMismatch.scala#sha256:092210c340fdef0e25383cb536db7d201a79ca19aa04d37c5d4278b8e1339db5; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:378547036b76f95d012005b840c2331142e4342f6e61bbf13ec328f50ec2b14d | PASS |
| AC-003 | automated | file:build.sbt#sha256:790e4ae7fa4ab9334b36c0273bc4d44d02d59cac352b50f5f0aeaadc026f5db8; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-core-compiler%2FCoreHasNoDownstream.scala#sha256:98f3d7e886ebfc2310849e97f9bc143dbff07c0864da874579c5d5d89d2d3f36; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:378547036b76f95d012005b840c2331142e4342f6e61bbf13ec328f50ec2b14d; file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FInstrument.scala#sha256:275ca8d2efef5a821083cdc64502e8c589c65ff430278b914f26bff982b39a7d | PASS |

## Implementation
- Task Group 1: `2e69c260793d85d1c9e1b4930a908421eaef2bb5`
- Final HEAD: `2e69c260793d85d1c9e1b4930a908421eaef2bb5`

## Review and QA
- Human Review: approve by requesting-user
- Human QA: skipped by requesting-user — The human requester authorized QA to be skipped for S-01 because the verified delivery adds transparent compile-time dimension aliases only and changes no runtime representation, behavior, service, wire format, or runnable user surface.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-04-name-instrument-dimensions`
- `openspec/changes/archive/2026-09-04-name-instrument-dimensions/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/53
