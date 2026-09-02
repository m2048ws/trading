# Actual execution lifecycle

`trading-execution-lifecycle` is the pure domain owner for authoritative facts about actual order execution. It is
deliberately separate from immutable order intent and from hypothetical execution scenarios.

The module may depend on instrument economics, order model, quantities available through those boundaries, and pure
support libraries. It does not own transport, codecs, effects, streams, clients, persistence, telemetry, venue SDKs,
fees, risk, or application/runtime coordination.

Identity authority is explicit:

- the application supplies command, logical execution-order, and lineage identifiers;
- an execution source and account qualify native source event, order, fill, stream, and sequence identifiers;
- checked constructors validate representation only—they do not generate identifiers or infer authority from hashes,
  timestamps, payload equality, receipt order, or delivery attempts.

All public values in this module reject Java object serialization. Later lifecycle values retain these identities and
their source qualification rather than replacing them with locally generated receipts.

## Evidence and derived knowledge

Commands, dispatch evidence, and source facts are independent immutable inputs. Recording a submit command does not
manufacture an acknowledgement; a timeout can remain indeterminate; a cancel command is only a request until a source
fact confirms effective cancellation. `ExecutionState` retains replay, conflicts, unresolved references, stream gaps,
rewinds, and explicit incompleteness instead of collapsing them into a mutable status.

`LifecycleObservation` derives submission and cancellation knowledge, an effective-fill ledger, exact signed
`PositionLots`, and anomalies from that retained evidence. Distinct qualified fills contribute separately, duplicate
reports do not double count, and ordered corrections replace only the referenced fill's effective economics. A bust
makes that fill's current contribution zero without deleting its history. Overfills and fills provably sequenced after
effective cancellation remain real exposure and are reported as typed anomalies.

For example, if a buy order has two active fills of two and three lots, observation reports exact exposure of five
position lots. A later authoritative correction of the second fill to four lots changes exposure to six; a later bust
of that fill returns it to two. If cancellation is sequenced before the corrected fill in the same qualified source
stream, the four-lot contribution is also retained as post-cancellation exposure. An unsequenced fill makes no such
before/after claim.

Canonical replay sorts normalized evidence by authority and dependency, not arrival time. `TransitionWork` records
index lookups, index updates, and full-history scans; ordinary indexed insertions and duplicate detection report zero
full-history scans. Equivalent evidence permutations converge to structurally equal state.

## Boundary and future direction

The sibling `trading-execution-scenario` module models hypothetical matched outcomes supplied by a caller. Scenario
slices are not source facts and carry no command, venue, account, event, fill, cancellation, or completeness authority.
Actual execution never converts a scenario into evidence.

This Slice deliberately exposes no native amend or atomic cancel-replace operation. Confirmed cancel-then-submit is a
checked lineage link between distinct logical execution orders and makes no atomicity, priority, or cutover promise.
It also excludes venue matching/routing, multi-order campaigns, accounting, PnL, fees, risk, transport, persistence,
and durable schemas.

Later Slices may place effect-polymorphic workflow ports in `trading-application`, concrete clients/resources/streams
and telemetry in `trading-runtime`, and versioned records in a boundary-codec artifact. Those layers will consume this
module's checked values; this pure module will not depend back on them.

Focused verification is available with `sbt executionLifecycle/test`; completed-JAR and forbidden-dependency checks
run under `sbt adversarialBoundary/test`.
