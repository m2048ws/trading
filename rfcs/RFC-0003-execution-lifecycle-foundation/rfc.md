# RFC-0003-execution-lifecycle-foundation: Establish actual execution lifecycle semantics

## Goal

Establish the first actual order-execution boundary above immutable orders. Applications shall be able to submit and
cancel one logical order, retain authoritative acknowledgements and fills from live or deterministic execution
sources, reconcile uncertain outcomes, and rebuild the same checked lifecycle from replay without confusing
hypothetical scenarios with real execution facts.

The feature begins with a pure execution-lifecycle model, then admits one narrow effect-polymorphic application
capability and deterministic runtime interpreter, and finally adds independently versioned durable execution records.
The resulting boundary shall preserve exact instrument-bound lots and prices, source identity and provenance,
idempotency, gaps, conflicts, corrections, and externally reported anomalies rather than reducing execution to a
mutable status flag or best-effort telemetry.

## Non-goals

- Do not define multi-order trade campaigns, entry/scale-in/take-profit relationships, portfolio accounting, realized
  versus unrealized attribution, campaign PnL, or campaign risk.
- Do not add market-data acquisition, matching logic, smart-order routing, venue selection, account balances, margin,
  buying-power checks, or enforcement of `PositionEffect.ReduceOnly` against account state.
- Do not add a live venue or broker client, database repository, transaction/outbox mechanism, background supervisor,
  generic stream algebra, filesystem/network transport, telemetry backend, operational dashboard, or deployment
  configuration.
- Do not implement native atomic amendment or atomic cancel-replace. The model may retain mechanism-neutral order
  lineage used by confirmed cancel-then-submit, but it shall expose no throwing, commented-out, or falsely supported
  native-amend operation.
- Do not treat timestamps, receive order, random receipt IDs, hashes of economic fields, or payload equality as
  authoritative event or fill identity.
- Do not make telemetry an execution audit record, serialize trusted JVM object graphs, or expose effect-runtime,
  parser-library, transport-client, or venue-SDK types in domain or application contracts.
- Do not introduce multi-instrument fills, allocations spanning several accounts, or a generic account/portfolio domain
  in this feature. One lifecycle is scoped to one immutable order and one execution account.

## Boundary

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

## Slices

### S-01-actual-execution-lifecycle: Define authoritative facts and deterministic reconciliation

- AC-001 [evidence: automated]: A pure `trading-execution-lifecycle` artifact owns closed command, source-fact,
  lifecycle-state, reconciliation-result, anomaly, and error vocabularies; completed-artifact checks prove the
  quantities, instrument-economics, order-model, execution-scenario, fee-policy, and risk artifacts cannot depend on it,
  while it imports no application, runtime, codec, concrete effect, stream, client, persistence, or telemetry mechanism.
- AC-002 [evidence: both]: Stable command, logical execution-order, lineage, source, execution-account, source-event,
  source-order, and fill identities have documented authorities and scopes. Model examples and adversarial tests prove
  idempotent same-identity replay, typed conflicting reuse, distinct economically identical fills, source-qualified
  deduplication, and retained native provenance without relying on random receipt IDs or payload hashes.
- AC-003 [evidence: automated]: The pure lifecycle transition is deterministic under replay and distinguishes
  authoritative sequenced from explicitly unsequenced facts. It detects duplicates, conflicting stream positions,
  gaps, rewinds, unresolved references, fill corrections, and busts while retaining later evidence without claiming
  completeness across missing authority.
- AC-004 [evidence: both]: Submission distinguishes accepted, rejected, proven-not-dispatched, and indeterminate
  outcomes. Checked examples demonstrate same-identity idempotent recovery, prevention of fresh duplicate submission,
  later acknowledgement/rejection/fill reconciliation, authoritative versus non-authoritative absence, and a fill that
  proves execution without fabricating the missing acceptance event.
- AC-005 [evidence: both]: Cancel request and effective cancellation remain distinct; partial fills, cancel/fill races,
  late delivery, corrections, overfills, and provable post-cancellation anomalies preserve exact exposure and source
  evidence. Confirmed cancel-then-submit links immutable predecessor and successor identities through lineage, while
  packaged API tests prove no native atomic amend/cancel-replace method or throwing placeholder exists.

### S-02-execution-application-boundary: Admit a narrow capability and deterministic interpreter

- AC-006 [evidence: automated]: `trading-application` exposes a minimal effect-polymorphic execution capability for
  submission, cancellation, and reconciliation using only execution-owned commands and typed results. It exposes no
  concrete runtime, client, queue, fiber, stream, transaction, telemetry, codec, or universal application-error type,
  and pure domain operations remain ordinary functions.
- AC-007 [evidence: both]: A deterministic scripted runtime interpreter supports a complete user path from immutable
  order submission through acceptance or indeterminate delivery, duplicate retry, one or more partial fills,
  reconciliation, and effective cancellation. It uses the same pure lifecycle transition and retains exact identities,
  ordering evidence, exposure, and anomalies observable through the application contract.
- AC-008 [evidence: automated]: A reusable application-level contract suite and interpreter-specific tests verify
  idempotent retry, command conflict, source duplicate/conflict, ordered-gap and unsequenced behavior, correction/bust,
  cancellation, effect cancellation around a possibly crossed external boundary, and deterministic reconstruction under
  repeated and concurrent interpretation.
- AC-009 [evidence: both]: Public examples keep venue-specific mechanisms in runtime wiring, make reconciliation an
  explicit business operation, and demonstrate that the capability has no native amend, live venue, market-data,
  account-state, campaign, PnL, risk, persistence, generic streaming, or service-locator promise.

### S-03-versioned-execution-records: Add canonical durable commands and facts

- AC-010 [evidence: automated]: `trading-boundary-codecs` adds independently versioned canonical record families for
  execution commands, qualified source facts, fills, corrections/busts, cancellation and reconciliation evidence,
  and mechanism-neutral order lineage. Records preserve exact identifiers, quantities, prices, normalized stream
  positions, completeness evidence, and provenance without serializing opaque transport cursors, trusted domain/runtime
  authority, or introducing lower-layer codec dependencies.
- AC-011 [evidence: both]: Checked decoding and replay reconstruct the same lifecycle, completeness, exposure, conflicts,
  and anomalies as direct pure-model application. Golden end-to-end examples cover accepted, rejected, indeterminate,
  duplicate, gapped, partial-fill, cancel/fill-race, corrected, busted, and confirmed cancel-then-submit histories.
- AC-012 [evidence: automated]: Generated local-reference JSON Schemas, canonical golden vectors, semantic round-trip
  properties, duplicate/unknown/null-field tests, malformed and adversarial payloads, configurable decode limits, batch
  index paths, packaged API inspection, and independent canonicalization/schema checks verify every V1 family and
  prevent parser, validation-container, effect, client, persistence, or venue-SDK leakage.
- AC-013 [evidence: both]: Compatibility and operator-facing documentation defines identity authority, uniqueness scope,
  ordering and gap semantics, idempotency/conflict rules, indeterminate recovery, source reconciliation, correction and
  bust handling, cancel/fill races, lineage, and explicit native-amend exclusion. A real reconstruction walkthrough uses
  one captured immutable context per batch and requires no live lookup inside pure replay.


## Risks

- Venue and broker protocols disagree on identifier scope, ordering, acknowledgement, cancellation, corrections, and
  absence guarantees. Mitigation: qualify source authority explicitly, admit ordered and unsequenced evidence as honest
  alternatives, require interpreter contracts, and retain unresolved or anomalous facts instead of normalizing them
  away.
- A single mutable status enum can lose concurrent or late facts. Mitigation: use immutable facts plus a pure checked
  transition whose derived knowledge, quantities, completeness, and anomalies remain independently observable.
- Retrying an indeterminate submission can create duplicate exposure. Mitigation: allocate command/order identities
  before effects, distinguish proven non-dispatch from indeterminate dispatch, require same-identity recovery, and make
  reconciliation a first-class capability outcome.
- Rejecting externally inconsistent fills can hide real economic exposure. Mitigation: retain authoritative fills,
  corrections, and busts and return typed anomaly evidence even when reported behavior violates order invariants.
- Premature native-amend abstraction can encode one venue's cutover and priority semantics as universal. Mitigation:
  ship only immutable order lineage and confirmed cancel-then-submit, enforce native amendment's absence, and require a
  later RFC based on a concrete source contract.
- Freezing execution records before the lifecycle is exercised can create an irreversible but unusable contract.
  Mitigation: block the record Slice on the pure lifecycle, reconcile its proposal against the application workflow when
  relevant, reuse the effective S-05 codec foundation, and require independent schemas, goldens, and semantic replay.
- Event history, reconciliation, and adversarial inputs can become unbounded or hot-path coordinated. Mitigation: use
  explicit decode/batch limits, immutable snapshots/cursors, incremental pure state, deterministic operation-count and
  representative benchmark evidence, and no live lookup per fact.
- The new execution vocabulary may be mistaken for trade campaigns, accounting, or risk. Mitigation: keep those domains
  explicit non-goals, expose one-order/account lifecycle semantics, and require a later RFC before aggregating orders or
  calculating whole-trade outcomes.
