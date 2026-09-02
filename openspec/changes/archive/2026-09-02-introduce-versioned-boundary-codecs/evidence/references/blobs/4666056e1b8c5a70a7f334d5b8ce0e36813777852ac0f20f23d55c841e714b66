# Task Group 4 — Exact primitives, envelope, and version dispatch

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:a0780f834d603904863513cb479428326c6f14b6ad1da856a49d874c221af9e4`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `1c5769046148f4bd8b42a8bba9b07539bccddff2`

Group 4 implements the reusable exact primitive and family-specific envelope kernel mapped to AC-017. Concrete V1
record families, snapshot/instrument reconstruction, batch operations, and public family encode/decode operations
remain owned by later Task Groups.

## Exact primitive and domain reconstruction kernel

Canonical signed and positive integers are JSON strings. The decoder checks the selected digit limit before `BigInt`
construction and rejects signs, leading zeroes, whitespace, decimal/exponent forms, and non-decimal spellings. Rational
records require a canonical numerator, positive denominator, reduced pair, and `0/1`; reconstruction invokes
`Rational` and verifies its public canonical projections rather than accepting normalization aliases.

Stable asset, grid, instrument, underlying, and dimension-atom identifiers pass through their owning checked
constructors and preserve the exact well-formed Unicode string without trimming, normalization, or case folding.
Dimensions traverse factor records, accumulate indexed zero/duplicate/order failures, use authoritative JVM UTF-16
ordering, call `DimKey`, and compare its exact public powers with the supplied vector. Full `GridIdentity` retains the
canonical dimension, stable grid ID, and positive domain-bounded `GridVersion` without parsing `toString` output.

## Envelope and version ownership

Public checked `RecordType` and `SchemaVersion` values have inaccessible JVM constructors and reject Java object
serialization. The package-private `EnvelopeCodec[A]` owns one record family, an explicit version-to-schema reader
table, one current writer version, and a declared set of known record types. It parses the exact closed
`{payload, recordType, schemaVersion}` product, dispatches only supported pairs, retains typed missing/invalid/unknown/
mismatched/unsupported header failures, and never guesses from payload shape or falls back to another version.

The shared schema shape now represents unconstrained header payloads and exact string/integer constants. Generated
Draft 2020-12 envelopes therefore close all objects and fix the family/version pair while keeping operational limits
out of the mathematical schema. Reordered/whitespace input canonicalizes to one writer form; grid version remains an
exact decimal string inside the payload while schema version remains the small JSON integer framing field.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch boundaryCodecs/test` | pass | All 43 strict-parser, canonical-rendering, exact primitive/property, Unicode, dimension/grid, envelope/dispatch, generated-schema, diagnostics, limit, dependency, and leakage tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 164 completed-JAR/compiler/adversarial tests pass, including the expanded codec public/internal boundary fixtures. |
| Formatting checks plus `sbt -batch clean test` | pass | Both Scala/SBT formatting checks and clean dependency-order compilation pass; all 962 repository tests pass. |
| Generated envelope schema | pass | NetworkNT validates the document against Draft 2020-12 and accepts/rejects canonical type/version fixtures with remote fetching disabled. |
| Exact/canonical fixtures | pass | Huge integers, reduced-rational properties, malformed aliases, exact Unicode/normalization/case behavior, authoritative UTF-16 factor ordering, member variation, malformed headers, and schema/grid version separation are covered. |
| Packaged JAR and bytecode inspection | pass | Public values/diagnostics expose no Cats/Jackson/validator/oracle types; exact/schema/envelope kernels remain Scala-inaccessible; checked value constructors are JVM-private. |
| Planning integrity | pass | Strict readiness passes all 12 checks at the frozen planning/source/traceability revisions. |

The automated Task Group review checked exact Run/Group identity, AC-017 scope, exact numeric and Unicode behavior,
domain-constructor ownership, normalization resistance, dimension/grid identity, envelope dispatch and version
separation, generated-schema closure, public dependency boundaries, bounded allocation, code quality, and evidence. It
returned no findings, changed no file, human-triaged no finding, and is not canonical whole-change Verify or Human
Review.
