## Context

See `proposal.md` for motivation and the two delta specifications for normative behavior.

The quantities module already provides exact opaque `Quantity[D]`, endpoint-oriented `Rate[A, B]`, `Rate.andThen`, `Quantity.applyRate`, checked runtime dimension recovery, registered grid provenance, and explicit grid narrowing. These operations are backed by `Rational`, so retaining their types through economics calculations does not add a second numerical representation.

The current economics implementation nevertheless has three recurring boundaries:

- instrument validation checks runtime facts and later repeats their conclusions through casts;
- market and valuation code projects rates and quantities to `Rational`, calculates, then attaches result types again;
- activation/pricing definitions and their scenario assumptions are independent ADTs, so scenario validation must reject their invalid Cartesian product.

The design must preserve exact arithmetic and explicit quantization (`INV-Q1`, `INV-Q2`, `INV-G4`), grid and registry independence (`INV-G5`, `INV-A4`), a domain-neutral quantities module (`INV-P1`), minimal proof authority (`INV-P2`), concrete and generic ergonomics (`INV-P4`, `INV-P5`), and real downstream compiler coverage (`INV-T1` through `INV-T3`). The quantities artifact and its settled static/runtime authority model are not changed.

## Goals / Non-Goals

**Goals:**

- Carry dimensional endpoints and validated grid relationships through economics code instead of recovering them after raw arithmetic.
- Turn successful instrument validation into reusable evidence and offer useful accumulated diagnostics without changing existing fail-fast semantics.
- Delete activation/pricing mismatch cases that arise only because independently valid ADTs can be paired arbitrarily.
- Represent non-empty scenario input and effective pricing directly.
- Use small, lawful functional abstractions where they remove manual folds or clarify dependency structure.
- Keep public construction ergonomic for ordinary Scala callers and adapters from untyped data.

**Non-Goals:**

- No change to `Quantity`, `Rate`, `DimRef`, `SameDimension`, grid provenance, registry ownership, or static-dimension authority.
- No public `Category[Rate]`, generic `Field[Rational]`, tagless-final service layer, general predicate DSL, or free-applicative rule language.
- No proof-composition API for `SameDimension`, `SameGrid`, `SameQuantum`, or grid `Embedding` in this change.
- No immutable registry-core extraction, total `Rational.from`, identifier-constructor redesign, affine `Position`/`PositionDelta` split, or arbitrary payoff-leg collection.
- No assumption that sizing risk is monotone and no binary-search optimization.
- No compatibility aliases for replaced pre-release scenario constructors.

## Decisions

### 1. Compose applicative validation stages with monadic transitions

Independent checks use an internal accumulating result equivalent to:

```scala
type Validation[A] = ValidatedNec[Violation, A]
```

Dependent checks remain `Either`-returning transformations. The conceptual flow is:

```text
raw input
  -> independent structural/runtime checks (ValidatedNec)
  -> checked narrowing or resolution (Either)
  -> proof-carrying result
  -> total domain construction
```

This is intentional rather than one universal validation abstraction:

- `ValidatedNec` is applicative, so independent branches can run and preserve every error.
- `Either` is monadic, so a later stage can consume evidence produced by an earlier stage.
- Accumulating `flatMap` would be unlawful because a dependent branch cannot run without its input.

Each rule has one stable ordinal. The accumulating API emits violations in ordinal order; the fail-fast API projects the first violation from the same rule set and maps it to the existing `EconomicsError`. A failed prerequisite ends only the dependent branch; unrelated checks in the same applicative stage still run.

Public errors are domain values such as a closed `DefinitionViolation` hierarchy and `InvalidDefinition(head, tail: Vector[DefinitionViolation])`. `NonEmptyChain` and other Cats error containers remain internal. Scenario diagnostics use the same head-plus-tail domain pattern and retain slice indices as refined or validated context rather than correlated optional fields where practical.

Alternatives rejected:

- Keeping only hand-written fail-fast `Either` loses useful configuration diagnostics.
- Converting the entire pipeline to `Validated` cannot express evidence-dependent stages lawfully.
- Changing existing constructors to return accumulated errors would unnecessarily break deterministic callers; a separate diagnostic path preserves both behaviors.
- A reusable rule DSL is premature without a second interpreter for documentation, schemas, UI, or audit metadata.

### 2. Make `ValidatedDefinition` the proof-carrying parse result

`Definition` remains the raw boundary representation. `Instrument.validate` returns either the domain aggregate error or a constructor-private `ValidatedDefinition`. Its conceptual shape is:

```scala
sealed abstract class ValidatedDefinition private[instrument] (
  val raw: Definition
):
  private[instrument] val positionGrid:
    RegisteredGridRef[raw.roles.position.D]
  private[instrument] val priceGrid:
    RegisteredGridRef[Divide[raw.roles.quote.D, raw.roles.base.D]]
  private[instrument] val basePerPosition:
    Rate[raw.roles.position.D, raw.roles.base.D]
  private[instrument] val quotePerPosition:
    Rate[raw.roles.position.D, raw.roles.quote.D]
```

The exact representation may use a private final implementation behind the public abstract type, but callers cannot construct it or extract general retagging evidence. Checked narrowing is localized to helpers that first prove object-role identity, registry ownership, and exact `DimKey` equality. Any unavoidable phantom cast occurs only inside that protected boundary.

`Instrument.fromValidated` is total. Existing `Instrument.create(raw)` delegates to validation, maps the first ordered violation to its current `EconomicsError`, and then calls total construction. The instrument stores the already typed grids and payoff rates; downstream capabilities no longer recast the payoff.

This applies “parse, do not repeatedly validate” without making a validated wrapper part of every later economics value.

Alternative rejected: making listing and payoff constructors themselves require the final role path would make deserialization and adapter construction cumbersome and would still need a raw boundary representation.

### 3. Keep settlement conversion and valuation endpoint-typed

`SettlementConversion` retains a dependent source and its rate:

```scala
final class SettlementConversion[S <: Dim] private[instrument] (
  val source: AssetRef
)(
  val target: AssetRef { type D = S },
  val rate: Rate[source.D, S]
):
  def coefficient: Rational = rate.coefficient
```

`MarketState` stores conversions as existential `SettlementConversion[S]` values rather than `(AssetRef, Rational)` pairs. Lookup first checks asset identity, registry provenance, and runtime dimension equality. It then aligns the supplied source quantity with checked `SameDimension` evidence and applies the stored rate. This recovery grants no grid identity or reusable public proof authority.

Market anchors are derived with typed operations wherever their endpoints permit it:

- quote anchor: `price.rate.andThen(quoteToSettle)`;
- base anchor: checked reciprocal/cross-rate composition from the positive price;
- dual anchors: endpoint types are already fixed; coefficient comparison checks market coherence.

Valuation becomes the direct typed expression already required by the canonical spec:

```scala
val settlePerPosition =
  basePerPosition.andThen(state.baseToSettle) +
    quotePerPosition.andThen(state.quoteToSettle)

val value = position.quantity.applyRate(settlePerPosition)
val pnl   = exitValue - entryValue
```

Scenario PnL, converted fee totals, net PnL, and downside risk likewise fold `Quantity[S]` values from an authoritative typed zero. Coefficients remain appropriate for sign tests, comparisons, error payloads, grid coordinates, and public observation, but not as the main calculation language.

Alternative rejected: retaining scalar storage and adding more comments or result constructors still leaves reversed or incompatible intermediate rates indistinguishable to the compiler.

### 4. Associate scenario evidence with activation and pricing shapes

Activation uses associated evidence rather than a second independent sum type:

```scala
sealed trait OrderActivation[P]:
  type Evidence

  def validate(
    evidence: Evidence
  ): Either[ActivationViolation, CheckedActivation[P]]
```

Each concrete activation supplies a constructor for its exact evidence shape. Fixed evidence accepts only an observed price. Trailing evidence accepts only a favorable extreme and observed price. Activation-owned reference and comparison values are captured from the activation instead of duplicated in evidence. Immediate activation has only its no-observation evidence. Builders operate from a stable activation value so the relevant type is `activation.Evidence`.

The associated member excludes cross-shape reuse, while fixed and trailing evidence also retains the private semantics of the exact activation value that constructed it. Because the public evidence class is intentionally shared within a shape, `validate` checks that captured reference, comparison, and trigger price or trailing offset against the current activation before accepting the observations. Thus semantically identical same-shape values remain usable, but evidence from a different same-shape instruction returns a typed mismatch instead of authorizing it.

Pricing follows the same pattern:

```scala
sealed trait Pricing[P]:
  type Resolution

  def resolve(
    resolution: Resolution
  ): Either[PricingViolation, EffectivePricing[P]]

enum EffectivePricing[+P]:
  case Market
  case Limited(price: P)
```

Direct market/limit pricing has a unit-like resolution constructed by that shape. Pegged pricing accepts only its associated reference-price/resolved-price input, captures its own reference and offset, and returns a checked limited price or a semantic failure. Execution delegates its associated resolution to its pricing shape.

Peg resolution similarly retains the private reference and offset of the pegged value that constructed it. `resolve` checks those captured semantics against the current pegged value, closing same-shape replay while preserving concrete stable-path and generic associated-resolution forwarding.

Scenario assumption construction uses dependent parameter lists tied to the stable order activation and execution values. If Scala inference becomes awkward, focused variant-specific helper methods may package the same evidence, but the public fallback must not reintroduce an unrestricted `ActivationAssumption × OrderActivation` or `PricingAssumption × OrderPricing` product.

This removes supported construction paths for the current missing, unexpected, fixed-versus-trailing, and direct-versus-pegged mismatch cases. Unsatisfied triggers, nonpositive derived trailing thresholds, invalid peg offsets, runtime instrument mismatch, and other genuinely semantic failures remain typed errors.

Alternative rejected: adding more pattern matches to the current independent ADTs improves formatting but preserves the invalid Cartesian product. Adding activation/pricing type parameters to every `Order` and scenario type could encode the relationship too, but would materially worsen ordinary public signatures; associated members keep the extra indices local.

### 5. Use a non-empty vector for slices and stage scenario diagnostics

`ScenarioAssumptions.matchedSlices` becomes `NonEmptyVector[LiquiditySlice[...]]`. This intentionally exposes the one Cats data type whose invariant is part of the public domain model. One-slice and many-slice helpers keep construction concise, and untyped adapters validate emptiness once.

After activation and pricing resolve, `EffectivePricing` drives slice checks without `Some`/`None` interpretation. Per-slice liquidity and price-quality checks are independent and use indexed applicative traversal to accumulate every violation in stable slice order. Identity validation and lot conservation are separate stages; later checks do not run when the typed prerequisite they consume is absent.

The fail-fast scenario constructor selects the first domain violation from the same staged rules. The diagnostic constructor returns the domain head-plus-tail aggregate. No separate scalar view of an order is introduced.

### 6. Keep Cats selective and internal except for non-empty slices

The economics module adds `cats-core` as a compile dependency and uses:

- `ValidatedNec` and `mapN` for independent validation;
- `Traverse` for fail-fast vector traversal and indexed validation;
- `Chain` for efficient fee-line and converted-line accumulation before a final `Vector` result;
- `NonEmptyVector` for the public non-empty slice invariant.

Existing public `Either[EconomicsError, A]` service boundaries remain. Cats typeclasses are not added to every domain service, and fee schedules do not become tagless-final programs. A fixed-instrument schedule may use an internal monoidal/Kleisli representation only if it simplifies the existing composition without changing the public `FeeSchedule` contract.

### 7. Make exhaustive sizing a pure stack-safe transition

Sizing continues to evaluate every candidate from one through the cap and stops at the first typed evaluation failure. Replace mutable loop state and non-local return with a tail-recursive or `tailRecM` state containing the next coordinate and greatest satisfying candidate. This is an implementation refactor only: evaluation order, non-monotone correctness, error propagation, and selected result stay unchanged.

No monotonicity claim is introduced, so binary search remains invalid.

## Risks / Trade-offs

- [Path-dependent evidence can be harder for Scala inference] → Add real downstream positive fixtures before completing the API, keep associated indices local, and provide focused helpers for immediate, fixed, trailing, direct, and pegged cases.
- [A public `ValidatedDefinition` could accidentally become retagging authority] → Seal it, keep its constructor and typed witnesses package-private, expose only ordinary definition observations, and add downstream negative authority probes.
- [Accumulated errors can become noisy or misleading] → Give rules stable order and explicit stage dependencies; never run a rule whose prerequisite evidence is unavailable.
- [Two validation entry points can drift] → Derive fail-fast and accumulating results from the same rule definitions and test first-error equivalence.
- [Existential conversion storage requires narrowing after lookup] → Recover only checked `SameDimension` from authoritative runtime identity, localize narrowing, and test foreign-registry and same-key cases independently.
- [Cats becomes visible through public slices] → Limit public exposure to `NonEmptyVector`, keep error accumulation domain-owned, and document the compile dependency as intentional.
- [Scenario API changes are source-breaking] → This is a pre-release change; update examples and compiler fixtures directly and add no compatibility aliases.
- [The combined scope could drift into the quantities kernel] → Treat any required quantities authority or proof API change as a design conflict and split it into a separate OpenSpec proposal.

## Migration Plan

1. Add the economics-only Cats dependency and domain violation/result types without changing behavior.
2. Implement `ValidatedDefinition`, shared ordered rules, total construction, and fail-fast equivalence; migrate instrument internals to the checked evidence.
3. Convert settlement conversion, market derivation, valuation, fee/PnL aggregation, and downside risk to typed operations; lock numerical equivalence with property tests.
4. Introduce associated activation/pricing evidence and `EffectivePricing`, then migrate order/scenario constructors and public examples.
5. Replace slice vectors with non-empty vectors and add staged accumulated diagnostics.
6. Refactor traversal, fee accumulation, and sizing state; run focused and full validation.

Because the API is unreleased, rollback is a normal source revert of this change rather than deprecation scaffolding. Implementation must retain the old fail-fast entry points but need not retain the replaced independent scenario-assumption constructors.

## Deferred Follow-up Roadmap

The following ideas from the combined review remain valuable but need separate scope and review:

1. Add lawful identity, inverse, and composition operations to `SameDimension`, `SameGrid`, and `SameQuantum`, and composition to `Embedding`, while preserving construction authority and provenance.
2. Extract registry conflict resolution into an immutable state transition with a small synchronized witness-interning shell.
3. Add total `Rational.from`, total identifier constructors, structured diagnostic paths, refined slice indices, and proof-preserving refinement constructors.
4. Distinguish additive `PositionDelta` from held `Position` if the domain begins modeling positions as affine state rather than only trade deltas.
5. Generalize two payoff legs to a foldable linear-map representation only if instruments genuinely need arbitrary legs.
6. Introduce a reusable or free-applicative rule algebra only when validation needs another interpreter such as documentation, schema, UI, or audit metadata.

The roadmap explicitly does not include a global `Category[Rate]`: lawful identity construction still requires authoritative `DimRef[D]`, which a generic category identity cannot obtain.
