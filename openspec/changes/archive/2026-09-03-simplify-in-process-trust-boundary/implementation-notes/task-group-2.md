# Task Group 2 — Order model static construction and semantic evidence

## Implementation

`Order.scala` no longer imports or invokes `MethodHandles`, `MethodType`, or
`privateLookupIn`. The checked activation, fixed/trailing trigger evidence, peg
resolution, order intent, and constructed aggregate now use direct construction
from their owning companions. Top-level representation constructors remain
companion-private; the aggregate implementation class is itself private to
`Order`, so its constructor needs no separate reflective access ceremony.

The rewrite removes six `private[this]` construction sites and every invocation
recovery cast. The three remaining `asInstanceOf[AnyRef]` uses compare the
singleton immediate-evidence/direct-resolution values by reference after the
closed associated type selects those singleton alternatives; they do not recover
an erased constructed value or manufacture authority.

Exact-runtime-class gates were removed from `OrderActivation`, `OrderPricing`,
`PricedVisibility`, `OrderExecution`, and `Order`. The alternatives remain
sealed/final, and supported callers continue to use exhaustive matching and the
same domain-named factories. The semantic checks remain at their established
owners:

- fixed/trailing evidence factories still prove instruction agreement and
  trigger satisfaction, including positive trailing thresholds;
- peg resolution still proves the exact supplied offset and rechecks instruction
  agreement when consumed;
- order intent still checks instrument identity and derives exact signed
  position change from side and lots;
- aggregate creation still accumulates instrument, activation, pricing,
  visibility, duration, liquidity, and reduce-only violations deterministically.

Hostile same-package Scala/Java constructor fixtures, foreign Java subclass
implementations, and the constructed-order bytecode privacy assertion were
removed. Retained coverage includes exhaustive valid alternatives, impossible
associated shapes, same-shape evidence mismatch, typed erased validation,
identity mismatch ordering, exact position derivation, and serialization policy.
An ordinary Java client now exercises checked activation and duration factories
plus market-price resolution without reflection.

The order-model migration allowance was removed from
`tools/in-process-reflection-baseline.tsv`; the repository guard now reports 595
reviewed tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| `sbt -batch orderModel/scalafmtAll adversarialBoundary/Test/scalafmtAll orderModel/test 'adversarialBoundary/testOnly external.EconomicsCompilerBoundarySuite'` | Pass: 7 order tests and 44 completed-artifact/compiler checks. |
| `sbt -batch executionScenario/test feePolicy/test risk/test boundaryCodecs/test executionLifecycle/test` | Pass: 255 downstream semantic tests (16 scenario, 38 fee, 40 risk, 95 codec, 66 lifecycle). |
| `tools/check-in-process-reflection.sh` | Pass: order source is clean; 595 reviewed migration tokens remain elsewhere. |
| Order source scan for method handles, `private[this]`, runtime class guards, and invocation casts | Pass: none remain. |
| `git diff --check` | Pass. |

The structured review covered requirement preservation, visibility, type/evidence
ownership, deterministic validation, client/API behavior, removed-test
replacement, downstream compatibility, and performance. Direct calls remove
lookup/invocation overhead and add no hot-path work. No critical or important
finding remains. The review changed no file itself, human-triaged no finding,
and is not canonical whole-change Verify or Human Review.
