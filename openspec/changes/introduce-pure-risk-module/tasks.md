## 1. Contract, Dependency, and Baseline Gates

- [ ] 1.1 Confirm the effective RFC/source/traceability bindings, the externally owned instrument-economics prerequisite
  is merged and archived, and this package has been reconciled against its delivered boundary before production edits.
- [ ] 1.2 Refresh Git/source/build/Corgi state and record a clean baseline for formatting, compilation, exact/property tests,
  external-artifact compilation, and current downside/sizing behavior.
- [ ] 1.3 Inventory sizing, downside-risk, scenario-builder, fee-inclusive result, error, example, build, and test call
  sites; classify each current sizing use as constructively monotone, complete-table-checkable, or explicitly
  exhaustive.

## 2. Narrow Risk Module and Aggregate Retirement

- [ ] 2.1 Add `risk` in `risk/`, artifact `trading-risk`, package `trading.risk`, with direct production
  dependencies only on quantities, instrument economics, and agreed pure library/test dependencies.
- [ ] 2.2 Wire root aggregation, completed-JAR external-artifact compilation, adversarial/compiler-test classpaths, and
  integration-test dependencies.
- [ ] 2.3 Add compiler guards proving quantities and instrument economics cannot access risk, risk cannot access order,
  execution-scenario, fee-policy, application/runtime, codecs, persistence, streams, clocks, transactions, tracing,
  metrics, or effect libraries, and no downstream module gains a reverse dependency.
- [ ] 2.4 Plan source/test redistribution so the transitional `economics` project can be deleted once risk migration
  leaves it empty, with no umbrella production artifact or forwarding facade retained.

## 3. Refined Downside-Risk Mathematics

- [ ] 3.1 Implement downside risk from an explicit instrument and core `Pnl` as exact
  `NonNegative[Quantity[settle.D]]` using typed quantity negation, order, zero, and refinement operations.
- [ ] 3.2 Validate ordinary runtime `InstrumentId` correspondence before inspecting PnL and return a focused typed
  identity error for a foreign result.
- [ ] 3.3 Accept risk budgets only as existing nonnegative refined settlement quantities and remove raw-budget sign
  validation, `InvalidRiskBudget`, and redundant `RiskBudget`/`DownsideRisk` wrappers.
- [ ] 3.4 Add exact tests for negative, zero, and positive PnL; dimension/identity mismatch; no quantization or floating-
  point conversion; and compile-time/API tests proving a raw negative quantity is not an accepted budget.

## 4. Lot-Risk Assessment and Validated Model Boundary

- [ ] 4.1 Implement private-construction lot-risk assessments retaining one positive instrument-bound `Lots` value and
  exact refined downside risk; provide only model construction and a checked instrument/lots/`Pnl` smart constructor
  that derives downside internally, never a constructor accepting an unrelated risk claim.
- [ ] 4.2 Implement an opaque monotone lot-risk model capturing one `InstrumentId`, one settlement dimension, a positive
  cap, and a total assessment operation over the exact domain `1..cap`.
- [ ] 4.3 Define domain-owned non-empty construction violations for identity, dimension, domain coverage, duplicate or
  missing coordinates, breakpoints, marginal loss, downward boundaries, and incompatible curve composition.
- [ ] 4.4 Accumulate independent construction violations applicatively in deterministic source order, convert internal
  Cats structures at the public boundary, and expose no partially validated model.
- [ ] 4.5 Add API/spoof tests proving model and assessment constructors/certificates are library-controlled and callers
  cannot supply a boolean, cast, subtype, type-class instance, or unchecked token to certify an arbitrary function.

## 5. Exact Monotone Loss-Curve Algebra

- [ ] 5.1 Implement the minimal closed exact loss representation and a compact evaluator; normalize explicit piecewise
  inputs without expanding quantization steps, compositions, or the declared cap into a complete table, and do not
  duplicate instrument valuation, order, scenario, or fee-policy semantics.
- [ ] 5.2 Implement affine construction from exact signed first-lot loss and refined nonnegative additional-lot loss;
  derive downside by exact `max(0, loss)`.
- [ ] 5.3 Implement checked piecewise construction with ordered contiguous coverage, exact boundary values,
  nonnegative per-segment marginal loss, and no downward boundary jump.
- [ ] 5.4 Implement only proven closure-preserving pointwise composition required by the spec: compatible addition,
  minimum, maximum, and order-preserving uniform-grid quantization.
- [ ] 5.5 Implement complete finite-table construction from instrument-bound lots/`Pnl` observations that derives
  assessments, verifies exact coverage, identity/dimension coherence, and adjacent nondecreasing risk, then publishes a
  reusable monotone model; document its `O(cap)` validation cost.
- [ ] 5.6 Add exact examples for linear and inverse-contract-shaped loss, fixed/proportional/minimum/capped fees,
  changing-but-nonnegative tier slopes, order-preserving quantization, and nested nonnegative liquidity increments.
- [ ] 5.7 Add negative examples for negative marginal loss, downward tier boundaries, missing/duplicate coordinates,
  mixed identities/dimensions, and account/hedge curves whose total risk decreases with added lots.
- [ ] 5.8 Add law/property suites proving totality, exactness, identity preservation, `a ≤ b` implies `risk(a) ≤ risk(b)`,
  and monotonic closure for every public primitive and combinator.
- [ ] 5.9 Add construction-cost instrumentation proving affine and algebraically composed models inspect expression and
  explicit breakpoint structure rather than every coordinate through a large cap.

## 6. Boundary-Certified Maximum-Affordable Sizing

- [ ] 6.1 Implement the closed primary decision with explicit no-affordable and selected alternatives; retain the one-lot
  lower boundary, selected assessment, and either cap-reached or immediately-next-unaffordable evidence as applicable.
- [ ] 6.2 Implement exact integer binary search over a validated model by checking cap, one lot, and adjacent midpoint
  boundaries; construct lots and compare risk only through typed/refined operations.
- [ ] 6.3 Retain every probed assessment needed by the result, observe no lot coordinate twice, and enforce/test the
  `2 + ceil(log2(cap))` distinct-observation upper bound including `cap == 1`.
- [ ] 6.4 Keep primary selection total after model construction: expose no per-probe error, raw zero/fractional lot,
  nullable/`Option[Lots]` sentinel, or recomputed scalar-only result.
- [ ] 6.5 Add examples for no affordable lot, exact-budget equality, interior maximum, cap maximum, stepped plateaus,
  very large `BigInt` caps, and preservation of selected/boundary assessments.
- [ ] 6.6 Add generated-model tests comparing the logarithmic result with a simple exhaustive reference for every
  generated monotone finite curve and budget.
- [ ] 6.7 Add probe-count instrumentation and a large inverse-contract-shaped fixture with hundreds or more possible lot
  sizes to demonstrate that primary sizing does not linearly enumerate the range.
- [ ] 6.8 Extend the existing non-published JMH project with a benchmark-only dependency on `trading-risk`; measure
  representative direct curve lookup, boundary-certified maximum sizing, and exhaustive-reference evaluation with
  recorded JDK/fork/warmup/measurement parameters, while keeping probe bounds and correctness in deterministic tests.

## 7. Explicit Arbitrary Exhaustive Fallback

- [ ] 7.1 Implement a separately named exhaustive operation from positive `Lots` to typed `Pnl` evaluation, deriving
  downside internally and exposing no overload that can be confused with primary monotone selection.
- [ ] 7.2 Traverse positive counts in deterministic ascending order with a pure stack-safe constant-success-memory state,
  retaining the greatest affordable assessment when all required evaluations succeed.
- [ ] 7.3 On the first ascending evaluation, construction, identity, or PnL failure, return the exact lot coordinate and
  caller-owned typed cause and no partial affordability decision.
- [ ] 7.4 Give the fallback a distinct decision/evidence shape, document its `O(cap)` observation cost, and provide no
  cast or conversion from an arbitrary evaluator/result to the opaque monotone model.
- [ ] 7.5 Test deliberately non-monotone sequences, one-lot-unaffordable/later-affordable behavior, interior decreases,
  typed failures at several positions in separate runs, deterministic first failure, exact greatest selection, and
  large-cap stack safety.
- [ ] 7.6 Compare primary and exhaustive decisions over monotone fixtures while proving only primary exposes the
  logarithmic observation and adjacent-boundary contract.

## 8. Downstream Integration and Migration

- [ ] 8.1 Keep scenario and fee-policy construction out of the risk artifact; use only test-owned fixtures based on the
  merged baseline for fixed exact parameters, complete-table validation, and deliberate exhaustive evaluation, without
  depending on future order/scenario/fee Slice APIs.
- [ ] 8.2 Migrate any arbitrary sizing call sites present in the merged baseline through the explicit exhaustive fallback
  first so behavior changes are visible and no callback is silently certified as monotone; leave future Slice-specific
  adapters to their owning delivery.
- [ ] 8.3 Migrate representative fixed isolated-instrument cases to the constructive model only after tests establish
  fixed instrument, direction, valuation state, adverse exit, fee inputs, cap, and nondecreasing total downside.
- [ ] 8.4 Move sizing/downside production sources, tests, examples, and properties from the transitional `economics`
  project into `risk` or their actual downstream integration owner; update packages and imports.
- [ ] 8.5 Remove the old `Sizing` service, `instrument.sizing`, generic candidate evaluator/assessment names, raw
  coefficient comparisons, one-policy wrapper, `Option[Lots]` result, universal sizing errors, and compatibility
  aliases.
- [ ] 8.6 Delete the now-empty transitional `economics` SBT project/directory, published artifact, root aggregation
  entry, and external-artifact task after verifying every source/test has an intentional owner.
- [ ] 8.7 Verify current-position, account/portfolio risk, margin, liquidation, funding, market-data/policy acquisition,
  caching, concurrency, and audit/persistence envelopes remain explicitly outside this capability.

## 9. Verification Evidence and Corgi Handoff

- [ ] 9.1 Format all affected Scala/SBT sources and run clean compilation in dependency order after aggregate removal.
- [ ] 9.2 Run complete exact/refinement/unit/law/property/model suites, primary-versus-reference comparisons, fallback
  regressions, scenario/fee integration tests, negative compilation, completed-JAR external boundaries, adversarial
  tests, explicit JMH compilation and the focused risk benchmark, and the full repository validation matrix.
- [ ] 9.3 Inspect packaged APIs and production imports for forbidden reverse dependencies, effect types/libraries,
  catalog access, raw `Rational` reconstruction, duplicate risk wrappers, unchecked monotonicity, implicit exhaustive
  fallback, old candidate/service names, and umbrella forwarding APIs.
- [ ] 9.4 Confirm the current Slice still satisfies strict planning/source/traceability integrity and reconcile any
  implementation drift before the final Task Group checkpoint.
- [ ] 9.5 Prepare the final acknowledged Task Group commit and evidence for separate canonical Verify, human review,
  human QA, and Archive; do not begin the application/runtime Slice in this delivery.
