---
type: memory
updated: 2026-09-04
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0005-simplify-post-trust-boundary
- **RFC Revision**: 7f1d1ab9eb88d0912d3219920eacadea4d01aa2e
- **Slice**: S-02-total-and-deterministic-execution-transitions
- **Issue**: 40 https://github.com/m2048ws/trading/issues/40
- **Change**: total-and-deterministic-execution-transitions
- **Worktree**: /Users/m/src/money/.worktrees/total-and-deterministic-execution-transitions
- **Phase at Checkpoint**: awaiting_verify
- **Task Group at Checkpoint**: 3
- **Observed Run Revision**: 4
- **Last Verified HEAD**: 869425ad202580b8da18835af8b741468cd995a8

## Next Action
- Commit and acknowledge Task Group 3, then run canonical whole-change Verify.

## Blockers
- none

## Uncommitted Work
- Task Group 3 passes the observation's single effective-fill ledger into anomaly construction and proves exact overfill
  value reuse; the zero-reflection guard and clean 1,047-test aggregate pass.

## Discoveries
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

## Promotion Queue
- none
