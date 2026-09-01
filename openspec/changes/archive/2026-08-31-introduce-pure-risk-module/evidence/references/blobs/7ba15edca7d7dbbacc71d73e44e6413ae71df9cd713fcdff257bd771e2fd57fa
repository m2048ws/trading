## Context

See `proposal.md` for motivation and the `position-risk-sizing` delta for normative behavior. This delivery is
`RFC-0002-architecture-portfolio/S-03-pure-risk` and is blocked only until the externally owned
`establish-pure-instrument-economics` delivery has established the pure instrument-economics boundary. The order,
scenario, and fee-policy Slices are not prerequisites and are deliberately not dependencies of the sizing kernel.

Current sizing accepts a risk budget, a cap, and an arbitrary callback that may build a completely different round trip
for each lot count. It evaluates every positive count through the cap and returns the greatest affordable value. That is
correct for an arbitrary finite function, but it makes the exceptional general case the foundational API:

```text
Lots => arbitrary scenario => arbitrary policy evaluation => PnL => risk
```

The callback shape cannot establish that increasing size preserves the instrument, direction, market assumptions,
execution shape, fee inputs, or adverse exit. It therefore cannot establish monotonicity, and exhaustive traversal is
the only sound generic algorithm.

Ordinary isolated sizing asks a narrower question. Under one fixed instrument and immutable sizing context, what is the
greatest standalone positive lot size whose exact downside does not exceed a budget? Fixed-price contract loss,
percentage/minimum/capped fees, ordered tiers, monotonic quantization, and nested adverse liquidity commonly form a
nondecreasing exact curve even when the curve is stepped or nonlinear. This design represents that curve as checked
data rather than accepting an arbitrary function and hoping it has the right law.

## Goals / Non-Goals

**Goals:**

- Give exact downside measurement and isolated maximum-affordable sizing a final pure artifact.
- Capture totality, finite domain, instrument identity, settlement dimension, and monotonicity in constructed values.
- Use a small domain-named algebra for common exact loss curves and expose only closure-preserving composition.
- Make primary maximum sizing logarithmic in the lot-count cap and return the observations that justify its boundary.
- Keep a visibly separate exhaustive operation for the genuinely arbitrary finite case.
- Apply the repository validation policy: accumulate independent model-construction violations, then keep the validated
  search path total and free of redundant checks.
- Remove generic “candidate” vocabulary from the public model in favor of lot size, lot-risk assessment, and boundary.

**Non-Goals:**

- Do not infer monotonicity from arbitrary Scala functions, scenario builders, fee policies, samples, or caller promises.
- Do not model an existing account position, portfolio hedges, cross-margin offsets, collateral, VaR, expected shortfall,
  Greeks, liquidation, funding, or fill probability.
- Do not create a second order, execution, fee, or valuation engine inside risk.
- Do not acquire market data, fee configuration, account state, clocks, or persistence within risk functions.
- Do not add tagless-final algebras, effect types, streams, tracing, metrics, transactions, caching, or parallelism.
- Do not publish generic category-theory vocabulary when domain-specific curve and sizing names express the same laws.

## Decisions

### 1. Create a narrow pure risk artifact and retire the aggregate

Add:

```text
SBT ID:       risk
directory:    risk
artifact:     trading-risk
package root: trading.risk
depends on:   trading-quantities,
              trading-instrument-economics
```

The risk artifact consumes `Instrument`, `Lots`, `Pnl`, typed settlement quantities, grids, and refinements. It may
use `cats-core` internally for applicative construction validation, but public errors remain domain-owned and it has
no effect runtime.

Risk no longer directly depends on execution scenarios or fee policy. Those modules produce economic inputs; they do
not define the maximum-affordable algorithm. Composition code that knows a venue's fixed price/fee formula can build a
checked loss curve. Code with only an opaque per-size scenario evaluator must use a checked finite table or the explicit
exhaustive fallback. This dependency direction prevents an arbitrary open `FeePolicy` from silently acquiring a
monotonicity promise it does not expose.

After moving risk production and tests, any transitional aggregate left by the merged instrument-economics delivery is
deleted if and only if it is empty. Cross-module examples belong in test-only integration/adversarial code; no umbrella
production artifact is retained.

The existing non-published `benchmarks` project gains a benchmark-only dependency on `trading-risk`; `trading-risk`
does not depend on benchmark or JMH APIs. Deterministic monotonicity, observation bounds, and reference equivalence stay
in unit/property suites, while representative JVM cost and allocation comparisons use the shared JMH harness.

Alternative considered: retain direct scenario and fee-policy dependencies for a convenience `maxLots` adapter.
Rejected because the adapter could only be exhaustive unless it inspected or trusted opaque policy behavior. Putting it
next to the primary API would obscure the distinction this revision establishes.

Alternative considered: retain `trading-economics` as an aggregate facade. Rejected because it recreates the broad
surface the portfolio is separating.

### 2. Reuse refined quantities for budget and downside

The exact operation remains:

```scala
Risk.downside(instrument)(pnl)
  : Either[RiskIdentityError, NonNegative[Quantity[instrument.roles.settle.D]]]
```

After ordinary `InstrumentId` validation, it returns refined zero for nonnegative net PnL or typed negation for
negative net PnL. It uses quantity order, zero, negation, and refinement operations rather than extracting a
`Rational`, computing separately, and rebuilding a quantity.

Maximum sizing accepts the same `NonNegative[Quantity[S]]` shape. `InvalidRiskBudget` disappears because a negative
quantity cannot enter the supported operation. No nominal `RiskBudget` or `DownsideRisk` wrapper is added: neither
currently carries policy, horizon, account, or provenance beyond the refinement and typed dimension already present.

Alternative considered: keep raw `Quantity` inputs and validate each operation. Rejected because it discards an
existing proof and repeats a boundary concern.

Alternative considered: publish a custom downside algebra. Rejected because nonnegative quantities already carry the
required order and additive laws.

### 3. Separate exact loss formulas, validated monotone models, and observations

The conceptual public shapes are:

```scala
final class MonotoneLotRisk[D <: Dim, S <: Dim] private[risk] (
  val instrumentId: InstrumentId,
  val cap: PositiveWhole,
  private val compiled: CompiledLotLoss[S]
):
  private[risk] def assess(count: PositiveWhole): LotRiskAssessment[D, S]

final class LotRiskAssessment[D <: Dim, S <: Dim] private[risk] (
  val lots: Lots[D],
  val downsideRisk: NonNegative[Quantity[S]]
)

object LotRiskAssessment:
  def fromPnl[D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    instrument: Instrument[D, B, Q, S]
  )(
    lots: Lots[D],
    pnl: Pnl[S]
  ): Either[RiskIdentityError, LotRiskAssessment[D, S]]
```

`MonotoneLotRisk` captures one instrument and a finite domain `1..cap`. Its private assessment operation is total for
that domain and constructs `Lots` through the captured instrument boundary. The checked PnL constructor validates the
instrument/lots/PnL association and derives downside through `Risk.downside`; no public constructor accepts an
unrelated quantity.

The monotone law is:

```text
for every 1 <= a <= b <= cap:
  assess(a).downsideRisk <= assess(b).downsideRisk
```

The type signifies that this law has been established by a library-controlled constructor. It does not claim that the
chosen market assumptions are current, conservative, or approved by an account policy. Those are facts about the
inputs and application workflow, not facts Scala can infer from curve shape.

Alternative considered: `type MonotoneLotRisk = Lots => Quantity`. Rejected because the alias carries no law, finite
domain, totality, identity, or controlled construction.

Alternative considered: a public marker trait or type-class instance supplied for an arbitrary function. Rejected
because conformance would be an unchecked assertion with correctness consequences.

Alternative considered: encode every dynamic lot count as a singleton type. Rejected because the relevant theorem is
about order over an entire runtime interval, not the identity of one literal.

### 4. Use a closed exact curve algebra to earn monotonicity

The initial internal representation is a small domain algebra over exact settlement loss, where positive loss means
negative PnL:

```scala
sealed trait LotLossFormula[S]

object LotLossFormula:
  final case class Affine[S](
    firstLotLoss: Quantity[S],
    additionalLotLoss: NonNegative[Quantity[S]]
  )

  final case class Piecewise[S](
    segments: NonEmptyVector[LossSegment[S]]
  )

  final case class Add[S](left: LotLossFormula[S], right: LotLossFormula[S])
  final case class Minimum[S](left: LotLossFormula[S], right: LotLossFormula[S])
  final case class Maximum[S](left: LotLossFormula[S], right: LotLossFormula[S])
  final case class Quantized[S](source: LotLossFormula[S], grid: GridRef[S], policy: OrderPreservingPolicy)
```

This is a design sketch, not a requirement to expose these constructors verbatim. The public vocabulary stays
domain-specific.

For an affine formula:

```text
loss(n) = firstLotLoss + (n - 1) * additionalLotLoss
risk(n) = max(0, loss(n))
```

The first loss may be signed; the marginal loss is nonnegative. Clamping a nondecreasing signed loss at zero remains
nondecreasing. A piecewise segment has an ordered inclusive lot interval, an exact starting loss, and nonnegative
marginal loss. Construction checks complete contiguous coverage through the cap and ensures the first loss at each new
segment is no lower than the preceding segment's final loss.

Pointwise addition, minimum, and maximum preserve monotonicity for compatible nondecreasing curves. Only quantization
policies already proven order-preserving over a uniform grid are admitted. Exact threshold, minimum-fee, capped-fee,
ordered tier, and decreasing-but-still-nonnegative marginal shapes can therefore be expressed without treating them as
arbitrary functions.

Signed economic components such as rebates are netted into exact loss before a nonnegative marginal or checked segment
is constructed. The risk algebra does not pretend every individual fee component is nonnegative. If the resulting net
curve has a negative marginal or downward boundary, construction fails and the caller must change the fixed model or
use exhaustive evaluation.

Construction validates each primitive and composition node structurally, normalizes explicit piecewise boundaries, and
publishes an opaque compact evaluator. Addition, minimum, maximum, and quantization may remain compiled expression
nodes; they are monotone by closure and do not need to be expanded into one segment or table row per lot. Algebraic
construction is therefore proportional to explicit expression/breakpoint structure, not the declared cap. Suitable
internal accumulation is `ValidatedNec`, converted at the public boundary to a domain-owned non-empty error collection
in stable source order. Search never repeats these checks.

Alternative considered: accept an arbitrary evaluator and test a few points. Rejected because samples do not prove the
law over an unobserved finite domain.

Alternative considered: symbolically support arbitrary Scala fee/scenario logic. Rejected because it is impossible in
general and would turn risk into a second interpreter for other domain modules.

Alternative considered: expose a general free algebra/fixpoint API. Rejected because the small closed vocabulary is
easier to read and sufficient for the accepted formulas; internal representation can change without affecting the
domain contract.

### 5. Support checked finite tables without confusing validation with inference

A second controlled constructor accepts a complete ordered table of instrument-bound `Lots` and core `Pnl`
observations for `1..cap`, derives every lot-risk assessment internally, and verifies:

- nonempty exact coverage with no duplicate or missing lot coordinate;
- one instrument and settlement dimension;
- nonnegative refined risk at every point; and
- nondecreasing adjacent risk.

If validation succeeds, the resulting table-backed value is a legitimate `MonotoneLotRisk`. This route is useful when
an external pure formula is not represented by the closed algebra, or when one validated table will serve many budget
queries. It costs `O(cap)` once and does not make table creation cheap.

This is different from an unchecked promise. The complete finite domain is actually observed. It is also different from
one-shot exhaustive selection: a checked table establishes a reusable monotone capability, while exhaustive selection
can correctly select from a non-monotone table but cannot produce that capability.

Alternative considered: omit the table constructor and require every model to use the initial algebra. Rejected because
that would force premature expansion of the formula vocabulary for every legitimate venue-specific monotone shape.

### 6. Make primary maximum sizing a boundary-certified binary search

The success ADTs are conceptually:

```scala
enum MaxAffordableLots[D, S]:
  case NoAffordable(first: LotRiskAssessment[D, S])
  case Selected(
    best: LotRiskAssessment[D, S],
    upper: AffordableUpperBoundary[D, S]
  )

enum AffordableUpperBoundary[D, S]:
  case AtCap
  case NextUnaffordable(next: LotRiskAssessment[D, S])
```

The algorithm is:

1. assess the cap; if affordable, return it with `AtCap`;
2. assess one lot; if unaffordable, return `NoAffordable(one)`;
3. otherwise maintain an affordable lower observation and an unaffordable upper observation;
4. probe the integer midpoint until the observations are adjacent;
5. return the lower observation with the upper observation as `NextUnaffordable`.

The monotone model plus one adjacent boundary is sufficient evidence of maximality. The result does not claim a
machine-checked theorem; it retains the exact observations on which the domain conclusion rests. No result represents
absence with zero lots, `null`, or an untyped option.

The implementation retains probed assessments and never observes a coordinate twice. It uses no more than
`2 + ceil(log2(cap))` distinct observations; edge cases such as `cap == 1` avoid duplicate endpoint work. Curve
lookup may itself be logarithmic in the number of compiled segments, so the precise CPU bound is separate from the
observable model-probe bound.

Once a `MonotoneLotRisk` exists, maximum sizing is total:

```scala
MaxAffordableLots.select(model)(budget): MaxAffordableLots[D, S]
```

There is no `Either` around each probe. Construction owns invalid structure; the decision owns affordability. This is
the same validate-then-construct principle used elsewhere in the portfolio.

Alternative considered: continue exhaustive traversal for simplicity. Rejected as the primary operation because it
ignores a law deliberately captured by the input and scales linearly for ordinary large lot caps.

Alternative considered: return only `Lots`. Rejected because it discards exact assessed risk and the maximality
boundary.

Alternative considered: accept `assumeMonotone = true`. Rejected because a boolean is not evidence and would make
incorrect skipping easy.

### 7. Keep arbitrary non-monotone sizing explicit and exhaustive

Some callers may intentionally change the scenario, fee regime, hedge, or other assumption by lot count. The separate
fallback is conceptually:

```scala
ExhaustiveLotSizing.select(instrument)(budget, cap)(
  evaluate: Lots[D] => Either[E, Pnl[S]]
): Either[LocatedLotEvaluationFailure[E], ExhaustiveLotDecision[D, S]]
```

For each positive count in ascending order it constructs `Lots`, evaluates the caller's pure function, validates
`InstrumentId`, derives downside through `Risk.downside`, and retains the greatest affordable assessment. It is
stack-safe and constant-memory for successful observations, but its documented observation cost is `O(cap)`.

The first evaluation failure in ascending lot order terminates with its exact coordinate and typed cause. Unlike
independent fields in model construction, later lot computations cannot restore a sound decision once a required value
is unknown. Fail-fast traversal is therefore the appropriate composition here; evaluating all remaining expensive
alternatives merely to accumulate diagnostics is not the default. A future diagnostic batch evaluator can be specified
separately if a real use case requires it.

The fallback has a distinct result type because an exhaustive non-monotone maximum is justified by complete traversal,
not by an adjacent monotone boundary. It cannot be cast or converted into `MonotoneLotRisk`. A caller wanting a
reusable model must materialize and validate the complete table explicitly.

Alternative considered: overload the primary `select` method for arbitrary functions. Rejected because overload
resolution would hide a major semantic and complexity difference.

Alternative considered: remove arbitrary sizing entirely. Rejected because it remains a useful honest escape hatch and
provides a reference implementation for model/property comparisons.

### 8. Integrate scenarios and fees outside the risk kernel

Downstream fee-inclusive evaluation can still feed sizing, but it does not automatically prove a curve law. Three
honest integration paths exist:

1. venue/application composition code derives a closed exact loss formula from fixed price, conversion, fee, threshold,
   and quantization parameters and constructs `MonotoneLotRisk`;
2. pure code evaluates the complete finite scenario table once, derives downside at each lot, and asks the checked table
   constructor to validate monotonicity; or
3. code deliberately invokes `ExhaustiveLotSizing` for a one-shot arbitrary or known non-monotone problem.

No generic scenario adapter lives in `trading-risk` in this change. If repeated venue integrations later reveal a
stable pure abstraction, it can be proposed in a downstream composition module without changing the sizing law.

A model version freezes its instrument, side, market state, adverse exit, conversions, fee inputs, and cap. Changes to
any of those inputs produce a new model. The risk value does not read a live catalog, policy service, or market source.

Alternative considered: let search call a scenario builder and dynamically check monotonicity as it probes. Rejected
because binary probes cannot detect an unobserved downward interval.

### 9. Bound the economic meaning to standalone isolated sizing

Primary sizing is from flat exposure: lot count denotes the magnitude of one proposed standalone position. It does not
consume a current position. Adding lots to an existing short, hedge, option portfolio, or cross-margined account can
reduce total risk; that is a different feasible-set and objective problem and should not be forced into
`MonotoneLotRisk`.

The fixed model may include nonlinear but nondecreasing features:

- exact linear or inverse-contract loss at fixed entry/adverse-exit states;
- percentage fees and order-preserving quantization;
- minimum and maximum fee amounts;
- ordered fee/risk tiers whose total marginal loss remains nonnegative;
- nested liquidity slices with nonnegative incremental adverse loss.

It makes no assertion about fill probability, market freshness, liquidation, funding, or portfolio suitability.
Effectful applications may acquire inputs, choose versions, trace decisions, and persist results, then pass immutable
values through this pure boundary.

Alternative considered: call both isolated and portfolio computations “sizing” behind one generic optimizer. Rejected
because their laws, inputs, complexity, and evidence differ materially.

## Risks / Trade-offs

- [The closed curve algebra could grow into a second valuation language] → Keep only operations needed to establish
  monotone exact loss, reuse quantity/grid primitives, and route opaque policy behavior through tables or exhaustive
  evaluation.
- [A constructed model proves shape, not that its assumptions are current or conservative] → Keep input acquisition,
  approval, versioning, and model governance in application/portfolio capabilities.
- [Some real account-level sizing problems are non-monotone] → Name the primary operation standalone/isolated and defer
  portfolio optimization rather than weakening its law.
- [Checked finite-table construction remains linear] → Document that cost; use it for unsupported formulas or repeated
  budget queries, not as evidence that arbitrary construction is cheap.
- [Binary search correctness depends on every constructor preserving the law] → Seal construction, add law/property
  suites for every primitive and combinator, compare against exhaustive reference results, and include negative
  construction tests.
- [Compact expression lookup adds implementation complexity] → Keep the representation minimal, normalize only where
  it reduces work without enumerating the cap, prove probe counts deterministically, benchmark JVM lookup/allocation in
  the shared JMH project, and avoid publishing representation-specific APIs.
- [Separate primary and fallback result types add API surface] → Accept the small distinction because they carry
  different evidence and complexity contracts.
- [Deleting the umbrella artifact changes test organization] → Move tests by behavioral owner and keep cross-module
  examples in a test-only project.

## Migration Plan

1. Confirm the effective RFC/source/traceability bindings, merge and archive the externally owned instrument-economics
   prerequisite, and reconcile this package against its delivered boundary. No order/scenario/fee Slice is a prerequisite.
2. Add the narrow `risk` artifact, completed-JAR task, root aggregation, and forbidden reverse-dependency tests.
3. Implement exact refined downside risk and remove raw budget validation/error paths.
4. Add domain-owned model-construction violations and private lot-risk assessment construction.
5. Implement the minimal exact loss formula representation, normalization, constructive validation, and opaque
   `MonotoneLotRisk`.
6. Implement affine, piecewise, addition, minimum, maximum, order-preserving quantization, and complete-table
   constructors with algebra/property tests.
7. Implement boundary-certified logarithmic maximum sizing and compare it with a simple exhaustive reference over
   generated monotone curves.
8. Implement the separately named arbitrary exhaustive fallback with typed located fail-fast errors and no
   monotonicity conversion.
9. Extend the shared non-published JMH project with representative curve-lookup, maximum-sizing, and exhaustive-
   reference measurements without introducing a production dependency or numeric release threshold.
10. Migrate existing scenario-based sizing call sites first through the explicit fallback for behavioral continuity;
   migrate representative fixed isolated cases to the constructive model where their formulas establish the law.
11. Add representative linear/inverse contract, minimum/capped fee, ordered tier, quantization, and nested-liquidity
    fixtures plus deliberately invalid/non-monotone counterexamples.
12. Move remaining production/tests out of `economics/`; delete `Sizing`, old errors/capability paths, the SBT
    project, artifact task, and compatibility aliases.
13. Rewire root/adversarial/integration dependencies and verify scenario/fee modules do not gain a reverse risk
    dependency.
14. Run formatting, clean compilation, unit/property/refinement tests, observation-bound tests, explicit JMH compile/
    focused measurements, external-JAR compiler checks, adversarial tests, and the full repository matrix.
15. If the merged prerequisite changes an assumption, reconcile this Corgi planning package before claim; otherwise
    prepare evidence for separate canonical Verify and human review without starting the application/runtime Slice.

Rollback before release is a source/build revert to the merged instrument-economics boundary; no persisted schema or external state is
changed.
