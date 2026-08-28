# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 17.

The current implementation has two production modules. `trading-quantities` provides exact quantities, uniform-grid
arithmetic and projection, checked refinements, runtime identity and logical reconstruction, plus optional Typelevel
Algebra integration. `trading-economics` contains the current aggregate instrument, order, scenario, fee, valuation,
and sizing surface.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-economics | `economics` | `economics` | `trading-economics` | `trading.economics.instrument` |

See the [quantity module guide](quantities/README.md), [economics module guide](economics/README.md), and
[quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The target reference-data, instrument-economics, order-model, execution-scenario, fee-policy, risk, application,
runtime, boundary-codec, and benchmark modules are active proposals, not implemented modules or available APIs. The
current `economics` aggregate and several quantity-owned identity/persistence responsibilities are explicitly
transitional; their owning proposals are listed in the audit. No empty target module is created solely to match the
diagram.

## Build commands

```text
sbt quantities/test
sbt economics/test
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
