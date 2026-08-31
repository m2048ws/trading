## Context

See `proposal.md` for motivation and the `order-scenarios` delta for normative behavior. This delivery is
`RFC-0002-architecture-portfolio/S-02-order-execution-scenarios`. It is blocked until the externally owned
`establish-pure-instrument-economics` delivery has established `trading-instrument-economics`, removed instrument-owned capability
views, and temporarily placed downstream order/scenario code in conceptual packages inside the broad `economics`
artifact.

The current order model already has useful algebraic structure: closed activation and execution alternatives, associated
evidence/resolution types, positive refined inputs, and immutable values. Its main architectural problems are ownership
and duplication. One `Orders` service owns instruction and evidence constructors; `Scenarios` owns hypothetical matched
outcomes; both are manufactured by `Instrument`. `ScenarioAssumptions` retains a target order while scenario creation
also accepts an order, then validates `assumptions.target.eq(order)`. Scenario evaluation recomputes signed position
change from side and lot count even though those inputs already determine it.

All behavior is pure. A scenario is a planning/simulation statement about a complete hypothetical outcome, not live
order state or an execution report. The implementation must preserve that distinction for the later application/runtime
proposal.

## Goals / Non-Goals

**Goals:**

- Give immutable order instructions and hypothetical matched outcomes separate physical dependency owners.
- Retain the best current dependent typing while eliminating duplicated claims and service-locator APIs.
- Make invalid shapes unrepresentable and semantic failures explicit, accumulated, and locally owned.
- Preserve exact lots, prices, market states, and signed positions from instrument economics without scalar projection.
- Leave fee policy and risk able to consume scenarios without either becoming a dependency of the scenario artifact.

**Non-Goals:**

- Do not model submission, acknowledgement, replacement, cancellation, partial-fill lifecycle, venue IDs, or reported
  fills and fees.
- Do not enforce reduce-only against account position, reserve balances, check margin, or guarantee maker-only fills.
- Do not fetch trigger/reference prices, resolve pegs from a market-data source, or interpret venue-specific TIF clocks.
- Do not add free-monad command ASTs, tagless-final algebras, effects, streams, or execution interpreters.
- Do not introduce an order owner token, generative order identity, or an `OrderId` merely to associate scenario
  evidence.
- Do not make order/scenario values persistence schemas; `S-05-versioned-boundary-codecs` owns stable encodings.

## Decisions

### 1. Create two final pure artifacts

Add:

```text
SBT ID:       orderModel
directory:    order-model
artifact:     trading-order-model
package root: trading.order
depends on:   trading-quantities, trading-instrument-economics

SBT ID:       executionScenario
directory:    execution-scenario
artifact:     trading-execution-scenario
package root: trading.scenario
depends on:   trading-instrument-economics, trading-order-model
```

The direct quantities dependency on `orderModel` is intentional because the public model uses numeric refinements such
as positive tick offsets. `executionScenario` depends directly on instrument economics because public slices contain
`Lots` and `MarketState`, even though order model also depends on it. Both may use `cats-core` internally for pure
validation; neither has an effect runtime.

After the move, the transitional `economics` artifact contains only fee-policy and risk code and depends downward on
`executionScenario`. The fee-policy and pure-risk Slices remove those remaining concerns. Completed-JAR compiler tests enforce that
`orderModel` cannot see scenario classes and neither artifact can see fee, risk, application, or runtime classes.

Alternative considered: one `trading-order-scenarios` artifact with two packages. Rejected because order instructions
are useful without hypothetical execution, while scenario necessarily consumes order and market-state semantics; the
one-way edge is real and immediately useful.

Alternative considered: three artifacts for order, trigger/pricing evidence, and scenario. Rejected because associated
evidence defines the semantics of the instruction algebra and has no independent publication boundary.

### 2. Represent order instructions as dimension-indexed sums of products

The conceptual core is:

```scala
final case class OrderIntent[D <: Dim](
  instrumentId: InstrumentId,
  side: Side,
  lots: Lots[D],
  positionChange: PositionLots[D],
  positionEffect: PositionEffect
)

sealed trait OrderActivation[B <: Dim, Q <: Dim]:
  type Evidence
  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]]

sealed trait OrderExecution[D <: Dim, B <: Dim, Q <: Dim]:
  type Resolution
  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]]

sealed abstract class Order[D <: Dim, B <: Dim, Q <: Dim]:
  type Activation <: OrderActivation[B, Q]
  type Execution <: OrderExecution[D, B, Q]
  val intent: OrderIntent[D]
  val activation: Activation
  val execution: Execution
```

Exact names and variance may adjust for Scala inference, but the indices and associated shapes may not be erased into
`Any`, untyped `Rational`, kind tags, or optional fields. The product cases remain:

- immediate, fixed, and trailing activation;
- market execution with refined non-resting duration;
- priced execution with limit or peg, general duration, liquidity constraint, and displayed/hidden/iceberg visibility;
- explicit market or effective-limit resolution.

Alternative considered: a single case class with enums and `Option` fields. Rejected because it makes illegal
combinations representable and moves exhaustiveness from the compiler into validation.

Alternative considered: public existential fields with unindexed `Lots` and `Price`. Rejected because dimension
parameters remain manageable at this module boundary and protect trigger, limit, and position relationships.

### 3. Associate evidence with instruction shape but not object identity

The path-dependent `Evidence` and `Resolution` members are retained. They give each instruction alternative its exact
input shape:

```text
ImmediateActivation  -> no observation
FixedActivation      -> observed price
TrailingActivation   -> favorable extreme + observed price
Market/Limit         -> direct resolution
PeggedPricing        -> reference price + resolved limit
```

Evidence constructors are behavior of the corresponding activation or pricing value. They copy/close over fields owned
by that value rather than asking callers to repeat reference, comparison, trigger, or offset. Verification still checks
those semantic fields when same-shape evidence is replayed against a different instruction value.

No hidden issuance token or JVM `eq` is added. The repository's trusted-client stance needs accidental mismatch
prevention and truthful semantic validation, not non-forgeable per-order authority. Structurally equal immutable
instructions are intentionally interchangeable.

Alternative considered: a generic `ActivationEvidence` enum accepted by every activation. Rejected because invalid
cross-shape pairings would become runtime cases again.

Alternative considered: a fresh token per instruction value. Rejected because it would make structural values
needlessly identity-sensitive, complicate reconstruction, and repeat the generative-owner pattern already rejected for
economics.

### 4. Construct and retain signed position change in OrderIntent

`OrderIntent.create(instrument)(side, lots, positionEffect)` checks runtime instrument identity and computes the exact
signed coordinate once through the pure `PositionLots` boundary:

```text
Buy  -> +lots.coordinate
Sell -> -lots.coordinate
```

The resulting position change is retained with intent. Scenario and round-trip logic consume it and never multiply sign
and count again. This is a small proof-carrying normalization: downstream code sees the economic result of direction,
while the original side remains available as instruction semantics for price-quality rules and venue adapters.

`ReduceOnly` remains data. Enforcing it without account position would be a false guarantee, so application/execution
logic must supply that state later.

Alternative considered: move signed position back to the scenario because only a filled order changes position.
Rejected because this model is explicitly a complete hypothetical outcome and intent already determines the proposed
signed change; retaining it prevents formula duplication while making no claim of actual fill.

### 5. Give order construction one canonical accumulating validator

Local constructors enforce local refinements immediately: positive trailing offset, positive price/lots inherited from
instrument economics, and non-resting market duration. Final `Order.create(instrument)(intent, activation, execution)`
then applies staged aggregate rules:

1. Accumulate instrument-identity violations across intent, lots, trigger/limit prices, and displayed iceberg lots.
2. If relevant values are coherent, accumulate independent iceberg size and duration constraints.
3. Construct the order only when the non-empty ordered violation collection is absent.

Convenience market/limit/stop constructors build the same ADT values and delegate to this boundary. Fail-fast is
`violations.head`; it is not a second rule implementation.

Errors are module-owned:

```scala
enum OrderComponent:
  case Intent, Lots, TriggerPrice, LimitPrice, DisplayedLots

enum OrderViolation:
  case InstrumentMismatch(component: OrderComponent, expected: InstrumentId, supplied: InstrumentId)
  case RestingMarketDuration(value: TimeInForce)
  case NonRestingIceberg
  case IcebergExceedsOrder(displayed: BigInt, ordered: BigInt)
  case InvalidTrailingOffset(value: BigInt)

final case class OrderViolations(head: OrderViolation, tail: Vector[OrderViolation])
```

Activation and pricing semantic violations remain focused ADTs because scenario evaluation must retain their causes.
The exact case grouping may combine errors with identical remediation, but it cannot fall back to free-form path strings
or a repository-wide `EconomicsError`.

Alternative considered: preserve only fail-fast order creation because there are few rules. Rejected because the
repository has adopted a consistent staged-accumulation policy, and independent configuration mistakes are useful
together.

### 6. Make one dependent ScenarioAssumptions value own the order

The conceptual shape becomes:

```scala
final class ScenarioAssumptions[D, B, Q, S] private (
  val order: Order[D, B, Q]
)(
  val activationEvidence: order.activation.Evidence,
  val pricingResolution: order.execution.Resolution,
  val matchedSlices: MatchedSlices[D, B, Q, S]
)

object OrderScenario:
  def evaluate(instrument)(
    assumptions: ScenarioAssumptions[...]
  ): Either[ScenarioViolations, OrderScenario[...]]
```

The input stores no duplicate `instrumentId`; it projects identity from its order and slices. Evaluation accepts no
second order. Therefore `AssumptionOrderMismatch`, `.eq` validation, and any attempt to synchronize two targets vanish.
The output may retain the assumptions and project its order/slices rather than store duplicate fields.

An adapter reconstructing dynamic data first identifies the concrete activation/execution alternatives, then invokes
their associated evidence constructors before it can build assumptions. The boundary-codec Slice specifies the stable codec; this
proposal provides the pure destination.

Alternative considered: keep `evaluate(order, assumptions)` but compare structural equality rather than reference
identity. Rejected because there is no reason to represent two claims at all.

Alternative considered: put observations in an untyped map for adapter convenience. Rejected because it discards the
associated shape and makes missing/extraneous combinations runtime validation cases.

### 7. Introduce a domain-owned non-empty MatchedSlices product

The public collection is conceptually:

```scala
final class MatchedSlices[A] private (
  val head: A,
  val tail: Vector[A]
):
  def toVector: Vector[A]

object MatchedSlices:
  def one[A](head: A): MatchedSlices[A]
  def of[A](head: A, tail: A*): MatchedSlices[A]
  def fromVector[A](values: Vector[A]): Either[EmptySlices, MatchedSlices[A]]
```

The real type carries the slice dimensions rather than a bare `A`, but it exposes the same domain meaning. It prevents
empty scenarios without leaking Cats' `NonEmptyVector` as a public contract. Internally the implementation may convert
to Cats structures for traversal/validation; public callers see `MatchedSlices` and `Vector`.

This wrapper is justified by a domain invariant and operation set, not by wrapping every collection mechanically.

Alternative considered: keep public `NonEmptyVector`. Rejected because the public concept is matched liquidity, and a
domain name keeps validation-library choice out of signatures while giving codecs a natural reconstruction boundary.

Alternative considered: accept `Vector` and check emptiness during every scenario evaluation. Rejected because the
same invalid state would remain repeatedly representable.

### 8. Stage scenario validation by evidence dependency

`OrderScenario.evaluate` uses one rule graph with stable ordinal labels:

1. Validate runtime instrument identity across the instrument, order components, evidence prices, and every slice
   component. Closed `ScenarioLocation` values identify fields and indexed slice roles.
2. For identity-coherent branches, independently validate exact lot total, activation evidence, pricing resolution,
   market/taker implication, and maker-only/maker implication.
3. For each slice whose effective pricing exists, validate side-dependent limit quality.
4. Construct output only if no violations exist; retain verified activation/effective pricing and intent's position
   change.

The graph is branch-sensitive rather than globally fail-fast between numbered stages. For example, an activation
failure does not suppress an independently checkable lot-total error or a valid pricing branch; missing effective
pricing suppresses only limit-quality checks. Conversely, foreign price identity suppresses semantic tick comparison
on that branch so validation does not invent misleading trigger or limit failures.

Diagnostics order by semantic stage, rule ordinal, then slice index. `ScenarioViolations` is a domain-owned non-empty
head/tail product. `evaluateFirst` projects the head from the same result.

Conceptual closed locations include order intent, trigger observation, trailing extreme, peg reference/resolution, and
slice(index, lots/market/price). They replace strings such as `"scenario.slices[2].price"` without losing formatting
context.

Alternative considered: one applicative tuple running every check unconditionally. Rejected because some checks would
operate on absent or semantically foreign evidence and create cascading false diagnostics.

Alternative considered: sequential `Either` comprehensions. Rejected because they lose independent errors and make
rule order an accidental control-flow artifact.

### 9. Keep liquidity outcomes in scenario, not order

`LiquidityConstraint` remains an instruction (`Unrestricted` or `MakerOnly`) in `trading.order`. `LiquidityRole`
(`Maker` or `Taker`) is an assumed property of each matched slice in `trading.scenario`. Universal scenario implications
remain pure:

- market effective pricing implies taker;
- maker-only constraint implies maker;
- unrestricted priced execution permits either or a mixture.

Hidden/iceberg venue fee classification and venue-specific post-only semantics are not universal and remain available
to the fee-policy Slice or a future venue adapter. This avoids encoding a venue taxonomy in the order algebra.

Alternative considered: put expected liquidity role on the order. Rejected because an unrestricted limit order can
have different roles across slices and the role is an outcome assumption, not an instruction.

### 10. Construct round trips with checked position algebra

`RoundTripScenario.create(instrument)(entry, exit)` first reconciles ordinary runtime identity. It then uses the checked
same-instrument `PositionLots` combination from instrument economics and requires exact flat zero. The held position is
the entry intent's retained position change.

The error retains both observed signed coordinates. No side inference, absolute-lot shortcut, implicit resizing, or raw
integer sign multiplication occurs in round-trip construction. Entry and exit scenario details remain available for
fee attribution.

Alternative considered: require only equal absolute lots and opposite sides. Rejected because the signed economic
invariant is direct, works for any future position construction, and does not duplicate side logic.

### 11. Keep hypothetical scenarios distinct from live execution

`OrderScenario` means “under these explicit observations, resolutions, market states, and liquidity allocations, this
order is modeled as completely matched.” It does not mean an exchange executed it. Its completeness rule is exact lot
conservation, not mutable fill progress.

Future live interpreters in `trading-runtime` may consume orders as commands and produce separately specified execution
events/reports. Those values may later be normalized into economic contributions or compared with a scenario, but they
do not extend this ADT with submitted/working/partially-filled/cancelled cases.

Alternative considered: generalize scenario now into an order lifecycle state machine. Rejected because hypothetical
planning and observed execution have different epistemic status, validation, persistence, and concurrency needs.

## Risks / Trade-offs

- [Dependent evidence signatures may be awkward across artifacts] → Compile representative direct, existential, and
  adapter-style calls from external JAR fixtures before settling exact type aliases; preserve associated types even if
  helper constructors are needed.
- [A domain non-empty wrapper duplicates a small library type] → Keep it minimal (`head`, `tail`, `toVector`, safe
  constructors) and justify it solely by matched-slice semantics and public dependency isolation.
- [Branch-sensitive validation can drift into ad hoc control flow] → Encode explicit rule prerequisites and ordinals in
  one validation model, then derive accumulating and first-error APIs from the same result.
- [Moving `Side` breaks instrument call sites] → Migrate order intent and scenario fixtures in one slice and add a core
  negative-compile assertion that `Side` no longer leaks downward.
- [Structural equality permits evidence reuse for equal instructions] → Treat that as intended immutable-value
  semantics; semantic field mismatch remains checked and no anti-forgery claim is made.
- [Two artifacts add build overhead] → Keep dependencies minimal and use separate artifacts because the order-only use
  case and direction are real; verify incremental compile impact rather than speculating.

## Migration Plan

1. Confirm the effective RFC/source/traceability bindings, merge and archive the externally owned instrument-economics
   prerequisite, then reconcile this planning package against that delivered boundary before claim.
2. Add `orderModel` and `executionScenario` projects, direct dependencies, external-JAR tasks, root aggregation, and
   empty forbidden-dependency compiler tests.
3. Move order instruction ADTs and errors to `trading.order`, then replace `Orders` with value-oriented constructors and
   staged order validation.
4. Move side-directed signed-position construction into `OrderIntent` and update all callers before removing the old
   scenario recomputation.
5. Move associated activation/pricing evidence creation with the instruction values and prove compile-time pairing from
   an external artifact.
6. Add `MatchedSlices`, reshape `ScenarioAssumptions` to own one order, and migrate scenario constructors away from the
   duplicate order parameter and reference-equality check.
7. Move evaluation, liquidity roles/slices, scenario errors, and round trips to `trading.scenario`; implement the staged
   branch-sensitive validator and checked position closure.
8. Update transitional fee-policy/risk code to import the new artifacts without redesigning their policy behavior.
9. Delete order/scenario code and universal error cases from the transitional `economics` artifact; add no aliases.
10. Run formatting, clean module compilation, unit/property tests, negative compilation, external-JAR boundary tests,
    adversarial tests, exact behavioral comparisons, and the full repository matrix.
11. If Scala constraints expose a semantic conflict, reconcile this Corgi planning package before implementation;
    otherwise prepare evidence for separate canonical Verify and human review without starting the fee-policy Slice.

Rollback before release is a source revert to the merged instrument-economics boundary; this proposal changes no external state or
persistence format.
