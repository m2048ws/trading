---
type: delivery
updated: 2026-08-31
rfc: RFC-0002-architecture-portfolio
slice: S-02-order-execution-scenarios
change: separate-order-and-execution-scenario-modules
status: archived
archived: 2026-08-31
evidence_manifest: sha256:cf55e7a63baf6f69f82624c3dda1f4cf8b2ca1ca742c6465fea71b43c28f7a03
source_digest: sha256:5d573cd0d12bbcffacb9b047475ee4beb81069dedabd420018b5d3f32f7acdd0
---

# RFC-0002-architecture-portfolio/S-02-order-execution-scenarios

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
| AC-005 | automated | file:target%2Fs02-tg13-canonical-verify.log#sha256:86072eecc35d7fc6e6190413a0c34b39d5f154b4089344c286c9fb3a8ecd87e5; file:build.sbt#sha256:efb082cc63779899e258cbf9a3e51b71b4c4259bc73670ada206f916120d08a1; file:order-model%2FREADME.md#sha256:190e8c48b1e40dfaf4b56415a373c31d23fa18cdddaace0a4eba33bd7903a156; file:execution-scenario%2FREADME.md#sha256:ad4fdd33443362273df33b346640b486576fde852e0a35a582788fb3c7ecb98a; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:7e5db82154d5754e1c1b8bc76e03f76e90fd42046ca21078ba3e00b32ce294cf; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-model-compiler%2FOrderModelHasNoDownstream.scala#sha256:9259801d41163f57f66b228c8c5c0c19fc625b05612c61ac1fabedcfa0805f21; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-scenario-compiler%2FExecutionScenarioHasNoUpstreamMutationOrDownstream.scala#sha256:e518bb05d05ae530875f28e07ac148262c18ee9bfd1654cd851b3afc7d342396 | PASS |
| AC-006 | both | file:target%2Fs02-tg13-canonical-verify.log#sha256:86072eecc35d7fc6e6190413a0c34b39d5f154b4089344c286c9fb3a8ecd87e5; file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FOrder.scala#sha256:8c0a089c9921dccddd6549b3f3f32f752313561f80e655d575d4b8a95169b3a0; file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FError.scala#sha256:6e74215c46ae50ddebec0cdefd548d0a845c16173943b608517168aa469bd5a7; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FOrderInstructionSuite.scala#sha256:2b613fb749ef221b0e9b0438f2d64c3443b87fc4a01717f5452ab8f5ca1081a3; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FOrderPropertiesSuite.scala#sha256:4d3090d374bde4de9461a6e4bddad9e56f550a749795f9a3458f94b1f97783e5; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-scenario-java%2Fnegative%2FRejectedAlgebraImplementations.java#sha256:74964606179030713936d10d755126ebaa70876f0455cd16a762224f769b623c; file:target%2Fs02-tg13-human-qa-carry-forward.log#sha256:eee2c2ebf937b2f16459d0a6e7c4219c2c773508f92aa5b386d5db1f27144d6b | PASS |
| AC-007 | automated | file:target%2Fs02-tg13-canonical-verify.log#sha256:86072eecc35d7fc6e6190413a0c34b39d5f154b4089344c286c9fb3a8ecd87e5; file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FScenario.scala#sha256:113162e734faaccf960e4e17c081b71df2df071a34d85bfd0baaafef25dc470f; file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FValidation.scala#sha256:75cee9b7493a965d00d5dc8b8abf797a907f38db6ac46eb0917f135acb2e2033; file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FError.scala#sha256:c19a6ab0aa62f29d9dfac9f563881ae4f80668ec1625f0f3d3cfe7684c2af56a; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FOrderScenarioSuite.scala#sha256:29a163d7d3671e1c2048808412618b547f77233b4c70c998ca5bad218b2a7c51; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FScenarioPropertiesSuite.scala#sha256:3a46eff955316d9c5efa0607b378dd411e2e6ff5d344da9451cb2487c88879bf; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-scenario-java%2Fnegative%2FErasedScenarioAssumptions.java#sha256:caf53663ed593a2c064d229ae9c37ada295edf78c5089bb979fa710746d272a5 | PASS |
| AC-008 | both | file:target%2Fs02-tg13-canonical-verify.log#sha256:86072eecc35d7fc6e6190413a0c34b39d5f154b4089344c286c9fb3a8ecd87e5; file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FScenario.scala#sha256:113162e734faaccf960e4e17c081b71df2df071a34d85bfd0baaafef25dc470f; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FOrderScenarioSuite.scala#sha256:29a163d7d3671e1c2048808412618b547f77233b4c70c998ca5bad218b2a7c51; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FScenarioPropertiesSuite.scala#sha256:3a46eff955316d9c5efa0607b378dd411e2e6ff5d344da9451cb2487c88879bf; file:economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Ffee%2Fpolicy%2FFeePolicy.scala#sha256:04fe60d1880d8c0a755e123405aa35c5c592abb65caee666e81b7c740abc5fea; file:economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FRisk.scala#sha256:a06d301f060b601f31d332893741385e4b6540705a24872467f111968c24a9ff; file:economics%2Fsrc%2Ftest%2Fscala%2Ftrading%2FDownstreamEconomicsSuite.scala#sha256:7f16fb88e3879c39bff684677079d1c4d7f1617ea0d9810701f8d8decec8f1cf; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:7e5db82154d5754e1c1b8bc76e03f76e90fd42046ca21078ba3e00b32ce294cf; file:target%2Fs02-tg13-human-qa-carry-forward.log#sha256:eee2c2ebf937b2f16459d0a6e7c4219c2c773508f92aa5b386d5db1f27144d6b | PASS |

## Implementation
- Task Group 1: `073f9340a4e64e781aea85b48f46f49c020cbea8`
- Task Group 2: `6bdd74f30f1730746498124c097158aae3c7a3fe`
- Task Group 3: `fb592df37a721c2cf6021df9eeaffc294a662b75`
- Task Group 4: `7d8604781c6ef810aa25c7d52b8100ee14474d84`
- Task Group 5: `0db021bc40848a72cc54c180178027979b799e32`
- Task Group 6: `97bec2609af7e330739bc3d14c0c966d5a00c765`
- Task Group 7: `79d099b117706a31e44c076fdc28c68a945de30f`
- Task Group 8: `5726eb8918778c026700963f0a48f91b062d1d70`
- Task Group 9: `a60b00a7f3a633e9d1c3ee6180ed991b2b0e9d0e`
- Task Group 10: `9d43e1127a814a90513a61a5e125be251acc8ef1`
- Task Group 11: `5a74678a78a27ff01c258ef4883a8a3d80d3bdc9`
- Task Group 12: `7843e98eae2594c322f6b1fe7d0a88a82c6dd43f`
- Task Group 13: `7ca2e135632a275562d46a0e6c5b08b3a3771639`
- Final HEAD: `7ca2e135632a275562d46a0e6c5b08b3a3771639`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: pass by m2048ws — Human downstream-library QA previously passed 50 of 50 focused order, scenario, and completed-JAR compiler-boundary tests at implementation commit 7843e98eae2594c322f6b1fe7d0a88a82c6dd43f. The approved exact final revision 7ca2e135632a275562d46a0e6c5b08b3a3771639 changes only the archive delta, task plan, and session checkpoint; production code, tests, build configuration, and boundary fixtures are byte-identical. Canonical Verify at the final revision passed 820 tests and the isolated Archive rehearsal preserved all canonical scenarios.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0002-architecture-portfolio`
- `openspec/changes/archive/2026-08-31-separate-order-and-execution-scenario-modules`
- `openspec/changes/archive/2026-08-31-separate-order-and-execution-scenario-modules/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/7
