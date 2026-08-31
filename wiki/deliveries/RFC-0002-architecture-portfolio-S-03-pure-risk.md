---
type: delivery
updated: 2026-08-31
rfc: RFC-0002-architecture-portfolio
slice: S-03-pure-risk
change: introduce-pure-risk-module
status: archived
archived: 2026-08-31
evidence_manifest: sha256:12efcb23d09389513848f9ff1dda4f7e03a0e9eb1eba0b2648d70af0e4a5442f
source_digest: sha256:1a8cca449f8fdfc009f8ccbe87989a288ea92e7ff41ee58d38aa513a53ed4f2d
---

# RFC-0002-architecture-portfolio/S-03-pure-risk

## Outcome
Deliver five already-designed architecture changes as independent, traceable Corgi changes without weakening their
existing semantics. Each delivery receives one RFC Slice, GitHub Issue, isolated worktree, `corgi/<change>` branch,
exclusive Run Contract claim, and draft PR. Separate slices may proceed concurrently when their declared dependencies
are closed; Task Groups within one change remain sequential.

The preserved planning source is commit `e6c6f9c4eb4d75087b5a81e307e5ea6bf00a21b1`. Exact source-tree identities,
conversion disposition, and dependency edges are recorded in `tools/corgi/migration-manifest.json`. Agent-assisted
rehydration must retain the original change names and reconcile each proposal, design, delta specification, and task
list into the Corgi source/traceability contract before Apply.

## Boundary Delivered
The five Slices are planning-authority boundaries, not substitutes for their detailed capability specifications. Every
rehydrated change must be semantically compared with its source Git tree, pass strict planning readiness, and retain
the Foundation RFC obligations for ownership, dependency direction, typed validation, effects, and evidence.

`introduce-application-and-runtime-foundation` is independent of the externally implemented instrument-economics
change. `separate-order-and-execution-scenario-modules` and `introduce-pure-risk-module` are blocked by that external
delivery. `introduce-pure-fee-policy-module` and `introduce-versioned-boundary-codecs` are blocked by the order/scenario
delivery. These edges are projected as native GitHub `blockedBy` relationships after Propose; the claim adapter refuses
Apply until every native dependency is closed.

All five deliveries retain the pre-release breaking-change policy: superseded packages and APIs are removed rather
than kept as speculative aliases. Pure artifacts remain free of concrete effects and live state. External identities
and durable records cross checked reconstruction boundaries against explicit immutable state. New dependencies remain
in the narrowest owning module and configuration and preserve the JDK 17 minimum.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-009 | automated | file:target%2Fs03-tg10-canonical-verify.log#sha256:7b90343a6220180a57b572b1a2b3286dde13fcd4731c9f498de8c6137ac512d8; file:build.sbt#sha256:67d40120a6e0e240e578caf3b00ed1cc1da415e2db083ea417181174ef03b526; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FRisk.scala#sha256:1d9ad8eb16566d5027e3edeecfb7c1566ac8ddd546a7cb1cfd0a0410d65b0b54; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FError.scala#sha256:47aad4c338a15896400f8eee81195a1e340f7d0a5c210057f1147e14d07517b8; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskDownsideSuite.scala#sha256:a781f696da156118f3c3843c172a4234956cfc48c2e14f508af847aee3d190c4; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FRiskCompilerBoundarySuite.scala#sha256:60692d4095821c1ad7438e053f2051231a40cfe4dc34482ba5a196281868fb5d; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fnegative%2FRiskHasNoDownstream.scala#sha256:18eee8b646b35a0b195e8cf23a763648fa72a9322ab5b1bea3e8aee14ba1b9cc | PASS |
| AC-010 | both | file:target%2Fs03-tg10-canonical-verify.log#sha256:7b90343a6220180a57b572b1a2b3286dde13fcd4731c9f498de8c6137ac512d8; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FCurve.scala#sha256:e4730e8f143f5e5c71e45786aae2ef05ba42af8aea396aa3110514023f5de6a2; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FModel.scala#sha256:cdd87e416dbb6ed363d0d197a1719eb802508f2b645a23ae4951f0a6f4e27fd0; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurveSuite.scala#sha256:6b698e3497521af0eaf5998aac7ec22db822570d8658c84a065e243c8f52aa62; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurvePropertiesSuite.scala#sha256:6720707bd528182e6708d91cec760854f65562a985138b13fd8ac2a4d4b765fc; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskModelBoundarySuite.scala#sha256:396534ec4ee44d893a5d4a2715bc24a1202410a41760c8a3158aa1a6f3382229; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fnegative%2FInvalidRiskInputs.scala#sha256:73410eba6294d7c85687daeb9f5503ba8b2246baf1f1a4963f82270e91397bdf; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fnegative%2FPackageSpoofRiskConstruction.scala#sha256:69dda4d3aeb27fbab94ce6e6de78fd126db25b7e56b5191501bdb6d4fe034746; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-java%2Fnegative%2FRejectedRiskDecisionImplementations.java#sha256:33c2844757f67e79c7cdbbc8e5691a67d39760ef0ad79581c111235a633eead8; file:target%2Fs03-tg10-human-qa-carry-forward.log#sha256:2d28792a8f339abd8f58252fc138572bff3b2694de6bcae944466978cc89991d | PASS |
| AC-011 | automated | file:target%2Fs03-tg10-canonical-verify.log#sha256:7b90343a6220180a57b572b1a2b3286dde13fcd4731c9f498de8c6137ac512d8; file:target%2Fs03-tg10-jmh-verify.log#sha256:5153f29deff9bc972c1375b62bcdb713ac0ab3f9043d9b016f2fb37aef11e909; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FSizing.scala#sha256:44dec3986befda23a1d9af356371d96fa299f967a00f4213b095ce14ccad12ac; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FMaximumAffordableSuite.scala#sha256:e8cdeb2a45eabc38755e48c8b2b998d49a8b5d5a7724e773d8637294f39884d5; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FMaximumAffordablePropertiesSuite.scala#sha256:99ae4fdce8e0e3a7aade9d9fa32374d205064dc166d3016e586ec6491e9244f7; file:benchmarks%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fbenchmark%2FRiskSizingBenchmark.scala#sha256:43655cf7cdde1bb984682ec8a5ea08bf676325ceddf219b31b776bcc58947a45 | PASS |
| AC-012 | both | file:target%2Fs03-tg10-canonical-verify.log#sha256:7b90343a6220180a57b572b1a2b3286dde13fcd4731c9f498de8c6137ac512d8; file:target%2Fs03-tg10-jmh-verify.log#sha256:5153f29deff9bc972c1375b62bcdb713ac0ab3f9043d9b016f2fb37aef11e909; file:target%2Fs03-tg10-archive-recovery.log#sha256:2782b715678c205bbc4108995eb02e1ce4405b5318013b1efd9cdc4b4589ff07; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FExhaustiveSizing.scala#sha256:c150730232ad6b08cee443ec3374cc7a6126775c0e2a7e7569e3ce20d149be63; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FExhaustiveLotSizingSuite.scala#sha256:47e53b78d4628f9db0e24f8a19195752b0779eb5c01f1ac248532543b723e878; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurvePropertiesSuite.scala#sha256:6720707bd528182e6708d91cec760854f65562a985138b13fd8ac2a4d4b765fc; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2FFeePolicyIntegrationSuite.scala#sha256:044191057573c26d06ae13402c42b7bfa3447ccae91cbb5ecc846a7458f2606a; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FRiskCompilerBoundarySuite.scala#sha256:60692d4095821c1ad7438e053f2051231a40cfe4dc34482ba5a196281868fb5d; file:openspec%2Fchanges%2Fintroduce-pure-risk-module%2Fspecs%2Fposition-risk-sizing%2Fspec.md#sha256:26dfcd224ad4b79a35a90741329bbfe110acbab68909245a3af818f15bc4ab77; file:target%2Fs03-tg10-human-qa-carry-forward.log#sha256:2d28792a8f339abd8f58252fc138572bff3b2694de6bcae944466978cc89991d | PASS |

## Implementation
- Task Group 1: `a2ad478de1353312bfe56c91a3f16bae9a272ee0`
- Task Group 2: `d01129c30691a0d978beb36eefe71bd7b9fcb623`
- Task Group 3: `8bf1ad6da61fd3f91f81aae85ecc543a8603e4a1`
- Task Group 4: `755eb071c94ea0465427d158a104c6be8a6e6446`
- Task Group 5: `253efcb01197f17946478a5c59571efcd2c177b6`
- Task Group 6: `65915625e0783aa51e599dcbd8d11b4339e6c7e6`
- Task Group 7: `176654ae34e44b9414aec61824345f5596417587`
- Task Group 8: `c01b94c496f51dd3ce15b4d389fc6e01486d99b2`
- Task Group 9: `d7fe800147f0cba6b8dca5ef523015ec9424f1b9`
- Task Group 10: `02d15a1cf37f236a15f40f7a88179387caaa3e81`
- Final HEAD: `02d15a1cf37f236a15f40f7a88179387caaa3e81`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: pass by m2048ws — Human exploratory library QA confirms the checked monotone-model and explicit exhaustive-fallback paths. Earlier human QA passed at implementation SHA d7fe800147f0cba6b8dca5ef523015ec9424f1b9; the exact final revision changes only planning and the session checkpoint, while fresh canonical Verify passed 869 tests, strict planning, Archive rehearsal, and focused JMH evidence.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0002-architecture-portfolio`
- `openspec/changes/archive/2026-08-31-introduce-pure-risk-module`
- `openspec/changes/archive/2026-08-31-introduce-pure-risk-module/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/8
