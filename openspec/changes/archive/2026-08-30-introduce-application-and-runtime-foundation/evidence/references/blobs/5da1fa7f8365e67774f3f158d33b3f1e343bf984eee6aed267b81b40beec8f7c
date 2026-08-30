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
- **Task Group at Checkpoint**: 5
- **Observed Run Revision**: 6
- **Last Verified HEAD**: c54f944a52e6d15ca45ff5fdf3d5a006da859219

## Next Action
- Commit and acknowledge Task Group 5 after its concurrency, cancellation, stress, and performance checks and
  structured review.

## Blockers
- none

## Uncommitted Work
- Concurrency/cancellation/stress tests, live-runtime benchmark code and evidence, the benchmark dependency update, and
  this Task Group 5 bridge checkpoint.

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

## Promotion Queue
- After whole-change verification and Archive, promote the delivered runtime/application dependency boundary and
  in-memory interpreter ownership from concrete source evidence.
- Retain the dependency-coordinate rename as delivery evidence; promote only if it remains true at Archive.
- Preserve the one-capture high-volume lookup guidance and atomic concurrency semantics if whole-change verification
  confirms them.
