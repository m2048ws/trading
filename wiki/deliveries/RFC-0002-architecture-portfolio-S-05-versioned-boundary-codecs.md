---
type: delivery
updated: 2026-09-02
rfc: RFC-0002-architecture-portfolio
slice: S-05-versioned-boundary-codecs
change: introduce-versioned-boundary-codecs
status: archived
archived: 2026-09-02
evidence_manifest: sha256:21835d70b9da0a74b9b03f9a22ca11833eec287671357f63a7281dd3de69c897
source_digest: sha256:625809aa552c0c3f4efd1a8e2d9982c76414f82ce405674ce6f91b475807cceb
---

# RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs

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
| AC-017 | automated | file:target%2Fcorgi-verify%2Fs05-canonical-verify.log#sha256:127bc0e9ca767ee9db109e5c3cd24663d46a48547b46a0d9836564674f1f4750; file:build.sbt#sha256:8ca7eb6d07cfef2edb6d38ec8602d7addee90e8b51c733b3b60702158b60ed0f; file:boundary-codecs%2FREADME.md#sha256:c34af73a955d79a6c549ad64c020839b865445b1a5474328e0b8add9550d0afd; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FBoundaryDependencyScopeSuite.scala#sha256:c9855e4eebb57cbfa01c86da6f95fdd3a8cdd9b248e4c3f95de3f8717dedc46b; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FStrictJsonSuite.scala#sha256:cd2f7ea1532765ce8b2b710836e7e610be96ea1a33dd9bf214240158ebc6fa62; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FExactPrimitivesSuite.scala#sha256:692617b1efdcf3df2a9f787803ef8d6c1b045b7e0d0223e41619436d08c12353; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FRecordEnvelopeSuite.scala#sha256:b903a272ee58fcd88159d19a6f51d72c4446f00083343a2a54fad390d11453fa; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:235c62f7161a92051180255e293dc6cfbb9992d509f1eaeb2abd2e788689acce | PASS |
| AC-018 | both | file:target%2Fcorgi-verify%2Fs05-canonical-verify.log#sha256:127bc0e9ca767ee9db109e5c3cd24663d46a48547b46a0d9836564674f1f4750; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FGridCoordinateRecordSuite.scala#sha256:c812421f852b98889af34780029345801e8e94edb8e9f9646cc2f7c16dd6dc9e; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FGridCoordinateRecordPropertiesSuite.scala#sha256:d544fe70a8387577ccb0a67dfaa463c646e26ce975264ee57dd2c46c1148f34e; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FInstrumentDefinitionRecordSuite.scala#sha256:37efe2d8ffe8d6696e32518c9abf8c4ad2b9c093052afda21a40e0ea94056c86; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FInstrumentDefinitionRecordPropertiesSuite.scala#sha256:69cb436ebb05a2d72c797cb549b6b9e5b36ab556122671b6d094aa093b6d8d2b; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FOrderRecordSuite.scala#sha256:cad84d3e16ffc6701e325e5fdf9bf487a9053cc3e4362dd071357599d48230f0; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FOrderRecordPropertiesSuite.scala#sha256:1195351d35c79dc0428aacfea3c9feb47458be7fc8ffa9706e7ab12509b175a7; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordSuite.scala#sha256:50efb21dc9b03963a44df0c4b74204770f98c70af3a1f7c8c6b6d8a5ee3cf5cc; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordPropertiesSuite.scala#sha256:a38a72326eda43af0f909be730637eab29b262240b789f23da6148de2c6f2a29 | PASS |
| AC-019 | automated | file:target%2Fcorgi-verify%2Fs05-canonical-verify.log#sha256:127bc0e9ca767ee9db109e5c3cd24663d46a48547b46a0d9836564674f1f4750; file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FCatalogJournal.scala#sha256:6d0dd55713633e3f02ae6a932e23c9d5613bd5cba0da80260b59d8270aae91d0; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FCatalogJournalSuite.scala#sha256:902e5d5fe8524df5aa828280fd3d1a3fb822195091e38e04a3d0b223e1b5c6bd; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FCatalogJournalPropertiesSuite.scala#sha256:a359ff0bc31db6e45d2ab42fa8c3feb6a68b0f03e02c49df54cd6a1342f6ec40; file:openspec%2Fchanges%2Fintroduce-versioned-boundary-codecs%2Fspecs%2Fcatalog-command-replay%2Fspec.md#sha256:f94e8c0656bb6f1c750e481d3c5ec5365bd765672253de5c3771db8c163bde18 | PASS |
| AC-020 | both | file:target%2Fcorgi-verify%2Fs05-canonical-verify.log#sha256:127bc0e9ca767ee9db109e5c3cd24663d46a48547b46a0d9836564674f1f4750; file:target%2Fcorgi-verify%2Fs05-ready-verify.json#sha256:bc529e370b5cdd7e5e546c1e8094c80befbe2e56e3d6ee01c10c46bba3d5170d; file:target%2Fcorgi-verify%2Fs05-jmh-verify.log#sha256:a1e093f43b3687c79c59abb780ddd94ebec2011eea08e47b85fab49f2f0ae7cf; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FBoundaryCodecCompatibilitySuite.scala#sha256:283196b99c21af4a089f339a62c1fb844fe1e4205e7e97299db77c87b03a8525; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FBoundaryCodecRobustnessPropertiesSuite.scala#sha256:35a95427a9746348fc7c5e69a118f1cb428ba64856d62c62bba083aa24997ebf; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:235c62f7161a92051180255e293dc6cfbb9992d509f1eaeb2abd2e788689acce; file:boundary-codecs%2FREADME.md#sha256:c34af73a955d79a6c549ad64c020839b865445b1a5474328e0b8add9550d0afd; file:docs%2Fboundary-codecs-apply-evidence.md#sha256:8ee61c1be5ab806127d047f361a8c341734849d7ab8e2bedda18faa091eea381 | PASS |

## Implementation
- Task Group 1: `bc3d2c18c97aa7d8390112f8fb5405ba474dd036`
- Task Group 2: `a658d9006fb3eb8d4d46faa15b72df43fd24ab17`
- Task Group 3: `1c5769046148f4bd8b42a8bba9b07539bccddff2`
- Task Group 4: `3e16373aa40d2fc589a08233ea6a1fef7f132e93`
- Task Group 5: `3d3dd8aa25343d7efbc86b4b26706222311548dd`
- Task Group 6: `e831e90e4fe4c2ae226c40a9d80b3f175ebb43ca`
- Task Group 7: `da476194a02600abc9675a0a1dab933e48587d16`
- Task Group 8: `2efdf26833ee1aa0bac6be61c9c8eea9c7306df9`
- Task Group 9: `c47480998d495257fa61f979c9ba56aa6b246ee7`
- Task Group 10: `ffc0b1eb097f34254c63dd5679ef6e0c702e808f`
- Task Group 11: `1f0d324a552b9d5be5bc6a43bade4ac5676e0b56`
- Task Group 12: `4c93275e3ce5dfaae219d1a9567461951cbc79e1`
- Final HEAD: `4c93275e3ce5dfaae219d1a9567461951cbc79e1`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human-confirmed no runtime impact: this delivery adds a pure library/JAR boundary with no runnable UI, API, CLI, or service surface; public consumption, reconstruction, compatibility, and failure paths are covered by canonical automated and completed-JAR evidence.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0002-architecture-portfolio`
- `openspec/changes/archive/2026-09-02-introduce-versioned-boundary-codecs`
- `openspec/changes/archive/2026-09-02-introduce-versioned-boundary-codecs/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/10
