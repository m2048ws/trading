# Task Group 2 — Boundary-codec module and dependency direction

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:15634f09c82654189a45cbfc9cffdd08b25c44209f553d4dbe983b1b04cb47f3`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `bc3d2c18c97aa7d8390112f8fb5405ba474dd036`

Group 2 implements only the physical artifact, package/resource roots, dependency scopes, build/test wiring, and
compiler guards mapped to AC-017. It does not claim the paths/errors/limits/schema algebra, exact primitives/envelope,
or any concrete V1 record-family API owned by later Task Groups.

## Artifact and dependency boundary

The root now aggregates and tests `boundaryCodecs` in dependency order after execution scenario. The project lives in
`boundary-codecs/`, publishes `trading-boundary-codecs`, exports a completed main JAR, and establishes the documented
`trading.codec` package root. Production depends directly on quantities, reference data, instrument economics, order
model, and execution scenario, plus Cats Core `2.13.0` for later pure validation/traversal and Jackson Core `3.2.2` for
the later package-private strict streaming adapter.

NetworkNT JSON Schema Validator `3.0.7` and the RFC 8785-listed Java JSON Canonicalization implementation `1.1` are
independently pinned in test scope. The production external classpath contains Scala, Cats Core/Kernel, Algebra, and
Jackson Core only; it contains no NetworkNT, JCS oracle, Jackson Databind/Scala module, Circe, fee policy, risk,
application, runtime, Cats Effect, FS2, persistence/client, telemetry, or benchmark artifact. The test dependency tree
confirms NetworkNT's Jackson Databind/YAML mechanisms remain confined to `Test`.

The completed codec JAR contains only its documented package object and production schema-resource root at this
checkpoint. `javap -public` exposes no codec method or field, and bytecode inspection rejects parser, validator,
reflection, effect, or stream vocabulary. Schema and golden resource roots are present for later owning groups without
presenting any future schema or record as already implemented.

## Compiler and artifact evidence

`BoundaryCodecCompilerBoundarySuite` compiles a downstream client using the completed codec production classpath and
the five intended domain owners, Cats validation, and Jackson Core. Its negative companion rejects fee policy, risk,
application, runtime, Cats Effect, FS2, persistence, telemetry, Jackson Databind/Scala, Circe, NetworkNT, and the JCS
oracle. Existing quantities, reference-data, instrument-economics, order-model, execution-scenario, fee-policy, and
risk completed-artifact fixtures each reject `trading.codec`, proving every lower/pure artifact remains independent.

During focused compilation the tests corrected two dependency-version API assumptions: NetworkNT 3.x exposes
`SchemaRegistry`, and Jackson Core 3.2 places `JsonFactory` under `tools.jackson.core.json`. The final compile-only
fixtures bind the actual selected mechanisms without exposing either in a public codec API.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass | All Scala, SBT, and configured Markdown sources are formatted. |
| `sbt -batch boundaryCodecs/test` | pass | One test proves both independent test-only mechanisms are available in `Test`. |
| `sbt -batch adversarialBoundary/test` | pass | All 162 completed-JAR/compiler/adversarial tests pass, including four new codec-boundary tests and seven strengthened reverse-import guards. |
| `sbt -batch clean test` | pass | Clean dependency-order compilation and 918 repository tests pass: 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument economics, 40 risk, 7 order model, 16 execution scenario, 1 boundary codec, 38 fee policy/integration, and 162 adversarial tests. |
| Production/test dependency reports | pass | Production excludes every downstream/effect/mapping/oracle concern; NetworkNT/JCS and their transitive validator mechanisms appear only in `Test`. |
| Packaged JAR and `javap -public` inspection | pass | The artifact contains only the package root and schema documentation resource; no future codec API or forbidden vocabulary is exposed. |
| Planning integrity | pass | Frozen proposal/design/specs/tasks/source/traceability paths are unchanged. |

The automated Task Group review checked Run/Group identity, AC-017 scope, code/build quality, dependency direction,
production/test containment, completed-JAR and public API boundaries, resource/documentation honesty, validation
evidence, and performance/security applicability with no findings. It changed no file, human-triaged no finding, and
is not canonical whole-change Verify or Human Review.
