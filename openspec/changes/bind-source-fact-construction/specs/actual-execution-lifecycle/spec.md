## ADDED Requirements

### Requirement: Lifecycle-bound source-fact construction
The execution lifecycle SHALL expose one immutable source-fact scope bound to one exact `ExecutionLifecycle`. The scope
SHALL construct order acceptance, order rejection, execution fill, fill correction, fill bust, effective cancellation,
reconciliation checkpoint, source-order completion, and source-order absence facts from the same operation-specific
inputs as their direct fact-owner constructors except for the captured lifecycle. Callers SHALL NOT need to repeat the
lifecycle or explicit position, base, or quote dimensions.

Every scoped operation SHALL return the same precise fact type or the same non-empty `SourceFactViolations` as direct
construction with that lifecycle. It SHALL preserve native and qualified identities, logical execution-order identity,
source target, authoritative ordering, instrument and grid provenance, modifier references, checkpoint and completeness
boundaries, validation staging, and deterministic violation order.

#### Scenario: Construct every source-fact form through one scope
- **WHEN** a caller binds one execution lifecycle and supplies valid event, logical-order, source-order, ordering, fill,
  modifier, checkpoint, completeness, lot, and price inputs required by the nine fact forms
- **THEN** every scoped result has the same precise fact type and observable fields as its corresponding direct
  fact-owner construction without repeated lifecycle or dimension arguments

#### Scenario: Accumulate the same invalid fact violations
- **WHEN** a scoped operation receives inputs with multiple independently detectable logical-order, target, instrument,
  grid, ordering, reference, checkpoint, completeness, or missing-value failures
- **THEN** it returns the same complete non-empty violations in the same deterministic order as direct construction
  supplied with the captured lifecycle

#### Scenario: Reject dimension-incompatible economics
- **WHEN** downstream Scala attempts to pass lots or prices from incompatible dimensions to scoped fill or correction
  construction
- **THEN** compilation fails without a cast, widening, runtime lookup, structural refinement, or replacement dimension
  evidence

#### Scenario: Keep concise calls usable from the completed artifact
- **WHEN** a completed-artifact Scala client constructs all nine source-fact forms from one bound lifecycle
- **THEN** it can use the public fact inputs directly without explicit dimension arguments or nested role projections

### Requirement: Bound source-fact construction preserves validation, purity, and ownership
A lifecycle-bound source-fact scope SHALL capture only its exact immutable execution lifecycle. It MUST NOT cache facts,
retain per-call inputs or results, mutate state, use thread-local or ambient state, acquire resources, perform effects,
or move lifecycle responsibility into a downstream owner. Reusing one scope sequentially or concurrently SHALL
construct independent immutable facts, and invocation order SHALL NOT change any result.

The direct checked constructors owned by each source-fact form SHALL remain available and SHALL remain the single
validation authority. Possession of a bound scope MUST NOT make independently supplied values trusted by association:
compile-time dimension relationships and runtime logical-order, target, instrument, grid, ordering, reference,
checkpoint, and completeness validation SHALL continue to apply.

The scope MUST NOT acquire command construction, dispatch evidence, lifecycle state transition, replay, observation,
effective-fill derivation, codec, application, or runtime responsibilities. The execution-lifecycle artifact SHALL
retain its existing pure dependency boundary.

#### Scenario: Reuse one scope independently
- **WHEN** one lifecycle-bound scope constructs multiple facts in different sequential, interleaved, or concurrent
  invocation orders
- **THEN** each call produces the same value or typed failure as direct construction, successful facts are independently
  constructed, and no call changes another result

#### Scenario: Reject a statically compatible foreign value
- **WHEN** a scoped operation receives a same-shaped value whose runtime instrument identity, grid identity, logical
  execution order, source target, modifier target, checkpoint target, or completeness target belongs elsewhere
- **THEN** canonical direct validation returns the corresponding typed violation rather than trusting scope association

#### Scenario: Retain direct checked construction
- **WHEN** a caller constructs a source fact through its fact owner with an explicit lifecycle
- **THEN** the existing checked API, precise result type, accumulated failure type, and observable validation behavior
  remain available

#### Scenario: Keep the scope in the pure execution owner
- **WHEN** the completed execution-lifecycle artifact is inspected or compiled in isolation
- **THEN** lifecycle source-fact construction introduces no scenario, fee, risk, codec, application, runtime,
  live-catalog, concrete-effect, stream, client, persistence, telemetry, or venue-SDK dependency or responsibility
