## Context

See `proposal.md` for motivation and the fee/scenario deltas for normative behavior. This is Proposal 6 and assumes the
pure instrument economics, order model, and execution-scenario artifacts from Proposals 4 and 5.

Today `FeeDenomination` owns percentage and minimum policy as well as grid quantization. `FeeSchedule` is an open pure
trait, but its result `FeeLine` contains both a caller-supplied slice index and caller-supplied market state. Later
valuation receives the scenario again, checks index bounds, then uses JVM `eq` to verify that the supplied market is the
one at that index. `Valuation.pnl` also owns scenario traversal, schedule evaluation, attribution validation, fee
conversion, price PnL, and core aggregation.

Proposal 4 moves denomination/fee values and core `PricePnl`/`SettledFeeContribution`/`Pnl` into instrument economics.
Proposal 5 makes a complete scenario own one order and non-empty matched slices. The remaining task is to give policy
and scenario attribution an honest downstream model without putting either back into the core.

## Goals / Non-Goals

**Goals:**

- Create one pure extension point for fee policy that supports venue/account specialization without an effect wrapper.
- Reuse quantity refinements so fee formulas are total after validation.
- Make source attribution derive from the exact scenario rather than trusting duplicated caller state.
- Preserve custom typed policy failures through composition and higher evaluation.
- Provide exact multi-slice round-trip price normalization and fee-inclusive scenario results around the core PnL value.
- Identify and use lawful empty/concatenation/folding structures only in the context where their operations are total.

**Non-Goals:**

- Do not load or select policy from a database, account service, clock, venue, or effective-date registry.
- Do not define execution-reported commissions, ledger postings, wallet settlement, tax, funding, interest, or margin.
- Do not make a fee policy or scenario result a durable wire schema.
- Do not add tagless-final `F[_]`, Cats Effect, ZIO, streaming, transactions, tracing, or metrics.
- Do not require all external policy implementations to share one repository-wide closed failure enum.
- Do not expose a globally total `Monoid[FeePolicy]` across runtime instrument identities.

## Decisions

### 1. Create the final fee-policy artifact

Add:

```text
SBT ID:       feePolicy
directory:    fee-policy
artifact:     trading-fee-policy
package root: trading.fee
depends on:   trading-quantities,
              trading-instrument-economics,
              trading-order-model,
              trading-execution-scenario
```

The direct quantities dependency is required because policy APIs expose `NonNegative[Quantity[D]]` and `FeeRate` exact
coefficients. Direct dependencies on order and scenario are intentional: policies may inspect instruction alternatives
and matched liquidity roles. The artifact may use `cats-core` for pure validation and folding but has no effect runtime.

The new artifact owns policy math, the open policy strategy, policy composition, directives, validated scenario fee
assessments, leg policy selection, and fee-inclusive scenario evaluation. Core `FeeDenomination`, `Fee`,
`SettledFeeContribution`, and `Pnl` remain in instrument economics. Exact price normalization is added to the already
downstream execution-scenario artifact, not duplicated in fee policy.

After migration the transitional `economics` artifact contains only risk/sizing. Completed-JAR tests prove the core and
scenario artifacts do not acquire a reverse dependency on fee policy.

Alternative considered: keep policy in instrument economics because fees affect PnL. Rejected because maker/taker,
account tier, and order inspection would pull order/scenario dependencies into the kernel.

Alternative considered: create separate fee-math, fee-policy, fee-assessment, and scenario-PnL artifacts. Rejected
because only the core/policy edge has an independent consumer and publication rationale today; the extra artifacts would
be thin ceremony.

### 2. Model FeePolicy as a pure strategy with a typed error channel

The conceptual public shape is:

```scala
trait FeePolicy[+E, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim]:
  def instrumentId: InstrumentId

  def evaluate(
    scenario: OrderScenario[PosD, B, Q, S]
  ): Either[PolicyErrors[E], Vector[FeeDirective]]
```

`PolicyErrors[E]` is a domain-owned non-empty head/tail aggregate. `E` is covariant and belongs to the policy
implementation. Repository-provided total policies use `Nothing`; applications can widen several policy-specific
failures into their own sum ADT. Higher assessment/evaluation errors retain `E` as a typed cause with policy/leg context
rather than stringifying or throwing it.

This is not tagless final. It has no abstract effect constructor or interpreter choice: semantically it is an immutable
pure function `Scenario -> Either[NonEmpty[E], Vector[Directive]]` with instrument context. A named trait is retained
instead of exposing a raw function type so identity, documentation, composition, and future source-compatible pure
combinators have one domain home.

Alternative considered: make every policy total. Rejected because open venue/account policies may have honest
scenario-specific domain rejection even when their static configuration was validated.

Alternative considered: force all custom failures into a sealed repository enum. Rejected because the extension point
cannot anticipate venue/account causes and would regress to string codes or continual core edits.

Alternative considered: parameterize by `F[_]`. Rejected because typed domain failure is not an environmental effect
and `Either` is the one intended pure interpretation.

### 3. Make policy formulas total over refined typed quantities

`FeeRate` remains a nominal exact scalar with policy sign convention:

```text
positive rate -> account charge
negative rate -> account rebate
```

Percentage math accepts `NonNegative[Quantity[D]]` and returns `Quantity[D]`:

```text
contribution = basis * -rate
```

The implementation scales the typed quantity. It does not extract a coefficient, validate its sign again, multiply raw
`Rational`, and reconstruct a quantity.

Minimum-charge adjustment accepts `Quantity[D]` contribution and `NonNegative[Quantity[D]]` minimum. It uses scalar
projection only for sign/order comparison, then returns either the original typed contribution or the typed negation of
the minimum. Rebates and zero remain unchanged. It is total for refined input.

Finally, policy passes the exact contribution to a validated `FeeDenomination`, whose core constructor quantizes and
retains conservation. Each policy component crosses that boundary separately so minimums and rounding are not
incorrectly aggregated before quantization.

Alternative considered: introduce `FeeBasis[D]` and `MinimumFee[D]` wrappers over already refined quantities. Rejected
because `NonNegative[Quantity[D]]` expresses the complete numeric invariant and another nominal layer rules out no
current mistake. Policy-specific names can be local fields.

Alternative considered: keep fallible raw-quantity helpers. Rejected because they repeatedly validate a property the
quantity refinement already represents.

### 4. Separate policy directives from assessed attribution

A policy output is intentionally incomplete:

```scala
opaque type SliceIndex = Int // constructed only when >= 0

sealed trait FeeDirective:
  type D <: Dim
  val fee: Fee[D]
  val sourceSlice: SliceIndex
```

The existential package retains the fee's asset dimension. A directive cannot store `MarketState`, a slice object, a
scenario token, or a free-form attribution path. Its index is nonnegative locally but cannot prove range without the
target scenario.

The canonical boundary is conceptually:

```scala
FeeAssessment.evaluate(instrument)(scenario, policy)
  : Either[FeeAssessmentErrors[E], ScenarioFees[...]]
```

It validates policy/scenario/instrument identity, executes the policy, and validates every returned directive. For a
valid index it selects the actual immutable slice from `scenario.matchedSlices` and constructs:

```scala
sealed trait AssessedFee[PosD, B, Q, S]:
  type D <: Dim
  val fee: Fee[D]
  val sourceIndex: SliceIndex
  val sourceSlice: LiquiditySlice[PosD, B, Q, S]

final class ScenarioFees[...] private (
  val scenario: OrderScenario[...],
  val fees: Vector[AssessedFee[...]]
)
```

The assessment owns its scenario once. `sourceSlice.market` is therefore the conversion context; no caller can pair the
index with an unrelated market through supported construction, and no later `.eq` check is needed. Invalid indices and
fee identities accumulate in directive order.

Alternative considered: store only the index and look up the market later from a separately supplied scenario.
Rejected because it recreates the duplicated-target relationship and permits the assessment to drift from its source.

Alternative considered: let custom policies construct final assessed fees through a context token. Rejected because
returned values could still be accidentally retained from another context; returning data requests and resolving them
centrally is simpler and token-free.

Alternative considered: identify a source slice by structural equality. Rejected because equal-looking duplicate
slices make attribution ambiguous; the index within one owned scenario is the actual domain coordinate.

### 5. Treat policy composition as a contextual monoid

For one fixed instrument, policy output vectors compose by concatenation and no-fee is empty identity. Policy error
aggregates also concatenate in stable component order. `FeePolicy.combine(instrument)(components)` first accumulates
foreign-instrument component errors, then returns one normalized composite.

Evaluation traverses all components applicatively so independent failures accumulate. Successful directive order is
component order followed by each component's order. Nested combinations normalize to one component vector, making
associativity observable and straightforward to law-test.

Because `instrumentId` is ordinary runtime data, combination of arbitrary `FeePolicy` values is partial. No global Cats
`Monoid[FeePolicy]` is published. The implementation applies vector/validation semigroup laws inside the checked fixed-
instrument context. A domain `FeePolicySet` may represent that validated context if it improves signatures, but it must
remain a small product rather than another service locator.

`Chain` is not part of the public design. The implementation may use `Chain`, `Vector` builders, or `foldMap` if
measurement justifies it; the observable algebra is ordered concatenation, not a particular collection library.

Alternative considered: fail at the first component error. Rejected because policies are independent and the repository
validation policy requires useful stable accumulation.

Alternative considered: expose the global monoid anyway and throw/drop mismatches. Rejected because the operation would
not be lawful over its advertised carrier.

### 6. Normalize complete scenario price PnL in execution-scenario

Add a pure operation conceptually named:

```scala
ScenarioValuation.pricePnl(instrument)(roundTrip)
  : Either[ScenarioValuationError, PricePnl[instrument.roles.settle.D]]
```

For each matched slice it asks order intent to derive side-directed `PositionLots` for that slice's positive lots,
calls core `Valuation.positionValue` at that slice's market state, negates the result as trade cashflow, and folds all
entry then exit cashflows using the settlement-quantity additive commutative group. It packages the result with the core
`PricePnl` constructor.

The formula is:

```text
sliceCashflow = -positionValue(side.positionChange(slice.lots), slice.market)
pricePnl      = sum(entry slice cashflows) + sum(exit slice cashflows)
```

It weights every market by exact slice lots and works for linear, inverse, and quanto normalization without an average-
price shortcut. For a one-slice entry/exit it must equal core exit-minus-entry valuation of the held position. The
execution-scenario artifact also owns a closed `RoundTripLeg` (`Entry`, `Exit`) used only by downstream attribution.

Alternative considered: implement price normalization inside fee policy. Rejected because scenario price PnL is useful
without fees and fee policy should not own the price component.

Alternative considered: calculate a weighted scalar price first. Rejected because inverse/quanto settlement is not in
general equivalent and would erase typed per-state conversion.

### 7. Make entry and exit policy selection an explicit product

One fee policy need not describe both legs: account tier, venue terms, or policy version may change between them. The
input is therefore:

```scala
final case class RoundTripFeePolicies[E, ...](
  entry: FeePolicy[E, ...],
  exit: FeePolicy[E, ...]
)
```

`RoundTripFeePolicies.same(policy)` is a convenience that constructs the product explicitly. Policy selection based on
clock/account/reference data occurs in the future application layer; this value only records already selected immutable
policies.

Both policies use a common widened `E`, which allows different concrete failures when the caller supplies a sum type.
The initial evaluation stage validates both runtime instrument identities before running dependent policy logic.

Alternative considered: keep one policy parameter and require callers to create a branching schedule. Rejected because
it hides a temporal/contextual distinction in policy internals and prevents each leg from retaining a separately
selected version naturally.

### 8. Orchestrate fee-inclusive scenario PnL as staged pure composition

`FeeInclusivePnl.evaluate(instrument)(roundTrip, policies)` is pure and uses a dependency graph:

1. Accumulate identity mismatches across instrument, round trip, and both policies. Stop dependent work on a foreign
   branch.
2. Independently normalize scenario `PricePnl` and assess entry/exit policies.
3. For each successful assessment, convert every assessed fee through `assessed.sourceSlice.market`, accumulating
   conversion errors in leg/slice/directive order.
4. Pass price PnL and ordered core contributions to `Pnl.create`.
5. Return a scenario-level result retaining:
   - the round trip;
   - entry and exit `ScenarioFees`;
   - attributed converted contributions carrying `RoundTripLeg` and `SliceIndex`;
   - the core `Pnl`.

This wrapper preserves planning provenance without making core PnL depend on scenario types. Failures use a generic
closed wrapper:

```scala
enum FeeInclusivePnlViolation[+E]:
  case Identity(...)
  case ScenarioPrice(cause: ScenarioValuationError)
  case Policy(leg: RoundTripLeg, cause: E)
  case Directive(leg: RoundTripLeg, ordinal: Int, cause: FeeAssessmentViolation)
  case Conversion(leg: RoundTripLeg, slice: SliceIndex, cause: FeeConversionError)
  case Core(cause: PnlError)
```

The actual implementation may use separate error types per boundary and wrap them at the top; it must retain typed
causes and a domain-owned non-empty ordered collection. Fail-fast, if offered, is the head of this result.

Alternative considered: let core `Pnl` retain leg/slice/scenario fields. Rejected because it would reintroduce the
upward scenario dependency Proposal 4 removed.

Alternative considered: stop at the first missing conversion. Rejected because independent fee lines and legs can be
diagnosed together once assessment succeeds.

### 9. Keep policy acquisition and execution effects outside

A live application may need to fetch account tier, select a version at a clock instant, read venue configuration, trace
evaluation, or persist assessments. Those are application capabilities. Their effectful program produces one immutable
`RoundTripFeePolicies` value, invokes this module's pure evaluation, and handles its typed result.

No `FeePolicyAlg[F[_]]` is introduced. Multiple live policy sources are interpreters of an application-level loading
port, not interpreters of percentage arithmetic or scenario traversal. This keeps deterministic replay possible by
retaining the selected policy inputs outside the core result or in an application audit envelope.

Alternative considered: add effectful `FeePolicy[F]` now for future database/account lookup. Rejected because it mixes
selection/acquisition with evaluation and makes risk sizing effectful even when all policy data is already present.

## Risks / Trade-offs

- [Generic policy failure `E` may lengthen signatures] → Keep it covariant, provide `Nothing` for total policies and
  focused type aliases at application edges; do not erase legitimate domain causes for cosmetic brevity.
- [An open policy can return malformed directives] → Treat directives as untrusted pure output and validate every fee
  identity/index centrally before constructing `ScenarioFees`.
- [Slice-index attribution is sensitive to ordering] → `MatchedSlices` already defines stable scenario order; retain
  both index and selected slice in assessed output and make codecs preserve order explicitly.
- [Different leg policies increase call-site data] → Provide `RoundTripFeePolicies.same` while preserving the honest
  product in the canonical API.
- [Multi-slice price normalization could diverge from current behavior] → Add one-slice equivalence and existing
  multi-slice regression tests before deleting `Valuation.scenarioPricePnl`.
- [Accumulating generic and domain errors can become complex] → Keep explicit stage/branch ordinals, domain-owned
  aggregates, and typed wrappers; prohibit catch-all exception/message conversion.
- [Scenario-level result duplicates some core observations] → Retain references to assessments/contributions/core PnL,
  not recalculated scalar totals, and test that the core value is the sole source of totals.

## Migration Plan

1. Observe the portfolio gate and land Proposals 0–5 before applying this change.
2. Add `feePolicy`, its direct dependencies, completed-JAR task, root aggregation, and forbidden-dependency compiler
   checks.
3. Move `FeeRate` and policy math to `trading.fee`; replace raw-sign validation with refined typed inputs and exact
   quantity operations.
4. Introduce generic pure `FeePolicy[E]`, non-empty policy errors, `SliceIndex`, existential fee directives, no-fee, and
   checked stable composition.
5. Introduce `AssessedFee` and `ScenarioFees`; implement canonical directive validation/attribution from the owned
   scenario and delete caller-supplied market/reference-equality paths.
6. Add exact `ScenarioValuation.pricePnl` and `RoundTripLeg` to execution scenario, then compare all single/multi-slice
   formulas with current behavior.
7. Add explicit entry/exit policy products, staged fee-inclusive evaluation, attributed converted contributions, and
   the scenario-level wrapper around core PnL.
8. Migrate risk/sizing and examples to the new APIs without changing Proposal 7's search behavior yet.
9. Delete `FeeSchedule`, `FeeLine`, `Fees`, denomination percentage/minimum methods, old PnL orchestration, universal
   fee errors, and old capability paths with no aliases.
10. Run formatting, clean compilation, exact/refinement/property/law tests, policy failure/attribution tests, external-
    JAR compiler tests, adversarial tests, and the full repository matrix.
11. Reconcile OpenSpec artifacts if implementation exposes a semantic issue; otherwise prepare for fresh independent
    review without self-certification or Proposal 7 work.

Rollback before release is a source revert to the Proposal 5 boundary; no persisted schema, external policy store, or
runtime state is changed.
