# Task Group 7 — Round Trips and Downstream Consumers

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `7` (`Round Trips and Downstream Consumers`)
- Starting state revision: `7`
- Previous acknowledged commit: `97bec2609af7e330739bc3d14c0c966d5a00c765`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered boundary

- `RoundTripScenario.create(instrument)(entry, exit)` validates ordinary scenario identity and delegates retained
  position closure to instrument economics' checked `PositionLots.combine` operation. Position-operation identity
  failures map into closed entry/exit position components rather than leaking the core string context.
- The combined position must equal the explicit instrument's exact flat `PositionLots` identity. A non-flat failure
  retains both original signed coordinates and performs no side inference, absolute-lot comparison, resizing, or raw
  coordinate addition in the scenario artifact.
- Successful long and short round trips retain both complete `OrderScenario` legs, including their assumptions and
  non-empty slices, and expose the entry intent's already-checked signed position as `heldPosition`.
- Transitional fee policy now consumes the scenario-owned `matchedSlices` projection directly. Percentage, minimum,
  slice valuation, attribution, contribution conversion, and PnL semantics are unchanged; the dedicated S-04 Slice
  still owns fee-policy redesign.
- Transitional risk continues to consume the new `RoundTripScenario` type without changing exhaustive candidate
  traversal, held-position agreement, fee-inclusive PnL, or downside-risk behavior. The economics production artifact
  now contains only `trading.fee.policy` and `trading.risk`; it defines no order/scenario aliases or forwarders.

## Test and compiler evidence

- `OrderScenarioSuite` covers exact long and short closure, retention of complete entry/exit assumptions, entry held
  position, unequal signed-coordinate rejection, foreign-leg rejection, and foreign retained-position rejection.
- The cross-instrument cases use package-level adversarial test setup to emulate malformed adapter/internal values;
  supported public construction remains cast-free and path-dependent.
- Downstream economics coverage preserves exact fee conversion and attribution, no-fee and fee-inclusive PnL,
  downside risk, exhaustive greatest-candidate sizing, and stable schedule composition.
- Completed-JAR inspection confirms the transitional economics JAR contains fee/risk classes and no instrument,
  order, or scenario production packages. Existing compiler fixtures continue to reject removed aggregate services,
  aliases, duplicated scenario inputs, and universal error paths.
- The first automated review identified missing explicit round-trip cross-instrument evidence. After adding foreign-leg
  and foreign-position cases and rerunning all checks, the second review reported no findings across requirements,
  behavior, architecture, code quality, coverage, and relevant performance/security axes. Neither review was a human
  decision or canonical whole-change Verify.

## Checks

- `sbt -batch scalafmtAll` — pass.
- Focused `OrderScenarioSuite` — pass, 7 tests.
- Focused `DownstreamEconomicsSuite` — pass, 8 tests.
- `sbt -batch scalafmtCheckAll clean test` after review remediation — pass: 601 quantity, 13 reference-data, 7
  application, 13 instrument-economics, 6 order-model, 7 execution-scenario, 8 downstream economics, and 126
  adversarial tests (781 total).
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
