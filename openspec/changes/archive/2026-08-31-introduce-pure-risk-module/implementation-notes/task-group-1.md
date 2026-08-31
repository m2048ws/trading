# Task Group 1 — Contract, dependency, and baseline gates

## Authority and prerequisite reconciliation

- Delivery: `RFC-0002-architecture-portfolio/S-03-pure-risk`
- Change: `introduce-pure-risk-module`
- Issue: `#8`
- Run: `run-a29691ac-e873-47ae-aa50-f27dff8c0381`
- Planning revision: `sha256:7d49085118e7b0083c8262dd38e09f1e796325df9e738cbe2b5513f352ed33c1`
- Source digest: `sha256:1a8cca449f8fdfc009f8ccbe87989a288ea92e7ff41ee58d38aa513a53ed4f2d`
- Traceability digest: `sha256:caaafeb6cd65026adcb4ea8f5f33d561ffce0ba8e6699d2301b57a5c510c4ab6`
- Planning baseline commit: `9d9b370f1c55fe8fe8ca2a1eab60a8a423323a1a`
- Integrated main parent: `cf210964f62f10602be0551682abda3ce7d24fae`

The guarded claim adapter confirmed the native GitHub dependency graph is closed. The planning baseline is a
single-parent commit directly on current `origin/main`, so it retains the archived instrument-economics prerequisite,
RFC S-01 application/runtime boundary, and RFC S-02 order/execution-scenario boundary without copying or hiding their
ancestry. The finalized proposal remains strict-ready at the same planning revision after reconciliation.

An earlier unacknowledged Run had been claimed from a stale parent. Its attempted current-main merge could not satisfy
the Run Contract's one-parent atomic Task Group rule. With explicit human authorization, its ignored Run/CAS state and
checkpoint were preserved under `/private/tmp/s03-run-restart.TLy1JE`, the uncommitted merge was aborted, the branch was
fast-forwarded, and Propose/Claim created this fresh topology. No production source from that Run was retained or lost.

## Behavioral baseline

The reconciled production tree ran on OpenJDK 26.0.2, satisfying the JDK 25 minimum, with SBT 1.12.15 and Scala 3.8.4.
The fresh planning baseline changes only planning/checkpoint artifacts relative to that tested production tree.

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch scalafmtCheckAll` | pass | All production, test, build, and adversarial Scala sources are formatted. |
| `sbt -batch clean test` | pass | 820 tests passed and none failed. |
| Quantity/reference/application/runtime | pass | 601 quantity, 13 reference-data, 9 application, and 18 runtime tests passed. |
| Pure economics/order/scenario | pass | 13 instrument-economics, 7 order-model, 10 execution-scenario, and 10 transitional downstream tests passed. |
| Completed-JAR and adversarial boundaries | pass | 139 compiler, Java, package-spoof, provenance, and runtime-boundary tests passed. |
| `sbt -batch "benchmarks/Jmh/compile"` | pass | Existing non-published JMH sources and generated harness compiled. |

The JDK emitted only the upstream Scala `sun.misc.Unsafe` terminal-deprecation warning. It did not affect compilation
or tests.

## Production ownership inventory

Both transitional production files currently live under `economics/src/main/scala/trading/risk/` and will move to the
final `risk` artifact.

| Current concept | Current behavior | S-03 action |
| --- | --- | --- |
| `Risk[I]` | Stores `FeePolicy[I]` and exposes its instrument. | Remove the policy-owned service wrapper; use explicit instrument inputs and pure risk values. |
| `Risk.downsideRisk` | Checks PnL identity and computes exact refined `max(0, -netPnl)`. | Preserve the mathematics as focused `Risk.downside(instrument)(pnl)`. |
| `Risk.maxLots` | Accepts a raw budget, scenario callback, fee schedule, and linearly visits `1..cap`. | Split into constructively monotone primary sizing and separately named exhaustive fallback. |
| `RiskError` | Combines identity, lot, scenario, fee-policy, and sizing-shape failures. | Replace with focused risk identity/model errors plus caller-owned located exhaustive failures. |
| `Option[instrument.Lots]` | Uses absence for no affordable positive lot. | Replace with a closed no-affordable/selected decision retaining boundary assessment. |

The current callback can construct a different round trip at each coordinate, validates scenario identity and held
position size, asks fee policy for PnL, and compares raw coefficients. It cannot establish a fixed total monotone model,
so no arbitrary callback will be silently admitted to the primary API.

## Current call-site classification

All runtime risk uses occur in `economics/src/test/scala/trading/DownstreamEconomicsSuite.scala`; one completed-JAR
client uses downside measurement only. There is no production application call site.

| Current use | Classification | Migration |
| --- | --- | --- |
| No-fee losing round trip and direct `downsideRisk` | Exact downside measurement | Move to focused risk tests using explicit instrument and core PnL. |
| Fixed entry 100 / adverse exit 90 with no fees for every lot | Constructively monotone affine | Express as an exact affine loss model and compare primary selection with the current result. |
| Visit-recording variant of the same fixed round trip | Constructively monotone affine | Replace linear-visit expectation with logarithmic distinct-observation evidence. |
| Exit 90 only at lot 2 and 101 at other lots | Explicitly exhaustive | Preserve as a deliberately non-monotone fallback regression. |
| Callback failure at lot 2 | Explicitly exhaustive | Preserve the exact coordinate and typed cause through the located fallback failure. |
| Fixed adverse exit plus one flat fee schedule | Constructively monotone after fixed inputs are compiled | Represent the fixed loss/fee shape algebraically; keep fee-policy evaluation outside risk. |
| `CompleteEconomicsClient` packaged downside call | Exact downside measurement | Move to the risk completed-JAR positive fixture. |

No existing call site supplies a reusable complete table. Complete-table validation remains an explicit checked path
for future opaque finite observations; it will not be manufactured from arbitrary callbacks.

## Source, build, documentation, and fixture migration inventory

| Surface | Current owner | Intended action |
| --- | --- | --- |
| `economics/src/main/scala/trading/risk/{Risk,Error}.scala` | Transitional economics | Move/rewrite under `risk/src/main/scala/trading/risk/`. |
| `economics/src/main/scala/trading/fee/policy/` | Transitional economics / future S-04 | Leave behavior unchanged; move integration tests to their honest owner before deleting the aggregate. |
| `DownstreamEconomicsSuite` risk cases | Transitional integration suite | Split focused risk laws into `risk`; retain fee/scenario integration outside risk production. |
| `economics-compiler/positive/CompleteEconomicsClient.scala` | Combined completed-JAR fixture | Add an isolated risk positive fixture and update combined coverage after aggregate retirement. |
| `economics-core-compiler/CoreHasNoDownstream.scala` | Lower-boundary negative fixture | Replace the old risk reference with a risk-unavailable assertion against the completed core classpath. |
| `EconomicsCompilerBoundarySuite` | Adversarial test owner | Add a completed risk JAR classpath and explicit forbidden-import/spoof probes. |
| `build.sbt` economics project, root aggregation, classpaths | Build | Add `risk`; later remove `economics` after every source/test has an intentional owner. |
| `benchmarks` | Non-published performance evidence | Add benchmark-only risk dependency and focused risk benchmarks in Group 6. |
| root/economics README and architecture audit | Documentation | Replace transitional ownership statements when the physical boundary exists and retires. |

The final risk artifact may depend only on quantities and instrument economics. It must not import or depend on order,
execution scenario, fee policy, application, runtime, codecs, effects, catalogs, persistence, concurrency, streams,
clocks, transactions, telemetry, or the benchmark harness.
