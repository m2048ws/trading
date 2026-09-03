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
- **Phase at Checkpoint**: applying
- **Task Group at Checkpoint**: 6
- **Observed Run Revision**: 7
- **Last Verified HEAD**: 5c362f39ab499198aef669bdecdb8f1b9eb87045

## Next Action
- Commit and acknowledge Task Group 6, sync the draft PR, then continue with Task Group 7.

## Blockers
- Task Group 6 execution identity/authority/ordering/lineage static construction, fixture reconciliation, note, and bridge
  checkpoint are ready for their dedicated commit.

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

## Promotion Queue
- none
