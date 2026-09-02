## Context

See `proposal.md` for motivation. The current repository has exact instrument-bound lots and prices, immutable typed
orders, and hypothetical matched scenarios, but no owner for facts from actual execution. The accepted RFC requires a
pure Slice first; application capabilities, runtime interpreters, and durable records are later blocked Slices.

The design must therefore establish a useful physical boundary without introducing effects or speculative downstream
APIs. It must also preserve the repository's JVM construction authority: Scala source privacy alone is insufficient for
closed public models consumed from completed JARs. The minimum build/runtime JDK remains 25.

## Goals / Non-Goals

**Goals:**

- Make source and application authority explicit in nominal identities and qualified references.
- Represent commands, transport knowledge, source facts, and derived knowledge as separate honest sums/products.
- Rebuild an equivalent lifecycle from immutable evidence without using delivery time as business ordering.
- Preserve exact typed fill economics and known exposure even when the source reports gaps, conflicts, or anomalies.
- Expose a domain-readable, total checked API with strong completed-JAR and JVM authority boundaries.
- Keep transition cost incremental and make replay complexity measurable.

**Non-Goals:**

- Choose an effect type, client protocol, queue, transaction model, persistence schema, or interpreter contract.
- Model venue matching, routing, market data, account balances, margin, portfolio state, campaigns, PnL, fees, or risk.
- Define native amendment semantics or reuse hypothetical `OrderScenario` values as actual facts.
- Claim that unsequenced facts, timestamps, or network delivery provide authoritative order.

## Decisions

### 1. Create a pure execution-lifecycle module with a narrow dependency cone

Add SBT project `executionLifecycle` in `execution-lifecycle/`, publishing `trading-execution-lifecycle` and owning
`trading.execution`. It depends on `instrumentEconomics` and `orderModel` (plus Cats Core only where its pure validated
or non-empty structures are used). The root aggregates it; `adversarialBoundary` consumes it only for verification.
No existing production module gains an execution-lifecycle dependency in this Slice.

The completed execution-lifecycle classpath will contain quantities/reference-data transitively, instrument economics,
order model, Scala, and admitted pure support. Separate source/package/classpath guards will prove that established
upstream and sibling artifacts cannot import execution lifecycle and that execution lifecycle cannot import scenario,
fee, risk, application, runtime, codec, effect, stream, client, persistence, or telemetry mechanisms.

Alternative considered: put actual execution into `order-model` or `execution-scenario`. Rejected because an immutable
instruction is not a source fact and a hypothetical matched slice has no external authority. Alternative considered:
wait for the application Slice before creating a module. Rejected because S-01 has a coherent non-empty pure body and
an independently enforceable dependency boundary.

### 2. Separate application identities from source-qualified identities

Use small nominal checked values for application command, logical execution-order, lineage, execution source,
execution account, native event, native source-order, native fill, stream, and sequence identities. Application-owned
identities are not interchangeable with source-owned identities. Public qualified source references are products of
source, account, and the relevant native identifier; sequence positions additionally carry stream scope.

Identifier constructors validate only representation invariants needed for safe domain use. They do not generate IDs,
hash economic content, infer uniqueness, or claim that a timestamp or local receipt is authoritative. Equality and
hashing cover the documented nominal value and qualifier fields. Normalized facts retain their qualified source
reference and typed economics, so deduplication compares identity first and normalized content only to classify same-ID
replay versus conflict.

Alternative considered: use strings or UUIDs everywhere. Rejected because representation does not establish ownership
or scope and would allow accidental cross-source deduplication. Alternative considered: derive fill identity from
payload hashes. Rejected because economically equal fills can be distinct and corrected source facts can legitimately
change content under explicit authority.

### 3. Bind one immutable typed order, instrument, source, and account per lifecycle

The lifecycle aggregate is parameterized by the order's position/base/quote dimensions and is created from one trusted
`Instrument`, one compatible immutable `Order`, one logical execution-order identity, one lineage identity, and one
execution target (source plus account). Construction accumulates independent identity/instrument mismatches before
issuing the trusted aggregate.

Fill values use the aggregate's dependent instrument types for positive `Lots` and exact grid `Price`; no untyped
quantity map or decimal conversion is admitted. The retained instrument supplies the trusted position grid needed to
derive exact signed `PositionLots` from the immutable order side. Foreign instrument, logical-order, source, or account
inputs return typed scope violations and contribute no exposure.

Alternative considered: erase all orders and fill economics behind existential wrappers. Rejected for the aggregate's
primary API because it would lose compile-time dimension association and force casts in the hot transition. A small
existential observation may be provided only if downstream heterogeneous collections need it, behind lifecycle-owned
checked construction.

### 4. Model evidence as closed alternatives and derive knowledge as a product

Use separate closed families for:

- submit and cancel commands with stable normalized bodies;
- dispatch evidence proving non-dispatch or recording indeterminate dispatch;
- source facts for acceptance, rejection, fill, correction, bust, effective cancellation, and explicit reconciliation
  or completeness evidence;
- source ordering as either explicitly unsequenced or authoritatively sequenced in a qualified stream scope.

The immutable lifecycle state retains canonical maps/indexes for command identity, source-event identity, qualified
fill identity, source stream position, unresolved reference, and effective fill. Its derived public view is a product:
submission knowledge, cancellation knowledge, known exact exposure, completeness diagnostics, conflicts, unresolved
references, and anomalies remain independently inspectable. It is not a single mutable status enum.

The public transition returns a typed classification with the resulting immutable state: applied, idempotent duplicate,
conflicting evidence, or rejected foreign/invalid input. Well-scoped source conflicts and authoritative invariant
breaches remain recorded evidence; structurally invalid or foreign values do not mutate trusted state. Expected cases
never throw.

Alternative considered: event-sourcing as an append-only delivery-order vector followed by a fold. Rejected because
delivery order is not authority and would make equivalent fact sets reconstruct differently. Alternative considered:
fail fast and discard conflicting facts. Rejected because source conflicts and exposure anomalies are themselves
important reconciliation evidence.

### 5. Represent authoritative order and completeness explicitly

A sequenced source fact carries a qualified stream position and explicit source continuation/checkpoint evidence. The
state indexes facts by stream and position, retaining all claimants at a conflicting position. Gaps are exact missing
ranges. A later fact may update known exposure when identity/reference requirements are satisfied, but the affected
stream remains incomplete until gaps, conflicts, rewinds, and unresolved references are cleared.

Unsequenced facts are canonicalized by qualified event identity and reference graph, never by arrival order. Explicit
source checkpoints/completeness boundaries—not a decreasing arrival position—are what can prove a rewind or authoritative
absence. Reconciliation evidence states the precise source/account/order or stream scope it covers.

Derived state is recomputed or incrementally updated from canonical identity/position indexes so permutations that add
the same authority-equivalent evidence set converge. Stable diagnostic ordering uses closed location keys (command,
stream/position, event, fill/reference) rather than map iteration or arrival time.

Alternative considered: treat every source as globally sequential. Rejected because source contracts qualify ordering
at different account/stream scopes and some provide no sequence. Alternative considered: use timestamps to break ties.
Rejected because timestamps are reported data, not ordering authority.

### 6. Preserve submission uncertainty without fabricating facts

The submit command records intent to dispatch; dispatch evidence separately records `ProvenNotDispatched` or
`Indeterminate`. Acceptance and rejection are source facts. The derived submission view selects the strongest supported
knowledge while retaining all underlying evidence and conflicts.

An indeterminate submit blocks any fresh submit for the same logical order because it could duplicate exposure. Exact
same-command replay is admitted as idempotent recovery, and cancel/reconciliation evidence can still be recorded. A
later fill establishes execution but not acceptance. A source absence result establishes non-acceptance only when its
explicit completeness boundary covers the target.

Alternative considered: map timeout/cancellation to rejection. Rejected because the external effect may have crossed
the boundary. Alternative considered: automatically allocate a new order identity on retry. Rejected because it can
create duplicate economic exposure.

### 7. Maintain an effective-fill ledger while retaining source history

Each fill has a stable qualified fill reference and exact lots/price. Distinct fill identities always remain distinct;
same-identity/same-content replay is a duplicate; same-identity/different-content is a conflict. A correction or bust is
another source fact that explicitly references a fill. Unknown targets remain unresolved. Once ordered authority is
sufficient, a correction replaces the target's effective lots/price and a bust makes its effective contribution zero,
while original and modifying facts remain observable.

Known exposure is the exact sum of signed active fill lots using the immutable order side and retained instrument grid.
Duplicate reports do not add exposure. Effective lots beyond the order quantity, and fills authoritatively proven after
effective cancellation, remain in the ledger and contribute to exposure while producing typed anomalies. Facts that
cannot be typed to the lifecycle instrument or target scope are rejected rather than coerced.

Alternative considered: reject overfills or post-cancel fills. Rejected because authoritative external execution has
already created exposure. Alternative considered: mutate or delete a fill on correction/bust. Rejected because replay,
audit, and conflict explanation require the original source evidence.

### 8. Treat cancellation and cancel-then-submit lineage as independent evidence

A cancel command records a request only. Effective cancellation is a qualified source fact. Source ordering determines
whether a fill is provably post-cancellation; network delivery after a cancel fact does not. Cancellation knowledge,
fill exposure, and anomalies remain separate derived observations.

Lineage is a nominal application-owned identity plus an explicit predecessor link between distinct logical execution
orders. This Slice admits the link only after effective predecessor cancellation is evidenced. It offers no operation
that changes the predecessor order and no atomicity, priority, or cutover promise.

Alternative considered: model cancel-replace as amendment of one order. Rejected because immutable order identities and
venue-specific race/priority semantics would be lost.

### 9. Close construction authority at both Scala and JVM boundaries

Public facts, lifecycle state, transition results, reconciliation evidence, and non-empty diagnostic aggregates use
checked factories. Sealed alternatives and private constructors prevent Scala callers from inventing variants. For
load-bearing authority, completed-JAR Scala and Java fixtures plus `javap -p` verify that constructors, implementation
classes, copy methods, erased generic inputs, and same-package/subclass bypasses cannot forge trusted values. If Scala's
generated bytecode cannot provide JVM privacy, use the repository's established JVM-private constructor bridge pattern
behind cached private method handles.

All public execution values reject Java object serialization. Structural equality/hashing is explicit for private
non-case-class values and covered by laws. Checked constructors accumulate independent field/scope errors in stable
non-empty order and sequence reference-dependent validation from prior evidence.

Alternative considered: rely on `private[execution]` or case-class generated constructors. Rejected because ordinary
external JVM callers can spoof package names or invoke public generated artifacts.

### 10. Verify laws, replay semantics, boundaries, and cost in dependency order

Unit/example tests cover every closed alternative and RFC scenario. ScalaCheck properties cover same-evidence replay,
idempotence, authority-respecting delivery permutations, exact exposure, correction/bust replacement, and stable
diagnostic ordering. Completed-JAR compiler fixtures cover positive use and forbidden construction/import/native-amend
surfaces from Scala and Java.

The transition uses persistent identity and ordered-position indexes, with expected lookup/update cost logarithmic in
retained evidence and no full-history scan for ordinary duplicate detection or one-fill updates. Replay is measured in
history size and uses explicit operation instrumentation; representative JMH evidence is added only if measurements are
needed to support a hot-path claim. The final gate compiles modules in dependency order, runs the full aggregate and
adversarial suites, inspects packaged APIs/imports, and validates planning/source/traceability strictly.

Alternative considered: optimize around mutable shared state in the pure module. Rejected because it weakens replay
equivalence and belongs, if needed, in later runtime-private coordination around the pure transition.

## Risks / Trade-offs

- [Source protocols provide weaker or differently scoped authority] → Preserve qualified ordered/unsequenced and
  complete/incomplete alternatives; normalize only evidence a concrete contract can honestly supply.
- [A highly typed aggregate becomes difficult to call] → Keep dependent types inside checked factories and expose small
  domain-named operations and observations for common submit/fact/replay paths.
- [Conflicting or unresolved evidence grows retained state] → Keep immutable indexed evidence, expose explicit counts
  and locations, and measure transition/replay complexity without adding eviction semantics to this Slice.
- [Corrections and busts form ambiguous chains] → Require explicit target identity and authoritative ordering; retain
  unresolved/conflicting chains rather than choosing by arrival time.
- [Private Scala models leak JVM construction authority] → Inspect bytecode early and keep adversarial Scala/Java
  completed-JAR tests in the same Task Group as each trusted constructor family.
- [Later application or codec needs tempt scope expansion] → Define only pure command/fact/result data now; defer ports,
  interpreters, envelopes, schemas, and durable compatibility to their accepted blocked Slices.

## Migration Plan

1. Add the module and boundary/compiler scaffolding without changing existing production dependencies.
2. Add identities and checked execution target/order aggregate construction.
3. Add closed commands, dispatch evidence, source facts, ordering/completeness evidence, and JVM authority tests.
4. Add canonical immutable transition/replay indexes and deterministic diagnostics.
5. Add submission, fill/correction/bust, cancellation, lineage, exact exposure, and anomaly derivations.
6. Update architecture documentation and run focused then complete dependency-ordered validation.

Rollback is removal of the new leaf/consumer-free module and its build/test/documentation wiring before later Slices
depend on it; no persisted data or runtime integration exists in S-01.
