# trading-risk

Pure position-risk mathematics over typed quantities and instrument economics.

The production artifact depends only on `trading-quantities`, `trading-instrument-economics`, and pure Cats Core
utilities. It does not own order construction, execution scenarios, fee policies, catalogs, application/runtime
effects, persistence, telemetry, or benchmarks.

The current boundary owns exact refined downside measurement, unforgeable lot-risk assessments, and constructive
monotone models. Its closed exact loss vocabulary provides:

- affine loss with a signed first-lot value and refined nonnegative marginal loss;
- checked contiguous piecewise loss with nonnegative slopes and nondecreasing boundaries;
- compatible pointwise addition, minimum, and maximum;
- floor or ceiling projection onto a typed uniform settlement grid; and
- an explicitly `O(cap)` complete-table validator for instrument-bound lots/PnL observations.

Algebraic models remain compact expression trees: their retained `CurveConstructionCost` counts expression nodes and
explicit breakpoints independently of the declared lot cap. The table route alone records inspected rows. Model
construction derives downside as exact `max(0, signedLoss)` and exposes neither the formula nor its total internal
observer.

`MaxAffordableLots.select(model)(budget)` performs exact boundary-certified binary search. Its closed result is either
`NoAffordable(first)` or `Selected(best, AtCap/NextUnaffordable)`. The result retains every distinct assessment probed
in order, observes no coordinate twice, and stays within `2 + ceil(log2(cap))` observations. Model construction owns
validation, so primary selection is total and has no per-probe error branch, zero/fractional sentinel, or discarded
scalar-only result.

The non-published JMH project measures direct curve lookup, boundary-certified maximum sizing, and a benchmark-local
exhaustive reference. The production risk artifact still exposes neither its internal observer nor an implicit
exhaustive path.

Subsequent S-03 Task Groups add the explicitly linear arbitrary fallback before the transitional `trading-economics`
artifact is retired.
