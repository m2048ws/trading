# Actual execution lifecycle evidence

This page maps RFC-0003 Slice `S-01-actual-execution-lifecycle` acceptance criteria to reviewable repository evidence.
It records the final Apply gate; it is not canonical CorgiSpec Verify or Human Review.

## Acceptance criteria

| Criterion | Automated evidence | Human-readable evidence |
| --- | --- | --- |
| AC-001 — pure owner and one-way dependency boundary | `build.sbt`; `ExecutionLifecycleCompilerBoundarySuite`; completed-JAR positive/negative compiler fixtures | root/module architecture tables and the actual-versus-hypothetical comparison in the module guides |
| AC-002 — stable scoped identities, replay, conflicts, and provenance | `ExecutionIdentitySuite`; `ExecutionAuthoritySuite`; `CommandStateSuite`; `SourceFactSuite` | `execution-lifecycle/README.md` authority rules and the completed-JAR client example |
| AC-003 — deterministic transition, ordering, completeness, and reconciliation | `ExecutionStateSuite`; `SourceFactSuite`; `EffectiveFillLedgerSuite` | module guide explanation of authoritative versus unsequenced facts, replay, exact known exposure, and incompleteness |
| AC-004 — honest submission knowledge and recovery | `SubmissionKnowledgeSuite`; completed-JAR exhaustive alternative match | retained-evidence explanation in the module guide and domain-readable checked paths in the completed-JAR client |
| AC-005 — exact fills, cancel races, anomalies, and lineage without native amend | `EffectiveFillLedgerSuite`; `CancellationSuite`; native-amend and JVM construction negative fixtures | exact correction/bust/race example and non-goals in the module guide |

The final formatting, clean compilation, aggregate test, and benchmark-compilation gates were:

```text
sbt -batch scalafmtCheckAll scalafmtSbtCheck
sbt -batch clean compile
sbt -batch test benchmarks/Jmh/compile
sbt -batch "quantities/test; referenceData/test; application/test; runtime/test; instrumentEconomics/test; \
  orderModel/test; executionLifecycle/test; executionScenario/test; feePolicy/test; risk/test; \
  adversarialBoundary/test"
```

They passed 988 tests in dependency order: 601 quantities, 13 reference data, 9 application, 18 runtime, 13 instrument
economics, 40 risk, 7 order model, 66 execution lifecycle, 16 execution scenario, 38 fee policy/integration, and 167
completed-JAR/compiler/adversarial tests. JMH sources compiled explicitly after the clean aggregate gate.

Representative operation evidence is retained in `ExecutionStateSuite`: an ordinary indexed fill insertion reports two
lookups, two index updates, and zero full-history scans; exact duplicate detection reports two lookups, zero updates,
and zero full-history scans. Replay, delivery-permutation, correction/bust, gap/rewind, source conflict, completeness,
submission recovery, cancel race, and lineage behavior run in the complete suite.

Packaged inspection confirms JVM-private constructors for trusted concrete representations, closed alternatives guarded
against unknown JVM subclasses, Java serialization rejection, no native amend/cancel-replace member, and no downstream
scenario, fee, risk, application, runtime, codec, effect, stream, client, persistence, telemetry, or venue-SDK import or
public signature. The packaged production classpath contains only instrument economics, quantities, reference data,
order model, Scala, and the admitted Cats/Algebra mathematical dependencies.
Strict OpenSpec validation and all deterministic CorgiSpec readiness checks pass at planning revision
`sha256:1cdf58a7f0094cce60e35d17849b37458553f7e8359f2e3a791d91140fc23c19`.
