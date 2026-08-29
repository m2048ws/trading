## Context

See [proposal.md](proposal.md) for motivation. The economics module currently preserves downstream cross-instrument safety by declaring almost every public value and capability as a nested member of `Instrument` and by keeping every implementation that constructs a sealed owned value in `Instrument.scala`. The current implementation then extracts scalar `Plan` and `View` records so package-level helper objects can validate behavior without receiving construction authority.

That arrangement satisfies the package-spoof boundary, but it creates three representations of the same concepts: nested public interfaces, private forwarding/implementation classes, and generic scalar plans or views. It also means numeric refinements such as positive lots and positive prices are guaranteed mainly by public abstraction and construction convention rather than by their concrete internal representation.

The design must preserve:

- one stable instrument path rejecting values from another equal-looking instrument;
- registry and grid provenance independent of dimension equality;
- exact rational arithmetic and explicit quantization;
- private construction of validated aggregates;
- public and same-package-spoof resistance without treating package qualification as authority;
- the existing product-family-neutral valuation, complete-scenario, fee, and sizing semantics;
- a domain-neutral `quantities` artifact.

## Goals / Non-Goals

**Goals:**

- Give instrument ownership one explicit technical representation that every owned value and capability shares.
- Represent positive values and mutually exclusive domain cases directly so invalid local shapes are unrepresentable.
- Let value-local observations live on the values and reserve capabilities for construction, aggregation, and cross-value behavior.
- Give each validation rule one clear owner and eliminate repeated scalar projections used only to recover domain shape.
- Permit concern-oriented implementation files without exposing trusted constructors or owner retagging.
- Keep ordinary and generic downstream call sites centered on a stable `instrument` value and its aliases.

**Non-Goals:**

- Introduce separate public `Contract`, `Listing`, venue, execution, account, ledger, margin, or lifecycle aggregates.
- Generalize the two-leg base/quote payoff into an arbitrary asset-vector payoff without a concrete additional-leg requirement.
- Add a generic validation framework, builder DSL, pass-through vector wrapper, or public proof family solely to shorten parameter lists.
- Change economic formulas, sizing traversal, fee sign conventions, quantization policy, textual parsing responsibility, or the quantities API.
- Preserve source compatibility with the unreleased capability-forwarding and kind-plus-option API.

## Decisions

### 1. Give every instrument a private generative owner authority

`Instrument` remains the public stable aggregate and introduces an abstract owner type whose concrete identity is fresh for each successfully constructed implementation. Conceptually:

```scala
sealed trait Instrument:
  type Owner

  type Lots         = InstrumentLots[Owner, position.D]
  type PositionLots = InstrumentPosition[Owner, position.D]
  type Price        = InstrumentPrice[Owner, base.D, quote.D]
  // Further public aliases hide owner-indexed carrier parameters.
```

The exact carrier arities may include private grid types where required, but callers continue to name `instrument.Lots`, `instrument.Price`, and the other aliases. The owner type is not a caller-selected public parameter.

Construction and representation access require an `OwnerAuthority[Owner]`. That authority is a sealed internal carrier whose implementations live in the same source as the instrument implementation, has no public factory, and is not exposed by `Instrument`. Concern implementations may receive it when the private instrument implementation wires its capabilities, but callers declaring `package trading.economics` cannot implement, obtain, or substitute one.

The packaged JVM boundary enforces the same rule independently of Scala/TASTy sealing. The Scala owner authority is final and its constructor requires a package-private raw JVM gate with a private constructor; successful checked `Instrument.create` is the gate's only issuer. Every emitted trusted carrier, aggregate, concrete implementation, and capability constructor requires either that raw gate or the gated final authority. Public Scala inspection types remain directly readable but cannot be implemented into usable values without the gate. Scala-only `sealed` or package-private metadata is not treated as sufficient Java access control.

This replaces "all constructors must remain in one file" with the narrower invariant "all trusted construction requires the unforgeable issuing authority." Package-private visibility alone is not trusted.

Alternatives considered:

- Keep all nested implementations in `Instrument.scala`: rejected because it makes authority and every domain concern share one physical boundary.
- Use only `private[economics]` constructors in separate files: rejected because downstream source can declare the same package.
- Use top-level services parameterized by a structural `Instrument`: rejected because structural projections obscure stable paths and do not provide construction authority.
- Expose a public owner token or evidence type: rejected because callers must not select or manufacture ownership authority.

### 2. Use refined owner-indexed carriers for lots, positions, and prices

The internal payload of `InstrumentLots` and `InstrumentPrice` is a `Positive[GridQuantity[...]]` on the issuing grid. `InstrumentPosition` contains an unrestricted signed `GridQuantity` on the position grid. Their constructors require the matching owner authority and grid witness.

Each carrier exposes only intrinsic observations and mathematically closed operations:

```text
InstrumentLots
  count
  exact quantity
  positive addition where needed

InstrumentPosition
  signed count
  exact quantity

InstrumentPrice
  positive ticks
  exact coefficient
  quote-per-base rate
```

An operation such as subtraction or negation that can violate positivity returns an unrestricted or checked result, not `InstrumentLots` or `InstrumentPrice`. The raw grid payload and its unrestricted arithmetic are not exposed through the owned carrier.

This reuses the existing quantities refinements instead of adding another positivity proof family. Keeping `Lots` and `PositionLots` as two abstract names for the same concrete grid type was rejected because the implementation itself then receives no help from the type checker.

### 3. Compose instrument construction from four semantic components

Instrument construction is organized around:

```text
InstrumentIdentity
  instrument id
  underlying id

InstrumentRoles
  base
  quote
  position
  settle

ListingRules
  position lot grid
  quote-per-base price grid

ContractPayoff
  signed base per position
  signed quote per position
```

`InstrumentRoles` is the stable dependent root used when constructing `ListingRules` and `ContractPayoff`, so grid dimensions and rate endpoints remain connected to the exact role witnesses. Component constructors check local invariants; final `Instrument` construction checks registry sharing, cross-component identity, distinct base/quote roles, price dimension, and nonempty payoff exactly once.

The public construction API may use dependent parameter lists or a small staged constructor to preserve those relationships, but it must culminate in `Instrument.create(definition)` or an equivalently cohesive final boundary rather than another ten-argument forwarding method. The implementation first produces one private validated definition and performs all existential witness casts at that boundary before creating owner authority.

A public `Contract`/`Listing` split was rejected for this change because the repository has no requirement for one contract to own multiple listings. The semantic components preserve that future seam without asserting a second public aggregate today. A generalized payoff vector was likewise rejected; the existing two signed legs receive a real `ContractPayoff` name and invariant without speculative generality.

### 4. Replace kind-plus-option records with safe domain alternatives

Activation and execution mechanics become owner-aware closed alternatives. Conceptually:

```scala
Activation[O, P] =
  Immediate
  | Fixed(reference, comparison, triggerPrice: P)
  | Trailing(reference, comparison, offset: PositiveWhole)

Pricing[O, P] =
  Limit(price: P)
  | Pegged(reference, offsetTicks)

Execution[O, L, P] =
  Market(nonRestingTimeInForce)
  | Priced(pricing: Pricing[O, P], timeInForce, liquidity, visibility: PricedVisibility[O, L])

PricedVisibility[O, L] =
  Displayed
  | Hidden
  | Iceberg(displayedLots: L)
```

There is no market `Visibility.NotApplicable`, no market maker-only field, and no record where `kind == Limit` while `limit == None`. A small refined non-resting duration represents immediate-or-cancel or fill-or-kill. Cross-field checks that remain meaningful, such as iceberg display not exceeding order lots or non-resting priced orders not being iceberg, stay at checked execution/order construction.

Safe alternative constructors may be public because their required fields and refined inputs make each local value valid by construction. Validated aggregates such as `Order` remain sealed with non-public constructors. Public construction of a safe activation case is not trusted aggregate construction and does not permit owner retagging.

`OrderIntent` groups side, positive lots, and position effect. The primary boundary becomes conceptually:

```scala
orders.create(intent, activation, execution)
```

Convenience market, limit, stop-market, and stop-limit methods construct these same values and converge on that boundary. No generic `OrderPlan[L, A, P, V]` is retained.

Alternatives considered:

- Keep kinds and optional fields but add validators: rejected because every consumer must rediscover the same case invariants and scenario validation requires partial `.get` operations.
- Put all eight former order fields in an `OrderParams` case class: rejected because it shortens a signature without changing the invalid-state model.
- Encode every order combination as a distinct top-level order class: rejected because activation remains orthogonal to market versus priced execution and a cross-product hierarchy would duplicate behavior.

### 5. Represent scenario evidence as explicit assumptions

Trigger evidence becomes a closed fixed-versus-trailing alternative; favorable-extreme data exists only in the trailing case. Scenario construction accepts one cohesive owner-indexed input:

```text
ScenarioAssumptions
  activation:
    ImmediateAssumption
    Triggered(FixedEvidence | TrailingEvidence)
  pricing:
    DirectPricing
    ResolvedPeg(PegResolution)
  matchedSlices: Vector[LiquiditySlice]
```

The explicit "not required" cases make adapter input inspectable without using independent `Option` fields. Aggregate validation compares those cases with the order's activation and execution alternatives, validates trigger observations and peg offsets, then validates slice totals, limit quality, and liquidity roles. A successful scenario stores the checked assumptions and derived signed position change.

The validator pattern-matches the actual domain alternatives. `ActivationView`, `InstructionView`, `EvidenceView`, `PegView`, `SliceView`, and `OrderView` are removed. Pure scalar helpers remain acceptable only for actual arithmetic predicates, such as comparing two tick coordinates, and not as a second representation of a complete order.

### 6. Put observations on values and keep capabilities authoritative

The seven capability names remain because they are useful discovery points, but their role narrows:

```text
prices     construct and quantize Price
market     construct coherent MarketState
orders     construct Order components and checked Order
scenarios  construct slices, assumptions, scenarios, and round trips
fees       construct fee denominations, attribution, and schedules
valuation  combine positions/scenarios/fees into exact value and PnL
sizing     search candidate scenarios under an exact risk budget
```

Intrinsic observations move to the values:

```text
lots.count / lots.quantity
position.count / position.quantity
price.ticks / price.coefficient / price.rate
marketState.convertToSettle(...)
fee.amount / fee.residual / fee.unrounded
```

This removes root methods and capability methods whose only purpose is to accept one owned value and reveal its own state. Multi-value calculations remain on `valuation`; contextual risk search remains on `sizing`. This rule avoids turning every calculation into an object method while removing the most obvious anemic-domain forwarding.

### 7. Validate fee denomination once

`FeeDenomination` is an instrument-owned dependent package containing:

```text
registered fee asset
matching registered grid
quantization policy
issuing instrument owner
```

Construction checks registry provenance and grid dimension against the asset once. Percentage and exact fee construction are operations of the denomination and therefore require only fee kind plus the appropriately typed basis or amount and fee rate where applicable. A schedule can capture and reuse a denomination.

The denomination does not imply that the fee asset equals settlement, does not choose a fee basis, and does not become instrument metadata. `FeeSchedule` remains contextual and may implement arbitrary venue/account logic. A generic `FeeParams` record was rejected because it would repeat the same validation for every component without naming the stable denomination policy.

### 8. Assign validation by invariant ownership

Validation follows three boundaries:

```text
local smart construction
  positive lots/price, positive trailing distance, identifier validity

structural domain construction
  activation/pricing/visibility case shape, fee denomination

aggregate construction
  registry and dimension coherence, order compatibility,
  market anchor equation, scenario evidence and lot conservation,
  round-trip closure, fee attribution
```

Later stages may rely on earlier guarantees and SHALL not project values to booleans such as `isMarket` or duplicate coordinates solely to repeat local checks. Validation functions return the domain value or a typed error; they do not return `Unit` when a validated component is the natural result.

Concern-specific violation alternatives replace free-form detail strings where downstream code needs to distinguish failure classes. Human-readable rendering is derived from the structured reason. This change does not introduce a generic validation accumulation framework; existing deterministic first-failure behavior remains unless a canonical spec explicitly requires plural diagnostics.

### 9. Organize implementation around the kernel and real values

`Instrument.scala` retains the public aggregate contract, validated definition transition, concrete owner authority issuance, and private assembly of stable capabilities. Owner-indexed carrier definitions and each concern implementation may live in focused files. Those files receive only the minimal authority or owned values they need.

Capability wiring is direct construction of the concern implementation, not an anonymous façade that forwards every method back into `InstrumentImpl`. Pure formula helpers remain small and domain-shaped. Examples include the payoff/conversion dot product, exact PnL summation, and exhaustive sizing traversal. Generic callback-heavy helpers or plans are retained only when they express a reusable algorithm rather than compensate for inaccessible domain data.

The implementation layout is responsibility-driven; there is no mandatory one-file-per-capability rule or line-count gate. The architectural check is that changing order validation does not require understanding fee construction, and construction authority does not depend on source-package privacy.

## Risks / Trade-offs

- [Owner-indexed carriers expose complex implementation types in compiler diagnostics] → Keep `instrument.Lots`, `instrument.Price`, and other path aliases as the documented surface and add downstream diagnostic/compiler fixtures before migrating the full implementation.
- [A Scala encoding that looks sound on paper may widen or unify owner types unexpectedly] → Begin implementation with focused positive and negative compiler probes for two equal-looking instruments, stable rebinding, generic forwarding, same-package spoofing, and strict ordinary-Java artifact access before building capabilities on the encoding.
- [Public safe alternatives could accidentally expose aggregate authority] → Permit public construction only for locally valid structural alternatives; require owner authority for owned primitive payloads and keep aggregate constructors sealed/non-public.
- [Refined wrappers may add allocations or awkward conversions] → Reuse opaque quantity refinements internally where practical, keep raw representation access private, and assess allocation only after the semantic model is correct.
- [Componentizing instrument definition can add ceremony] → Keep the four components small, provide one cohesive final construction path, and avoid builders whose stages add no validation or dependent-type value.
- [Replacing many APIs at once can conceal formula drift] → Preserve characterization/property tests around every exact formula, separate structural migration from formula helpers, and run downstream public fixtures throughout the change.
- [Structured error reasons enlarge the public error model] → Add only reasons corresponding to existing observable failure classes and remove superseded string-only forms before release rather than maintaining two hierarchies.
- [The refactor could reintroduce a public proof family] → Treat owner authority as private construction capability, never as public evidence, and audit packaged APIs plus same-package-spoof fixtures.

## Migration Plan

1. Establish compiler probes for the proposed owner encoding, refined carriers, generic forwarding, and package-spoof resistance before migrating domain behavior.
2. Introduce the private owner authority and owner-indexed lots, position, and price carriers; migrate intrinsic observations and their focused tests.
3. Introduce identity, roles, listing-rule, and payoff components and migrate instrument construction through one validated-definition boundary.
4. Introduce activation, pricing, execution, visibility, intent, and scenario-assumption alternatives; migrate order and scenario validation to pattern-match them directly.
5. Introduce fee denomination and value-local fee observations; migrate schedules and fee-inclusive valuation without changing formulas.
6. Move concern implementations behind the owner kernel, remove anonymous forwarding façades and all superseded `Plan`/`View` representations, and audit remaining helper parameter lists by semantic responsibility.
7. Migrate examples, behavioral/property suites, and downstream compiler fixtures atomically; remove old kinds, optional-field observers, root observers, wide checked constructors, and compatibility aliases.
8. Run focused economics tests, packaged downstream compiler tests, full clean multi-module tests, formatting, strict OpenSpec validation, public artifact inspection, and a fresh independent review before archival.

Rollback reverts the unreleased API and implementation together. No persistence, wire-format, publication, or deployment migration is required.
