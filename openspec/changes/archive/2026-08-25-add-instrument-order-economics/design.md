# Design: instrument-bound scenario economics above exact quantities

## Context

See [proposal.md](proposal.md) for motivation. The repository currently publishes only the domain-neutral
`trading-quantities` foundation. That artifact already provides exact rational `Quantity`, grid coordinates,
registered runtime asset and grid witnesses, checked runtime evidence, refinements, rate composition, exact grid
embedding, and explicit quantization. The economics layer must consume those public boundaries without acquiring
quantity-construction authority or moving trading concepts into the foundation.

The inputs in this slice are hypothetical but complete: an order remains an instruction, while an order scenario
supplies all matched-lot, market-state, and liquidity-role assumptions needed for deterministic calculation. The
design must support BitMEX-like linear, inverse, quanto, maker-rebate, and fee-rounding behavior, but venue metadata
must normalize into general types rather than introducing venue or product-family branches into core valuation.

## Goals / Non-Goals

**Goals:**

- Make invalid instrument, market-state, order, scenario, fee, and round-trip combinations unrepresentable after a
  checked public construction boundary.
- Keep lots, positions, prices, orders, scenarios, fees, PnL, and sizing results tied to one stable `Instrument` path.
- Perform all mathematical valuation and conversion exactly, with loss of precision confined to an explicitly chosen
  fee grid and quantization policy.
- Preserve enough component and source information to explain every price and fee contribution to net PnL.
- Leave fee policy extensible without turning `Instrument` into a venue, account, tier, or order-policy container.

**Non-Goals:**

- Define persistence or wire schemas for the new values.
- Model submission, acknowledgement, amendment, cancellation, fills, partial-fill lifecycle, or venue reports.
- Add venue adapters or a BitMEX API model; representative BitMEX-like definitions are test vectors only.
- Optimize the baseline lot search by assuming linear or monotone risk.
- Split the initial production API into speculative subpackages.

## Decisions

### 1. Add one downstream economics artifact

Add SBT project `economics` in `economics/`, published as `trading-economics`, depending on `quantities`. Production
types initially live in `trading.economics`; files may be separated by concern without creating public package
subdivisions. The module uses the Scala standard library and the public `trading.quantity` API, with no new production
dependency unless implementation evidence requires one.

The root aggregates and tests `economics` after `quantities`. The existing test-only downstream boundary is extended
to compile ordinary external Scala against packaged economics and quantities artifacts while retaining its existing
quantities probes. This checks path-dependent ownership and constructor visibility at the actual artifact boundary.

Keeping these types in `quantities` was rejected because it would violate the settled domain boundary. Creating
separate instrument, order, fee, and risk artifacts now was rejected because the types form one first coherent
consumer of quantities and do not yet have independent dependency or release needs.

### 2. Make `Instrument` the validated generative aggregate

`Instrument` is a privately constructed final value returned by a checked factory. It retains:

```text
InstrumentId
UnderlyingId
base:     AssetRef
quote:    AssetRef
position: AssetRef
settle:   AssetRef
position lot grid
quote-per-base price grid
base-per-position contract term
quote-per-position contract term
```

`UnderlyingId` is a nominal identity, not an `AssetRef`: an underlying may be a currency, index, basket, or other
reference, but it is not automatically a quantity-bearing role. Venue adapters can maintain their own association
between a currency-like underlying and an asset. `base`, `quote`, `position`, and `settle` remain authoritative
registry-owned assets because arithmetic uses their dimensions.

The factory accepts existential runtime witnesses, opens them internally, and uses `RuntimeEvidence` plus canonical
dimension keys to prove common registry provenance and the required dimensions. It retains the recovered typed
witnesses in the private implementation. In particular, the lot grid must inhabit `position.D`, and the price grid
must inhabit `Divide[quote.D, base.D]`. The signed contract terms must have endpoints
`Rate[position.D, base.D]` and `Rate[position.D, quote.D]`, and cannot both be zero. Expected validation failures are
returned through the economics error algebra rather than thrown or repaired.

Each stable instrument owns private-representation member types conceptually shaped as:

```scala
instrument.Lots         // positive whole coordinate on the position lot grid
instrument.PositionLots // signed whole coordinate on the same grid
instrument.Price        // positive coordinate on the price grid
```

Their implementation uses opaque wrappers over the matching public grid coordinate, not a second grid system.
`Lots` exposes its positive count and exact position quantity; `PositionLots` exposes its signed count and exact
position quantity; `Price` exposes its grid coordinate and exact quote-per-base rate. Buy and sell map positive lots
to positive and negative position-lot changes respectively. Exact price construction uses exact grid narrowing;
separately named quantizing construction returns the selected price and residual.

A top-level structural record with freely mixable `Lots` and `Price` type parameters was rejected because callers
could accidentally combine equal-looking definitions from distinct listings. A lasting `ResolvedInstrument` wrapper
was rejected because checked resolution is a construction step, not part of the valid domain value's name.

### 3. Normalize product families into signed contract terms

Core value per unit of position is always:

```text
basePerPosition.andThen(baseToSettle)
  + quotePerPosition.andThen(quoteToSettle)
```

Applying that rate to `PositionLots` first embeds the lot coordinate through the instrument's position grid, so lot
count and lot quantum cannot disappear from valuation. A base term captures price-dependent exposure through the
base conversion; a quote term captures quote-denominated exposure, including inverse-style behavior when quote must
be converted back to base settlement. Both terms are signed, allowing contract definitions to express direction and
offset without separate multiplier fields.

Adapters for spot, linear, inverse, and quanto-style listings produce these two terms plus the instrument roles and
market-state conversion anchor. The core never branches on a product-family flag. Product enums and venue multiplier
records were rejected because they duplicate information already present in the dimensional terms and inevitably
diverge across venues.

### 4. Bind coherent settlement conversions to each market state

`instrument.MarketState` contains exactly one `instrument.Price` and one immutable
`instrument.SettlementConversions`. Conversions are a heterogeneous, asset-keyed collection whose entries retain the
source `AssetRef` and an exact positive `Rate[source.D, instrument.settle.D]`. Access is through a type-safe lookup that
opens the stored existential and applies the rate to a quantity carrying the same registered source identity.

Construction always provides identity for the exact settlement asset and proves price coherence between the base and
quote entries:

```text
B = P.andThen(Q)

P : base -> quote
Q : quote -> settle
B : base -> settle
```

When settle is quote, `Q` is identity and `B` is the price. When settle is base, `B` is identity and `Q` is the exact
reciprocal price. For third-asset settlement, the caller may supply either a base-to-settle or quote-to-settle anchor;
the other rate is derived exactly. A diagnostic checked constructor may accept both and rejects coefficient
disagreement. Additional fee-asset conversions are explicit positive entries checked for registry and endpoint
coherence. Duplicate sources and a non-identity settle entry are rejected. In particular, the checked dual-anchor
constructor validates any base or quote role that is also the settlement asset as an exact identity before combining
duplicate sources, preserving the supplied source, target, and coefficient in a typed conversion diagnostic.

This is deliberately not a conversion graph. It does not discover paths, choose between routes, infer stablecoin
parity, fetch prices, select bid or ask, or cache mutable market data. A broad conversion service was rejected because
those policy choices would become hidden inputs to an otherwise deterministic scenario.

### 5. Represent order mechanics as independent instructions

`instrument.Order` binds an instrument-owned `Lots`, a `Side`, and independent closed instruction values for:

- activation: immediate, fixed trigger, or trailing trigger;
- price: market, limit, or pegged;
- time in force: good-till-cancelled, immediate-or-cancel, fill-or-kill, or day;
- liquidity constraint: unrestricted or maker-only;
- position effect: unrestricted or reduce-only;
- visibility: not-applicable, displayed, hidden, or iceberg with positive displayed lots.

Fixed triggers carry Last, Mark, or Index reference, AtOrAbove or AtOrBelow comparison, and a positive grid-valid
trigger price. Trailing triggers carry the same reference and comparison plus a positive price-grid tick offset.
Pegged instructions carry a peg reference and signed price-grid tick offset; they do not pretend to have a resolved
limit before scenario evaluation.

Checked construction uses one compatibility table for universally impossible combinations. For example, market plus
maker-only is rejected, non-resting mechanics cannot carry iceberg visibility, and iceberg displayed lots cannot
exceed order lots. Convenience constructors for market, limit, stop-market, and stop-limit select ordinary defaults
and delegate to the same check. Venue adapters may reject further combinations.

A flat `OrderType` enumeration was rejected because combinations such as stop-limit, pegged maker-only, and
reduce-only iceberg would require a growing cross-product. Putting `LiquidityRole` on the order was rejected because
role describes an outcome for matched quantity, not the instruction or its fill probability.

### 6. Keep activation, peg resolution, and matched slices in checked scenarios

`instrument.OrderScenario` contains the immutable order, the evidence required to resolve its activation or peg, and
a non-empty vector of `instrument.LiquiditySlice`. A slice contains positive instrument-owned lots, its own coherent
market state, and Maker or Taker role. A scenario constructor validates the exact total coordinate before exposing the
complete value.

Fixed-trigger evidence supplies the observed referenced price. Trailing-trigger evidence supplies the relevant
favorable extremum and the activating observation; the threshold is derived from the stored tick offset and checked
against the comparison. Peg evidence supplies a referenced price and resolved effective limit whose tick difference
must equal the instruction offset. This is immutable hypothetical evidence, not an observation stream or lifecycle
record.

Limit validation uses the explicit limit, or the checked resolved peg limit: a buy slice must be at or below it and a
sell slice at or above it. Market slices must be taker, maker-only slices must be maker, and unrestricted limit slices
may mix roles. Slice order is retained and supplies a stable zero-based source index for diagnostics and fee
attribution; it does not assert execution chronology.

`instrument.RoundTripScenario` accepts entry and exit scenarios only when their signed position changes sum exactly
to flat. It retains both legs and exposes the entry change as the held position. It never changes a side or lot count.

Calling these records `Fill` or adding partial completion was rejected because their inputs are assumptions and the
slice total is intentionally complete. Using one average price and one liquidity role was rejected because it would
lose exact grid prices, mixed maker/taker fees, and per-state currency conversions.

### 7. Make fee policy contextual and fee-to-market attribution explicit

`instrument.FeeSchedule` is an immutable policy algebra bound to the instrument path. It may retain venue, account,
tier, and version metadata and receives the complete order scenario, so it can inspect every order axis and every
slice. Percentage, flat, tiered, minimum, visibility-sensitive, and multi-component schedules are implementations or
compositions of that algebra rather than fields or branches in `Instrument`.

A calculated `Fee` is an existential package retaining one registered asset and grid, semantic `FeeKind`, signed grid
amount, and exact residual. The schedule first calculates an exact account contribution, then chooses the fee grid
and `QuantizationPolicy`. The stored conservation rule is:

```text
unrounded account contribution
  = fee grid amount embedded exactly + residual
```

The grid amount is the charged or rebated amount used by PnL; the residual explains discarded quantization difference
and is not charged a second time. Account signs are negative for charges and positive for rebates. `FeeRate` stores
the quoted policy sign, positive for a charge and negative for a rebate, so a percentage helper negates
`nonnegativeBasis * rate` before quantization.

Schedules return zero or more checked `FeeLine` values, not bare fees. A line retains its `Fee`, source slice index,
and the corresponding market state obtained from that exact scenario. The line constructor validates the index and
captures the scenario-owned state; callers cannot attach an arbitrary unrelated market. A per-slice policy naturally
uses that slice. An order-wide fee must explicitly choose an attribution slice or split itself into attributed lines,
because conversion can differ across slices and no universal default is honest.

Returning only `Fee` was rejected because its conversion state would be ambiguous. Taking a conversion map separately
at PnL time was rejected because a caller could accidentally use an exit state for an entry charge. Names such as
`EstimatedFee` and `ReportedFee` were rejected here: exactness follows from the supplied scenario, while report
provenance belongs to a future execution/reconciliation model.

### 8. Calculate round-trip PnL from exact slice contributions

For each scenario slice, the order side turns slice lots into a signed position change. Round-trip price PnL is the
negative sum of those changes valued at their corresponding market states:

```text
pricePnl = -sum(value(signedPositionChange(slice), slice.market))
```

For one entry state and one exit state this is exactly exit position value minus entry position value for the held
position. Summing slices avoids an average-price representation and remains valid for long, short, inverse, and
quanto-style round trips.

The schedule assesses each leg. PnL resolves every `FeeLine`'s original asset through the line's captured market-state
settlement conversions and converts the exact embedding of its signed grid amount. A converted line retains the
original fee, residual, leg, slice index, and exact settlement contribution. The result retains:

```text
pricePnl
convertedFeeLines
feePnl = sum(converted fee contributions)
netPnl = pricePnl + feePnl
```

No PnL field is quantized. A missing fee-asset conversion is a typed failure naming the source leg, slice, and asset.
Collapsing immediately to net PnL was rejected because it would make fee sign, rounding, conversion, and policy errors
unexplainable.

### 9. Use exhaustive discrete evaluation as the sizing baseline

`instrument.PositionSizer` accepts a nonnegative exact settlement risk budget, positive whole-number cap, one
instrument-bound fee schedule, and a total candidate builder:

```text
instrument.Lots -> Either[EconomicsError, instrument.RoundTripScenario]
```

It evaluates candidates in ascending lot-coordinate order from one through the cap through the same public PnL path,
computes `max(0, -netPnl)`, and retains the greatest candidate within budget. If any candidate construction or
evaluation fails, evaluation stops and returns the first error in that deterministic ascending order. If no candidate
fits, the result is `None`; zero is not represented as `Lots`.

Every returned round trip must correspond to the candidate being evaluated: the absolute held-position lot coordinate
must equal the candidate lot coordinate. A mismatch stops evaluation with a typed sizing error retaining both the
candidate and observed held-position coordinates rather than pricing a scenario for different exposure.

The exhaustive implementation is intentionally the reference semantics even when risk is stepped or non-monotone.
Binary search and multiplication of a one-lot result were rejected because minimum fees, tiers, rebates, per-line
rounding, and caller-built scenarios can violate both linearity and monotonicity. A future optimized strategy must
require explicit monotonicity evidence and be tested against this baseline.

### 10. Use typed errors and preserve diagnostic inputs

Expected failures form a public economics error algebra grouped by construction boundary: instrument, market state,
order, scenario, fee, conversion/PnL, and sizing. Errors retain authoritative IDs, grid keys, coordinates, exact
coefficients, leg/slice locations, and nested quantity errors needed for diagnosis. Public factories and calculations
return `Either`; they do not silently coerce, round, default, or catch programming defects such as nulls.

One unstructured string error was rejected because callers must distinguish invalid policy inputs from missing
conversion data and scenario contradictions. Re-exporting private quantity constructors or using casts as a public
escape hatch was rejected; any unavoidable existential opening remains internal and follows checked runtime evidence.

### 11. Validate behavior, laws, and the public artifact boundary

Module tests cover constructor matrices and exact examples for spot-like, linear, inverse, and quanto-style terms;
quote, base, and third-asset settlement; long and short multi-slice PnL; maker charges/rebates; mixed liquidity;
third-asset fees; and minimum/tier/rounding effects on sizing. Property tests cover valuation symmetry, scenario lot
conservation, fee quantization conservation, PnL component sums, risk nonnegativity, and exhaustive maximum selection.

Downstream compiler fixtures prove that values from two stable instrument paths do not mix, private constructors are
unavailable, and ordinary consumers can build and calculate through the public API. These supplement rather than
replace same-module behavioral tests.

## Risks / Trade-offs

- [Path-dependent APIs can be less ergonomic in generic collections] -> Keep operations on the owning stable
  instrument, provide dependent-method façades, and add downstream usage examples before stabilizing conveniences.
- [Existential runtime assets and fee lines are implementation-sensitive] -> Seal their packages, open them only behind
  checked registry evidence, and test foreign-registry and mismatched-endpoint attempts at the artifact boundary.
- [A complete scenario may be mistaken for an executed trade] -> Keep execution identifiers and lifecycle language
  absent, document scenarios as conditional inputs, and reserve fills/reports for a separate capability.
- [Order-wide fee conversion has no universally correct slice] -> Require every fee line to select a checked source
  slice explicitly; never default to first, last, entry, exit, or settlement parity.
- [Exhaustive sizing is expensive for very large caps] -> Make the cap explicit, keep the baseline simple and correct,
  and defer optimized search until a monotonicity contract exists.
- [The first economics slice has a broad public surface] -> Keep one package and one coherent calculation path, avoid
  venue adapters and persistence, and require public-consumer tests for ownership guarantees.
- [Venue conventions may use different fee rounding and notional bases] -> Put both decisions in immutable fee-policy
  implementations and preserve exact basis, selected policy, grid amount, and residual in tests and diagnostics.

## Migration Plan

This is an additive module with no stored data or API migration.

1. Add the `economics` project and its dependency/test wiring without changing quantities production APIs.
2. Implement the error algebra, nominal IDs, checked `Instrument`, owned values, conversions, and valuation.
3. Add order mechanics, checked scenarios, and round-trip construction.
4. Add fee scheduling, checked fee-line attribution, and exact PnL.
5. Add exhaustive risk sizing and the complete behavioral/property test matrix.
6. Add packaged downstream compiler fixtures and run focused, root, and clean validation.

Rollback removes the additive economics project and its root/boundary wiring. Because quantities remains unchanged and
no persistence format is introduced, rollback requires no data transformation.
