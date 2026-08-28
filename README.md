# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 17.

The current implementation has three production modules. `trading-quantities` provides exact quantities, anonymous
uniform-grid arithmetic and projection, checked refinements, domain-neutral runtime dimension identity, and optional
Typelevel Algebra integration. `trading-reference-data` owns stable asset/grid identity and trusted handles.
`trading-economics` contains the current aggregate instrument, order, scenario, fee, valuation, and sizing surface.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-reference-data | `reference-data` | `referenceData` | `trading-reference-data` | `trading.reference` |
| trading-economics | `economics` | `economics` | `trading-economics` | `trading.economics.instrument` |

See the [quantity module guide](quantities/README.md), [reference-data module guide](reference-data/README.md),
[economics module guide](economics/README.md), and [quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The target catalog, instrument-economics, order-model, execution-scenario, fee-policy, risk, application, runtime,
boundary-codec, and benchmark responsibilities remain active proposals. Reference data currently contains an
explicitly transitional synchronized construction bridge that Proposal 2 replaces, while Proposal 9 owns the removed
packing capability's durable replacement. No empty target module is created solely to match the diagram.

## Build commands

```text
sbt quantities/test
sbt referenceData/test
sbt economics/test
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
