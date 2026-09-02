---
type: delivery
updated: 2026-09-02
rfc: RFC-0003-execution-lifecycle-foundation
slice: S-01-actual-execution-lifecycle
change: establish-actual-execution-lifecycle
status: archived
archived: 2026-09-02
evidence_manifest: sha256:65afe49168a3c223c12556a2ffdfea42e0244580b2d29a30b3d5365b9d28c2e6
source_digest: sha256:f11a68db674bf8a6db3dbf79a9dc4d81df5403a8e597c182714ae2d4a296b5fc
---

# RFC-0003-execution-lifecycle-foundation/S-01-actual-execution-lifecycle

## Outcome
Establish the first actual order-execution boundary above immutable orders. Applications shall be able to submit and
cancel one logical order, retain authoritative acknowledgements and fills from live or deterministic execution
sources, reconcile uncertain outcomes, and rebuild the same checked lifecycle from replay without confusing
hypothetical scenarios with real execution facts.

The feature begins with a pure execution-lifecycle model, then admits one narrow effect-polymorphic application
capability and deterministic runtime interpreter, and finally adds independently versioned durable execution records.
The resulting boundary shall preserve exact instrument-bound lots and prices, source identity and provenance,
idempotency, gaps, conflicts, corrections, and externally reported anomalies rather than reducing execution to a
mutable status flag or best-effort telemetry.

## Boundary Delivered
The pure owner is a real `trading-execution-lifecycle` artifact under package root `trading.execution`. It consumes the
immutable order model and the instrument-economic values required by actual fills. Quantities, instrument economics,
and the order model remain independent of actual execution; execution lifecycle remains independent of hypothetical
scenarios, fee policy, risk, application, runtime, and codecs. `trading-application` may consume the lifecycle artifact,
`trading-runtime` may interpret the application capability, and `trading-boundary-codecs` may later encode the pure
execution values in that one-way direction.

The application owns command authority. Every submit or cancel command has a stable application-issued command
identity, and every submitted immutable order has a distinct stable logical execution-order identity. Re-delivering the
same command identity and normalized body is an idempotent retry; reusing that identity with different content is a
typed conflict. Reconciliation is an explicit query over source/order identity and prior evidence rather than another
state-changing command. Transport attempts, fibers, HTTP calls, reconnects, and queue deliveries do not become new
business commands.

Execution sources own reported-fact authority. Source and execution-account identity qualify native source-event,
venue-order, and fill identifiers according to the source's documented uniqueness scope. A normalized fill retains
that source reference and exact economic provenance. Identical price, lots, and time under different fill identities
remain distinct fills. The same qualified fill identity with identical content is a duplicate report; conflicting
content is a typed source conflict unless a new correction or bust fact explicitly references the original fill.

Ordering is evidence, not inference. Application commands may be sequenced per logical execution order. A source event
is ordered only inside the account/stream scope for which the source supplies authoritative sequence evidence; an
unsequenced source fact remains explicitly unsequenced. Network delivery and timestamps provide no business ordering
authority. Replay detects duplicate and conflicting positions, gaps, and rewinds. Later facts may be retained across a
gap, but no result claims a complete authoritative lifecycle until the gap or unresolved reference is reconciled.

Submission outcomes distinguish confirmed acceptance, confirmed rejection, proof that dispatch did not cross the
external boundary, and an indeterminate outcome where an external effect may have occurred. Indeterminate submission is
epistemic state, not rejection or a venue status. It prevents a fresh duplicate submission while permitting
source-supported lookup, idempotent retry with the original identities, reconciliation, or safe defensive cancellation.
A later authoritative fill proves execution occurred without manufacturing a missing acceptance event. Absence proves
non-acceptance only when the source contract supplies an authoritative completeness boundary.

Cancel requests and cancellation effectiveness are separate facts. Fills racing cancellation are retained and
reconciled through source ordering and exact cumulative quantities; delivery after a cancellation message is not by
itself contradictory. A fill that is provably post-cancellation, an overfill, or another authoritative invariant breach
is retained as exposure together with a typed anomaly rather than discarded. Confirmed cancel-then-submit may link two
immutable execution-order identities under one mechanism-neutral lineage. Native amendment receives no public
capability until a later RFC defines cutover, priority, identity, quantity-basis, and racing-fill semantics.

The application capability is request/reconciliation shaped rather than a general environment or stream registry. It
accepts pure submit/cancel commands and explicit reconciliation queries, then returns typed expected outcomes or
coherent reconciliation results. Its deterministic runtime interpreter exercises retries, duplicates, gaps,
indeterminate submission, corrections, and cancel/fill races against the same pure transition model. Concrete
scheduling, buffering, cancellation mechanics, and mutable coordination remain runtime-private.

Durable execution records, when introduced, belong to `trading-boundary-codecs` and use the established canonical
versioned-envelope, exact primitive, bounded decoding, schema, and golden-vector mechanisms. Records store stable IDs,
source-qualified references, exact data, and explicit facts; they never store trusted handles, effects, clients,
mutable state, or construction authority. Decoding reconstructs values through the lifecycle-owned constructors and
replay transition. Authentication, authorization, signing, encryption, transport framing, and storage retention remain
outside this feature.

`S-01-actual-execution-lifecycle` depends only on the delivered immutable-order and instrument-economic boundaries.
`S-02-execution-application-boundary` is blocked by S-01. `S-03-versioned-execution-records` is blocked by S-01 and by
effective closeout of RFC-0002/S-05 versioned boundary codecs; it need not wait for S-02 unless planning discovers a
record whose meaning depends on an application workflow.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-001 | automated | file:build.sbt#sha256:47ece9b184d750d417cde80384dddf73905fabc118ed6dd2de3a4f98cb9a0b50; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:ab629426812446bec0ab2b3b69db53d493415af36fbf9676eaa2f12bb85f636e; file:docs%2Farchitecture-charter-audit.md#sha256:c31ef09cee1eeb5088620bf4b03137279b1cfbd2ec46c7873eb8765834b13d3f; file:docs%2Fexecution-lifecycle-evidence.md#sha256:8100c51d4be9216c73375cdbfbac5d9aaa926867e34dd89fbf53df4de42f6d3a | PASS |
| AC-002 | both | file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionIdentitySuite.scala#sha256:f71cd6cd67097e8bed468efd9a36ab13255b9d020ad0429266889169543ebbd2; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionAuthoritySuite.scala#sha256:42aaa08fec9dab65572a630c5d7a39fb43aad1ad19042cc38dc5ed502e959a41; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCommandStateSuite.scala#sha256:296a5961b2bedd06e9048eb461851c8635e2dfabb7c55486853f4b67c61f03e7; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactSuite.scala#sha256:b35c095fd77fb2b08307272e4aeafd6cc28acdf57325bb6fc15f61eded2403a8; file:docs%2Fexecution-lifecycle-evidence.md#sha256:8100c51d4be9216c73375cdbfbac5d9aaa926867e34dd89fbf53df4de42f6d3a | PASS |
| AC-003 | automated | file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionStateSuite.scala#sha256:eaeeabc367a967ca53a9f57ae3356c51d21a14ae10ca93503de8a034c4408278; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactSuite.scala#sha256:b35c095fd77fb2b08307272e4aeafd6cc28acdf57325bb6fc15f61eded2403a8; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedgerSuite.scala#sha256:39940082e89fab6be9c73ec9dcc3f1943fd9f74049eb3e4a5f4151bf03133595; file:docs%2Fexecution-lifecycle-evidence.md#sha256:8100c51d4be9216c73375cdbfbac5d9aaa926867e34dd89fbf53df4de42f6d3a | PASS |
| AC-004 | both | file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSubmissionKnowledgeSuite.scala#sha256:754745b43d54efa245d03a9b3808e0e31583190268c8b695f32136655db77135; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCommandStateSuite.scala#sha256:296a5961b2bedd06e9048eb461851c8635e2dfabb7c55486853f4b67c61f03e7; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:ab629426812446bec0ab2b3b69db53d493415af36fbf9676eaa2f12bb85f636e; file:docs%2Fexecution-lifecycle-evidence.md#sha256:8100c51d4be9216c73375cdbfbac5d9aaa926867e34dd89fbf53df4de42f6d3a | PASS |
| AC-005 | both | file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedgerSuite.scala#sha256:39940082e89fab6be9c73ec9dcc3f1943fd9f74049eb3e4a5f4151bf03133595; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCancellationSuite.scala#sha256:9d34276c959a624714a55747e31b04e7460c1acfa38b3b90a338898031d7bea0; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:ab629426812446bec0ab2b3b69db53d493415af36fbf9676eaa2f12bb85f636e; file:docs%2Fexecution-lifecycle-evidence.md#sha256:8100c51d4be9216c73375cdbfbac5d9aaa926867e34dd89fbf53df4de42f6d3a | PASS |

## Implementation
- Task Group 1: `cc8162464633fdeb6c5006f2ce10c0608cdff32e`
- Task Group 2: `42d2379dbea0ab794e660e8860123190ea2e608a`
- Task Group 3: `3576953dc5f3158ad554c97df0dcc64bae2eb21c`
- Task Group 4: `04feb6d34ddc846215e57ed3a81f2fd1b22d44fe`
- Task Group 5: `b1b6b336ad5dce2ba5c48fa13de2cf40e528fee9`
- Task Group 6: `6c5c0dddd12f8949b924f29c249ff58afd06caf0`
- Task Group 7: `e0e060146cbff413d8870f4e00a03cd33dcb9386`
- Task Group 8: `69c7e5e8ad041d167e2ab1456a988f87225538c1`
- Task Group 9: `4f8d8194fe1bfba185c9effb6ab2ca6b1a653a41`
- Final HEAD: `4f8d8194fe1bfba185c9effb6ab2ca6b1a653a41`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human confirmed no runtime impact: RFC-0003 S-01 delivers a pure lifecycle library with no runnable runtime surface or production consumer.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0003-execution-lifecycle-foundation`
- `openspec/changes/archive/2026-09-02-establish-actual-execution-lifecycle`
- `openspec/changes/archive/2026-09-02-establish-actual-execution-lifecycle/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/34
