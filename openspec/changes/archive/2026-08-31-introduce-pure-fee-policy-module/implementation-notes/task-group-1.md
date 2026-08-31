# Task Group 1 — Contract, dependency, and baseline gates

## Authority and prerequisite reconciliation

- Delivery: `RFC-0002-architecture-portfolio/S-04-fee-policy`
- Change: `introduce-pure-fee-policy-module`
- Issue: `#9`
- Run: `run-f72899aa-a667-4f01-8c8c-22d2fc1d784a`
- Planning revision: `sha256:b2226bdaf8f713cf48bb49f599da6b5cd84462cb96d8f028a6263caed76c4158`
- Source digest: `sha256:d7face4865eb2e8e69abb4f305d67720842e19f9d1d4fa3b760b744fb8f06443`
- Traceability digest: `sha256:8ef137a161b86ab60b78dadf55c70e91b8727572212054d1d344ec878057f064`
- Planning baseline commit: `cb1b7109257e7ddf3fa29fb0051862971352f2fd`
- Integrated main parent: `0b7cc676054f8b9680174fce84f280d863e1a409`

The accepted RFC source binds exactly AC-013 through AC-016. GitHub Issues `#7` for
`S-02-order-execution-scenarios` and `#8` for `S-03-pure-risk` are closed. The guarded claim adapter confirmed the
native dependency graph, expected branch and registered worktree before claiming this Run. The planning baseline is a
single-parent commit directly on current `origin/main`, so it includes both delivered prerequisites and the provisional
fee-policy artifact established by S-03 without copying or obscuring their ancestry.

The original S-04 plan was reconciled against that delivered baseline before production work: it now finalizes the
existing `feePolicy` project and replaces its provisional API rather than creating another module or referring to the
retired transitional economics artifact. Strict Corgi readiness passes at the planning revision above, and Run Contract
v3 makes Group 1 the sole current Task Group. Relative to `origin/main`, the planning commit changes only planning,
delivery-sidecar, and session-checkpoint artifacts.

## Behavioral baseline

The reconciled production tree ran on OpenJDK 26.0.2, satisfying the JDK 25 minimum, with SBT 1.12.15 and Scala 3.8.4.

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass | Production, test, adversarial, and SBT sources are formatted. |
| `sbt -batch clean test` | pass | 869 tests passed and none failed after clean dependency-order compilation. |
| Quantity/reference/application/runtime | pass | 601 quantity, 13 reference-data, 9 application, and 18 runtime tests passed. |
| Pure instrument/risk/order/scenario/fee policy | pass | 13 instrument-economics, 40 risk, 7 order-model, 10 execution-scenario, and 11 fee-policy tests passed. |
| Completed-JAR and adversarial boundaries | pass | 147 compiler, Java, package-spoof, provenance, effect, and runtime-boundary tests passed. |
| `sbt -batch "benchmarks/Jmh/compile"` | pass | Three benchmark sources and the generated 48-source JMH harness compiled. |

The JDK emitted only the upstream Scala `sun.misc.Unsafe` terminal-deprecation warning. It did not affect compilation
or tests.

The current fee/PnL regression behavior is explicit:

- a `1/1000` quoted rate on a USD 10 basis produces an exact USD `-1/100` charge, while a `-1/1000` rate produces an
  exact USD `1/100` rebate; the quantized amount plus residual equals the unrounded contribution;
- a USD `-1/1000` charge contribution with a USD `1/100` minimum becomes USD `-1/100`;
- a 100-to-110 round trip has exact price PnL 10, and one token-millis fee assessed on each leg converts at that leg's
  selected market to `-1/500` then `-3/1000`, preserving entry-before-exit order and producing fee PnL `-1/200` and
  net PnL `1999/200`;
- the no-fee 100-to-90 path returns net PnL `-10`, which the separate risk artifact maps to exact downside 10; and
- a missing third-asset conversion retains `Entry`, slice index `0`, and the core `MissingConversion` cause, while a
  fee line carrying an equal-looking market from another scenario is rejected as `ForeignScenarioLine(0)`.

## Production ownership inventory

| Concept | Current implementation and call sites | Final owner and migration |
| --- | --- | --- |
| Core fee value | `instrument-economics/Fee.scala` owns `FeeKind`, `FeeDenomination`, and exact signed `Fee`; core tests and completed-JAR fixtures exercise charge, rebate, zero, quantization, residual, identity, and provenance behavior. | Keep in instrument economics. Fee policy supplies exact typed contributions to these checked constructors and does not duplicate value, grid, or settlement authority. |
| Fee scalar and mathematics | `FeeRate`, raw-sign-checked `percentage`, and `minimumCharge` are methods on the provisional `FeePolicy[I]`. | Fee policy owns the nominal rate and formula helpers. Replace raw `Quantity` sign checks with refined `NonNegative[Quantity[D]]` inputs while preserving the core sign convention: negative `Fee` amounts are charges and positive amounts are rebates. |
| Policy strategy and composition | `FeeSchedule` is the extension trait; `FeePolicy[I]` is an instrument-bound service wrapper; `none` and `combine` stop at the first error and use one universal error type. | Fee policy owns the open covariant `FeePolicy[E, ...]`, non-empty `PolicyErrors[E]`, no-fee identity, and checked same-instrument composition with stable accumulation. No global unconditional policy monoid or effect algebra is introduced. |
| Requested attribution | `FeeLine` stores a raw `Int`, a fee, and caller-supplied `sourceMarket`; `line` resolves a slice immediately, while later validation uses reference equality to compare the supplied market with the indexed slice. | Fee policy owns refined `SliceIndex` and existential `FeeDirective` for requests, then `AssessedFee` and `ScenarioFees` for validated results. Only assessment may resolve the index to the actual immutable scenario slice; callers cannot construct final attribution or supply a market. |
| Scenario price normalization | Private `FeePolicy.scenarioSignedValue` traverses every matched slice, reconstructs signed position lots from side, values at that slice's market, and is called only by provisional `pnl`. Execution scenario currently owns only immutable `OrderScenario` and `RoundTripScenario`. | Execution scenario owns the fee-independent exact per-slice cashflow fold, `RoundTripLeg`, focused valuation errors, and the typed order-intent signed-position operation. Fee policy consumes that result without importing price normalization back into the core. |
| Core and fee-inclusive PnL | Instrument economics owns `PricePnl`, `SettledFeeContribution`, and `Pnl`. Provisional `FeePolicy.pnl` performs round-trip validation, price aggregation, both schedule assessments, conversion, and final core construction. | Keep all core PnL values and constructors in instrument economics. Fee policy owns only scenario-level policy selection, assessment, attributed conversion, and the fee-inclusive result, invoking `Pnl.create` after all prerequisites succeed. |
| Risk integration | Risk production imports only quantities and instrument economics. Fee policy has a test-only risk dependency, and its integration suite feeds provisional PnL into monotone and exhaustive sizing examples. | Keep risk production unchanged and independently releasable. Migrate joint examples to honest downstream/test-only integration surfaces; do not move fee policy, scenario traversal, or custom policy errors into risk. |
| Errors | `FeePolicyError` mixes policy identity, raw basis, attribution, scenario-market reference, core fee, valuation, contribution, and PnL failures into one closed hierarchy. | Fee policy owns separate generic policy, assessment, and fee-inclusive violation aggregates with stable locations and typed causes. Execution scenario owns price-normalization errors. Existing core fee, valuation, contribution, and PnL errors remain core-owned and are retained as causes instead of flattened. |

## Test, fixture, example, and documentation inventory

There is no application or runtime production call site for fee policy or fee-inclusive PnL. Current downstream usage is
limited to the provisional fee-policy production implementation and the following test/documentation surfaces.

| Surface | Current coverage | Migration |
| --- | --- | --- |
| `FeePolicyIntegrationSuite` | Eleven tests cover scenario diagnostics, round trips, percentage/minimum math, fee conversion and ordering, no-fee PnL, attributed failures, risk sizing, composition, and foreign-market rejection. | Split into focused fee math, policy laws, assessment, price-normalization, fee-inclusive PnL, and downstream risk integration tests under their primary owners. |
| `DownstreamFixtures` | Constructs the resolved instrument, orders, scenarios, fee assets/grids, and market states used by the integration suite. | Retain only genuinely shared pure fixtures; locate scenario, policy, and risk-specific builders with the tests that own them. |
| `economics-compiler/SharedEconomicsSetup.scala` | Supplies a completed-artifact fixture and constructs the provisional `FeePolicy(instrument)`. | Update to the final public policy/assessment API without weakening its completed-JAR boundary. |
| `economics-compiler/positive/CompleteCompositionClient.scala` | Compiles and runs order → scenario → fee schedule → fee-inclusive PnL → risk downside against packaged artifacts. | Migrate to final policy/directive/assessment/PnL calls and retain the explicit one-way dependency proof. |
| Compiler negative fixtures | Core, order, execution-scenario, and risk classpaths reject downstream fee-policy imports; removed flat APIs and unlawful algebra are also rejected. | Preserve and extend these guards for the final package/API, construction authority, no global policy monoid, no reverse dependency, and no effect wrapper. |
| `build.sbt` and root CI | The root aggregates the existing `feePolicy` project; adversarial tests consume its exported product and completed dependency classpaths; CI runs format, clean tests, and JMH compile. | Finalize the existing project, add only the narrow direct dependencies required by public types, and extend completed-JAR classpaths/guards without creating another artifact. |
| Root and module READMEs | Describe the provisional physical owner and explicitly defer semantic redesign to S-04. | Replace provisional wording after the final API exists and document the final one-way module and ownership boundaries. |

No file is scheduled to move or be deleted until its replacement owner and boundary tests exist in its designated Task
Group. Policy acquisition, account/clock/version selection, audit envelopes, execution reports, codecs, catalogs,
effects, concurrency, persistence, telemetry, and runtime state have no current fee-policy production call sites and
remain outside S-04.
