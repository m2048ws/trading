## 1. Contract and Baseline Gates

- [ ] 1.1 Confirm the effective RFC/source/traceability bindings are unchanged and `origin/main` contains the accepted
  contract; no other RFC-0002 Slice is an implementation prerequisite.
- [ ] 1.2 Refresh Git/source/build/Corgi state and record a clean baseline for formatting, compilation, pure catalog
  tests, application compiler checks, external-artifact compilation, and full repository tests.
- [ ] 1.3 Inventory existing application-port, catalog-root/bootstrap, conformance-harness, build, test, and forward-
  reference surfaces; reconcile them with this proposal before implementation.

## 2. Runtime Module and Dependency Boundary

- [ ] 2.1 Add `runtime` in `runtime/`, artifact `trading-runtime`, package `trading.runtime`, depending only on
  application, reference data, an approved compatible Cats Effect 3 release, and test-scoped `munit-cats-effect-3` plus
  the Cats Effect-version-aligned `cats-effect-testkit`.
- [ ] 2.2 Wire root aggregation, ordered tests, completed-JAR external-artifact compilation, adversarial/compiler-test
  classpaths, and documentation for the new runtime artifact.
- [ ] 2.3 Preserve the existing application artifact without a concrete effect-runtime or FS2 production dependency and
  add compiler guards proving every pure artifact remains independent of application/runtime.
- [ ] 2.4 Add module/API checks proving application exposes no `IO`, `Ref`, `Resource`, fiber, queue, lock, client,
  transaction handle, tracer, metrics backend, or runtime implementation type.

## 3. In-Memory Live-Catalog Construction

- [ ] 3.1 Implement effect-delayed fresh catalog-root and revision-zero state allocation behind an
  `InMemoryLiveCatalog` factory returning only the abstract `LiveCatalog[F]` capability.
- [ ] 3.2 Evaluate an optional explicit bootstrap batch through `CatalogModel.commit` before allocating/exposing live
  state; return identical ordered typed violations and no interpreter for invalid bootstrap.
- [ ] 3.3 Initialize one private Cats Effect `Ref` with the successful bootstrap successor or empty state and retain one
  lineage for the interpreter lifetime.
- [ ] 3.4 Keep the weakest honest `F[...]` constructor for the resource-free in-memory interpreter and verify it composes
  into a larger runtime `Resource` without adding a ceremonial finalizer.
- [ ] 3.5 Add construction tests for empty/valid/idempotent/invalid bootstrap, revision/delta results, fresh lineage per
  instance, and inaccessible internal state/constructors.

## 4. Atomic Snapshot and Commit Interpretation

- [ ] 4.1 Implement snapshot capture as one atomic state read followed by immutable `CatalogSnapshot` use, with no
  effectful point-lookup methods or retained pointer from snapshot to live state.
- [ ] 4.2 Implement commit as one private atomic modification invoking only the total pure catalog transition; retain the
  current state on failure/idempotence and publish the complete successor only for a non-empty valid change.
- [ ] 4.3 Ensure effects, telemetry, callbacks, resource acquisition, and external publication never execute inside a
  retryable atomic-modification function.
- [ ] 4.4 Test sequential publication, idempotent retry, immutable conflict, accumulated stable errors, revision/delta
  conservation, historical snapshot stability, and canonical within-lineage handle relationships.

## 5. Concurrency, Cancellation, and Performance Evidence

- [ ] 5.1 Test racing independent valid commits for one linear order, consecutive revisions, no lost additions, and a
  final snapshot containing every accepted definition on a real multi-threaded Cats Effect runtime.
- [ ] 5.2 Test racing conflicting commits so at most one publishes and every loser is revalidated against the winning
  successor with the expected typed conflict on a real multi-threaded Cats Effect runtime.
- [ ] 5.3 Add controlled cancellation tests proving a commit exposes only a complete predecessor or successor and that an
  identical retry after an unobserved publication is unchanged without revision advancement; use Cats Effect TestKit
  for deterministic scheduling/time where applicable without treating it as contention evidence.
- [ ] 5.4 Add repeatable stress tests for snapshot/commit coherence and interpreter isolation without asserting scheduler-
  specific interleavings.
- [ ] 5.5 Extend the existing non-published JMH project with benchmark-only application/runtime dependencies and
  directional measurements separating coordinated snapshot capture, uncontended/contended commit, and pure snapshot
  lookup; record JDK/fork/warmup/measurement/thread parameters and verify high-volume lookup/decoding takes one captured
  snapshot rather than one `Ref` access per value.

## 6. Shared Interpreter Conformance

- [ ] 6.1 Reuse and, where necessary, generalize the existing application-test conformance harness around an interpreter
  constructor without moving Cats Effect or MUnit into application production sources.
- [ ] 6.2 Instantiate the shared suite for the in-memory interpreter and compare bootstrap, lookup, publication,
  idempotence, conflicts, error ordering, revisions, deltas, and within-lineage relationships with the pure model.
- [ ] 6.3 Verify two independently created interpreters with equal visible definitions have structurally equivalent
  outcomes but distinct non-reconciling lineages.
- [ ] 6.4 Document the shared-versus-interpreter-specific contract split so later database, actor, live, simulation,
  backtest, or venue interpreters reuse observable suites and add their own resource/integration coverage.

## 7. Application/Runtime Architecture Enforcement

- [ ] 7.1 Document the initial/final encoding rule: durable commands/events remain explicit data, pure semantics remain
  functions, and only genuine environment-varying operations become narrow `F[_]` ports.
- [ ] 7.2 Add architecture/API inspections proving there is no global application environment, service locator,
  capability registry, universal application error, free-program layer, or effect-wrapped pure domain facade.
- [ ] 7.3 Document and inspect the future-port admission checklist for market data, trade persistence, business time,
  order execution, transactions, and telemetry without adding speculative production interfaces for them.
- [ ] 7.4 Document concurrency/streaming placement and verify no FS2/custom stream, callback registry, scheduler, or
  concurrency constraint enters pure domain or application APIs in this change.
- [ ] 7.5 Document wall-clock versus monotonic scheduling, business-shaped atomic operations versus scoped
  transactions, and runtime telemetry decorators versus guaranteed durable audit.
- [ ] 7.6 Reconcile existing documentation and forward references so this Slice is the runtime/admission foundation
  and does not imply delivery of semantically unspecified future ports.

## 8. Verification Evidence and Corgi Handoff

- [ ] 8.1 Format all affected Scala/SBT/Markdown sources and run clean compilation in dependency order.
- [ ] 8.2 Run pure catalog/application tests, shared conformance, TestKit-controlled cancellation/time cases, real-
  runtime concurrency/stress tests, explicit JMH compilation and focused runtime benchmarks, completed-JAR compiler
  boundaries, adversarial tests, and the full repository validation matrix.
- [ ] 8.3 Inspect packaged APIs/dependency reports for leaked Cats Effect outside runtime, leaked `Ref`/concrete classes,
  reverse dependencies, per-value live reads, speculative ports, broad capability products, or false durability/
  exactly-once guarantees.
- [ ] 8.4 Confirm the current Slice still satisfies strict planning/source/traceability integrity and reconcile any
  implementation drift before the final Task Group checkpoint.
- [ ] 8.5 Prepare the final acknowledged Task Group commit and evidence for separate canonical Verify, human review,
  human QA, and Archive; do not begin the boundary-codec Slice in this delivery.
