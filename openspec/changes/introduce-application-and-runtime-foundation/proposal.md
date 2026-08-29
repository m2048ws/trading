## Why

The preceding proposals deliberately make quantities, reference data, economics, orders, scenarios, fees, and risk
pure, but the first genuinely live capability—catalog publication—still needs a concrete home and execution model.
Establishing that boundary now lets future market data, trade persistence, time, execution, concurrency, telemetry, and
transactions grow around the pure domain without pushing effects or interpreter assumptions back into it.

## What Changes

- Establish Proposal 2's `trading-application` artifact as the home for effect-polymorphic workflows and the smallest
  genuine external-capability interfaces they require; initially it owns only the specified `LiveCatalog[F]` port.
- Add a `trading-runtime` artifact for concrete effects, resources, concurrent state, external clients, streaming loops,
  telemetry decorators, and capability interpreters.
- Supply a Cats Effect in-memory live-catalog interpreter whose atomic state transition is exactly the pure reference-
  catalog model and whose lifecycle creates one stable catalog lineage.
- Use MUnit Cats Effect and Cats Effect TestKit in runtime test scope for effect-returning suites and deterministic
  cancellation/time control, while retaining real multi-threaded runtime tests for races and contention.
- Extend Proposal 2's reusable capability contract testing so every future live, simulation, backtest, or deterministic
  interpreter is checked against the same observable semantics.
- Require application workflows to depend on minimal explicit capabilities rather than a global environment, service
  locator, concrete client, or universal application algebra.
- Define admission rules for future market-data, trade-persistence, time, and order-execution ports: their durable
  commands/events remain inspectable algebraic data, their expected domain outcomes remain typed, and their interpreter-
  dependent execution remains in `F`.
- Keep concurrency, fibers, queues, backpressure, resource acquisition, and stream libraries in runtime wiring unless a
  later proposal demonstrates that streaming itself is part of a capability's observable contract.
- Keep tracing and metrics as runtime decorators by default, distinct from guaranteed business audit/persistence
  effects.
- Require transactional guarantees to be expressed at a domain-named capability boundary or by a properly scoped
  transaction program; do not introduce a context-free `F[A] => F[A]` transaction wrapper.
- Do not create speculative market-data, trade-store, clock, or execution APIs before their command/query/event and
  consistency semantics are proposed.

## Capabilities

### New Capabilities

- `application-runtime-foundation`: Defines application-port admission, workflow composition, runtime interpretation,
  resource/concurrency placement, transaction scoping, observability, and interpreter conformance.

### Modified Capabilities

- `live-catalog-capability`: Adds the first effect-scoped concrete interpreter and its required atomicity, cancellation,
  lineage, snapshot, and contract-test behavior without changing the pure catalog semantics.

## Impact

- Proposal 2's `application` project (`trading-application`, package `trading.application`) remains dependent only on
  reference data plus minimal functional composition.
- New SBT project/directory: `runtime` (`trading-runtime`, package `trading.runtime`), depending on application/reference
  data and an approved Cats Effect 3 runtime.
- Runtime test dependencies add `munit-cats-effect-3` and the Cats Effect-aligned `cats-effect-testkit`; neither appears
  in application or production APIs.
- Proposal 2's non-published JMH project gains benchmark-only runtime/application dependencies for snapshot capture,
  commit, and pure snapshot-lookup measurements rather than adding a second benchmark harness.
- Root aggregation, packaged-artifact checks, adversarial dependency checks, and the validation matrix gain both
  artifacts.
- Existing pure artifacts gain no effect-runtime, stream, client, telemetry, clock, or persistence dependency.
- Proposal 2's `LiveCatalog[F]` API remains the application-facing contract; this change provides its concrete runtime
  realization and conformance kit.
- Future boundary-codec/ingress work may consume application capabilities and runtime resources, but no production
  market-data, trade-persistence, or venue protocol is invented by this proposal.
