# RFC-0005-simplify-post-trust-boundary: Simplify the code exposed by the in-process trust boundary

## Goal

Complete the simplification enabled by RFC-0004 now that constructor secrecy and reflective access are no longer
treated as domain authority. Remove obsolete Java-build and reflection-migration scaffolding, make actual-execution
transition results total, express deterministic replay ordering with typed values instead of encoded strings, avoid
recomputing derived execution state, and replace repetitive derived-result class machinery with direct Scala 3 sums
and products where construction establishes no additional semantic authority.

The resulting code SHALL be smaller and easier for one maintainer to read while preserving exact arithmetic,
dimension and grid safety, execution identity and provenance, validation, deterministic replay, exposure,
uncertainty, and anomaly semantics.

## Non-goals

- Do not change wire formats, schema versions, canonical encodings, persisted records, economic calculations, or
  externally observable execution outcomes.
- Do not weaken checked construction for commands, source facts, execution lifecycles, refinements, non-empty
  collections, catalog lineage, dimension/grid associations, or values whose construction establishes a predicate.
- Do not preserve Java source or JVM binary compatibility for the execution-lifecycle derived result and observation
  types changed by this RFC. Java API ergonomics for those types are not a current design priority.
- Do not remove the JDK 25 baseline, Java compilation tests that protect still-supported boundary factories, or
  fail-closed Java object-serialization behavior.
- Do not convert every domain class to a case class, expose unchecked `apply` or `copy` operations for
  invariant-bearing values, or perform unrelated naming and formatting churn.
- Do not add a general ordering framework, effect abstraction, cache, mutable optimization, or cross-module facade.

## Boundary

This amendment applies the cooperative in-process model from RFC-0004 to migration tooling, the runtime build, and the
pure actual-execution lifecycle. It covers three independently deliverable slices.

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
signatures for them are explicitly not compatibility commitments. Their domain meaning, public field information,
closed alternatives, structural equality, and Java-serialization rejection SHALL remain observable. Existing Java
support outside this boundary, especially checked external-data and boundary-codec construction, remains unchanged.

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
- Dropping Java compatibility could inadvertently remove useful Java coverage outside execution-derived outputs.
  Mitigation: scope compatibility changes to the named lifecycle representations and retain completed-artifact Java
  tests for checked boundary, codec, quantity, catalog, order, scenario, risk, and fee APIs.
- Combining cleanup with semantic refactoring could make failures difficult to localize. Mitigation: deliver the three
  slices separately and keep every Task Group buildable with focused characterization tests before representation
  changes.
