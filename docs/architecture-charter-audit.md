# Architecture Charter Portfolio Audit

This is the Proposal 0 coherence and traceability record for the active change
`establish-architecture-and-functional-design-charter`. It records the audit
performed on 2026-08-28 before project guidance was changed.

## Portfolio completeness

All nine dependent changes exist and OpenSpec reports complete proposal,
delta-specification, design, and task artifacts for each:

1. `separate-quantity-and-reference-data-boundaries`
2. `introduce-functional-reference-catalog`
3. `introduce-instrument-assembly-boundary`
4. `establish-pure-instrument-economics`
5. `separate-order-and-execution-scenario-modules`
6. `introduce-pure-fee-policy-module`
7. `introduce-pure-risk-module`
8. `introduce-application-and-runtime-foundation`
9. `introduce-versioned-boundary-codecs`

Each dependent change passed strict validation individually. Repository-wide
strict validation passed all 17 registered change/spec items. No missing or
materially unresolved planning artifact blocks Proposal 0.

## Current physical implementation

Proposal 1 has now established the first dependent physical boundary. This table describes the implemented SBT
projects and must not be confused with the remaining proposed target.

| State | SBT project | Artifact/directory | Production dependency |
| --- | --- | --- | --- |
| Current | root `trading` | unpublished aggregate | aggregates quantities, reference data, economics, adversarial boundary |
| Current | `quantities` | `trading-quantities` / `quantities/` | external mathematical libraries only |
| Current | `referenceData` | `trading-reference-data` / `reference-data/` | quantities |
| Current | `economics` | `trading-economics` / `economics/` | quantities and reference data |
| Current test-only | `adversarialBoundary` | unpublished / `adversarial-boundary/` | packaged quantities, reference data, and economics artifacts |

The current graph is acyclic: `quantities <- referenceData <- economics <- adversarialBoundary`, with the test-only
boundary consuming each completed production artifact directly.

Current dependency coordinates include Scala `3.8.4`, independent
`catsVersion = 2.13.0` and `algebraVersion = 2.13.0` coordinates,
`discipline-munit` `2.0.0`, MUnit `1.3.4`, ScalaCheck `1.19.0`, and
`munit-scalacheck` `1.0.0`. The minimum build/runtime JDK is documented as 17.
with no resolved dependency upgrade from the Proposal 0 baseline.

## Primary ownership and target dependency audit

| Proposed responsibility | Primary owner | Direct lower-level needs | Owning proposal |
| --- | --- | --- | --- |
| Exact arithmetic, dimensions, anonymous grids, refinements, mathematical algebra | `trading-quantities` | mathematical foundations | 1 |
| Stable asset/grid identity, canonical handles, catalog state/snapshots/errors | `trading-reference-data` | quantities | 1–2 |
| Stable-ID instrument input and proof-producing assembly | instrument-assembly boundary, initially alongside economics | reference data and quantities | 3 |
| Assembled instruments, lots, positions, prices, payoff, valuation, economic fee values, P&L | `trading-instrument-economics` | quantities and reference data | 4 |
| Immutable order intent and instruction evidence | `trading-order-model` | instrument economics | 5 |
| Hypothetical matched execution evidence and checked scenarios | `trading-execution-scenario` | order model and instrument economics | 5 |
| Venue/account/tier fee rules and assessed attribution | `trading-fee-policy` | instrument economics, order model, execution scenario | 6 |
| Isolated-instrument downside and sizing procedures | `trading-risk` | quantities and instrument economics | 7 |
| Effect-polymorphic ports/workflows | `trading-application` | required reference/domain modules | 2 and 8 |
| Concrete effects, resources, concurrency, clients, streams, telemetry | `trading-runtime` | application plus required domain/codec modules | 8 |
| Wire/database/packed records, versions, parsing, encoding, checked reconstruction | `trading-boundary-codecs` | values it encodes; never runtime | 9 |

Every introduced or moved concept has one primary owner. The combined target
graph is acyclic: mathematical/reference/domain layers point only to smaller
lower-level meanings; order and scenario policy consume instrument economics;
fee and risk remain downstream; application declares capabilities; runtime
interprets them; codecs consume domain constructors but no lower layer depends
back on codecs.

## Adjacent-boundary coherence

| Boundary | Accepted names and trust transition | Validation/error owner | Effect/codec placement |
| --- | --- | --- | --- |
| Quantities -> reference data | anonymous `GridRef[D]`; stable `Asset`, `DimensionHandle[D]`, `GridIdentity`, `GridHandle[D]` | quantities own mathematical errors; reference data own stable identity, lineage, and reconciliation errors | no effects or packing in quantities |
| Reference state -> live catalog | `CatalogBatch + CatalogState -> CatalogState/result`; `CatalogSnapshot` is the only read view; `LiveCatalog[F]` captures/commits | reference data own pure catalog violations | port in application; concrete coordination in runtime |
| Reference data -> assembly | `InstrumentDefinition + CatalogSnapshot -> InstrumentSpec` | reference data own generic lookup failures; assembly adds instrument-role context and owns non-empty assembly errors | assembler is pure and accepts neither live catalog nor codec/parser |
| Assembly -> economics | `InstrumentDefinition -> InstrumentSpec -> Instrument` | assembly owns resolution/coherence; total instrument construction introduces no repeated lookup error | trusted instrument retains handles, not snapshot/live state |
| Economics -> orders/scenarios | `Instrument`, `OrderIntent`, instruction-shaped evidence, `ScenarioAssumptions`, `MatchedSlices` | order errors own instruction construction; scenario errors own evidence/outcome validation | both layers are pure and codec-free |
| Scenario -> fee policy | pure `FeePolicy` produces directives; assessment binds them to actual scenario slices; core `Pnl` stays in economics | policy owns rule/output errors; scenario assessment owns attribution errors | policy acquisition/effects remain outside |
| Economics -> risk | explicit instrument plus core `Pnl` produces refined downside/model observations | risk owns model/search errors; it does not reinterpret scenario/fee failures | pure; no scenario/fee dependency hidden in kernel |
| Application -> runtime | `LiveCatalog[F]` is the initial minimal port; runtime supplies Cats Effect interpreter | application owns capability outcomes; runtime owns lifecycle/cancellation mechanics | concrete effects and telemetry remain in runtime |
| Domain -> codecs | V1 record families contain stable IDs/exact primitives; decoding uses one snapshot and owning constructors | codec owns syntax/path/resource errors; reference/assembly/order/scenario errors remain typed stages | codecs are pure; caller captures snapshot; no `F`, live lookup, or I/O |

Naming, trust transitions, catalog responsibility, validation staging, effect
placement, codec ownership, and error ownership agree across adjacent proposals.
No proposal requires another proposal to expose a contradictory intermediate
API.

## Accepted implementation order and compatibility policy

The accepted order is Proposal 0 followed by Proposals 1 through 9 in the order
listed under portfolio completeness. Each change uses the steward apply,
independent-review, archive, and validation gates.

The important intermediate constraints are:

- Proposal 1 creates reference data and a temporary synchronized construction
  bridge; Proposal 2 replaces that bridge with pure state/snapshots and the
  application port, so the two should land consecutively.
- Proposal 3 establishes snapshot-based assembly before Proposal 4 narrows the
  instrument economics artifact.
- Proposal 4 leaves order/scenario, fee, and risk in a transitional aggregate;
  Proposals 5–7 move those concerns consecutively and Proposal 7 removes the
  empty aggregate.
- Proposal 8 supplies concrete runtime interpretation only after the pure
  application contract exists.
- Proposal 9 introduces the first explicitly versioned durable record families
  after stable IDs, snapshots, assembly, and order/scenario constructors exist.
- Every intermediate commit remains buildable, testable, reviewable, and
  revertible; no artifact is published and no release is declared until the
  complete sequence and final validation matrix pass.

Affected APIs are unreleased. Proposals update callers directly and add no
deprecated aliases, forwarding facades, or compatibility shims unless a later
explicit proposal identifies a real external consumer. In-memory forms before
Proposal 9 carry no persistence promise. Proposal 9's versioned records begin
the first deliberate durable compatibility contract.

## Known transitional exceptions

| Current exception | Why it is transitional | Migration owner |
| --- | --- | --- |
| Reference data contains the synchronized `QuantityRegistry` construction bridge | No immutable catalog/snapshot boundary exists yet | Proposal 2 replaces it with pure catalog state/snapshots; Proposal 8 adds runtime interpreter |
| Quantity-owned packing has been removed, leaving a deliberate durable-codec gap | Stable records require snapshots and an explicit schema owner | Proposal 9 adds versioned codecs |
| `trading-economics` owns instruments, orders, scenarios, fee policy, P&L, and sizing | Current aggregate predates responsibility split | Proposals 3–7, with Proposal 7 removing the empty aggregate |
| Instrument construction starts from issued handles and repeats provenance checks | Assembly does not yet own one snapshot-based trust transition | Proposal 3 |
| Order/scenario/fee/risk capabilities are discoverable through `Instrument` | Instrument currently acts as a service locator | Proposals 4–7 |
| Root documentation previously listed only quantities | Documentation lagged the implemented economics module | Proposal 0 documentation update |
| Application, runtime, codec, and benchmark target modules do not exist | Their physical boundaries require real implementation/dependency bodies | Proposals 2, 8, and 9 |

These exceptions describe current implementation facts, not accepted permanent
architecture. No proposed module or API is presented as available today.

## Requirement-to-artifact trace

### `repository-architecture`

| Requirement | Guide | Stable context/decision | Review enforcement |
| --- | --- | --- | --- |
| Cohesive ownership and directed dependencies | Responsibility and dependency direction | `.agent/project.md`, architecture invariants, active charter decision | Charter ownership/dependency questions |
| Target responsibility layers | Responsibility and dependency direction; layer profile | `.agent/project.md` target graph | Current/proposed state and scope review |
| Mature mechanisms contained by responsibility | Dependency admission | dependency/platform invariants and active decision | Dependency-admission review |
| Logical boundaries before physical modules | Responsibility and dependency direction | logical-before-physical invariant | Speculative module check |
| Boundary data becomes trusted once | Preserve semantic information | trust-boundary invariant | Boundary/evidence review |
| Pure core and explicit effect shell | Layer profile; effects | pure/effect and runtime-containment invariants | Effect-placement/interpreter review |
| Control plane outside hot paths | Codecs, effects, and hot paths | hot-path invariant | Snapshot/coordination/performance review |
| Architecture obligations reviewable | Proposal checklist | active charter decision | steward and worker charter gates |

### `scala-functional-design`

| Requirement | Guide | Stable context/decision | Review enforcement |
| --- | --- | --- | --- |
| Algebra before control flow | Algebra before control flow | algebra-first invariant | Algebra/law review |
| Semantic information remains in types | Preserve semantic information | semantic-preservation invariant | Trust/type-erasure review |
| Independent vs dependent validation | Validation and errors | evidence-validation invariant | Validation-stage/error-order review |
| Public APIs total | `docs/design-principles.md` validation/errors and proposal checklist | `.agent/project.md`; `INV-C7` and `INV-C10` in `.agent/invariants.md` | `.agent/review-policy.md`; `.agent/steward.md`; apply/review/remediation charter gates; `AGENTS.md` |
| Advanced Scala serves semantics | Advanced Scala and readable APIs | domain-readable ergonomics invariant | Concrete/generic ergonomics review |
| Standard composition replaces plumbing | Algebra and validation sections | mature-mechanism and algebra invariants | Mechanism/abstraction review |
| Effect polymorphism for genuine capabilities | Layer profile | pure/effect invariant | Port-admission review |
| Claims verified at their boundary | Verification matrix | configured-check invariants remain authoritative | Claim-proportional evidence matrix |
| Rigor readable at call sites | Advanced Scala section | domain-readable ergonomics invariant | API ergonomics review |

## Audit conclusion

The proposal portfolio has complete planning artifacts, consistent ownership
and adjacent boundaries, an acyclic combined dependency graph, a buildable
implementation order, and an explicit pre-release compatibility policy. No
planning conflict requires edits to a dependent proposal before Proposal 0
guidance is applied.
