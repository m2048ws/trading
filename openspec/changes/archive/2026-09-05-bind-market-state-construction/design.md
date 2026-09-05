## Context

This design implements only RFC-0008/S-06 (AC-022–AC-025), from accepted commit
`bf457296faa7d29aec0840a7e8f48d41ed0f7491`. The isolated planning HEAD is
`54dc75521ed2d22d59bc42b95e0e5773fb1061a5`, which includes S-01 through S-05.
See `proposal.md` for motivation and `specs/instrument-economics/spec.md` for the behavior contract.

`instrument-economics/.../Market.scala` currently defines eight short overloads forwarding an empty vector to eight
full overloads. Scalar anchor entry points construct typed rates and forward to rate operations; rate/settled
operations share the private checked constructor. That constructor accumulates identity, anchor validity, dependent
anchor coherence, and additional-conversion violations in a deliberate order, then constructs retained typed
conversions. `MarketState` and `SettlementConversion` are immutable reference-valued classes, not structural products.

`Instrument` already exposes `BaseD`, `QuoteD`, `SettleD`, `Price`, and `MarketState`. Earlier RFC Slices use a
final owner-local `InstrumentScope[I <: Instrument]` plus a factory returning
`InstrumentScope[instrument.type]`. The existing economics compiler suite already supplies completed core-only JARs,
independently compilable negative preludes, compiler-internal diagnostic rejection, and runnable positive fixtures.

## Goals / Non-Goals

Deliver the four added requirements of the existing instrument-economics capability in one buildable Task Group.
Keep direct operations canonical, bind exactly one stable instrument, and retain all existing mathematical and error
semantics. Prices, scalar anchors, typed rates, and additional conversions remain explicit invocation inputs.

No new market math, valuation, price refinement, instrument assembly, conversion owner, universal facade, effect
interface, dependency, runtime service, or module is introduced. The other five Slices are already delivered.
No new support for hostile in-process fabrication, ordinary Java domain callers, binary compatibility, or null inputs
is promised. Existing operation-level null/exception timing is characterized where relevant and is not intentionally
changed by binding; legitimate invalid domain inputs keep their existing typed failures.

## Decisions

### D1: Keep one direct implementation per mode (AC-022)

Remove only the no-additional overload of each mode. Add `= Vector.empty` to the full operation's existing
`additional: Vector[SettlementConversion[instrument.SettleD]]` argument, preserving its name and parameter lists.
The direct definitions and their checked call graph remain the sole owners of validation and calculation.

| Direct and scoped operation | Required input after the instrument is bound | Existing derivation |
| --- | --- | --- |
| `quoteSettled` | price | quote identity, base from price |
| `baseSettled` | price | base identity, quote from reciprocal price |
| `fromQuoteAnchor` | price, rational quote anchor | typed quote rate, then derive base |
| `fromBaseAnchor` | price, rational base anchor | typed base rate, then derive quote |
| `fromAnchors` | price, rational base and quote anchors | typed rates, then check coherence |
| `fromQuoteRate` | price, quote-to-settle rate | derive base through typed composition |
| `fromBaseRate` | price, base-to-settle rate | derive quote through reciprocal composition |
| `fromRates` | price, both typed rates | check supplied coherence |

Each row also accepts explicit additional conversions with an empty default, direct and scoped.
Do not add duplicate defaults on retained overloads or implement typed operations through scalar extraction.

Alternatives: keeping pairs leaves AC-022 unmet; moving the checked kernel into the scope makes independent direct calls
depend unnecessarily on scope allocation; a mode enum or generic facade obscures the endpoint-specific signatures.
The accepted RFC permits removal of old JVM descriptors. Ordinary positional and named Scala calls with omitted,
explicit-empty, and non-empty conversions must continue compiling from completed artifacts.

### D2: Preserve the captured singleton in a MarketState-owned scope (AC-023)

Add a final `MarketState.InstrumentScope[I <: Instrument]` with the sole stored context
`val instrument: I`, created by
`MarketState.forInstrument[I <: Instrument](instrument: I): InstrumentScope[instrument.type]`.
Its constructor stores the reference only; it does not dereference it for validation or compute a market value.

Use exact aliases:
`BaseD = instrument.BaseD`, `QuoteD = instrument.QuoteD`, `SettleD = instrument.SettleD`,
`Price = instrument.Price`, `State = instrument.MarketState`,
`BaseRate = Rate[BaseD, SettleD]`, `QuoteRate = Rate[QuoteD, SettleD]`,
`Conversion = SettlementConversion[SettleD]`, and `Violations = MarketStateViolations`.
These aliases introduce no wrapper or new endpoint evidence.

Each of the eight methods is one direct forwarding call with the corresponding required inputs, an optional
`additional: Vector[Conversion] = Vector.empty`, and result `Either[Violations, State]`.
For example, `val markets = MarketState.forInstrument(instrument)` permits
`markets.fromQuoteRate(price, quoteRate)` and
`markets.fromQuoteRate(price, quoteRate, additional = Vector(conversion))`.
A downstream caller can assign either result to `Either[MarketStateViolations, instrument.MarketState]`.

Do not use casts, structural wrappers, a new type class, hidden contextual rates, memoization, or extra captured
values to repair inference. A non-generic widened scope would lose the relationship needed by existing prices and is
rejected. A method on `Instrument` would violate the accepted boundary.

### D3: Preserve checked stages and semantic equality (AC-024)

Retain the current checked implementation and the existing stage order:

1. Mode-specific prerequisites and typed rate derivation keep their existing behavior.
2. Price identity and each additional conversion's instrument identity accumulate first.
3. Base/quote numeric and same-asset identity validity accumulate next.
4. Anchor coherence is checked only when anchor validity succeeds.
5. Additional conversions retain input-index locations and their target reconciliation, source lineage, numeric,
   target identity, and duplicate-source checks in the current order.
6. Successful construction retains checked base/quote/settlement conversions, generated-source deduplication,
   additional ordering, and existing immutable lookup behavior.

The price smart constructor and assembler continue owning positivity, grid membership, and assembly evidence;
this change neither revalidates every grid in the scope nor promises a new aggregate grid check.
Runtime identity remains checked for supported values with compatible types. Foreign-value test setup must use
existing checked construction or already documented field-valid types, not reflection or unchecked casts.

First establish baseline characterization against the current direct operations before editing production bodies.
Use explicit expected rates, values, and ordered violation vectors as independent oracles. After consolidation and
binding, run those same cases against every appropriate path. Compare successful states by instrument/settlement
identity, retained price and ticks, both rate coefficients and endpoints, conversion-source order, additional
conversion fields, and conversion/valuation observations. Do not change public equality to make tests pass.
Compare failures by full `.violations` vectors, including paths, source/target IDs, and original coefficients;
verify `MarketState.firstError` remains the accumulated head.

Properties range over positive grid-valid prices and exact positive rational anchors for all applicable settlement
relationships, proving scalar/rate/direct/scoped agreement and the conversion equation. Include coherent non-empty
additional conversions and deterministic invalid/duplicate cases. Exercise null behavior only as baseline preservation
at the existing operation boundary, without adding a new expected-input contract.

### D4: Verify Scala ergonomics and the pure owner boundary (AC-025)

Extend `adversarial-boundary/src/test/scala/external/EconomicsCompilerBoundarySuite.scala` using its
`coreCompilationClasspath`, `compileCore`, and prelude validation. Add planned fixtures under
`adversarial-boundary/src/test/resources/economics-core-compiler/`:
`MarketStateScopeClient.scala` and `MarketStateScopeMismatch.scala`.
Positive coverage compiles and runs concrete assembled instruments and a generic helper, verifies exact result
assignments, all direct/defaulted argument forms, and scope reuse across all eight modes.
Call sites must contain no explicit dimension type arguments or nested role-dimension projections,
unchecked casts, structural refinements, or duplicate local D/B/Q/S aliases.

Each negative expression independently rejects a wrong price shape, reversed base or quote rate, foreign rate
endpoint, or wrong settlement conversion. Include nearby valid calls; the prelude compiles alone and assertions
require the intended type diagnostics, with no compiler-internal failures.
Use the restricted completed economics JAR classpath rather than the aggregate classpath; extend existing classpath
and package checks for order, execution, scenario, fee policy, risk, codecs, application, and runtime.

`SettlementConversion.exact` and `fromRate` remain independently available. They are not forwarded by the scope.
The production `Instrument` API gains no construction method. No library mechanism is needed beyond current Scala,
MUnit/ScalaCheck tests, and the existing compiler/JMH harnesses.

### D5: Purity, allocation, laws, and verification cost

The scope is a one-field immutable product of established context, not a new algebra or interpretation.
Typed rate composition, reciprocal handling, non-empty accumulated errors, and deterministic traversal semantics stay
with existing owners. There is no new Monoid or effect law to claim; equivalence, coherence, independence, and
invocation-order laws are the relevant properties.

Scope creation is constant-time storage. Reuse forwards once per call and adds no collection traversal, lock, cache,
thread-local, live lookup, or coordination. Existing additional-conversion construction cost is preserved; do not
claim a new asymptotic bound for the existing vector accumulation. Independent successful invocations allocate
independent states; concurrent tests use bounded test-owned scheduling and compare against sequential expectations.

Add a small `MarketStateConstructionBenchmark` in the existing non-published benchmarks module to compare direct and
reused-scope quote/third-asset construction with empty and non-empty conversions, consuming results so work is
observable. Setup and catalog assembly remain outside measured methods. Record host JDK, release target, warmup,
measurement, fork, and allocation settings. Short local JMH numbers are indicative evidence, not a deterministic
performance guarantee; investigate any material regression before the Task Group commit.

## Verification Plan

| AC | Primary planned evidence | Boundary/failure evidence |
| --- | --- | --- |
| AC-022 | `MarketStateInstrumentScopeSuite`: all eight direct modes with omitted/empty/non-empty vectors; source review confirms one direct definition per mode | completed `MarketStateScopeClient` positional/named/default inference and existing callers |
| AC-023 | exact alias/result assignments; all eight scoped modes; independent allocation and concurrent reuse | generic helper and concrete core-JAR clients without casts or repeated dimensions |
| AC-024 | baseline expected values and full error vectors, then direct/scoped comparisons; `EconomicsPropertiesSuite` coherent rational/anchor/rate laws | foreign identity/lineage, invalid/contradictory anchors, duplicates, price-grid boundary, first-error projection |
| AC-025 | positive/negative core-only compiler fixtures and existing dependency scans | separate conversion owner, no `Instrument` methods, purity/reflection guard, representative construction benchmark |

Planned focused checks are `instrumentEconomics/test` and
`adversarialBoundary/testOnly external.EconomicsCompilerBoundarySuite`.
Before the Task Group commit run formatting checks, `clean test`, `benchmarks/Jmh/compile`, the construction
measurement, both reflection-policy scripts, `git diff --check`, strict Corgi readiness, and the automated Task Group
review loop. Repository baseline remains Scala 3.8.4, SBT 1.12.15, and JDK 25 minimum with release target 25.
Any use of a newer host JDK must be recorded separately from the target and from evidence of execution on JDK 25.
Canonical whole-change Verify and explicit human review remain later gates; planning records no test PASS.

## Risks / Trade-offs

- Default arguments can affect inference or named arguments: compile all eight direct/scoped modes, all three vector
  forms, and generic/concrete clients against completed artifacts before committing the group.
- A scope can accidentally widen paths: preserve the singleton-returning factory and exact aliases, with negative
  endpoint fixtures and positive result assignments.
- Shared delegation can hide a common regression: establish baseline expected-value and full-error tests first.
- Reference equality can give misleading comparisons: compare complete public observations and verify allocation
  independently; retain production equality as-is.
- Validation may change timing if hoisted: keep the scope field-only and existing prerequisites at each operation.
- Allocation can matter to repeated market construction: reuse scopes, retain the existing algorithm, and measure
  representative direct/scoped construction without claiming deterministic latency from a short run.

## Migration Plan

One Task Group first captures baseline direct behavior, then consolidates defaults, adds the scope and tests, and
completes the required checks/review before its dedicated commit. Keep the old direct operation names and update only
any call sites that actually need adaptation; the full build verifies downstream source compatibility.

The canonical planning package contains added requirements on the existing capability, leaving all existing
requirement/scenario identities intact for Archive. Propose keeps HEAD unchanged; Apply creates the planning baseline
and implementation commit separately. Any accepted contract gap must use RFC governance rather than expanding this
Slice. Rollback of a published change would be a separately authorized revert; no data/schema migration is needed.
