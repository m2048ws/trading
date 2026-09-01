# Task Group 5 — Domain Scenario Inputs

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `5` (`Domain Scenario Inputs`)
- Starting state revision: `5`
- Previous acknowledged commit: `7d8604781c6ef810aa25c7d52b8100ee14474d84`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered boundary

- `LiquidityRole` and `LiquiditySlice` remain owned solely by `trading.scenario`. `LiquiditySlice.create(instrument)`
  consumes exact positive instrument lots, market state, and role and validates ordinary runtime identity at the pure
  explicit-instrument boundary.
- `MatchedSlices[L, M]` replaces Cats `NonEmptyVector` in the public scenario contract. It exposes a guaranteed head,
  immutable tail, vector projection, `one`/`of` construction, and typed `fromVector` rejection of empty input.
- `ScenarioAssumptions` now stores exactly one `order`; it no longer duplicates an instrument ID or a target-order
  claim. Its activation evidence and pricing resolution remain dependent on that stored order, and its slices use the
  domain non-empty product.
- `ScenarioAssumptions.create`, `one`, `many`, and `fromVector` provide direct adapter-friendly construction without
  untyped maps or duplicate evidence claims. The transitional `Scenarios` helpers delegate to this value API until the
  service and second-order evaluation path are removed in Task Group 6.

## Test and compiler evidence

- Downstream tests cover `MatchedSlices` head/tail/vector behavior, one and many construction, non-empty reconstruction,
  and typed empty rejection while continuing to exercise complete scenario behavior.
- External positive compilation covers immediate/direct assumptions; the associated-shape fixture prelude additionally
  compiles fixed/direct, trailing/direct, and immediate/pegged assumptions.
- External negative compilation rejects missing or extraneous activation evidence, fixed/trailing mismatch,
  direct/pegged mismatch, untyped evidence/resolution maps, and the removed duplicate assumption identity/target fields.
- The isolated execution-scenario JAR rejects raw or copy-based `LiquiditySlice` forgery as well as mutation, preserving
  explicit-instrument construction as the only supported source of slices.
- Packaged-JAR inspection requires `MatchedSlices` and `ScenarioAssumptions` and verifies that the latter has no Cats
  `NonEmptyVector` bytecode signature.

## Checks

- `sbt -batch scalafmtAll` and dependency-order compilation — pass with `-Werror`.
- `executionScenario/test` — pass (no module-local suites yet; behavior is exercised downstream and externally).
- `economics/test` — pass, 8 tests.
- `adversarialBoundary/test` — pass, 126 tests, including 29 economics compiler-boundary tests.
- `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 6 order-model, 8 downstream economics, and 126 adversarial tests (774 total).
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
