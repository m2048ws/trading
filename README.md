# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 17.

The current implementation has five production modules. `trading-quantities` provides exact quantities, anonymous
uniform-grid arithmetic and projection, checked refinements, domain-neutral runtime dimension identity, and optional
Typelevel Algebra integration. `trading-reference-data` owns stable asset/grid identity, immutable catalog transitions,
trusted handles, and coherent snapshots. `trading-application` owns the minimal interpreter-neutral `LiveCatalog[F]`
port. `trading-instrument-economics` owns assembled instruments and the pure exact valuation, fee-value, and PnL kernel.
The transitional `trading-economics` artifact now contains only downstream order, scenario, fee-policy, and risk
packages and depends one-way on instrument economics.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-reference-data | `reference-data` | `referenceData` | `trading-reference-data` | `trading.reference` |
| trading-application | `application` | `application` | `trading-application` | `trading.application` |
| trading-instrument-economics | `instrument-economics` | `instrumentEconomics` | `trading-instrument-economics` | `trading.economics.instrument` |
| trading-economics | `economics` | `economics` | `trading-economics` | `trading.order`, `trading.scenario`, `trading.fee.policy`, `trading.risk` |

See the [quantity module guide](quantities/README.md), [reference-data module guide](reference-data/README.md),
[application module guide](application/README.md), [instrument economics guide](instrument-economics/README.md),
[economics module guide](economics/README.md),
[catalog benchmark evidence](docs/catalog-benchmark.md), and [quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The catalog, snapshot, application-port, instrument-assembly, and shared benchmark responsibilities now have concrete
owners. Instrument economics is now a physical pure boundary. The final order-model, execution-scenario, fee-policy,
risk, runtime, and boundary-codec artifacts remain owned by their later active proposals. No empty target module is
created solely to match the diagram.

## Build commands

```text
sbt quantities/test
sbt referenceData/test
sbt application/test
sbt instrumentEconomics/test
sbt economics/test
sbt benchmarks/Jmh/compile
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
