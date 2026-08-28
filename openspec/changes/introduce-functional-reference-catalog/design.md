## Context

See `proposal.md` for motivation and the two delta specifications for normative behavior. This proposal depends on
`separate-quantity-and-reference-data-boundaries`, which introduces `trading-reference-data`, stable identities,
`Asset`, `DimensionHandle`, `GridHandle`, and a temporary relocated synchronized registry. The proposal portfolio must
be reconciled before either change is applied.

The current registry protects mutable maps with one object monitor. Every read and write shares that monitor; packed
decode performs multiple lookups; an unknown dimension-local grid may scan other dimension maps; and count metrics also
hold the same lock. Existing instrument calculations retain witnesses and normally avoid registry lookup, but future
ingress, replay, and instrument assembly could turn snapshot resolution into a data-plane path.

The accepted catalog policy is stronger than the current implementation makes explicit:

- identity definitions are append-only and immutable by key;
- corrections use new stable versions or identities;
- activation and delisting are separate from identity;
- a batch publishes atomically as one revision;
- idempotent or failed registration does not advance revision;
- historical snapshots and issued handles remain valid.

The design must preserve Proposal 1's private lineage and handle authority, the charter's functional-core/effect-shell
boundary, deterministic evidence-producing validation, and the rule that economics never receives a live catalog.

## Goals / Non-Goals

**Goals:**

- Give catalog definitions one deterministic, pure, reusable semantic model.
- Represent registration intent and catalog outcomes with closed algebraic data types.
- Make batch validation atomic, deterministic, and evidence-producing.
- Preserve canonical path-dependent handles across immutable state versions.
- Make snapshot lookup coherent and lock-free on every hot read.
- Specify enough concurrency semantics that all live interpreters agree.
- Introduce tagless-final only for the genuinely effectful live publication boundary.
- Remove the temporary synchronized registry without choosing a concrete runtime mechanism.
- Establish one reusable JVM benchmark boundary for the portfolio's concrete performance questions.

**Non-Goals:**

- Do not define market data, instrument definitions, venue listings, account eligibility, availability intervals, or
  catalog distribution protocols.
- Do not define wire/database schemas, command persistence, replay checkpoints, or migrations.
- Do not add subscriptions, event streams, retries, clocks, telemetry, transactions spanning other services, or runtime
  health management.
- Do not select Cats Effect `Ref`, STM, an actor/fiber, a database transaction, or remote consensus.
- Do not expose `F[_]` in reference-data state, snapshots, handles, or lookup.
- Do not resolve instrument-specific role, payoff, listing, or economic coherence.
- Do not promise that equal stable definitions rebuilt after a process restart retain in-memory lineage.
- Do not make a machine-specific throughput value a normative correctness or release threshold.

## Decisions

### 1. Use initial commands, a pure transition model, and a final live port

The three representations serve different purposes:

```text
CatalogCommand / CatalogBatch
    inspectable registration intent (initial data)
                 |
                 v
CatalogModel.commit(state, batch)
    pure immutable transition semantics
                 |
                 v
LiveCatalog[F]
    effect-polymorphic publication capability (final encoding)
```

Conceptually:

```scala
enum CatalogCommand:
  case RegisterAsset(definition: AssetDefinition)
  case RegisterDimension(key: DimKey)
  case RegisterGrid(definition: GridDefinition)

final case class CatalogBatch private (
  head: CatalogCommand,
  tail: Vector[CatalogCommand]
)

object CatalogModel:
  def commit(
    state: CatalogState,
    batch: CatalogBatch
  ): Either[CatalogViolations, CatalogTransition]
```

Commands are data because applications may inspect, audit, test, and eventually encode them. The pure model is an
ordinary function because its meaning does not vary by interpreter. The live port is tagless-final because publication
really may use different effects and coordination mechanisms.

Alternative considered: make registration methods directly tagless-final and place all semantics in each interpreter.
Rejected because conflict, idempotence, revision, and canonicalization rules would be duplicated and could drift.

Alternative considered: build a free-monad AST for arbitrary catalog programs. Rejected because one closed batch ADT
already supplies inspection and atomicity; arbitrary higher-order programs would complicate serialization and
validation without another required interpreter behavior.

Alternative considered: expose only pure methods on mutable `QuantityRegistry`. Rejected because method purity does not
make shared mutable state or coarse synchronization disappear.

### 2. Model one immutable state with explicit indexes

The internal state shape is conceptually:

```scala
final case class CatalogState private (
  lineage: CatalogLineage,
  revision: CatalogRevision,
  dimensions: Map[DimKey, DimensionHandleBox],
  assets: Map[AssetId, Asset],
  assetByDimension: Map[DimKey, AssetId],
  grids: Map[GridIdentity, GridHandleBox]
)
```

Existential boxes and any casts required to recover path-dependent members remain private. Every cast is preceded by
the state-owned key/lineage/definition checks that justify it. The public surface returns dependent handles, not maps or
retagging functions.

Full grid identity is a direct map key, so lookup cannot degrade into scanning every dimension. Counts are state fields
or ordinary immutable-map sizes. Scala immutable maps structurally share old entries when a successor adds new keys.

`CatalogRevision` is a nonnegative opaque `BigInt`, not `Long`, so a long-lived process has no overflow branch. It is a
publication sequence, not a timestamp or external consensus version.

Alternative considered: keep separate mutable maps and publish them independently. Rejected because a snapshot could
observe cross-index disagreement and batch rollback would be difficult.

Alternative considered: use one untyped `Map[Any, Any]`. Rejected because it erases distinct key spaces and makes
conflict classification and internal narrowing less reviewable.

### 3. Treat catalog-root creation as controlled generativity

One fresh catalog root owns an implementation-private immutable lineage token. Creating a root is intentionally
generative, like creating an anonymous mathematical grid: two roots are distinct even with equal visible definitions.
After the root exists, every catalog transition is immutable and referentially stable in its public catalog semantics.

The root/token constructor is lexically controlled and never derived from `AssetId`, `DimKey`, `GridIdentity`, a UUID
string supplied by a caller, or a serialized field. Fresh-root creation is an explicit generative authority boundary,
not part of `CatalogModel.commit`'s pure transition. Tests or explicitly state-threaded programs create one root at
their outer boundary and then pass the resulting state explicitly. A live interpreter delays root allocation in its
effect at interpreter construction and retains it for its lifetime.

The implementation must not use the mutable registry instance itself as provenance. States, snapshots, and handles all
carry the dedicated token, so structural state replacement does not change lineage.

Alternative considered: define lineage as visible stable IDs plus definitions. Rejected because two independently
bootstrapped catalogs would then authorize cross-catalog narrowing merely because their external data looked equal.

Alternative considered: make lineage a public phantom type parameter on every handle and state. Rejected in Proposal 1
because it would dominate downstream instrument signatures while existential/runtime reconciliation would still be
needed.

### 4. Make asset/dimension binding one-to-one and order-independent

Within a lineage:

- `AssetId -> atomic DimKey` is immutable;
- an atomic asset dimension may be bound to only one `AssetId`;
- standalone dimensions may exist without assets;
- an asset may later reuse an already canonical standalone dimension when it is unbound;
- registering dimension before asset or asset before dimension has the same final meaning.

This preserves the current safety intention that distinct asset identities are not accidentally made fungible by
sharing one quantity dimension, while removing the current order-dependent behavior where pre-registering a dimension
can prevent later asset registration.

If future reference data needs aliases, aliases should point to one canonical `AssetId`; they should not create two
independent `Asset` handles whose quantities share a dimension. If an asset's dimensional meaning was wrong, the
corrected definition uses a new `AssetId`.

Alternative considered: allow any number of assets to share a dimension. Rejected because ordinary quantity addition
would then treat distinct assets as directly fungible and the type-level safety claim would be misleading.

Alternative considered: preserve the registry's registration-order conflict exactly. Rejected because command order is
not a semantic property and atomic batches must validate their complete proposed state.

### 5. Normalize and validate a whole batch before issuing handles

Batch evaluation has explicit stages:

1. Index commands by stable key while retaining original command indices.
2. Detect contradictory proposals for the same key and conflicts with the input state.
3. Form the complete proposed dimension set from existing and valid new dimensions/assets.
4. Validate asset/dimension one-to-one binding.
5. Validate grids only when their proposed dimension prerequisite exists coherently.
6. If any violation exists, return a domain-owned non-empty ordered collection and the unchanged state.
7. If valid, issue handles for genuinely new keys, build one successor state, compute one delta, and choose unchanged or
   published outcome.

Independent violations accumulate applicatively. Dependency stages sequence through `Either`-like transitions and do
not fabricate errors after missing evidence. Ordering is command index, then a stable rule ordinal, making results
independent of map iteration order. Duplicate identical commands collapse idempotently; contradictory duplicates report
one structured conflict retaining the involved indices/definitions.

The public error model is conceptually:

```scala
sealed trait CatalogViolation
final case class InvalidCatalogBatch(
  head: IndexedCatalogViolation,
  tail: Vector[IndexedCatalogViolation]
)

sealed trait CatalogLookupError
```

Cats `ValidatedNec` or similar machinery may implement independent accumulation, but no validation-library container
appears in the public API.

Alternative considered: fold commands fail-fast in order. Rejected because it makes valid dimension/grid batches order
sensitive, hides independent configuration problems, and can expose partial state in a mutable implementation.

Alternative considered: accumulate every downstream error. Rejected because grid-handle construction is meaningless
when its dimension proposal is absent or contradictory.

### 6. Represent publication explicitly

The transition result separates state from outcome:

```scala
final case class CatalogTransition(
  state: CatalogState,
  outcome: CatalogCommit
)

enum CatalogCommit:
  case Unchanged(snapshot: CatalogSnapshot)
  case Published(snapshot: CatalogSnapshot, delta: CatalogDelta)
```

`CatalogDelta` contains non-empty, deterministically ordered identity additions grouped or tagged as dimensions,
assets, and grids. It contains no replacement/removal. A valid batch that adds any number of keys increments revision
once. A fully idempotent valid batch yields `Unchanged`; failure remains the `Left` case and therefore cannot be confused
with a no-op success.

Callers obtain newly issued handles by resolving their stable keys from the outcome snapshot. This avoids a
path-dependent heterogeneous result tuple mirroring every command shape and makes batch results naturally useful for
later assembly.

Alternative considered: increment revision for every command. Rejected because readers need one transaction boundary,
not implementation-step revisions.

Alternative considered: increment revision for idempotent retries. Rejected because it turns delivery retries into
false reference-data changes and noisy downstream events.

### 7. Preserve canonical handles by structural sharing

A successful successor copies the immutable state indexes with only new entries and retains every existing handle
object. Thus the normal linear history has one canonical handle allocation for each key. An idempotent registration
returns the existing entry. New grid versions allocate new anonymous mathematical grids; old versions retain the exact
grid witness and quantum they already owned.

Public semantics remain checked identity, not JVM `eq`. This matters for repeated evaluation of the pure transition,
CAS retries, alternate immutable representations, and future database-backed snapshot materialization. Two trusted
handles with the same lineage, full identity, dimension, and immutable definition reconcile through Proposal 1's
handle evidence even if implementation allocation differs. Candidate immutable handle allocation is therefore not an
observable side effect of transition evaluation; unchecked reference equality is never required downstream.

All path-dependent casts stay in state-owned construction/lookup helpers. No state transition returns a general
`A =:= B`, public coercion, or arbitrary handle constructor.

Alternative considered: rebuild every handle from definitions for each snapshot. Rejected because it creates needless
allocation, risks changing anonymous grid namespaces, and makes retained instruments harder to reason about.

Alternative considered: define reference equality as the public canonicality guarantee. Rejected because it couples
semantics to one in-memory representation and cannot survive future snapshot materialization strategies.

### 8. Make snapshots the only read boundary

`CatalogSnapshot` wraps one immutable state's lookup indexes, lineage, revision, and counts behind a read-only API:

```scala
final class CatalogSnapshot private[reference] (...):
  def revision: CatalogRevision
  def resolveAsset(id: AssetId): Either[CatalogLookupError, Asset]
  def resolveDimension(key: DimKey): Either[CatalogLookupError, DimensionHandle[?]]
  def resolveGrid(identity: GridIdentity): Either[CatalogLookupError, GridHandle[?]]
```

Exact dependent return packaging may use path-dependent result traits rather than wildcards, but lookup always retains
the authoritative handle with its type member. A typed overload may accept a trusted `DimensionHandle[D]` plus local
`GridKey` and return `GridHandle[D]` after membership/lineage checks.

Snapshots do not expose the transition model, maps, lineage token, or a pointer to current live state. No lazy mutable
cache is required. They are safe for concurrent reads precisely because they never change.

The hot-path pattern is:

```scala
for
  snapshot <- liveCatalog.snapshot
  result   <- processBatchPurely(snapshot, records)
yield result
```

This pays one effect/atomic read for a coherence boundary, not one monitor acquisition per resolved value.

Alternative considered: put `resolveAsset` and `resolveGrid` on the live port. Rejected because it encourages per-record
effects, makes batches observe mixed revisions, and hides the catalog view on which a calculation depends.

Alternative considered: update one shared snapshot object in place. Rejected because retained readers would observe
time-dependent meaning and could not reproduce a replay.

### 9. Keep old snapshots and handles valid without automatic retention policy

A successor never mutates a predecessor. An old snapshot resolves its original keys and returns unknown for later keys.
Any caller retaining a snapshot or handle determines its lifetime; the catalog does not revoke it. This is required for
assembled instruments, in-flight batches, historical replay, and deterministic investigation.

Append-only structural sharing makes retaining several nearby snapshots cheap in common cases, but retaining an
unbounded history still consumes memory. This proposal does not add a history store: the live port retains only its
current state unless an interpreter or caller deliberately stores old snapshots/deltas.

Process restart creates a new root and lineage. Stable commands/definitions may be replayed, but trusted handles and
snapshots are never serialized as authority. Proposal 9's codecs resolve stable IDs against the chosen new snapshot.

Alternative considered: invalidate old handles on update. Rejected because adding a grid version must not retroactively
change or break an existing instrument.

### 10. Put the live port in the application artifact

Proposal 2 creates the first real surface in `trading-application`:

```scala
trait LiveCatalog[F[_]]:
  def snapshot: F[CatalogSnapshot]
  def commit(
    batch: CatalogBatch
  ): F[Either[CatalogViolations, CatalogCommit]]
```

The application artifact depends on reference data and contains no concrete effect runtime. The trait itself requires
no Cats type class; workflows using it request only the capabilities/combinators they actually need. Proposal 8
establishes the application/runtime admission rules and supplies the first concrete catalog interpreter without
relocating this port. Market-data, trade-persistence, business-time, execution, transaction, and observability ports are
added only by later proposals after their domain protocols and observable contracts are defined.

This early module is justified by a concrete effectful capability and enforces the charter's ownership now. It is not a
general `Registry[F]` passed to economics and does not expose point lookup or registration methods for every definition
shape. One batch method preserves transactional intent.

Alternative considered: place `LiveCatalog[F]` in reference data. Rejected because reference data owns pure meanings;
running and coordinating updates is an application capability.

Alternative considered: defer the port entirely to Proposal 8. Rejected because Proposal 2 must state the observable
atomicity contract its future interpreters implement and remove the temporary registry without leaving live ownership
ambiguous.

### 11. Specify live concurrency without choosing a mechanism

Every live interpreter must linearize `commit` and `snapshot` around published states:

- a commit evaluates against one latest state;
- publication swaps the whole state atomically;
- conflicting concurrent commits are revalidated in publication order;
- no update is lost;
- a snapshot sees one entire revision;
- idempotent/failed batches do not publish.

Because the transition function is pure, a CAS-based interpreter may safely reevaluate it after contention. An actor or
fiber may serialize commands. STM may transact it. A database interpreter may lock or compare revisions. Those are
runtime decisions with different performance, cancellation, durability, and failure characteristics.

The generic port deliberately promises no exactly-once network delivery, persistent durability, retry policy, or
cancellation rollback. A successful `Published` return identifies a publication; ambiguous interruption semantics are
owned by the concrete interpreter/workflow proposal.

Alternative considered: mandate `Ref[F, CatalogState]` now. Rejected because it selects one single-process interpreter,
places Cats Effect in the planning core, and precludes database or actor ownership without an architectural reason.

### 12. Keep availability and change distribution outside identity

There is no `Update`, `Delete`, `Activate`, or `Delist` catalog command. Grid correction uses a new `GridVersion`; an
asset dimension correction uses a new `AssetId`. Availability policy can later refer to stable identities and time
without modifying historical definitions.

Similarly, `LiveCatalog` has no subscription method. `CatalogDelta` is returned as data and can later feed an event or
FS2 stream capability. Catalog metrics derive from snapshots or interpreter instrumentation; polling metrics must not
introduce synchronized traversals into lookup.

Alternative considered: add active flags to definitions. Rejected because mutable flags make identity snapshots
temporal, conflate historical meaning with current permission, and turn delisting into apparent definition mutation.

Alternative considered: expose a stream directly from the port. Rejected because delivery, buffering, backpressure,
replay, and subscriber lifecycle require a later capability-specific proposal; Proposal 8 establishes only their
application/runtime placement and admission rules.

### 13. Establish one shared non-published JMH benchmark project

This proposal is the first in the portfolio with a concrete performance-evidence obligation, so it adds:

```text
SBT ID:       benchmarks
directory:    benchmarks
publication:  skipped
plugin:       sbt-jmh
initially depends on: trading-reference-data
```

The project is excluded from the root test aggregation so ordinary `clean test` does not execute long measurements.
Its JMH configuration and sources are compiled explicitly by verification, and directional runs record JVM, arguments,
thread count, and result data. The initial benchmark compares immutable snapshot lookup under representative reader
counts and confirms the implementation has no shared registry-monitor path; it does not assert one portable operations-
per-second threshold.

Deterministic semantics and complexity remain in ordinary tests. For example, map lookup behavior, snapshot coherence,
and absence of mutable/live references are proved structurally and behaviorally; JMH supplies only throughput,
contention, latency, or allocation evidence that the JVM must measure. Later risk, runtime, and codec proposals add
benchmarks and their direct benchmark-only project dependencies to this same project.

Alternative considered: retain one-off console loops or wall-clock assertions in unit tests. Rejected because JVM
warmup, dead-code elimination, forking, scheduling, and measurement error make those results difficult to reproduce or
interpret.

Alternative considered: create one benchmark project per production module. Rejected because benchmark code is
non-published verification infrastructure and one project can depend downward on each completed artifact without
creating production dependency cycles.

## Risks / Trade-offs

- [Path-dependent handles inside immutable maps require existential narrowing] → Keep boxes and checked casts lexical to
  the catalog implementation, prove keys/lineage/definitions first, and add adversarial fixtures against public
  retagging authority.
- [Batch validation may accidentally depend on input/map order] → Retain command indices, assign stable rule ordinals,
  sort public violations/deltas deterministically, and test command permutations that should have equal meaning.
- [CAS interpreters may reevaluate handle allocation under contention] → Define public canonicality through immutable
  identity/evidence rather than reference equality; publish only handles in the winning successor and test pure/live
  semantic equivalence.
- [One-to-one asset dimensions may later need aliases] → Model aliases as reference-data names pointing to one canonical
  `AssetId`; changing direct fungibility requires a separate proposal because it alters quantity semantics.
- [Old snapshots retained indefinitely can consume memory] → Do not keep automatic history in the minimal live
  interpreter; ownership and retention are explicit at callers or persistence layers.
- [Creating `trading-application` with one port could be seen as premature] → The port is already required to replace the
  live registry and has a genuine interpreter-varying contract; Proposal 8 establishes the surrounding admission and
  runtime-interpreter architecture without inventing unrelated ports.
- [A minimal generic port can be overinterpreted as durable or cancellation-safe] → State only atomic publication and
  linearization guarantees; make interpreter-specific lifecycle/failure contracts explicit later.
- [Removing the registry before a concrete interpreter leaves no production live implementation] → Pure state threading
  remains complete for build/startup/tests; Proposal 8 supplies the first runtime interpreter before release.
- [A benchmark result varies across machines and JDKs] → Record environment and JMH parameters, compare shapes rather
  than promise one throughput number, and keep correctness/complexity assertions in deterministic tests.

## Migration Plan

1. Satisfy the Proposal 0 portfolio gate and apply the quantity/reference-data boundary so its handles and temporary
   registry bridge exist.
2. Add catalog command, non-empty batch, revision, delta, commit, violation, lookup-error, state, and snapshot values to
   `trading-reference-data`.
3. Implement staged pure batch validation and immutable transition construction, including asset/dimension one-to-one
   indexes, full grid keys, idempotence, conflicts, canonical handle reuse, and deterministic outcomes.
4. Add unit/property tests for transition purity, atomic rollback, validation accumulation/dependency suppression,
   command-order independence, revision laws, delta conservation, append-only state, historical snapshots, and restart
   lineage.
5. Replace registry-backed setup in reference-data/economics fixtures with explicit pure root/state/batch construction
   and snapshot resolution.
6. Add the `application` SBT project and packaged-artifact wiring, define only `LiveCatalog[F]`, and add a deterministic
   conformance test harness interpreters can reuse without adding a concrete production interpreter.
7. Remove the temporary synchronized `QuantityRegistry`, its monitor/count/scan paths, obsolete errors, and remaining
   imports; verify no domain module receives the live capability.
8. Add the non-published JMH project and initial snapshot-lookup benchmark, compile it explicitly, and record one
   directional run without establishing a machine-specific release threshold.
9. Update docs and the accepted Proposal 1 transition notes to point at the final catalog model and remaining runtime/
   codec follow-ons.
10. Run formatting, focused reference-data/application tests, immutable-JAR compiler/adversarial tests,
   `sbt -batch clean test`, strict OpenSpec validation, staged-scope checks, and fresh independent review.

Rollback is a source-level revert of this unreleased change. No persisted catalog schema or production interpreter is
introduced, so rollback requires no external data migration.
