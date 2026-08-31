# trading-fee-policy

Pure downstream fee-policy and scenario composition over completed quantities, instrument-economics, order-model, and
execution-scenario artifacts. The SBT project is `feePolicy`, the published artifact is `trading-fee-policy`, and its
public package root is `trading.fee`.

The public root now owns exact `FeeRate`/`FeeCalculation` mathematics, non-empty typed `PolicyErrors`, refined
`SliceIndex`, existential `FeeDirective`, and the open pure `FeePolicy` strategy with checked same-instrument
composition. The `trading.fee.policy.FeeOrchestration` subpackage is a provisional integration bridge for assessment,
settlement conversion, and fee-inclusive PnL paths that later S-04 Task Groups replace. The artifact contains no risk
sizing, live policy acquisition, concrete effects, runtime state, I/O, persistence, telemetry, or codecs.

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
