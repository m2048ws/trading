# Trading Execution Scenario

Pure hypothetical execution outcomes owned by `trading.scenario`.

The artifact depends only on instrument economics and the order model. It interprets immutable orders from explicit
evidence without owning or mutating instrument or order definitions, and contains no fee or risk policy, application
services, runtime effects, or live execution lifecycle.

`ScenarioValuation.pricePnl(instrument)(roundTrip)` normalizes a complete checked round trip into exact core `PricePnl`.
It values every entry and exit slice independently at that slice's own typed market state, converts order side and
positive slice lots through the order intent's checked position operation, and folds settlement cashflows without a
scalar average-price shortcut or quantization. `RoundTripLeg` provides the closed entry/exit attribution used by
downstream pure consumers; focused `ScenarioValuationError` values retain the failing leg, slice, and core cause.
