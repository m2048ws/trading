# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 17.

The current implementation has four production modules. `trading-quantities` provides exact quantities, anonymous
uniform-grid arithmetic and projection, checked refinements, domain-neutral runtime dimension identity, and optional
Typelevel Algebra integration. `trading-reference-data` owns stable asset/grid identity, immutable catalog transitions,
trusted handles, and coherent snapshots. `trading-application` owns the minimal interpreter-neutral `LiveCatalog[F]`
port. `trading-economics` contains the current aggregate instrument, order, scenario, fee, valuation, and sizing surface.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-reference-data | `reference-data` | `referenceData` | `trading-reference-data` | `trading.reference` |
| trading-application | `application` | `application` | `trading-application` | `trading.application` |
| trading-economics | `economics` | `economics` | `trading-economics` | `trading.economics.instrument` |

See the [quantity module guide](quantities/README.md), [reference-data module guide](reference-data/README.md),
[application module guide](application/README.md), [economics module guide](economics/README.md),
[catalog benchmark evidence](docs/catalog-benchmark.md), and [quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The catalog, snapshot, application-port, and shared benchmark responsibilities now have concrete owners. The target
instrument-economics, order-model, execution-scenario, fee-policy, risk, runtime, and boundary-codec responsibilities
remain active proposals. Proposal 8 owns the first live catalog interpreter; Proposal 9 owns the removed packing
capability's durable replacement. No empty target module is created solely to match the diagram.

## Build commands

```text
sbt quantities/test
sbt referenceData/test
sbt application/test
sbt economics/test
sbt benchmarks/Jmh/compile
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
