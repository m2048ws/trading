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

Subsequent S-03 Task Groups add boundary-certified maximum sizing and the explicitly linear arbitrary fallback before
the transitional `trading-economics` artifact is retired.
