## Context

See [proposal.md](proposal.md) for motivation. The order model currently exposes path-dependent activation evidence and pricing resolution but also implements package-visible `Any` acceptance hooks. `ScenarioAssumptions` uses those hooks to defend an erased Java call path, so `create`, `one`, and `many` return `Either` despite well-typed Scala arguments and an already non-empty slice value. The adversarial boundary separately maintains ordinary-Java fixtures and dynamic Java compiler helpers for reference data, order/scenario, execution lifecycle, and codecs.

The accepted RFC keeps the JVM, JDK 25, Java libraries, external wire representations, exact decimal conversion, and Java-object-serialization rejection. It removes only the promise that ordinary Java source is a supported domain client.

## Goals / Non-Goals

**Goals:**

- Let associated Scala types and `MatchedSlices` carry the structural proof needed for direct assumptions construction.
- Keep same-shape activation and pricing agreement as semantic checks owned by evaluation.
- Remove Java-only compiler fixtures and only the harness code dedicated to compiling or loading them.
- Preserve completed-artifact Scala, negative compiler, dependency, semantic, wire, null-boundary, and serialization evidence.
- Make active specifications consistently distinguish the supported Scala 3 domain surface from Java-library integration and external data.

**Non-Goals:**

- Converting the remaining production Java identifier classes; S-04 owns that work.
- Removing Java dependencies, JVM platform APIs, or `ObjectOutputStream` rejection tests.
- Changing wire schemas, canonical JSON, catalog semantics, exact arithmetic, or the JDK baseline.
- Removing general Scala compiler-fixture classloading that is still needed to execute completed-artifact clients.

## Decisions

### Associated types replace erased shape acceptance

Remove `acceptsEvidence` and `acceptsResolution` from activation, pricing, and execution abstractions. `ScenarioAssumptions.create` will construct directly from `order.activation.Evidence`, `order.execution.Resolution`, and `MatchedSlices`; `one` and `many` will first build the non-empty collection and return directly. This uses the existing path-dependent Scala contract rather than duplicating it with runtime classifiers.

An alternative was to retain `Either` with an unreachable success-only implementation. That would preserve source shape but falsely advertise expected failure and keep downstream projections that no longer express domain meaning.

### Expected emptiness remains typed

`ScenarioAssumptions.fromVector` will continue to return `Either[ScenarioViolation, ScenarioAssumptions]`, sequencing `MatchedSlices.fromVector` before direct construction. Vector emptiness is genuine adapter input invalidity, unlike associated evidence shape after Scala type checking.

An alternative was to make all constructors direct and throw on an empty vector. That would violate the repository rule that expected invalidity remains in a domain result.

### Semantic replay checks stay with activation and pricing

Instruction-owned `verify` and `resolve` operations remain unchanged so evidence or resolution built for a different same-shaped instruction still returns typed mismatch errors during `OrderScenario.evaluate`. Only erased shape mismatches and their reflection-based regression test disappear.

An alternative was to move same-shape agreement into assumption construction. That would make assumptions construction effectually validate semantics and duplicate the staged evaluation boundary.

### Remove Java fixture branches, retain independent Scala evidence

Delete the ordinary-Java fixture resources and `ReferenceDataJavaBoundarySuite`. In mixed compiler suites, remove Java fixture roots, Java compiler data structures/helpers, Java-specific tests, and classloader code used only to run those Java classes. Retain the Scala compilation helpers and loaders used for completed-JAR clients, plus all semantic, dependency, wire, null, and serialization suites.

An alternative was to keep the Java fixtures as non-contract regression coverage. That would continue constraining signatures and adapters to an explicitly unsupported source language.

### Specifications state source support without conflating JVM mechanisms

Architecture and domain specifications will name Scala 3 as the supported source API while explicitly preserving Java-library interoperation behind owning boundaries and Java-object-serialization rejection. Quantity and reference-data raw JVM reconstruction paths may remain temporarily where S-04 still owns their removal, but they are external checked boundaries rather than ordinary-Java domain APIs.

## Risks / Trade-offs

- [Risk] Direct return types require broad downstream call-site edits and a missed `.toOption.get` can hide in compiler fixtures. → Compile focused production modules and all affected completed-artifact fixtures before the aggregate matrix.
- [Risk] Removing Java helper code could accidentally remove classloading still used by Scala completed-JAR tests. → Remove helpers by call graph, retain shared Scala loaders, and run every adversarial-boundary suite.
- [Risk] Eliminating erased shape checks might appear to eliminate semantic mismatch detection. → Keep and strengthen fixed/trailing and pegged same-shape replay tests through evaluation.
- [Risk] Specification updates could imply that all Java/JVM behavior is removed. → State retained Java-library, external reconstruction, and serialization policies explicitly.

## Migration Plan

1. Convert assumptions construction and all Scala consumers together, then run order/scenario, fee, and codec tests plus compiler fixtures.
2. Remove the four Java fixture families and their dedicated harness branches, then run the adversarial boundary from clean completed artifacts.
3. Align specifications and run the complete clean test, benchmark-compile, formatting, and repository guard matrix.

Rollback is the reversal of the dedicated Task Group commit; no persisted data, external schema, or deployed runtime migration is involved.
