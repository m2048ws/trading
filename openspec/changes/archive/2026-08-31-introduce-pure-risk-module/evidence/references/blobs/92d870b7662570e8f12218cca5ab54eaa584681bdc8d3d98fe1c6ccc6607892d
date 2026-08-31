# Architecture Charter Portfolio Audit

This began as the Proposal 0 coherence and traceability audit performed on
2026-08-28. It now serves as a historical design map. Accepted RFCs, Corgi
planning packages, and the current source tree supersede its old proposal and
steward sequencing language when they differ.

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

Delivered slices have established the current dependent physical boundaries. This table describes the implemented SBT
projects and must not be confused with the remaining proposed target.

| State | SBT project | Artifact/directory | Production dependency |
| --- | --- | --- | --- |
| Current | root `trading` | unpublished aggregate | aggregates quantities, reference data, application, runtime, instrument economics, order model, execution scenario, fee policy, risk, adversarial boundary |
| Current | `quantities` | `trading-quantities` / `quantities/` | external mathematical libraries only |
| Current | `referenceData` | `trading-reference-data` / `reference-data/` | quantities |
| Current | `application` | `trading-application` / `application/` | reference data |
| Current | `runtime` | `trading-runtime` / `runtime/` | application and reference data |
| Current | `instrumentEconomics` | `trading-instrument-economics` / `instrument-economics/` | quantities and reference data |
| Current | `orderModel` | `trading-order-model` / `order-model/` | quantities and instrument economics |
| Current | `executionScenario` | `trading-execution-scenario` / `execution-scenario/` | instrument economics and order model |
| Current | `feePolicy` | `trading-fee-policy` / `fee-policy/` | instrument economics, order model, execution scenario; risk in tests only |
| Current | `risk` | `trading-risk` / `risk/` | quantities and instrument economics |
| Current test-only | `adversarialBoundary` | unpublished / `adversarial-boundary/` | all packaged production artifacts |
| Current benchmark-only | `benchmarks` | unpublished / `benchmarks/` | reference data, application, runtime, and risk; outside root aggregation |

The current graph is acyclic:

```text
quantities <- referenceData <- application <- runtime
quantities <- instrumentEconomics
referenceData <- instrumentEconomics
quantities <- orderModel
instrumentEconomics <- orderModel
instrumentEconomics <- executionScenario
orderModel <- executionScenario
instrumentEconomics <- feePolicy
orderModel <- feePolicy
executionScenario <- feePolicy
instrumentEconomics <- risk

all completed production artifacts -> adversarialBoundary (test-only)
```

The test-only boundary consumes each completed production artifact directly. The first live-catalog runtime
interpreter is concrete; boundary codecs remain future-owned.

Current dependency coordinates include Scala `3.8.4`, independent
`catsVersion = 2.13.0` and `algebraVersion = 2.13.0` coordinates,
`discipline-munit` `2.0.0`, MUnit `1.3.5`, ScalaCheck `1.20.0`, and
`munit-scalacheck` `1.3.0`. Runtime-scoped Cats Effect is `3.7.0` and MUnit Cats Effect is `2.2.0`. The minimum
build/runtime JDK is 25.

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

The historical proposal ordering has been superseded by the accepted
architecture-portfolio RFC and GitHub dependency graph. Application/runtime is
independent; order/scenarios and risk wait for instrument economics; fee policy
and codecs then wait for order/scenarios. Each admitted change uses the Corgi
Run Contract gates for Apply, Verify, explicit Human Review, Human QA when
applicable, and Archive.

The important intermediate constraints are:

- Proposal 2 has replaced Proposal 1's temporary construction bridge with pure state/snapshots and the application
  port; Proposal 8 supplies its first runtime interpreter.
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
| Quantity-owned packing has been removed, leaving a deliberate durable-codec gap | Stable records require snapshots and an explicit schema owner | Proposal 9 adds versioned codecs |
| The boundary-codec target module does not exist | Its physical boundary requires a real implementation and dependency body | Proposal 9 |

These exceptions describe current implementation facts, not accepted permanent
architecture. No proposed module or API is presented as available today.

## Requirement-to-artifact trace

### `repository-architecture`

| Requirement | Guide | Stable context/decision | Review enforcement |
| --- | --- | --- | --- |
| Cohesive ownership and directed dependencies | Responsibility and dependency direction | canonical architecture spec, accepted RFCs | Charter ownership/dependency questions |
| Target responsibility layers | Responsibility and dependency direction; layer profile | architecture-portfolio RFC | Current/proposed state and scope review |
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
| Public APIs total | `docs/design-principles.md` validation/errors and proposal checklist | canonical Scala functional-design spec | Corgi Task Group review, Verify, and Human Review gates |
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

## 2026-08-30 S-01 delivery refresh

This refresh supersedes the historical current-state statements above. RFC-0002 Slice
`S-01-application-runtime-foundation` now physically adds `trading-application` and `trading-runtime`: application owns
the narrow runtime-neutral `LiveCatalog[F]` port, runtime owns its Cats Effect in-memory interpreter, the completed-JAR
adversarial boundary consumes both artifacts, and the non-published JMH project measures snapshot and publication
paths. The resulting graph remains acyclic: `quantities <- referenceData <- application <- runtime`, while
instrument economics depends on quantities and reference data and downstream economics depends one-way on instrument
economics. Adversarial and benchmark projects remain non-production consumers.

S-01 is the implemented runtime and future-port admission foundation. It intentionally does not claim delivery of
market-data, persistence, business-time, execution, transaction, telemetry, or codec ports; versioned codecs remain
owned by RFC-0002 Slice S-05. Runtime-scoped Cats Effect is `3.7.0`, and the minimum build/runtime JDK is 25.

## 2026-08-30 S-02 repair integration

The S-02 repair commit integrates `main` at `05a8af0ff836b846247c082901ac3baea3d0c169`, retaining S-01's application,
runtime, JDK 25, dependency, workflow-retirement, and archived-delivery changes while retaining S-02's physical order
model, execution-scenario, and downstream fee/risk migration. Conflicts were resolved by responsibility owner: current
`main` owns application/runtime and repository infrastructure; S-02 owns order/scenario sources, completed-JAR
compiler fixtures, and the transitional fee/risk consumers.

Scala package-qualified constructor privacy is not a JVM access boundary. Invariant-bearing order intent,
activation/peg evidence, liquidity slices, assumptions, checked scenarios, and round trips therefore use JVM-private
constructors; their owning companions alone cache privileged construction access after the existing typed validation
paths succeed through a private JDK method handle. The completed order/scenario JARs are exercised by same-package
Scala and Java negative fixtures, which
also confirm that removed case-class `copy`/`apply` paths cannot recreate those values. Domain-readable checked factory
calls and the associated evidence/resolution relationships remain unchanged.

## 2026-08-31 S-03 delivery refresh

RFC-0002 Slice `S-03-pure-risk` physically adds `trading-risk`, moves the unchanged downstream fee-policy implementation
into `trading-fee-policy`, and retires the broad `trading-economics` aggregate after deleting its obsolete policy-owned
risk wrapper. Risk depends only on quantities and instrument economics; fee policy depends on instrument economics,
order model, and execution scenario, with risk present only on its test integration classpath. The later S-04 Slice
still owns semantic redesign of fee policy rather than this ownership-only move.

Risk owns exact refined downside, checked compact monotone loss models, logarithmic boundary-certified primary sizing,
and a separately named linear exhaustive fallback with typed located failures. Scenario and fee-policy construction,
current positions, account/portfolio offsets, margin, liquidation, funding, market acquisition, concurrency,
persistence, tracing, and audit envelopes remain outside the risk capability. The production graph stays acyclic and
the minimum build/runtime JDK remains 25.
