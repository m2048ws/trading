---
type: delivery
updated: 2026-09-04
rfc: RFC-0005-simplify-post-trust-boundary
slice: S-03-use-direct-scala-derived-models
change: use-direct-scala-derived-models
status: archived
archived: 2026-09-04
evidence_manifest: sha256:aa9b0dc3e278d955dd75f2edab6fd5d572f17f2754252b7e4cb6b345e9e80658
source_digest: sha256:495368726ab7e6d992b882779e3d87f360ee51dc422efd00ceb7cae29dedba56
---

# RFC-0005-simplify-post-trust-boundary/S-03-use-direct-scala-derived-models

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
| AC-008 | automated | file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCommands.scala#sha256:d91707cff41ee925dd7829b1bb0832411397b7609032d694547d54c5cd332e1c; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceEvidenceState.scala#sha256:5f7c5fecb64d226e990d93ad20ceb1e0fbaf2aaf1d2a19665f8da1d4c0bd2c1e; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSubmissionKnowledge.scala#sha256:77c378bc786f15b955ed19e6bc3b52229ab889f7b41ca8707484e7893da5e591; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCancellation.scala#sha256:8e41a05bbda8b3cbed41f6dc7b52ba33fb593b8cad91faec07ac4bdbeff8194e; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedger.scala#sha256:c6886d60f1781cc520b5fd6f34e94d426c11636907eb691f074520529a9534db; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FExecutionState.scala#sha256:ff8f2a64f43d2c693e7e9697f121d9701745ea92402f05ddff130d6d33eb7205 | PASS |
| AC-009 | automated | file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSubmissionKnowledge.scala#sha256:77c378bc786f15b955ed19e6bc3b52229ab889f7b41ca8707484e7893da5e591; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCancellation.scala#sha256:8e41a05bbda8b3cbed41f6dc7b52ba33fb593b8cad91faec07ac4bdbeff8194e; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCommands.scala#sha256:d91707cff41ee925dd7829b1bb0832411397b7609032d694547d54c5cd332e1c; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceEvidenceState.scala#sha256:5f7c5fecb64d226e990d93ad20ceb1e0fbaf2aaf1d2a19665f8da1d4c0bd2c1e; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedger.scala#sha256:c6886d60f1781cc520b5fd6f34e94d426c11636907eb691f074520529a9534db; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FExecutionState.scala#sha256:ff8f2a64f43d2c693e7e9697f121d9701745ea92402f05ddff130d6d33eb7205 | PASS |
| AC-010 | automated | file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fpositive%2FExecutionAuthorityBoundaryClient.scala#sha256:5371e4023bd1410338a92470a92dba843ebf1190884b817f49f01e6e2f5e5197; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCommandStateSuite.scala#sha256:473b111742d40ff2cc356347e31263ad19229e75a64c3c1c3d4de88f70f0d1a7; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactSuite.scala#sha256:498910d5f85e3efeb2bc5b2f5e3af86a5f4922f098a39c8c8334404377538422; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSubmissionKnowledgeSuite.scala#sha256:fa09dbd16b03f2a8b003bd8a20031682bfe52d79b60cb63a65681b69f6e63870; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCancellationSuite.scala#sha256:9ebf9573f20179c2c6d6c81630cfda6e7a7a817e816c0d2dd8085ff8969b7c11; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedgerSuite.scala#sha256:4fdebac83e6e2fe237f43179f69e2ed9360d225dec93863583bc30ad3d3cda16; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionStateSuite.scala#sha256:bc756b946c9564325cfcc642ad6129a2ac5cefebd154e4945e543d748cf6add3 | PASS |
| AC-011 | automated | file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCommands.scala#sha256:d91707cff41ee925dd7829b1bb0832411397b7609032d694547d54c5cd332e1c; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceFacts.scala#sha256:f7c8ea3a31c08d8408568a3ecf1414944065068a9d48caf446063de6b9676354; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FLifecycle.scala#sha256:9ccd2e3bf45259d33616bc7f0a5efa6580fb24d43b1d5ad27ba0075c06dea47a; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FIdentity.scala#sha256:26465ddadb1457742cf1125a899ecb64365cb5ceb7bb69104af2e04097382563; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCommandStateSuite.scala#sha256:473b111742d40ff2cc356347e31263ad19229e75a64c3c1c3d4de88f70f0d1a7; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactSuite.scala#sha256:498910d5f85e3efeb2bc5b2f5e3af86a5f4922f098a39c8c8334404377538422; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionIdentitySuite.scala#sha256:08f7d0cb9b5fe0d1cb4808675acfef54429fff89bcad848f7fab121c20d760fe; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fnegative%2FEstablishedOwnerCannotImportExecution.scala#sha256:c01d1abcaf83b27bacf711fb30eb126b1832a1e0a1074e897d48b4797da66a3c; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fnegative%2FExecutionLifecycleHasNoDownstream.scala#sha256:6c2ce12394ff392ffdad46db771a0189589d0b2dc0d7e710b546ad3be98e34ea | PASS |

## Implementation
- Task Group 1: `07ecbaba79ab1092bf73514268054f477597d183`
- Task Group 2: `60b585f8ac4a3a92941bd4f47bca5a643b28fc6e`
- Task Group 3: `f46c95751e6f72e45ee33881eb1705625b2228d9`
- Final HEAD: `f46c95751e6f72e45ee33881eb1705625b2228d9`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — The approved slice changes only internal immutable Scala representations and exposes no runnable user, UI, API, CLI, or backend path requiring real-user QA.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0005-simplify-post-trust-boundary`
- `openspec/changes/archive/2026-09-04-use-direct-scala-derived-models`
- `openspec/changes/archive/2026-09-04-use-direct-scala-derived-models/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/42
