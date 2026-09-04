## Context

See `proposal.md` for motivation. `Order` currently owns four standard convenience constructors—`market`, `limit`, `stopMarket`, and `stopLimit`—that each receive an exact instrument in a first parameter list and compose `OrderIntent.create` with the canonical accumulating `Order.create` boundary. Their result types retain the instrument's position, base, and quote dimensions together with precise activation and execution subtypes. Repeated callers must nevertheless pass the same instrument on every call.

RFC-0008/S-01 is already integrated, so one captured instrument directly exposes `PositionD`, `BaseD`, and `QuoteD` as exact aliases of its retained role projections. This Slice consumes those aliases but does not move order behavior into instrument economics.

## Goals / Non-Goals

**Goals:**

- Bind all four standard order conveniences to one exact instrument value.
- Preserve every existing argument, default, result refinement, identity check, validation failure, and error order.
- Keep one canonical implementation for intent and aggregate order validation.
- Prove that a scope is immutable, reusable, deterministic, and safe for concurrent pure use.
- Preserve compile-time rejection of inputs with incompatible dimensions and runtime checking of independently supplied generic components.

**Non-Goals:**

- Add a general order builder, mutable session, cache, ambient context, effect, or thread-local state.
- Add scope-owned generic `create` or `createFirst`; those operations intentionally accept independently supplied components and an explicit instrument.
- Change order kinds, activation, execution, pricing, visibility, position-effect, or error vocabulary.
- Move order construction onto `Instrument`, weaken ordinary runtime `InstrumentId` checks, or promise Java source or binary compatibility.

## Decisions

### Expose one owner-local scope from `Order`

`Order.forInstrument(instrument)` will return a final immutable `Order.InstrumentScope` whose only captured value is that exact stable instrument. The scope's `market`, `limit`, `stopMarket`, and `stopLimit` methods omit only the instrument parameter. Their parameters use the captured instrument's `Lots`, `Price`, `PositionD`, `BaseD`, and `QuoteD` relationships, and their result types retain the same exact `Order.Aux` activation and execution refinements as the existing companion calls, including the singleton type of a supplied stop trigger.

The scope belongs to the `Order` companion because order construction is the primary responsibility; `Instrument` gains no downstream method or dependency. A final class with one immutable field is preferred to ambient givens, extensions, structural refinements, or a mutable builder because it makes the captured context explicit and reusable without inventing authority or state.

### Delegate to the characterized checked constructors

Each scoped convenience delegates to its corresponding existing companion operation. This preserves one implementation of intent creation, default selection, identity validation, dependent validation, deterministic accumulation, and construction. Existing companion calls remain available for source compatibility. `Order.create` and `Order.createFirst` remain unchanged and explicit because their purpose is to validate independently assembled intent, activation, and execution values rather than infer trust from a scope.

Scope creation is total: the captured value is already an assembled instrument. No validation moves into scope creation, and no input is cached, normalized, or precomputed. Expected invalidity continues to appear in `Either[OrderViolations, ...]`; no exception, unchecked extraction, cast, widening, or replacement dimension evidence is introduced.

### Preserve purity, independence, and invocation-order invariance

The scope retains no order, mutable collection, memo table, synchronization primitive, effect, resource, clock, catalog, client, or thread-local value. Every invocation constructs a fresh immutable order through the canonical functions. Therefore repeated and concurrent calls are observationally identical to direct calls, independently allocated on success, and invariant under invocation order.

Focused tests will compare scoped and direct results for all four constructors, defaults and representative explicit options, and invalid orders with multiple independently detectable violations. Reuse tests will interleave and concurrently evaluate calls against one scope, compare results independent of order, and establish that successful orders are distinct immutable values rather than cached instances.

### Verify the public dependent boundary from the completed artifact

A positive packaged Scala fixture will create one scope and construct market and priced orders without explicit dimension type arguments, role projections, casts, or duplicated local dimension aliases. It will ascribe the exact activation and execution-refined `Order.Aux` results through the instrument's direct aliases.

An independently compilable negative fixture will attempt to pass lots, prices, triggers, pegged pricing, and iceberg visibility with incompatible dimensions into scoped or generic construction. The compiler boundary will require the intended type mismatch diagnostics and reject compiler-internal failures. A focused runtime test will also show that same-shaped but foreign independently supplied values remain subject to canonical `InstrumentId` validation. Existing artifact-dependency tests continue to exclude execution scenarios, lifecycle, fees, risk, codecs, application, runtime, and concrete effects.

## Risks / Trade-offs

- [The captured type widens from the exact instrument and loses dependent precision] → Return a scope parameterized by the argument's singleton type and compile exact `Order.Aux` ascriptions from the packaged artifact.
- [Delegation changes defaults or refined result shapes] → Compare every scoped constructor with the characterized direct operation under default and representative explicit arguments.
- [A scope is mistaken for validated authority over arbitrary components] → Keep generic construction explicit and retain runtime foreign-component tests as well as cross-dimension compiler rejection.
- [Reusable syntax accidentally introduces caching or order dependence] → Keep the scope field-only and final; test fresh successful identities, interleaved calls, and concurrent equality.
- [The concise API crosses an ownership boundary] → Define it only under `Order` and retain completed-artifact dependency and forbidden-import checks.

## Migration Plan

1. Add the instrument scope and four thin dependent delegates while retaining all existing companion entry points.
2. Add focused equivalence, invalidity, reuse, concurrency, and exact-refinement tests.
3. Add completed-artifact positive and negative compiler fixtures and retain module-boundary checks.
4. Run focused and full JDK-25 repository verification and the automated Task Group review. Roll back by reverting the dedicated Task Group commit; no data, catalog, schema, wire, or runtime migration is required.
