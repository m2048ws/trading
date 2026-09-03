# Boundary-codec Apply evidence

This is the Task Group 12 implementation handoff for
`RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`. It records checks run on 2026-09-02 after
acknowledged Task Group 11 commit `1f0d324a552b9d5be5bc6a43bade4ac5676e0b56`. It is not canonical Corgi Verify,
Human Review, Human QA, Archive, or a storage-durability claim.

## Contract identity

- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Accepted RFC revision: `7b6f7a58f4dcbb8fb4bbdf3a8ba74ba66f222cce`
- RFC digest: `sha256:a00c0cf1e3a4440bfce7dc828ad4ac119414f10d72e8d8c52c613403ef5977e2`
- Source digest: `sha256:625809aa552c0c3f4efd1a8e2d9982c76414f82ce405674ce6f91b475807cceb`
- Traceability digest: `sha256:c7545d03bf6039543fee8aab7947aa9c10c46923f8e0ee9cc716a12ab66ac44f`

## Validation matrix

| Check | Result |
| --- | --- |
| `sbt -batch clean compile` | Passed in dependency order for all production artifacts |
| `sbt -batch test benchmarks/Jmh/compile` | Passed: 1,024 tests and JMH compilation |
| Boundary codec tests | 95 passed, including schema, golden, model, property, fuzz, limits, and serialization |
| Completed-JAR/adversarial tests | 174 passed, including 16 boundary-codec compiler tests |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | Passed |
| Compatibility resource regeneration and JSON parsing | Passed byte-for-byte for seven schemas, goldens, and invalid fixtures |
| `corgispec ready introduce-versioned-boundary-codecs --strict --json` | Ready; all 12 deterministic checks passed |
| Cross-artifact semantic readiness review | No errors or warnings; no file changed |

The 1,024 tests comprise 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument economics, 40
risk, 7 order model, 16 execution scenario, 95 boundary codec, 38 fee policy/integration, and 174 completed-JAR/compiler/
adversarial tests.

## Focused benchmark smoke

JMH 1.37 ran on OpenJDK 26.0.2 with no VM arguments, one thread, one fork, one 100 ms warmup, and one 100 ms
measurement. The workload was a 1,024-record `general-grid-coordinate-v1` batch with 351-byte payloads and a 128-digit
coordinate. Directional throughput was:

| Path | ops/ms |
| --- | ---: |
| Capture one snapshot outside codecs | 63.229 |
| Decode and reconstruct batch | 0.016 |
| Parse JSON | 3.715 |
| Reconstruct parsed batch | 18.195 |
| Render JSON | 31.675 |

These smoke values confirm that parsing, rendering, one external snapshot capture, pure lookup/reconstruction, and the
combined path remain separately measurable; they are not reusable performance baselines.

## Packaged boundary audit

The production dependency report contains only the five owning domain artifacts, Scala, Cats Core 2.13.0, Jackson Core
3.2.2, and their Cats/Scala transitive dependencies. NetworkNT 3.0.7, Java JSON Canonicalization 1.1, MUnit,
ScalaCheck, and NetworkNT's transitive Databind dependencies occur only in test scope. Circe, Jackson's Scala module,
Cats Effect, FS2, application/runtime artifacts, persistence/network clients, and telemetry are absent from the codec
production classpath.

Completed-JAR and `javap -public` inspection found only family-specific codec/domain APIs and typed result sums; parser
ASTs, Jackson types, Cats validation containers, test validator/oracle types, effects, live catalogs, generic
`Any` registries, and runtime resources do not appear in supported public signatures. The JAR contains exactly the
seven V1 schema resources.

Production-source inspection found no floating conversion or raw-number precision narrowing, live snapshot selection,
monitor/atomic coordination, file/network operation, repository/checkpoint implementation, or serialized authority.
Production codec construction is statically callable and contains no reflective construction or invocation-recovery
casts. No cast selects a record alternative or makes incompatible domain evidence fit.

## Scope and gate separation

RFC AC-017 through AC-020, both delta capabilities, the proposal/design, all 12 ordered Task Groups, source binding, and
traceability remain aligned. The delivered record families remain grid coordinates, published catalog journal entries,
instrument definitions, immutable orders, hypothetical scenarios, and hypothetical round trips. Storage, transport,
atomic publication plus journaling, trusted checkpoints, executed trades, lifecycle events, fees, PnL, risk decisions,
market-data feeds, and runtime interpreters remain outside this Slice.

The next lifecycle operation after this Task Group is separate canonical Verify against the acknowledged exact commit.
Human whole-change Review, Human QA where required, and Archive remain later gates.
