## Context

See [proposal.md](proposal.md) for motivation. The current implementation has the right external shape: one sealed generative `Instrument`, direct path-owned types such as `instrument.Price` and `instrument.Order`, and seven stable capability values. It also has the right authority boundary: `InstrumentImpl`, the existential grid casts, and every implementation of a nested sealed value remain private inside the `Instrument` companion's compilation unit.

That authority boundary is why the first refactor extracted only scalar rules. Scala sealing requires the implementations of the nested sealed traits to remain in the defining source file, while `private[economics]` is not an anti-spoof boundary because downstream source can declare `package trading.economics`. A useful split therefore cannot consist of moving the concrete value classes or the aggregate constructor to package-visible helpers.

The safe seam is between trusted representation and capability workflow. Validation and orchestration can live elsewhere when they return authority-neutral plans or receive narrowly scoped construction callbacks. The private aggregate remains the only place that owns the genuine grid witnesses, performs existential witness casts, and closes callbacks over constructors of its path-owned values.

## Goals / Non-Goals

**Goals:**

- Make the root file readable as a composition boundary rather than as the complete economics implementation.
- Give each public capability a correspondingly named internal implementation unit that contains its workflow, validation sequencing, and local calculations.
- Keep all authority-bearing operations lexically inside the private `Instrument` implementation while allowing authority-neutral orchestration to be tested by concern.
- Preserve the exact public member names, path-dependent relationships, validation order, typed failures, formulas, and deterministic evaluation behavior.
- Keep the internal structure concrete and domain-named instead of introducing a reusable framework for capability engines.

**Non-Goals:**

- Move nested sealed public interfaces or their concrete implementations to package-visible source files.
- Replace path-dependent members with owner-tagged top-level public types, opaque types, structural refinements, or type projections.
- Unseal `Instrument`, its capability interfaces, or its owned value hierarchies.
- Change a public method signature, add an alias, alter a formula, reorder validation, or change a diagnostic.
- Establish a line-count target; navigation and responsibility boundaries are the acceptance criteria.
- Change modules, dependencies, publication topology, serialization policy, canonical specifications, or SBT settings beyond restoring ordinary quantities main-to-test wiring and the economics package-ordering prerequisite described below.

## Decisions

### 1. Keep `Instrument.scala` as the trusted composition shell

`Instrument.scala` retains exactly the responsibilities that depend on source-file sealing or companion privacy:

1. the public sealed `Instrument` contract and its nested owned interfaces;
2. `Instrument.create` and aggregate-level registry/grid/payoff validation;
3. the companion-private `InstrumentImpl` and its owner-bound grid witness casts;
4. the minimal concrete implementations of nested sealed values;
5. stable `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, and `sizing` adapters that close over the current instrument and delegate workflow to internal engines;
6. final wrapping or unwrapping at points where a genuine path-owned value is created or its private identity must be checked.

The file will be ordered as public contract, validated construction, private owner state, stable capability adapters, and trusted leaf representations. Complete validation/calculation workflows do not remain inline merely because their final wrapper is constructed there.

Moving `InstrumentImpl` or the nested implementations to `private[economics]` top-level classes was rejected. Package-qualified privacy is visible to a same-package downstream spoofer and would weaken the current compiler boundary. Moving the public owned types to top-level owner-parameterized representations was also rejected: it would turn a readability refactor into a public type-model redesign.

### 2. Create one domain-named internal engine per public capability

The implementation is divided into these concern units:

| Public capability | Internal implementation unit | Primary responsibility |
| --- | --- | --- |
| `prices` | `InstrumentPrices.scala` | exact price selection, quantization decisions, coordinate validation, and observations |
| `market` | `InstrumentMarket.scala` | settlement-anchor derivation, conversion-set validation, coherence, and lookup plans |
| `orders` | `InstrumentOrders.scala` | visibility, activation, price-instruction, and order validation plans |
| `scenarios` | `InstrumentScenarios.scala` | activation/peg/slice validation, position changes, and round-trip plans |
| `fees` | `InstrumentFees.scala` | minimums, fee quantization inputs, fee attribution, and schedule composition |
| `valuation` | `InstrumentValuation.scala` | settle valuation, price PnL, fee conversion orchestration, and net PnL plans |
| `sizing` | `InstrumentSizing.scala` | downside risk and deterministic exhaustive candidate selection |

These are `private[economics]` implementation details, not services exposed from `Instrument`. Each unit is allowed to see public economics values, exact scalar data, enums, and deliberately supplied functions. It is not allowed to extend `Instrument`, implement one of its nested sealed traits, own a registered witness cast, or expose a constructor/factory for a genuine instrument-owned value.

The existing broad helpers `PriceMarketRules`, `OrderScenarioRules`, and `FeeValuationSizingRules` are folded into the corresponding units rather than retained as a second organizational axis. A small rule may remain adjacent to the engine that uses it; cross-capability utility extraction requires a concrete second use and must remain authority-free.

A single file per public capability is deliberate here. The names create a direct navigation map from a call such as `instrument.scenarios.order` to the workflow that validates it, while the trusted shell remains the map of how those capabilities are bound to one owner.

### 3. Cross the source-file boundary with plans and private callbacks, not authority

Each engine uses the narrowest of two safe seams:

- **Authority-neutral plan:** the engine validates inputs and returns immutable scalar/enumeration/reference data. `InstrumentImpl` converts that plan into the private sealed representation.
- **Private construction callback:** when a plan would only duplicate strongly typed data, `InstrumentImpl` supplies a small function that closes over a private constructor. The engine may invoke the function but never returns it, stores it in a public value, or obtains a general factory object.

Inputs may include genuine values already produced by the same stable instrument. Engines inspect them only through their public owned interfaces or through narrowly supplied observations such as tick/lot coordinates. Downcasts to private implementation classes, reference-identity checks, existential grid casts, and creation of `Quantity`/`GridQuantity` under hidden witnesses remain in the trusted shell.

Internal plan and engine types are not trusted evidence. Same-package code may be able to name or instantiate an authority-neutral plan, but doing so cannot create an `instrument.Price`, `instrument.Order`, `instrument.Fee`, or any other genuine owned value. This distinction is what makes package-visible implementation code compatible with the existing package-spoof invariant.

A general construction algebra, package-visible owner token, or package-visible `InstrumentImpl` factory was rejected. Those designs enlarge the authority surface and make the compiler boundary depend on token discipline. Local closures and neutral results keep authority flowing in only one direction from the existing private root.

### 4. Preserve the public and semantic baseline exactly

This change has `skip_specs: true`: canonical economics specs remain the normative behavior and receive no delta. The implementation must preserve:

- every public root/capability signature and default argument;
- direct `instrument.X` paths for all owned types;
- rejection of cross-instrument values at compilation;
- rejection of direct, nested-private, and same-package-spoof construction;
- price/grid exactness and residual-bearing quantization;
- market conversion positivity, identity, provenance, duplicate, and coherence checks;
- order/scenario validation order and diagnostics;
- fee attribution and conversion behavior;
- exact valuation/PnL formulas and deterministic exhaustive sizing, including failure order.

The existing packaged positive and negative compiler fixtures are therefore release gates, not tests to rewrite around a new API. Behavioral/property tests should normally remain unchanged. Focused unit tests may be added for authority-neutral engines, but they supplement rather than replace aggregate and packaged-boundary coverage.

Review must compare the public surface and representative diagnostics with the committed baseline. Any extraction that requires a public signature change, a new cast outside the root, unsealing, or a package-visible genuine-value constructor is a design conflict and stops routine implementation.

### 5. Optimize for local reading, not maximum extraction

Every nontrivial capability workflow belongs in its corresponding engine, but the last trusted wrapping statement stays beside its private representation. A short adapter that converts a validated plan into a sealed value is preferable to a generic factory abstraction.

Within `Instrument.scala`, repeated capability forwarding may be reduced with private local helpers only when the resulting code remains explicit about which capability and owner are involved. Macro generation, reflection, structural invocation, and code generation are out of scope.

This deliberately accepts a moderately sized trusted shell. The objective is that a maintainer can read one capability engine for behavior and the shell for authority, without reconstructing either responsibility from seven unrelated utility layers.

### 6. Keep same-project tests ordinary and make the economics external artifact deterministic

Quantities' own tests use SBT's ordinary same-project `Compile / classes` product. The prior `Test / internalDependencyClasspath` override that routed them through `Compile / packageBin` is removed: it replaced a standard compile dependency with a packaging step, made quantities test compilation depend on a JAR assembled from the same mutable output, and produced an incomplete JAR during an authoritative clean build. `quantitiesExternalArtifact` remains the immutable package boundary for the genuinely downstream adversarial project; no dependent-project classpath or exported-product setting changes.

The packaged downstream compiler boundary consumes `economicsExternalArtifact`, which delegates to `economics / Compile / packageBin`. On a clean build, packaging must not run before economics class generation and produce a manifest-only immutable artifact. Therefore `economics / Compile / packageBin` has an explicit direct prerequisite on `economics / Compile / compile`, while `economicsExternalArtifact` continues to consume the completed package.

Restoring the standard quantities test edge and retaining the bounded economics ordering edge introduce no project, module, library dependency, published artifact, or public behavior. Downstream external artifact selection remains unchanged. The resulting graph avoids packaging the quantities module merely to compile its own tests while ensuring direct economics package consumers receive a completed artifact without retries or filesystem-timing assumptions.

## Risks / Trade-offs

- [The engine/callback seam could add more indirection than it removes] → Keep engines domain-specific, use concrete plan names, and reject a generic capability framework or multi-layer adapter stack.
- [A callback or raw plan could accidentally leak construction authority] → Keep genuine constructors and callbacks private fields/local values of `InstrumentImpl`; audit exported signatures and rerun same-package-spoof compiler fixtures.
- [Moving validation can reorder failures or arithmetic] → Extract one capability at a time while retaining existing aggregate tests; explicitly compare failure precedence and exact coefficients before deleting inline code.
- [Path-dependent Scala inference may make a proposed seam impractical] → Prefer scalar observations and result plans; do not compensate with public types, structural casts, or witness casts outside `Instrument.scala`.
- [Seven files can become mechanical fragmentation] → Each file owns the complete workflow for its named capability and absorbs its current rules; shared helpers require a real cross-capability use.
- [The trusted shell will not become tiny because sealing is intentionally retained] → Judge success by responsibility and navigation boundaries, not by an arbitrary line count.
- [A same-project immutable-JAR workaround can obscure the normal compile dependency and package mutable output too early] → Keep quantities tests on SBT's ordinary classes product and reserve `quantitiesExternalArtifact` for downstream consumers.

## Migration Plan

1. Record the clean committed baseline and run focused economics plus packaged compiler-boundary checks before structural edits.
2. Introduce the seven authority-neutral implementation units and extract capabilities incrementally, keeping each stable public adapter and private representation in `Instrument.scala`.
3. After each extraction, run the affected behavior suites and inspect diagnostics where validation order is observable.
4. Remove the superseded broad rule helpers after their logic has one clear capability home; audit that no trusted constructor, witness cast, or private implementation downcast escaped the root file.
5. Restore quantities' ordinary same-project test classpath, inspect both external-artifact task edges and the bounded economics package-to-compile prerequisite, then run focused economics tests, the packaged downstream compiler-boundary suite, formatting, strict OpenSpec validation, and one unretried full clean multi-module test.
6. Stage the complete change for fresh independent review. After approval, finalize/archive and run post-archive validation under the normal steward workflow.

Rollback is a source-only revert; there is no wire, persistence, dependency, or deployment migration.
