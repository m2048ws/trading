## 1. Runtime Instrument Coherence

- [x] 1.1 Add one typed `InstrumentMismatch` diagnostic carrying context, expected `InstrumentId`, and supplied `InstrumentId`.
- [x] 1.2 Add one package-local, token-free helper for checking named runtime instrument identities and cover its matching and mismatching behavior.
- [x] 1.3 Inventory every multi-value economics boundary and record which values supply or derive runtime `InstrumentId`, avoiding identity fields on stateless alternatives.

## 2. Direct Instrument Values and Aggregate

- [x] 2.1 Replace owner-indexed lots and signed-position carriers with direct owner-free refined values retaining `InstrumentId`, count, and exact quantity.
- [x] 2.2 Replace the owner-indexed price carrier with a direct owner-free refined value retaining `InstrumentId`, positive ticks, exact coefficient, and endpoint-typed rate.
- [x] 2.3 Preserve positive lots/price construction, flat and signed positions, exact grid narrowing, explicit quantization, and refinement-loss behavior with focused tests.
- [x] 2.4 Replace the abstract `Instrument`/private implementation pair with one validated aggregate that preserves component, registry, grid, dimension, and nonempty-payoff validation.
- [x] 2.5 Wire focused final concern classes from the stable instrument without an owner authority and retain only aliases that materially improve downstream annotations.

## 3. Direct Order Model

- [x] 3.1 Replace owner-indexed activation and trigger subtypes with directly pattern-matchable immediate, fixed, and trailing alternatives, retaining positive trailing-offset validation.
- [x] 3.2 Replace owner-indexed pricing, visibility, and execution subtypes with direct limit/peg, displayed/hidden/iceberg, and market/priced alternatives.
- [x] 3.3 Replace owner-indexed intent and order carriers with direct owner-free values that retain or derive `InstrumentId`.
- [x] 3.4 Centralize final order checks for instrument coherence, market duration, iceberg size, activation/execution compatibility, and existing typed failure reasons.
- [x] 3.5 Preserve market, limit, stop-market, and stop-limit conveniences over the same primary order boundary and add direct case-pattern tests.

## 4. Market and Scenario Aggregates

- [x] 4.1 Replace owner-indexed settlement conversions and market states with direct values and retain exact positivity, registry, target, identity-anchor, duplicate-source, and conversion-coherence checks.
- [x] 4.2 Add runtime instrument checks to market construction without changing quote-settled, base-settled, scalar/rate, additional-conversion, or state-owned conversion behavior.
- [x] 4.3 Replace owner-indexed trigger evidence, activation/pricing assumptions, peg resolution, and liquidity slices with direct closed values.
- [x] 4.4 Replace owner-indexed scenario assumptions and order scenarios with direct values and preserve trigger, peg, lot-conservation, limit-quality, liquidity-role, and runtime instrument checks.
- [x] 4.5 Replace owner-indexed round trips with direct values and preserve flat closure, held-position derivation, and typed runtime rejection of cross-instrument legs.

## 5. Fees, Valuation, and Sizing

- [x] 5.1 Replace owner-indexed fee denominations, fees, fee schedules, fee lines, converted lines, and PnL with direct owner-free values retaining runtime `InstrumentId` where aggregation requires it.
- [x] 5.2 Preserve denomination registry/grid validation, exact quantization and residual conservation, charge/rebate signs, third-asset fees, tiers, minimums, and direct schedule composition.
- [x] 5.3 Remove opaque `AnyRef` fee-line scenario identity and retain explicit leg/slice attribution, index bounds, market-state association, and runtime instrument checks.
- [x] 5.4 Preserve exact universal price valuation and fee-inclusive PnL conversion while rejecting foreign runtime identities and missing or contradictory conversions.
- [x] 5.5 Remove owner parameters from sizing while preserving exhaustive candidate evaluation, callback failure propagation, candidate-lot verification, target-instrument verification, and non-monotone behavior.

## 6. Source Organization and Anti-Forgery Removal

- [x] 6.1 Collocate each concern's direct types, smart constructors, aggregate validation, and calculations in its concern file; reduce `Instrument.scala` to aggregate construction, minimal value entry points, and capability wiring.
- [x] 6.2 Collapse capability trait/private-implementation pairs that have no extension role into final concern classes and replace repeated constructor dependencies with the stable instrument or a smaller validated input.
- [x] 6.3 Remove `Instrument.Owner`, all owner parameters and mirrored owner-indexed aliases, `Instrument.OwnerAuthority`, authority forwarding, gate assertions, and gated abstract/private implementation pairs.
- [x] 6.4 Delete `JvmOwnerAuthority.java` and remove economics-only `JavaSerializationUnsupported` inheritance without modifying the quantities trait or any quantities usage.
- [x] 6.5 Remove economics-specific Java release configuration only if it is obsolete after deleting the Java gate, leaving Scala and repository-wide compatibility targets unchanged.
- [x] 6.6 Audit remaining long methods and parameter lists, introducing a cohesive request or aggregate only when the fields share reusable domain meaning, and verify there is no generic validation framework or replacement issuance kernel.

## 7. Client and Regression Migration

- [x] 7.1 Migrate economics fixtures, behavioral suites, property suites, and the end-to-end example to the direct owner-free API without compatibility aliases.
- [x] 7.2 Update the packaged positive economics client to exercise direct alternatives, focused concerns, generic dimension-safe values, and runtime instrument diagnostics.
- [x] 7.3 Delete economics authority bytecode audits plus hostile Java, same-package-spoof, private-carrier, and compile-time cross-owner fixtures whose requirements were removed.
- [x] 7.4 Replace compile-time cross-instrument fixtures with representative runtime mismatch tests for orders, scenarios, round trips, fees, valuation, and sizing.
- [x] 7.5 Retain compiler-negative refinement-loss and removed-API coverage, and verify quantities package-spoof, carrier, registry, serialization, and bytecode suites are unchanged.

## 8. Validation and Independent Review

- [x] 8.1 Format production and test sources and run focused economics compilation, behavioral tests, property tests, and the economics downstream boundary suite.
- [x] 8.2 Run the quantities and full adversarial/downstream suites to prove the economics trust change did not weaken quantities authority, provenance, or serialization behavior.
- [x] 8.3 Run one unretried full clean multi-module test command, formatting checks, strict OpenSpec validation, and staged/unstaged Git diff checks.
- [x] 8.4 Inspect the complete staged diff for formula drift, lost economic validation, scattered identity checks, owner/authority remnants, a replacement kernel, compatibility scaffolding, and unrelated quantities changes.
- [x] 8.5 Obtain a fresh independent review of the fully staged implementation and validation evidence; only finalization after approval may complete this task, and any remediation SHALL return to another fresh independent review.
