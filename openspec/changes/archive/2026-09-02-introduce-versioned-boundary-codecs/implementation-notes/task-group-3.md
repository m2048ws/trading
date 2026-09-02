# Task Group 3 — Strict JSON, paths, limits, and schema algebra

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:023ed559ae231763b5ba72412427988f2c3bf7990476e2f9e8d7e1714a191a1e`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `a658d9006fb3eb8d4d46faa15b72df43fd24ab17`

Group 3 implements the strict parsing, diagnostic, operational-limit, canonical-rendering, and shared schema-algebra
foundation mapped to AC-017. Exact primitive encodings, envelopes, version dispatch, concrete V1 record families, and
public encode/decode/reconstruction operations remain owned by later Task Groups.

## Public diagnostic and limit foundation

The boundary now owns immutable structured field/index paths, syntax coordinates, stable validation-stage ordinals,
named decode limits, typed encode/decode/limit/configuration violations, and a deterministic non-empty
`WireViolations` aggregate. `WirePath`, `WireViolations`, and `DecodeLimits` use JVM-private constructors reached only
through checked companions, so completed-artifact clients cannot forge invalid values through generated constructors.

`DecodeLimits.default` fixes the documented character, UTF-8 byte, nesting, batch, object, array, string/identifier,
integer-digit, dimension-factor, catalog-command, scenario-slice, and market-conversion limits. `create` accumulates
all independent nonpositive settings and then all dependent containment errors. Strict parsing checks exact Unicode
code-point and UTF-8 byte counts in project code before constructing a parser.

## Strict JSON, canonical rendering, and schema ownership

One package-private Jackson Core 3.2 adapter disables every permissive `JsonReadFeature`, enables strict duplicate
detection, retains raw number spellings and parser locations, and configures stream constraints beyond the public
project-owned thresholds as defense in depth. Expected parser failures become typed violations; parser objects,
exceptions, and mutation remain confined to the adapter. The resulting ordered AST is immutable and package-private.

The package-private renderer implements the restricted RFC 8785 surface used by this change: well-formed Unicode,
unsigned UTF-16 member ordering, minimal required escaping, canonical integer literals, and whitespace-free output.
Its vectors agree with the independent test-only Java JCS oracle. Unsupported number spellings, malformed Unicode, and
duplicate members remain typed encoding failures.

The shared package-private `WireSchema[A]` algebra owns invariant mapping, checked refinement, fields, associative
products, closed tagged coproducts, vectors/traversal, path propagation, operational-limit selection, and applicative
error accumulation. Its single inspectable shape also renders stable-URN Draft 2020-12 documents with local `$ref`,
closed objects/cases, and no operational-limit keywords. NetworkNT validates the generated document against the
Draft 2020-12 meta-schema and checks codec/schema agreement with remote resource fetching disabled.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch boundaryCodecs/test` | pass | All 27 unit, law/property, strict-parser, JCS-oracle, generated-schema, limits, diagnostics, dependency, and leakage tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 164 completed-JAR/compiler/adversarial tests pass, including six codec artifact/API boundary tests. |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test` | pass | Both formatting checks and clean dependency-order compilation pass; all 946 repository tests pass. |
| Production/test dependency reports | pass | Production adds no validator/oracle dependency; NetworkNT, JCS, ScalaCheck, and MUnit ScalaCheck remain test-only. |
| Packaged JAR and bytecode inspection | pass | Public foundations expose no Cats/Jackson/validator/oracle types; parser internals alone bind Jackson, internal schema/AST names are Scala-inaccessible, and checked constructors are JVM-private. |
| Planning integrity | pass | Strict readiness passes all 12 checks at the frozen planning/source/traceability revisions. |

The automated Task Group review checked exact Run/Group identity, AC-017 scope, functional behavior, code quality,
architecture, public API and completed-JAR isolation, parser security and limits, canonicalization, schema/codec
agreement, dependency containment, and evidence. It found and remediated one misleading invalid-definition diagnostic,
then returned no findings on the final pass. The final pass changed no file, human-triaged no finding, and is not
canonical whole-change Verify or Human Review.
