## ADDED Requirements

### Requirement: Instrument-bound standard-order construction
The order model SHALL expose one immutable scope created from one exact assembled `Instrument`. The scope SHALL provide market, limit, stop-market, and stop-limit methods accepting the same operation-specific arguments and defaults as the corresponding direct constructors except for the captured instrument. Callers SHALL NOT need to repeat the instrument, explicit position, base, or quote dimension arguments, nested role projections, casts, structural refinements, or local dimension aliases.

Each scoped method SHALL return exactly the corresponding activation- and execution-refined `Order.Aux` type. It SHALL use the same canonical intent and aggregate construction implementation as the direct operation and preserve instrument identity, signed position change, order instructions, defaults, validation staging, the full non-empty `OrderViolations`, and deterministic violation order.

#### Scenario: Construct standard orders through one scope
- **WHEN** a caller binds one instrument and invokes market, limit, stop-market, and stop-limit with compatible lots, prices, triggers, visibility, and other current operation inputs
- **THEN** each result has the same observable order fields or typed failure as the corresponding direct constructor and retains its precise activation and execution type without repeated instrument or dimension arguments

#### Scenario: Preserve defaults and explicit options
- **WHEN** a caller uses either default or explicit time-in-force, liquidity, position-effect, and visibility arguments on a scoped constructor
- **THEN** the resulting instruction has exactly the values and validation behavior of the characterized direct constructor

#### Scenario: Accumulate the same invalid order violations
- **WHEN** scoped construction receives an invalid combination with multiple independently detectable violations
- **THEN** it returns the same complete non-empty violations in the same deterministic order as canonical direct construction

#### Scenario: Reject dimension-incompatible inputs
- **WHEN** downstream Scala attempts to pass incompatible lots, prices, triggers, pegged pricing, or visibility to scoped or generic order construction
- **THEN** compilation fails without a cast, widening, runtime lookup, or replacement dimension evidence

### Requirement: Bound order construction preserves purity and independent validation
An instrument-bound order scope SHALL capture only its exact immutable instrument. It MUST NOT cache orders, retain per-call inputs or results, mutate state, use thread-local or ambient state, acquire resources, perform effects, or move order ownership onto `Instrument`. Reusing a scope sequentially or concurrently SHALL construct independent immutable orders, and invocation order SHALL NOT affect any result.

Generic accumulating and fail-fast order construction SHALL remain available with an explicit instrument and independently supplied intent, activation, and execution. Scope possession MUST NOT mark independently supplied components as valid: canonical compile-time relationships and runtime instrument-identity checks SHALL continue to apply.

#### Scenario: Reuse one scope independently
- **WHEN** one bound scope constructs multiple orders in different sequential or concurrent invocation orders
- **THEN** each call produces the same observable order fields or typed failure as its direct operation, successful results are independently constructed, and no call changes another result

#### Scenario: Validate independently supplied generic components
- **WHEN** a caller uses generic checked construction with independently assembled same-shaped or foreign components
- **THEN** canonical validation checks those components rather than trusting association with any previously created scope

#### Scenario: Keep the scope in the pure order owner
- **WHEN** the completed order-model artifact is inspected or compiled in isolation
- **THEN** `Order` owns the scope, `Instrument` owns no order operation, and no execution-scenario, lifecycle, fee, risk, codec, application, runtime, live-catalog, effect, or concurrency dependency is present
