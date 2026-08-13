# Exact dimension-safe quantity foundation

## Why

Monetary and asset calculations need one mathematically exact interior that is not limited to storage, protocol, or
settlement grids. Values such as `6000.001 USD`, `2/100001 XBT`, and `17/3 EUR` are legitimate exact results even when
no registered grid contains them. Discrete boundaries still need integer coordinates, stable grid identity, explicit
projection, and exact conservation.

## What Changes

- Establish `trading` as a non-published SBT aggregator with the `quantities` project in `quantities/`, published as
  `trading-quantities`.
- Organize production ownership as `trading.quantity`, `trading.quantity.grid`, `trading.quantity.refinement`,
  `trading.quantity.runtime`, and `trading.quantity.algebra`.
- Introduce `Quantity[D]` as the unrestricted exact rational value in dimension `D`.
- Introduce `GridQuantity[D, G]` as a separate integer-coordinate value proven to inhabit grid `G`.
- Keep raw coefficient and coordinate attachment lexically private, with dimension-witnessed quantity construction and
  witness-owned grid construction and inspection even for same-package downstream Scala.
- Make grid-to-quantity embedding canonical and explicit through `asQuantity`.
- Require checked `narrowExactlyTo` or residual-bearing `quantizeTo` when returning an exact value to a grid.
- Define `Rate[From, To]` and `Ratio` as exact quantities without a representation parameter.
- Preserve same-grid coordinate closure while returning unrestricted exact quantities from operations that generally
  leave a grid.
- Add one closed, zero-cost `NonNegative[A]`/`NonZero[A]`/`Positive[A]` refinement lattice, with scalar vocabulary as
  aliases, a closed project-owned `Sign[A]`, and positive weakening that never revalidates.
- Require `NonZero[Quantity[D]]` for quantity division and nonzero evidence for total exact-scalar reciprocal.
- Keep distinct scalar-division operations and expose exact-only standard algebra through one strongest production
  instance per carrier: rational commutative ring, exact quantity vector space, grid integer module, runtime dimension
  group, nonzero-rational multiplicative group, exact orders, and closed refined addition.
- Reuse the standard ring, group, module, and vector-space hierarchy internally without making primitive quantity or
  runtime operations depend on optional algebra imports.
- Add lexically registry-owned runtime witness implementations, heterogeneous grid arithmetic, and logical packing
  specifically for registry-produced grid provenance with immutable quantum interpretation.
- Verify core, grid, refinement, and runtime same-package boundaries, counterfeit provenance rejection, and the
  complete fail-closed Java serialization inventory.
- Verify supported production structures with standard law suites and distinctive exact, graded, projection,
  quantization, division, allocation, normalization, rate, and refinement behavior with reusable project laws and
  independent expected models.
- Keep arbitrary exact quantities unpacked until an explicit numerator/denominator wire schema is designed.

## Scope

This change owns the `trading-quantities` module: exact numbers, dimensions, uniform grids, projection, refinements,
runtime grid identity, logical grid-quantity reconstruction, optional exact algebra, tests, examples, and
documentation. Business and policy layers remain outside this foundation.

## Review Gate

Implementation validation and independent review are separate. The staged foundation must pass its automated checks,
then receive a fresh independent review before the unborn repository's initial commit.
