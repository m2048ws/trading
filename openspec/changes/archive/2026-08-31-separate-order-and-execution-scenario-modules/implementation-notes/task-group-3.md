# Task Group 3 — Algebraic Order Model

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `3` (`Algebraic Order Model`)
- Starting state revision: `3`
- Previous acknowledged commit: `6bdd74f30f1730746498124c097158aae3c7a3fe`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Delivered algebra

- `OrderIntent[D]`, `Order[D, B, Q]`, activation, pricing, execution, evidence, and resolution types now retain the
  position, base, and quote dimensions directly. Public products contain `Lots[D]`, `PositionLots[D]`, and
  `Price[B, Q]`; no `Any`, raw `Rational`, public cast, owner parameter, issuance token, or optional kind record is
  used.
- Immediate, fixed, and trailing activation remain a closed sum. Market and priced execution remain a closed sum;
  market execution contains only `NonRestingTimeInForce`, while priced execution contains pricing, general duration,
  liquidity constraint, and a closed displayed/hidden/iceberg visibility value.
- `EffectivePricing` is an explicit typed `Market()` or `Limited(price)` sum, so a market result is not represented as
  a missing price.
- `NonRestingTimeInForce.from` returns focused `InvalidMarketDuration`, and `TrailingActivation.create` is the only
  public raw-offset constructor and returns focused `InvalidTrailingOffset`. Positive lots and prices continue to use
  instrument-economics refinements.
- Fixed and trailing evidence is constructed by the corresponding activation value. Peg resolution is constructed by
  the corresponding pegged-pricing value. These methods derive reference/comparison/trigger/offset fields from their
  owner and return focused semantic failures.
- Verification compares semantic fields, not JVM identity. Structurally equal instruction values accept replay;
  changed same-shape fixed, trailing, and peg values reject replay with their focused mismatch alternatives.

The transitional `Orders` service remains only so TG3 does not absorb TG4's canonical intent/order validation and
convenience-constructor migration. Its evidence/resolution methods now delegate to the instruction values. Scenario and
fee-policy type signatures were mechanically updated for the new indices; their behavioral redesign remains in later
Task Groups.

## Test and compiler evidence

- `OrderInstructionSuite` checks focused local refinements, direct evidence/resolution semantics, fixed/trailing/peg
  same-shape replay, exhaustive case inspection, and all 150 structurally supported activation/execution products.
- The completed order-model JAR compiles `positive/InstructionAlgebra.scala` with exhaustive activation and execution
  matches.
- `negative/ImpossibleInstructionShapes.scala` produces at least eight expected compiler errors for bypassing the
  trailing smart constructor, using a resting market duration, adding priced-only state to market execution, crossing
  fixed/trailing evidence, crossing direct/peg resolution, and using a price as iceberg lots.
- The pre-existing external evidence-shape fixture continues to reject unsupported order/scenario pairings.
- The former positive fixture's foreign-position `copy` was removed because `OrderIntent[D]` now rejects a different
  position dimension at compile time. TG4 still owns ordinary runtime identity diagnostics for same-dimension foreign
  components at the canonical construction boundary.

## Checks

- `sbt -batch scalafmtAll` and `sbt -batch scalafmtCheckAll` — pass.
- Dependency-order compilation of order model, execution scenario, economics, and adversarial tests — pass with
  `-Werror`.
- `orderModel/Test/test` — pass, 3 tests.
- `adversarialBoundary/Test/testOnly external.EconomicsCompilerBoundarySuite` — pass, 29 tests.
- `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 3 order-model, 12 downstream economics, and 126 adversarial tests (775 total).
- Production API inspection finds no `Any`, casts, `Option`, raw `Rational`, JVM `.eq`, owner token, or untyped kind
  representation in `trading.order`.
- `git diff --check` — pass.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
