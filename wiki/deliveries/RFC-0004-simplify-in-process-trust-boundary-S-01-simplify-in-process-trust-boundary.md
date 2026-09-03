---
type: delivery
updated: 2026-09-03
rfc: RFC-0004-simplify-in-process-trust-boundary
slice: S-01-simplify-in-process-trust-boundary
change: simplify-in-process-trust-boundary
status: archived
archived: 2026-09-03
evidence_manifest: sha256:77f7954a068af43ca196f8e88262efca7c1b782ca20ebfa860cf2ef3847ef022
source_digest: sha256:9c8d459b6066675a3128ef4974967a1f199bbd3b0e4871179110e0fe57337880
---

# RFC-0004-simplify-in-process-trust-boundary/S-01-simplify-in-process-trust-boundary

## Outcome
Make the repository easier for one maintainer to understand, refactor, and operate by aligning its implementation with
its actual threat model: supported code running inside the application is cooperative and trusted, while data arriving
from wire, persistence, configuration, venue, or other external boundaries is untrusted until checked.

The delivery removes production reflection and method-handle machinery used to hide constructors, methods, or fields
from deliberately hostile same-package Scala, Java, or handcrafted JVM callers. It retains protections that prevent
ordinary programming mistakes: exact and dimension-safe types, refinements, closed domain alternatives, smart
constructors, typed validation, coherent catalog lineage, checked state transitions, bounded decoding, and explicit
versioned persistence formats.

## Boundary Delivered
The supported caller is well-typed Scala or ordinary Java application code that uses the documented API without trying
to defeat its visibility. The repository does not treat another class in the same JVM as a hostile security principal.
If mutually untrusted code is introduced later, isolation must be designed at a process, module, class-loader, or
sandbox boundary in a separate RFC rather than simulated through domain-object constructor tricks.

Production code in order model, execution scenario, risk, fee policy, boundary codecs, execution lifecycle, and
benchmarks SHALL contain no `java.lang.invoke.MethodHandle`, `MethodHandles.privateLookupIn`, reflective constructor,
private-method, or private-field access. Cached constructor descriptors and the casts required only to recover erased
types after dynamic invocation SHALL be removed. Cross-owner behavior SHALL instead move to the owning value or be
exposed as a narrow, statically callable, checked operation.

JVM-private construction SHALL no longer be treated as a domain proof. Values with purely local invariants MAY use
ordinary constructors when their field types make invalid states unrepresentable. Values requiring validation SHALL
retain domain-named smart constructors and typed expected failures. Essential defensive checks MAY remain in ordinary
constructors when erased Java inputs or accidental internal misuse could otherwise create a malformed instance, but
exceptions SHALL not replace the supported typed-failure path.

Evidence, assessment, replay, and transition values SHALL NOT become authoritative merely because their representation
was difficult to instantiate. Any operation that turns supplied data into a stronger trusted result SHALL establish
the required predicate itself or consume evidence whose authority still follows from a retained semantic capability.
For example, trigger verification must establish both instruction/evidence agreement and satisfaction of the trigger;
catalog handles must still originate from and reconcile against coherent lineage; execution replay must still check
identity, ordering, conflict, and completeness rules.

Closed Scala ADTs, associated evidence types, path-dependent dimensions, refinements, non-empty structures, exact
identities, and deterministic validation remain the normal accidental-error defenses. Runtime exact-class guards and
completed-JAR tests whose only purpose is rejecting deliberate Java subclassing, same-package spoofing, or direct
constructor invocation SHALL be removed or rewritten around observable semantic failure. Negative compiler tests that
prove ordinary type errors, illegal dimension/grid combinations, impossible ADT compositions, or dependency leaks
remain in scope.

External records remain data rather than authority. Boundary codecs SHALL continue strict bounded parsing, explicit
schema-version dispatch, exact primitive decoding, snapshot-coherent resolution, and reconstruction through owning
domain checks. Java object serialization SHALL continue to fail closed where it could accidentally establish an
unstable persistence contract. Reference-data lineage and other generative identities that prevent accidental mixing
remain semantic capabilities even though this RFC removes hostile-caller claims.

This is a pre-release source and JVM compatibility change. Existing domain-named Scala entry points and observable
successful/failing semantics SHOULD remain stable where they do not encode the retired threat model. JVM constructor
modifiers, synthetic access details, and unsupported Java construction paths are not compatibility commitments.
Normative specifications, architecture guidance, examples, and tests SHALL be reconciled so they describe one
consistent trusted-code/untrusted-data boundary.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-001 | both | file:rfcs%2FRFC-0004-simplify-in-process-trust-boundary%2Frfc.md#sha256:7eaf8391404d174836ff35e429e1618cc5ee06e15c5ec3eeebdc9289ebd3a8e5; file:docs%2Fdesign-principles.md#sha256:4caf82a903ba53c16948d32d6ff3f30cb150fda19a4b88a770cc920c9eaec99f; file:docs%2Farchitecture-charter-audit.md#sha256:d888a0883449e4d12195c2bb1622a62096d9231825e6717f7ab75ee1d79973cb; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fspecs%2Frepository-architecture%2Fspec.md#sha256:fd298a38a7ec00fc2a47c52b130aaa2bd0987c2a03dbf8996ba974663cfe165f; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fspecs%2Fscala-functional-design%2Fspec.md#sha256:ea4bdd5b676e586b4c4819296100eb27075ccbf2a3bf42d80eea55d7edadf2d2; file:target%2Fcorgi-verify%2Ftrust-ready-verify.json#sha256:7a48e65d5982dd8646c630781c445f4a6dd3477beb6997ecfcb40b7b53aec14b | PASS |
| AC-002 | automated | file:target%2Fcorgi-verify%2Ftrust-static-verify.log#sha256:cac5ce646914444567bf394b7f763a4084945e687ad3692b7c5113302f20e997; file:tools%2Fcheck-in-process-reflection.sh#sha256:12bd5296f3c5e613f7a47389122323f81c3723371fb059e87610ee99802662df; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-10.md#sha256:98d5993714cb1976471639f674ef8e2e3a442a60c3e622533335c8c5711acc61; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-11.md#sha256:7af6bd0ccb7836d94c30d17c8b2c089c0be2a20815290d51a687cafe04a8621c | PASS |
| AC-003 | automated | file:target%2Fcorgi-verify%2Ftrust-canonical-verify.log#sha256:b9f8727239366ad8160af8f6ace7327c018c88372c8f0735341f914cfa48cf2d; file:reference-data%2Fsrc%2Ftest%2Fscala%2Ftrading%2Freference%2FReferenceDataSuite.scala#sha256:c0ea239f0f1bed95109f656c0a060424833194e359e1519853552b8f8aea5608; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FOrderPropertiesSuite.scala#sha256:cdb9b4691bb9bb1c0373737d636e7c01e3a34e3a71c36f8e7619059e4d3638b9; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurvePropertiesSuite.scala#sha256:6720707bd528182e6708d91cec760854f65562a985138b13fd8ac2a4d4b765fc; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FExecutionStateSuite.scala#sha256:d437f7204700a1de676ca0939684d50a1ee481d909216d01ce34444a8f24da49; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FBoundaryCodecRobustnessPropertiesSuite.scala#sha256:35a95427a9746348fc7c5e69a118f1cb428ba64856d62c62bba083aa24997ebf | PASS |
| AC-004 | automated | file:target%2Fcorgi-verify%2Ftrust-canonical-verify.log#sha256:b9f8727239366ad8160af8f6ace7327c018c88372c8f0735341f914cfa48cf2d; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-10.md#sha256:98d5993714cb1976471639f674ef8e2e3a442a60c3e622533335c8c5711acc61; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-11.md#sha256:7af6bd0ccb7836d94c30d17c8b2c089c0be2a20815290d51a687cafe04a8621c; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FBoundaryCodecCompatibilitySuite.scala#sha256:283196b99c21af4a089f339a62c1fb844fe1e4205e7e97299db77c87b03a8525; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FEffectiveFillLedgerSuite.scala#sha256:3e0faa08a6c884ecc2e023c40f6f2e4a767f4f445b80cc51a3f6ca19808e3065 | PASS |
| AC-005 | automated | file:target%2Fcorgi-verify%2Ftrust-static-verify.log#sha256:cac5ce646914444567bf394b7f763a4084945e687ad3692b7c5113302f20e997; file:target%2Fcorgi-verify%2Ftrust-canonical-verify.log#sha256:b9f8727239366ad8160af8f6ace7327c018c88372c8f0735341f914cfa48cf2d; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-10.md#sha256:98d5993714cb1976471639f674ef8e2e3a442a60c3e622533335c8c5711acc61; file:openspec%2Fchanges%2Fsimplify-in-process-trust-boundary%2Fimplementation-notes%2Ftask-group-11.md#sha256:7af6bd0ccb7836d94c30d17c8b2c089c0be2a20815290d51a687cafe04a8621c; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FReferenceDataCompilerBoundarySuite.scala#sha256:7f47be784c51936201eab814ac885dba2bd9c88d8b4256cf1e75d0101bc4a56d; file:build.sbt#sha256:3f6a4ac80a23b6c00ba7592074ff2291b71b35d05753f0e2a3f4550c8eb1d12b | PASS |
| AC-006 | both | file:target%2Fcorgi-verify%2Ftrust-canonical-verify.log#sha256:b9f8727239366ad8160af8f6ace7327c018c88372c8f0735341f914cfa48cf2d; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Freference-data-java%2Fpositive%2FGridReconciliationAuthority.java#sha256:171bbc81400b2f959cb9b34dc8be771d5019a5f9f7aae94d00802d4e5d547e41; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FApplicationRuntimeBoundarySuite.scala#sha256:a4d3fce39644762c0d79c17bd63e775ea50cb0f1464cd051093835ddd1fad412; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:5423928c7246a027131db466ecf045b39ca2f859b75edfd164e77759414e6549; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FStrictJsonSuite.scala#sha256:cd2f7ea1532765ce8b2b710836e7e610be96ea1a33dd9bf214240158ebc6fa62; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FCatalogJournalSuite.scala#sha256:bd31152f99c6a796c5f0961a775dccb15c00fa11f0f7777b835b07087d23583f | PASS |

## Implementation
- Task Group 1: `cec75317dae5efb557e2ffaebe0e90270c7edab7`
- Task Group 2: `ddd81d7a7eeabe46e6f091677cfe20683267b6df`
- Task Group 3: `26e7e18d6ba907cf33c2c99f09cdfbd81968eaa7`
- Task Group 4: `2183827046526520b1531d8334266e6823dd52c1`
- Task Group 5: `5c362f39ab499198aef669bdecdb8f1b9eb87045`
- Task Group 6: `97099809d7a0509da2ec256878979e0be69d3878`
- Task Group 7: `3d6c696a080623ece84c2d04ddd2abbb90ef9de3`
- Task Group 8: `e0122e1616338c1ca515f39d14ea269ce5b52bf5`
- Task Group 9: `aff014e3c3356d32699a4af3a09fa59d354946d3`
- Task Group 10: `c5c2e90d7f9ceede40db22816e9794818416762f`
- Task Group 11: `1833257ee0fed4941e59d3482d8bf5cef85e7933`
- Final HEAD: `1833257ee0fed4941e59d3482d8bf5cef85e7933`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — Human reviewer confirmed no runtime impact: this change simplifies in-process construction and trust mechanics while preserving observable behavior and V1 wire representations.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0004-simplify-in-process-trust-boundary`
- `openspec/changes/archive/2026-09-03-simplify-in-process-trust-boundary`
- `openspec/changes/archive/2026-09-03-simplify-in-process-trust-boundary/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/36
