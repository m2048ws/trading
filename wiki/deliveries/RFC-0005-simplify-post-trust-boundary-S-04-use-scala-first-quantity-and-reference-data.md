---
type: delivery
updated: 2026-09-04
rfc: RFC-0005-simplify-post-trust-boundary
slice: S-04-use-scala-first-quantity-and-reference-data
change: use-scala-first-quantity-and-reference-data
status: archived
archived: 2026-09-04
evidence_manifest: sha256:95f7a7ae69b6481f23ea39097a3533e1ad73ee57563f054d6581379a78567432
source_digest: sha256:196d46b1fb5039303f50ba631ba482d2231ee6c737a0e055a83f3653f1e91314
---

# RFC-0005-simplify-post-trust-boundary/S-04-use-scala-first-quantity-and-reference-data

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
| AC-012 | automated | file:reference-data%2Fsrc%2Fmain%2Fscala%2Ftrading%2Freference%2FReferenceData.scala#sha256:79c70e90ceb39e6e4aa7fb85c57bf8b23d41f161ac03c1e58a63d83c3625fea5; file:reference-data%2Fsrc%2Ftest%2Fscala%2Ftrading%2Freference%2FReferenceDataSuite.scala#sha256:751355b84d090f36cdce812e80600ce077e5c68062aafab715a6e0ae71739749; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FReferenceDataRuntimeBoundarySuite.scala#sha256:c42d8355290ff5e374d29e3fdf38de811a0457d07d5ce5b028b16a41a7b8068f; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FReferenceDataCompilerBoundarySuite.scala#sha256:946521a3092cd7628122ba9d4862d5f57c9970b57bebc24fb5e6fc264396273e | PASS |
| AC-013 | automated | file:quantities%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fquantity%2FGridQuantity.scala#sha256:9227506bc20e1760091752453f59cef5af1f027cf48c88331798f74515efad79; file:reference-data%2Fsrc%2Fmain%2Fscala%2Ftrading%2Freference%2FReferenceData.scala#sha256:79c70e90ceb39e6e4aa7fb85c57bf8b23d41f161ac03c1e58a63d83c3625fea5; file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FExactPrimitives.scala#sha256:f1e65dfe26dc9b1ebff002a708f3d7d13b96998ba06b5dbdf4da16bb0596ad3a; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FCatalogJournalSuite.scala#sha256:8eff2f0274dffa9650e840b3b7826444bc9d404ef26e6bb2acdd524abe515473; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Freference-data-compiler%2Fnegative%2FRemovedStableGridFactory.scala#sha256:f02c4f3ebf31fe3674c580a0808b72c27305a8087803d9a77fd19f0a5b35f292 | PASS |
| AC-014 | automated | file:reference-data%2Fsrc%2Fmain%2Fscala%2Ftrading%2Freference%2FCatalog.scala#sha256:5a315285fe5a8b55e6f0b4b2415dafe9d37bd88f70d9409e157294a3f657182e; file:reference-data%2Fsrc%2Ftest%2Fscala%2Ftrading%2Freference%2FReferenceDataSuite.scala#sha256:751355b84d090f36cdce812e80600ce077e5c68062aafab715a6e0ae71739749; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Freference-data-compiler%2Fnegative%2FCatalogObservationConstruction.scala#sha256:f363634fe105c79e1402ae125879b50ad970995120f90511ecd8d1f9d83456c7; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Freference-data-compiler%2Fpositive%2FConcreteReferenceDataClient.scala#sha256:dfe436e245d61dad9c4140765f782eaab105f77c56f20c7dd9e9d120f668311e | PASS |
| AC-015 | automated | file:quantities%2FREADME.md#sha256:fb9237b259175100a05fc9eaa42406c036e29923ccaff41813a036e15b73c0a8; file:reference-data%2FREADME.md#sha256:a31b0e6b0945920c36e32833c499837a4fccb2c338b23fdba306d9e5a7f959da; file:openspec%2Fchanges%2Fuse-scala-first-quantity-and-reference-data%2Fspecs%2Fquantity-grid-projection%2Fspec.md#sha256:010c43893e2d6b8e2bdb95a82574612164866dace823976d5f2e4fb61201b922; file:openspec%2Fchanges%2Fuse-scala-first-quantity-and-reference-data%2Fspecs%2Fruntime-quantity-identity%2Fspec.md#sha256:1049e7e19ac27fb9ba8e320f25d6ebd0cdbecf8e6aa04e3b391de7ebe1326eea; file:openspec%2Fchanges%2Fuse-scala-first-quantity-and-reference-data%2Fspecs%2Freference-data-identity%2Fspec.md#sha256:63b573cbe94a3b47179a54a7c7682afd4304308b2f909e13176c904174592efb; file:openspec%2Fchanges%2Fuse-scala-first-quantity-and-reference-data%2Fspecs%2Freference-data-catalog%2Fspec.md#sha256:7cee3381a7a2bab1ca5f480ef0c62b0019f1b514310b5e26d77e1d5816d29118; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FConstructionAndProvenanceBoundarySuite.scala#sha256:72d01feb20a7c2bcdee3d5bdf3594751f849597459d84a2587c391a4554da53d; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FReferenceDataRuntimeBoundarySuite.scala#sha256:c42d8355290ff5e374d29e3fdf38de811a0457d07d5ce5b028b16a41a7b8068f | PASS |

## Implementation
- Task Group 1: `2b4440eb9f376c3110cf9921fbe53b72c5773f3d`
- Task Group 2: `d29337fb6b72f7cbc4e986e8137a808fc1240c1e`
- Task Group 3: `806a6b435614a23536f5f26a221469281f5253b6`
- Final HEAD: `806a6b435614a23536f5f26a221469281f5253b6`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human reviewer confirmed no runtime-facing QA path: this slice changes Scala-owned quantity/reference-data construction and pure catalog result representations, with no UI, API, CLI, external effect, or deployed runtime interpreter. Exact behavior is covered by clean aggregate, compiler-boundary, codec, serialization, source-guard, and strict traceability evidence at the verified final HEAD.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0005-simplify-post-trust-boundary`
- `openspec/changes/archive/2026-09-04-use-scala-first-quantity-and-reference-data`
- `openspec/changes/archive/2026-09-04-use-scala-first-quantity-and-reference-data/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/46
