---
type: delivery
updated: 2026-09-04
rfc: RFC-0005-simplify-post-trust-boundary
slice: S-05-remove-java-api-compatibility
change: remove-java-api-compatibility
status: archived
archived: 2026-09-04
evidence_manifest: sha256:60fca10695ecec821f321730ede792e35d7310ca94e497504462a719c001b84b
source_digest: sha256:d2278178847daa4d028e46f160af897ea1d2ebde0ad37b0bf9c480d5b5faa853
---

# RFC-0005-simplify-post-trust-boundary/S-05-remove-java-api-compatibility

## Outcome
Complete the simplification enabled by RFC-0004 now that constructor secrecy and reflective access are no longer
treated as domain authority. Remove obsolete Java-build and reflection-migration scaffolding, make actual-execution
transition results total, express deterministic replay ordering with typed values instead of encoded strings, avoid
recomputing derived execution state, and replace repetitive derived-result class machinery with direct Scala 3 sums
and products where construction establishes no additional semantic authority. Make Scala 3 the sole supported source
API, remove Java-specific construction and compatibility machinery from quantities, reference data, order/scenario,
execution, and codecs, and retain Java interoperability only where the JVM platform or an external dependency requires
it.

The resulting code SHALL be smaller and easier for one maintainer to read while preserving exact arithmetic,
dimension and grid safety, execution identity and provenance, validation, deterministic replay, exposure,
uncertainty, and anomaly semantics.

## Boundary Delivered
This amendment applies the cooperative in-process model from RFC-0004 to migration tooling, the runtime build, the
pure actual-execution lifecycle, quantity/reference-data construction, and order/scenario composition. It covers five
independently deliverable slices. Well-typed Scala 3 is the sole supported repository source API. Java source callers,
generated static forwarders, Java-facing constructor shapes, and JVM binary compatibility are not contracts. This does
not change the JDK platform baseline or turn Java object serialization into a supported data format.

The repository-wide production and benchmark reflection guard SHALL become a permanent zero-tolerance invariant. Its
implementation SHALL not retain an allowance baseline or migration accounting after the final allowance has been
removed. CI SHALL execute the guard. The runtime project SHALL use the repository's normal mixed-source settings now
that it has no Java-owned construction bridge; stale bridge-specific compilation order and duplicated compiler
configuration SHALL be removed.

Command transitions SHALL model successful classifications and rejected inputs as mutually exclusive alternatives.
A rejected transition SHALL always carry non-empty typed violations, while an applied, idempotent, or conflicting
transition SHALL not expose an optional violation channel. Public lifecycle operations SHALL consume this total result
without unchecked extraction.

Authoritative replay order SHALL be represented by owner-local typed ordering values composed from the existing
source, account, stream, position, event, fill, and continuation identities. Concatenated delimiter strings SHALL NOT
serve as composite comparison keys. Ordering SHALL remain deterministic when identity text contains punctuation that
could collide under string concatenation, and equivalent authoritative evidence sets SHALL retain delivery-order
independence.

One execution observation SHALL derive the effective-fill ledger once and reuse that value for exposure and anomaly
derivation. This is a pure data-flow simplification, not a mutable cache, and SHALL preserve the independently callable
owner operations needed by focused tests or other domain consumers.

Within `trading-execution-lifecycle`, closed derived alternatives such as submission knowledge, cancellation
knowledge, effective-fill classification, and transition results MAY become Scala 3 enums or equivalent direct sum
types. Structurally valid derived evidence, anomaly, and observation products MAY use generated structural equality
and product support. Such conversions SHALL remove hand-written equality, constructor forwarding, or tag plumbing only
where the owning derivation already establishes every semantic predicate.

Scala source shapes and exhaustiveness patterns for these derived outputs MAY change. Java source shape and JVM binary
signatures are explicitly not compatibility commitments. Their domain meaning, public field information, closed
alternatives, structural equality, and Java-serialization rejection SHALL remain observable.

Quantity and reference-data APIs SHALL accept already refined Scala values at trusted construction points without
revalidating them solely because their representation erases for Java callers. Raw checked factories with no
production Scala consumer and whose only purpose is a Java/JVM alternative to an existing refinement SHALL be removed.
External-data reconstruction SHALL continue to refine raw values before invoking trusted construction. Stable
`AssetId`, `GridId`, and `GridVersion` values SHALL move from hand-written Java implementations to Scala-owned checked
values while retaining their precise expected failures, value equality, hashing, display, and serialization rejection.

Reference-data commit and transition observations SHALL use direct Scala sums and products where construction conveys
no authority consumed by a later transition. Catalog state, delta, lineage, revision, conflict, and non-empty
invariants SHALL remain owned and checked by the catalog model. Changing a generated JVM class shape SHALL not weaken
those semantic checks.

Order/scenario composition SHALL rely on its path-dependent Scala evidence and resolution types instead of additional
`Any`-based hooks whose sole purpose is rejecting erased Java calls. Construction from an already non-empty
`MatchedSlices` SHALL be direct; construction from an ordinary vector SHALL retain its typed empty failure. Activation
verification, pricing resolution, same-shape semantic association, scenario identity, and boundary-codec
reconstruction SHALL remain checked by their current owners.

Ordinary-Java positive and negative API fixtures and their dynamic compiler/classloader harnesses SHALL be removed.
Completed-artifact Scala clients, negative Scala compiler fixtures, dependency-boundary checks, semantic tests, and
external wire/null cases SHALL remain. Runtime null checks MAY be removed only as a local consequence of replacing a
Java-only adapter or duplicated erased-input path; this RFC does not establish a repository-wide assumption that every
JVM reference is non-null.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-016 | automated | file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FOrder.scala#sha256:ca0ada236fa5c6fe9d18de097acb1e1e7233617efabb1a6c634b95a5c8441028; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-compiler%2Fnegative%2FAssociatedEvidenceShapes.scala#sha256:05a1c73381da421cc7f41315985e90c0be411916d77a3ef067bea3c67c8f647b; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-compiler%2Fpositive%2FSameShapeReplayClient.scala#sha256:624fc0b6450c005fa086517955a2a5d93c7d46a14103ac4525a9f8994cd29bb4; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FOrderScenarioSuite.scala#sha256:d0a119529bb76efb91eebd2c626c4666ae72fd41002f856c5bc8830f0053357f | PASS |
| AC-017 | automated | file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FScenario.scala#sha256:826cbe189394f1ea158621d9b491a2879460216fab71f0c212265a8421d9ac05; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FOrderScenarioSuite.scala#sha256:d0a119529bb76efb91eebd2c626c4666ae72fd41002f856c5bc8830f0053357f; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2FFeePolicyIntegrationSuite.scala#sha256:4f2d30ba78997268ac18b7cb82c573cc256c68a7524189af6211bf8a4065af28; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordSuite.scala#sha256:50efb21dc9b03963a44df0c4b74204770f98c70af3a1f7c8c6b6d8a5ee3cf5cc | PASS |
| AC-018 | automated | file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FReferenceDataCompilerBoundarySuite.scala#sha256:7f47be784c51936201eab814ac885dba2bd9c88d8b4256cf1e75d0101bc4a56d; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:003086363b15a4ddc948d6ef1d2d253416289abb703acbf688e90c31fe785e75; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:127fa27cc4942e1c04534dcc924669f32d706e2f57754ad3f271ea2e950ffe8c; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:1530e7e013723ce5f942c9da1c5b399026d63d0e5e57a66009a06f204f754699; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Feconomics-compiler%2Fpositive%2FCompleteCompositionClient.scala#sha256:a42b3904705cb5b58f9f399bc9acf5dee174f4d9dd187e77eef6879cb75466fe | PASS |
| AC-019 | automated | file:README.md#sha256:121f5dbabf2598bdbbdd9d26904d609e16f6c4246240aa628ad0e636570c5189; file:docs%2Fdesign-principles.md#sha256:4e50ecda0495052c935efc444d8c89192a4aa614da91476db401a41769cd2ae9; file:build.sbt#sha256:790e4ae7fa4ab9334b36c0273bc4d44d02d59cac352b50f5f0aeaadc026f5db8; file:quantities%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fquantity%2Fruntime%2FJavaSerializationPolicySuite.scala#sha256:f7e61e3e34ae37a3e55d699d1f735fa043d3caf29d96e8935d5ed690381f1d86; file:openspec%2Fchanges%2Fremove-java-api-compatibility%2Fspecs%2Fquantity-grid-projection%2Fspec.md#sha256:ee856b77fd9bfeefac0866a13e996ea6e4376483d4320b0725277ff817ada009; file:openspec%2Fchanges%2Fremove-java-api-compatibility%2Fspecs%2Freference-data-identity%2Fspec.md#sha256:060ef27537c5d25c93fff9c6f896becf58f4bef74e7bf03feb6d67c0fa2c9447; file:openspec%2Fchanges%2Fremove-java-api-compatibility%2Fspecs%2Forder-scenarios%2Fspec.md#sha256:9bf7b7837b76c8dd84fa0f017a120a73bf4d79cdbc7f92de15b0884190a647ce; file:openspec%2Fchanges%2Fremove-java-api-compatibility%2Fspecs%2Factual-execution-lifecycle%2Fspec.md#sha256:d55f4b8613b6d9c3e1102d31dee2ad9e8b19c4a528d65670f5cf316f53443d9b; file:openspec%2Fchanges%2Fremove-java-api-compatibility%2Fspecs%2Fversioned-boundary-codecs%2Fspec.md#sha256:48842ea46fd94b0212dc0f80365016f556ffb5b378596be9e18d02d7e434c4e0 | PASS |

## Implementation
- Task Group 1: `0aa35e63b443bca111bba0a0cb156f891f9ec81a`
- Task Group 2: `f98ef4f37e4597858c9c1d105be4aa9e002b2699`
- Task Group 3: `2754b2e91ebc5f9f29fc95e51793d01f4d71681a`
- Final HEAD: `2754b2e91ebc5f9f29fc95e51793d01f4d71681a`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — The approved slice removes unsupported ordinary-Java source compatibility and simplifies Scala construction without adding or changing a runnable user, UI, API, CLI, or backend path requiring real-user QA.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0005-simplify-post-trust-boundary`
- `openspec/changes/archive/2026-09-04-remove-java-api-compatibility`
- `openspec/changes/archive/2026-09-04-remove-java-api-compatibility/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/44
