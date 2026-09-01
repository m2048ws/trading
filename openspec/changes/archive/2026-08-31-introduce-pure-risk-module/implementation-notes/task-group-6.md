# Task Group 6 — Boundary-certified maximum-affordable sizing

## Closed decision and retained evidence

`MaxAffordableLots.select(model)(budget)` consumes only a certified `MonotoneLotRisk` and a refined nonnegative typed
settlement budget. It returns exactly two business alternatives:

- `NoAffordable(first, observations)` retains the assessed one-lot lower boundary; or
- `Selected(best, upper, observations)` retains the greatest affordable assessment and either `AtCap` or the assessed
  `NextUnaffordable` coordinate.

Every result retains the complete distinct probe trace in observation order. The trace is an audit/instrumentation
surface, while `first`, `best`, and `upper` are the business evidence. There is no zero/fractional lot sentinel,
nullable or optional lot, scalar-only reconstruction, per-probe `Either`, or model-validation branch.
The two sealed sums execute built-in-class guards from their JVM base constructors, so ordinary Java cannot introduce
an unknown decision or upper-bound alternative.

## Exact logarithmic search

Selection observes the cap first. An affordable cap completes immediately. Otherwise cap one reuses that sole
assessment; larger models observe one lot, return `NoAffordable` if necessary, then maintain an affordable lower and
unaffordable upper assessment. Each exact `BigInt` midpoint is strictly interior, so no coordinate repeats. Tail-
recursive search stops only when the retained boundaries are adjacent.

Affordability compares `NonNegative[Quantity[S]]` values through the typed exact quantity order. Model observation is
the only lot-construction path. The public `maximumObservationBound` is `2 + ceil(log2(cap))`, calculated with exact
positive-whole/`BigInt` operations.

Examples cover one-lot unaffordability, equality at an interior budget, cap selection, cap one, stepped plateaus, a
1,000-coordinate inverse-contract-shaped curve, and a `10^100` cap. Generated affine models compare every result with
an exhaustive test reference, then assert distinct coordinates, adjacent/cap evidence, and the contractual probe
bound.

## Benchmark evidence

The non-published `trading-benchmarks` project now has a benchmark-only dependency on `trading-risk` and contains
`RiskSizingBenchmark`. Its direct lookup uses a benchmark-local cached private-method handle; no observer is added to
the production public API. The exhaustive reference is likewise benchmark-only and does not create a fallback API.

Recorded JMH 1.37 run on OpenJDK 26.0.2, cap 1,024, throughput mode, one thread, one fork, three 1-second warmups, and
five 1-second measurements:

| Benchmark | Throughput |
| --- | ---: |
| direct curve lookup | 5,023,995 ops/s |
| boundary-certified maximum | 367,684 ops/s |
| exhaustive reference evaluation | 3,752 ops/s |

Deterministic tests, rather than benchmark timing, own correctness and the probe-count guarantee. Canonical Verify and
Human Review remain separate gates. The focused completed-JAR/compiler suite passes 8 tests, including a compiled Java
probe that confirms both closed-sum guards reject unknown implementations during construction.
