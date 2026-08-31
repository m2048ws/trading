# trading-fee-policy

Pure downstream fee-policy and scenario composition over completed instrument-economics, order-model, and
execution-scenario artifacts.

The production artifact owns `trading.fee.policy`: exact percentage/minimum logic, schedule composition, fee
attribution, settlement conversion, and fee-inclusive PnL orchestration. It preserves the behavior moved from the
retired transitional aggregate and contains no risk sizing, live policy acquisition, concrete effects, runtime state,
I/O, persistence, telemetry, or codecs.

Risk is a test-only integration dependency. Downstream tests demonstrate three explicit composition routes without a
production dependency from risk to fee policy:

- fixed successful PnL observations may earn a reusable monotone model through complete-table validation;
- genuinely arbitrary or non-monotone evaluation deliberately uses `ExhaustiveLotSizing`; and
- fee/scenario failures remain typed at the downstream boundary.

The later `introduce-pure-fee-policy-module` Slice owns any semantic redesign of this API. This move establishes the
physical owner needed to retire `trading-economics`; it does not claim delivery of that later Slice.
