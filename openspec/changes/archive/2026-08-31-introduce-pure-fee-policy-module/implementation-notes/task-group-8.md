# Task Group 8 — Downstream migration and provisional-surface removal

## Canonical package and call paths

The `feePolicy` artifact now exposes only its canonical `trading.fee` production package. Unit, integration, and
completed-JAR clients construct denominations through `FeeDenomination`, calculate exact components through
`FeeCalculation` and `Fee.create`, assess through `FeeAssessment`, and compose scenario PnL through explicit
`RoundTripFeePolicies` plus `FeeInclusivePnl`.

The provisional `trading.fee.policy.FeeOrchestration` facade and its universal orchestration error hierarchy are
deleted. No forwarding alias or secondary compatibility package remains. `openspec/config.yaml`, module guidance, and
the architecture audit now identify `trading.fee` as the sole public root.

## Downstream and risk migration

Fee-policy integration tests and external positive clients now consume `FeeInclusivePnl` directly. Risk receives only
the successful core `Pnl` projection at the caller-owned boundary; its production artifact and API remain unchanged
and retain no order, scenario, or fee-policy dependency. Arbitrary scenario evaluation continues to use the explicit
exhaustive fallback, while fixed successful fee-inclusive observations continue to earn a checked monotone table.

Canonical accumulation replaces the bridge's first-error behavior: independently eligible entry and exit conversion
failures are retained in stable leg order. Completed lower-layer fixtures now reject imports from the real
`trading.fee` package root rather than a retired subpackage.

## Removed and deferred capabilities

Completed-JAR inspection rejects the entire `trading/fee/policy/` path and root aliases for `FeeSchedule`, `FeeLine`,
`FeePolicyError`, and `FeeOrchestration`. Negative external compilation names those retired surfaces so their absence
cannot regress silently.

Live policy acquisition, clock and account access, venue/tier/version selection, audit envelopes, and execution
reports remain absent. Applications own policy selection and acquisition, runtime interpreters own clocks and
external state, and the future boundary-codec Slice owns durable versions and records.

## Verification

| Check | Result |
| --- | --- |
| `sbt scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt feePolicy/test` | 38 passed, including 11 migrated fee/scenario/risk integration tests |
| `sbt risk/test` | 40 passed; production risk API and dependency boundary unchanged |
| `sbt adversarialBoundary/test` | 158 passed, including 45 economics/scenario/fee completed-JAR tests |
| Source and package search | no production, positive-client, or integration reference to the provisional facade/package |
| Completed-JAR inspection | no provisional package, retired alias, policy acquisition, account/tier/version, audit, or execution-report surface |

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The review loop covered Task Group 8 scope, AC-013/AC-016 alignment, canonical-call migration, accumulated error
semantics, unchanged pure-risk ownership and search behavior, provisional package and retired capability removal,
deferred application/runtime/codec responsibilities, package and completed-JAR boundaries, code quality, security
applicability, and linear conversion behavior. It returned no findings, changed no file during the review, required no
human triage, and is neither canonical Verify nor Human Review.
