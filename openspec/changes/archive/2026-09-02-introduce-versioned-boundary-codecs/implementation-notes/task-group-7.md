# Task Group 7 — Instrument-definition codec and assembly composition

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:2f9ff791b9ce45db65f86c40dc73584ddf770210b92fd063280fe6d5c8ccf480`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `e831e90e4fe4c2ae226c40a9d80b3f175ebb43ca`

Group 7 implements only the frozen instrument-definition V1 representation and its separate pure assembly composition
mapped to AC-018. Orders, scenarios, cross-family robustness/benchmarks, checked-in schemas/goldens, documentation,
and final verification remain owned by later Task Groups.

## Frozen publication record and structural decoding

`InstrumentDefinitionRecord.V1` is an immutable Java-serialization-rejecting product containing stable
`InstrumentId` and `UnderlyingId`, the four role `AssetId` values, full position-lot and price `GridIdentity` values,
and exact reduced base/quote payoff coefficients. It contains no catalog revision, snapshot, lineage, handle, authority,
venue, market, product-family, or trusted reconstructed instrument. Its frozen envelope record type is
`trading.instrument-definition` at schema version 1.

The package-private schema composes the exact Group 3/4 primitives into closed nested identity, role, listing, and
payoff products. Parsing is therefore purely structural and accumulates every independent syntax, identifier,
exact-number, and local-product violation deterministically before any catalog lookup. Successful parsing yields only
an `InstrumentDefinition`; canonical encoding is available from the definition or the stable V1 product, not from an
assembled `Instrument` or `InstrumentSpec`.

## Explicit snapshot assembly and batch semantics

Assembly is a separate pure operation that receives one explicit immutable `CatalogSnapshot`, delegates definition
resolution to the canonical `InstrumentAssembler.assemble`, and then calls `Instrument.fromSpec`. Public
`InstrumentDefinitionReconstructionFailure` alternatives distinguish codec violations from the complete ordered
`InstrumentAssemblyErrors`; malformed input cannot trigger assembly and catalog failures cannot be recast as wire
failures.

Family batch reconstruction accepts an ordered `Vector[String]` and one snapshot. It checks `maxBatchRecords` once
before record work, traverses each within-limit record exactly once, retains stable zero-based record indices, and
returns either every instrument in input order or a non-empty ordered collection of indexed codec/assembly failures.
There is no partial-success channel and no live catalog capture, revision selection, fallback lookup, or hidden
authority acquisition.

## Model, compiler, and authority evidence

Golden and property tests cover exact linear, inverse, and quanto definitions; canonical structural round trips;
non-reduced and non-canonical rationals; malformed identifiers; equal roles; empty payoff; missing assets and grids;
both grid-dimension conflicts; historical grid selection with a newer version present; ordered batch success/failure;
batch limits; and stable definitions replayed against distinct fresh lineages. Direct composition is compared with the
canonical assembler and instrument constructor.

Completed-JAR positive fixtures compile definition retention, decoding, explicit-snapshot assembly, atomic batching,
and stable projections. Negative fixtures reject encoding assembled/trusted values, passing a live root instead of a
snapshot, and record fields or projections for catalog revisions, lineages, snapshots, handles, instruments/specs,
markets, venues, and product families. Packaged inspection confirms the new record and fixtures are present without
admitting reverse dependencies or authority surfaces.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| Focused instrument-definition tests | pass | All 7 runtime/property tests pass. |
| `sbt -batch boundaryCodecs/test` | pass | All 68 strict-kernel, coordinate, journal/replay, and instrument-definition tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 170 completed-JAR/compiler/adversarial tests pass, including twelve codec-boundary tests. |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test` | pass | Both formatting gates, clean dependency-order compilation, and all 993 repository tests pass. |
| Exact reconstruction and batch semantics | pass | Linear/inverse/quanto, historical grids, direct model equivalence, cross-lineage failure, stable order, suppression, and atomic failure are covered. |
| Compiler and packaged API boundary | pass | Supported stable-record/snapshot calls compile; trusted values, roots, authority, revision, lineage, handle, venue, and market escapes fail. |
| Planning integrity | pass | All 12 strict readiness checks pass at the frozen planning/source/traceability revisions. |

The automated Task Group review checked Run/Group identity, AC-018 scope, frozen record shape, structural/assembly
staging, canonical assembler and `Instrument.fromSpec` reuse, exact and historical-grid behavior, stable atomic batch
semantics, typed diagnostics, authority exclusion, completed-JAR/dependency boundaries, linear bounded work, and the
validation evidence with no findings. It changed no file, human-triaged no finding, and is not canonical whole-change
Verify or Human Review.
