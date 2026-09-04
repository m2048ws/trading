## 1. Lifecycle-bound source-fact construction and boundary evidence

- [ ] 1.1 Add final immutable `SourceFact.LifecycleScope[D, B, Q]` and `SourceFact.forLifecycle`, with typed
  `accepted`, `rejected`, `fill`, `corrected`, `busted`, `cancellationEffective`, `reconciliationCheckpoint`,
  `sourceOrderCompleted`, and `sourceOrderAbsent` delegates that omit only the captured lifecycle and leave every
  fact-owner `create` operation unchanged; verify `executionLifecycle / Compile / compile` succeeds and focused source
  inspection finds no cast, mutable/cache, effect, thread-local, state, replay, observation, or downstream mechanism.
- [ ] 1.2 Add `SourceFactScopeSuite` coverage comparing every scoped operation with its direct fact-owner counterpart
  for precise result types, valid values, complete deterministic common and fact-specific violations, same-shaped
  foreign logical-order/target/instrument/grid inputs, independent instances, interleaved invocation-order invariance,
  and concurrent reuse; verify `executionLifecycle / Test / testOnly trading.execution.SourceFactScopeSuite` passes.
- [ ] 1.3 Add completed-artifact Scala fixtures that construct all nine fact forms through one lifecycle scope without
  explicit dimensions, nested role projections, casts, or structural refinements at the calls, and independently valid
  negative preludes that reject incompatible fill/correction lots and prices; extend the execution compiler-boundary
  suite to verify the exact types, intended diagnostics, scope ownership, and unchanged pure dependency cone, then
  verify `adversarialBoundary / Test / testOnly external.ExecutionLifecycleCompilerBoundarySuite` passes.
- [ ] 1.4 Run `tools/check-in-process-reflection.sh`, `tools/test-check-in-process-reflection.sh`, `git diff --check`,
  and `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test benchmarks/Jmh/compile`; verify the complete JDK-25
  repository matrix passes with no source-fact algebra, validation order, identity, grid, ordering, reference,
  completeness, dependency, serialization, or runtime regression, then complete the automated Task Group review with
  no unresolved actionable finding.
