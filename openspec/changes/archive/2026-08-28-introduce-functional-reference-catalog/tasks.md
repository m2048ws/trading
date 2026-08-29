## 1. Portfolio and Dependency Gate

- [x] 1.1 Confirm Proposal 0's complete-portfolio gate has passed and the applied quantity/reference-data boundary
  supplies the accepted identity, handle, lineage, and anonymous-grid contracts before changing production code.
- [x] 1.2 Refresh Git, OpenSpec, source, and build state and record a clean baseline compile/test plus the exact temporary
  registry surface this change will remove.
- [x] 1.3 Reconcile this change's snapshot and live-port names with the instrument-assembly, boundary-codec, and
  application/runtime proposals; resolve any ownership or implementation-order conflict before implementation.

## 2. Catalog Algebra and Public Domain Values

- [x] 2.1 Add closed asset/dimension/grid `CatalogCommand` alternatives and a guarded non-empty `CatalogBatch` that
  retains ordered inspectable definitions without trusted handles or effects.
- [x] 2.2 Add nonnegative arbitrary-precision `CatalogRevision`, non-empty append-only `CatalogDelta`, and explicit
  unchanged/published `CatalogCommit` outcomes with guarded construction and fail-closed serialization behavior.
- [x] 2.3 Add closed indexed `CatalogViolation`, domain-owned non-empty `CatalogViolations`, and `CatalogLookupError`
  hierarchies covering duplicate proposals, immutable conflicts, one-to-one asset/dimension conflicts, missing
  prerequisites, foreign lineage, and unknown identities.
- [x] 2.4 Add focused product/equality/null/serialization tests for every new public value and compiler checks preventing
  construction of empty batches, empty deltas, negative revisions, or caller-owned authority.

## 3. Immutable State and Staged Transition Semantics

- [x] 3.1 Implement lexically controlled generative catalog-root/lineage creation outside `CatalogModel.commit` and
  immutable `CatalogState` indexes for dimensions, assets, reverse asset-dimension binding, and full grid identities;
  verify repeated transition evaluation never creates a new lineage.
- [x] 3.2 Implement batch indexing and deterministic duplicate/current-state conflict detection while retaining command
  indices and stable rule ordinals independent of immutable-map iteration order.
- [x] 3.3 Implement complete proposed-dimension formation and one-to-one asset/dimension validation so asset-before-
  dimension and dimension-before-asset batches have equal meaning and standalone dimensions can be reused safely.
- [x] 3.4 Implement dependent grid validation only after coherent dimensions exist, including idempotent exact
  definitions, immutable full-key conflicts, and coexistence of corrected `GridVersion`s.
- [x] 3.5 Compose independent checks applicatively and dependent stages sequentially, converting internal validation
  containers to deterministic public `CatalogViolations` without issuing handles on failure.
- [x] 3.6 Implement atomic successor construction, one-revision publication, complete deterministic deltas, and explicit
  unchanged outcomes while leaving the input state untouched on every path.

## 4. Canonical Handles and Immutable Snapshots

- [x] 4.1 Issue canonical `DimensionHandle`, `Asset`, and `GridHandle` values only after successful validation, localizing
  existential boxes and justified casts inside catalog-owned helpers.
- [x] 4.2 Reuse existing canonical handles and anonymous mathematical grids through immutable structural sharing across
  successor states; return existing handles for idempotent definitions and distinct handles for new versions.
- [x] 4.3 Implement immutable `CatalogSnapshot` with revision, counts, and direct pure asset/dimension/full-grid lookup,
  including dependent return packaging and typed unknown/foreign-lineage failures without map scans or mutable caches.
- [x] 4.4 Add tests proving historical snapshots remain unchanged, old handles remain valid after additions, new keys are
  unknown to old snapshots, equal rebuilt catalogs have different lineages, and public semantics never require JVM
  reference equality.
- [x] 4.5 Add property tests for append-only state, transition/input immutability, idempotence, revision monotonicity,
  delta/state conservation, batch atomicity, deterministic validation order, and valid command-order permutations.

## 5. Application-Level Live Catalog Port

- [x] 5.1 Add the initial `application` SBT project and `trading-application` artifact depending on reference data, wire it
  into the root and immutable-JAR adversarial classpaths, and add no concrete effect-runtime dependency.
- [x] 5.2 Define the minimal `LiveCatalog[F[_]]` port with snapshot capture and atomic batch commit only, returning the
  reference-data domain outcomes without effectful point lookup, reset, subscription, telemetry, or interpreter state.
- [x] 5.3 Define a reusable interpreter-conformance test contract for linearized snapshot/commit behavior, concurrent
  independent and conflicting batches, idempotent retry, no lost updates, and equivalence with the pure catalog model.
- [x] 5.4 Add compiler/module tests proving pure reference data and every domain artifact are independent of
  `LiveCatalog[F]`, while application workflows can remain polymorphic over it without importing a concrete runtime.

## 6. Remove the Transitional Registry and Migrate Construction

- [x] 6.1 Replace reference-data and economics fixture setup with explicit catalog root/state/batch transitions and pure
  snapshot resolution, retaining existing stable definitions and expected handles.
- [x] 6.2 Remove the temporary synchronized `QuantityRegistry`, mutable maps, shared monitor, grid-scan decode helper,
  count-under-lock methods, and registry-object provenance checks without leaving compatibility aliases.
- [x] 6.3 Rehome or remove obsolete registry errors and tests so reference data exposes only catalog definition,
  transition, handle-evidence, and lookup failures owned by the new model.
- [x] 6.4 Search production signatures to verify quantities, economics, orders, scenarios, fee values, valuation, and risk
  receive neither catalog state nor `LiveCatalog[F]`, and snapshot consumers never retain a pointer to live state.

## 7. Documentation, Performance Contract, and Follow-On Boundaries

- [x] 7.1 Update reference-data and application documentation with the command/transition/port distinction,
  append-only correction policy, activation/delisting exclusion, snapshot capture pattern, and interpreter-neutral
  guarantees.
- [x] 7.2 Add the non-published `benchmarks` SBT project using the `sbt-jmh` plugin, initially depending only on reference
  data, exclude benchmark execution from root test aggregation, and wire explicit JMH compilation into verification
  without creating a production or publication dependency.
- [x] 7.3 Add a focused JMH benchmark demonstrating immutable snapshot lookup under representative reader counts without
  a serialized registry-monitor path; record JDK, forks, warmup, measurement, threads, and directional results without
  making environment-specific throughput a normative test.
- [x] 7.4 Document that snapshots/handles are in-memory authority rather than persistence formats and link the accepted
  boundary-codec proposal for reconstruction and the runtime proposal for concrete state, resources, streams, and
  durability.

## 8. Verification and Independent Review

- [x] 8.1 Run Scala and SBT formatting for every changed source and verify `scalafmtCheckAll`, `scalafmtSbtCheck`, and
  `git diff --check` pass.
- [x] 8.2 Run focused reference-data transition/snapshot suites, application port/compiler suites, economics fixtures,
  and immutable-JAR adversarial tests, including every new positive and negative authority path.
- [x] 8.3 Compile the JMH project explicitly, run the focused catalog benchmark with recorded parameters, then run
  `sbt -batch clean test` and confirm the complete aggregate passes with no synchronized registry remaining.
- [x] 8.4 Run strict validation for this change and `openspec validate --all --strict`, then audit the diff against the
  pure-reference/application-port/runtime-interpreter ownership boundary.
- [x] 8.5 Stage exactly the intended implementation, tests, build, documentation, and active-change artifacts in a
  validated commit-ready worktree without committing unless separately authorized.
- [x] 8.6 Obtain fresh independent review of the fully staged implementation and validation evidence; remediation must
  return to another fresh independent review before finalization.

## Implementation Evidence

- Tasks 1.1-1.3: Proposal 0 and Proposal 1 are archived at baseline HEAD
  `7ee13adb7a3454379917a3c038a79229e7b38634`; the starting index/worktree was clean; strict validation passed; and the
  clean baseline passed 747/747 tests. The removed bridge surface is the synchronized `QuantityRegistry` plus
  `QuantityRegistryKernel`, their mutable asset/dimension/grid maps, monitor-protected registration/lookup/counts, and
  dimension-local grid map. Adjacent Proposals 3, 8, and 9 consistently consume `CatalogSnapshot`, `CatalogBatch`,
  `CatalogState`, `CatalogRoot`, and `LiveCatalog[F]` with no ownership or ordering conflict.
- Tasks 8.1-8.2: `scalafmtCheckAll`, `benchmarks/scalafmtCheckAll`, `scalafmtSbtCheck`, and `git diff --check` passed.
  Focused verification passed 11 reference-data tests, application main/test compilation, 35 economics tests, 106
  packaged compiler/Java/adversarial tests, and explicit positive generic/concrete and negative authority fixtures.
- Task 8.3: the JMH project compiled and the focused 1,024-asset snapshot lookup benchmark ran on JDK 26.0.2 with one
  fork, three one-second warmups, five one-second measurements, and one/four reader threads; the recorded directional
  results were 35,789,553.461 and 136,622,996.666 operations/second respectively. The final `sbt -batch clean test`
  passed 753/753 tests, and production-source scans found no transitional registry, synchronization, mutable-map, grid-
  scan, or registry-object provenance implementation.
- Task 8.4: production-module Scaladoc passed sequentially; strict validation passed for this change and all 18 OpenSpec
  items. The final ownership audit leaves immutable definitions/transitions/snapshots in reference data, the two-method
  effect-polymorphic port in application, no port dependency in domain artifacts, no concrete interpreter/runtime, and
  benchmarks outside root aggregation and publication.
- Task 8.5: all 47 intended implementation, test, build, documentation, and active-change paths are staged; the
  unstaged diff and untracked-file list are empty, `git diff --cached --check` passes, and no commit was created.
- Review remediation F1-F4: current-state and within-batch binding classifiers are non-overlapping for already-bound
  dimensions; public catalog errors reject null and malformed nested payloads; `GridHandle.Reconciliation` is an opaque
  Scala value backed by one JVM-private permit held by the checked companion; and the reusable live contract now compares
  typed pure failures, consecutive commits, idempotent retries, concurrent losing conflicts, and overlapping snapshots.
- Remediation focused verification passed 13 reference-data tests, 6 application contract/rejection tests, 35 economics
  tests, all 106 completed-artifact adversarial tests, and completed-JAR Scala/Java checked-retyping, constructor, and
  catalog-error probes. Benchmark compilation, repository formatting checks, sequential production Scaladoc, Git diff
  checks, active/all strict OpenSpec validation, and production boundary scans also passed before the final clean gate.
- The first remediation candidate used a mixed Java production evidence class and was discarded after its one-shot clean
  build exposed a downstream classpath failure. The final smaller opaque-permit implementation removed that class, then
  a fresh clean aggregate build passed 761/761 tests (601 quantities, 13 reference data, 6 application, 35 economics,
  and 106 adversarial) without retrying the corrected implementation.
- Review cycle 2 remediation reproduced a lineage-resetting interpreter that the reusable contract certified and public
  conflict values that retained repeated positions, mismatched named keys, duplicate evidence, and equal conflict sides.
  The contract now checks supplied-initial and cross-generation asset/dimension/grid authority and rejects the retained
  resetting meta-interpreter; guarded violations reject the malformed semantic class in module, packaged Scala, and
  packaged Java probes while ordinary valid products and model-generated violations remain usable. The stale OpenSpec
  current-state context now records the application port and immutable catalog without claiming runtime/codecs exist.
- Final review-cycle-2 validation passed formatting, benchmark compilation, sequential production Scaladoc, focused
  reference-data/application/economics tests, all completed-artifact compiler/Java/adversarial tests, strict assigned/all
  OpenSpec validation, bytecode/source boundary inspection, and a fresh clean aggregate with 763/763 tests (601
  quantities, 13 reference data, 7 application, 35 economics, and 107 adversarial). The clean gate first exposed stale
  in-process test classloaders; narrow test-only process isolation at quantities and application made the completed
  dependency generations deterministic without changing production artifacts or runtime semantics.
- Review cycle 3 reproduced the retained packaged Scala/Java counterexamples for reversed duplicate positions,
  non-positive grid-conflict quanta, non-atomic binding dimensions, impossible rule/source positions, duplicate delta
  additions, and incoherent state/snapshot/publication products. Per-alternative guards now reject the locally
  impossible evidence, while cross-product `CatalogViolations`, `CatalogCommit`, and `CatalogTransition` construction
  requires the existing catalog issuance permit and preserves public Scala extractors, Java inspection, and valid
  model-generated values. Current architecture prose now includes the implemented application boundary and keeps
  runtime/codecs future-owned.
- Review-cycle-3 validation passed 13 reference-data, 7 application, 35 economics, and 109 packaged
  compiler/Java/runtime/adversarial tests plus all 601 quantity tests. The clean gate exposed mutable class-directory
  completion races first at the forked economics runner and then at quantity test compilation; no unchanged failing
  state was retried. Test-only internal dependency classpaths now consume completed immutable main artifacts, and the
  corrected final unretried clean aggregate passed 765/765. Formatting, benchmark compilation, sequential production
  Scaladoc, direct snapshot bytecode inspection, JDK-17 bytecode, and production ownership scans also passed.
- The final public-factory audit then replaced duplicate-delta constructor exceptions with typed
  `DuplicateCatalogAddition` evidence and retained raw-constructor rejection only as a JVM-forgery guard. Clean-gate
  validation of that adjustment exposed two further mutable-class-directory orderings (economics missing the completed
  reference-data generation, then an intentionally narrower diagnostic sequence missing the completed quantities
  generation); neither unchanged failure was retried. The root test gate now explicitly completes each existing main
  artifact before its sequential module test, without changing production dependency settings. The corrected final
  clean aggregate again passed 765/765, followed by formatting, JMH compilation, sequential production Scaladoc,
  source/bytecode ownership scans, and the exact former Scala/Java compiler reproductions.
- Review-cycle-5 remediation preserved the failed finalization report and reproduced its quantities-test compiler
  failure from that unretried evidence without rerunning the unchanged build. Graph inspection showed that the custom
  root prebuild stages and metadata-free `Test / internalDependencyClasspath` replacements bypassed SBT's standard
  exported-product construction. The remediation removes those custom artifact tasks and classpaths, enables
  `Compile / exportJars` on each production project, and builds the standalone compiler classpath from standard
  `Compile / exportedProducts`; SBT now owns every module test classpath and exposes only completed production JARs.
  The artifact regression checks the exact quantities classes and TASTy that were missing during finalization. The one
  corrected unretried `sbt -batch clean test` passed 765/765, the focused 45-test compiler boundary passed, formatting
  checks and JMH compilation passed, sequential production Scaladoc succeeded, and representative artifacts retain
  JDK-17 classfile version 61. The active change remains unarchived and task 8.6 remains incomplete for review cycle 5.

## Review-Cycle-3 Public Product Audit

| Public `Catalog.scala` surface | Complete retained-value invariant | Construction/result boundary | Permanent evidence |
| --- | --- | --- | --- |
| `NegativeCatalogRevision`; `CatalogRevision`/`from`/`zero` | Negative evidence is strictly negative; revisions are nonnegative | Expected negative input returns the existing typed error; raw JVM forgery is guarded | Module product/null/equality and serialization suite |
| `EmptyCatalogBatch`; all three `CatalogCommand` alternatives; `CatalogBatch`/`one`/`of`/`from` | Payloads are non-null and batches are ordered/non-empty | Empty expected input returns `EmptyCatalogBatch`; constructors cannot yield an empty retained batch | Module products, compiler constructor rejection, model/property tests |
| `CatalogAddition` alternatives; `EmptyCatalogDelta`; `DuplicateCatalogAddition`; `CatalogDelta`/`from` | Payloads are non-null and additions are non-empty and unique | Empty or duplicate expected input returns typed `CatalogDeltaError`; JVM constructor forgery is guarded | Module plus packaged Scala/Java negative and positive delta fixtures |
| `DuplicateAssetProposal`; `DuplicateGridProposal` | Named key matches every definition; definitions genuinely conflict; source indices are nonnegative, distinct, ordered, and cover the definitions | Public evidence constructors reject impossible nested evidence | Module and packaged Scala exact/nearby negatives plus valid counterparts |
| `ImmutableAssetConflict`; `ImmutableGridConflict` | Named identities match both sides; sides differ; grid quanta are positive | Public evidence constructors reject non-conflicts and non-domain quanta | Module and packaged Scala/Java exact negatives plus valid counterparts |
| `AssetDimensionAlreadyBound`; `MissingGridDimension` | Binding dimensions are atomic and asset IDs differ; missing-grid identity is non-null | Public evidence constructors reject impossible binding evidence | Module and packaged Scala/Java exact negatives plus valid counterparts |
| `IndexedCatalogViolation`; `CatalogViolations` | Index is canonical for duplicate evidence; ordinal matches the alternative; collection positions are non-empty, unique, and ordered | Indexed evidence is locally guarded; collection issuance uses the existing catalog permit | Module, model accumulation, packaged Scala/Java ordinal/source/constructor fixtures |
| Four `CatalogLookupError` alternatives | Every identity/key payload is non-null | Closed public products retain typed expected lookup absence/conflict | Module lookup/null/equality and serialization tests |
| `CatalogRoot`; `CatalogState`; `CatalogSnapshot` | One opaque lineage; coherent revision/index generation; direct immutable lookup | Existing private permit owns issuance; only `CatalogRoot.create` is public construction | Module history/property tests, packaged authority checks, snapshot bytecode scan |
| `CatalogCommit.Unchanged`; `CatalogCommit.Published` | Outcome snapshot and unique non-empty delta are exactly model-issued | Existing permit owns construction; public Scala extractors and Java fields retain inspection | Module plus packaged Scala/Java construction negatives and model-issued positives |
| `CatalogTransition`; `CatalogModel.commit` | State, snapshot, revision, outcome, and additions belong to the same exact transition | Existing permit owns transition construction; the pure model is the sole issuer | Module/property/model probes, packaged Scala/Java cross-root negatives, live-contract suites |

Every row also retains fail-closed Java serialization. No caller-visible lineage, permit, handle, or new authority family
was added by remediation.

Final review-cycle-3 reconciliation retains 54 staged Proposal-2/remediation paths over unchanged parent HEAD
`7ee13adb7a3454379917a3c038a79229e7b38634`; the unstaged diff and untracked list are empty,
`git diff --cached --check` passes, no commit was created, and task 8.6 remains incomplete for fresh review.
