## 1. Capability Boundary and Known-Dimension Inputs

- [x] 1.1 Add the instrument-owned sealed `Prices`, `Market`, `Orders`, `Scenarios`, `Fees`, `Valuation`, and `Sizing` capability interfaces and stable values while retaining identity, metadata, owned types, and primitive lot/position operations at the `Instrument` root.
- [x] 1.2 Implement `prices.exact`, `prices.fromRate`, `prices.fromTicks`, scalar and typed quantization, and price observers with identical positivity and grid-membership behavior across scalar and typed paths.
- [x] 1.3 Add scalar and typed explicit `SettlementConversion` construction when source and target asset references fix the endpoints, preserving registry, positivity, and endpoint diagnostics.
- [x] 1.4 Implement `market.quoteSettled`, `baseSettled`, scalar anchor constructors, typed-rate constructors, checked dual-anchor construction, and settle conversion lookup through one shared checked path.
- [x] 1.5 Add focused equivalence and failure tests for scalar versus typed price/conversion/market inputs, positive tick reconstruction, grid rejection, settlement identity, anchor coherence, and additional-conversion vector validation.

## 2. Capability-Local Domain Operations

- [x] 2.1 Move activation, price-instruction, visibility, checked-order, and market/limit/stop convenience construction behind `orders` with capability-local names and unchanged compatibility checks.
- [x] 2.2 Move activation evidence, peg resolution, liquidity-slice, complete order-scenario, and round-trip construction behind `scenarios`, accepting directly named matched-slice vectors and preserving every aggregate check.
- [x] 2.3 Move minimum-charge, fee quantization, percentage-fee, fee-line, no-fee, and component-schedule operations behind `fees`, retaining direct zero-or-many fee-line and schedule vectors.
- [x] 2.4 Move settle-per-position, position-value, price-PnL, and fee-inclusive-PnL operations behind `valuation` without changing formulas, attribution, conversions, residual visibility, or errors.
- [x] 2.5 Move downside-risk and exhaustive maximum-lot selection behind `sizing.downsideRisk` and `sizing.maxLots`, retaining deterministic failure and non-monotone evaluation semantics.
- [x] 2.6 Remove the superseded flat public operations after all internal callers use the capability surface; do not add permanent forwarding aliases or pass-through collection wrappers.

## 3. Instrument Implementation Readability

- [x] 3.1 Reorganize `Instrument.scala` so public owned-type/capability contracts, trusted private construction, and capability wiring are visually separated and documented by concern.
- [x] 3.2 Extract authority-free price, conversion, and market coefficient rules into concern-named private implementation helpers without exposing witness casts or trusted constructors.
- [x] 3.3 Extract authority-free order, trigger, peg, slice-total, and round-trip rules without changing validation order or typed diagnostics.
- [x] 3.4 Extract authority-free fee, valuation, risk, and candidate-traversal calculations without changing exact arithmetic, fee attribution, or deterministic sizing behavior.
- [x] 3.5 Audit every remaining root method against the placement rule and every extracted helper against the construction-authority boundary; keep sealed implementations resistant to same-package spoofing.

## 4. Caller Migration and Regression Coverage

- [x] 4.1 Migrate economics fixtures and behavioral/property suites to the capability-local API while preserving the existing spot-like, linear, inverse, quanto-style, order, fee, PnL, and sizing coverage.
- [x] 4.2 Update the end-to-end example to parse price text to exact `Rational` at an adapter-style boundary and then use `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, and `sizing` without reconstructing known dimension references.
- [x] 4.3 Update the positive packaged downstream compiler fixture to exercise the complete capability-oriented workflow through documented public imports.
- [x] 4.4 Extend negative compiler fixtures to reject superseded flat calls, cross-instrument capability mixing, private construction, and same-package-spoof attempts after helper extraction.
- [x] 4.5 Add direct-vector coverage for zero/many additional conversions, one/many matched slices, zero/many fee lines, and zero/many component schedules, including all owning aggregate failures.

## 5. Validation and Review

- [x] 5.1 Format production and test sources and run focused economics compilation and tests with strict warnings.
- [x] 5.2 Run the packaged downstream compiler-boundary suite, including capability positive fixtures and all ownership, removed-API, private-construction, and package-spoof negatives under its strict compiler settings.
- [x] 5.3 Diagnose both missing-TASTy failures against the actual SBT task graph: establish that `GridConstraint.tasty` and then `DivisionByZero.tasty` failed in quantities test compilation while consuming mutable main classes, that the latter occurred in an isolated SBT process and existed again afterward, and that the later packaged-consumer diamond is non-causal.
- [x] 5.4 Remove the non-causal packaged-consumer workaround and make only `quantities / Test / internalDependencyClasspath` consume the completed `quantities / Compile / packageBin` JAR; do not change quantities source, public APIs, artifact contents, global `exportJars`, dependent-project classpaths, or introduce retry, delay, hard-coded target paths, publication, polling, or repository-wide serialization.
- [x] 5.5 Verify that no competing SBT process targets the checkout, then run one authoritative unretried full clean multi-module test command on the corrected boundary and confirm the quantities artifact, economics artifact, and downstream compiler boundary are unchanged and green.
- [x] 5.6 Audit that the quantities test internal classpath uses the completed JAR and that it contains expected TASTy without economics classes; re-run focused economics and packaged compiler-boundary tests, formatting, strict OpenSpec validation, and complete staged-diff inspection for formula drift, authority leakage, compatibility aliases, unneeded wrappers, venue parsing in core, build-scope drift, or generated artifacts.
- [x] 5.7 Obtain a fresh independent review of the fully staged change and its validation evidence; this task is completed only during finalization after fresh approval, must be repeated after any remediation, and must not be self-certified by an implementation or remediation worker.
