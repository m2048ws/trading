# Task Group 6 — Catalog journal and pure replay

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:efa2ac9516475985f0f3afa6f0ef6d3feed69313673e6c0584f3e2478b1cf6af`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `3d3dd8aa25343d7efbc86b4b26706222311548dd`

Group 6 implements only the frozen catalog-journal V1 representation and deterministic pure replay mapped to AC-019.
Instrument definitions, orders, scenarios, cross-family robustness/benchmarks, checked-in schemas/goldens,
documentation, and final verification remain owned by later Task Groups.

## Frozen publication record and structural decoding

`CatalogJournalEntry.V1` is an immutable Java-serialization-rejecting product of one positive successor
`CatalogRevision` and one ordered non-empty `CatalogBatch`. Its codec freezes exactly three tagged V1 alternatives:
dimension registration with full `DimKey`, asset registration with complete `AssetDefinition`, and grid registration
with full `GridIdentity` plus exact positive rational quantum. A future domain command is therefore not silently
admitted to V1. The supported writer accepts only the submitted batch and `CatalogCommit.Published`; there is no path
from failure or `Unchanged`.

Canonical envelopes reuse the strict Group 3/4 kernel and preserve command order. History parsing checks the batch
record limit once before entry work, decodes every within-limit independent input, assigns stable zero-based record
indices, and accumulates all wire violations in deterministic record/path order before exposing any replay operation.
Malformed or future command alternatives fail structurally and cannot be skipped or replaced.

## Pure fresh-lineage replay

`CatalogReplay` accepts an explicit immutable `CatalogState` and ordered V1 vector. It first requires revision zero and
empty dimension/asset/grid indexes, then checks each recorded revision against exactly the last successful revision
plus one before delegating the batch to the normative pure `CatalogModel.commit`. `Published` must report the expected
state and snapshot revision; catalog violations and `Unchanged` remain typed failures. Replay never sorts history,
captures a live catalog, selects a hidden snapshot, or performs an effect.

Success returns one constructor-private `CatalogReplayResult` retaining the final state and projecting its snapshot and
revision. Failures retain entry index, expected/recorded/last-successful revision context, complete ordered catalog
violations where applicable, and no successful partial state. Empty history returns the exact supplied fresh state;
prefix replay is the historical-state mechanism, and independent fresh roots reproduce stable definitions while
issuing distinct lineage authority.

## Model, compiler, and authority evidence

Runtime and property tests exercise empty and published histories, multi-command and valid duplicate batches, exact
1,000-digit quanta, all independent command permutations, gaps, repeats, conflicts, no-op entries, later-entry
suppression, historical grid versions, prefix results, and fresh-lineage separation. Structural fixtures accumulate
invalid revisions, empty batches, invalid quanta, and unknown commands across indexed records before state checks.

Completed-JAR positive fixtures compile publication retention, history decode, explicit-fresh-state replay, and final
projections. Negative fixtures reject private construction, `Unchanged` input, state encoding, root input, entry
root/state/snapshot/lineage/timestamp/checkpoint/activation/delisting fields, repository/checkpoint APIs, and both
ordinary and same-package Scala attempts to invoke hidden entry/result factories. Packaged inspection confirms JVM-
private record/result constructors and the absence of durability shortcuts.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| Focused journal/model/property tests | pass | All 9 catalog-journal runtime/property tests pass. |
| `sbt -batch boundaryCodecs/test` | pass | All 61 strict-kernel, coordinate, journal, and replay tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 168 completed-JAR/compiler/adversarial tests pass, including ten codec-boundary tests. |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test` | pass | Both formatting gates, clean dependency-order compilation, and all 984 repository tests pass. |
| Exact replay and failure semantics | pass | Model equivalence, huge exact values, prefixes, versions, ordering, duplicates, gaps/repeats, conflicts, no-ops, and non-fresh input are covered. |
| Compiler and packaged API boundary | pass | Supported publication/replay calls compile; construction, authority, state, durability, and same-package escapes fail. |
| Planning integrity | pass | All 12 strict readiness checks pass at the frozen planning/source/traceability revisions. |

The first automated Task Group review found that package-private entry/result factories admitted a same-package Scala
construction bypass. The remediation made entry construction object-private and moved result construction behind a
replay-owned cached private method handle, then added the matching compiler attack fixture. The final pass checked
Run/Group identity, AC-019 scope, functional behavior, deterministic diagnostics/order, exactness, architecture,
resource/security applicability, compiler/JAR authority boundaries, and evidence with no findings. It changed no file,
human-triaged no finding, and is not canonical whole-change Verify or Human Review.
