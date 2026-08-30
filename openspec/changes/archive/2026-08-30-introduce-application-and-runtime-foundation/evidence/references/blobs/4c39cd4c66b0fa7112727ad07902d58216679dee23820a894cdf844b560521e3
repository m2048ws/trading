# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 17.

The current implementation has five production modules. `trading-quantities` provides exact quantities, anonymous
uniform-grid arithmetic and projection, checked refinements, domain-neutral runtime dimension identity, and optional
Typelevel Algebra integration. `trading-reference-data` owns stable asset/grid identity, immutable catalog transitions,
trusted handles, and coherent snapshots. `trading-application` owns the minimal interpreter-neutral `LiveCatalog[F]`
port. `trading-runtime` owns the concrete Cats Effect dependency and the implementation boundary for application
interpreters; its public in-memory factory constructs a fresh atomic live catalog while the Ref-backed implementation
remains private. No speculative market-data, persistence, clock, execution, transaction, stream, or telemetry port is
part of this foundation.
`trading-economics` contains the current aggregate instrument, order, scenario, fee, valuation, and sizing surface.
Its instrument entry point is the pure `InstrumentDefinition + CatalogSnapshot -> InstrumentSpec -> Instrument` trust
transition; constructed instruments retain trusted handles and do not consult a live catalog.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-reference-data | `reference-data` | `referenceData` | `trading-reference-data` | `trading.reference` |
| trading-application | `application` | `application` | `trading-application` | `trading.application` |
| trading-runtime | `runtime` | `runtime` | `trading-runtime` | `trading.runtime` |
| trading-economics | `economics` | `economics` | `trading-economics` | `trading.economics.instrument` |

See the [quantity module guide](quantities/README.md), [reference-data module guide](reference-data/README.md),
[application module guide](application/README.md), [economics module guide](economics/README.md),
[runtime module guide](runtime/README.md),
[catalog benchmark evidence](docs/catalog-benchmark.md), and [quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The catalog, snapshot, application-port, runtime-interpreter, instrument-assembly, and shared benchmark responsibilities
now have concrete owners. The target physical instrument-economics, order-model, execution-scenario, fee-policy, risk,
and boundary-codec responsibilities remain later RFC Slices. RFC-0002 S-01 is the runtime and future-port admission
foundation and supplies the first live-catalog interpreter; S-05 owns the removed packing capability's durable
replacement. The runtime module exists because it owns a concrete effect dependency and independently verified
publication boundary, not merely to match the target diagram.

## Build commands

```text
sbt quantities/test
sbt referenceData/test
sbt application/test
sbt runtime/test
sbt economics/test
sbt benchmarks/Jmh/compile
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
