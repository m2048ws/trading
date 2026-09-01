## Why

Risk sizing is a downstream decision procedure, not an intrinsic instrument capability. The current design also makes
an arbitrary per-lot scenario callback the primary abstraction, forcing linear work through every lot count and mixing
ordinary isolated-instrument sizing with the much broader problem of non-monotone scenario or portfolio optimization.

This change creates a pure risk module whose primary sizing model captures the common foundational case directly: for
one instrument under fixed immutable assumptions, increasing the proposed standalone lot size cannot reduce exact
downside risk. That law is earned through construction, then used for efficient and evidence-preserving maximum sizing.

## What Changes

- Add a pure `trading-risk` artifact depending only on quantities and instrument economics.
- Return downside risk as the existing `NonNegative[Quantity[S]]` refinement and accept risk budgets in the same
  refined typed form, eliminating repeated negative-budget checks and `InvalidRiskBudget`.
- Introduce an immutable lot-risk assessment that associates one positive `Lots` value with its exact nonnegative
  downside risk; use lot-size vocabulary instead of the generic and easily misunderstood term “candidate.”
- Introduce an opaque monotone lot-risk model over a finite positive lot range. It is created only from checked exact
  curve representations and law-preserving combinators; callers cannot certify an arbitrary function with a boolean,
  cast, marker trait, or unchecked token.
- Model ordinary fixed-context loss curves algebraically, including affine and piecewise exact loss, upward/downward
  slope changes that remain nonnegative in total, thresholds, caps, minimums, and monotonic grid quantization.
- Keep algebraic model construction proportional to explicit formula/breakpoint structure rather than the lot cap;
  complete finite-table validation is the only monotone constructor that intentionally enumerates every lot.
- Replace exhaustive primary traversal with exact maximum-affordable sizing over the monotone model. The operation uses
  logarithmically many distinct lot-risk observations and returns the selected assessment plus evidence that it is at
  the cap or that the immediately following lot is unaffordable.
- Replace `Option[Lots]` with an explicit decision ADT distinguishing no affordable positive lot size from a selected
  maximum; no-affordable retains the assessed one-lot boundary rather than using zero or an untyped sentinel.
- Validate all model structure and immutable economic inputs before search. A successfully constructed monotone model
  is total over its declared lot range, so search cannot silently reinterpret a failed observation as unaffordable.
- Provide a separately named exhaustive fallback for genuinely arbitrary or non-monotone finite lot evaluation. Its
  linear cost and typed failure semantics are explicit, and it does not manufacture monotonicity evidence.
- Keep scenario construction and fee-policy evaluation outside the sizing kernel. Upstream pure code may compile fixed
  economic inputs into a monotone loss model or deliberately choose the exhaustive fallback; an arbitrary scenario
  callback is never promoted automatically to the primary model.
- **BREAKING** Remove `Sizing`, `instrument.sizing`, raw `Quantity` risk budgets, generic candidate-evaluator APIs,
  `CandidateAssessment`, `Option[Lots]` results, and universal sizing errors without aliases.
- Remove the now-empty transitional `trading-economics` aggregate artifact after moving its final risk sources and
  tests.
- Exclude current positions, account/portfolio offsets, margin, liquidation, funding forecasts, fill probability,
  market-data loading, concurrency, persistence, tracing, and other effects. Those require later explicit portfolio or
  application capabilities rather than weakening the isolated monotonic model.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `position-risk-sizing`: Move exact downside risk into a pure risk artifact, replace arbitrary exhaustive sizing as
  the default with constructively monotone isolated-instrument sizing, and retain an explicit exhaustive escape hatch
  for genuinely non-monotone finite problems.

## Impact

- Adds the `risk` SBT project in `risk/`, artifact `trading-risk`, package root `trading.risk`.
- Narrows risk's production dependencies to `trading-quantities` and `trading-instrument-economics`; scenario and fee
  modules integrate from downstream application or composition code rather than becoming hidden sizer dependencies.
- Changes risk budget, lot-risk model, error, and maximum-lot result APIs and moves all remaining production/tests out
  of `economics/`.
- Removes the transitional `economics` project/artifact and updates root/adversarial build wiring.
- Adds algebra-law, constructive-monotonicity, boundary-evidence, logarithmic-observation, representative contract,
  exhaustive-fallback, refinement, and module-boundary tests.
- Extends the existing non-published JMH project with benchmark-only risk dependencies and representative curve-lookup,
  maximum-sizing, and exhaustive-reference measurements; deterministic probe bounds remain ordinary tests.
- Introduces no effect-system dependency or external state change.
