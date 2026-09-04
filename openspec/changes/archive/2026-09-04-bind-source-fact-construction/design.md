## Context

See `proposal.md` for motivation. The nine authoritative source-fact alternatives are already checked values under
`trading.execution`. Each companion exposes a curried `create(lifecycle)(...)` operation that returns its precise
`Either[SourceFactViolations, Fact[D, B, Q]]`; shared validation compares the supplied logical order, qualified event,
source order, authoritative ordering, instrument and grids, modifier reference, checkpoint, or completeness boundary
with the immutable lifecycle.

The lifecycle is therefore stable shared context, but current normalization call sites repeat it for every fact and
may need to restate its three dimensions. Source facts already depend on `ExecutionLifecycle`; lifecycle state,
transitions, replay, observation, and downstream runtime concerns consume facts and must not become construction
dependencies.

## Goals / Non-Goals

**Goals:**

- Bind all nine source-fact constructors to one typed execution lifecycle.
- Preserve every existing operation input, precise fact result, validation rule, failure accumulation order, and
  runtime identity check.
- Keep the fact companions and shared validator as the only construction implementation.
- Make one scope safely reusable under sequential, interleaved, and concurrent pure calls.
- Prove the concise dependent API and nearby invalid calls against the completed execution-lifecycle artifact.

**Non-Goals:**

- Remove, narrow, or deprecate the existing fact-owner `create` operations.
- Default or hide the supplied logical execution-order identity; retaining it is necessary to detect mismatches.
- Add a source-event builder, normalization session, mutable stream cursor, cache, ambient context, effect, or
  thread-local state.
- Bind commands, dispatch evidence, source-evidence state, replay, observation, effective fills, reconciliation
  derivation, codecs, application ports, or runtime interpreters.
- Change source-fact alternatives, identity or ordering authority, validation vocabulary, serialization behavior,
  dependency coordinates, or the JDK 25 minimum.

## Decisions

### Expose an owner-local scope from `SourceFact`

`SourceFact.forLifecycle(lifecycle)` will return a final immutable `SourceFact.LifecycleScope[D, B, Q]` whose only
captured value is the supplied `ExecutionLifecycle[D, B, Q]`. The scope will expose the domain-named operations
`accepted`, `rejected`, `fill`, `corrected`, `busted`, `cancellationEffective`, `reconciliationCheckpoint`,
`sourceOrderCompleted`, and `sourceOrderAbsent`.

`SourceFact` is the primary owner because the scope constructs the closed fact vocabulary and facts already depend on
the lifecycle. An instance method on `ExecutionLifecycle` was rejected because it would introduce the reverse
conceptual edge from lifecycle identity into fact construction. A top-level builder, extension method, ambient given,
or mutable normalization session would obscure ownership or introduce an unnecessary mechanism.

The scope retains the lifecycle's `D`, `B`, and `Q` parameters, so `fill` accepts `Lots[D]` and `Price[B, Q]`,
`corrected` accepts replacement values with those same dimensions, and every operation returns its exact fact type.
The factory and calls require no explicit method type arguments, nested instrument-role projections, casts, or
structural refinements.

### Omit only the captured lifecycle and delegate exactly once

Each scope operation will accept the same second-parameter-list arguments as the corresponding fact companion and
delegate directly to that companion's existing `create` operation with the captured lifecycle. In particular,
`executionOrderId` stays explicit: a scope is context, not authority to replace or silently correct independently
supplied provenance.

No validation, defaulting, normalization, or precomputation moves into scope creation. This retains the exact staging
and deterministic ordering implemented by `SourceFactValidation`, including common failures before fact-specific
missing, target, instrument, and grid failures. It also preserves direct construction, null/missing-value behavior,
and the same-shaped foreign-value checks. There is no widened fallback return, exception, unchecked extraction, or
production cast.

### Preserve purity and independent allocation

`LifecycleScope` will contain no fact, per-call value, collection, cursor, memo table, synchronization primitive,
resource, effect, clock, catalog, client, or thread-local state. Every invocation reaches a checked fact constructor
and allocates a fresh successful fact. Multiple calls through one scope are therefore observationally equal to their
direct counterparts and independent of call order or concurrency.

A focused `SourceFactScopeSuite` will compare all nine scoped operations with direct construction using valid inputs
and exact result ascriptions. It will also compare complete ordered failures across common validation, fill economics,
modifier references, checkpoint, and completeness cases; exercise a same-dimensional foreign instrument/grid and
foreign target/logical-order values; interleave distinct calls; and concurrently reuse one scope while checking that
successful instances are independently allocated.

### Verify the completed-artifact boundary

A positive packaged Scala fixture will create one lifecycle scope and construct all nine fact forms without explicit
dimension arguments or lifecycle role projections at the call sites. It will ascribe the exact result alternatives so
loss of `D`, `B`, or `Q` precision fails compilation.

An independently valid negative fixture will pass incompatible lots and prices from another lifecycle dimension to
`fill` and `corrected`; its prelude must compile and only the marked calls must fail with ordinary type mismatches. A
runtime test will separately use deliberately erased same-shaped foreign values to prove that scope association does
not bypass logical-order, qualified-target, instrument, or grid checks. The completed-JAR dependency checks will also
retain the absence of scenario, fee, risk, codec, application, runtime, concrete-effect, stream, persistence,
telemetry, and venue dependencies.

## Risks / Trade-offs

- [The facade becomes a second validation implementation] → Keep every method a typed one-line delegation and compare
  successes and complete failures with the corresponding companion operation.
- [Binding is mistaken for authority over independent provenance] → Keep logical order and qualified source values
  explicit and cover same-shaped foreign values with runtime rejection tests.
- [The scope widens economics dimensions] → Ascribe exact fact result types in focused and packaged-client tests and
  compile incompatible lot and price fixtures negatively.
- [Reusable syntax introduces caching or order dependence] → Use a final field-only scope and verify independent
  allocations, interleavings, and concurrent calls.
- [The facade pulls construction into the wrong concept or module] → Own it under `SourceFact`, retain direct fact
  owners, and rerun completed-artifact dependency and forbidden-import checks.
- [Nine delegates drift in names or argument order] → Keep a one-to-one operation table in focused tests and exercise
  every operation through the packaged fixture.

## Migration Plan

1. Add the lifecycle scope and nine direct typed delegates while retaining all existing fact companion entry points.
2. Add focused equivalence, invalidity, same-shaped foreign-value, reuse, ordering, and concurrency tests.
3. Add completed-artifact positive and negative compiler fixtures and retain dependency-boundary assertions.
4. Run reflection guards, formatting, focused checks, the clean JDK-25 repository test/JMH-compile matrix, and automated
   Task Group review. Roll back by reverting the dedicated Task Group commit; no data, schema, wire, dependency, or
   runtime migration is required.
