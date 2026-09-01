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
- **Phase at Checkpoint**: awaiting_verify
- **Task Group at Checkpoint**: 9
- **Observed Run Revision**: 2
- **Last Verified HEAD**: 27fe6b25b114dd2803c963c69bdeec828b3a04c4

The phase and revision above are the required next checkpoint carried by the Task Group commit. Live Run Contract
authority remains `applying` at revision 1 until Corgi acknowledges that commit.

## Next Action
- Commit and acknowledge repair Task Group 9, synchronize draft PR #16, and stop Apply at `awaiting_verify`; canonical
  Verify remains a separate gate.

## Blockers
- none

## Uncommitted Work
- The JVM construction-boundary repair, shared multi-error commit contract, genuine-publication contention benchmark,
  current-`main` reconciliation, and this Task Group 9 verification/handoff checkpoint.

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
- The final clean dependency-ordered gate passes 802 tests: 601 quantities, 13 reference-data, 9 application, 18
  runtime, 40 economics, and 121 packaged adversarial tests; formatting and explicit JMH compilation also pass.
- A focused JDK 26.0.2 JMH rerun completed all four runtime paths with the recorded one-fork, three-warmup,
  five-measurement configuration and preserved the intended one-capture high-volume lookup shape.
- Production dependency reports place Cats Effect only in runtime; the completed application API contains only
  `LiveCatalog.snapshot` and `LiveCatalog.commit`, while Scala compiler evidence rejects the private Ref-backed class.
- The Ref-backed implementation is a private nested JVM class with a private constructor behind a non-public bridge;
  the bridge binds the actual `InMemoryLiveCatalog` factory class and code source, while the constructor independently
  rejects reflective and private-method-handle callers. Package-spoofing Scala and Java fixtures reject the actual
  nested type, and artifact checks prove private modifiers, no `Ref`/`Sync` fields or constructor parameters, and JDK
  17 classfile version 61.
- The shared `LiveCatalogContract` now requires failed commits to return both stable ordered violations while retaining
  the complete predecessor and reconcilable handles; both interpreters run it, and a targeted lineage-corrupting
  negative control proves that exact shared case rejects structurally similar replacement state.
- The four-thread contention benchmark now submits unique valid batches, rejects any non-publication, resets its shared
  interpreter per iteration, and recorded 3,659.590 ±69.860 aggregate ops/s on the documented JDK 26.0.2 run.
- The repair group incorporates current `main`'s removal of the retired steward/worker workflow and reconciles the only
  overlapping architecture audit without entering Issues #7–#10.
- Strict readiness passes with planning revision
  `sha256:24744edcf2e6291c9487647650d576e233e604c1fa42e9cb14f73554d7dd748f`, current RFC ancestry, source provenance,
  and complete AC traceability.

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
