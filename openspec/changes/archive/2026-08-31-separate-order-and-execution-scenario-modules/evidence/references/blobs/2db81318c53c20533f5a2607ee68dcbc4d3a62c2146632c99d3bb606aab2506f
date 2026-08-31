# Task Group 4 — Intent and Order Validation

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `4` (`Intent and Order Validation`)
- Starting state revision: `4`
- Previous acknowledged commit: `fb592df37a721c2cf6021df9eeaffc294a662b75`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered boundary

- `OrderIntent.create(instrument)(side, lots, positionEffect)` is the sole public intent constructor. It validates the
  runtime lots identity and retains the exact signed `PositionLots` computed by instrument economics. Its constructor
  and representation copy are unavailable outside `trading.order`, so callers cannot forge a contradictory position
  change. `ReduceOnly` remains retained instruction data and performs no account-state enforcement.
- `OrderComponent` is a closed location vocabulary. `OrderViolation` owns local refinement, identity, and iceberg
  failures, while `OrderViolations` guarantees a non-empty, ordered result through `head`, `tail`, and vector
  projection. The former universal `OrderError`, free-form string locations, and repository-wide mappings are gone.
- `Order.create(instrument)(intent, activation, execution)` is the single accumulating final-order validator. It first
  orders all detectable identity failures by stable component ordinal; only coherent inputs proceed to independent
  iceberg-size and non-resting-duration checks. `createFirst` projects `violations.head` from that same result.
- `Order.market`, `limit`, `stopMarket`, and `stopLimit` construct the closed instruction alternatives and delegate to
  the canonical boundary. The transitional `Orders` owner service and instrument-owned construction path are removed.

## Test and compiler evidence

- `OrderInstructionSuite` checks buy/sell signed position changes, reduce-only retention, all four convenience
  constructors, simultaneous iceberg violations, fail-fast/head agreement, closed identity locations, stable ordering,
  and suppression of dependent iceberg checks after identity failures.
- The order-model compiler fixture rejects direct `OrderIntent` construction and copy-based position-change forgery in
  addition to the impossible instruction/evidence shapes established in Task Group 3.
- Downstream economics and external fixtures use only explicit-instrument `Order`/`OrderIntent` construction. The
  compiler boundary and packaged-JAR inspection prove that `Orders` is absent.

## Checks

- `sbt -batch scalafmtAll "orderModel/Test/compile" "executionScenario/Compile/compile" "economics/Test/compile"
  "adversarialBoundary/Test/compile"` — pass with `-Werror`.
- `orderModel/test` — pass, 6 tests.
- `economics/test` — pass, 8 tests.
- `adversarialBoundary/test` — pass, 126 tests, including 29 economics compiler-boundary tests.
- `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 6 order-model, 8 downstream economics, and 126 adversarial tests (774 total).
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
