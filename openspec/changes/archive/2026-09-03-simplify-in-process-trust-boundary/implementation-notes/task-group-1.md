# Task Group 1 — Trust contract, inventory, and regression guard

## Authority and baseline

- Delivery: `RFC-0004-simplify-in-process-trust-boundary/S-01-simplify-in-process-trust-boundary`
- Change: `simplify-in-process-trust-boundary`
- Issue: `#36`
- Run: `run-60752c8c-717b-4c6b-9307-ae6faecd9ae3`
- Planning revision: `sha256:559601683cb10e23a63595429e066972c911a1ebf5de25ec244b5cb28d7addd1`
- Source digest: `sha256:9c8d459b6066675a3128ef4974967a1f199bbd3b0e4871179110e0fe57337880`
- Traceability digest: `sha256:31952a7384c61bba43d4c9498a68703047d3a93902c05c7e328122087bae5dc2`
- Planning baseline commit: `e46eaaba970aeba2d9b4e9aa0239082837940823`
- Integrated `origin/main`: `b8cb04bfe19c5681621ab7066ac1473875f341a5`

Strict readiness passed before claim. The registered worktree is
`.worktrees/simplify-in-process-trust-boundary` on
`corgi/simplify-in-process-trust-boundary`; native dependencies are closed and
the pilot admits this change. Group 1 changes documentation and regression
infrastructure only, so it has no runtime behavior impact.

## Dynamic-access inventory

The deterministic baseline contains 623 forbidden-token occurrences in 26
production/benchmark files. Counts are deliberately mechanical and include
imports, descriptors, lookup declarations, and invocation sites; the semantic
classification below is the reviewed ownership inventory.

| Owner/files | Classification | Invariants retained by the migration |
| --- | --- | --- |
| `order-model/Order.scala` | Checked construction and derived evidence/state | Positive lots/prices/offsets; activation and pricing instruction agreement; trigger satisfaction; order aggregate validation and position change |
| `execution-scenario/Scenario.scala` | Checked construction and derived evidence/state | Positive slices, non-empty matches, associated order evidence, instrument/grid/market consistency, ordering, and round-trip flatness |
| `fee-policy/Assessment.scala`, `FeeInclusivePnl.scala` | Field-valid products plus checked derived aggregates | Denomination/instrument identity, exact grid value/residual, ordered non-empty attribution, source leg/state, and settlement composition |
| `risk/Model.scala`, `Sizing.scala` | Checked construction plus checked observation | Assessment identity/dimension, model coverage and monotonicity, compatibility, exact quantization, deterministic violations, and sizing witnesses |
| `benchmarks/RiskSizingBenchmark.scala` | Observation of already valid risk values | The benchmark consumes the same checked model and exact observation as production sizing |
| `execution/Identity.scala` | Checked values and field-valid qualified products | Null/empty validation, source/account/native qualification, stable equality/hash, and provenance |
| `execution/Authority.scala`, `Ordering.scala`, `OrderLineage.scala` | Checked construction and derived evidence/state | Positive sequences, continuation/coverage, authoritative completeness, and distinct lineage links |
| `execution/Commands.scala`, `SubmissionKnowledge.scala`, `Cancellation.scala` | Checked values and derived evidence/state | Idempotent command identity/body, dispatch uncertainty, conflicts, cancellation confirmation, target/order/lineage scope, and race/anomaly semantics |
| `execution/SourceFacts.scala`, `SourceEvidenceState.scala` | Checked fact construction and pure state transition | Qualified identities, exact fill economics, duplicate/conflict classification, ordering, gaps/rewinds, references, and completeness |
| `execution/ExecutionState.scala`, `EffectiveFillLedger.scala`, `Lifecycle.scala` | Derived state/results and checked observation | Lifecycle transition predicates, correction/bust replacement, ambiguity, exact exposure, overfill/post-cancel anomalies, replay equivalence, and indexed lookup |
| `codec/RecordEnvelope.scala`, `DecodeLimits.scala`, `WireDiagnostics.scala` | Checked boundary records | V1 dispatch, nonnegative/bounded limits, path structure, and deterministic non-empty wire diagnostics |
| `codec/GridCoordinateRecords.scala`, `OrderRecord.scala`, `ScenarioRecords.scala` | Non-authoritative field-valid records plus checked reconstruction | Exact primitive/refinement parsing, coherent catalog identity, associated order/scenario shapes, and deterministic errors |
| `codec/CatalogJournal.scala` | Non-authoritative records plus derived replay authority | Contiguous revisions, publication/no-op/conflict rules, atomic batches, stable error context, and fresh replay lineage |

Across these files, 115 `private[this]` declarations support the dynamic access
scheme and 124 casts recover erased invocation results. Runtime exact-class
guards occur in order activation/pricing/visibility/execution/aggregate, risk
sizing alternatives, and execution command/fact/ordering/knowledge/state
families. Their hostile-only use is scheduled with the corresponding owner;
casts justified by a checked path-dependent identity remain in scope for review.

Test-only reflection was reviewed separately. Artifact class loading, bytecode
or API inspection for retained dependency/type/serialization contracts, and
compiler-fixture loaders may remain. Constructor-modifier, same-package spoof,
foreign-subclass, exact-class, or reflective factory fixtures are scheduled for
removal or semantic replacement in the owner groups and final adversarial
reconciliation.

## Guard design

`tools/check-in-process-reflection.sh` scans every Scala production and benchmark
source for method handles, lookup/descriptors, Java reflection imports, declared-
member lookup, and accessible-member overrides. The checked TSV records the
reviewed migration ceiling per file: an unlisted site or an increased count
fails immediately. Each owner group removes its row; Group 10 leaves the
allowance empty, at which point the same command proves zero production and
benchmark sites. Test sources are intentionally outside this source guard so
retained completed-artifact inspection remains possible.

## Verification and automated review

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck compile benchmarks/Jmh/compile` | Pass: all Scala/SBT formatting, every production module, and the JMH harness compiled. |
| `openspec validate simplify-in-process-trust-boundary --strict` | Pass. |
| `openspec validate --specs --strict` | Pass: all 17 capability specs. |
| `tools/check-in-process-reflection.sh` | Pass: 623 reviewed migration tokens and no count regression. |
| `sbt -batch 'adversarialBoundary/testOnly external.ConstructionAndProvenanceBoundarySuite'` | Pass: 4 least-trusted construction/provenance checks. |
| `git diff --check` | Pass. |

The structured review covered scope, documentation consistency, guard
determinism, baseline accuracy, architecture, behavior, and performance. It
found incorrect copied source/traceability and Git revisions in the first draft
of this note; those important findings were corrected before commit. No open
critical or important finding remains. The review changed no file itself,
human-triaged no finding, and is not canonical whole-change Verify or Human
Review.
