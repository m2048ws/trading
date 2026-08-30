---
type: memory
updated: 2026-08-30
---

# Session Bridge

> Durable checkpoint mirror. Read first at startup; `.corgi/loop` remains lifecycle authority. `corgispec archive --local` alone writes archive closeout fields.

## Delivery Pointer
- **RFC**: RFC-0002-architecture-portfolio
- **RFC Revision**: 7b6f7a58f4dcbb8fb4bbdf3a8ba74ba66f222cce
- **Slice**: S-01-application-runtime-foundation
- **Issue**: 6 https://github.com/m2048ws/trading/issues/6
- **Change**: introduce-application-and-runtime-foundation
- **Worktree**: /Users/m/src/money/.worktrees/introduce-application-and-runtime-foundation
- **Phase at Checkpoint**: applying
- **Task Group at Checkpoint**: 6
- **Observed Run Revision**: 7
- **Last Verified HEAD**: 14854ce174059114e01ee7e9ef29578c8268cfb0

## Next Action
- Commit and acknowledge Task Group 6 after its shared conformance checks and structured review.

## Blockers
- none

## Uncommitted Work
- The framework-neutral effectful conformance harness, its thunk registration layer, the in-memory IO instantiation,
  shared-versus-interpreter-specific documentation, and this Task Group 6 bridge checkpoint.

## Discoveries
- Baseline inventory found `LiveCatalog[F]`, `CatalogModel`, completed-JAR compiler fixtures, and the non-published
  benchmark project, but no runtime module or concrete live-catalog interpreter at that checkpoint.
- Current Typelevel `munit-cats-effect` 2.x is Cats Effect 3-only and supersedes the old
  `munit-cats-effect-3` artifact coordinate named by the planning prose.
- The `trading.runtime` package anchor contains documentation only and publishes no speculative callable member before
  the Group 3 interpreter factory.
- The public factory exposes only `LiveCatalog[F]`; the concrete class and its single `Ref[F, CatalogState]` are private
  and rejected by the completed-artifact compiler fixture.
- Sequential, failed, idempotent, and historical-snapshot checks confirm one published state lineage with exact
  revision/delta conservation and deterministic accumulated errors.
- Real-runtime races establish one lossless publication order and typed loser revalidation; deterministic TestControl
  evidence proves cancellation exposes only a complete predecessor or successor and supports an unchanged retry.
- The live-runtime JMH run separates coordinated snapshot capture, one-capture high-volume lookup, uncontended
  publication, and four-thread contention; its JDK 26.0.2 figures are directional machine-local evidence only.
- The application test-source contract now returns effectful cases without MUnit, Cats Effect, or synchronous effect
  execution; thin thunk and IO suites supply their own constructors, sequencing, and concurrency.
- Pure-oracle snapshots compare structurally across independently allocated roots; handle reconciliation is asserted
  only within one interpreter lineage, while equal independent interpreters are required not to reconcile.

## Promotion Queue
- After whole-change verification and Archive, promote the delivered runtime/application dependency boundary and
  in-memory interpreter ownership from concrete source evidence.
- Retain the dependency-coordinate rename as delivery evidence; promote only if it remains true at Archive.
- Preserve the one-capture high-volume lookup guidance and atomic concurrency semantics if whole-change verification
  confirms them.
- Promote the shared observable contract versus interpreter-specific mechanism-test split if final verification
  confirms both instantiations.
