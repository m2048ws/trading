## 1. Bound scenario-record contexts and compatibility evidence

- [ ] 1.1 Add final immutable `ScenarioRecord.Encoder` and `ScenarioRecord.Decoder` contexts with
  singleton-preserving `encoder` and `decoder` factories; implement the four order/round-trip projection and
  encoding methods plus six record, wire, and batch reconstruction methods as exact typed delegates while leaving
  parsing, record-only encoding, schemas, limits, indices, and all current record-family entry points unchanged;
  verify `boundaryCodecs / Compile / compile` succeeds and focused source inspection finds no cast, mutable/cache,
  live-catalog, effect, application/runtime, persistence, or downstream mechanism.
- [ ] 1.2 Add `ScenarioRecordScopeSuite` coverage comparing all ten bound operations with their direct counterparts
  for exact result types, V1 records, canonical golden bytes, successful values, malformed/version/limit/null and
  domain/catalog failures, complete deterministic ordering, paths and indices, atomic batch ordering, explicit limits,
  context-free record/parser/schema use, distinct-snapshot coherence, independent values, interleaved invocation-order
  invariance, and concurrent reuse; verify
  `boundaryCodecs / Test / testOnly trading.codec.ScenarioRecordScopeSuite` passes.
- [ ] 1.3 Add completed-artifact Scala fixtures that reuse one encoder and one decoder across order and round-trip
  scenarios without explicit dimension arguments, nested role projections, local `D`/`B`/`Q` aliases, casts, or
  structural refinements at the calls, plus an independently valid negative prelude rejecting both scenario families
  from incompatible dimensions; extend the boundary-codec compiler suite to verify exact result relationships,
  intended diagnostics, context-free operations, codec ownership, and the unchanged dependency cone, then verify
  `adversarialBoundary / Test / testOnly external.BoundaryCodecCompilerBoundarySuite` passes.
- [ ] 1.4 Run `tools/check-in-process-reflection.sh`, `tools/test-check-in-process-reflection.sh`,
  `git diff --check`, and `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test benchmarks/Jmh/compile`; verify
  the complete JDK-25 repository matrix passes with no scenario record, schema, canonical wire, version,
  associated-evidence, catalog, market/scenario validation, error location/order, batch, dependency, serialization, or
  runtime regression, then complete the automated Task Group review with no unresolved actionable finding.
