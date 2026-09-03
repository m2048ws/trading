# Architecture and Functional Design Charter

This guide explains the repository-wide design rules defined normatively by the
OpenSpec capabilities `repository-architecture` and `scala-functional-design`.
It governs new design and review; it does not claim that every proposed module
or API already exists. The [portfolio audit](architecture-charter-audit.md)
separates the current tree, transitional exceptions, and proposed target.

## Responsibility and dependency direction

Every production concept has one primary owning layer. That layer owns its
vocabulary, invariants, construction rules, and errors. A consumer depends on
the smallest lower-level concept it needs; lower layers do not depend on
higher-level policy, workflows, codecs, or runtime implementations.

The proposed responsibility order is:

```text
quantities
  -> reference data
  -> instrument economics
  -> order model -> execution scenario
  -> fee policy / risk
  -> application
  -> runtime

boundary codecs consume the internal values they encode,
but no domain layer depends on codecs or runtime.
```

Arrows mean "may be consumed by", not "is currently an SBT dependency". A
logical boundary becomes a physical module only when real code, a dependency or
publication boundary, or an independent verification need makes that split
useful. Empty diagram-shaped modules are forbidden.

The root is a non-published aggregate. The packaged adversarial boundary is
test-only and may consume production artifacts to verify public boundaries.

## Algebra before control flow

Before choosing flags, primitive containers, mutation, or procedural branching,
identify the domain structure:

- mutually exclusive states are sums;
- values that must coexist are products;
- predicates use refinements or smart constructors;
- intrinsically non-empty collections use a non-empty representation;
- associative combination, identity, order, traversal, and state transition are
  modeled when their laws are honest;
- independent computations compose applicatively, while dependent computations
  sequence from evidence produced by earlier stages.

Use the weakest abstraction that captures the complete law. Do not publish a
`Monoid` when only a `Semigroup` is lawful, and do not invent a type class when a
direct domain operation has no meaningful generic consumer. Algebraic structure
must remove invalid states, preserve information, enable useful composition, or
make a boundary explicit; sophistication alone is not a reason.

## Preserve semantic information

Dimension, grid, refinement, identity, provenance, validation, and endpoint
information established by an input stays in the resulting type. Normal domain
code must not unwrap values to `Rational`, `BigInt`, strings, tuples, booleans,
or untyped maps and reconstruct the same meaning later.

Primitive representations are appropriate only inside an owning abstraction,
at an explicit interoperability or serialization boundary, or behind a measured
optimization that leaves semantic behavior unchanged.

Boundary data follows one trust transition:

```text
wire / database / configuration / external identifiers
  -> parse
  -> resolve against one coherent immutable view
  -> validate and assemble
  -> trusted proof-carrying domain value
  -> pure domain and economic calculations
```

Successful validation returns the strongest useful value or evidence. Trusted
values retain that evidence and do not repeatedly consult a live registry.
Possessing a trusted value does not reveal construction authority, mutable
catalog access, or runtime provenance that its public contract does not grant.

### In-process trust and supported boundaries

The repository protects cooperative code from mistakes; it does not treat code
already executing in the JVM as an adversary. Constructor bytecode modifiers,
package access, reflection, method handles, unsafe bytecode, instrumentation,
and constructor-bypassing deserialization are therefore not security
boundaries. A future requirement to isolate mutually distrustful code must name
a real process, class-loader, module, sandbox, or service boundary and receive
its own design and threat model.

Within one process, statically callable owner-defined operations are preferred
to reflective construction or observation. Narrow companion or package access
may connect cooperating owners, while the documented public API remains made
of domain-named factories and transitions. Authority comes from semantic
checks: refined fields, checked smart constructors, coherent identity and
lineage, associated evidence, non-empty structure, and validated state
transitions. Merely hiding a constructor does not establish those facts.

External and least-trusted supported inputs remain fully checked. Wire records,
database rows, configuration, venue facts, erased Java values, and replayed
events do not become trusted because they entered cooperative code; their
owning parser, resolver, assembler, or transition must still validate them.
Tests should attack those supported ingress paths and semantic relationships,
not claim resistance to a deliberate same-JVM bypass.

## Layer-specific functional profile

| Layer | Required profile |
| --- | --- |
| Quantities | Pure, exact, type-indexed, algebraic, total where mathematically possible, and law-tested |
| Reference data | Immutable state transitions, canonical handles, coherent snapshots, and typed conflicts |
| Domain models | Closed ADTs, refinements, smart constructors, proof-carrying validation, and no infrastructure effects |
| Instrument economics | Pure typed transformations and explicit domain errors; no live catalog, codec, or runtime dependency |
| Application | Effect-polymorphic ports and workflows only for genuine environmental capabilities |
| Boundary codecs | Pure parsing/encoding where possible and checked reconstruction against an explicit immutable view |
| Runtime | Concrete effects, resources, concurrency, streams, transactions, clients, mutation, and telemetry |

Pure computations remain ordinary functions. `F[_]` is appropriate for a
capability whose execution genuinely varies, such as market data, persistence,
time, transactions, telemetry, or external communication. Durable commands and
events remain inspectable ADTs even when an effectful workflow produces or
consumes them.

Runtime mutation and coordination are permitted only behind an effect boundary
with pure immutable transition semantics where practical. Application ports do
not expose concrete `Ref`, fiber, queue, stream, client, or transaction
implementations.

## Application port admission and runtime semantics

Use the following encoding rule at the pure/effect boundary:

- durable commands, queries, events, and expected outcomes are explicit closed
  data owned by the layer that defines their meaning;
- semantics determined by immutable inputs are ordinary pure functions and
  state transitions;
- only operations that genuinely vary with coordinated live state, time,
  persistence, communication, or another environment become narrow final
  `F[_]` capabilities;
- concrete effects, resources, clients, concurrency, streaming loops, and
  operational decorators belong to runtime interpreters.

Application does not define a repository-wide environment, service locator,
capability registry, universal application error, free-program layer, or an
effect-wrapped facade over pure quantities, catalog semantics, instrument
assembly, valuation, P&L, or other domain calculations. A workflow receives
only the small product of capabilities that it actually uses. Expected absence,
conflict, rejection, and idempotent replay remain typed capability results;
unexpected infrastructure failure and cancellation use the documented effect
semantics without being recast as one universal business error.

A future port is admitted only after its own RFC Slice or amendment answers the
applicable questions below. This foundation does not deliver these interfaces:

| Candidate | Required semantic contract before admission | Runtime-owned mechanism |
| --- | --- | --- |
| Market data | observation identity, freshness, ordering, gaps, replay, absence, and subscription lifetime | feed client, reconnect, buffering, batching, and stream supervision |
| Trade persistence | durable command/fact identity, typed commit/read outcomes, idempotency, consistency, and retention | database client, schema, migration, session, and outbox wiring |
| Business time | named workflow need, wall-clock authority, timestamp meaning, and deterministic interpretation | live clock interpreter; timeout/delay scheduling remains monotonic runtime work |
| Order execution | explicit commands/events, account and venue identity, lifecycle, ordering, replay, and ambiguous-ack reconciliation | live venue, paper, simulation, backtest, and replay interpreters |
| Transactions | business-shaped atomic operation, or a rank-scoped composition with commit, rollback, isolation, retry, cancellation, and non-escape rules | connection/session acquisition and scoped transaction lifecycle |
| Telemetry and audit | distinction between best-effort operational observation and a guaranteed durable business fact | tracing/metrics decorators and exporters; durable audit uses an explicit persistence/event capability |

Fibers, queues, schedulers, callback registries, backpressure, merge policy,
parallelism, and FS2 or another stream representation stay in runtime wiring by
default. Runtime may batch and supervise ingress around the same domain-named
operation available to non-streaming callers. If ordering, replay,
backpressure, or subscription lifetime becomes observable application behavior,
its proposal must define that protocol directly; processing many values alone
does not justify a generic stream algebra or per-value coordinated lookup.

Wall-clock time is business data only when one explicit observation affects a
command, event, validity rule, or result. Timeouts, retry delays, deadlines, and
scheduling intervals use monotonic runtime facilities and do not become domain
timestamps. Pure code never reads a global system clock.

Prefer one business-shaped atomic capability operation for one indivisible
durable outcome. A context-free `F[A] => F[A]` wrapper cannot prove that its
effects share a transaction. Multi-operation atomic composition requires a
scoped program whose session-bound capabilities cannot escape and whose
proposal defines isolation, retry, cancellation, idempotency, and coordination
with external effects.

Tracing, metrics, and logs are runtime decorators and may be sampled, delayed,
duplicated, or lost according to their operational contract. If an observation
is a guaranteed audit fact, it is explicit durable data committed by a named
application capability; telemetry must never be allowed to masquerade as that
guarantee.

## Validation and errors

Validation is staged by dependency:

1. parse syntax and enforce boundary resource limits;
2. resolve external identities and versions against one immutable view;
3. accumulate independent structural violations in deterministic order;
4. sequence dependent checks from the normalized value or witness produced by
   the preceding stage;
5. enforce economic or policy invariants in their owning layer.

Failure collections that cannot be empty use an honest non-empty type. A later
check must not invent secondary errors when its prerequisite is absent. Errors
belong to the layer that can explain and remediate them; an adjacent boundary
may add role/path context without replacing the underlying meaning with strings
or one repository-wide error hierarchy.

Public mathematical and domain APIs are total for expected inputs. Expected
absence, invalidity, conflict, and failure appear in result types, not as
`null`, unchecked extraction, sentinel values, or ordinary exceptions.
Unavoidable partial operations, casts, and mutable mechanisms are narrowly
scoped, explicitly named or hidden, protected by a stated invariant, and
inaccessible as public construction authority.

## Advanced Scala and readable APIs

Opaque types, enums, refinements, phantom and path-dependent types, match types,
context parameters, type classes, and higher-kinded interfaces are warranted
when they make an invalid state unrepresentable, preserve downstream
information, encode a lawful reusable structure, or enforce an architectural
boundary.

Every such mechanism needs:

- a small domain-named public operation;
- supported concrete and generic examples where relevant;
- focused positive and negative coverage;
- laws or properties for claimed algebra;
- documentation of non-obvious inference or safety behavior;
- containment of unavoidable casts, mutation, and partial implementation
  details behind a checked invariant.

Contextual values carry stable authority or lawful behavior, not dynamic prices,
rates, or business data. Implicit conversions must not hide validation,
alignment, quantization, I/O, or another semantically significant transition.

The presentation rule is: mathematically sophisticated foundations,
domain-readable interfaces.

## Dependency admission and containment

Before building a general effect runtime, stream, standards parser, schema
validator, law harness, or benchmark harness, evaluate maintained mechanisms
that already satisfy the required contract. A selected dependency belongs in
the narrowest module and configuration that needs it.

Every dependency proposal records:

- the mechanism supplied and why it is needed;
- the owning module and production/test/plugin configuration;
- whether library types intentionally appear in public signatures;
- compatibility with the Scala and JDK baseline;
- why an already selected vocabulary does not satisfy the need.

The project retains ownership of domain meanings, trusted transitions, public
domain errors, durable schema semantics, and project-specific laws. Parser
objects, generic mapping models, validation containers, runtime references, and
similar implementation representations remain confined unless deliberately
made part of an owning public contract.

The current minimum build and runtime JDK is 25 while the project uses Scala
3.8.x. Changing that floor or selecting a dependency major that requires a higher
floor is an explicit compatibility decision. Independently released libraries
use independently named version coordinates even if their current version
strings match. Proposal 1 owns splitting the current shared Cats/Algebra
coordinate; this charter does not change `build.sbt`.

A second effect, JSON, refinement, derivation, or testing vocabulary requires a
distinct integration or semantic need. Convenience alone is insufficient.

## Codecs, effects, and hot paths

Durable records contain stable IDs and exact primitive data, not trusted
handles, path-dependent evidence, process lineage, clients, or object graphs.
Codecs own wire/database/packed representations and schema versions; owning
domain constructors and assemblers retain validation authority.

Control-plane identity, registration, configuration, and publication work stays
out of data-plane arithmetic, valuation, event processing, decoding, and replay.
Prefer immutable snapshots, resolved capabilities, batching, and caching over a
shared coordinated read for every value. If a live coordinated hot-path read is
unavoidable, the proposal documents its invariant, access pattern, measured
cost, and rejected snapshot/caching alternatives.

## Verification proportional to the claim

| Claim | Minimum evidence |
| --- | --- |
| Pure domain behavior | Focused examples and unit/property tests |
| Algebraic law | Property or discipline law tests |
| Refinement closure | Boundary examples plus closure properties |
| Type-level rejection or semantic authority | Packaged downstream negative fixture and nearby positive fixture |
| Cooperative in-process construction | Ordinary Scala/Java client plus source scan excluding reflective production access |
| External or erased input | Least-trusted supported-input tests at the owning checked boundary |
| Static/runtime coherence | Tests of both the static result and runtime identity/value |
| Multiple interpreters | Shared contract suite plus interpreter-specific tests |
| Concurrent transition | Atomicity/coherence tests appropriate to the interpreter |
| Algorithmic complexity | Deterministic operation/probe-count properties |
| Hot-path suitability | Representative JMH evidence when contention, latency, throughput, or allocation may matter |

Negative compiler fixtures require a valid independent prelude, failure at the
intended expression, relevant diagnostics, and rejection of compiler-internal
failures. A configured test, formatting, OpenSpec, packaged-boundary, or Git
check is a release gate.

## Proposal and review checklist

For every nontrivial change, record or explicitly mark not applicable:

1. primary owner and owned errors;
2. allowed and forbidden dependencies;
3. boundary and trusted representations;
4. validation stages and evidence retained;
5. expected-input totality and containment of unavoidable partiality or unsafe mechanisms;
6. algebraic model and laws;
7. pure versus effectful work and interpreter variation;
8. dependency mechanism, scope, public exposure, and platform compatibility;
9. codec/schema owner;
10. hot-path and coordination implications;
11. claim-proportional verification;
12. common-call-site ergonomics;
13. migration order and buildable intermediate state.

A deliberate exception is a design change. Apply and remediation work must stop
for design escalation instead of introducing an exception as incidental
cleanup.
