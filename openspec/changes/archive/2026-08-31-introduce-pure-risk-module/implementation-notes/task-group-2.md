# Task Group 2 — Narrow risk module and aggregate-retirement plan

## Physical boundary

`risk/` is now the non-empty `trading-risk` artifact rooted at `trading.risk`. Its production graph is:

```text
trading-quantities ──────────────┐
                                 ├──> trading-risk
trading-instrument-economics ────┘
```

The initial production owner is the focused `RiskIdentityError` family and its `PnlInstrumentMismatch` case. This is
the identity failure required by the next downside operation, not a marker or speculative facade. The artifact admits
Cats Core as a pure implementation dependency and test-only MUnit/ScalaCheck support. It has no direct order,
execution-scenario, fee-policy, application, runtime, effect, codec, persistence, stream, clock, transaction,
telemetry, or benchmark dependency.

The root build aggregates and tests `risk` after instrument economics. The adversarial project consumes its completed
JAR, and `riskCompilerClasspath` contains only completed upstream/risk JARs, their pure dependencies, and the narrow
Scala compiler support needed to compile external fixtures.

## Enforced dependency direction

The completed-JAR boundary suite proves:

- the quantities-only external classpath cannot import `trading.risk.RiskIdentityError`;
- the instrument-economics-only external classpath cannot import `trading.risk.RiskIdentityError`;
- the risk-only external classpath contains the quantities, reference-data, instrument-economics, and risk JARs, but
  none of the order, scenario, transitional economics, application, runtime, Cats Effect, FS2, Circe, Doobie,
  OpenTelemetry, or JMH artifacts;
- an external risk client compiles and runs against completed JARs; and
- compiled risk class bytes contain no references to downstream owners, concrete effects, streams, codecs,
  persistence, clocks, transactions, telemetry, or the benchmark harness.

No production project gains a reverse dependency on risk in this group. Root aggregation is non-linking, and the only
new downstream consumer is the adversarial test project.

## Transitional aggregate retirement plan

The transitional `economics/` project remains intact until its behavior has an intentional owner:

1. Group 3 adds exact downside measurement in `risk` and its focused tests without changing fee/scenario behavior.
2. Groups 4–7 add the private assessment/model boundary, constructive curve algebra, logarithmic primary sizing, and
   explicitly linear arbitrary fallback entirely inside `risk`.
3. Group 8 first migrates arbitrary callback call sites to the exhaustive operation, then migrates only proven fixed
   cases to constructive models.
4. Risk-owned production, tests, examples, and completed-JAR fixtures move to `risk`; fee-policy production and its
   scenario composition tests move to their honest downstream integration owner without making risk depend on them.
5. Only after `economics/` is empty, remove its SBT project, artifact, root aggregation, exported-product classpath,
   README, and combined compiler fixture. No forwarding package, umbrella facade, or compatibility alias remains.

The benchmark project remains unchanged in this group; Group 6 adds a benchmark-only dependency from benchmarks to
risk after the sizing API exists. Risk never depends on JMH.

## Checks

- `sbt -batch "risk/Compile/scalafmt" "adversarialBoundary/Test/scalafmt" scalafmtSbt` — pass.
- `sbt -batch "risk/compile" "adversarialBoundary/testOnly external.RiskCompilerBoundarySuite external.QuantityArtifactBoundarySuite external.EconomicsCompilerBoundarySuite"` — pass, 41 tests.
