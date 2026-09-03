# Task Group 5 — Risk model, sizing, and benchmark static access

## Implementation

Risk assessment, non-empty model violations, certified monotone models, model
observation, formula composition, and primary sizing now use ordinary Scala
calls. `LotRiskAssessment` owns direct companion construction;
`ModelViolations.of` is the narrow package operation that receives an already
non-empty head/tail; and `MonotoneLotRisk` directly constructs its private
representation and observes its private formula. Its `private[trading] assess`
member is the narrow checked observer shared by risk algorithms, tests, and the
benchmark without reflective setup.

`MaxAffordableLots` calls that observer directly. `RiskSizingBenchmark` does the
same for direct and exhaustive reference paths. The risk model/sizing imports,
lookup descriptors, method handles, invocation recovery casts, warning
annotations, and all three production/benchmark migration allowances were
removed.

Four casts remain in `Model.scala`, each adjacent to its established proof:
single-model assessment typing follows instrument identity plus position and
settlement dimension equality; composition typing follows identity/dimension/
cap compatibility; and complete-table lots/PnL casts follow per-row instrument
and dimension validation. None recovers a reflective result or substitutes for
semantic validation.

Hostile exact-class gates were removed from primary upper-bound/decision and
exhaustive cause/decision families. Their alternatives remain sealed and
exhaustively matched. Same-package model-construction, constructor-modifier, and
foreign Java alternative fixtures were removed; retained tests cover arbitrary-
function rejection, invalid curves/tables, compatibility, exact assessment,
monotonicity, exhaustive fallback, serialization, and boundary witnesses.

All checked construction predicates remain: assessment identity/dimensions,
closed-form and table coverage/monotonicity, exact quantization, deterministic
non-empty errors, compatible composition, exhaustive failure location, and
binary-search adjacency. Existing operation-count properties still enforce the
primary bound of `2 + ceil(log2(cap))` observations and equivalence with the
exhaustive reference.

The repository guard now reports 507 reviewed tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| `sbt -batch risk/scalafmtAll risk/Test/scalafmtAll benchmarks/scalafmtAll adversarialBoundary/Test/scalafmtAll risk/test 'adversarialBoundary/testOnly external.RiskCompilerBoundarySuite' benchmarks/Jmh/compile` | Pass: 40 risk tests, 6 completed-artifact checks, and JMH generation/compilation. An unrelated formatter-only benchmark change was reverted before commit. |
| Short JMH run, cap 1024, one 200 ms warmup/measurement | Pass: direct lookup 5,148,824.606 ops/s; boundary-certified maximum 322,669.350 ops/s; exhaustive reference 3,827.577 ops/s. Indicative only; operation-count properties are the deterministic complexity evidence. |
| `tools/check-in-process-reflection.sh` | Pass: risk and risk benchmark source are clean; 507 reviewed migration tokens remain elsewhere. |
| Risk/benchmark source scan for method handles, `private[this]`, and exact-class guards | Pass: none remain. |
| `git diff --check` | Pass. |

The structured review covered invariant ownership, visibility, retained-cast
proof adjacency, monotonicity and exactness, closed alternatives, binary-search
witnesses, complexity, benchmark validity, dependency scope, and removed-test
replacement. No critical or important finding remains. The review changed no
file itself, human-triaged no finding, and is not canonical whole-change Verify
or Human Review.
