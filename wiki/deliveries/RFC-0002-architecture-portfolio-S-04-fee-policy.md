---
type: delivery
updated: 2026-08-31
rfc: RFC-0002-architecture-portfolio
slice: S-04-fee-policy
change: introduce-pure-fee-policy-module
status: archived
archived: 2026-08-31
evidence_manifest: sha256:4556f072667579a209e7a80bfe55a0a29d43cdebc873a3682893dc9479016eb7
source_digest: sha256:d7face4865eb2e8e69abb4f305d67720842e19f9d1d4fa3b760b744fb8f06443
---

# RFC-0002-architecture-portfolio/S-04-fee-policy

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
| AC-013 | automated | file:target%2Fs04-tg10-canonical-verify.log#sha256:b2af2022fc5349b21e1fbaf172794f60a67842a0f3381c05065af6d2ec75fb57; file:build.sbt#sha256:864bcc328b1c9517e1ab7f4c1412dfc1bd3c83d5b5039ece773f3bb577fa9f04; file:fee-policy%2FREADME.md#sha256:08adca205113ae9b4f2d45751e48cdd32f6ee86237b699acdc5725a3f660eb07; file:fee-policy%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2FPolicy.scala#sha256:392d909334f5f67ca27a183b6436aa43bb5895b27f345769412ad7bce3d85c92; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Ffee-policy-compiler%2Fnegative%2FFeePolicyHasNoDownstream.scala#sha256:5470190d5ff6b7a12923afce59aa1b2a1568c183ff5165c4b9116bbf8121b9cc | PASS |
| AC-014 | both | file:target%2Fs04-tg10-canonical-verify.log#sha256:b2af2022fc5349b21e1fbaf172794f60a67842a0f3381c05065af6d2ec75fb57; file:fee-policy%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2FFeeCalculation.scala#sha256:581074f6fb9b65c96f4c87f33901fdf9f427fb0f7f0c8c4d08bcddd8e407c2a4; file:fee-policy%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2FPolicy.scala#sha256:392d909334f5f67ca27a183b6436aa43bb5895b27f345769412ad7bce3d85c92; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2Ffee%2FFeeCalculationSuite.scala#sha256:1f0a406ff667f5b927d38a7043c4524239d26c3f60416db24889924fd173dcfe; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2Ffee%2FFeePolicyStrategySuite.scala#sha256:7e8f82bb3a7d29682f73f9aaaedcd08d430c67c073f1979f12a3453938ceccd7 | PASS |
| AC-015 | automated | file:target%2Fs04-tg10-canonical-verify.log#sha256:b2af2022fc5349b21e1fbaf172794f60a67842a0f3381c05065af6d2ec75fb57; file:fee-policy%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2FAssessment.scala#sha256:adf536e821cb043d367f9c497c2f7867045d1776c392811bc87851f6488bd892; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2Ffee%2FFeeAssessmentSuite.scala#sha256:dc37e2391cf1a8a9ab979321cee9c3724a0dc1fd001253df1d0f5ec83d27e81b; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Ffee-policy-compiler%2Fnegative%2FForgedAssessment.scala#sha256:521e5b94a6adc49db5916c057a10bac2fc5ebaec49efec1f5fbc58da52379879 | PASS |
| AC-016 | both | file:target%2Fs04-tg10-canonical-verify.log#sha256:b2af2022fc5349b21e1fbaf172794f60a67842a0f3381c05065af6d2ec75fb57; file:target%2Fs04-tg10-archive-rehearsal.log#sha256:a00558f9cd23b820cb9b77860cc65c3fab509be7c3745d9e8b16cc11fbc9b07f; file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FScenarioValuation.scala#sha256:15e788e3d2a0ec912ae084fd64efa235d12ee26dd085fe30586f266aba109b0b; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FScenarioValuationSuite.scala#sha256:1c9175fc44a0ce0900a58a5035769d3ae2d5ef21e3cfae2b7c1fecb8447f9574; file:fee-policy%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2FFeeInclusivePnl.scala#sha256:e8126e83a60dcdfcaedefd9eda661a3d9a00f3b939733d07e77b1cc1183bef5c; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2Ffee%2FFeeInclusivePnlSuite.scala#sha256:f51e2771656830b59c62bba60388ae0e44d1110448ae96e6372017c7efc4e36b; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2FFeePolicyIntegrationSuite.scala#sha256:6ccbcd9d6a0a9f6cd4f253a0592489b5f95897fd36c3aa8ad7028c598f26bef1 | PASS |

## Implementation
- Task Group 1: `e085160202b88a9e2fa4f1589b29eb4260beae93`
- Task Group 2: `d59b081b4c2fed6e2ee7d6e25fc7707803fa1773`
- Task Group 3: `958899c0b40eb3bac8b0d9e96b5d73e976e1f892`
- Task Group 4: `031c233c756d77483286229ad9e7a7f6d9d58d57`
- Task Group 5: `a02ef16497214ebb0dc4618be5e03c4078d5ca3d`
- Task Group 6: `0060e65f5bf4174fab0542a919007a825271d57f`
- Task Group 7: `78146fd80b622f332af825619e5b7228efeb1434`
- Task Group 8: `3a8fd512d036462e50c27cd3aad3d10f0e77cd73`
- Task Group 9: `a293b9980b2220ca816b3911a655cdd4722b1d32`
- Task Group 10: `0eceeabf1d2eb1f8ad2b534f65f00552dc6f53dc`
- Final HEAD: `0eceeabf1d2eb1f8ad2b534f65f00552dc6f53dc`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human-confirmed no runtime impact: the recovery changes only planning and session documentation; production, runtime, and build code are unchanged.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0002-architecture-portfolio`
- `openspec/changes/archive/2026-08-31-introduce-pure-fee-policy-module`
- `openspec/changes/archive/2026-08-31-introduce-pure-fee-policy-module/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/9
