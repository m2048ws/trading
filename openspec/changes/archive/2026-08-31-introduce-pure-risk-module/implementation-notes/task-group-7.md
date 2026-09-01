# Task Group 7 — Explicit arbitrary exhaustive fallback

## Separate typed boundary

`ExhaustiveLotSizing.select(instrument)(budget, cap)(evaluate)` is the only arbitrary-evaluator sizing entry point. The
evaluator consumes instrument-bound positive `Lots` and returns a caller-owned typed `Either[E, Pnl]`; the risk module
validates the resulting PnL identity and derives exact refined downside internally. There is no overload on primary
`MaxAffordableLots.select`, no monotonicity flag or function-to-model conversion, and no implicit fallback.

`LocatedLotEvaluationFailure` retains the exact refined positive coordinate plus one closed cause:

- `CallerEvaluation(cause)` preserves the caller's typed scenario, fee, conversion, or PnL cause;
- `LotConstruction(cause)` preserves instrument-lot construction failure; or
- `RiskAssessment(cause)` preserves lot/PnL identity failure while deriving downside.

The first failed ascending coordinate terminates traversal. No partial best-so-far decision is returned, later
coordinates are not evaluated, and a failure is never reinterpreted as unaffordable risk.

## Linear traversal and distinct evidence

Successful evaluation visits every positive count from one through the refined cap in deterministic ascending order.
The tail-recursive loop retains only the first assessment and optional greatest affordable assessment, so successful
state is constant-size while observation cost is explicitly `O(cap)`.

`ExhaustiveLotDecision` is intentionally distinct from the primary result:

- `NoAffordable(first, evaluatedThrough)` retains one assessed positive lower coordinate plus full-range evidence; or
- `Selected(best, evaluatedThrough)` retains the greatest affordable assessment plus full-range evidence.

It has no probe vector and no adjacent monotone upper boundary because complete traversal, not a curve law, justifies
the result. Both the decision and cause sums execute built-in-class guards from their JVM base constructors; the
completed-JAR Java fixture proves unknown alternatives cannot be instantiated.

## Focused evidence

Nine examples cover a deliberately non-monotone sequence, one-lot-unaffordable/later-affordable behavior, an interior
decrease, the all-unaffordable case, typed failures at three separate coordinates, deterministic first-failure
short-circuiting, foreign-PnL identity failure, a 20,000-coordinate stack-safe traversal, and agreement with primary
sizing over monotone data while retaining distinct evidence contracts.

The focused gate passes 40 risk tests and 8 completed-JAR/compiler-boundary tests. Canonical Verify and Human Review
remain separate gates. Automated Task Group review identified missing completed-JAR coverage for the newly added
exhaustive public sums; the positive client and Java rejection fixture were extended, and the final review pass has no
findings.
