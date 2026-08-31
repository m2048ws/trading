# Task Group 5 — Exact monotone loss-curve algebra

## Closed exact representation

`MonotoneLotRisk` now owns a private closed formula tree over exact typed settlement loss. Public construction is
limited to affine loss, checked contiguous piecewise loss, compatible addition/minimum/maximum, floor or ceiling
uniform-grid quantization, and a complete observed table. No arbitrary evaluator, marker, boolean, subtype, cast, or
proof token enters this construction channel.

Affine loss uses the exact equation `first + (n - 1) * marginal`; the first value may be signed while the marginal is
already refined nonnegative. Model observation derives exact downside as `max(0, signedLoss)`. Piecewise input retains
inclusive coordinates, exact starting loss, and a proposed exact marginal. Construction accumulates out-of-domain
coordinates, invalid ranges/adjacency, missing coverage, negative marginals, and downward boundaries in stable source
order before normalizing the segments. Successful lookup uses binary search over explicit segments.

Pointwise addition, minimum, and maximum require equal instrument identity, position/settlement dimensions, and cap,
accumulating every incompatible property. Floor and ceiling are the only admitted quantization policies because the
uniform-grid quantity laws already establish their order preservation. All operations retain exact rationals and typed
dimensions; none performs floating-point conversion or raw scalar reconstruction.

## Complete finite table

`fromCompleteTable` accepts an explicit positive cap plus instrument-bound lots/PnL rows. Its first applicative phase
checks row identity, position/settlement dimension, domain, ascending order, duplicates, and exact coverage. Only after
that evidence exists does it derive checked assessments through `LotRiskAssessment.fromPnl`; a dependent adjacent pass
then rejects decreasing downside. Success retains the already derived assessment table as a reusable total model.

The table path is deliberately `O(cap)`: coverage builds one hash set and scans ascending coordinates without sorting,
while row, ordering, duplicate, assessment, and adjacency passes are each linear. It does not infer monotonicity from
samples and does not turn an arbitrary evaluator into a capability.

## Compactness and examples

Every model exposes retained construction instrumentation: expression nodes, explicit breakpoints, and table rows
inspected. Affine and composed construction reports work independent of cap; a `10^100`-lot composed model still
contains three expression nodes and zero table rows. Piecewise work follows its explicit segment vector, while complete
table work records every row.

Exact examples cover signed/clamped affine loss, inverse-contract-shaped linear loss, fixed/proportional/minimum/capped
fees, changing nonnegative tier slopes, nested liquidity increments, and floor/ceiling quantization. Negative examples
cover negative marginal loss, gaps, downward boundaries, mixed identities/dimensions, duplicate/missing coordinates,
and decreasing observed risk. Generated laws cover totality, identity, exact affine evaluation, monotonicity, and every
closure-preserving combinator.

## Boundary evidence

- 23 risk tests pass: 4 downside, 5 model-boundary, 11 exact curve/table examples, and 3 generated law suites.
- The completed-JAR client compiles affine and piecewise construction from the public artifact.
- Negative completed-JAR fixtures reject raw affine marginals and a nonexistent arbitrary-function certification path.
- Runtime API inspection confirms the model constructor remains JVM-private and `assess`, `lossAt`, `formula`, and
  `makeLots` are not public methods.
- The 7-test completed risk-JAR/compiler suite and repository formatting checks pass.

The automated Task Group review is separate from canonical Verify and Human Review.
