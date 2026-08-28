## Context

See `proposal.md` for motivation and the four delta specifications for normative behavior. This is Proposal 4 in the
architecture portfolio and depends on the preceding quantity/reference-data split, immutable catalog, and instrument-
assembly boundary. In particular, it assumes the progression:

```text
InstrumentDefinition + CatalogSnapshot
                  |
                  v
           InstrumentSpec
                  |
                  v
             Instrument
```

The current `economics` project places all production code in `trading.economics.instrument`. `Instrument` retains
static meaning but also creates seven capability views: prices, market state, orders, scenarios, fees, valuation, and
sizing. `MarketState` and fee conversion preserve some endpoint types, yet heterogeneous paths still perform registry-
provenance checks during calculation. `Valuation.pnl` evaluates a fee schedule over a round-trip scenario, converts its
lines, and aggregates them in one service, so the pure formula depends upward on order, scenario, and policy types.

The code is unreleased. Migration may therefore remove old paths directly, but the repository must stay buildable at
every applied proposal boundary. Proposals 5–7 will create final order/scenario, fee-policy, and risk artifacts; this
proposal establishes their common dependency first and leaves their code in one transitional downstream artifact.

## Goals / Non-Goals

**Goals:**

- Establish one physically enforced, pure instrument-economics dependency root.
- Preserve assembly proofs in path-dependent handles and endpoint-typed rates all the way through valuation.
- Put each smart constructor beside the value whose invariant it establishes.
- Make runtime reconciliation explicit only where heterogeneous input makes it necessary.
- Separate exact fee values and PnL composition from fee-policy and scenario orchestration.
- Leave higher concerns in packages that can move to their final artifacts without changing the core again.

**Non-Goals:**

- Do not design the final order/scenario, fee-policy, or risk module APIs beyond reversing their dependency on
  `Instrument`; Proposals 5–7 own those designs.
- Do not acquire prices, choose conversion routes, select a fee schedule, or build risk scenarios.
- Do not add `F[_]`, tagless-final services, Cats Effect, ZIO, streams, actors, persistence, clocks, tracing, metrics, or
  transaction semantics.
- Do not introduce generative instrument-owner types or a catalog-lineage type parameter across economics.
- Do not turn every semantic wrapper into a public algebra instance when its operation is only partial across runtime
  instrument identities.
- Do not add compatibility aliases or deprecated forwarding methods for the old instrument capability surface.

## Decisions

### 1. Create the final instrument-economics artifact now

Add the following SBT project:

```text
SBT ID:       instrumentEconomics
directory:    instrument-economics
artifact:     trading-instrument-economics
package root: trading.economics.instrument
depends on:   trading-quantities, trading-reference-data
```

It contains:

- `InstrumentDefinition`, `InstrumentAssembler`, `InstrumentSpec`, and `Instrument` from Proposal 3;
- `InstrumentId`, `UnderlyingId`, roles, listing, and payoff;
- `Lots`, `PositionLots`, and `Price`;
- `SettlementConversion` and `MarketState`;
- `FeeKind`, `FeeDenomination`, and `Fee` as economic values;
- `PricePnl`, `SettledFeeContribution`, and `Pnl`;
- pure valuation functions and core-specific error ADTs.

The artifact may use `cats-core` for pure applicative validation and lawful data operations. It has no effect runtime.
Its production classpath is tested from a completed JAR, not merely inferred from source-package discipline.

The existing `economics` project remains temporarily, depends on `instrumentEconomics`, and contains downstream code
under conceptual package roots:

```text
trading.order
trading.scenario
trading.fee.policy
trading.risk
```

This avoids a split JVM package while making later physical extraction mechanical. Proposals 5–7 replace the
transitional aggregate; it is not a second enduring economics abstraction.

Alternative considered: rename the existing project and leave all source in it until the final split. Rejected because
the core could still import higher concerns and tests would not prove the desired dependency direction.

Alternative considered: create separate artifacts for price, market state, fee values, and valuation. Rejected because
they form one cohesive economic kernel around the same assembled instrument, and those extra publication boundaries
would add ceremony without independent consumers or release lifecycles.

### 2. Make Instrument a static value and use value-oriented constructors

`Instrument.fromSpec` remains total and retains only assembled semantic data. It has no `prices`, `market`, `orders`,
`scenarios`, `fees`, `valuation`, or `sizing` fields and no generic `services` or `capabilities` replacement.

Smart constructors live with their result types and take a stable instrument in the first parameter list, conceptually:

```scala
Lots.fromCount(instrument)(count)
PositionLots.fromCoordinate(instrument)(coordinate)
Price.exact(instrument)(coefficient)
Price.fromRate(instrument)(rate)
Price.fromTicks(instrument)(ticks)
MarketState.quoteSettled(instrument)(price, additional)
FeeDenomination.create(instrument)(asset, grid, policy)
Valuation.pricePnl(instrument)(position, entry, exit)
```

Separating the instrument parameter permits return types to refer to its path-dependent role dimensions. Companion
names make the constructed invariant visible and avoid replacing the old service locator with a monolithic
`InstrumentEconomics` facade. Small Scala 3 extensions may forward to the canonical companions only when they improve
local readability and do not create duplicate authority or API paths; the companions remain the single construction
boundary.

Alternative considered: retain only the four intrinsic capability views (`prices`, `market`, `fees`, `valuation`).
Rejected because it preserves an arbitrary ownership pattern and encourages every future operation to seek a slot on
`Instrument`.

Alternative considered: expose only free functions in one utility object. Rejected because unrelated constructors
would again be grouped together and the invariant established by a call would be less discoverable.

### 3. Put order side outside signed position economics

`PositionLots` represents a signed coordinate on the position grid, including zero. The pure core constructs it from a
signed coordinate and observes its exact typed position quantity. `Buy` and `Sell` are order instructions, so applying
side to positive `Lots` moves to order-intent construction in the downstream order package.

For one fixed instrument/grid, signed coordinates inherit the additive group of integers and exact quantities. The
implementation uses those lawful underlying operations rather than reimplementing scalar arithmetic. It does not
publish an unconditional global `Group[PositionLots]`: values with different runtime `InstrumentId`s cannot be combined
totally, and the additive identity also needs a specific instrument/grid context. Checked public combination first
reconciles identity, then delegates to the underlying algebra.

Alternative considered: keep `Side` in instrument economics as a generic sign. Rejected because the only current
meaning is an order instruction and it would create an upward domain dependency for one convenience operation.

Alternative considered: publish a global algebra and ignore instrument identity. Rejected because that would make an
invalid cross-instrument operation appear lawful.

### 4. Preserve proofs instead of reconstructing them from Rational

The kernel follows this representation rule:

```text
boundary scalar
    |
    | checked once
    v
Rate[D1, D2] / Quantity[D] / GridQuantity[D, G]
    |
    | typed composition
    v
typed economic result
```

`Rational` remains the exact scalar carrier and is valid at definition/adapter boundaries, for comparisons,
diagnostics, and coefficient observers. Once endpoints or dimensions are known, the calculation retains `Rate`,
`Quantity`, `GridQuantity`, positive refinements, and named economic result types. It does not extract coefficients,
calculate the main formula in raw scalars, then cast or rebuild the type proof.

The universal payoff formula is expressed through typed rate composition and addition:

```scala
basePerPosition.andThen(baseToSettle) +
quotePerPosition.andThen(quoteToSettle)
```

Position value applies the resulting position-to-settle rate to `PositionLots.quantity`. Exit-minus-entry uses the
quantity additive group and returns `PricePnl`, which retains the instrument and settlement context as well as the exact
quantity.

`PricePnl` is introduced because “price component for this instrument” is a meaningful invariant later PnL composition
must check. A separate wrapper for every intermediate rate or position value is not introduced: their dimension types
already express the relevant invariant and another nominal layer would not rule out a current error.

Alternative considered: keep the existing scalar implementation with local casts after equality checks. Rejected
because it discards evidence already present in the model and makes review depend on re-proving relationships at every
stage.

Alternative considered: introduce opaque wrappers for every formula term. Rejected because types should capture real
invariants or alternatives, not merely mirror every local variable.

### 5. Make MarketState the one pure heterogeneous conversion boundary

`SettlementConversion[S]` retains a source `Asset { type D }`, the instrument settlement asset, and
`Rate[D, S]`. Additional conversions use an existential package whose source type and rate endpoint remain coupled;
they do not store `(Asset, Rational)` pairs.

`MarketState` construction validates:

- price and instrument identity;
- immutable handle lineage and endpoint identity;
- positive rates and identity conversion truthfulness;
- exact anchor equation `baseToSettle = price.andThen(quoteToSettle)`;
- additional-conversion targets and duplicate sources.

Independent additional-conversion violations accumulate in stable input order. Dependent coherence checks run only
when their endpoints and positivity evidence exist.

After construction, conversion captures the matching existential entry, obtains source-identity/dimension evidence,
and immediately applies the retained typed rate. It may fail for an absent or wrong source identity, but it performs no
catalog lookup, lock, state read, or reconstruction from a scalar. Immutable lineage is checked when the market state
is built, not during each PnL arithmetic step.

Alternative considered: precompute a map from `AssetId` to scalar and rebuild rates on lookup. Rejected because the map
would erase precisely the endpoint dependency the reference-data handles establish.

Alternative considered: require a statically typed field for every possible fee asset. Rejected because fee assets are
open reference data and a heterogeneous checked vector is the honest runtime boundary.

### 6. Split fee values from fee policy

The core owns the representation invariant of a fee denomination and calculated fee:

```text
FeeDenomination = InstrumentId + Asset[D] + GridHandle[D] + QuantizationPolicy
Fee[D]          = denomination + unrounded Quantity[D]
                  + signed GridQuantity[D, G] + residual Quantity[D]
```

`FeeDenomination.create` validates dimension, lineage, and instrument context once. `Fee` construction accepts an exact
typed signed amount, delegates quantization to the retained grid/policy, and proves:

```text
unrounded = quantized + residual
```

The core retains `FeeKind` as descriptive result data, but it does not own `FeeRate`, percentage-basis calculation,
minimums, maker/taker choice, tiers, schedule composition, account selection, or scenario attribution. Those are fee-
policy concerns. A policy calculates an exact `Quantity[D]` and asks the denomination to construct the fee.

Alternative considered: move every fee type downstream. Rejected because denomination/grid coherence, exact
quantization, and the fee value are economic invariants required by pure PnL regardless of how policy selected them.

Alternative considered: keep percentage construction as a convenience on `FeeDenomination`. Rejected because it pulls
one policy model into the otherwise policy-neutral value and invites minimum/tier/venue variants to follow it.

### 7. Represent PnL as typed contribution composition

Downstream scenario/fee policy first produces attributed `Fee` values and selects the market state applicable to each
line. The core then performs two small operations:

1. Convert each `Fee[D]` through that explicit `MarketState` into a `SettledFeeContribution[S]` retaining both the
   original existential fee and exact `Quantity[S]`.
2. Construct `Pnl[S]` from one `PricePnl[S]` and a vector of contributions for the same instrument/settlement asset.

The fee total uses the additive monoid of `Quantity[S]`; the empty vector is zero and order does not affect the exact
sum. `Pnl` retains the vector rather than only its total so rounding and denomination decisions remain observable. The
constructor validates runtime instrument/settlement coherence once and then computes:

```text
feePnl = foldMap(contributions)(_.quantity)
netPnl = pricePnl.quantity + feePnl
```

No global `Monoid[Pnl]` is published. Combining arbitrary PnL values is not total across instrument identities, and
concatenating breakdowns can double-count price components. The lawful algebra is applied at the settlement-quantity
level where its domain is honest.

Alternative considered: keep `Valuation.pnl(roundTrip, schedule)` in the core. Rejected because it reverses the
dependency from economics into scenario and fee-policy and prevents independent reuse for reported or non-order
economic inputs later.

Alternative considered: let PnL accept raw settlement quantities. Rejected because a named contribution retains the
original fee and makes conversion scope, sign, and audit breakdown explicit.

### 8. Use boundary-owned errors, not one economics catch-all

Pure constructors and operations own focused closed errors:

```text
LotError
PriceError
MarketStateViolation / MarketStateViolations
FeeValueError
ValuationError
PnlError
```

Names may be consolidated when two boundaries genuinely have the same remediation semantics, but a universal error
enum SHALL NOT absorb catalog, order, scenario, fee-policy, or risk errors. Downstream layers wrap a typed core cause
with their own context when needed.

`ForeignRegistry` disappears. Construction errors may describe incompatible immutable handle lineage or identity;
calculation errors describe missing conversions or ordinary instrument mismatch. Accumulating boundaries expose a
domain-owned non-empty ordered collection and derive fail-fast as its head.

Alternative considered: retain `EconomicsError` so callers match one type. Rejected because it hides ownership,
continually grows as downstream concerns arrive, and makes the lowest artifact depend conceptually on failures it
cannot produce or remediate.

### 9. Keep effects at future application ports

Every operation in `instrumentEconomics` is a deterministic function of immutable inputs. There is therefore no
`InstrumentEconomics[F[_]]` or tagless-final algebra in this artifact. Abstracting pure calculation behind `F` would
make algebraic composition harder to see, require interpreters with no real environmental choice, and let application
effects migrate inward.

Future application capabilities—market-data loading, persistence, time, streams, tracing, metrics, transactions, and
multiple execution interpreters—may use tagless-final algebras in `trading-application`. They resolve or acquire data,
then call this pure kernel with ordinary values. Runtime interpreters belong in `trading-runtime`.

Alternative considered: parameterize every service now to prepare for future effects. Rejected because effects are not
transitive properties of the calculations they feed; the correct preparation is a stable pure boundary.

### 10. Make the migration source-breaking and mechanically reviewable

There are no deprecated aliases for old capability paths. Compile-negative tests assert absence of the old
`instrument.orders`, `instrument.scenarios`, `instrument.fees`, `instrument.valuation`, and `instrument.sizing` API and
absence of downstream packages from the new artifact.

Behavioral tests compare old and new exact formulas during the migration using fixtures for linear, inverse, and
quanto-style payoffs; quote/base/third-asset settlement; positive/negative/zero positions; charges/rebates; missing
conversions; and off-grid exact PnL. Property/law tests cover quantity/rate preservation and fee conservation. Review
also searches production core source for live catalog types, effect kinds, raw-scalar calculation detours, and imports
from downstream packages.

Alternative considered: keep old capability views as forwarding methods until all downstream modules exist. Rejected
under the pre-release API policy and because duplicate paths obscure whether dependency inversion is complete.

## Risks / Trade-offs

- [Dependent companion signatures may challenge Scala inference] → Put the stable `Instrument` in the first parameter
  list, prototype calls from external compiled fixtures, and use focused helper products without exposing casts.
- [Moving files can look like behavior change] → Separate mechanical moves from semantic rewrites in task order and run
  equivalence/property tests after each slice.
- [The transitional `economics` artifact can be mistaken for the final module] → Name it explicitly in code comments
  and proposal gates, enforce its downward dependency, and require Proposals 5–7 before release.
- [Runtime `InstrumentId` checks remain fallible] → Keep them at aggregate boundaries; do not pretend ordinary IDs are
  static or anti-forgery evidence.
- [Existential fee conversion may tempt casts] → Couple source asset and typed rate in a private existential entry and
  permit narrowing only immediately after public reference-data evidence succeeds.
- [Removing a global error enum increases explicit wrapping] → Let each downstream layer retain typed causes; the
  additional syntax records real responsibility boundaries.
- [Too many nominal wrappers could make simple arithmetic opaque] → Add only `PricePnl` and settled fee contribution
  where they prevent cross-context composition or preserve necessary breakdown; rely on existing typed quantity/rate
  algebra elsewhere.

## Migration Plan

1. Observe the portfolio gate: do not apply this change until Proposals 0–9 are complete, mutually consistent, and
   approved for implementation.
2. Add `instrumentEconomics`, its external-artifact task, dependency graph, and empty package-boundary tests.
3. Move Proposal 3's definition/assembly/spec types and the static `Instrument` into the new artifact without changing
   behavior.
4. Move and reshape lots, signed positions, prices, conversions, market state, fee-value primitives, and valuation in
   small compile-tested slices.
5. Introduce `PricePnl`, `SettledFeeContribution`, and pure `Pnl` composition; compare exact results to the existing
   implementation before deleting the old orchestration path.
6. Move order, scenario, fee-policy, and risk sources to their transitional conceptual packages in `economics` and make
   their constructors consume an explicit instrument.
7. Remove instrument capability fields, forwarding aliases, obsolete registry/provenance errors, and the universal
   catch-all error branches no longer owned by the core.
8. Update downstream and adversarial tests, run formatting, clean compilation, unit/property/law suites, external-JAR
   compile checks, and the repository's full validation matrix.
9. Stop if the change cannot preserve exact outputs or requires a new cross-layer semantic decision; update the
   proposal portfolio rather than silently expanding implementation scope.

Rollback before release is a normal source revert to the preceding proposal boundary; no persisted format or external
state is changed by this proposal.
