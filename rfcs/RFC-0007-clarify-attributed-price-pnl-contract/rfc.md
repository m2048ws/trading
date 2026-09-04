# RFC-0007-clarify-attributed-price-pnl-contract: Establish exact trade campaigns with strict reference validation

## Goal

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

## Non-goals

- Do not infer campaign membership from instrument identity, execution target, timestamps, order direction, matching
  quantities or prices, source fills, or `OrderLineageId`. Lineage continues to mean mechanism-neutral order
  continuation, not campaign intent.
- Do not support multi-instrument or multi-target campaigns, cross-account or cross-venue netting, baskets, books,
  portfolios, reporting-currency aggregation, or transfers between accounts or execution targets.
- Do not calculate venue-reported or estimated trading fees, rebates, commissions, funding, interest, tax, liquidation
  charges, deposits, withdrawals, or fee-inclusive campaign net PnL. Existing hypothetical fee-policy and
  fee-inclusive round-trip behavior remains unchanged for reference-coherent inputs.
- Do not add campaign risk, VaR, expected shortfall, margin, liquidation, collateral, hedge, concentration, portfolio
  sizing, or account-aware enforcement of `PositionEffect.ReduceOnly`.
- Do not claim that campaign-relative exposure is the surrounding account's position or that a declared increase,
  reduction, or close intent achieved its intended economic effect.
- Do not create, amend, submit, cancel, activate, schedule, or route orders; add OCO/bracket orchestration; or make
  campaign relationships an execution-control mechanism.
- Do not add an application port, live market-data acquisition, clock, ID generator, persistence repository,
  transaction, stream, runtime interpreter, external client, telemetry integration, or boundary codec.
- Do not introduce a universal calculation-target hierarchy or make instruments, scenarios, campaigns, baskets, and
  portfolios interchangeable domain entities merely because their economic results may compose.
- Do not change authoritative execution command, source-fact, replay, correction, bust, anomaly, completeness, or
  reconciliation semantics owned by `trading-execution-lifecycle`.
- Do not add a bounded numeric representation, approximation, grid projection, or unreachable arithmetic/refinement
  error merely to manufacture a failure case for already compatible checked values.
- Do not preserve successful valuation of same-ID foreign-lineage values obtained through erased or cast input, and do
  not add a fifth outer `ScenarioValuationError` alternative to explain their rejection.

## Boundary

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

## Slices

### S-01-generalize-attributed-price-pnl: Establish one exact finite-change price calculation

- AC-001 [evidence: automated]: Instrument economics exposes one direct pure operation that derives exact ending
  position, per-change settled price contributions, and `PricePnl` from an explicit instrument, zero or more
  attribution-preserving priced position changes, and a flat or marked endpoint according to the RFC formula; valid
  empty flat input produces the typed exact zero without raw-scalar reconstruction.
- AC-002 [evidence: automated]: Flat input with non-flat derived exposure, marked input with flat exposure, foreign
  instrument identities or retained references, and invalid per-change valuation inputs return precise typed failures;
  independently detectable failures accumulate in stable input order, endpoint consistency is derived whenever valid
  position evidence permits it, and dependent valuation does not run without its prerequisite evidence. Compatible
  checked finite inputs are total under the current unbounded exact representation and require no synthetic
  arithmetic/refinement failure.
- AC-003 [evidence: automated]: Reference-coherent single-slice and multi-slice long and short `RoundTripScenario`
  valuation and fee-inclusive evaluation use the shared calculation and remain exactly equal in result, error location,
  contribution order, fee attribution, settlement reference, and rational value to characterized pre-generalization
  behavior. A same-ID foreign-lineage market is deliberately rejected at its original leg and slice through the
  existing outer `ScenarioValuationError.SliceValue` case carrying a truthful
  `ValuationReferenceDataMismatch(context, cause)`; the nested `ValuationError` exhaustiveness change is covered from
  completed artifacts.
- AC-004 [evidence: automated]: Property and completed-artifact tests cover three-or-more-change scale-in and scale-out
  examples, direct open-short and cross-zero/reversal paths, exact zero, input-order-preserving attribution,
  permutation-invariant totals, signed exact totality, and linear traversal cost; instrument-economics production and
  compiler boundaries contain no order, scenario, fee, execution, campaign, risk, application, codec, or runtime
  dependency.

### S-02-create-append-only-campaigns: Define explicit single-instrument single-target campaign membership

- AC-005 [evidence: automated]: Checked construction consumes a caller-supplied `CampaignId` and first checked
  `ExecutionLifecycle` and returns one non-empty campaign retaining the exact assembled instrument, execution target,
  root execution-order identity, campaign-relative flat origin, and stable member order without generating an ID or
  performing an effect.
- AC-006 [evidence: automated]: Pure registration appends a checked lifecycle with its closed declared exposure intent
  and existing-member relationships; successful construction makes all relationships rooted and acyclic, preserves
  member provenance, and exposes a total transition whose alternatives cannot combine success with violations.
- AC-007 [evidence: automated]: Exact member re-registration is idempotent, while reuse of an execution-order identity
  with different lifecycle, intent, or relationship content is a typed conflict; foreign instrument or target,
  missing relationship, duplicate relationship, and other independent definition violations accumulate in stable
  source order without changing the previous campaign.
- AC-008 [evidence: automated]: Public APIs provide no member removal, reparenting, unchecked membership constructor,
  inferred-membership operation, or campaign interpretation of `OrderLineageId`; downstream positive and negative
  compiler fixtures preserve the associated instrument dimensions and reject foreign lifecycle shapes.
- AC-009 [evidence: automated]: The new non-empty campaign artifact and root aggregate build with a one-way production
  dependency on execution lifecycle and instrument economics, while all upstream and sibling production classpaths
  remain free of campaign classes and campaign production code remains free of fee, risk, application, runtime, codec,
  persistence, clock, concurrency, and client types.

### S-03-project-campaign-execution: Derive exact campaign exposure from lifecycle observations

- AC-010 [evidence: automated]: Campaign observation accepts exactly one matching `LifecycleObservation` per member,
  accumulates missing, duplicate, unrecognized, foreign-instrument, foreign-target, and lifecycle-identity violations
  deterministically, and performs no economic projection until membership association succeeds.
- AC-011 [evidence: automated]: Projection reuses each supplied observation's effective-fill ledger, derives signed
  changes from member order intent and effective lots, includes corrected active lots and prices exactly once, gives
  busted fills no exposure, and returns exact campaign-relative known exposure with stable member and qualified-fill
  attribution.
- AC-012 [evidence: automated]: Ambiguous or conflicting effective fills, overfill and cancellation anomalies, command
  and source conflicts, incomplete streams, explicitly unsequenced evidence, unresolved fill references, and
  submission or cancellation uncertainty remain inspectable at their owning member and campaign locations; known
  exposure is never labeled complete or authoritative while a relevant blocker remains.
- AC-013 [evidence: automated]: Equivalent campaign membership and lifecycle evidence yield structurally equal
  observations under every input-observation permutation and replay delivery permutation, with deterministic typed
  output ordering and traversal proportional to members plus effective-ledger entries rather than a nested rescan of
  raw command or source facts.

### S-04-value-campaign-price-pnl: Reuse the shared calculation for actual campaign fills

- AC-014 [evidence: automated]: Campaign price valuation requires one associated immutable `MarketState` for every
  active effective fill, validates ordinary instrument identity and exact equality with the effective fill price, and
  adapts the resulting signed attributed changes to the instrument-economic calculation without a second price-PnL
  fold.
- AC-015 [evidence: automated]: A flat authoritative observation uses the flat endpoint, including the exact zero result
  when no member filled; a non-flat authoritative observation requires one coherent terminal mark and returns exact
  mark-to-market campaign-relative price PnL. Missing, duplicate, foreign, fill-price-mismatched, incoherent-conversion,
  or wrong-endpoint evidence returns deterministic typed failures and no partial authoritative result.
- AC-016 [evidence: automated]: Successful valuation retains campaign, member, qualified-fill, correction, market-state,
  endpoint, settlement, and contribution provenance, and tests cover long and short campaigns with at least three
  orders, partial scale-in and reduction, corrected fills, busted fills, and exact agreement with direct use of the
  shared calculation.
- AC-017 [evidence: automated]: Campaign PnL contains price PnL only and neither invokes hypothetical `FeePolicy` nor
  manufactures fees, funding, account cashflows, or risk; existing scenario fee-inclusive results remain unchanged for
  reference-coherent inputs and campaign production classpaths contain no fee-policy or risk dependency.

### S-05-certify-campaign-closeout: Make flat complete closeout proof-carrying

- AC-018 [evidence: automated]: Checked closeout returns a proof-carrying closed campaign bound to the exact campaign,
  observations, flat-endpoint valuation, contributions, and price PnL only when effective campaign exposure is flat
  and every member is never issued or has sufficient authoritative terminal evidence with no possible unresolved
  ordinary fill.
- AC-019 [evidence: automated]: Non-flat exposure, a marked endpoint, pending or indeterminate submission, a possibly
  live accepted order, incomplete authoritative streams, command or source conflicts, ambiguous fills, unresolved
  corrections or busts, and every other independent closeout blocker produce a non-empty deterministic typed
  collection and no closed campaign; retained historical anomalies are not erased from a successful result.
- AC-020 [evidence: automated]: Repeating closeout over identical immutable inputs returns an exactly equal result;
  changed membership or later correction, bust, fill, completeness, or conflict evidence requires a newly evaluated
  observation and cannot mutate, extend, or silently validate a prior closeout value.
- AC-021 [evidence: automated]: Only the checked closed alternative is accepted by a downstream operation requiring
  campaign closeout, the open campaign API has no member-removal operation and the closed API has no registration
  operation, and completed-artifact compiler fixtures plus focused, property, dependency-boundary, serialization, and
  clean aggregate tests pass on JDK 25.

## Risks

- A reusable price calculation could grow into a vague universal calculator or erase instrument-specific evidence.
  Mitigation: generalize only the finite exact position-change fold, keep the instrument explicit, retain typed
  settlement and endpoint evidence, and leave scenario and campaign adapters domain-owned.
- Strict retained-reference validation changes one previously undetected same-ID foreign-lineage scenario from success
  to failure. Mitigation: state the exception explicitly, retain its original leg and slice, preserve the complete
  `ReferenceDataError`, and compare both the reference-coherent and newly rejected paths with the baseline.
- Extending `ValuationError` can break exhaustive downstream Scala matches even though the four outer
  `ScenarioValuationError` cases remain stable. Mitigation: accept that pre-release source compatibility effect
  explicitly and add completed-artifact exhaustive-match coverage for the truthful reference-data alternative.
- Claiming an unreachable arithmetic/refinement failure could lead to an invented numeric ceiling or a dishonest error
  branch. Mitigation: specify totality only after compatibility validation under the current finite unbounded exact
  representation, retain constructor-owned invalidity, and require a later contract before adding a bounded
  representation or new refinement.
- Migrating round-trip valuation could alter price signs, error ordering, attribution, or fee totals beyond the stated
  reference-validation exception. Mitigation: characterize the current public behavior first and require exact
  example, property, and completed-artifact equivalence through the existing fee composition.
- An empty effective-fill set could be confused with an invalid empty campaign. Mitigation: keep campaign membership
  non-empty while recognizing zero priced changes as the lawful exact flat-origin economic identity.
- Campaign relationships could be mistaken for authority to submit, activate, cancel, or classify actual execution.
  Mitigation: name them declared exposure intent, keep orchestration out of the artifact, and derive every economic
  effect from lifecycle-owned effective fills and immutable order intent.
- Aggregating different instruments or targets could make a flat total hide real account or venue exposure. Mitigation:
  require one exact assembled instrument and one exact `ExecutionTarget` for every member in this RFC and defer
  composition to an explicit later capability.
- Re-reading raw facts or original fills could double-count corrections and retain busted exposure. Mitigation: consume
  only each supplied observation's effective-fill ledger and retain ambiguous or conflicting classifications as
  blockers rather than guessing.
- A fill price alone may be insufficient to value settlement for base- or third-asset-settled instruments. Mitigation:
  require explicit fill-associated coherent `MarketState` evidence and validate its price and identity before the
  shared calculation.
- Closeout could be mistaken for permanent external finality despite a later correction or bust. Mitigation: bind the
  proof to exact immutable observations, expose retained evidence, and require a fresh projection and decision after
  any new authoritative fact.
- Path-dependent dimensions across campaigns, lifecycles, observations, and valuation evidence may make ordinary Scala
  calls difficult or tempt unchecked casts. Mitigation: provide small domain-named creation, registration, observation,
  valuation, and closeout operations; isolate any unavoidable strengthening immediately behind checked runtime
  identity and reference evidence; and verify both ergonomic positive and hostile negative downstream compilation.
- Campaign projection could become quadratic as members and fill histories grow. Mitigation: retain stable indexed
  membership, consume already-indexed effective ledgers once, state the linear traversal claim narrowly, and measure
  representative large campaigns.
