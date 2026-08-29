# trading-instrument-economics

This artifact is the pure instrument-economic dependency root. It depends only on `trading-quantities` and
`trading-reference-data` and packages `trading.economics.instrument`.

It owns snapshot-based instrument assembly, static `Instrument`, instrument-bound lots and signed positions, positive
grid prices, immutable settlement conversions and market states, exact fee values, `PricePnl`, settled fee
contributions, pure PnL composition, and their focused errors.

All constructors and calculations are deterministic functions of immutable inputs. The artifact contains no order,
scenario, fee-policy, risk, application, codec, runtime, effect-system, synchronization, or I/O package. Completed-JAR
compiler tests enforce that boundary.
