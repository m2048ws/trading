# Task Group 2 — Fee-policy module boundary

## Finalized module coordinates

| Coordinate | Value |
| --- | --- |
| SBT project | `feePolicy` |
| Directory | `fee-policy/` |
| Published artifact | `trading-fee-policy` |
| Public package root | `trading.fee` |
| Current provisional subpackage | `trading.fee.policy` |
| Production internal dependencies | quantities, instrument economics, order model, execution scenario |
| Pure external dependency | Cats Core |
| Test-only downstream dependency | risk |

The existing non-empty project and artifact are retained; no duplicate module or placeholder package was created. The
root project already aggregates `feePolicy` and sequences its tests after execution scenario and risk. Group 2 adds the
accepted explicit quantities edge because later public policy mathematics consumes refined typed quantities directly.
Instrument economics, order model, and execution scenario remain the other direct production edges.

SBT's evaluated `feePolicy / Compile / internalDependencyClasspath` contains exactly the required internal production
graph: quantities, reference data transitively through the core, instrument economics, order model, and execution
scenario. It does not contain risk. `feePolicy / Test / internalDependencyClasspath` adds the completed fee-policy and
risk products, preserving the existing downstream integration coverage without creating a production edge.

The documentation now identifies `trading.fee` as the artifact package root while stating that the S-03 provisional API
remains in its `trading.fee.policy` subpackage until Groups 3–8 replace it. No provisional fee mathematics, policy,
assessment, scenario valuation, PnL orchestration, or error API was changed in this boundary-only Task Group.

## Completed-JAR classpaths

Two immutable generated compiler classpaths close gaps in the earlier all-artifacts test classpath:

- the reference-data-only classpath contains one completed quantities JAR and one completed reference-data JAR plus
  their legitimate libraries and the Scala compiler support needed by the fixture; and
- the fee-policy production classpath contains one completed JAR each for quantities, reference data, instrument
  economics, order model, execution scenario, and fee policy plus their pure dependencies.

The fee-policy classpath explicitly excludes `trading-risk`, `trading-application`, `trading-runtime`, the retired
`trading-economics` artifact, Cats Effect, FS2, Circe, Doobie, OpenTelemetry, and JMH. The positive completed-artifact
client constructs an instrument, order assumptions, a scenario, denomination, percentage fee, and attributed fee line
against only that classpath and executes successfully. It therefore cannot be satisfied by hidden transitional classes
or the broad root test graph.

## Reverse-boundary compiler guards

| Compiled boundary | Fee-policy proof |
| --- | --- |
| Quantities | The completed quantities-only fixture names `trading.fee.policy.FeePolicy` and receives an independent missing-`trading.fee` diagnostic. |
| Reference data | A dedicated reference-data-only fixture compiles its prelude, then rejects the fee-policy type with no fee-policy JAR present. |
| Instrument economics | The completed core fixture independently rejects fee policy alongside order, scenario, and risk. |
| Order model | Isolated nested imports prove scenario, fee policy, risk, application, and runtime are each unavailable without diagnostic contamination from an earlier missing import. |
| Execution scenario | Its completed classpath independently rejects fee policy, risk, application, and runtime while retaining existing mutation/construction-authority checks. |
| Fee policy | Its dedicated production classpath rejects risk, application, runtime, effects, streams, codecs, persistence, telemetry, and benchmarks. |

The existing packaged-JAR ownership inspection also confirms that the fee-policy JAR contains its own classes but does
not package upstream instrument/order/scenario classes or downstream risk classes.

## Verification

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch feePolicy/test` | 11 passed, 0 failed |
| `sbt -batch adversarialBoundary/test` | 151 passed, 0 failed |
| Completed fee-policy positive fixture | compiled without warnings and executed |
| Lower/reverse negative fixtures | all rejected only at their intended forbidden expressions |

The first focused run exposed one order-model fixture whose sequential wildcard imports caused the missing scenario path
to contaminate the fee diagnostic. Each forbidden import was isolated in its own nested object; the 38-test economics
compiler suite and the final 151-test adversarial gate then passed. The only runtime warning remained Scala's upstream
`sun.misc.Unsafe` terminal-deprecation warning.
