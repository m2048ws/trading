# Task Group 2 — Module Boundaries

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `2` (`Module Boundaries`)
- Starting state revision: `2`
- Previous acknowledged commit: `073f9340a4e64e781aea85b48f46f49c020cbea8`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered boundary

- `orderModel` owns `order-model/`, publishes `trading-order-model`, and contains the existing `trading.order`
  implementation as real production code. Its direct project dependencies are quantities and instrument economics.
- `executionScenario` owns `execution-scenario/`, publishes `trading-execution-scenario`, and contains the existing
  `trading.scenario` implementation as real production code. Its direct project dependencies are instrument economics
  and order model.
- The transitional `economics` project now contains only fee-policy and risk production packages and depends downward
  on instrument economics and execution scenario.
- Root aggregation and its ordered test gate include both new projects between instrument economics and transitional
  economics.
- All three domain artifacts export completed main JARs. The adversarial project generates distinct immutable compiler
  classpaths for instrument economics, order model, execution scenario, and the full downstream aggregate.

The source relocation intentionally preserves the prerequisite implementation unchanged. Algebraic order refactoring,
validation redesign, scenario input redesign, scenario evaluation, and consumer migration remain owned by Task Groups
3–7.

## Boundary evidence

`EconomicsCompilerBoundarySuite` now checks the physical JAR ownership and isolated classpaths:

- the order JAR owns order classes but no `MarketState`, scenario, fee, risk, application, or runtime classes;
- the scenario JAR owns scenario classes but no instrument, order, fee, risk, application, or runtime classes;
- the transitional economics JAR owns fee-policy and risk classes but no instrument, order, or scenario classes;
- an order-only external client compiles its order prelude but rejects scenario, fee, risk, application, and runtime
  imports;
- a scenario-only external client compiles its scenario prelude but rejects fee, risk, application, and runtime imports
  as well as attempted mutation of an immutable slice field.

Production-source inspection found no scenario, fee, risk, application, runtime, or `MarketState` references in
`order-model`, and no fee, risk, application, or runtime references in `execution-scenario`.

## Checks

- `sbt -batch scalafmtAll` — pass.
- Dependency-order compile of `orderModel`, `executionScenario`, `economics`, and the adversarial test project — pass.
- `adversarialBoundary/Test/testOnly external.EconomicsCompilerBoundarySuite` — pass, 27 tests.
- `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 12 downstream economics, and 124 adversarial tests (770 total).
- Resolved artifact identities are `trading-order-model` and `trading-execution-scenario`, both with completed-JAR
  export enabled; the economics dependency classpath contains both JARs.
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
