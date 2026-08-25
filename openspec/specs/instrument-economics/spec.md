# instrument-economics Specification

## Purpose
Defines validated trading instruments and the exact, grid-aware market inputs needed to value signed positions uniformly in an instrument's settlement asset.
## Requirements
### Requirement: Downstream economics boundary
Instrument, order, fee, PnL, and risk APIs SHALL be delivered by a downstream economics artifact that depends on the public quantities artifact. The quantities artifact SHALL remain independently usable and SHALL NOT acquire these domain concepts, their construction authority, or a dependency on the economics artifact.

#### Scenario: Consume economics above quantities
- **WHEN** downstream Scala adds the economics artifact
- **THEN** it can use the new domain API together with public `trading.quantity` values and witnesses

#### Scenario: Keep quantities domain-neutral
- **WHEN** downstream Scala depends only on the quantities artifact
- **THEN** instrument, order, fee, PnL, and position-sizing types are absent from that artifact

### Requirement: Validated Instrument identity and roles
The public validated contract/listing type SHALL be named `Instrument`. It SHALL retain a stable instrument identity, an underlying identity that is not required to be a currency, and the quantity-bearing roles `base`, `quote`, `position`, and `settle` as registered asset references. Its construction boundary SHALL require registry-coherent asset and grid witnesses, a position lot grid, a price grid dimensionally equal to quote per base, and signed base-per-position and quote-per-position contract terms. At least one contract term SHALL be nonzero.

Construction SHALL reject foreign registry provenance, mismatched grid dimensions, contradictory instrument definitions, a price grid not dimensionally equal to quote per base, and an economically empty pair of contract terms. A successfully constructed value SHALL require no `ResolvedInstrument` or similarly qualified lasting wrapper.

#### Scenario: Construct a coherent instrument
- **WHEN** a definition resolves registry-coherent base, quote, position, and settle assets; matching lot and quote-per-base price grids; and at least one nonzero signed contract term
- **THEN** construction returns an `Instrument` retaining those exact identities and terms

#### Scenario: Represent a non-currency underlying
- **WHEN** an instrument references an index or basket as its underlying
- **THEN** its underlying identity is retained without manufacturing a fifth currency role

#### Scenario: Reject a foreign grid
- **WHEN** the supplied lot or price grid is owned by another registry or has the wrong dimension
- **THEN** instrument construction fails without retagging the grid or weakening its provenance

#### Scenario: Reject an empty payoff
- **WHEN** both signed contract terms are zero
- **THEN** instrument construction fails because the position has no economic value

### Requirement: Instrument-bound lots, positions, and prices
Each stable `Instrument` value SHALL own its `Lots`, `PositionLots`, and `Price` types so values from different instrument paths cannot be mixed implicitly. `Lots` SHALL be a strictly positive arbitrary-precision coordinate count on that instrument's position lot grid. `PositionLots` SHALL be a signed coordinate on the same grid, including flat zero. `Price` SHALL be strictly positive and grid-constrained on that instrument's quote-per-base price grid.

An instrument SHALL construct `Lots` from a positive count, convert `Lots` plus `Buy` or `Sell` into a signed `PositionLots` change, and expose exact position quantity through the matching lot grid. Price construction from a coordinate SHALL validate positivity; exact-value construction SHALL require exact grid membership, while any rounding operation SHALL be explicitly named and residual-bearing.

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

#### Scenario: Preserve listing price grid membership
- **WHEN** an exact quote-per-base rate is not on the instrument price grid
- **THEN** exact price construction fails and no hidden quantization occurs

### Requirement: Coherent market state and settle-targeted conversions
A market state SHALL contain one positive, grid-valid instrument price and an immutable set of exact positive conversions from relevant assets into the instrument's `settle` asset. The conversion set SHALL provide base-to-settle and quote-to-settle rates, identity conversion for the settle asset, and explicitly supplied additional conversions for fee or other relevant assets. It SHALL NOT fetch market data, choose a conversion route, infer parity, select bid or ask, or round.

For price `P` in quote per base, base-to-settle rate `B`, and quote-to-settle rate `Q`, construction SHALL establish exactly `B = P × Q`, equivalently `P = B / Q`. Ordinary constructors SHALL derive one settlement rate from the price and one supplied anchor where possible; a checked constructor accepting both rates SHALL reject exact disagreement. All asset and rate endpoints SHALL retain authoritative registry identity.

#### Scenario: Settle in quote
- **WHEN** settle and quote are the same registered asset
- **THEN** quote-to-settle is identity and base-to-settle is exactly the listing price

#### Scenario: Settle in base
- **WHEN** settle and base are the same registered asset
- **THEN** base-to-settle is identity and quote-to-settle is exactly the reciprocal of the nonzero listing price

#### Scenario: Reject a nonidentity settlement anchor truthfully
- **WHEN** settle is base or quote and checked dual anchors satisfy `B = P × Q` but the same-asset settlement conversion has a coefficient other than one
- **THEN** market-state construction fails with an invalid-conversion diagnostic preserving the supplied source, target, and coefficient rather than fabricating a contradictory price

#### Scenario: Settle in a third asset
- **WHEN** a positive quote-to-settle anchor is supplied for a third settlement asset
- **THEN** base-to-settle is derived by exact composition of the listing price and the anchor

#### Scenario: Reject incoherent conversions
- **WHEN** independently supplied base-to-settle and quote-to-settle rates imply a quote-per-base rate different from the grid-valid price
- **THEN** market-state construction fails with the contradictory values preserved for diagnosis

#### Scenario: Require an additional fee conversion
- **WHEN** a fee is denominated in an asset other than base, quote, or settle
- **THEN** settlement valuation succeeds only when the market state contains an explicit checked conversion for that asset

### Requirement: Universal exact contract valuation
An instrument SHALL calculate exact settle value per unit of position as:

```text
basePerPosition.andThen(baseToSettle)
  + quotePerPosition.andThen(quoteToSettle)
```

It SHALL calculate position value by applying that rate to the exact signed position quantity, including the instrument lot quantum and lot coordinate. It SHALL calculate price PnL as exit position value minus entry position value for the same signed position. These calculations SHALL remain exact `Quantity[settle.D]` values and SHALL NOT quantize to a settlement, wallet, or storage grid.

#### Scenario: Include lot count and quantum in valuation
- **WHEN** a position contains `n` lot coordinates and each lot represents exact position quantum `q`
- **THEN** position value applies settle-per-position to exact signed quantity `n × q`, rather than omitting or separately approximating lots

#### Scenario: Calculate long and short PnL symmetrically
- **WHEN** equal-magnitude long and short positions are valued at the same entry and exit market states
- **THEN** their price PnL values are exact negations

#### Scenario: Preserve off-grid inverse PnL
- **WHEN** inverse-style valuation produces a rational settle amount not on the settlement asset's storage grid
- **THEN** price PnL remains that exact rational quantity without rounding

### Requirement: Product-family normalization without core flags
Spot, linear, inverse, and quanto-style definitions SHALL normalize into the same signed base/quote contract terms and market-state conversions. Core valuation SHALL NOT branch on public `isSpot`, `isLinear`, `isInverse`, `isQuanto`, contract-family enums, or venue-specific multiplier fields. Venue adapters MAY interpret source metadata to produce the validated generic terms.

#### Scenario: Normalize a linear payoff
- **WHEN** an adapter supplies a signed base-per-position term and zero quote-per-position term
- **THEN** universal valuation produces the linear price-dependent settle value through base-to-settle conversion

#### Scenario: Normalize an inverse payoff
- **WHEN** an adapter supplies zero base-per-position and a signed quote-per-position term with settle equal to base
- **THEN** universal valuation uses the reciprocal-price quote-to-settle conversion and produces inverse PnL without an inverse branch

#### Scenario: Normalize a quanto-style payoff
- **WHEN** price and an explicit third-asset settlement anchor produce coherent base-to-settle and quote-to-settle rates
- **THEN** universal valuation produces the quanto-style result without a quanto branch
