## Why

Fee denomination and quantization are instrument-economic invariants, while percentage rules, minimums, tiers, maker/taker selection, account terms, and slice attribution are contextual policy. The current API combines both and lets policies return arbitrary slice indices and market states that valuation must distrust and reconcile afterward.

This change gives pure fee policy its own module and makes assessed attribution a validated result derived from the exact scenario, allowing fee-inclusive PnL to remain exact and compositional without coupling the instrument core to order policy.

## What Changes

- Add a pure `trading-fee-policy` artifact that depends on instrument economics, order model, and execution scenarios.
- Define an open, pure `FeePolicy` strategy for venue/account/tier rules; it is an ordinary scenario-to-validated-directives function, not a tagless-final effect algebra.
- Express nonnegative fee bases and minimums with existing refined typed quantities so negative inputs are rejected at refinement boundaries rather than rediscovered inside raw `Rational` helpers.
- Keep percentage rate sign convention, minimum-charge adjustment, tiers, maker/taker choice, schedule selection, and policy composition in the fee-policy artifact.
- Let policies emit fee directives containing a calculated core `Fee` and a requested matched-slice index; only the assessment boundary may resolve that index to the actual immutable slice and market state.
- Introduce scenario-owned fee assessments that contain their target scenario once and validated assessed fees; remove arbitrary caller-supplied source markets and later object-reference reconciliation.
- Compose zero or more same-instrument policies in stable order, accumulate independent policy/output failures, and retain the underlying empty/concatenation algebra without advertising a globally total monoid across instrument identities.
- Add pure round-trip scenario price normalization and fee-inclusive PnL orchestration that preserves typed quantities, converts each fee with its validated source slice, and delegates final exact composition to the core `Pnl` constructor.
- **BREAKING** Remove `FeeSchedule`, `FeeLine`, `Fees`, percentage/minimum methods on `FeeDenomination`, `instrument.fees`, and `instrument.valuation.pnl` without compatibility aliases.
- Exclude clocks, policy loading, account lookup, persistence, network access, streaming, transactions, tracing, and execution provenance.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `fee-inclusive-pnl`: Separate pure fee policy/directives from validated scenario attribution and define exact downstream orchestration into the core PnL value.
- `order-scenarios`: Add pure round-trip price normalization over complete matched slices so scenario economics can be consumed independently of fee policy.

## Impact

- Adds the `feePolicy` SBT project in `fee-policy/`, artifact `trading-fee-policy`, package root `trading.fee`.
- Moves policy types and helpers out of the transitional `economics` artifact; after this change only risk/sizing remains there.
- Changes fee policy extension points, attribution types, error ownership, PnL entry points, packages, and tests.
- Adds exact/refinement laws, policy-composition laws, attribution integrity checks, multi-error validation, and external-JAR module-boundary tests.
- Introduces no concrete effect dependency or external state changes.
