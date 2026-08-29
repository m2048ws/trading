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
- **Task Group at Checkpoint**: 7
- **Observed Run Revision**: 8
- **Last Verified HEAD**: 82cb7509bc062cacc045c4508d16afc14515d0db

## Next Action
- Commit and acknowledge Task Group 7 after its application/runtime architecture checks and structured review.

## Blockers
- none

## Uncommitted Work
- The strengthened completed-artifact architecture scan, RFC-first architecture/audit reconciliation, explicit future-
  port admission rules, and this Task Group 7 bridge checkpoint.

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
- The completed application JAR still contains only `LiveCatalog`, whose only methods are `snapshot` and `commit`; the
  scan rejects universal environments/errors, service locators, capability registries, free programs, speculative
  future ports, effect-wrapped pure facades, and runtime concurrency/stream vocabulary.
- RFC-0002 S-01 is now documented as the implemented runtime and port-admission foundation, with wall versus monotonic
  time, business atomicity versus scoped transactions, and telemetry versus durable audit explicitly separated.

## Promotion Queue
- After whole-change verification and Archive, promote the delivered runtime/application dependency boundary and
  in-memory interpreter ownership from concrete source evidence.
- Retain the dependency-coordinate rename as delivery evidence; promote only if it remains true at Archive.
- Preserve the one-capture high-volume lookup guidance and atomic concurrency semantics if whole-change verification
  confirms them.
- Promote the shared observable contract versus interpreter-specific mechanism-test split if final verification
  confirms both instantiations.
- Promote the future-port admission checklist only after whole-change verification confirms the packaged API remains
  the single narrow live-catalog capability.
