# Task Group 10 — Adversarial reconciliation and whole-repository evidence

## Fixture classification and implementation

Every completed-artifact and adversarial fixture was reviewed against the
cooperative in-process trust model. Twenty-five fixtures whose only purpose was
same-package/JVM spoofing, hostile subclass or evidence implementation, exact
implementation access, or deliberate constructor bypass were removed:

| Retired category | Removed fixtures | Replacement evidence |
| --- | ---: | --- |
| Same-package, package-spoof, or private runtime implementation access | 10 | Supported quantity/grid construction, catalog issuance, runtime behavior, dependency isolation, and ordinary Scala/Java clients. |
| Hostile subclass or direct evidence implementation | 5 | Checked evidence derivation, mismatched-evidence rejection, exhaustive closed matching, and semantic strengthening tests. |
| Deliberate raw/private constructor bypass | 10 | Typed smart-constructor failures, erased-input validation, product-copy rejection where it remains a supported API promise, and malformed external-data tests. |

Constructor-modifier and exact/final implementation-class assertions were also
removed from quantity, reference-data, risk, execution-lifecycle, application,
runtime, order, and scenario tests. Mixed fixtures were narrowed instead of
deleted where they still covered ordinary type errors, immutable API behavior,
or checked erased input.

The runtime `LiveCatalogBridge` no longer loads or verifies its caller with
`StackWalker` and `Class.forName`; it is a package-local erased adapter used by
the public checked runtime factory. The source guard now scans both Scala and
Java production/benchmark sources and rejects reflective class loading, stack
walking, method handles, private-member reflection, and accessibility bypasses.

Module guidance now consistently describes catalog-issued and checked values,
not non-forgeable JVM representations. The permanent architecture/spec contract
continues to distinguish cooperative in-process code from untrusted wire,
persistence, configuration, venue, replay, and erased input.

## Retained supported-boundary evidence

- Quantity and grid suites retain exact arithmetic, dimension mismatches,
  refinement checks, witness-backed construction, runtime-key consistency,
  projection, and serialization rejection.
- Reference-data suites and ordinary Java clients retain checked stable IDs,
  positive grid quanta, catalog-issued handles, lineage/identity reconciliation,
  atomic catalog transitions, typed violations, and serialization rejection.
- Completed instrument, order, scenario, fee, risk, and execution clients retain
  associated-shape evidence, identity checks, exact economics, attribution,
  monotonicity, command/fact conflicts, replay, completeness, and erased-input
  rejection.
- Codec unit, law, completed-JAR, and ordinary Java fixtures retain strict and
  bounded parsing, duplicate rejection, malformed V1 diagnostics, exact numeric
  reconstruction, incoherent catalog identity failures, explicit V1 dispatch,
  canonical/JCS bytes, golden vectors, and replay semantics.
- Packaged classpath/API checks retain the dependency graph, JDK target,
  forbidden downstream symbols, closed-match compilation, and supported public
  method surfaces without depending on private implementation shapes.

## Acceptance evidence

| Acceptance criterion | Evidence |
| --- | --- |
| AC-001 | `docs/design-principles.md`, `docs/architecture-charter-audit.md`, module READMEs, the accepted RFC, and repository-architecture/Scala-functional-design deltas state the cooperative-code/untrusted-data distinction and same-JVM forgery non-goal. Human proportionality confirmation remains for the later Review gate. |
| AC-002 | `tools/check-in-process-reflection.sh` passes with zero Scala or Java production/benchmark sites. Final source inspection found no method handles, reflective construction/private-member access, `Class.forName`, or `StackWalker`, and no new unchecked public helper or authority-manufacturing cast. |
| AC-003 | The clean focused/property matrix exercises field/refinement validity and every checked strengthening boundary across quantities, reference data, instruments, orders, scenarios, fees, risk, execution, codecs, and replay. |
| AC-004 | The clean aggregate passed all 1,043 tests, including exact dimension/grid behavior, lineage and identity, associated evidence, risk/fee semantics, lifecycle conflicts/completeness, strict/canonical V1 codecs, and Java-serialization rejection. |
| AC-005 | The 25 hostile-only fixtures and remaining constructor/finality assertions were removed or narrowed; retained completed-artifact tests cover supported type, semantic, erased-input, dependency, and serialization behavior. `build.sbt` and project dependency definitions are unchanged. |
| AC-006 | Ordinary Scala completed-artifact clients and Java clients for reference data, orders, execution, and codecs compile and run without reflective setup. Codec tests reject malformed V1 records and catalog/execution reconstruction tests reject incoherent identities without schema or golden-vector changes. Human walkthrough confirmation remains for the later Review gate. |

## Verification and automated review

| Check | Result |
| --- | --- |
| Clean root matrix: `clean scalafmtCheckAll scalafmtSbtCheck test benchmarks/Jmh/compile` | Pass: all 1,043 tests across 12 projects, all formatting checks, completed artifacts, adversarial suites, and JMH compilation. |
| Focused migration matrix | Pass: 596 quantity, 13 reference-data, 18 runtime, 64 execution-lifecycle, and 135 completed-artifact/adversarial tests. |
| Short JMH risk maximum sizing, cap 1024 | Pass: 307,604.779 ops/s with one 200 ms warmup and measurement; indicative only. |
| Short JMH parsed codec reconstruction, batch 1024 | Pass: 21,232.260 ops/s with one 200 ms warmup and measurement; indicative only. |
| Short JMH runtime snapshot capture | Pass: 5,871.583 ops/s with one 200 ms warmup and measurement; indicative only. |
| Reflection guard, source/diff inspection | Pass: zero production/benchmark reflective sites and no whitespace error. |
| Dependency, schema, and golden-resource diff inspection | Pass: no build/project dependency or boundary-codec JSON resource changed. |
| OpenSpec strict validation | Pass: the change and all 17 repository specifications validate. |
| Corgi status/source/traceability | Pass at planning revision `sha256:559601683cb10e23a63595429e066972c911a1ebf5de25ec244b5cb28d7addd1`, source digest `sha256:9c8d459b6066675a3128ef4974967a1f199bbd3b0e4871179110e0fe57337880`, and traceability digest `sha256:31952a7384c61bba43d4c9498a68703047d3a93902c05c7e328122087bae5dc2`. |

The structured review covered requirement/scenario coverage, code quality,
functional behavior, architecture, external-data security, compatibility,
dependency scope, performance, Java usability, and fixture classification. Its
findings array is empty. The review changed no file, no finding was
human-triaged, and it is not canonical whole-change Verify or Human Review.
