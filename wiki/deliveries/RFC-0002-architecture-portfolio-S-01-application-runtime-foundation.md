---
type: delivery
updated: 2026-08-30
rfc: RFC-0002-architecture-portfolio
slice: S-01-application-runtime-foundation
change: introduce-application-and-runtime-foundation
status: archived
archived: 2026-08-30
evidence_manifest: sha256:4a4ee2d82d93b117f9e43689b2304cfa84221b2c8a3d23c66650e2ef6f0e4795
source_digest: sha256:b6456620959f0fc121106393b6a78980fe81c550cbfa0d157f1dcec29f172d87
---

# RFC-0002-architecture-portfolio/S-01-application-runtime-foundation

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
| AC-001 | automated | file:application%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fapplication%2FLiveCatalog.scala#sha256:b584ba2a0cdc46c1bb088ade2f1ea2daab33cd5e9f8f270f522caa50282d1aa3; file:runtime%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalog.scala#sha256:47a5465a4d3af8ac7286e52b2af04b0e8802ebd473d3ab6e2beb7ce895766eb3; file:build.sbt#sha256:59d08475dcd49da4af7e5b280d785e7289a7bfa510c38f32d963265c21780ac5; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FApplicationRuntimeBoundarySuite.scala#sha256:16deebec9a010b22ff2108ea07b7ee78ca5299dcae92dda32eceac51c5bd4b30; file:docs%2Farchitecture-charter-audit.md#sha256:a336a9b5f03253dbd21db3fdeb3e8e3ede9f3aad0dafe38161bc41eaa182da9d | PASS |
| AC-002 | both | file:runtime%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalog.scala#sha256:47a5465a4d3af8ac7286e52b2af04b0e8802ebd473d3ab6e2beb7ce895766eb3; file:runtime%2Fsrc%2Fmain%2Fjava%2Ftrading%2Fruntime%2FLiveCatalogBridge.java#sha256:e86ccffa4292dacbd80f3b219f738549bccbd660bd0a6cdd7b5f83cbdf42f68c; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogConstructionSuite.scala#sha256:e5f85399b20636daaa9db6c934899ce9d092f6308c686b0c4ced200cfcfa0987; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogAtomicSuite.scala#sha256:36bf701fef43ea4962a0d150de5136f61e41634850e3025b556916a357525e41; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogConcurrencySuite.scala#sha256:1583e04a7a73cc0f1f59f0583ca82c0c6c45ec9cf30045038795b94ce74ebf99; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogContractSuite.scala#sha256:80178bda163b07a7f76d81d857ce81be67bfe2a76feaa7f89eaeef3430397392; file:runtime%2FREADME.md#sha256:6fc66b607d8194b756a680da3200eb681e1cdeb428c854b11d0f4ec486cc8bea | PASS |
| AC-003 | human | file:application%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fapplication%2FLiveCatalog.scala#sha256:b584ba2a0cdc46c1bb088ade2f1ea2daab33cd5e9f8f270f522caa50282d1aa3; file:application%2FREADME.md#sha256:6c2c1be703c09a80f376965fff1467971631393679c44de3e38cbbed99dec094; file:docs%2Farchitecture-charter-audit.md#sha256:a336a9b5f03253dbd21db3fdeb3e8e3ede9f3aad0dafe38161bc41eaa182da9d; file:openspec%2Fchanges%2Fintroduce-application-and-runtime-foundation%2Fdesign.md#sha256:da3ce25343673c5cd1d1b7449efae5f596a59841e29c3184b8e15aa3954a5c09 | PASS |
| AC-004 | automated | file:application%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fapplication%2FLiveCatalogContract.scala#sha256:19d70e09a0452b2e14bf57db69a6318b40656cefb843afd48f051797436fffd0; file:application%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fapplication%2FLiveCatalogContractSuite.scala#sha256:4185f37756d0301c8e40f9e43ab3962a484e4438faa847643af11174e762e1df; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogContractSuite.scala#sha256:80178bda163b07a7f76d81d857ce81be67bfe2a76feaa7f89eaeef3430397392; file:runtime%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fruntime%2FInMemoryLiveCatalogConcurrencySuite.scala#sha256:1583e04a7a73cc0f1f59f0583ca82c0c6c45ec9cf30045038795b94ce74ebf99; file:benchmarks%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fbenchmark%2FLiveCatalogRuntimeBenchmark.scala#sha256:47f88c9aa2fa53339f71067750a21c631fa84b324ee7ff9b228170aded125056; file:docs%2Fcatalog-benchmark.md#sha256:b1d095ee685033695be5276ae26da97182699b9654071929e1627f476730ac31 | PASS |

## Implementation
- Task Group 1: `a7b29bab55b368bb50cf3cadb8412d472d2087d0`
- Task Group 2: `c408671fab69cd22cd3e3c5aed38b7129f49a004`
- Task Group 3: `6a077e9eca36516da689afa31c7b3e84ae6a1b12`
- Task Group 4: `c54f944a52e6d15ca45ff5fdf3d5a006da859219`
- Task Group 5: `14854ce174059114e01ee7e9ef29578c8268cfb0`
- Task Group 6: `82cb7509bc062cacc045c4508d16afc14515d0db`
- Task Group 7: `391c5e6641d2b9333bc931926bd84f3fa0f93ef9`
- Task Group 8: `c0e80a3ecb4f2f45c67899685c1b092e8d523527`
- Task Group 9: `6c0d35bb652d75668ba6550dd8155f33dce0f58b`
- Final HEAD: `6c0d35bb652d75668ba6550dd8155f33dce0f58b`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: pass by m2048ws — Human real-user-path QA confirmed AC-002 and AC-003 pass; no defects found.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0002-architecture-portfolio`
- `openspec/changes/archive/2026-08-30-introduce-application-and-runtime-foundation`
- `openspec/changes/archive/2026-08-30-introduce-application-and-runtime-foundation/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/6
