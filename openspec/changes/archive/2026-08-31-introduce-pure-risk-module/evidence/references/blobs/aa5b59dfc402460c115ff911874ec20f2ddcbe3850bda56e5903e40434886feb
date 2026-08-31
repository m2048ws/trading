# Task Group 9 — Verification evidence and Corgi handoff

## Planning integrity

`corgispec ready introduce-pure-risk-module --strict --json` reports `ready` at planning revision
`sha256:7d49085118e7b0083c8262dd38e09f1e796325df9e738cbe2b5513f352ed33c1`. All deterministic checks pass:
the proposal, design, delta spec, and unique task artifact are complete; strict OpenSpec validation succeeds; all nine
Task Groups have stable unique IDs; no placeholders or open questions remain; capability names match the delta; and
the RFC contract, accepted source, branch ancestry, Slice binding, Issue, and complete AC traceability are current.

The read-only semantic review of every CLI-reported planning artifact found no error, warning, or implementation drift.
Goals and non-goals agree with the requirements; success, failure, boundary, and scope scenarios cover the required
behavior; the design owns interfaces, validation, complexity, integration, migration, and exclusions; and Task Groups
map the behavior in dependency order. The readiness assessment changed no file.

## Clean validation matrix

Formatting checks pass for every Scala and SBT source. `sbt -batch clean test` then rebuilds the post-aggregate graph in
dependency order and passes 869 tests:

| Project | Tests |
| --- | ---: |
| quantities | 601 |
| reference data | 13 |
| application | 9 |
| runtime | 18 |
| instrument economics | 13 |
| risk | 40 |
| order model | 7 |
| execution scenario | 10 |
| fee policy and downstream risk integration | 11 |
| completed-JAR/compiler/adversarial boundary | 147 |

This covers exact/refinement laws, constructive model validation, algebraic monotonicity, primary-versus-exhaustive
reference comparisons, logarithmic observation bounds, the explicit arbitrary fallback, scenario/fee integration,
negative compilation, JVM construction authority, completed-product classpaths, and the repository-wide matrix.

The non-published JMH project compiles explicitly. A focused JMH 1.37 run on OpenJDK 26.0.2 with cap 1,024, one thread,
one fork, three one-second warmups, and five one-second measurements records:

| Benchmark | Throughput |
| --- | ---: |
| direct curve lookup | 5,759,312 ops/s |
| boundary-certified maximum | 345,771 ops/s |
| exhaustive reference evaluation | 3,358 ops/s |

The benchmark is observational evidence only; deterministic suites own correctness and the probe-count guarantee.

## Packaged boundary audit

The completed `trading-risk` JAR contains only `trading.risk` production classes. Its SBT project has direct production
dependencies only on quantities and instrument economics plus pure Cats Core; source and bytecode inspection find no
order, scenario, fee-policy, application/runtime, catalog, effect, stream, codec, persistence, telemetry, or JMH
dependency. Bytecode signatures mention reference assets and handles only through the instrument-economics public
types; risk has no direct reference import or catalog access.

`javap -p` confirms that `LotRiskAssessment` and `MonotoneLotRisk` constructors, the model evaluator, and the retained
formula are JVM-private. Public construction remains limited to checked PnL, closed algebraic, compatible composition,
and complete-table routes. Public primary sizing accepts only a validated model, while arbitrary function evaluation is
available only through the separately named exhaustive boundary and its distinct evidence/failure types.

Production source inspection finds no duplicate risk wrapper, unchecked monotonicity marker or promise, implicit
exhaustive overload, old candidate/service name, or umbrella forwarding API. Exact `Rational` use is confined to the
quantity-backed closed evaluator and comparisons; downside and public budgets retain typed quantity/refinement
evidence, with no raw-risk reconstruction boundary. The only remaining `trading-economics`, `instrument.sizing`, and
similar strings are explicit retirement documentation or negative compiler fixtures. No tracked `economics/` project
or broad artifact remains.

## Handoff boundary

This Task Group prepares the final Apply checkpoint only. Canonical whole-change Verify, explicit Human Review, Human
QA when applicable, and Archive remain separate gates. No application/runtime Slice or additional capability has
started in this delivery.

The automated Task Group review checked scope, RFC/AC coverage, test and benchmark evidence, packaged construction and
dependency boundaries, architecture, performance/security applicability, planning integrity, and gate separation with
no findings. It changed no file during the final pass, human-triaged no finding, and is not canonical whole-change
Verify or Human Review.
