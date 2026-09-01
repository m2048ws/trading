# Task Group 6 — Staged Scenario Evaluation

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `6` (`Staged Scenario Evaluation`)
- Starting state revision: `6`
- Previous acknowledged commit: `0db021bc40848a72cc54c180178027979b799e32`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered boundary

- `ScenarioLocation` and `ScenarioSliceComponent` provide a closed vocabulary for order, instruction, evidence, and
  indexed slice identity. `ScenarioViolation` directly owns identity, total, activation, pricing, role, and limit-
  quality failures; `ScenarioViolations` guarantees a non-empty ordered result. Free-form paths, target-reference
  mismatch, `ScenarioError`, failure-reason projection, and universal diagnostic wrappers are removed.
- Order-owned activation and pricing observation keys are closed enums rather than strings. Scenario evaluation maps
  them into closed downstream locations without introducing an upward dependency from order model to scenarios.
- `OrderScenario.evaluate(instrument)(assumptions)` is the only accumulating evaluation entry point and consumes no
  second order. `evaluateFirst` projects its head. The `Scenarios` service and JVM-reference reconciliation are gone.
- Identity checks cover order, intent, lots, signed position, trigger/limit/displayed values, activation observations,
  peg reference/resolution, and each slice's identity/lots/market/price in stable semantic order.
- The validator runs branch-sensitive rules: coherent lot values control conservation; activation and pricing verify
  independently; market/taker and maker-only/maker use only their prerequisites; effective-limit quality runs only for
  coherent pricing and slice-price branches. Violations order by stage, rule, then slice index.
- Successful scenarios retain the one assumptions value, verified activation, effective pricing, matched slices, and
  the exact `PositionLots` already stored by intent. No side/count recomputation or parallel scalar view remains.
- The removed service's round-trip call moved provisionally to `RoundTripScenario.create`; Task Group 7 owns its final
  checked position-algebra implementation and downstream consumer migration.

## Test and compiler evidence

- `OrderScenarioSuite` covers successful mixed-price/mixed-role evaluation, output retention, independent activation,
  lot-total and market-role failures, pricing-dependent quality suppression, independent maker-only validation, closed
  foreign-slice location, branch suppression, stable order, and accumulating/fail-fast agreement.
- Downstream economics tests retain multi-slice limit-quality and maker diagnostics and now assert rule-before-slice
  ordering through the canonical static evaluation API.
- Completed-JAR checks require closed scenario location/violation classes and reject the removed `Scenarios` service and
  universal error classes. Negative compilation rejects a second evaluation order, free-form locations, and obsolete
  universal constructors; isolated-JAR compilation also rejects raw, copy-based, or mutation-based `OrderScenario`
  forgery.

## Checks

- `sbt -batch scalafmtAll` and dependency-order compilation — pass with `-Werror`.
- `orderModel/test` — pass, 6 tests.
- `executionScenario/test` — pass, 4 tests.
- `economics/test` — pass, 8 tests.
- `adversarialBoundary/test` — pass, 126 tests, including 29 economics compiler-boundary tests.
- `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 6 order-model, 4 execution-scenario, 8 downstream economics, and 126 adversarial tests (778 total).
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
