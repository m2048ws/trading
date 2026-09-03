# Task Group 11 — Verify repair: reference authority and final hygiene

## Verify findings repaired

The first whole-change Verify passed AC-001, AC-002, AC-003, AC-004, and
AC-006, but failed AC-005 because reference-data still used singleton permits
as a universal internal authority mechanism. It also found trailing blank-line
errors in ten change-specification deltas. The repair planning baseline removed
those whitespace errors without changing normative text and added this Task
Group before implementation resumed.

## Reference-data authority simplification

`GridDefinition`, `DimensionHandle`, `Asset`, `GridHandle`, `CatalogRoot`,
`CatalogState`, `CatalogSnapshot`, `CatalogViolations`, `CatalogCommit`, and
`CatalogTransition` no longer carry or validate singleton permits. Their private
or owner-local construction paths remain, as do null checks, positive-quantum
revalidation, ordered/non-empty violation checks, and transition invariants.

The catalog's lineage object remains because it represents real provenance,
not construction permission. Handle reconciliation still checks lineage, full
stable grid identity, canonical dimension, and immutable quantum before
returning opaque reconciliation evidence. That evidence now carries an already
checked stable identity rather than a globally shared permission object;
retyping rejects null evidence and contains the one cast justified by the
preceding reconciliation predicate. Deliberate JVM fabrication remains the
accepted non-goal.

## Runtime simplification

`InMemoryLiveCatalog` now constructs `LiveCatalog[F]` directly over the same
`Ref[F, CatalogState]`. The erased Java `LiveCatalogBridge`, Java functional
interfaces, effect casts, and bridge-result cast were removed. Bootstrap,
snapshot, atomic `Ref.modify` commit, publication, and concurrency behavior are
unchanged.

This supersedes the interim Task Group 10 state in which the Java bridge had
already lost its reflective caller checks but still existed as an erased
adapter.

## Acceptance and verification evidence

| Acceptance criterion | Repair evidence |
| --- | --- |
| AC-001 | The accepted RFC, design, permanent architecture/design guidance, and change specs continue to define cooperative in-process code and deliberate forgery as a non-goal. |
| AC-002 | The reflection guard passes with no production or benchmark sites; the runtime Java bridge and its casts are absent. |
| AC-003 | Positive grid construction, catalog commands, typed violations, null checks, and all semantic strengthening checks remain exercised by focused and aggregate tests. |
| AC-004 | Catalog lineage, identity, dimension, quantum, atomic publication, snapshot, codec/replay, and concurrency behavior pass the complete test matrix. |
| AC-005 | All reference-data singleton permission machinery is gone; source scans find no construction, handle, or reconciliation permit and no `LiveCatalogBridge`. Supported ordinary Scala and Java boundaries remain green. |
| AC-006 | Ordinary Scala and Java reference-data clients, application/runtime clients, and codec/replay paths compile and run without reflection or the erased bridge. |

| Check | Result |
| --- | --- |
| Focused reference-data, application, runtime, codec/replay, and ordinary Scala/Java boundary suites | Pass: 13 reference-data, 9 application, 18 runtime, 94 codec/replay, and 24 selected boundary tests. |
| Clean root matrix: `clean scalafmtCheckAll scalafmtSbtCheck test benchmarks/Jmh/compile` | Pass: all 1,043 tests across 12 projects, formatting checks, completed artifacts, adversarial suites, and JMH compilation. |
| Short JMH runtime snapshot capture | Pass: 94,479.436 ops/s with one 200 ms warmup and measurement; indicative only. |
| Reflection, permit/bridge, cast, and diff inspection | Pass: no production/benchmark reflection, no singleton permit machinery or bridge, only checked semantic strengthening casts, and no whitespace error. |
| OpenSpec strict validation | Pass: the change and all 17 repository specifications validate. |
| Corgi status/source/traceability | Pass at planning revision `sha256:f7e50395afcf0531965ac1859002ce18e1163103f0e74dd96941ce2eb1f967d4`, source digest `sha256:9c8d459b6066675a3128ef4974967a1f199bbd3b0e4871179110e0fe57337880`, and traceability digest `sha256:31952a7384c61bba43d4c9498a68703047d3a93902c05c7e328122087bae5dc2`. |

The structured automated review covered requirement and scenario coverage,
code quality, functional behavior, architecture, external-data security,
compatibility, Java usability, and performance. Its findings array is empty.
The review changed no file, no finding was human-triaged, and it is not
canonical whole-change Verify or Human Review.
