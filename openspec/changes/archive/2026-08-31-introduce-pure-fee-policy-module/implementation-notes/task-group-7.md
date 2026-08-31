# Task Group 7 — Fee-inclusive round-trip evaluation

## Explicit leg-policy product

`RoundTripFeePolicies[E, PosD, B, Q, S]` is the canonical immutable input product. Its entry and exit fields may hold
different already-selected policies, including policies whose distinct failure types have been widened into one honest
sum. `RoundTripFeePolicies.same(policy)` preserves that product while placing the same policy in both fields. The value
contains no clock, account lookup, catalog access, effect, or acquisition behavior.

## Staged dependency graph

`FeeInclusivePnl.evaluate(instrument)(roundTrip, policies)` first accumulates round-trip, entry-policy, and exit-policy
identity mismatches and suppresses every dependent branch if that initial gate fails. Eligible work then evaluates
scenario price normalization, entry assessment, and exit assessment independently.

Each successful assessment immediately converts all of its assessed fees through each fee's centrally selected
`sourceSlice.market`; one leg's policy failure does not suppress conversion on the independently successful leg.
Conversion traverses every directive and accumulates failures in stable entry/exit and directive order. Core
`Pnl.create` runs only when price normalization and both converted assessment branches succeed.

## Typed failures and successful provenance

`FeeInclusivePnlErrors[E]` is a domain-owned non-empty head/tail collection over closed
`FeeInclusivePnlViolation[E]` alternatives. Identity, scenario-price, located assessment, located conversion, and core
failures remain separate. Assessment causes retain the original generic policy error without exception or string
erasure. `evaluateFirst` is exactly the accumulated result's head projection.

Successful `FeeInclusivePnl` and `AttributedFeeContribution` representations have JVM-private constructors. The result
retains its round trip, entry and exit `ScenarioFees`, and ordered attributed contributions. Each attribution projects
its assessed fee's exact `RoundTripLeg`, directive ordinal, refined `SliceIndex`, immutable source slice, original fee,
and core settled contribution. The sole `Pnl` member owns price PnL, settled contribution order, fee PnL, and net PnL;
the scenario wrapper exposes projections without duplicating calculated totals.

## Verification

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch feePolicy/test` | 38 passed, including 7 focused fee-inclusive PnL tests |
| `sbt -batch adversarialBoundary/test` | 158 passed, including 45 economics/scenario/fee completed-JAR tests |
| Public JVM API inspection | success and attributed-contribution constructors private; staged helpers private |
| Source and packaged-bytecode audit | no live catalog, clock, effect, runtime, stream, raw scalar kernel, or catch-all exception concern |

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The review loop covered Task Group 7 scope, AC-016 alignment, the explicit leg-policy product, initial identity
suppression, independent branch eligibility, policy/directive/conversion accumulation, generic cause provenance,
selected-slice conversion, core-total ownership, scenario attribution, successful replay equality, JVM construction
authority, public packaged dependencies, linear complexity, security applicability, and checkpoint integrity. Its
first pass found only that this implementation note still contained its pre-gate evidence placeholder; the observed
check matrix and review scope are now recorded above. The final pass found no findings. It changed no file during that
pass, required no human triage, and is neither canonical Verify nor Human Review.
