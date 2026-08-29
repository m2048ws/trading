## Context

See `proposal.md` for motivation and the two delta specifications for normative behavior. This is Proposal 8 and
assumes Proposals 0–7 have established pure quantity, reference-data, instrument-economic, order, scenario, fee-policy,
and risk artifacts. Proposal 2 already creates `trading-application`, defines the minimal `LiveCatalog[F]` port, and
supplies its pure semantic model plus an interpreter-conformance harness; it intentionally supplies no live interpreter.

The repository currently has no concrete effect runtime, stream library, persistence model, external client, business
clock, operational execution protocol, trade fact, or telemetry integration. Inventing uniform ports for those nouns now
would move unknown semantic choices into permanent interfaces. At the same time, the catalog port is real, has precise
atomicity semantics, and needs a production-usable concurrent interpreter before the architecture portfolio is complete.

Proposal 9 follows with boundary representations and batch decode/replay. It may use the application/runtime placement
rules here but must not turn codecs into effects or serialize trusted catalog handles.

## Goals / Non-Goals

**Goals:**

- Make the application/runtime dependency boundary physically enforceable.
- Provide one small, lawful concurrent interpreter for the already specified live catalog.
- Choose a concrete effect system only where concrete execution requires it.
- Establish repeatable admission and testing rules for future external capabilities.
- Make cancellation, resource, transaction, streaming, time, and telemetry distinctions explicit before they spread.
- Preserve commands/events as data and pure domain semantics as the common reference for interpreters.

**Non-Goals:**

- Do not define a market-data protocol, durable trade model, execution lifecycle, account model, database schema, clock
  API, transaction API, telemetry schema, or venue client.
- Do not add FS2 merely because future ingress will stream.
- Do not create a universal `Application[F]`, `Environment[F]`, `Services[F]`, `Runtime[F]`, or registry of algebras.
- Do not make pure modules depend on Cats Effect or lift pure functions into `F`.
- Do not promise durable restart recovery, distributed linearizability, exactly-once acknowledgement, or external
  transactional behavior from the in-memory catalog interpreter.
- Do not create executable service configuration, deployment manifests, or a long-running process in this change.

## Decisions

### 1. Complete the two-layer effect boundary instead of adding one broad effects module

The final initial graph is:

```text
trading-reference-data
          ^
          |
trading-application       abstract capabilities/workflows
          ^
          |
  trading-runtime         concrete effects/resources/interpreters
```

Proposal 2 already creates `application/`, artifact `trading-application`, package `trading.application`, depending only
on reference data. This proposal adds:

```text
SBT ID:       runtime
directory:    runtime
artifact:     trading-runtime
package root: trading.runtime
depends on:   trading-application, trading-reference-data
```

`trading-runtime` starts with one interpreter, so it has real code and a useful dependency boundary. Future adapters
may remain packages within it until independent clients, release cadence, or dependency weight justify narrower
artifacts. Application adds no dependency on all downstream domain modules merely to anticipate workflows; a later
workflow adds only its actual dependencies.

Alternative considered: put the interpreter beside `CatalogState` in reference data. Rejected because coordinated live
state and an effect runtime are not reference-data meaning and would put runtime dependencies below every domain layer.

Alternative considered: make one `trading-effects` artifact containing ports and implementations. Rejected because it
would permit application workflows to import clients/concurrency and runtime implementations to become domain APIs.

### 2. Use Cats Effect 3 in runtime, while keeping application runtime-neutral

The runtime artifact uses an approved compatible Cats Effect 3 release for `Sync`, atomic references, cancellation, and
eventual resource composition. The application production artifact continues to expose `LiveCatalog[F[_]]` without a
Cats Effect type-class requirement or concrete effect. It may use Cats Core composition only when an actual workflow
needs it; no dependency is added for a hypothetical style preference.

Runtime tests use `munit-cats-effect-3` so suites return effects without unsafe execution, plus the
`cats-effect-testkit` release aligned with the selected Cats Effect version for deterministic scheduling, time, and
cancellation cases. TestKit's controlled single-threaded runtime does not prove physical races or contention, so
linearization races and stress coverage also run on a real multi-threaded Cats Effect runtime. These dependencies remain
in runtime test scope; application production and its generic conformance contract expose neither test framework nor
Cats Effect types.

No FS2 dependency is added in this proposal. A future feed/ingress proposal may add FS2 to runtime after it specifies
batching, ordering, replay, backpressure, and failure behavior. Those stream values remain runtime-facing unless the
observable port contract genuinely is a stream.

Alternative considered: implement concurrency directly with `AtomicReference`, `synchronized`, and threads. Rejected
because Cats Effect supplies referentially transparent construction/composition and testable cancellation semantics,
while raw mechanisms would form a second bespoke runtime vocabulary.

Alternative considered: make application depend on Cats Effect so every port can require `Async`. Rejected because a
port's operation signatures describe its capability; interpreters and workflows request only the effect operations
they actually use. Deterministic non-IO interpreters should remain possible.

Alternative considered: abstract over Cats Effect, ZIO, actors, and every stream library in the runtime implementation.
Rejected because the port is already abstract; abstracting the interpreter machinery itself would add a redundant
framework layer.

Alternative considered: use `unsafeRunSync` and sleeps throughout ordinary MUnit suites. Rejected because it obscures
cancellation and lifecycle behavior, produces scheduler-sensitive tests, and duplicates support already supplied by
the Cats Effect test integrations.

### 3. Implement live catalog as a private `Ref` over the one immutable state

The runtime constructor is conceptually:

```scala
object InMemoryLiveCatalog:
  def create[F[_]: Sync](
    bootstrap: Option[CatalogBatch]
  ): F[Either[CatalogViolations, LiveCatalog[F]]]
```

Creation delays fresh `CatalogRoot` allocation in `F`, constructs revision-zero `CatalogState`, and evaluates an
optional bootstrap through `CatalogModel.commit`. Invalid bootstrap returns its ordered typed violations without
publishing a capability. On success it initializes one `Ref[F, CatalogState]`; the implementation class and reference
remain private.

Operations are conceptually:

```scala
def snapshot: F[CatalogSnapshot] =
  ref.get.map(_.snapshot)

def commit(batch: CatalogBatch): F[Either[CatalogViolations, CatalogCommit]] =
  ref.modify: current =>
    CatalogModel.commit(current, batch) match
      case Left(errors)       => current          -> Left(errors)
      case Right(transition)  => transition.state -> Right(transition.outcome)
```

The exact names follow Proposal 2. The function supplied to `modify` is total and pure, so CAS reevaluation is safe.
One state reference makes all indexes/revision/lineage publish together. `get` captures one immutable state; resolution
then occurs on the returned snapshot with no `Ref` access per value.

The interpreter owns no releasable resource, so its weakest honest constructor is `F[...]`, not a ceremonial
`Resource`. An executable runtime may lift creation into its larger `Resource` graph. Future database clients,
subscriptions, and fibers must use real scoped resources with finalizers.

Alternative considered: hold separate references for assets, dimensions, grids, and revision. Rejected because no
transaction can expose a coherent cross-index snapshot without rebuilding the same single-state invariant.

Alternative considered: put effects inside `Ref.modify`. Rejected because retries could duplicate them; all transition
semantics and allocation of candidate successor handles stay pure, and observation happens outside atomic modification.

Alternative considered: expose the `Ref` or concrete class. Rejected because callers could bypass the port, replace a
lineage, or depend on one coordination mechanism.

### 4. Treat cancellation as an acknowledgement problem, not rollback magic

Atomic `Ref.modify` makes a commit visible wholly or not at all. Cancellation may be observed before modification, or
after publication but before the caller receives `Published`; no in-process API can guarantee that publication is
rolled back merely because acknowledgement was lost. The existing idempotent batch semantics are the reconciliation
mechanism: retry against the published state returns `Unchanged` without a new revision.

Runtime tests introduce cancellation around a controlled commit boundary and verify that snapshots contain either the
entire predecessor or entire successor. Documentation states that a cancelled caller may need to retry/inspect; it does
not promise exactly-once acknowledgement.

Alternative considered: make the whole effect uncancelable and claim success is always observed. Rejected because a
caller or process can still disappear after publication, and broad uncancelability harms responsiveness without
eliminating the distributed ambiguity.

### 5. Use final ports over initial command/event data

`LiveCatalog[F]` is a tagless-final interface because publication and snapshot capture vary by interpreter.
`CatalogCommand`, `CatalogBatch`, `CatalogCommit`, and `CatalogDelta` are initial data because they must be inspected,
validated, tested, retried, and later encoded. `CatalogModel.commit` remains an ordinary pure state transition.

This is the intended recurring pattern:

```text
explicit command/query/event data
              |
              v
small capability F interface
              |
              v
runtime interpreters sharing one observable contract
```

Expected business/catalog outcomes remain inside typed sums such as `F[Either[CatalogViolations, CatalogCommit]]`.
Runtime exceptions, cancellation, or transport failures remain in the chosen `F`; they are not converted to a universal
`ApplicationError`. Workflow internals may use standard transformers where useful, but public APIs retain domain-named
results.

Alternative considered: a free-monad AST for every application workflow. Rejected because durable commands already
supply inspectability where required, while arbitrary program inspection has no current consumer and complicates
resources/concurrency.

Alternative considered: direct `IO` methods. Rejected because deterministic and alternative live interpreters are a
stated requirement and the application contract does not need one runtime.

### 6. Admit future ports only after their semantics exist

This proposal reserves locations and tests a decision procedure; it does not guess missing protocols:

| Future concern | Application owns once specified | Runtime owns | Required prior decisions |
|---|---|---|---|
| Market data | Typed query/subscription contract and expected outcomes | Feed/client, buffering, reconnect, stream loop | Observation shape, freshness, ordering, gaps, replay |
| Trade persistence | Durable trade command/fact and typed commit/read contract | Database/schema client and migrations | Trade identity, idempotency, consistency, retention |
| Business time | Narrow wall-clock capability used by a named workflow | Live and deterministic clock interpreters | Timestamp meaning and authority |
| Order execution | Operational commands/events and execution port | Venue, paper, simulator, replay interpreters | Account/venue identity, lifecycle, idempotency, ordering |
| Transactions | Domain atomic operation or scoped unit of work | Connection/session, isolation, retry, outbox | Atomic aggregate and failure semantics |
| Telemetry | Only explicit guaranteed audit/event capability | Tracing/metrics decorators and exporters | Bounded observations, durability requirement |

Each future proposal must identify whether it is a command, query, event source, or state transition and must specify
observable equivalence across promised interpreters. Generic CRUD, arbitrary string operations, callback registries,
and `F[A] => F[A]` wrappers fail admission because they do not encode those meanings.

Alternative considered: add empty `MarketData[F]`, `TradeStore[F]`, `Clock[F]`, `Execution[F]`, and `Telemetry[F]`
traits immediately. Rejected because their method signatures would either be content-free or silently decide domain
models that have not been explored.

### 7. Construct workflow-specific capability products, never a repository environment

A workflow takes individual ports or a small named product containing exactly its dependencies:

```scala
final class OnboardReferenceData[F[_]](
  catalog: LiveCatalog[F]
)
```

If a later workflow needs catalog, market data, and persistence, its own constructor may contain that product. Adding a
clock elsewhere does not grow it. Top-level runtime wiring builds concrete interpreters and passes them downward; it
does not expose a mutable global registry. Capability products contain services only, not copies of instrument, market,
or account business state.

Alternative considered: one application environment case class containing every port. Rejected because it creates
false dependencies, makes tests construct irrelevant values, and becomes a de facto service locator.

Alternative considered: implicit global givens for all ports. Rejected because ambient capability resolution obscures
which effects a workflow can perform and makes local reasoning/refactoring harder.

### 8. Keep concurrency and streaming in runtime composition by default

Application workflows are semantic units that can be called once. Runtime decides whether to call them from a request,
batch loop, supervised fiber, queue consumer, or FS2 stream. Pure batch functions receive complete immutable inputs;
when runtime evaluates independent work concurrently, it restores any specified deterministic ordering before returning
or persisting results.

A future stream-valued port is allowed only if subscription/backpressure/replay/lifetime is itself part of what callers
observe. In that case, the proposal chooses and exposes a stream abstraction deliberately rather than hiding it behind
callbacks. The application layer still does not define its own `Stream[F, A]` clone.

Alternative considered: make every source return a generic callback registration. Rejected because callbacks obscure
resource lifetime, cancellation, backpressure, and error composition.

Alternative considered: put `Concurrent[F]` constraints on domain operations so callers may parallelize them. Rejected
because parallel scheduling is independent of pure semantics and belongs to the caller/runtime.

### 9. Separate wall-clock facts from monotonic runtime time

When a future business event needs an authoritative occurrence timestamp, the owning workflow will depend on a narrow
clock capability returning a wall-clock instant and place that instant explicitly in the event. Deterministic tests and
backtests interpret it from supplied time.

Timeouts, sleeps, retry delays, rate limiting, and elapsed durations use Cats Effect monotonic/temporal behavior inside
runtime. They do not create business timestamps and do not justify a domain clock dependency. No clock is added until a
workflow consumes it.

Alternative considered: call `Instant.now()` inside entities/validators. Rejected because it makes construction
nondeterministic and replay dependent on execution time.

Alternative considered: one clock method serving both wall time and monotonic scheduling. Rejected because those
values have different laws and only wall time can be persisted as a business observation.

### 10. Put atomicity in business-shaped operations, with scoped transactions only when necessary

The preferred persistence port makes one aggregate commit one operation—for example, a future trade-plus-outbox commit.
Its interpreter starts/commits/rolls back the database transaction internally, and its command includes the idempotency
identity required to reconcile ambiguous acknowledgement.

If several operations must be composed atomically, their proposal must introduce a rank-scoped transaction program: it
provides only repositories bound to one hidden session/effect and prevents that session from escaping the result. The
exact Scala encoding is deferred until the participating operations are known, but it cannot have the unsound shape:

```scala
def transaction[A](alreadyBuilt: F[A]): F[A]
```

That shape cannot prove which connections `alreadyBuilt` captured. External network calls are not silently included in
a database transaction; an outbox, saga, or other explicit protocol states the coordination.

Alternative considered: expose a raw JDBC/database transaction handle. Rejected because application logic would depend
on a concrete client and callers could mix transactional and nontransactional operations unknowingly.

### 11. Apply telemetry as runtime decorators and model required audit separately

Runtime may wrap any port or workflow with spans, counters, histograms, and bounded attributes. A decorator preserves
the underlying typed result; best-effort telemetry failure is handled by its backend policy and does not turn success
into domain failure. No `traceId: String` or arbitrary label map is added to domain entities merely for instrumentation.

Required audit is different: an audit fact is explicit durable event data and participates in a specified persistence
capability/transaction. This distinction prevents a best-effort tracing pipeline from becoming an accidental system of
record.

No telemetry library is added now because there is no runtime integration to instrument beyond an in-memory reference.
The module and decorator boundary are enforced through imports and later interpreter contract review.

Alternative considered: add `Telemetry[F]` to every workflow constructor. Rejected because it creates pervasive
coupling and lets operational labels become arbitrary business APIs. A narrow semantic observation capability remains
possible if a later workflow needs guaranteed observation.

### 12. Reuse Proposal 2's conformance harness and extend it at the runtime boundary

Proposal 2 places a parameterized `LiveCatalog` semantic contract in application test sources. Runtime tests depend on
that test configuration and instantiate it with the in-memory constructor, then add cancellation/stress tests specific
to Cats Effect. The shared suite compares definitions, revisions, deltas, lookup results, error order, idempotence, and
within-lineage handle relationships with the pure model; independently created roots are compared structurally while
also proving their lineages do not reconcile.

Deterministic cancellation, temporal boundaries, and acknowledgement ambiguity may use Cats Effect TestKit. Concurrent
commit ordering, lost-update rejection, and contention behavior use the real runtime and assertions over permitted
outcomes rather than scheduler-specific interleavings. Proposal 2's non-published JMH project is extended with
benchmark-only dependencies on application and runtime to separate one coordinated snapshot capture, pure lookup,
uncontended commit, and contended commit costs; no numeric result becomes a portable contract.

This avoids a third production module solely for tests. If independently published interpreter projects later need the
suite, extracting `trading-application-testkit` is justified then by an actual publication consumer.

Alternative considered: put MUnit/Cats Effect test helpers in application production sources. Rejected because it would
pollute the public runtime-neutral artifact with test/runtime dependencies.

Alternative considered: test only the `Ref` implementation directly. Rejected because future interpreters need one
observable standard and implementation-shaped tests can miss semantic drift.

## Risks / Trade-offs

- [Choosing Cats Effect biases the first runtime ecosystem] → Confine it to `trading-runtime`; application ports and all
  pure artifacts remain runtime-neutral, so another runtime can interpret the same contracts in another artifact.
- [A CAS interpreter may reevaluate a large pure batch under contention] → Keep catalog publication control-plane,
  benchmark snapshot and commit paths separately in the shared JMH project, bound operational batch sizes if
  measurement requires it, and retain actor/database alternatives behind the same port.
- [Cancellation can lose a successful acknowledgement] → Document the ambiguity, preserve atomic publication, and use
  catalog idempotence for safe retry instead of claiming rollback or exactly-once delivery.
- [Broad architectural rules may decay as integrations arrive] → Make every new port proposal answer the admission
  checklist and include module/contract tests in its tasks and independent review.
- [Test-to-test project dependency is repository-specific] → Keep it while there is one runtime consumer; extract a
  published testkit only when an external or independently published interpreter needs it.
- [`trading-runtime` could grow into an infrastructure monolith] → Start with one real interpreter and split adapters by
  dependency/publication boundary when actual client code makes that useful; do not add empty adapter modules.
- [Fresh in-memory roots cannot provide restart continuity] → Persist stable commands/IDs, create a new lineage on
  restart, and resolve through the new snapshot as required by Proposals 2 and 9.

## Migration Plan

1. Apply Proposals 0–7 in order and verify Proposal 2 has created the runtime-neutral application artifact, pure catalog
   model, `LiveCatalog[F]`, and shared conformance harness.
2. Add the `runtime` project, packaged-artifact/adversarial wiring, Cats Effect 3 production dependency, MUnit Cats
   Effect/Cats Effect TestKit test dependencies, and strict compiler guards against reverse dependencies.
3. Add effect-delayed fresh-root/bootstrap construction and the private `Ref`-backed interpreter using only the pure
   catalog transition inside atomic modification.
4. Instantiate the shared semantic suite and add TestKit-controlled cancellation/time cases plus real-runtime
   concurrent race, bootstrap, lineage, stress, and hot-read tests.
5. Add application/runtime architecture documentation and negative/API checks for service locators, leaked runtime
   primitives, concrete effects in application, and effect dependencies in pure artifacts.
6. Reconcile Proposal 2's forward references so Proposal 8 is described as the admission/runtime foundation rather than
   as a promise to invent unspecified ports.
7. Extend the shared non-published JMH project with runtime snapshot/commit measurements and recorded environment/
   harness parameters, without a numeric release threshold.
8. Run formatting, clean dependency-ordered compilation, focused/runtime/conformance tests, explicit JMH compilation/
   measurement, completed-JAR compiler checks, adversarial tests, full repository validation, strict OpenSpec
   validation, and fresh independent review.
9. Apply Proposal 9 only after this interpreter and the pure snapshot boundary are accepted, so boundary decode/replay
   callers can capture one live snapshot and process batches without per-record coordination.

Rollback is a source-level revert of the new runtime project and forward-reference documentation. No durable schema,
external resource, or deployment is created; the pure catalog and application port from Proposal 2 remain usable with
explicit state threading and deterministic test interpreters.
