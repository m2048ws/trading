# actual-execution-lifecycle Specification

## Purpose
Defines the pure, authoritative lifecycle for one immutable order and execution account, preserving exact execution
facts, source authority, uncertainty, reconciliation, exposure, and anomalies independently of transport and runtime.
## Requirements
### Requirement: Actual execution has one pure domain owner

A non-empty `trading-execution-lifecycle` artifact under `trading.execution` SHALL own the closed command, dispatch
evidence, source-fact, lifecycle-state, reconciliation-result, anomaly, and error vocabularies for actual execution.
It SHALL consume only immutable order-model and instrument-economic values plus pure mathematical support. It MUST NOT
depend on hypothetical execution scenarios, fee policy, risk, application, runtime, codecs, concrete effects, streams,
clients, persistence, telemetry, or venue SDKs, and those lower or sibling pure artifacts MUST NOT depend on it.

#### Scenario: Compile the completed execution artifact

- **WHEN** the execution-lifecycle artifact is built and inspected from its completed JAR
- **THEN** it contains a usable pure production API and only the admitted order, instrument-economic, quantity, and pure-support dependencies

#### Scenario: Reject reverse dependencies from established pure owners

- **WHEN** quantities, instrument economics, order model, execution scenario, fee policy, or risk is compiled against its intentional completed-artifact classpath
- **THEN** actual-execution imports are unavailable and the established dependency direction remains unchanged

#### Scenario: Reject downstream mechanisms from actual execution

- **WHEN** code compiled against only the execution-lifecycle artifact attempts to use application, runtime, codec, effect-runtime, stream, client, persistence, telemetry, or venue-SDK types
- **THEN** compilation fails because those mechanisms are outside the pure lifecycle boundary

#### Scenario: Keep hypothetical evidence distinct

- **WHEN** a caller has a hypothetical matched execution scenario
- **THEN** it cannot submit that scenario or its slices as an authoritative actual-execution fact

### Requirement: Execution identities retain explicit authority and scope

Application-issued command, logical execution-order, and lineage identities SHALL be distinct nominal values with
documented application authority. Execution-source and execution-account identities SHALL qualify native source-event,
source-order, and fill identities at their documented uniqueness scope. A normalized fact SHALL retain its qualified
native reference and exact economic provenance; receipt order, timestamps, random local receipt identifiers, hashes of
economic fields, and payload equality MUST NOT become identity or ordering authority.

#### Scenario: Qualify the same native identifier in different scopes

- **WHEN** two reports carry the same native fill or event identifier under different execution sources or accounts
- **THEN** their qualified identities are distinct and neither report deduplicates the other

#### Scenario: Retain economically identical distinct fills

- **WHEN** two fills have equal lots, price, and reported time but different qualified fill identities
- **THEN** both fills remain distinct contributions to exact exposure

#### Scenario: Replay the same qualified fact

- **WHEN** the same qualified source identity and normalized content is presented again
- **THEN** the lifecycle classifies it as an idempotent duplicate without adding exposure

#### Scenario: Reuse a qualified identity with conflicting content

- **WHEN** a source reuses a qualified event or fill identity with different normalized content
- **THEN** the lifecycle returns and retains a typed source conflict without silently replacing the original fact

#### Scenario: Retain native provenance

- **WHEN** a fill, correction, bust, acceptance, rejection, or effective cancellation is normalized
- **THEN** its public observation retains the source, account, native reference, and any authoritative ordering evidence used to interpret it

### Requirement: Commands are stable business data with idempotent reuse

Every submit or cancel command SHALL have a stable application-issued command identity and normalized immutable body.
Every submitted immutable order SHALL have a distinct logical execution-order identity. Re-delivering the same command
identity and body SHALL be idempotent; reusing that identity with different content SHALL produce a typed command
conflict. Transport attempts, calls, reconnects, fibers, queue deliveries, and receipt identifiers MUST NOT allocate or
substitute business command identities.

#### Scenario: Retry the same submit command

- **WHEN** a submit command is redelivered with the same command identity, logical order identity, target, lineage, and immutable order
- **THEN** the lifecycle returns the existing command evidence without creating another submission or exposure

#### Scenario: Conflict on command identity reuse

- **WHEN** a command identity already observed for one normalized body is reused for a different submit or cancel body
- **THEN** the lifecycle returns a typed conflict and preserves the original command

#### Scenario: Distinguish transport attempts from commands

- **WHEN** the same command crosses multiple transport attempts or is received through multiple deliveries
- **THEN** those attempts do not become new commands or new logical execution-order identities

### Requirement: Ordering, gaps, and completeness are evidence

A source fact SHALL be either explicitly unsequenced or carry authoritative sequence evidence scoped to one execution
source, account, and stream. The lifecycle MUST NOT infer business order from network delivery order or timestamps. It
SHALL detect duplicate and conflicting stream positions, explicit source rewinds, missing positions, and unresolved
references. Later evidence SHALL remain available across a gap, but the affected scope MUST remain incomplete until
the missing authority or reference is reconciled. Absence SHALL prove non-acceptance or completion only when accompanied
by a source-authoritative completeness boundary.

#### Scenario: Receive a later sequenced fact across a gap

- **WHEN** authoritative positions before a received fact are missing
- **THEN** the fact is retained, the exact missing range is reported, and the lifecycle does not claim complete authoritative state

#### Scenario: Fill a previously reported gap

- **WHEN** all missing authoritative positions later arrive without conflict
- **THEN** replay incorporates them in source order and clears only the completeness defect they resolve

#### Scenario: Conflict at one stream position

- **WHEN** different source facts claim the same qualified authoritative stream position
- **THEN** both evidence and a typed position conflict remain observable and the stream is not reported complete

#### Scenario: Observe an authoritative rewind

- **WHEN** source-supplied checkpoint or continuation evidence moves behind an already established authoritative boundary
- **THEN** the lifecycle reports a typed rewind rather than treating arrival order or timestamp order as proof

#### Scenario: Receive explicitly unsequenced facts

- **WHEN** facts have no authoritative source sequence
- **THEN** they remain usable by identity and reference while the lifecycle makes no before-or-after claim from their delivery order

#### Scenario: Reconcile an unresolved reference

- **WHEN** a correction, bust, cancellation, or acknowledgement arrives before the source order or fill it references
- **THEN** the later fact is retained as unresolved and becomes resolvable when its referenced authority arrives

#### Scenario: Report non-authoritative absence

- **WHEN** a reconciliation lookup finds no matching order but supplies no authoritative completeness boundary
- **THEN** the result remains unknown and does not prove rejection or non-dispatch

### Requirement: Pure transition and replay are deterministic

Lifecycle evolution SHALL be a total checked pure transition over immutable state and normalized evidence. Equivalent
authoritative evidence sets SHALL derive the same commands, facts, effective fills, exact exposure, submission and
cancellation knowledge, completeness, conflicts, unresolved references, and anomalies regardless of delivery order
where that order has no authority. Expected invalidity, scope mismatch, duplicate, conflict, and uncertainty SHALL be
represented in domain result types rather than exceptions, flags, nulls, or mutable status.

Command recording and dispatch observation SHALL return a closed total transition sum. Its rejected alternative SHALL
carry non-empty command violations, while applied, idempotent, command-conflict, and dispatch-conflict alternatives
SHALL carry no optional violation payload; production transition handling MUST NOT use unchecked violation extraction.
Replay, effective-fill, anomaly, continuation, and diagnostic ordering SHALL compare typed components
lexicographically under owner-local orderings rather than encode composite keys as delimiter-concatenated strings.
One lifecycle observation SHALL derive one effective-fill ledger and supply that same derivation to anomaly
construction.

Submission knowledge, cancellation knowledge, effective-fill classification, and source and command transitions SHALL
be closed exhaustive Scala sums without parallel kind flags, optional case payloads, or hand-written per-alternative
equality. Derived evidence, anomalies, ledgers, replay results, and observations SHALL use direct structural products
where every field combination is valid and SHALL retain their public field information and structural equality.
Commands, source facts, lifecycle identity, refinements, non-empty wrappers, and external reconstruction SHALL retain
checked construction and typed failures wherever construction establishes a semantic predicate. Converted derived
results MUST continue to reject Java object serialization.

#### Scenario: Replay the same evidence repeatedly

- **WHEN** the same normalized command and fact history is replayed from an empty lifecycle more than once
- **THEN** every replay produces structurally equal state, derived observations, and deterministic diagnostics

#### Scenario: Permute delivery without changing authority

- **WHEN** the same source-qualified evidence is delivered in different orders while carrying identical authoritative sequence and reference relationships
- **THEN** the reconstructed authoritative lifecycle is equivalent and network order contributes no semantics

#### Scenario: Reject a foreign lifecycle input

- **WHEN** a command or fact targets another logical order, execution account, source scope, or instrument
- **THEN** a typed scope error is returned and no foreign economic contribution is applied

#### Scenario: Inspect an incomplete lifecycle

- **WHEN** gaps, source conflicts, rewinds, or unresolved references remain
- **THEN** callers can inspect retained evidence and exact known exposure while completeness remains an explicit non-empty diagnostic

#### Scenario: Classify every command transition

- **WHEN** command recording or dispatch observation applies evidence, receives an idempotent duplicate, retains a
  command or dispatch conflict, or rejects invalid input
- **THEN** exactly one exhaustive transition alternative is returned, only rejection carries non-empty violations,
  and the prior state and classification semantics are preserved

#### Scenario: Compare identifiers containing former delimiters

- **WHEN** distinct command, source, account, stream, event, order, fill, or lineage identifiers contain characters
  formerly used to concatenate sort keys
- **THEN** replay and observation compare their typed components without collisions and remain deterministic across
  delivery permutations

#### Scenario: Reuse the observation ledger for anomalies

- **WHEN** one lifecycle observation derives effective fills, exact exposure, corrections, busts, conflicts,
  unresolved references, cancellation races, and overfill anomalies
- **THEN** anomaly construction consumes that observation's single effective-fill ledger and preserves the existing
  structural results

#### Scenario: Exhaustively consume derived execution results

- **WHEN** a completed-artifact Scala client matches submission, cancellation, effective-fill, source-transition, or
  command-transition results
- **THEN** every domain alternative is available as an exhaustive sum case with the same public field information and
  structural equality as the derived result it replaces

#### Scenario: Retain guarded inputs and fail-closed derived values

- **WHEN** focused positive and negative tests construct commands, source facts, lifecycle identities, refinements,
  non-empty wrappers, or external reconstructions and attempt Java object serialization of converted derived results
- **THEN** semantic predicates still require their checked factories and typed failures, while every converted derived
  result rejects serialization

### Requirement: Submission knowledge preserves uncertainty

Submission SHALL distinguish confirmed acceptance, confirmed rejection, proof that dispatch did not cross the external
boundary, and indeterminate dispatch where an external effect may have occurred. Indeterminate submission MUST NOT be
collapsed into rejection or a venue status. It SHALL prevent a fresh duplicate submission while permitting same-identity
retry, reconciliation, source-supported lookup, and safe defensive cancellation. Later authoritative facts SHALL refine
knowledge without inventing missing facts, and absence SHALL be conclusive only under an authoritative completeness
boundary.

#### Scenario: Record proven non-dispatch

- **WHEN** dispatch evidence proves that a submit command did not cross the external boundary
- **THEN** the lifecycle records proven non-dispatch distinctly from source rejection and indeterminate dispatch

#### Scenario: Record indeterminate dispatch

- **WHEN** cancellation, timeout, disconnect, or another boundary outcome cannot prove whether submission crossed externally
- **THEN** the lifecycle records indeterminate submission and rejects a fresh logical submission that could duplicate exposure

#### Scenario: Recover with the original identities

- **WHEN** an indeterminate submission is retried with the original command and logical order identities
- **THEN** the lifecycle accepts it as idempotent recovery rather than a new submission

#### Scenario: Reconcile a later acceptance or rejection

- **WHEN** an authoritative acceptance or rejection later identifies the submitted logical order
- **THEN** the lifecycle retains both the earlier uncertainty and the later source fact while deriving the strongest supported submission knowledge

#### Scenario: Observe a fill without an acceptance

- **WHEN** an authoritative fill arrives for an indeterminate submission without any acceptance fact
- **THEN** execution is proven and exact exposure is retained, but the lifecycle does not fabricate an acceptance event

#### Scenario: Prove absence at a completeness boundary

- **WHEN** a source-supported reconciliation result authoritatively covers the relevant order scope and reports absence
- **THEN** the lifecycle may derive non-acceptance according to that declared boundary while retaining the evidence that justifies it

### Requirement: Effective fills preserve exact exposure and corrections

Every fill SHALL carry positive instrument-bound lots, an instrument-bound exact grid price, a qualified fill identity,
the referenced source order or explicitly unresolved reference, and source provenance. Effective exposure SHALL be the
exact signed position contribution of distinct active fills under the immutable order side. Duplicate fills MUST NOT
double count. Corrections and busts SHALL explicitly reference an existing fill identity or remain unresolved; an
authoritatively ordered correction SHALL replace that fill's effective economics and a bust SHALL remove its effective
contribution without deleting historical evidence. Externally reported overfills and other invariant breaches SHALL be
retained as exposure with typed anomalies when their source scope and identity are authoritative.

#### Scenario: Accumulate partial fills exactly

- **WHEN** one logical order receives several distinct partial fills on its retained lot and price grids
- **THEN** the lifecycle retains every fill and derives the exact signed cumulative position without decimal or floating-point conversion

#### Scenario: Correct an existing fill

- **WHEN** an authoritative correction references a known fill and supplies replacement exact lots or price
- **THEN** the original and correction remain observable while effective exposure and economics use only the authoritative replacement

#### Scenario: Bust an existing fill

- **WHEN** an authoritative bust references a known active fill
- **THEN** the fill remains in history and its effective exposure contribution becomes zero

#### Scenario: Receive a correction before its fill

- **WHEN** a correction or bust references a fill that has not yet arrived
- **THEN** it remains unresolved, contributes no invented exposure, and can resolve deterministically when the fill arrives

#### Scenario: Retain an overfill

- **WHEN** authoritative effective fill lots exceed the immutable order quantity
- **THEN** all reported exposure is retained and an exact typed overfill anomaly reports the excess

### Requirement: Cancellation facts and lineage remain honest

A cancel command or request SHALL remain distinct from an effective cancellation fact. Fills racing cancellation SHALL
be interpreted only through source identity and authoritative ordering. Delivery after a cancellation message MUST NOT
alone be treated as contradictory; a fill provably effective after cancellation or another source invariant breach
SHALL remain in exact exposure with a typed anomaly. Confirmed cancel-then-submit MAY link distinct immutable logical
execution orders under one mechanism-neutral lineage. No public native atomic amend or cancel-replace operation,
placeholder, or throwing implementation SHALL exist.

#### Scenario: Request cancellation without confirmation

- **WHEN** a cancel command is recorded but no authoritative effective-cancellation fact exists
- **THEN** the lifecycle reports cancellation requested without claiming that execution has stopped

#### Scenario: Reconcile a cancel and fill race

- **WHEN** a fill and effective cancellation are delivered in either network order with authoritative source ordering
- **THEN** the lifecycle derives their source order, retains the fill exposure, and reports an anomaly only when the fill is provably post-cancellation

#### Scenario: Receive an unsequenced fill after a cancel message

- **WHEN** an unsequenced fill is delivered after an effective-cancellation message
- **THEN** delivery order alone does not prove a post-cancellation violation and both facts remain observable

#### Scenario: Link confirmed cancel then submit

- **WHEN** a predecessor order is authoritatively cancelled and a successor immutable order is submitted under the same lineage
- **THEN** the lifecycle preserves distinct logical order identities and exposes their predecessor-successor relationship without claiming atomic replacement

#### Scenario: Attempt native amendment

- **WHEN** a caller inspects or compiles against the completed execution-lifecycle API
- **THEN** no native amend or atomic cancel-replace capability, unsupported branch, or throwing placeholder is available

### Requirement: Execution authority is semantic rather than constructor-secret

Execution identities, commands, source facts, ordering evidence, lifecycle states, observations, and replay results
SHALL be created and transformed through statically callable owner-defined operations. Hidden constructors or dynamic
private access SHALL NOT be treated as their authority. Each strengthening transition SHALL establish the required
identity scope, command/fact shape, source ordering, duplicate/conflict classification, reference resolution,
completeness, cancellation, lineage, and exact-exposure predicates before returning authoritative state.

#### Scenario: Record a source fact

- **WHEN** a source fact is supplied to a lifecycle transition
- **THEN** its qualified identity, target scope, payload, ordering evidence, duplicates, conflicts, and references are
  classified before state is updated

#### Scenario: Replay equivalent evidence

- **WHEN** the same authoritative sequenced facts are delivered in different network orders
- **THEN** replay produces equivalent lifecycle authority and exposure without constructor provenance contributing
  semantics

#### Scenario: Preserve incomplete knowledge

- **WHEN** ordering, acknowledgement, referenced facts, or completeness authority is absent or conflicting
- **THEN** the lifecycle returns the corresponding typed uncertainty or diagnostic rather than manufacturing stronger
  evidence

