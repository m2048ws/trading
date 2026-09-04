## Why

The supported domain surface is Scala 3, but order/scenario construction still carries erased runtime shape checks and the test suite still maintains ordinary-Java API contracts that the project no longer promises. Removing those compatibility paths makes associated Scala evidence authoritative, reduces duplicate validation, and lets the active specifications and compiler-boundary evidence describe one coherent source API.

## What Changes

- **BREAKING** Remove `Any`-based activation-evidence and pricing-resolution acceptance hooks from order types.
- **BREAKING** Make `ScenarioAssumptions.create`, `one`, and `many` return constructed assumptions directly when their associated Scala types and non-empty slices already prove structural validity; retain typed empty rejection in `fromVector`.
- **BREAKING** Remove ordinary-Java domain API fixtures and the dedicated dynamic Java compiler/classloader harness paths for reference data, order/scenario, execution lifecycle, and boundary codecs.
- Retain completed-artifact Scala clients, negative Scala compiler fixtures, dependency checks, semantic and wire tests, Java-library interoperability, external representation checks, exact decimal conversion, and Java-object-serialization rejection.
- Align the active architecture, quantity-grid, reference-data, order/scenario, execution, and codec specifications on Scala 3 as the supported domain source API without changing the JDK 25 baseline.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `repository-architecture`: Define Scala 3, rather than ordinary Java domain callers, as the supported cooperative source API while preserving JVM-library and trust-boundary rules.
- `quantity-grid-projection`: Remove the ordinary-Java raw-grid source contract while preserving checked Scala construction and external reconstruction.
- `runtime-quantity-identity`: Narrow documented runtime-witness use to supported Scala callers without weakening serialization rejection.
- `reference-data-identity`: Remove ordinary-Java construction promises for stable identities and handles while preserving checked Scala and external boundaries.
- `reference-data-catalog`: Remove ordinary-Java construction and authority promises while retaining checked catalog invariants and reconciliation.
- `order-scenarios`: Make associated Scala evidence sufficient for direct assumptions construction and retain semantic validation at evaluation.
- `actual-execution-lifecycle`: Retire the ordinary-Java lifecycle API fixture while preserving completed-artifact Scala, dependency, semantics, and serialization evidence.
- `versioned-boundary-codecs`: Retire the ordinary-Java codec API fixture while preserving Scala client, wire, schema, null-boundary, and serialization evidence.

## Impact

The change affects order-model and execution-scenario construction, adversarial compiler fixtures and harnesses, and the active specifications named above. Downstream Scala scenario, fee, execution, and codec call sites will adapt to direct assumptions construction; production dependency direction, wire formats, exact numeric behavior, JDK baseline, and serialization policy remain unchanged.
