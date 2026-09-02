# Task Group 8 — Immutable order codec

## Authority and scope

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Task Group fingerprint: `sha256:b447c35b802770de10f26a9b7a5f9a2ffed4d4e635c4657c12a1892ec38f7a89`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Parent checkpoint: `da476194a02600abc9675a0a1dab933e48587d16`

Group 8 implements only the frozen immutable-order V1 representation and explicit-instrument reconstruction mapped to
AC-018. Scenario and round-trip records, cross-family robustness/benchmarks, checked-in schemas/goldens,
documentation, and final verification remain owned by later Task Groups.

## Frozen instruction algebra

`OrderRecord.V1` is an immutable Java-serialization-rejecting product containing one stable `InstrumentId`, closed
side and position-effect values, one exact lot coordinate, and closed tagged activation and execution coproducts.
Activation is immediate, fixed with reference/comparison/trigger-price coordinate, or trailing with
reference/comparison/positive-refined tick offset. Execution is market with a locally refined non-resting duration or
priced with limit/peg pricing, duration, liquidity constraint, and displayed/hidden/iceberg visibility.

Every tagged alternative contains only its case-local primitive fields. Peg offsets remain exact signed integers;
lots, prices, displayed lots, and trailing offsets remain stable coordinates until reconstruction. The V1 record has
no derived `PositionLots`, component instrument identity, scenario, lifecycle, venue-order, fill, reported-fee,
account, catalog revision, lineage, snapshot, handle, or generic attribute map. Its frozen envelope record type is
`trading.order` at schema version 1.

## Explicit-instrument reconstruction and typed stages

Reconstruction first compares the record's root identity with the one explicitly supplied assembled `Instrument`, so
foreign input cannot attach stable coordinates to another instrument's grids. It then delegates lot and price creation
to `Lots.fromCount` and `Price.exact`, trailing offsets to `TrailingActivation.create`, and market duration to
`NonRestingTimeInForce.from`. Independent local failures accumulate in deterministic instruction/path order.

Only after local success does reconstruction call `OrderIntent.create`, which derives the side-directed signed
position change, and canonical accumulating `Order.create`, which remains sole owner of cross-component identity and
iceberg validation. `OrderReconstructionFailure` distinguishes structural codec violations, foreign instrument
identity, non-empty local refinement failures, and `OrderViolations`; malformed or locally invalid records cannot
reach dependent order validation.

Family batch reconstruction checks `maxBatchRecords` once before record work, evaluates every within-limit input
against exactly the supplied instrument, retains stable zero-based indices, and returns either every order in input
order or every indexed staged failure. There is no partial-success channel, live catalog/root/snapshot acquisition,
untyped `Any`, or kind-plus-option dispatch.

## Exhaustive model, compiler, and authority evidence

Runtime coverage reconstructs and re-encodes all 504 valid combinations of both sides, both position effects, three
activation alternatives, two market durations, limit and signed-offset peg pricing, four priced durations, two
liquidity constraints, and every valid visibility combination. It also covers cross-case fields, unknown vocabulary,
zero lots/prices/displayed lots/trailing offsets, resting market duration, derived buy/sell positions, both independent
iceberg violations, foreign-instrument precedence, exact signed peg offsets, stable atomic batches, and batch limits.
Generated properties vary exact coordinates, side/effect, activation, market/priced execution, and limit/peg pricing.

Completed-JAR positive fixtures compile stable record retention, order encoding, explicit-instrument reconstruction,
atomic batching, and derived-position observation. Negative fixtures reject trusted-instrument encoding, live roots or
snapshots as reconstruction authority, derived position/component IDs, scenario/lifecycle/venue/fill/fee/account
facts, revision/lineage/snapshot/grid handles, and generic `Any` or kind-plus-option decoding. Packaged bytecode
inspection confirms the non-empty refinement aggregate constructor is JVM-private and public codec classes leak no
parser/test-oracle or reverse dependency.

## Validation

| Check | Result | Evidence |
| --- | --- | --- |
| Focused immutable-order tests | pass | All 7 runtime/property tests pass, including 504 valid instruction combinations. |
| `sbt -batch boundaryCodecs/test` | pass | All 75 strict-kernel, coordinate, journal/replay, instrument-definition, and order-codec tests pass. |
| `sbt -batch adversarialBoundary/test` | pass | All 172 completed-JAR/compiler/adversarial tests pass, including fourteen codec-boundary tests. |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck clean test` | pass | Both formatting gates, clean dependency-order compilation, and all 1,002 repository tests pass. |
| Staged reconstruction and batch semantics | pass | Foreign identity, local accumulation, canonical aggregate validation, suppression, stable indices, and atomic failure are covered. |
| Compiler and packaged API boundary | pass | Supported stable-record/instrument calls compile; authority, derived state, execution facts, untyped decoding, and JVM constructor escapes fail. |
| Planning integrity | pass | All 12 strict readiness checks pass at the frozen planning/source/traceability revisions. |

The initial automated Task Group review found that `OrderRefinementFailures` had a JVM-public constructor despite its
Scala-private spelling. The remediation moved construction behind a cached private method handle, made the constructor
bytecode-private, and retained the packaged check. The final pass checked Run/Group identity, AC-018 scope, frozen
coproduct shape, exact and case-local data, smart-constructor and canonical-order delegation, deterministic staged
failures, derived-data omission, exhaustive alternatives, atomic batching, serialization rejection, completed-JAR and
dependency authority, linear bounded work, and validation evidence with no findings. It changed no file,
human-triaged no finding, and is not canonical whole-change Verify or Human Review.
