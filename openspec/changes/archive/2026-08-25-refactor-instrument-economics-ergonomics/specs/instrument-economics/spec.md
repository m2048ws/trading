## ADDED Requirements

### Requirement: Focused instrument-owned capability surface
A stable `Instrument` SHALL retain identity, asset roles, contract terms, lot/position operations, and its path-owned public types at the instrument root. It SHALL expose focused instrument-owned capability values named `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, and `sizing` for the corresponding operations. Moving an operation behind a capability SHALL NOT weaken private construction authority, path-dependent ownership, exactness, registry provenance, or typed failures.

The superseded flat operation names SHALL NOT remain as permanent forwarding aliases. Public examples and compiler-boundary fixtures SHALL demonstrate the capability-oriented surface as an ordinary downstream caller sees it.

#### Scenario: Discover operations by concern
- **WHEN** a downstream caller has one stable instrument and needs to construct a price, construct a market state, and value a position
- **THEN** those operations are available through `prices`, `market`, and `valuation` respectively without scanning unrelated order, fee, or sizing methods

#### Scenario: Preserve one owner across capability views
- **WHEN** a caller obtains several capability views from the same stable instrument
- **THEN** every view consumes and produces the same instrument-owned lots, prices, market states, orders, scenarios, fees, and PnL types

#### Scenario: Reject a superseded flat call
- **WHEN** downstream source attempts to use a removed flat operation instead of its capability-oriented replacement
- **THEN** compilation fails rather than selecting a compatibility alias with a second public path

## MODIFIED Requirements

### Requirement: Instrument-bound lots, positions, and prices
Each stable `Instrument` value SHALL own its `Lots`, `PositionLots`, and `Price` types so values from different instrument paths cannot be mixed implicitly. `Lots` SHALL be a strictly positive arbitrary-precision coordinate count on that instrument's position lot grid. `PositionLots` SHALL be a signed coordinate on the same grid, including flat zero. `Price` SHALL be strictly positive and grid-constrained on that instrument's quote-per-base price grid.

An instrument SHALL construct `Lots` from a positive count, convert `Lots` plus `Buy` or `Sell` into a signed `PositionLots` change, and expose exact position quantity through the matching lot grid. Its `prices` capability SHALL provide an ordinary exact constructor from a `Rational` coefficient, an endpoint-typed constructor from `Rate[base.D, quote.D]`, and an explicitly low-level tick-coordinate constructor from `PositiveWhole`. The rational and typed-rate exact constructors SHALL have identical positivity and exact-grid-membership behavior. Any rounding operation SHALL be explicitly named and residual-bearing. Price observers SHALL expose both the positive tick coordinate and the endpoint-typed quote-per-base rate.

The economics core SHALL NOT accept `String` as a price-construction input. Textual grammar, parsing errors, and conversion to `Rational` belong to the calling adapter or application boundary.

#### Scenario: Construct positive lots through an instrument
- **WHEN** a caller asks one instrument to construct `1,000` lots
- **THEN** the result is positive `instrument.Lots` whose exact position quantity is coordinate `1,000` multiplied by that instrument's lot quantum

#### Scenario: Reject zero or negative lots
- **WHEN** a caller supplies a zero or negative lot count
- **THEN** `Lots` construction fails

#### Scenario: Derive a signed position change
- **WHEN** buy and sell sides are applied to the same positive lots value
- **THEN** buy produces the positive lot-grid coordinate and sell produces its exact negation as `PositionLots`

#### Scenario: Prevent cross-instrument mixing
- **WHEN** two stable instruments have equal-looking grids or definitions but distinct instrument paths
- **THEN** their lots, positions, prices, orders, and scenarios are not implicitly interchangeable

#### Scenario: Construct an ordinary exact price from a coefficient
- **WHEN** an adapter parses market text to a positive rational coefficient that belongs exactly to the instrument's price grid
- **THEN** `instrument.prices` returns an instrument-bound `Price` without requiring the caller to reconstruct base and quote dimension references

#### Scenario: Preserve listing price grid membership
- **WHEN** an exact rational coefficient or endpoint-typed quote-per-base rate is not on the instrument price grid
- **THEN** exact price construction fails and no hidden quantization occurs

#### Scenario: Reconstruct a price from stored ticks
- **WHEN** a persistence or venue adapter holds a validated positive whole-number coordinate on the instrument's price grid
- **THEN** the explicitly named tick-coordinate constructor reconstructs the corresponding `Price`

#### Scenario: Keep text grammar outside economics
- **WHEN** raw market data contains malformed text or a venue-specific number form
- **THEN** the adapter rejects or parses that text before invoking the economics API, and the core price constructor receives only an exact rational value

### Requirement: Coherent market state and settle-targeted conversions
A market state SHALL contain one positive, grid-valid instrument price and an immutable set of exact positive conversions from relevant assets into the instrument's `settle` asset. The conversion set SHALL provide base-to-settle and quote-to-settle rates, identity conversion for the settle asset, and explicitly supplied additional conversions for fee or other relevant assets. It SHALL NOT fetch market data, choose a conversion route, infer parity, select bid or ask, or round.

The instrument's `market` capability SHALL expose relationship-oriented construction named `quoteSettled` when quote is settlement, `baseSettled` when base is settlement, `fromQuoteAnchor` when quote-to-settle is supplied, `fromBaseAnchor` when base-to-settle is supplied, and `fromAnchors` when both are supplied and checked. When the operation or explicit source and target asset references already determine rate endpoints, its ordinary form SHALL accept a `Rational` coefficient and construct the endpoint-typed rate under the owning authority. A parallel endpoint-typed form SHALL accept an already derived `Rate` without discarding its static dimensions. Both forms SHALL apply identical positivity, identity, registry, and coherence checks.

For price `P` in quote per base, base-to-settle rate `B`, and quote-to-settle rate `Q`, construction SHALL establish exactly `B = P × Q`, equivalently `P = B / Q`. Ordinary single-anchor constructors SHALL derive the other settlement rate exactly. `fromAnchors` SHALL reject exact disagreement. Additional conversions SHALL remain a directly supplied immutable vector because zero, one, or many distinct source assets are valid; the market-state boundary SHALL validate target, provenance, positivity, identity, and duplicate-source invariants for the vector as a whole. All asset and rate endpoints SHALL retain authoritative registry identity.

#### Scenario: Settle in quote
- **WHEN** settle and quote are the same registered asset
- **THEN** `quoteSettled` makes quote-to-settle identity and base-to-settle exactly the listing price

#### Scenario: Settle in base
- **WHEN** settle and base are the same registered asset
- **THEN** `baseSettled` makes base-to-settle identity and quote-to-settle exactly the reciprocal of the nonzero listing price

#### Scenario: Reject a nonidentity settlement anchor truthfully
- **WHEN** settle is base or quote and checked dual anchors satisfy `B = P × Q` but the same-asset settlement conversion has a coefficient other than one
- **THEN** `fromAnchors` fails with an invalid-conversion diagnostic preserving the supplied source, target, and coefficient rather than fabricating a contradictory price

#### Scenario: Settle in a third asset from a scalar anchor
- **WHEN** a positive quote-to-settle rational coefficient is supplied for a third settlement asset
- **THEN** `fromQuoteAnchor` constructs the known endpoint rate and derives base-to-settle by exact composition with the listing price

#### Scenario: Preserve a typed derived anchor
- **WHEN** caller-owned typed arithmetic produces a `Rate[quote.D, settle.D]`
- **THEN** the typed quote-anchor entry point accepts it directly and constructs the same market state as the equal rational coefficient

#### Scenario: Reject incoherent conversions
- **WHEN** independently supplied base-to-settle and quote-to-settle anchors imply a quote-per-base rate different from the grid-valid price
- **THEN** `fromAnchors` fails with the contradictory values preserved for diagnosis

#### Scenario: Validate plural additional conversions
- **WHEN** a caller supplies zero, one, or many additional settle-targeted conversions
- **THEN** the market-state boundary accepts the coherent vector unchanged in meaning and rejects invalid or duplicate source entries without requiring a pass-through collection wrapper

#### Scenario: Require an additional fee conversion
- **WHEN** a fee is denominated in an asset other than base, quote, or settle
- **THEN** settlement valuation succeeds only when the market state contains an explicit checked conversion for that asset

### Requirement: Universal exact contract valuation
An instrument's `valuation` capability SHALL calculate exact settle value per unit of position as:

```text
basePerPosition.andThen(baseToSettle)
  + quotePerPosition.andThen(quoteToSettle)
```

It SHALL calculate position value by applying that rate to the exact signed position quantity, including the instrument lot quantum and lot coordinate. It SHALL calculate price PnL as exit position value minus entry position value for the same signed position. These calculations SHALL remain exact `Quantity[settle.D]` values and SHALL NOT quantize to a settlement, wallet, or storage grid.

#### Scenario: Include lot count and quantum in valuation
- **WHEN** a position contains `n` lot coordinates and each lot represents exact position quantum `q`
- **THEN** `valuation` applies settle-per-position to exact signed quantity `n × q`, rather than omitting or separately approximating lots

#### Scenario: Calculate long and short PnL symmetrically
- **WHEN** equal-magnitude long and short positions are valued at the same entry and exit market states
- **THEN** their price PnL values are exact negations

#### Scenario: Preserve off-grid inverse PnL
- **WHEN** inverse-style valuation produces a rational settle amount not on the settlement asset's storage grid
- **THEN** price PnL remains that exact rational quantity without rounding
