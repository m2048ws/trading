# Trading Execution Scenario

Pure hypothetical execution outcomes owned by `trading.scenario`.

The artifact depends only on instrument economics and the order model. It interprets immutable orders from explicit
evidence without owning or mutating instrument or order definitions, and contains no fee or risk policy, application
services, runtime effects, or live execution lifecycle.

| Concern | `trading-execution-scenario` | `trading-execution-lifecycle` |
| --- | --- | --- |
| Meaning | hypothetical matched outcome | authoritative actual-execution evidence |
| Input authority | caller-supplied checked assumptions and slices | application commands plus source/account-qualified facts |
| Ordering | explicit scenario structure | source-authoritative stream positions or explicit unsequenced evidence |
| Output | checked scenario and exact hypothetical valuation inputs | retained lifecycle knowledge, exact effective exposure, conflicts, incompleteness, and anomalies |
| Relationship | no production dependency on actual execution | no production dependency on scenarios |

A scenario must never be used as proof that a command was dispatched, an order was accepted, a fill occurred, or a
cancellation became effective. See the [actual-execution lifecycle guide](../execution-lifecycle/README.md) for that
separate authority boundary.

`ScenarioValuation.pricePnl(instrument)(roundTrip)` normalizes a complete checked round trip into exact core `PricePnl`.
It values every entry and exit slice independently at that slice's own typed market state, converts order side and
positive slice lots through the order intent's checked position operation, and folds settlement cashflows without a
scalar average-price shortcut or quantization. `RoundTripLeg` provides the closed entry/exit attribution used by
downstream pure consumers; focused `ScenarioValuationError` values retain the failing leg, slice, and core cause.
