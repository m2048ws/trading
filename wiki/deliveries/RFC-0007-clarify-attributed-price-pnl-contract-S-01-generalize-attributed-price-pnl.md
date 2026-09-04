---
type: delivery
updated: 2026-09-04
rfc: RFC-0007-clarify-attributed-price-pnl-contract
slice: S-01-generalize-attributed-price-pnl
change: generalize-attributed-price-pnl
status: archived
archived: 2026-09-04
evidence_manifest: sha256:99cc050096550e9c57241125e37fa4d6aaa341a8c54413e55d493d1da26c8551
source_digest: sha256:9eb8ee95174cead0868ba533797007510fb3101a3471ef65c79a485a33f1705c
---

# RFC-0007-clarify-attributed-price-pnl-contract/S-01-generalize-attributed-price-pnl

## Outcome
Establish the first pure multi-order trade aggregate above actual execution lifecycles. An application or strategy
shall be able to declare that immutable orders belong to one trading intent, grow that campaign without rewriting its
history, observe the combined effective execution evidence, calculate exact campaign-relative price PnL, and obtain a
checked closeout only when the supplied evidence proves that campaign exposure is flat and no unresolved member can
still contribute a fill.

Generalize the existing exact round-trip price calculation only far enough to give hypothetical round trips and actual
campaigns one instrument-economic implementation. The shared calculation shall fold attributed priced position changes
from a campaign-relative flat origin, retain exact dimension, grid, identity, settlement, endpoint, and provenance
information, and preserve all reference-coherent round-trip and fee-inclusive scenario results. Campaign construction,
observation, valuation, and closeout shall remain pure and deterministic.

Clarify the validation contract exposed by that generalization. Retained asset and grid references are semantic
evidence, not interchangeable labels: a round-trip market assembled from a foreign reference-data lineage is invalid
even when its displayed instrument ID is equal to the requested instrument ID. Scenario valuation shall reject that
input at its original leg and slice with the truthful retained `ReferenceDataError`; it shall not report a fabricated
instrument mismatch or silently preserve the previously undetected success. Once all checked identities and retained
references are compatible, finite `BigInt`/`Rational` arithmetic is total under the current exact representation and
does not need an invented arithmetic-failure branch.

The initial campaign boundary is deliberately narrow: one assembled instrument and one exact execution target per
campaign. Membership is explicitly declared by its owning caller and is append-only until closeout. This makes flatness
and source/account scope unambiguous without presenting a single campaign as a portfolio, cross-venue netting set, or
account position.

This amendment retains the five RFC-0006 Slice identifiers and acceptance-criterion identifiers. It changes only the
`S-01-generalize-attributed-price-pnl` compatibility and validation contract; the four later campaign Slices retain
their prior scope, and their references to unchanged scenario behavior inherit the explicit S-01 compatibility
exception.

## Boundary Delivered
Delivery begins from the integrated result of all five RFC-0005 Slices. No Slice in this RFC shall implement against or
restore a pre-RFC-0005 representation solely to avoid the simplified Scala-first execution and scenario APIs.

Instrument economics remains the sole owner of exact price and PnL mathematics. It shall expose one direct pure
calculation over an explicit instrument, zero or more attributed priced position changes, and a closed endpoint choice.
Each priced change retains a signed instrument-bound `PositionLots`, the immutable `MarketState` at which that change
is valued, and an opaque caller-owned attribution value. The calculation derives the ending campaign-relative position
and exact settlement cashflow as:

```text
ending position = sum(position change i)
execution cashflow = -sum(positionValue(position change i, market state i))

flat result = execution cashflow, when ending position is flat
marked result = execution cashflow + positionValue(ending position, terminal mark), otherwise
```

The endpoint is an honest sum: a flat endpoint is valid only for flat ending exposure, while non-flat exposure requires
one explicit coherent terminal `MarketState`. Zero changes with a flat endpoint is the lawful zero-price-PnL campaign
outcome; campaign membership itself remains non-empty. Position evidence is validated independently from market
evidence so that a derivable endpoint mismatch accumulates beside unrelated market/reference failures. Independent
identity, retained-reference, and per-change validation failures accumulate deterministically before dependent
valuation. The implementation shall preserve input attribution and exact rational arithmetic and shall not depend on
order, scenario, fee, execution, campaign, risk, application, codec, or runtime types.

For every finite input whose checked position, market, asset, grid, and endpoint references reconcile with the explicit
instrument, the current unbounded `BigInt`-backed quantity coordinates and normalized exact `Rational` operations make
the calculation's arithmetic total. Expected invalid coordinates, non-positive values, off-grid values, and malformed
references remain rejected by their owning constructors or by the calculation's reference-validation stage. Resource
exhaustion is not a domain arithmetic failure. A later bounded representation or new refinement would require its own
explicit contract and typed owning error.

`trading-execution-scenario` shall adapt each checked round-trip matched slice to that shared calculation. Its entry and
exit vocabulary, checked flatness, public result, error location, slice ordering, deterministic fail-fast presentation,
and downstream fee attribution remain scenario-owned. `ScenarioValuation` and fee-inclusive composition shall not
retain a second price-PnL fold. The existing two-leg facade remains domain-readable even though the underlying economic
calculation accepts any finite number of priced changes.

Reference-coherent round trips retain characterized pre-generalization results and failures. As one deliberate
compatibility exception, a same-ID market carrying foreign asset or grid lineage shall now fail rather than pass. The
existing four-alternative outer `ScenarioValuationError` remains unchanged: the failure is located with
`ScenarioValuationError.SliceValue(leg, sliceIndex, cause)`. Instrument economics shall add a truthful
`ValuationReferenceDataMismatch(context, cause: ReferenceDataError)` alternative to the existing `ValuationError`
algebra, preserving the original reference-data cause instead of synthesizing an instrument mismatch whose expected
and supplied IDs are equal. This nested public-algebra expansion and its Scala exhaustive-match impact are explicitly
accepted; completed-artifact clients shall characterize the new alternative.

A new non-empty `trading-trade-campaign` artifact under package root `trading.campaign` shall own campaign identity,
membership, declared relationships, observation, valuation attribution, and closeout evidence. It depends one-way on
instrument economics and actual execution lifecycle. Quantities, reference data, instrument economics, order model,
execution lifecycle, hypothetical scenarios, fee policy, and risk remain independent of campaigns; the campaign
artifact contains no concrete effect or external representation dependency.

The caller supplies a stable checked `CampaignId` and one already checked `ExecutionLifecycle` to create a campaign.
The first member is the campaign root. Further lifecycles are registered through a pure immutable transition with a
closed declared exposure intent such as increase, reduce, or close and relationships that reference only members
already present. All members retain the root's exact assembled instrument and `ExecutionTarget`. Exact re-registration
is idempotent; reuse of an execution-order identity with different lifecycle, intent, or relationship content is a
typed conflict. Missing relationships, foreign instruments or targets, and independently detectable structural
violations are typed and deterministic. Membership order is stable and append-only: no public operation removes,
reparents, or silently relabels a member. Declared intent remains attribution metadata and never overrides order intent
or effective execution evidence.

Campaign observation consumes the campaign plus exactly associated public `LifecycleObservation` values. It validates
membership before economic work and reuses each observation's already-derived effective-fill ledger rather than
replaying source facts or deriving another ledger. Active effective fills contribute their corrected effective lots
and prices; busted fills contribute no exposure; ambiguous or conflicting fills remain explicit blockers. Signed
position changes come from the member order's checked intent applied to effective lots. The result retains member and
fill attribution, exact known campaign-relative exposure, lifecycle anomalies, conflicts, incomplete streams,
unsequenced evidence, unresolved references, and submission or cancellation uncertainty. Known exposure is not
misrepresented as authoritative final exposure while any relevant blocker remains.

Campaign price valuation consumes a campaign observation and explicit immutable valuation evidence. Every active
effective fill has exactly one associated `MarketState` whose instrument and price agree with that fill; this supplies
the settlement conversions that a bare execution price does not carry. A non-flat observation additionally requires
one terminal mark. Structural and valuation failures remain typed and no partial authoritative PnL is returned after a
missing, duplicate, foreign, incoherent, ambiguous, or conflicting economic input. Successful evaluation adapts the
fill-attributed changes to the shared instrument-economic calculation and retains the campaign, member, fill, market,
endpoint, and exact price-PnL provenance. It does not manufacture fee contributions or call hypothetical fee policy.

Closeout is checked evidence bound to the exact campaign membership and lifecycle observations supplied. It succeeds
only when campaign-relative effective exposure is flat, valuation is a flat-endpoint result, each member is either
never issued or has an authoritative terminal outcome, and no pending or indeterminate submission, possibly live
accepted order, incomplete stream, conflict, ambiguous fill, unresolved modifier, or other retained evidence can hide
or later contribute ordinary execution. Historical anomalies remain visible even when reconciled evidence permits
closeout. A closeout does not assert a timeless fact about source evidence: later authoritative corrections, busts, or
other facts require a new observation and closeout decision rather than mutating or silently refreshing the old value.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-001 | automated | file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FAttributedPricePnl.scala#sha256:9e769d17aed1bdda81fa122a2b65c2379150679622cef4c3cc492084886c5330; file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FInstrument.scala#sha256:2f109d0e4a15f83c0c81792b6460a05cdb5da3f170920108c6396f242a9c2fe8; file:instrument-economics%2Fsrc%2Ftest%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FAttributedPricePnlSuite.scala#sha256:a3da720a165100d5bcc9e8e50ee7a962865db921acf6eea11d66a1706d2399ac | PASS |
| AC-002 | automated | file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FError.scala#sha256:88a789e9bd4253b6a70ebb27cd5045f47d74062c4b76b5c6505af9c9a86e2098; file:instrument-economics%2Fsrc%2Fmain%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FAttributedPricePnl.scala#sha256:9e769d17aed1bdda81fa122a2b65c2379150679622cef4c3cc492084886c5330; file:instrument-economics%2Fsrc%2Ftest%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FAttributedPricePnlSuite.scala#sha256:a3da720a165100d5bcc9e8e50ee7a962865db921acf6eea11d66a1706d2399ac | PASS |
| AC-003 | automated | file:execution-scenario%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fscenario%2FScenarioValuation.scala#sha256:1125a7cd51547543e5f5f7df0988f3c4b5e200193156d61883727425e24532a2; file:execution-scenario%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fscenario%2FScenarioValuationSuite.scala#sha256:cca762fd7affe8c3297999f0da9a8c4cb1e35c92208552745f1d8e6927ba8dad; file:fee-policy%2Fsrc%2Ftest%2Fscala%2Ftrading%2Ffee%2FFeeInclusivePnlSuite.scala#sha256:4ecf89684c35ea05db6dbe31a33becddffcf3afa67624f20cd91f338652a51cd; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-scenario-compiler%2Fpositive%2FScenarioValuationClient.scala#sha256:584a866deada05d6673259a4b8c4a4a42094c87f5d83b51f8805c142a3fce7f4 | PASS |
| AC-004 | automated | file:instrument-economics%2Fsrc%2Ftest%2Fscala%2Ftrading%2Feconomics%2Finstrument%2FEconomicsPropertiesSuite.scala#sha256:dfb9a78030b49b5f7bfcc46791cb43f64913f0dd321571b894326d5ec4cff91c; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:243406f00e966df7756f7e0a27abe11ee46c7be2ee78c31d375c7161d4a9ca4f; file:target%2Fcorgi-verify-rfc0007-s01%2Fci-33872401345.json#sha256:3f8ae5c721148844cb07a9b6e26aa74e94bff8be05b5a63c04f6c54ce7e165c8 | PASS |

## Implementation
- Task Group 1: `8739045a2d5c995e519a97e8ab3902b0e41ac6e6`
- Task Group 2: `ac05a9a3946fe3210ab0016745be6466ec5a307f`
- Task Group 3: `19eb99b65b8b3583eef9966ec5cb0ce4742eeef0`
- Task Group 4: `fe95e1d2fcb8eb80a832b5d8a808c1857dab250f`
- Task Group 5: `659f1ae339d92130e48924f55661d5b8fe7767b0`
- Final HEAD: `659f1ae339d92130e48924f55661d5b8fe7767b0`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: pass by m2048ws — The pure backend public-call-chain walkthrough passed at the verified final revision; all RFC criteria require automated evidence and passed canonical Verify.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0007-clarify-attributed-price-pnl-contract`
- `openspec/changes/archive/2026-09-04-generalize-attributed-price-pnl`
- `openspec/changes/archive/2026-09-04-generalize-attributed-price-pnl/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/48
