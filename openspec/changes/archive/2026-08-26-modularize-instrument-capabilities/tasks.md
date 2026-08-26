## 1. Baseline and Extraction Boundary

- [x] 1.1 Record the committed `Instrument` public surface and run the focused economics plus packaged compiler-boundary suites before structural edits, so path ownership, package-spoof rejection, diagnostics, and runtime behavior have a current baseline.
- [x] 1.2 Inventory every nontrivial `InstrumentImpl` workflow under exactly one of `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, or `sizing`; identify the minimal authority-neutral plan or private construction callback for each workflow before moving it.
- [x] 1.3 Establish the trusted-shell guard: keep `Instrument.create`, `InstrumentImpl`, registered-witness casts, nested sealed implementations, private-implementation downcasts, reference-identity checks, and final genuine-value construction inside `Instrument.scala`.

## 2. Price and Market Engines

- [x] 2.1 Create `InstrumentPrices.scala` and move exact selection, quantization decisions, coordinate validation, and price observation workflow behind an authority-neutral price engine while the root adapter retains typed grid ownership and final `Price` construction.
- [x] 2.2 Create `InstrumentMarket.scala` and move settlement-anchor derivation, conversion-set validation, coherence checks, and conversion lookup planning behind an authority-neutral market engine while the root retains path-owned `MarketState`/`SettlementConversions` construction and typed `Quantity`/`Rate` manufacture.
- [x] 2.3 Run focused price and market tests and compare success values, residuals, validation precedence, and typed diagnostics with the baseline before removing the corresponding logic from `PriceMarketRules`.

## 3. Order and Scenario Engines

- [x] 3.1 Create `InstrumentOrders.scala` and move visibility, activation, price-instruction, and checked/convenience order workflow into the order engine, returning neutral construction plans for the root's sealed owned values.
- [x] 3.2 Create `InstrumentScenarios.scala` and move activation evidence, peg resolution, slice aggregation, effective-limit checks, position-change derivation, and round-trip validation into the scenario engine while genuine scenario wrappers remain root-private.
- [x] 3.3 Run focused order/scenario tests and verify the same invalid combinations fail in the same order with the same diagnostics before removing `OrderScenarioRules`.

## 4. Fee, Valuation, and Sizing Engines

- [x] 4.1 Create `InstrumentFees.scala` and move minimum-charge arithmetic, quantization planning, fee attribution checks, and schedule composition into the fee engine while asset/grid witness casts and sealed `Fee`/`FeeLine` construction remain in the trusted shell.
- [x] 4.2 Create `InstrumentValuation.scala` and move settle-per-position, position value, price PnL, schedule assessment, fee-conversion orchestration, and net-PnL planning into the valuation engine while typed settle quantities and sealed PnL values remain root-owned.
- [x] 4.3 Create `InstrumentSizing.scala` and move downside-risk calculation plus exhaustive ascending candidate selection into the sizing engine without changing non-monotone selection, scenario mismatch checks, or deterministic first-failure behavior.
- [x] 4.4 Run focused fee, valuation, property, and sizing tests and compare exact coefficients, attribution, failure order, and evaluated candidate behavior with the baseline before removing `FeeValuationSizingRules`.

## 5. Trusted Shell and Source Readability

- [x] 5.1 Reorder `Instrument.scala` into public contract, validated construction, private owner state, stable capability adapters, and trusted leaf representations; leave no complete capability workflow inline when it can use an established neutral plan/callback seam.
- [x] 5.2 Compare the finished public trait declaration and packaged client surface with the committed baseline; restore any accidental change to public names, signatures, defaults, sealing, direct `instrument.X` paths, or capability stability.
- [x] 5.3 Audit all new package-visible signatures and the final source tree: no engine extends `Instrument` or an owned sealed interface, no genuine-value factory or private callback escapes, no registered-witness/private-implementation cast moved out of the root, and no generic capability framework or redundant rule layer remains.
- [x] 5.4 Retain existing aggregate/property coverage and add focused tests only where a new neutral plan seam contains validation or failure-order logic not directly distinguished by the aggregate suites.

## 6. Validation and Independent Review

- [x] 6.1 Format production and test sources and run focused economics tests plus `EconomicsCompilerBoundarySuite`, including the complete positive client and cross-instrument, private-construction, removed-flat-API, and same-package-spoof negatives under strict compiler settings.
- [x] 6.2 Restore `quantities / Test / internalDependencyClasspath` to SBT's ordinary same-project `Compile / classes` wiring, retain `quantitiesExternalArtifact` for downstream consumers, make `economics / Compile / packageBin` depend directly on `economics / Compile / compile`, verify both external artifact consumers, and run one authoritative unretried `sbt -batch clean test`, followed by `scalafmtCheckAll` and `scalafmtSbtCheck`, without changing module, dependency, or publication topology or relying on retry/timing behavior.
- [x] 6.3 Run strict OpenSpec validation, inspect the complete diff for behavioral/API/build drift, stage every intended source/test/OpenSpec change, and confirm no unexpected unstaged or untracked files remain.
- [x] 6.4 Obtain fresh independent review of the fully staged change, including explicit falsification of public-surface preservation, path ownership, package-spoof resistance, authority confinement, diagnostic/failure-order preservation, source responsibility boundaries, and ordinary quantities main-to-test classpath restoration; this task is completed only during finalization after fresh approval and never by implementation or remediation.
