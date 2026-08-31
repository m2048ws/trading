# trading-fee-policy

Pure downstream fee-policy and scenario composition over completed quantities, instrument-economics, order-model, and
execution-scenario artifacts. The SBT project is `feePolicy`, the published artifact is `trading-fee-policy`, and its
public package root is `trading.fee`.

The public root now owns exact `FeeRate`/`FeeCalculation` mathematics, non-empty typed `PolicyErrors`, refined
`SliceIndex`, existential `FeeDirective`, and the open pure `FeePolicy` strategy with checked same-instrument
composition. Canonical `FeeAssessment` now resolves directives against one exact scenario and returns JVM-sealed
`ScenarioFees`/`AssessedFee` attribution with generic ordered violations.

`RoundTripFeePolicies` records explicit immutable entry and exit selections, while `FeeInclusivePnl.evaluate` stages
identity, exact scenario price normalization, per-leg assessment, selected-slice conversion, and core PnL composition.
Its non-empty generic violations retain every policy cause, leg, directive, slice, and core cause. Successful results
retain the round trip, both assessments, attributed settled contributions, and one core `Pnl`; price, fee, and net
totals are projections of that core value rather than recalculated fields. There is no compatibility facade or
secondary package: callers use the canonical `trading.fee` API directly. The artifact contains no risk sizing, live
policy acquisition, clock or account access, venue/tier/version selection, audit envelopes, execution reports,
concrete effects, runtime state, I/O, persistence, telemetry, or codecs. Applications select and acquire policies;
runtime interpreters own clocks and external state; future boundary codecs own durable records and versions.

Risk is a test-only integration dependency. Downstream tests demonstrate three explicit composition routes without a
production dependency from risk to fee policy:

- fixed successful PnL observations may earn a reusable monotone model through complete-table validation;
- genuinely arbitrary or non-monotone evaluation deliberately uses `ExhaustiveLotSizing`; and
- fee/scenario failures remain typed at the downstream boundary.

Completed-JAR compiler fixtures use a fee-policy-only production classpath. They prove that the published artifact can
consume quantities, instrument economics, orders, and execution scenarios while risk, application, runtime, effect,
stream, codec, persistence, telemetry, and benchmark concerns remain unavailable to production fee policy. Lower-layer
fixtures independently prove that quantities, reference data, instrument economics, order model, and execution scenario
cannot import fee policy.
