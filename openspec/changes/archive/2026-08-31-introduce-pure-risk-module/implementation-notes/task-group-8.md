# Task Group 8 — Downstream integration and migration

## Intentional production owners

The broad `trading-economics` aggregate and `economics/` project are removed. Its obsolete
`trading.risk.TransitionalRisk` wrapper and universal `RiskError` family are deleted without aliases. The existing
fee-policy implementation moves unchanged into its physical owner:

```text
trading-instrument-economics ──┐
trading-order-model ───────────┼──> trading-fee-policy
trading-execution-scenario ────┘

trading-quantities ────────────────┐
                                    ├──> trading-risk
trading-instrument-economics ──────┘
```

`trading-fee-policy` has `trading-risk` only on its test classpath for downstream composition coverage. Resolved SBT
classpath inspection and completed-JAR byte inspection confirm neither production artifact depends on or packages the
other; risk also contains no order or scenario classes. This ownership-only move does not claim the later S-04
fee-policy redesign.

Root aggregation, sequential tests, completed-product classpaths, artifact assertions, repository/module guides, and
OpenSpec repository context now name `feePolicy` and `risk`; no `economics` SBT project, artifact, or directory remains.
The removed tracked sources are recoverable from Git, while deleted `economics/target` contents were generated outputs.

## Explicit integration routes

The old policy-owned `maxLots` accepted a scenario callback, compared raw coefficients, and returned `Option[Lots]`.
All such calls are removed.

The migrated downstream integration suite now demonstrates the three honest routes from the accepted design:

1. Fixed entry, adverse exit, instrument, cap, and no-fee PnL observations form a complete checked table before
   `MaxAffordableLots.select` returns the exact three-lot boundary.
2. A fixed two-leg flat-fee schedule is evaluated outside risk, then its complete PnL table earns a monotone model
   before primary sizing returns one lot.
3. Arbitrary and deliberately non-monotone per-lot scenario evaluation explicitly calls `ExhaustiveLotSizing`; it
   visits all four coordinates, finds a later affordable value after a decrease, and retains a caller-owned failure at
   exact coordinate two with no partial decision.

Risk's own fixtures remain based only on instrument economics and exact typed quantities. No scenario builder,
fee-policy evaluator, or generic callback is admitted to the monotone model.

## Capability boundary

The implemented capability remains standalone proposed sizing from flat exposure. Current-position adjustment,
account/portfolio risk, cross-margin offsets, collateral, liquidation, funding, market-data or policy acquisition,
fill probability, caching, concurrency, audit, persistence, tracing, and telemetry are neither inputs nor implied
claims. These require later named application or portfolio capabilities.

## Focused evidence

- 40 risk tests pass.
- 11 migrated fee-policy and risk-integration tests pass.
- 106 focused static-dimension, quantity/reference, instrument/order/scenario/fee/risk completed-JAR tests pass.
- The full repository aggregate passes 869 tests, including all 147 adversarial boundary tests.
- The non-published JMH project compiles against the post-aggregate graph.
- SBT production classpaths confirm risk has only quantities, reference data, and instrument economics; fee policy has
  instrument economics, quantities/reference data, order model, and execution scenario, with risk appearing only in
  fee-policy tests.

Canonical Verify and Human Review remain separate gates.

Automated Task Group review identified aggregate-era naming on the cross-module external client and stale/split
dependency descriptions after the artifact was removed. The fixture is now `CompleteCompositionClient`, making its
test-only integration role explicit, and the build/reference guides state the resolved graph cleanly. The final review
pass has no findings.
