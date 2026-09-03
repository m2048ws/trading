# Task Group 3 — Execution scenario static construction

## Implementation

`Scenario.scala` now constructs liquidity slices, non-empty matched slices,
associated assumptions, evaluated scenarios, and round trips directly from
their owning companions. Their constructors remain companion-private. All five
method-handle lookups, descriptors, `private[this]` sites, and eight invocation-
only casts were removed; no production cast was added or retained in this owner.

The checked operations are unchanged. `LiquiditySlice.create` still accumulates
instrument identity failures for lots, market state, and price. `MatchedSlices`
still represents non-emptiness structurally and rejects an empty vector.
`ScenarioAssumptions.create` still checks activation-evidence and pricing-
resolution shapes before it retains the path-dependent values. Evaluation still
orders identity failures deterministically, sequences semantic checks from
identity coherence, validates exact lot totals and execution constraints, and
returns only verified activation, effective pricing, and derived signed
position change. Round-trip construction still checks both identities and exact
flatness and retains the entry position as held-position derivation.

Hostile same-package Scala/Java constructor fixtures and matched-slice bytecode
privacy assertions were removed. The independent module-boundary fixture still
checks downstream dependency absence and immutable fields/copy absence without
asserting constructor secrecy. Retained positive/negative fixtures cover
associated evidence shapes, erased Java assumptions validation, exhaustive
alternatives, exact valuation, identity/grid mismatch, empty slices,
equality/hash, serialization, and complete round trips.

The scenario migration allowance was removed; the repository guard now reports
571 reviewed tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| `sbt -batch executionScenario/scalafmtAll adversarialBoundary/Test/scalafmtAll executionScenario/test 'adversarialBoundary/testOnly external.EconomicsCompilerBoundarySuite'` | Pass: 16 scenario tests and 41 completed-artifact/compiler checks. |
| `sbt -batch feePolicy/test boundaryCodecs/test` | Pass: 38 fee and 95 codec downstream tests. |
| `tools/check-in-process-reflection.sh` | Pass: scenario source is clean; 571 reviewed migration tokens remain elsewhere. |
| Scenario source scan for method handles and `private[this]` | Pass: none remain. |
| `git diff --check` | Pass. |

The structured review covered semantic strengthening, path-dependent types,
validation sequencing/order, non-empty representation, removed-test
replacement, module direction, serialization, client compatibility, and
performance. Direct construction removes dynamic overhead and changes no
algorithm. No critical or important finding remains. The review changed no file
itself, human-triaged no finding, and is not canonical whole-change Verify or
Human Review.
