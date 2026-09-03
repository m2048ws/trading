# RFC-0005-simplify-post-trust-boundary: Simplify the code exposed by the in-process trust boundary

## Goal

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

## Non-goals

- Do not change wire formats, schema versions, canonical encodings, persisted records, economic calculations, or
  externally observable execution outcomes.
- Do not weaken checked construction for commands, source facts, execution lifecycles, refinements, non-empty
  collections, catalog lineage, dimension/grid associations, or values whose construction establishes a predicate.
- Do not provide or preserve a supported Java source API or JVM binary API shape for repository domain artifacts.
  Scala source shapes MAY change where a more direct Scala 3 model preserves the documented domain semantics.
- Do not remove the JDK 25 baseline, JVM execution, Java-library integration, exact `java.math.BigDecimal` conversion,
  or fail-closed Java object-serialization behavior.
- Do not enable Scala explicit nulls or mechanically remove runtime null checks. External-data, Java-library,
  construction-invariant, and useful public-boundary checks remain governed by their owning semantics.
- Do not convert every domain class to a case class, expose unchecked `apply` or `copy` operations for
  invariant-bearing values, or perform unrelated naming and formatting churn.
- Do not add a general ordering framework, effect abstraction, cache, mutable optimization, or cross-module facade.

## Boundary

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

## Slices

### S-01-retire-trust-boundary-migration-scaffolding: Make the simplified boundary permanent

- AC-001 [evidence: automated]: The production and benchmark source guard fails on every prohibited reflection or
  method-handle token, has no allowance baseline or per-file migration accounting, passes on the repository, and is
  invoked by the normal CI workflow.
- AC-002 [evidence: automated]: The runtime project contains no bridge-specific `JavaThenScala` compile order, stale
  Java-bridge comment, or duplicated project-local JDK release option, while its focused tests and the clean
  repository build pass.
- AC-003 [evidence: automated]: A guard regression fixture proves that adding one prohibited production token fails
  deterministically without modifying tracked source or relying on a historical baseline.

### S-02-total-and-deterministic-execution-transitions: Simplify transition and observation data flow

- AC-004 [evidence: automated]: Command recording and dispatch observation return a closed total transition sum in
  which rejection always carries non-empty `CommandViolations` and every non-rejected classification carries no
  violation option; production execution code contains no unchecked violation extraction.
- AC-005 [evidence: automated]: Focused transition tests cover applied, idempotent, command-conflict,
  dispatch-conflict, and rejected alternatives and prove that each preserves the previous state and classification
  semantics.
- AC-006 [evidence: automated]: Execution replay, effective-fill, anomaly, continuation, and diagnostic ordering reuse
  owner-local typed orderings rather than delimiter-concatenated composite strings; collision-focused tests include
  identifiers containing the former delimiters and preserve deterministic delivery-order-independent results.
- AC-007 [evidence: automated]: One lifecycle observation derives one effective-fill ledger and supplies it to anomaly
  derivation, while existing exact exposure, correction, bust, overfill, cancellation-race, conflict, and unresolved-
  reference results remain structurally equal under focused and property tests.

### S-03-use-direct-scala-derived-models: Remove representation boilerplate from derived execution results

- AC-008 [evidence: automated]: Submission knowledge, cancellation knowledge, effective-fill classification, and
  source/command transition alternatives are represented as exhaustive Scala sums without parallel kind flags,
  optional payloads, or one-class-per-case hand-written equality boilerplate.
- AC-009 [evidence: automated]: Derived evidence, anomaly, ledger, and observation products use direct structural
  products wherever all field combinations are valid; redundant constructor-forwarding and manual equality/hash code
  are removed from the converted types without exposing unchecked construction for invariant-bearing inputs.
- AC-010 [evidence: automated]: Downstream Scala fixtures compile exhaustive matches for the revised derived sums and
  observe the same fields and structural equality, while tests prove changed derived values still reject Java object
  serialization.
- AC-011 [evidence: automated]: Checked factories and typed failures for commands, source facts, execution lifecycle,
  non-empty wrappers, identity/refinement values, and external reconstruction remain present and retain their focused
  positive and negative coverage.

### S-04-use-scala-first-quantity-and-reference-data: Remove Java-owned construction and catalog result boilerplate

- AC-012 [evidence: automated]: `AssetId`, `GridId`, and `GridVersion` are Scala-owned checked values and the repository
  contains no production Java source; their valid construction, precise empty/nonpositive failures, value equality,
  hashing, domain-readable display, and Java-object-serialization rejection remain covered.
- AC-013 [evidence: automated]: `UniformGrid` and `GridDefinition` expose refined Scala construction without a
  Java-only raw factory or defensive revalidation of an already established `PositiveRational`; raw nonpositive values
  still fail at the owning refinement or external reconstruction boundary before grid authority is returned.
- AC-014 [evidence: automated]: Catalog commit and transition observations use exhaustive direct Scala sums/products
  without hand-written `Product`, extractor, equality, hash, or rendering machinery, while publication, unchanged,
  revision, delta, lineage, and structural-equality semantics remain unchanged.
- AC-015 [evidence: automated]: Quantity and reference-data specifications and completed-artifact Scala fixtures state
  the Scala-only source boundary and retain dimension, generative grid identity, stable identity, catalog conflict,
  reconciliation, null-boundary, and serialization coverage.

### S-05-remove-java-api-compatibility: Simplify order/scenario composition and retire Java API fixtures

- AC-016 [evidence: automated]: Order activation, pricing, and execution types contain no `Any`-based evidence or
  resolution acceptance hooks used only for erased Java calls; associated-evidence negative Scala fixtures still fail
  to compile and semantic activation/pricing mismatches still return their typed failures.
- AC-017 [evidence: automated]: `ScenarioAssumptions.create`, `one`, and `many` construct directly from associated
  Scala evidence and already non-empty slices, while `fromVector` retains its typed empty-slice result and downstream
  scenario, fee, and codec behavior remains unchanged.
- AC-018 [evidence: automated]: Ordinary-Java domain API fixtures and their dedicated dynamic compiler/classloader
  harnesses are removed from reference data, order/scenario, execution lifecycle, and boundary codecs; completed-JAR
  Scala API, negative compiler, dependency, wire, and semantic coverage remains in the clean aggregate test matrix.
- AC-019 [evidence: automated]: Active architecture, quantity-grid, reference-data, order/scenario, execution, and codec
  specifications consistently describe Scala 3 as the supported source API without changing the JDK baseline,
  external representations, Java-library integration, exact decimal conversion, or serialization rejection.

## Risks

- A case-class or enum conversion could accidentally expose `apply` or `copy` as unchecked construction authority.
  Mitigation: classify each candidate as a derived sum, field-valid product, checked value, or evidence-bearing value;
  convert only the first two and retain owner-only derivation where stronger meaning is established.
- Scala 3 enum encoding with path-dependent dimensions may worsen inference or make ordinary pattern matching harder.
  Mitigation: spike the most type-rich effective-fill alternative first and retain an equivalent sealed sum when it is
  clearer or more reliable.
- Shared ordering could silently change tie-breaking or omit a provenance component. Mitigation: characterize current
  results first, define typed lexicographic components explicitly, and test delimiter collisions, equivalent evidence
  permutations, gaps, conflicts, corrections, busts, and unsequenced facts.
- Reusing a derived ledger could couple anomaly logic to an incomplete projection. Mitigation: keep ledger derivation
  pure, pass the complete value explicitly, and compare all observations against characterized pre-change results.
- Removing raw JVM factories could strand a legitimate Scala caller or move invalidity past its owning boundary.
  Mitigation: prove the removed entries have no production Scala consumer, retain the canonical refinement, and keep
  raw validation in external reconstruction.
- Removing erased Java shape checks could accidentally remove a semantic association check. Mitigation: distinguish
  type-shape acceptance from activation/pricing verification, preserve the latter, and retain same-shape mismatch and
  negative associated-type coverage.
- Retiring Java fixtures could remove useful semantic or artifact evidence along with compatibility evidence.
  Mitigation: map every removed assertion to retained Scala compiler, focused behavioral, completed-JAR, or external-
  boundary coverage before deleting the fixture or harness.
- Scala-owned identifiers or direct result sums could accidentally alter equality, rendering, serialization rejection,
  or catalog authority. Mitigation: characterize those behaviors before conversion and verify them from completed
  artifacts without asserting a generated JVM layout.
- A broad null-check deletion would conflate unsupported Java source compatibility with external-data safety.
  Mitigation: retain the hybrid null policy, exclude explicit-nulls adoption from this RFC, and require an owning
  semantic reason for every removed runtime check.
- Combining cleanup with semantic refactoring could make failures difficult to localize. Mitigation: deliver the five
  slices separately and keep every Task Group buildable with focused characterization tests before representation
  changes.
