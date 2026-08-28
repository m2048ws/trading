## Context

The current repository has a strong exact-quantity kernel and a growing economics surface. The quantity model already demonstrates the value of algebraic design: dimensions compose, quantities preserve their indices, grid coordinates remain distinct from exact values, refinements encode predicates, and rates expose endpoint-oriented composition. Recent economics work similarly improved validation by accumulating independent violations, sequencing dependent checks, and carrying successful evidence into construction.

At the same time, current physical boundaries reflect the order in which the project grew rather than the intended final architecture. Runtime identity, stable grid/catalog concerns, instrument meaning, order policy, execution assumptions, fee policy, valuation, and risk are not yet separated consistently. Upcoming market-data loading, trade persistence, clocks, concurrency, streaming, tracing, metrics, transactions, and multiple live/simulated interpreters will add genuine effects. Without a common charter, those effects and policies could spread into otherwise pure calculations.

Existing settled invariants remain authoritative. In particular, this design preserves exact arithmetic, contextual grids, domain neutrality of the quantity foundation, minimal public proof authority, concrete ergonomics, static/runtime coherence, and the rule against speculative public abstractions. This change adds repository-wide design governance; it does not weaken or replace those invariants.

## Goals / Non-Goals

**Goals:**

- Establish one durable standard for architecture, Scala 3 design, functional programming, validation, effects, and review.
- Make algebraic discovery an explicit design activity rather than an accidental implementation outcome.
- Preserve proofs and semantic information after validation or resolution instead of reconstructing them in downstream code.
- Define a target responsibility graph that later proposals can implement without circular dependencies.
- Give application effects an intentional home before live infrastructure arrives.
- Make sophisticated foundations compatible with simple, domain-readable client APIs.
- Give reviewers objective questions and verification obligations instead of asking whether code merely "looks functional."
- Reuse mature infrastructure and functional libraries deliberately without allowing their representations to become
  accidental domain or compatibility contracts.
- Define the order and coherence gate for the broader architectural proposal portfolio.

**Non-Goals:**

- Move production code or create the target SBT modules in this change.
- Define concrete catalog, instrument-assembly, persistence, streaming, database, telemetry, or transaction APIs.
- Require every target responsibility to become a physical module immediately.
- Put `F[_]` into pure code or mandate tagless-final encodings for entities and calculations.
- Force every domain operation into a standard type class or expose mathematical implementation vocabulary in every public API.
- Eliminate all runtime mutation, synchronization, casts, or partial implementation details; the goal is to isolate and justify them.
- Preserve compatibility for unreleased APIs when a later approved proposal establishes a cleaner final design.
- Replace domain judgment with a mechanical style checklist.
- Mandate one library for every possible abstraction or prohibit small project-owned mechanisms when they are clearer.

## Decisions

### 1. Treat the charter as a normative architectural capability

The charter will have three representations with different purposes:

1. OpenSpec capabilities are the normative behavioral contract.
2. A detailed project design guide explains principles, examples, and review heuristics.
3. `AGENTS.md`, stable `.agent` context, and module guides contain concise operational rules and links to the detailed source.

Future proposals must demonstrate conformance to the charter or explicitly propose an amendment. An implementation worker cannot reinterpret a charter violation as incidental cleanup.

**Alternative considered: keep the guidance only in `AGENTS.md`.** Rejected because an agent prompt is too compressed and operational to explain architectural rationale, and changes to it are difficult to review as a lasting capability.

**Alternative considered: keep the charter as non-normative prose.** Rejected because the same responsibility mixing would remain reviewable only as taste. The two capability specs turn the principles into requirements and scenarios.

### 2. Use algebra-first modeling

"Algebraic" has two relevant meanings, and the project will deliberately use both.

Algebraic data modeling asks:

- Which states are mutually exclusive and therefore form a sum?
- Which values must exist together and therefore form a product?
- Which predicates deserve a refinement or smart constructor?
- Which collections are intrinsically non-empty?
- Which successful checks can produce reusable evidence?

Algebraic structure asks:

- What combines associatively?
- Is there an honest identity, inverse, order, bound, or scalar action?
- What transformations preserve structure?
- Which independent computations compose applicatively?
- Which dependent computations compose monadically?
- Which conversions or morphisms compose with compatible endpoints?
- Can state evolution be described as a pure transition over immutable state?

When a structure exists, the implementation uses the weakest abstraction that captures all required laws. A semigroup is preferred over a monoid when no lawful or authoritative empty value exists. Direct category-shaped composition is preferred over claiming a global category instance when identity construction needs runtime authority. An applicative validation is preferred for independent checks, while dependent evidence uses monadic sequencing.

The requirement is not to maximize the number of abstractions. An abstraction earns its place when it makes invalid states harder to express, removes special cases, enables useful composition, or exposes laws consumed by generic code.

The working maxim is:

> Look for algebra before control flow, and preserve discovered structure instead of erasing it into primitives.

**Alternative considered: prescribe a fixed catalogue of type classes.** Rejected because the appropriate structure follows from domain laws. For example, rate composition can be category-shaped without supporting a globally available identity for every endpoint.

**Alternative considered: use direct procedural implementations until duplication appears.** Rejected as a default because representation choices made early can erase invariants and make later algebraic recovery substantially harder. Direct code remains appropriate when no honest reusable law exists.

### 3. Apply a layer-specific functional-programming profile

The repository will not use one uniform level of abstraction everywhere.

| Layer | Functional profile |
| --- | --- |
| Quantities | Pure, exact, total where mathematically possible, type-indexed, algebraic, and law-tested |
| Reference data | Pure immutable state transitions, canonical handles, immutable snapshots, typed conflicts and resolution failures |
| Domain models | Closed ADTs, refinements, smart constructors, proof-carrying validation, no infrastructure effects |
| Instrument economics | Pure typed transformations and explicit domain errors; no registry, codecs, or runtime effects |
| Application | Effect-polymorphic ports and workflows for genuine environmental capabilities |
| Boundary codecs | Pure parsing/encoding where possible and checked reconstruction against an explicit immutable view |
| Runtime | Concrete effects, resources, fibers, queues, streams, transactions, mutable coordination, external clients, and telemetry |

This yields a functional core with a controlled effect shell without pretending that a live trading system has no state or concurrency.

**Alternative considered: use tagless-final interfaces throughout the repository.** Rejected because pure arithmetic and entities gain no meaning from an abstract effect. Tagless-final is reserved for application capabilities with genuinely variable execution.

**Alternative considered: use concrete effects throughout.** Rejected because domain semantics would become coupled to a runtime, deterministic testing would be harder, and pure calculations would inherit irrelevant cancellation and scheduling concerns.

### 4. Establish the target responsibility and dependency graph

The target production responsibilities are:

| Target module | Owns | May depend on |
| --- | --- | --- |
| `trading-quantities` | Rational arithmetic, dimensions, exact quantities, anonymous mathematical grids, refinements, algebra | External mathematical foundations only |
| `trading-reference-data` | Asset identity, stable grid identity/version, trusted identity-bearing grid handles, definitions, pure catalog state/transitions, snapshots, catalog errors | Quantities |
| `trading-instrument-economics` | Assembled instrument specification, roles, listing economics, payoff, lots, positions, prices, valuation, economic fee values, P&L | Quantities and immutable reference-data capabilities |
| `trading-order-model` | Order intent, time in force, trigger and peg instructions, visibility, liquidity constraints, position effect | Instrument economics |
| `trading-execution-scenario` | Trigger/peg evidence, matched slices, execution assumptions, scenario validation | Order model and instrument economics |
| `trading-fee-policy` | Venue/account schedules, tiers, maker/taker policy, rebates, policy selection | Instrument economics and validated execution information |
| `trading-risk` | Downside measures, sizing, candidate search, future portfolio constraints | Instrument economics, execution scenarios, and fee policy as required |
| `trading-application` | Effect-polymorphic ports, commands, events, and workflows | Reference data and required domain modules |
| `trading-boundary-codecs` | Wire/database/packed representations, schema versions, encoding, checked decoding, replay reconstruction | The internal values it encodes, but never runtime implementations |
| `trading-runtime` | Concrete live, backtest, database, network, telemetry, concurrency, streaming, and transaction interpreters | Application ports, boundary codecs, and required domain modules |

The root remains a non-published aggregate. The adversarial-boundary project remains test-only and consumes packaged public artifacts.

This table describes semantic ownership first. A physical module is created only when it contains a real coherent surface or enforces a useful dependency/publication/test boundary. Until then, the same direction is enforced through packages and API ownership.

The following dependency rules are load-bearing:

- quantities know nothing about assets or instruments;
- reference data know nothing about instruments or workflows;
- instruments know nothing about orders, execution policy, or risk search;
- domain modules know nothing about codecs or concrete effects;
- application ports know nothing about concrete interpreters;
- runtime may assemble the system but does not become the owner of domain semantics.

**Alternative considered: keep a broad `economics` module and use packages only.** Rejected as the final target because the existing surface already spans independently evolving policies and will soon acquire different dependencies. Packages remain a valid intermediate migration step.

**Alternative considered: create all target modules immediately.** Rejected because it conflicts with the existing rule against speculative subdivision. Later proposals create modules only together with the code and dependency boundaries they own.

### 5. Admit mature libraries for mechanisms without outsourcing domain meaning

Third-party dependencies are selected by responsibility, not by fashion or a repository-wide preference for maximum
abstraction. Before implementing a general parser, effect runtime, stream, law harness, schema validator, or benchmark
harness, a proposal evaluates maintained libraries that already provide the required mechanism. A selected dependency
is placed in the narrowest module and configuration that needs it.

The project continues to own:

- domain sums, products, refinements, invariants, and error vocabulary;
- quantity and economic laws whose semantics are specific to this system;
- trust-boundary transitions and evidence-bearing results;
- durable record versions, field meanings, and canonical wire contracts;
- application capability semantics even when a runtime library interprets them.

Library types appear in a public API only when that abstraction is deliberately part of the owning layer's contract.
For example, Cats Kernel algebra instances are a mathematical interoperability surface and `F[_]` is part of an
application port, while Cats validation containers, Jackson parser objects, Cats Effect `Ref`, and JSON-schema
validator values remain implementation or test details. Domain-named public errors and values do not become library
containers merely to save an adapter.

The initial coherent stack is:

- Algebra and Cats Kernel/Core for honest mathematical structures, traversal, and pure validation where used;
- Cats Effect for concrete runtime state, cancellation, resources, and concurrency;
- FS2 only after a stream capability specifies ordering, batching, backpressure, replay, and failure semantics;
- boundary-specific low-level parsers and validators confined to codec or test adapters rather than domain modules.

A second effect system, JSON codec stack, refinement framework, generic derivation system, or equivalent vocabulary
requires an explicit distinct integration or semantic need. Convenience alone is insufficient because parallel stacks
multiply adapters, laws, operational behavior, and public leakage risk.

Independently released libraries receive independently named version coordinates even when current version strings
happen to match. Dependency-major selection also follows an explicit platform baseline. This repository's Scala 3.8.4
baseline already requires JDK 17 for compilation and execution, so JDK 17 is the documented minimum build and runtime
JDK unless a later explicit compatibility proposal changes the Scala line or raises the platform floor.

**Alternative considered: implement every mechanism locally to keep the dependency list small.** Rejected because
effect runtimes, standards parsers, schema validation, law testing, and JVM benchmarking contain difficult generic
behavior that mature focused libraries already verify more broadly.

**Alternative considered: standardize on one broad ecosystem and expose its representations everywhere.** Rejected
because library consistency does not justify moving runtime effects, generic errors, parser ASTs, or automatic
derivation into layers whose domain contract does not require them.

### 6. Separate mathematical grids from stable grid identity

The quantity kernel's mathematical grid needs a quantum, a dimension, and a generative coordinate namespace. Stable external `GridId`, `GridVersion`, and catalog provenance are reference-data concerns.

Later boundary proposals will therefore distinguish conceptually:

```text
mathematical grid
    dimension + quantum + coordinate type

identity-bearing grid handle
    stable key/version + mathematical grid + catalog provenance
```

Quantization, exact embedding, same-quantum relationships, and mathematical grid operations remain in quantities. Packed encoding, stable reconstruction, listing rules, and persisted denomination identities consume the identity-bearing handle.

This separation does not weaken grid provenance. It makes provenance explicit at the layer that owns stable identity instead of requiring every mathematical grid to be catalog-addressable.

**Alternative considered: parameterize every mathematical grid by an arbitrary identity type.** Rejected because it spreads catalog concerns and an additional generic parameter across pure grid arithmetic. A wrapper composes identity with the mathematical grid only where needed.

**Alternative considered: leave stable identity inside every grid witness.** Rejected because anonymous mathematical grids and stable catalog records have different lifecycles and consumers.

### 7. Establish one-way trust boundaries

Boundary representations are intentionally less trusted than domain values:

```text
wire / database / configuration / external identifiers
                         |
                         v
              parse + resolve + validate
                         |
                         v
       canonical immutable capabilities and values
                         |
                         v
        instrument economics and domain workflows
```

Parsing owns syntactic validity. Reference-data resolution owns external identity and catalog membership. Instrument assembly owns cross-role and listing coherence. Domain validation owns domain alternatives. Economics owns valuation coherence. Errors remain in the layer that can explain them.

A successful boundary must return a value that retains the evidence downstream code needs. Trusted values do not repeatedly return to a registry to prove the same facts. This rule does not require public names beginning with `Resolved`; natural names such as `Asset`, `GridHandle`, `InstrumentSpec`, and `Instrument` are preferred when the trusted/untrusted distinction is already clear from their types and constructors.

For live reference data, immutable snapshots are the pure read boundary. A batch captures one snapshot and uses it consistently. Mutable publication or registration stays behind an application/runtime capability. Existing instruments retain the immutable meanings with which they were assembled.

**Alternative considered: give the live registry to P&L and other domain services.** Rejected because it repeats validation, introduces effects and contention into calculations, and allows reference-data updates to influence an in-progress economic result.

**Alternative considered: use primitive IDs throughout and validate opportunistically.** Rejected because it makes every consumer reconstruct identity and provenance proofs and permits untrusted values to travel too far into the domain.

### 8. Standardize validation as evidence-producing composition

Validation is divided by dependency, not by a universal preference for accumulation or fail-fast behavior.

Independent checks use applicative accumulation. Suitable concrete vocabulary includes `ValidatedNec` or equivalent composition, with deterministic conversion to the layer's public error collection.

Dependent checks use `Either`-shaped sequencing. A later check runs only when its prerequisite produced the value or evidence it consumes. The implementation must not synthesize misleading secondary violations merely to return more errors.

Successful validation returns a trusted value containing or closing over the evidence it established. Construction consumes this value rather than re-running validation. `NonEmptyChain`, `NonEmptyList`, or another honest non-empty representation is used when failure collections cannot be empty.

The stages remain distinct:

1. parse boundary syntax;
2. resolve external identities and versions;
3. accumulate independent structural violations;
4. sequence dependent evidence construction;
5. enforce economic or policy invariants in their owning layer.

**Alternative considered: accumulate every possible error.** Rejected because dependent checks can become meaningless or unsafe after a prerequisite fails.

**Alternative considered: fail fast everywhere.** Rejected because independent definition errors can be reported together without ambiguity and provide substantially better boundary ergonomics.

### 9. Preserve semantic values through calculations

Typed quantities, rates, refinements, grid coordinates, identities, and validated definitions are the domain representation, not ceremonial wrappers around raw data. Calculations compose their public operations and preserve endpoint or witness information.

Unwrapping to a raw `Rational`, `BigInt`, tuple, or string is appropriate only for:

- implementing the private representation of the owning abstraction;
- an explicit serialization/interoperability boundary;
- a measured optimization hidden behind unchanged semantic behavior.

It is not appropriate as the normal implementation strategy for downstream economics followed by reconstructing the same type-level proof.

The preferred public shape is sophisticated internally and direct externally, for example a named `andThen`, `applyRate`, `traverse`, or validation operation rather than callers manually transporting coefficients and evidence.

### 10. Use advanced Scala when it pays its semantic cost

Advanced Scala is expected where it protects real invariants. Candidate tools include opaque types, enums and sealed ADTs, private smart constructors, refinements, phantom indices, path-dependent members, match types, contextual parameters, type classes, extension methods, and higher-kinded application ports.

Before adding one, a design must answer at least one of:

1. Which invalid state becomes unrepresentable?
2. Which information required downstream is preserved?
3. Which honest laws and generic consumers are enabled?
4. Which architectural boundary becomes explicit?

It must also provide:

- a domain-readable public operation;
- focused positive and negative examples;
- laws or properties when a lawful abstraction is claimed;
- an explanation for non-obvious inference or safety;
- containment of unavoidable casts and unsafe details.

Contextual values are used for stable authority or lawful behavior, not for dynamic business data such as market prices or conversion rates whose presence should remain visible. Implicit conversions must not hide validation, dimensional alignment, quantization, I/O, or other semantically significant work.

The guiding presentation rule is:

> Mathematically sophisticated foundations; domain-readable interfaces.

**Alternative considered: prefer simpler primitive code even when it loses proofs.** Rejected because local syntactic simplicity can create system-wide validation and reconstruction complexity.

**Alternative considered: expose every underlying functional representation directly.** Rejected because domain-named facades can preserve the same laws with substantially better discoverability.

### 11. Use tagless-final only at application capability boundaries

The upcoming system has real reasons for effect polymorphism: loading market data, persisting trades, obtaining time, tracing, metrics, transactions, concurrency, streaming, and live/backtest/test execution.

Application ports may therefore use tagless-final interfaces when execution varies by interpreter. Workflows remain polymorphic in the required capabilities. Concrete Cats Effect and FS2 implementations belong to runtime modules.

Pure reference-data snapshots, instrument values, orders, scenarios, fee values, risk calculations, and P&L remain ordinary values and functions. They do not receive `F[_]` to make the code appear uniformly functional.

Durable commands and events use initial algebraic data types so they can be inspected, persisted, replayed, versioned, and tested. Effectful ports describe how an application performs or observes those operations. The two encodings serve different purposes and may coexist.

Common workflow transformers such as `EitherT`, `Kleisli`, or streaming composition are used when they remove real effect/error/environment plumbing. They are hidden behind domain-facing operations when their raw types would dominate the API.

### 12. Keep runtime coordination replaceable and hot reads pure

Functional programming does not remove the physical need to coordinate concurrent writes. It moves that requirement to an interpreter around a pure state-transition model.

The intended shape for stateful services is:

```text
command + immutable state
          |
          v
typed error or (new immutable state + result/event)

application capability
          |
          v
runtime interpreter using Ref, STM, actor/fiber ownership, database transaction,
or another suitable coordination mechanism
```

Readers capture immutable views where coherence and throughput benefit. In particular, catalog-backed decoding should resolve a batch against one snapshot rather than taking a coarse shared lock for every packed value. Runtime metrics must not turn otherwise independent reads into a serialized path.

The exact coordination mechanism remains a later interpreter decision. Its observable atomicity and consistency contract is specified before choosing it.

### 13. Make verification proportional to the claim

Different abstractions require different evidence:

| Claim | Minimum verification |
| --- | --- |
| Pure domain behavior | Focused examples and unit/property tests |
| Algebraic law | Property or discipline law tests |
| Refinement closure | Boundary examples plus closure properties |
| Type-level rejection | Packaged downstream negative fixture and nearby positive fixture |
| Runtime/static coherence | Tests exercising both type result and runtime identity/value |
| Multiple interpreters | Shared contract suite plus interpreter-specific tests |
| Concurrent transition | Atomicity/coherence tests appropriate to the interpreter |
| Algorithmic complexity | Deterministic operation/probe-count properties against the stated bound |
| Hot-path suitability | Representative JMH measurement when contention, throughput, latency, or allocation may matter |

Passing examples do not justify a law claim, and a negative compiler fixture is meaningful only when its prelude is valid and it fails for the intended reason.

### 14. Enforce the charter through normal repository mechanisms

Applying Proposal 0 will:

- create a detailed design-principles document as the human-readable source;
- add a concise normative summary and link to `AGENTS.md`;
- extend stable `.agent` project/invariant/decision context with accepted charter rules;
- extend review guidance and worker prompts with the architecture questions;
- require each module guide to state ownership plus allowed and forbidden dependencies;
- record dependency purpose, scope, platform compatibility, version coordinate, and public-type exposure;
- use SBT project dependencies to enforce physical boundaries as later modules are created;
- retain compiler-boundary, property, law, and adversarial tests for claims that need them.

The review checklist for later proposals is:

1. What layer owns the new concept and its errors?
2. Which dependencies are allowed and forbidden?
3. Does each external library implement a needed mechanism in the narrowest layer, and does any library type leak into
   the public contract without deliberate justification?
4. What arrives as boundary data, and what leaves as trusted data?
5. Which invariants or evidence are preserved in types?
6. Which algebraic data types or lawful structures exist?
7. Which validations are independent, and which are dependent?
8. What is pure, what is effectful, and why?
9. Does effect polymorphism correspond to a genuine variable interpreter?
10. Are casts, mutation, and partiality quarantined behind checked boundaries?
11. Which laws, compiler boundaries, interpreter contracts, complexity properties, or JMH measurements are required?
12. Could control-plane coordination enter a data-plane hot path?
13. Is the public API simpler than the machinery that makes it safe?

Mechanical checks enforce what can be enforced mechanically. Review remains responsible for semantic questions that a linter cannot decide.

**Alternative considered: introduce a large lint rule set immediately.** Rejected because most charter obligations are semantic, and premature custom linting could reward surface form rather than correct architecture. Later experience may identify narrow mechanical rules worth adding.

### 15. Complete and reconcile the proposal portfolio before applying it

Proposal 0 is the governing change. The planned dependent proposal sequence is:

1. quantity and reference-data boundary, including mathematical versus stable grid identity;
2. pure catalog state, canonical handles, immutable snapshots, and live catalog capability;
3. instrument definition resolution and assembly boundary;
4. pure typed instrument-economics surface;
5. order-model and execution-scenario separation;
6. fee-policy separation;
7. risk and sizing separation;
8. application/runtime admission rules and the first concrete live-catalog interpreter;
9. versioned boundary codecs and pure catalog replay/reconstruction, without inventing storage or ingress effects.

This order is normative for the initial migration. The repository's affected APIs are unreleased, so each proposal
updates callers directly and introduces no deprecated aliases, forwarding facades, or compatibility shims unless a
later explicit proposal identifies a real external consumer. Every intermediate change must remain buildable,
testable, reviewable, and revertible, but transitional boundaries are not release endpoints: Proposals 1 and 2 should
land consecutively, as should the aggregate-economics decomposition in Proposals 4 through 7. No artifact is published
or release declared until the complete sequence has landed and its final module graph, canonical specs, packaged
boundaries, and full validation matrix pass. The first durable compatibility commitment begins only with Proposal 9's
explicitly versioned record families; earlier in-memory forms and temporary packed values gain no persistence promise.

All proposals will be drafted before any apply worker is launched. A cross-proposal review will then verify:

- every concept has exactly one primary owner;
- dependency directions are compatible and acyclic;
- names and trust transitions agree across proposal boundaries;
- the catalog and effect models do not leak into pure economics;
- migrations do not require mutually inconsistent intermediate APIs;
- implementation order is explicit;
- no proposal silently implements another proposal's unresolved choice.

This process intentionally allows a later proposal to reveal a necessary amendment to Proposal 0 before implementation starts.

## Risks / Trade-offs

- **[Risk] The charter becomes a slogan rather than a constraint.** → The capability specs provide normative scenarios, and application updates review prompts, stable invariants, and proposal requirements.
- **[Risk] Strong separation produces excessive modules or adapter types.** → Logical boundaries precede physical modules; a module requires a coherent body of code or an enforceable dependency/publication/test boundary.
- **[Risk] Algebra-first review encourages performative abstraction.** → Every abstraction must identify honest laws, downstream information, invalid states prevented, or an architectural boundary; the weakest sufficient abstraction wins.
- **[Risk] Advanced types make call sites difficult.** → Public API ergonomics and downstream positive fixtures are explicit acceptance criteria, not optional polish.
- **[Risk] Pure/effect separation merely hides mutation without clarifying semantics.** → Stateful services retain a pure transition model and an explicit observable interpreter contract.
- **[Risk] Typed values increase allocation or inhibit a hot path.** → Representations may be optimized behind opaque or stable APIs, with representative measurement; semantic information is not discarded as an unmeasured optimization.
- **[Risk] A preferred ecosystem becomes an automatic dependency everywhere.** → Admit each artifact by a concrete
  mechanism and layer, keep library containers internal unless intentionally contractual, and require an explicit
  proposal for competing stacks or platform-baseline changes.
- **[Risk] The proposal portfolio delays implementation.** → The up-front cost is accepted because these changes alter foundational ownership. Coherence review is cheaper than unwinding cyclic modules or incompatible migrations after several proposals have been applied.
- **[Risk] Existing code temporarily violates the target charter.** → Later proposals own those migrations. Proposal 0 does not pretend that documenting the target has already moved the implementation.

## Migration Plan

1. Draft Proposal 0 and all linked architectural proposals without applying any of them.
2. Perform a cross-proposal coherence review and revise proposals until ownership, naming, dependency, trust, effect, and migration decisions agree.
3. Accept an explicit implementation order and compatibility policy for the pre-release repository.
4. Apply Proposal 0's documentation, stable-context, and review-governance tasks.
5. Apply each dependent proposal through the steward workflow with a fresh independent review and normal archive gate.
6. Create physical modules only when their corresponding migration supplies a coherent implementation surface.
7. After each migration, verify the current public API, module graph, canonical specs, and downstream boundary tests before beginning the next dependent implementation.

Before Proposal 0 is archived, rollback is simply removal or revision of the active change artifacts. After archival, material changes to the charter require a new explicit OpenSpec amendment rather than silent edits. No production rollback is required for Proposal 0 itself because it changes governance and documentation only.
