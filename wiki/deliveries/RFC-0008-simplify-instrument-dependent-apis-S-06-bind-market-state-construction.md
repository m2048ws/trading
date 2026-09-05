---
type: delivery
updated: 2026-09-05
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-06-bind-market-state-construction
change: bind-market-state-construction
status: archived
archived: 2026-09-05
evidence_manifest: sha256:bb970e1c554cb05bb7dee15c534853753af47e55c5339d4c6aefb796b290c36c
source_digest: sha256:7228b2dcc47996a30649f1192bf82b83a696dacc62686de4ab86fffbe432c0ae
---

# RFC-0008-simplify-instrument-dependent-apis/S-06-bind-market-state-construction

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
| AC-022 | automated | file:target%2Fcorgi-evidence%2Fs06-verify%2Fsource-review.json#sha256:b2d70601772ce3b4ef252893b8b0cd9e6731cf3679f52df01af451e643c898c7; file:target%2Fcorgi-evidence%2Fs06-verify%2Ffull-gate.log#sha256:e5dc40e316f96c36a8e210e725499f4a2120dc317d22d37fe7dbcf62f6052400; file:target%2Fcorgi-evidence%2Fs06-verify%2Fapply-baseline-pass.log#sha256:d384da983090a3b7b04a374c6912c6a93013a49fd17edecb5aaf7a4c6d98f6b4; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fhuman-confirmation.json#sha256:16639a3d3c3fba86cc287652795fc884beb8fb099e249dea5119a5f75fb8618d; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fqa-observations.json#sha256:42597138c4390fcad41d336a7e629347a5a36bcb62431fd7c6576b70bd0da609; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fwalkthrough.txt#sha256:cc10a3af2e279a50e783667fe9e91d94fca657dbe7dc232b39fc450b568875f0 | PASS |
| AC-023 | automated | file:target%2Fcorgi-evidence%2Fs06-verify%2Ffull-gate.log#sha256:e5dc40e316f96c36a8e210e725499f4a2120dc317d22d37fe7dbcf62f6052400; file:target%2Fcorgi-evidence%2Fs06-verify%2Fsource-review.json#sha256:b2d70601772ce3b4ef252893b8b0cd9e6731cf3679f52df01af451e643c898c7; file:target%2Fcorgi-evidence%2Fs06-verify%2Fscope-bytecode.txt#sha256:0935a09698094e8cae15b040f16a92bb2737329bfa401477c91bd1c660d2fe55; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fhuman-confirmation.json#sha256:16639a3d3c3fba86cc287652795fc884beb8fb099e249dea5119a5f75fb8618d; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fqa-observations.json#sha256:42597138c4390fcad41d336a7e629347a5a36bcb62431fd7c6576b70bd0da609; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fwalkthrough.txt#sha256:cc10a3af2e279a50e783667fe9e91d94fca657dbe7dc232b39fc450b568875f0 | PASS |
| AC-024 | automated | file:target%2Fcorgi-evidence%2Fs06-verify%2Fapply-baseline-pass.log#sha256:d384da983090a3b7b04a374c6912c6a93013a49fd17edecb5aaf7a4c6d98f6b4; file:target%2Fcorgi-evidence%2Fs06-verify%2Ffull-gate.log#sha256:e5dc40e316f96c36a8e210e725499f4a2120dc317d22d37fe7dbcf62f6052400; file:target%2Fcorgi-evidence%2Fs06-verify%2Fsource-review.json#sha256:b2d70601772ce3b4ef252893b8b0cd9e6731cf3679f52df01af451e643c898c7; file:target%2Fcorgi-evidence%2Fs06-verify%2Fwhole-change-assessment.json#sha256:56ffef771937ed1dcf095e04df184e7579f8795cb156310d3006bfb6d4e6c8fe; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fhuman-confirmation.json#sha256:16639a3d3c3fba86cc287652795fc884beb8fb099e249dea5119a5f75fb8618d; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fqa-observations.json#sha256:42597138c4390fcad41d336a7e629347a5a36bcb62431fd7c6576b70bd0da609; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fwalkthrough.txt#sha256:cc10a3af2e279a50e783667fe9e91d94fca657dbe7dc232b39fc450b568875f0 | PASS |
| AC-025 | automated | file:target%2Fcorgi-evidence%2Fs06-verify%2Ffull-gate.log#sha256:e5dc40e316f96c36a8e210e725499f4a2120dc317d22d37fe7dbcf62f6052400; file:target%2Fcorgi-evidence%2Fs06-verify%2Fsource-review.json#sha256:b2d70601772ce3b4ef252893b8b0cd9e6731cf3679f52df01af451e643c898c7; file:target%2Fcorgi-evidence%2Fs06-verify%2Fscope-bytecode.txt#sha256:0935a09698094e8cae15b040f16a92bb2737329bfa401477c91bd1c660d2fe55; file:target%2Fcorgi-evidence%2Fs06-verify%2Fwhole-change-assessment.json#sha256:56ffef771937ed1dcf095e04df184e7579f8795cb156310d3006bfb6d4e6c8fe; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fhuman-confirmation.json#sha256:16639a3d3c3fba86cc287652795fc884beb8fb099e249dea5119a5f75fb8618d; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fqa-observations.json#sha256:42597138c4390fcad41d336a7e629347a5a36bcb62431fd7c6576b70bd0da609; file:target%2Fcorgi-evidence%2Fs06-human-qa%2Fwalkthrough.txt#sha256:cc10a3af2e279a50e783667fe9e91d94fca657dbe7dc232b39fc450b568875f0 | PASS |

## Implementation
- Task Group 1: `7012636e9ff868ec14daa1be58c0cc59ac426b67`
- Final HEAD: `7012636e9ff868ec14daa1be58c0cc59ac426b67`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: pass by m2048ws — Human explicitly reported QA passed for the retained S-06 guided consumer walkthrough; reviewer identity was supplied in this conversation. This is a QA pass, not a skip or waiver.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-05-bind-market-state-construction`
- `openspec/changes/archive/2026-09-05-bind-market-state-construction/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/63
