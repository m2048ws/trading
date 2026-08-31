# Task Group 3 — Refined downside-risk mathematics

## Public operation

`Risk.downside(instrument)(pnl)` now returns
`Either[RiskIdentityError, NonNegative[Quantity[instrument.roles.settle.D]]]` from the pure risk artifact.

The operation performs the ordinary runtime `InstrumentId` check before accessing net PnL. For a matching PnL it:

1. constructs typed settlement zero from the retained settlement dimension witness;
2. compares the typed `Quantity` with Cats `Order[Quantity[D]]` backed by the exact quantity order;
3. subtracts a negative PnL from typed zero, rather than extracting or rebuilding a `Rational`; and
4. refines the mathematically nonnegative branch, with the impossible refinement failure quarantined as an internal
   invariant violation.

Zero and profitable PnL return `NonNegative.quantityZero`. There is no grid quantization, floating-point conversion,
custom downside wrapper, effect, or external lookup.

## Budget and transitional API migration

The supported budget representation is the existing `NonNegative[Quantity[S]]`. A raw `Quantity[S]`, including a raw
negative quantity, cannot satisfy that type at the completed-JAR boundary. No `RiskBudget` alias/wrapper is introduced.

The old policy/scenario service was renamed to the explicit `TransitionalRisk` so the final `Risk` companion is owned
by `trading-risk` without duplicate JVM classes across artifacts. Its temporary exhaustive `maxLots` now accepts only
the existing nonnegative refinement and compares exact typed quantities through `Order`; `InvalidRiskBudget` and its
raw sign-validation branch are removed. Its downside method delegates to the new pure operation and maps the focused
identity error only for the legacy service's temporary error surface.

The transitional economics project now has the correct downstream dependency on risk. Risk has no dependency back to
economics, fee policy, or scenarios. Group 8 will remove `TransitionalRisk` and the aggregate after primary/exhaustive
replacement APIs and their migrations exist; no alias for the former `Risk.create` API is retained.

## Evidence

- Exact unit tests cover negative fractional, zero, and positive PnL and a same-static-dimension foreign identity.
- Completed-JAR negative compilation rejects a raw quantity where a nonnegative budget is required and rejects a PnL
  whose settlement dimension is not proven equal to the instrument's settlement dimension.
- The completed-JAR positive client invokes `Risk.downside` directly.
- Existing fee/scenario sizing integration remains behaviorally green under the explicit transitional name and refined
  budget input.
- Focused gate: 4 risk tests, 10 downstream economics tests, and 40 risk/economics completed-JAR compiler tests pass.
