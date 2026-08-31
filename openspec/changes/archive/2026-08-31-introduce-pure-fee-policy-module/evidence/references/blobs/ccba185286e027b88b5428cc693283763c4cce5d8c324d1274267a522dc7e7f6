# Task Group 6 — Exact scenario price normalization

## Fee-independent normalization owner

`ScenarioValuation.pricePnl(instrument)(roundTrip)` now belongs to `trading-execution-scenario`. It validates the
round-trip identity, derives signed position lots through `OrderIntent.positionChangeFor`, values every matched slice
at its retained market state with the core `Valuation.positionValue`, negates that value into trade cashflow, and folds
entry and exit settlement quantities into core `PricePnl`.

The operation retains the instrument's exact settlement dimension throughout. It does not calculate or accept a
scalar average price, and it has no fee-policy, quantization, catalog, effect, stream, runtime, or live-data concern.
The transitional fee-policy consumer delegates its price component to this boundary instead of duplicating the fold.

## Closed attribution and checked order intent

`RoundTripLeg` is the closed entry/exit attribution owner. `ScenarioValuationError` separately represents round-trip
identity, located slice-position derivation, located core valuation, and final core `PricePnl` construction failures.
Located causes preserve the leg and zero-based slice index.

`OrderIntent.positionChangeFor(instrument)(sliceLots)` validates both the intent and slice-lot runtime identities, then
returns dimension-indexed `PositionLots`. Side-directed scalar arithmetic remains inside the intent implementation.
`Side` exposes no sign member in either Scala or JVM bytecode, including to same-package Java consumers.

## Exactness and artifact evidence

One-slice linear, inverse, and quanto cases equal core exit-minus-entry valuation exactly. Multi-slice laws cover
weighted long and short linear outcomes, reciprocal inverse valuation, third-asset quanto settlement, and off-grid
rational results. A completed-scenario-JAR client compiles and runs normalization without fee policy; compiler and
packaged-bytecode guards reject the removed leg name, raw side signs, fee policy, quantization, catalogs, effects,
streams, and average-price/coefficient shortcuts.

## Verification

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch orderModel/test executionScenario/test feePolicy/test` | 7 order, 16 scenario, and 31 fee-policy tests passed |
| `sbt -batch adversarialBoundary/test` | 158 passed, including 45 economics/scenario/fee completed-JAR tests |
| Public JVM API inspection | `Side` has no sign member; `ScenarioValuation.cashflow` is private; only typed normalization is public |
| Source and packaged-bytecode audit | no fee, quantization, catalog, effect, stream, scalar coefficient, or average-price shortcut |

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The review loop covered Task Group 6 scope, AC-016 alignment, exact long/short linear/inverse/quanto behavior, typed
identity and settlement preservation, closed and located errors, migration of the transitional consumer, source and
completed-artifact dependency purity, public JVM shape, linear complexity, security applicability, evidence, and
checkpoint integrity. Its first pass found that the packaged-bytecode purity assertion omitted generated valuation
error case classes and did not explicitly reject average symbols. The test now scans every valuation-prefixed class
case-insensitively for all forbidden concerns; the focused 45-test boundary suite passed again. The final pass found no
findings. It changed no file during that pass, required no human triage, and is neither canonical Verify nor Human
Review.
