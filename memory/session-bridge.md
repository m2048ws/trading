---
type: memory
updated: 2026-09-04
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0008-simplify-instrument-dependent-apis
- **RFC Revision**: bf457296faa7d29aec0840a7e8f48d41ed0f7491
- **Slice**: S-05-bind-instrument-risk
- **Issue**: 61 https://github.com/m2048ws/trading/issues/61
- **Change**: bind-instrument-risk
- **Worktree**: /Users/m/src/money/.worktrees/bind-instrument-risk
- **Phase at Checkpoint**: planning_ready
- **Task Group at Checkpoint**: 1
- **Observed Run Revision**: none
- **Last Verified HEAD**: b7c762de8f9a1a0fa512be58a7eb95fb2f12d93d

## Next Action
- Start Apply for `bind-instrument-risk` Task Group 1.

## Blockers
- none

## Uncommitted Work
- none

## Discoveries
- RFC-0008/S-02 Task Group 1 adds a final `Order`-owned scope that retains only one exact instrument and delegates all
  four standard constructors to canonical validation; focused, concurrent-reuse, and completed-artifact tests preserve
  exact result refinements, deterministic failures, runtime identity checks, and five incompatible input boundaries.
- No active Corgi Run Contract or OpenSpec Change exists on primary `main`.
- RFC-0003/S-01 is archived; its delivery record is `wiki/deliveries/RFC-0003-execution-lifecycle-foundation-S-01-actual-execution-lifecycle.md`.
- The registered `introduce-application-and-runtime-foundation` worktree has no active Change and remains cleanup-only.
- Task Group 1 classifies 623 method-handle/reflection tokens in 26 production/benchmark files; the checked regression
  ceiling will be removed owner by owner and must reach zero in Task Group 10.
- Task Group 2 removed the order-model allowance (28 tokens), all six reflective construction sites, invocation
  recovery casts, and hostile exact-class gates while retaining order evidence and aggregate validation.
- Task Group 3 removed the scenario allowance (24 tokens), all five reflective construction sites and invocation-only
  casts while retaining associated evidence, ordered validation, non-empty slices, and round-trip flatness.
- Task Group 4 removed both fee allowances (24 tokens) and all four reflective construction/result casts while retaining
  identity, source-slice/leg attribution, deterministic errors, exact settlement composition, and serialization policy.
- Task Group 5 removed all three risk/benchmark allowances (40 tokens), staticized construction and observation, and
  removed hostile decision guards while retaining checked casts, monotonicity, exactness, witness and complexity bounds.
- Task Group 6 removed four execution identity/authority/ordering/lineage allowances (144 tokens), staticized checked
  construction, and removed hostile exact-class gates while retaining qualification, ordering, and lineage predicates.
- Task Group 7 removed three execution command/submission/cancellation allowances (102 tokens), staticized derived
  evidence and state, and removed hostile implementation guards while retaining idempotency, conflicts, and race semantics.
- Task Group 8 removed five execution fact/state/replay/effective-ledger allowances (185 tokens), staticized derived
  transitions and observations, and removed hostile implementation guards while retaining exact economics, conflicts,
  ordering, completeness, correction/bust, anomaly, permutation, and indexed-complexity semantics.
- Task Group 9 removed the final seven codec/catalog-replay allowances (76 tokens), staticized boundary construction,
  added an ordinary Java checked-factory client, and retained strict external-data, V1 compatibility, canonicalization,
  reconstruction, publication, and replay semantics. The guard now reports no production or benchmark sites.
- Task Group 10 removed 25 hostile-only fixtures and the remaining constructor/finality assertions, simplified the
  runtime bridge, expanded the zero-reflection guard to Java, retained supported semantic/external-data coverage, and
  passed the clean 1,043-test aggregate plus representative risk, codec, and runtime measurements.
- Task Group 11 repaired the failed whole-change Verify by removing the remaining reference-data singleton permits and
  erased Java runtime bridge, preserving semantic lineage/reconciliation and erased-input checks, repairing change-spec
  whitespace, and passing the clean 1,043-test aggregate plus ordinary Scala/Java and runtime performance checks.
- RFC-0005/S-01 Task Group 1 replaced migration allowance accounting with a permanent zero-tolerance reflection guard,
  added an isolated deterministic regression fixture, and made the normal CI workflow execute both checks.
- RFC-0005/S-01 Task Group 2 removed the runtime-only Java-first compile order, stale bridge explanation, and duplicate
  release option; the runtime inherits mixed compilation and repository `--release 25`, with 1,043 tests and benchmark
  compilation passing after a clean build.
- RFC-0005/S-02 Task Group 1 replaces the command transition kind plus optional violations with five owner-sealed
  alternatives, removes unchecked violation extraction, and preserves applied, idempotent, command-conflict,
  dispatch-conflict, and rejection state semantics under focused and completed-artifact compiler tests.
- RFC-0005/S-02 Task Group 2 replaces replay, effective-fill, cancellation-anomaly, continuation, unsequenced-event,
  conflict, and diagnostic string keys with owner-local typed lexicographic comparisons, retaining null rejection and
  deterministic permutation semantics even when identifier components contain former `|` and `-` delimiters.
- RFC-0005/S-02 Task Group 3 removes the second effective-fill derivation from observation assembly, passes the one
  ledger value into anomaly construction, and proves the non-empty overfill object is reused by identity while all
  correction, bust, unresolved-reference, conflict, cancellation-race, exposure, and serialization behavior remains.
- RFC-0005/S-03 Task Group 1 replaces command and source transitions with generated sealed cases and replaces the
  lifecycle accepted-kind tag with applied, idempotent, conflicting, and rejected cases while preserving state,
  violation, conflict, work-accounting, exhaustive compiler-boundary, equality, and serialization behavior.
- RFC-0005/S-03 Task Group 2 replaces submission knowledge, cancellation knowledge, and effective-fill alternatives
  with direct generated sealed cases while preserving evidence, precedence, uncertainty, exact economics, modifier
  order, conflict detail, exhaustive matching, structural equality, and fail-closed serialization.
- RFC-0005/S-03 Task Group 3 converts field-valid evidence, command/source conflicts, ledgers, anomalies, observations,
  and replay results to package-owned generated products, removes their forwarding constructors and manual equality,
  retains computed accessors and fail-closed serialization, and leaves guarded states, commands, facts, identities,
  refinements, non-empty wrappers, and external reconstruction under checked construction.
- RFC-0005/S-05 Task Group 1 removes `Any`-based activation/pricing acceptance hooks and their erased-shape errors,
  makes `ScenarioAssumptions.create`, `one`, and `many` direct, keeps `fromVector` typed for empty input, and preserves
  same-shape semantic mismatch, fee, codec reconstruction, and negative associated-evidence compiler behavior.
- RFC-0005/S-05 Task Group 2 removes the reference-data, order/scenario, execution-lifecycle, and boundary-codec Java
  domain fixtures and their dedicated compiler/classloader code while retaining completed-artifact Scala clients,
  negative compiler cases, dependency inspections, semantic tests, external wire/null cases, and serialization checks.
- RFC-0005/S-05 Task Group 3 establishes Scala 3 as the supported domain source API in durable architecture guidance
  and the active specification deltas while explicitly retaining the JDK 25 baseline, Java-library/JVM integration,
  checked external representations, exact decimal conversion, canonical wire formats, and serialization rejection.
- RFC-0005/S-04 Task Group 1 replaces `AssetId`, `GridId`, and `GridVersion` with Scala-owned checked case classes,
  denies generated product reconstruction, removes the repository's last production Java sources, and preserves typed
  invalidity, value semantics, stable codec representations, Scala field observation, and serialization rejection.
- RFC-0005/S-04 Task Group 2 removes raw `UniformGrid` and `GridDefinition` factories and redundant positive-quantum
  reconstruction, trusts established `PositiveRational` values directly, and retains null rejection, generative grid
  identity, exact interpretation, and precise raw-invalidity diagnostics at refinement and codec boundaries.
- RFC-0005/S-04 Task Group 3 replaces catalog commit and transition product emulation with an exhaustive sealed Scala
  sum and direct structural products, retains model-owned issuance, null and serialization rejection, publication and
  idempotence semantics, and passes the clean 1,042-test aggregate, benchmark compile, guards, source scans, formatting,
  and strict readiness.

## Promotion Queue
- RFC-0008/S-04 Task Group 1 adds final codec-owned `ScenarioRecord` encoder and immutable-snapshot decoder contexts
  whose ten methods delegate to the canonical order- and round-trip-scenario record operations; focused and
  completed-artifact evidence preserves exact dependent types, canonical wire and diagnostic behavior, coherent
  snapshot use, incompatible-shape rejection, independent allocation, concurrent reuse, and the codec dependency cone.
- RFC-0008/S-03 Task Group 1 adds a final `SourceFact`-owned lifecycle scope whose nine operations delegate to the
  canonical fact constructors; focused and completed-artifact evidence preserves exact types, deterministic validation,
  foreign identity/grid rejection, independent allocation, concurrent reuse, and the pure execution dependency cone.
