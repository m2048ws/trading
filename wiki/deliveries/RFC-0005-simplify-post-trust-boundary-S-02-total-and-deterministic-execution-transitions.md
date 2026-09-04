---
type: delivery
updated: 2026-09-04
rfc: RFC-0005-simplify-post-trust-boundary
slice: S-02-total-and-deterministic-execution-transitions
change: total-and-deterministic-execution-transitions
status: archived
archived: 2026-09-04
evidence_manifest: sha256:fc98726449f5ed9c353d75b88b17d255c3c509f1c8d84be94eda66e5ee00e8c9
source_digest: sha256:06321277e89e5cf534d3885211fe15f6392a8d0f633ead24e71281d22804ea13
---

# RFC-0005-simplify-post-trust-boundary/S-02-total-and-deterministic-execution-transitions

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
| AC-004 | automated | file:target%2Fcorgi-verify%2Fs02-canonical.log#sha256:d65efb330db1568b9049da8b91b29ac76b964c7dcc6b44a94dfaf83bcbd7f9c2; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCommands.scala#sha256:0e5f8767e4f7dbdceea1b10186a053e06a5d99f27f602f0b249cea55137d5a66; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCommandStateSuite.scala#sha256:9b6db2367fd101a7ff0129293d2ca98d92e4a2380c9aefa8008d25b80a32ca28; file:openspec%2Fchanges%2Ftotal-and-deterministic-execution-transitions%2Fspecs%2Factual-execution-lifecycle%2Fspec.md#sha256:038ab01f52aa95819d937e9b4e11b954583f7616b46fa9e8c87653c97440ea68 | PASS |
| AC-005 | automated | file:target%2Fcorgi-verify%2Fs02-static.log#sha256:a3fa5b83749c90f4390da305ec8f5dc046e0604a217fd7d97d01daccf3517c72; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FExecutionState.scala#sha256:07767ffc45c8d067e8f8486993c01bea5e3d23e8016eb3deef5b94c2bc29855e; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fpositive%2FExecutionAuthorityBoundaryClient.scala#sha256:7199a9c8f2d847f10d1813aa73d7ea5f95a44255de450ea6fcc954f7f041b401; file:openspec%2Fchanges%2Ftotal-and-deterministic-execution-transitions%2Fspecs%2Factual-execution-lifecycle%2Fspec.md#sha256:038ab01f52aa95819d937e9b4e11b954583f7616b46fa9e8c87653c97440ea68 | PASS |
| AC-006 | automated | file:target%2Fcorgi-verify%2Fs02-canonical.log#sha256:d65efb330db1568b9049da8b91b29ac76b964c7dcc6b44a94dfaf83bcbd7f9c2; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FExecutionOrderings.scala#sha256:056fec3a5d5521e216fcc83287c198adc56ef7a08cef4cab53c1cce85e0f6d4d; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionOrderingsSuite.scala#sha256:67bdf3936d89df0a34521050e628b2e16975bc9549ee1bc8731fc61e94ca3052; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedger.scala#sha256:3d2f8a48bc2393e16eb916142b181b316b53a6aaeac0b373f16cdabd0ffe5853; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCancellation.scala#sha256:bf3301df2c38076006930ca2ffb960c4092639bf6a393c37ff83f90df5daa78c | PASS |
| AC-007 | automated | file:target%2Fcorgi-verify%2Fs02-static.log#sha256:a3fa5b83749c90f4390da305ec8f5dc046e0604a217fd7d97d01daccf3517c72; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FExecutionState.scala#sha256:07767ffc45c8d067e8f8486993c01bea5e3d23e8016eb3deef5b94c2bc29855e; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FCancellation.scala#sha256:bf3301df2c38076006930ca2ffb960c4092639bf6a393c37ff83f90df5daa78c; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FCancellationSuite.scala#sha256:e982eba61a7be8c5d0a8a95b02766779ed8082b21465673994af96d24f7be277 | PASS |

## Implementation
- Task Group 1: `42c59c4860900f24e1b523428574503f138931be`
- Task Group 2: `869425ad202580b8da18835af8b741468cd995a8`
- Task Group 3: `a977df576bf089a366930183c14f45029cf77a14`
- Final HEAD: `a977df576bf089a366930183c14f45029cf77a14`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human reviewer confirmed no runtime-facing QA path: this slice changes only pure in-process execution lifecycle transitions, deterministic ordering, and observation derivation, with no UI, API, CLI, external effect, or deployed runtime interpreter. The full behavior is covered by exact-head automated and compiler-boundary evidence.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0005-simplify-post-trust-boundary`
- `openspec/changes/archive/2026-09-04-total-and-deterministic-execution-transitions`
- `openspec/changes/archive/2026-09-04-total-and-deterministic-execution-transitions/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/40
