---
type: delivery
updated: 2026-09-04
rfc: RFC-0005-simplify-post-trust-boundary
slice: S-01-retire-trust-boundary-migration-scaffolding
change: retire-trust-boundary-migration-scaffolding
status: archived
archived: 2026-09-04
evidence_manifest: sha256:048651d8e79a23efe53616bb45e0c46e0f4a986f1278d5816470b1fc68702ad8
source_digest: sha256:dfac56dc14fb55e286cd1493adb5014f44a2dba9858e8bb8e75e4314d6e58782
---

# RFC-0005-simplify-post-trust-boundary/S-01-retire-trust-boundary-migration-scaffolding

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
| AC-001 | automated | file:target%2Fcorgi-verify%2Fretire-static.log#sha256:d8f4d6c07ea5f1bf7e3653240a4b140b1c2a776df84228eca548ce213e5b3308; file:target%2Fcorgi-verify%2Fretire-canonical.log#sha256:d42660628c7c20b9884de2a7bad2250b5e72eee76633524a937773dc7bf2a14d; file:tools%2Fcheck-in-process-reflection.sh#sha256:bca2729121ce2bde4dfe3e8db2af53d6129ea0db1162eaab0dd982d769ddf3d6; file:tools%2Ftest-check-in-process-reflection.sh#sha256:0679a80193d81a4ab8836387de8054644fd1569e9b4275506cb3171fdbce9510; file:.github%2Fworkflows%2Fci.yml#sha256:8680ddc9cf71dc5c8b8ea8d7bdc66305f18c26578fd83defe96a9432ae3032c1; file:openspec%2Fchanges%2Fretire-trust-boundary-migration-scaffolding%2Fspecs%2Fscala-functional-design%2Fspec.md#sha256:b95059f4f262b87737d159ab30e8f2b2b372c37fe72996d9700b2d9695e971ce | PASS |
| AC-002 | automated | file:target%2Fcorgi-verify%2Fretire-static.log#sha256:d8f4d6c07ea5f1bf7e3653240a4b140b1c2a776df84228eca548ce213e5b3308; file:target%2Fcorgi-verify%2Fretire-canonical.log#sha256:d42660628c7c20b9884de2a7bad2250b5e72eee76633524a937773dc7bf2a14d; file:build.sbt#sha256:790e4ae7fa4ab9334b36c0273bc4d44d02d59cac352b50f5f0aeaadc026f5db8; file:openspec%2Fchanges%2Fretire-trust-boundary-migration-scaffolding%2Fspecs%2Frepository-architecture%2Fspec.md#sha256:80bd7e8759f43f4bb72c092656264ef93467aad84892d068cb7fff98a3a64567 | PASS |
| AC-003 | automated | file:target%2Fcorgi-verify%2Fretire-static.log#sha256:d8f4d6c07ea5f1bf7e3653240a4b140b1c2a776df84228eca548ce213e5b3308; file:tools%2Fcheck-in-process-reflection.sh#sha256:bca2729121ce2bde4dfe3e8db2af53d6129ea0db1162eaab0dd982d769ddf3d6; file:tools%2Ftest-check-in-process-reflection.sh#sha256:0679a80193d81a4ab8836387de8054644fd1569e9b4275506cb3171fdbce9510; file:openspec%2Fchanges%2Fretire-trust-boundary-migration-scaffolding%2Fspecs%2Fscala-functional-design%2Fspec.md#sha256:b95059f4f262b87737d159ab30e8f2b2b372c37fe72996d9700b2d9695e971ce | PASS |

## Implementation
- Task Group 1: `847a532f595bbe63e796f480c7d1eb61a812a04a`
- Task Group 2: `522fad5d570c1a76da6c30b408c26f0991bee1af`
- Final HEAD: `522fad5d570c1a76da6c30b408c26f0991bee1af`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human reviewer confirmed no runtime impact: this slice changes only the production-source reflection guard, its isolated regression fixture, CI policy wiring, and obsolete runtime build overrides while preserving production source and observable behavior.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0005-simplify-post-trust-boundary`
- `openspec/changes/archive/2026-09-04-retire-trust-boundary-migration-scaffolding`
- `openspec/changes/archive/2026-09-04-retire-trust-boundary-migration-scaffolding/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/38
