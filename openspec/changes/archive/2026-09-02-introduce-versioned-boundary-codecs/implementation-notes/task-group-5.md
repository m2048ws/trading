# Task Group 5 — Grid-coordinate record families

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:153cb27d19c92cd66905fe2689b978b71155c2e499fbcffed534b0757f167ba5`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `3e16373aa40d2fc589a08233ea6a1fef7f132e93`

Group 5 implements only the general and asset-qualified V1 grid-coordinate families mapped to AC-018. Catalog
journals, instrument definitions, orders, scenarios, cross-family limit work, checked-in schemas/goldens, and final
verification remain owned by later Task Groups.

## Exact records and dependent reconstruction

`GeneralGridCoordinateRecord.V1` contains exactly a full `GridIdentity` and signed arbitrary-precision coordinate.
`AssetGridCoordinateRecord.V1` adds exactly one `AssetId`. Both immutable products reject Java serialization and use
the Group 4 exact primitive/envelope kernel. Their packers accept only a `GridHandle` and a `GridQuantity` on that
handle's path-dependent grid (plus the owning asset where applicable), then copy stable identity and coordinate without
catalog lookup, quantum duplication, projection, or quantization.

The JVM-private `DecodedGridQuantity` and `DecodedAssetGridQuantity` constructors retain canonical dependent
relationships among dimension/asset, grid, and reconstructed value. General reconstruction resolves the recorded
dimension before the dimension-scoped full grid key. Asset reconstruction resolves the asset, rejects a recorded
dimension mismatch, and only then performs the full-grid lookup. Typed failures distinguish unknown dimension, unknown
asset, asset-dimension mismatch, unknown full grid, and otherwise impossible snapshot/lineage inconsistency. Public
failure products reject nulls and false mismatch values.

Both families expose all-valid-or-indexed-errors batch reconstruction over one explicitly supplied immutable snapshot.
Traversal evaluates every record, preserves success order, reports failures in input order, returns no partial-success
channel, and performs only the snapshot's direct map lookups plus linear batch work. No live catalog or registry is
captured.

## Compiler, exactness, and boundary evidence

Runtime and property coverage exercises canonical envelopes, reordered input, typed field paths, compound signed
dimensions, negative and 500/600-digit coordinates, historical grid versions with different immutable quanta, unknown
dimensions/assets/full versions, asset meaning drift, dependent coordinate-to-quantity coherence, ordered batch
success, and accumulated indexed failures. Schema inspection confirms neither family copies a quantum.

Completed-JAR positive fixtures compile exact general/asset packing and both dependent result projections. Negative
fixtures reject arbitrary `Quantity` packing, cross-grid values, direct dependent-package construction, and every old
`Packed*`, `Resolved*`, and registry escape name. Packaged inspection confirms the result constructors are JVM-private
and the retired names are absent.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch boundaryCodecs/test` | pass | All 52 strict-kernel and grid-coordinate unit/property tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 166 completed-JAR/compiler/adversarial tests pass, including eight codec-boundary tests. |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test` | pass | Both formatting gates, clean dependency-order compilation, and all 973 repository tests pass. |
| Exact and historical reconstruction | pass | Compound dimensions, signed huge coordinates, exact embedded quantities, asset drift, and version-1 selection beside version 2 are covered. |
| Compiler and packaged API boundary | pass | Exact dependent calls compile; arbitrary/off-grid calls, private construction, and retired names fail; completed constructors are private. |
| Planning integrity | pass | All 12 strict readiness checks pass at the frozen planning/source/traceability revisions. |

The first automated Task Group review found and remediated forgeable null/false-mismatch failure products. The final
pass checked Run/Group identity, AC-018 scope, functional behavior, exactness, dependent typing, lookup staging,
batch semantics, architecture, complexity/security applicability, compiler/JAR boundaries, and evidence with no
findings. It changed no file, human-triaged no finding, and is not canonical whole-change Verify or Human Review.
