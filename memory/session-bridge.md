---
type: memory
updated: 2026-09-03
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0004-simplify-in-process-trust-boundary
- **RFC Revision**: 24090e8f0af2b04c733f8bffd2b394f232084877
- **Slice**: S-01-simplify-in-process-trust-boundary
- **Issue**: 36 https://github.com/m2048ws/trading/issues/36
- **Change**: simplify-in-process-trust-boundary
- **Worktree**: /Users/m/src/money/.worktrees/simplify-in-process-trust-boundary
- **Phase at Checkpoint**: awaiting_verify
- **Task Group at Checkpoint**: 10
- **Observed Run Revision**: 11
- **Last Verified HEAD**: aff014e3c3356d32699a4af3a09fa59d354946d3

## Next Action
- Run the separate canonical whole-change Verify gate, then proceed to explicit human Review.

## Blockers
- none

## Uncommitted Work
- none

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

## Promotion Queue
- none
