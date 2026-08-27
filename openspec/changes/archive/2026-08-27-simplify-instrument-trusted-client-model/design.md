## Context

See `proposal.md` for motivation. The current economics implementation has a sound but expensive anti-forgery architecture:

- `Instrument.scala` is 1,274 lines and contains the public carriers, capability interfaces, generative `Instrument.Owner`, roughly thirty owner-indexed aliases, `OwnerAuthority`, forwarding factories, private implementation classes, validated instrument construction, and capability wiring.
- The economics sources contain hundreds of owner-type occurrences and more than one hundred references to `JvmOwnerAuthority` or `OwnerAuthority`; every concern implementation receives the authority even when its economic calculation does not use it.
- Each domain alternative normally exists twice: a gated sealed abstract carrier and a private implementation class. Pattern matching therefore depends on abstract subtype APIs plus hidden implementations instead of the direct data shape.
- A package-private Java gate, Java-release build configuration, bytecode audits, Java negative fixtures, Scala same-package spoof fixtures, and serialization rejection exist to keep determined callers from minting those carriers.
- Numeric, grid, registry, order, scenario, conversion, fee, valuation, and sizing checks already live largely in the concern modules. Those checks are conceptually independent from the issuance machinery.

The relevant delta specifications replace only the economics ownership boundary. The quantities artifact remains governed by its existing closed-carrier, runtime witness, registry provenance, and serialization invariants.

## Goals / Non-Goals

**Goals:**

- Make the source read as a domain model: direct values and alternatives, followed by the validators and calculations that operate on them.
- Remove owner and authority plumbing from public signatures, implementation constructors, callbacks, and pattern matches.
- Centralize the small amount of runtime instrument-coherence validation that remains, instead of scattering equality checks.
- Give each concern file ownership of both its domain types and its behavior so `Instrument.scala` is an aggregate rather than a type catalogue and issuance kernel.
- Preserve the economic results and failures that protect benign clients from ordinary mistakes.
- Keep representative source files and method signatures small enough to understand locally; prefer a cohesive request or aggregate when several parameters form one reusable concept.

**Non-Goals:**

- Protect economics values against hostile same-package source, Java/JVM bytecode, reflection, unsafe casts, or constructor-bypassing deserialization.
- Weaken any construction, identity, grid, registry, or serialization rule in `quantities`.
- Make economics objects a supported Java-serialization persistence format.
- Add a public contract/listing hierarchy, venue parsing, execution lifecycle, accounts, ledgers, or compatibility aliases for the unreleased owner-indexed API.
- Remove endpoint dimension typing, positive refinements, contextual grids, exact arithmetic, or typed aggregate failures in the name of fewer type parameters.

## Decisions

### 1. The economics trust boundary is explicit and local

Economics assumes callers use its supported constructors honestly. Public or package-visible constructors are allowed when their field types already express the local shape. We will delete the special JVM issuer, owner authority, gate assertions, hostile construction audits, and economics-specific serialization blocker inheritance.

This is not a general repository trust-policy change. An economics value may contain a `Quantity`, `GridQuantity`, `DimRef`, `RegisteredGridRef`, or `AssetRef`, but the economics refactor must obtain those values through their existing public APIs and must not expose a new way to manufacture or retag them.

Alternative considered: keep `JvmOwnerAuthority` but hide it behind a smaller kernel. That preserves anti-forgery but also preserves the cause of the duplicate carriers, forwarding factory, JVM audit, Java fixture matrix, and most constructor plumbing. It does not meet the readability goal.

### 2. Remove generative ownership; retain ordinary runtime identity

Remove `Instrument.Owner` and the owner parameter `O` from every economics type and capability. Representative transitions are:

```scala
InstrumentLots[O, D]             -> InstrumentLots[D]
InstrumentPrice[O, B, Q]         -> InstrumentPrice[B, Q]
OrderActivation[O, P]            -> OrderActivation[P]
InstrumentOrder[O, L, P]         -> InstrumentOrder[L, P]
InstrumentFeeSchedule[O, ...]    -> InstrumentFeeSchedule[...]
```

The stable external `InstrumentId` becomes the simple runtime coherence key. Instrument-dependent leaves such as lots and prices retain it. Aggregates such as market states, orders, scenarios, denominations, fees, and PnL either store it directly or derive and store it once from their validated inputs. Stateless alternatives such as immediate activation, side, liquidity constraint, or a peg reference do not need a duplicate identity field.

One package-local helper will compare an expected `InstrumentId` with named supplied identities and return a single typed error such as:

```scala
InstrumentMismatch(context, expected, supplied)
```

The helper is validation reuse, not authority: it issues no token, performs no cast, and callers can construct equal `InstrumentId` data. Checks occur at multi-value boundaries, including market-state, order, complete scenario, round trip, fee schedule evaluation/composition, valuation, and sizing callback results where applicable.

The implementation inventory is:

| Boundary/value | Runtime identity inputs | Retained or derived identity |
| --- | --- | --- |
| `Lots`, `PositionLots`, `Price`, settlement conversion, fee denomination, and fee | Target instrument or validated parent value | Retained directly because each leaf can cross a later aggregate boundary |
| `positionLots` | Target instrument and supplied lots | `PositionLots` derives the target identity after checking lots |
| Market-state construction | Target instrument, price, and every additional conversion | Market state derives the target identity after checking every supplied identity |
| Order construction | Target instrument, intent/lots, trigger or limit price, and iceberg lots | Intent derives lots identity; final order derives the target identity after checking all instrument-dependent components |
| Liquidity-slice and complete-scenario construction | Target instrument, order, evidence/resolution prices, slice lots, and slice market states | Slice, assumptions, and complete scenario retain the target identity; final scenario validates every nested supplied identity |
| Round-trip construction | Target instrument, entry/exit scenarios, and their position changes | Round trip derives the target identity after checking both legs |
| Fee-line construction and schedule composition/evaluation | Target instrument, scenario, fee/denomination, source market, component schedules, and returned lines | Schedule, fee line, and converted line retain the target identity; returned custom-policy lines are rechecked |
| Valuation/PnL | Target instrument, round trip, schedule, scenarios, fee lines/fees/denominations, and source market states | PnL derives the target identity after every input and conversion association is checked |
| Sizing | Target instrument, fee schedule, callback round trip, held position, and both scenario legs | Selected lots already retain the target identity; each callback result is checked before risk evaluation |

Side, liquidity constraint/role, position effect, time in force, price reference, comparison, immediate activation, peg metadata, and other stateless alternatives carry no identity field. Trigger evidence and peg resolution retain their instrument-dependent prices rather than duplicating an identity tag.

Alternative considered: drop all cross-instrument checks. That is the smallest code shape, but ordinary accidental mixing could silently produce financially wrong results. Runtime ID comparison is cheap, easy to diagnose, and does not reintroduce the anti-spoof architecture.

Alternative considered: introduce a new opaque `InstrumentToken` or UUID distinct from `InstrumentId`. That would rebuild an issuance abstraction and complicate persistence. The stable domain identity is sufficient under the trusted-client premise.

### 3. Use direct closed alternatives and refined value classes

Replace each sealed-abstract-carrier/private-implementation pair with one direct sealed hierarchy or enum and final data cases. A type should have a private or smart constructor only when construction performs an actual local correctness check, for example:

- positive lots;
- positive grid-valid prices;
- positive trailing offsets;
- non-resting market duration;
- coherent fee denomination.

Structural alternatives whose fields already make invalid shapes unrepresentable should be directly constructible and directly pattern-matchable. For example, immediate/fixed/trailing activation, limit/pegged pricing, displayed/hidden/iceberg visibility, trigger evidence, and pricing assumptions should expose their real cases without hidden implementation downcasts.

No compatibility carrier, forwarding alias, or deprecated owner-indexed facade is retained because the artifact is unreleased.

Alternative considered: make every type a case class with `require`. This shortens code but converts typed construction failures into exceptions and permits invalid cross-value combinations until later. Smart constructors remain for the local checks already promised by the specs.

### 4. Each concern owns its types and behavior

`Instrument.scala` will contain only the validated aggregate, its semantic components or direct references to them, final construction, minimal convenience entry points such as lots/flat position, and focused capability wiring.

Concern files will collocate their direct domain types, capability API, validation, and calculation:

- prices and positive price construction;
- market states and conversions;
- order alternatives and order aggregation;
- scenario evidence, liquidity slices, complete scenarios, and round trips;
- fee denominations, fees, fee lines, and schedules;
- valuation/PnL;
- sizing.

Capability interfaces that have only one implementation and no extension role become final concern classes. Each concern object receives the stable `Instrument` (or the minimum validated fields when that is materially clearer), not an authority plus many repeated witnesses. Capabilities may use the instrument's path-dependent role dimensions; those paths represent real dimension relationships, not construction authority.

`Instrument` may retain a small number of ergonomic aliases only when they materially simplify downstream type annotations. The large alias mirror of every carrier and capability is not a requirement; inferred ordinary domain types are preferred.

Alternative considered: place all data in `Model.scala` and all behavior in the existing `Instrument*` files. That reproduces the present navigation problem in another file. Collocating a concern's data and behavior keeps validation ownership visible.

### 5. Validation follows one ownership rule

Validation will be organized by the invariant's natural owner:

1. Refined numeric construction validates sign, nonzero, and exact-grid membership.
2. Direct alternatives encode structural validity with their cases and field types.
3. The final instrument boundary validates registry, dimension, role, grid, and payoff coherence.
4. Order, market, scenario, fee, valuation, and sizing aggregate boundaries validate relationships among already locally valid values, including runtime instrument identity.
5. Calculation code consumes validated aggregates and does not repeat their checks.

Shared validation is extracted only when the same rule appears at multiple boundaries. The main expected shared abstraction is named runtime instrument coherence; registry and conversion helpers remain shared where they already represent reusable domain rules. A generic validation framework, builder hierarchy, or proof algebra is explicitly avoided.

Wide methods are replaced only when their inputs have reusable meaning. Existing cohesive values such as `InstrumentDefinition`, `OrderIntent`, `ScenarioAssumptions`, and `FeeDenomination` remain. New request values are justified for repeated multi-field boundaries, but single-use wrappers that merely hide argument count are not.

### 6. Remove opaque scenario-identity anti-abuse; keep cheap callback correctness checks

The private `AnyRef` scenario identity stored in fee lines exists to stop a schedule implementation from returning a line minted for another equal-looking scenario. Under the trusted-client boundary it is removed. Fee lines retain explicit runtime `InstrumentId`, leg/slice attribution, bounds validation, fee data, and market state; valuation continues to verify those ordinary fields and conversions.

Sizing continues to verify that a scenario callback returns a scenario for the candidate lot count and target `InstrumentId`. This is a small correctness check against an easy accidental callback bug and does not require hidden identity or non-forgeable authority. `SizingScenarioMismatch` may be generalized or paired with `InstrumentMismatch`, but sizing remains exhaustive over the configured candidate range.

Alternative considered: remove all callback result verification. It saves little and would let a benign but incorrect callback produce an apparently valid risk limit for a different position size.

### 7. Boundary tests shift from hostile construction to retained semantics

Delete economics fixtures and bytecode assertions whose only expected result is that callers cannot access an authority, subclass a carrier, spoof the package, or bypass a private implementation. Delete the economics Java gate access matrix with the gate itself.

Retain or replace coverage for:

- downstream use of the direct API;
- explicit alternatives and pattern matching;
- positive refinement and exact-grid rejection;
- runtime `InstrumentId` mismatch failures at representative aggregate boundaries;
- registry/grid provenance and conversion coherence;
- scenario conservation, trigger, peg, limit, and liquidity checks;
- fee denomination, attribution, conversion, and conservation;
- exact valuation and exhaustive sizing;
- removal of the superseded owner/authority API.

Cross-instrument compiler-negative fixtures become runtime mismatch tests. Quantity package-spoof, carrier, serialization, registry, and bytecode tests remain unchanged because they test a different artifact and trust boundary.

### 8. Serialization and Java target are not conflated with domain correctness

Remove `JavaSerializationUnsupported` inheritance from economics-only types when it serves solely as constructor-bypass hardening. Do not remove or alter the trait itself or its use by quantities types. The economics API still makes no promise that Java serialization is stable, portable, or suitable for storage.

If `JvmOwnerAuthority.java` is the only economics Java production source, delete it. Remove an economics-specific `javac --release 17` override only if it was introduced solely for that deleted gate and its audit. Do not change the Scala bytecode target or repository-wide compatibility policy as part of this change.

## Risks / Trade-offs

- [A caller can forge an economics value or lie about `InstrumentId`] → This is accepted by the trusted-client premise; document the boundary and keep all quantities authority unchanged.
- [Compile-time cross-instrument errors become runtime failures] → Store `InstrumentId` on instrument-dependent values, centralize equality diagnostics, and cover each major aggregate class with mismatch tests.
- [Direct constructors can bypass recommended smart constructors] → Use private constructors only for actual numeric/grid invariants and test the supported construction surface; do not claim resistance to unsafe or same-package bypasses.
- [Runtime identity fields add repetition] → Store identity only on leaves and aggregates that cross boundaries; derive it from validated children when possible rather than tagging every stateless alternative.
- [Refactoring many signatures can accidentally change formulas] → Preserve existing property tests and exact result fixtures, migrate concern by concern, and compare representative outputs before deleting the old machinery.
- [A new shared context object could become another kernel] → Prefer passing the stable `Instrument` to final concern classes and keep any package-local validation helper data-only, token-free, and cast-free.
- [Removing economics serialization blockers may be misread as persistence support] → Keep the explicit no-persistence-contract requirement and use adapter/schema tests for any future persistence capability.

## Migration Plan

1. Add the ordinary runtime mismatch error/helper and direct lots, position, and price values while preserving existing economic tests.
2. Convert activation, pricing, visibility, execution, and order types to direct owner-free alternatives; migrate order constructors and tests.
3. Convert market, scenario, round-trip, fee, PnL, and sizing types concern by concern, adding runtime identity checks at aggregate boundaries.
4. Remove fee-line `AnyRef` scenario identity and retain explicit attribution/index validation.
5. Collapse single-implementation capability traits into final concern classes where no public extension contract exists; wire them from the validated `Instrument` without an authority object.
6. Delete `Instrument.Owner`, all owner parameters and aliases, `OwnerAuthority`, the abstract/private implementation pairs, `JvmOwnerAuthority`, and economics-only serialization blocker inheritance.
7. Replace hostile economics boundary fixtures and bytecode audits with direct API, runtime mismatch, and retained-correctness coverage. Leave quantities boundary suites unchanged.
8. Remove build configuration made obsolete solely by the deleted Java gate, format, run focused economics and downstream boundary suites, then run the full clean repository validation and strict OpenSpec validation.

Because the API is unreleased, migration is an in-tree source rewrite with no compatibility layer. Rollback is the ordinary Git revert of this change before release; there is no persisted owner-token format or deployed data migration.
