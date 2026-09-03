# Task Group 4 — Fee policy and attribution static construction

## Implementation

Fee assessment and fee-inclusive PnL no longer import or invoke method handles.
`AssessedFeeValue`, `ScenarioFees`, `AttributedFeeContributionValue`, and
`FeeInclusivePnl` use direct construction from the package-local operations that
already establish their semantics. Their constructors use `private[fee]`, while
the concrete existential implementations remain package-private where
appropriate; no public unchecked reconstruction helper was added.

The rewrite removes four reflective constructors, their descriptors, four
invocation-result casts, and the associated `private[this]`/warning ceremony.
It leaves policy selection and directive validation independent from the core
economic values. The existing boundaries still:

- reject scenario/policy, fee, and denomination identity mismatches in stable
  deterministic order;
- validate each source-slice index before binding an assessed fee to the exact
  immutable slice and market state;
- retain the source leg, directive ordinal, source index, original fee
  dimension, exact settled contribution, and ordered attribution;
- accumulate entry/exit price, assessment, and conversion failures before
  composing the exact core `Pnl`; and
- return the existing closed typed errors and reject Java serialization.

The constructor-forgery compiler fixture and bytecode-private-constructor test
were removed. Fee unit/property/integration tests and the completed-artifact
positive client continue to cover charges, rebates, multi-asset conversion,
entry/exit attribution, invalid directives, identity mismatch order, and exact
PnL equivalence.

Both fee migration allowances were removed; the repository guard now reports
547 reviewed tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| `sbt -batch feePolicy/scalafmtAll feePolicy/test` | Pass: 38 fee assessment, policy, attribution, PnL, integration, and property tests. |
| `sbt -batch adversarialBoundary/Test/scalafmtAll 'adversarialBoundary/testOnly external.EconomicsCompilerBoundarySuite'` | Pass: 39 completed-artifact/compiler checks. |
| `tools/check-in-process-reflection.sh` | Pass: fee source is clean; 547 reviewed migration tokens remain elsewhere. |
| Fee source scan for method handles and `private[this]` | Pass: none remain. |
| `git diff --check` | Pass. |

The structured review covered ownership/visibility, existential dimensions,
identity and attribution retention, validation order, exact settlement
composition, policy separation, fixture replacement, serialization, and
performance. Direct construction removes dynamic overhead and changes no
algorithm. One compile finding—an obsolete `Modifier` import after removing the
bytecode test—was corrected before the final check. No critical or important
finding remains. The review changed no file itself, human-triaged no finding,
and is not canonical whole-change Verify or Human Review.
