# Task Group 9 — Boundary codec and catalog replay static construction

## Implementation

Record types, schema versions, decode limits, structured wire paths, non-empty
wire violations, decoded grid-coordinate packages, order/scenario refinement
failures, catalog journal entries, and catalog replay results now use direct
owner-defined construction. All method-handle descriptors, private lookups,
invocations, recovery casts, and warning suppressions were removed from the
boundary-codec production module.

The existing checked paths remain the only supported route from external data:
strict JSON parsing and duplicate detection, bounded decoding, exact numeric and
identifier refinements, explicit V1 envelope dispatch, canonical JSON/JCS
rendering, coherent snapshot resolution, dependent grid reconstruction, stable
path/error ordering, order/scenario associated-shape validation, catalog batch
publication, and fresh-lineage replay are unchanged.

Constructor-modifier and deliberate direct-construction assertions were
removed from codec unit and completed-artifact fixtures. Tests continue to
cover malformed and incoherent external data, off-grid and cross-grid values,
authority/dependency escapes, non-empty diagnostics, equality, serialization,
golden vectors, schema compatibility, catalog no-op/conflict behavior, and
atomic batch replay. A new ordinary Java client exercises checked record type,
schema version, decode limit, and wire violation factories.

The repository reflection baseline now contains only its explanatory header;
the guard reports no production or benchmark sites.

## Verification and automated review

| Check | Result |
| --- | --- |
| `boundaryCodecs/test` | Pass: all 94 codec, law, property, golden-vector, strict-input, limit, reconstruction, compatibility, and replay tests. |
| Completed-artifact boundary-codec suite | Pass: all 17 classpath, packaged API, ordinary Scala/Java client, dependency, authority, and malformed-boundary checks. |
| `benchmarks/Jmh/compile` | Pass: benchmark sources and JMH generated sources compile. |
| Short JMH run, parsed batch 1024, one 200 ms warmup/measurement | Pass: 18,931.490 ops/s. Indicative only; the test matrix supplies deterministic semantic evidence. |
| `tools/check-in-process-reflection.sh` | Pass: no production or benchmark sites. |
| Production scan and `git diff --check` | Pass: no method handles, private lookups, invocation recovery casts, warning suppressions, or whitespace errors remain. |

The structured review covered checked external-data ownership, exact
refinements, canonical bytes, deterministic error ordering, snapshot and grid
coherence, associated evidence, journal publication and replay, batch
atomicity, V1 compatibility, dependency scope, performance, Java usability,
and fixture replacement. No critical or important finding remains. The review
changed no file, no finding was human-triaged, and it is not canonical
whole-change Verify or Human Review.
