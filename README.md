# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root. The
minimum build and runtime JDK is 25.

The current implementation has eleven production modules. `trading-quantities` provides exact quantities, anonymous
uniform-grid arithmetic and projection, checked refinements, domain-neutral runtime dimension identity, and optional
Typelevel Algebra integration. `trading-reference-data` owns stable asset/grid identity, immutable catalog transitions,
trusted handles, and coherent snapshots. `trading-application` owns the minimal interpreter-neutral `LiveCatalog[F]`
port. `trading-runtime` owns the concrete Cats Effect dependency and the implementation boundary for application
interpreters; its public in-memory factory constructs a fresh atomic live catalog while the Ref-backed implementation
remains private. No speculative market-data, persistence, clock, execution, transaction, stream, or telemetry port is
part of this foundation. `trading-instrument-economics` owns assembled instruments and the pure exact valuation,
fee-value, and PnL kernel. `trading-order-model` owns immutable order intent and instruction evidence, while
`trading-execution-lifecycle` owns authoritative actual-execution commands, source facts, reconciliation, and exact
exposure. `trading-execution-scenario` separately owns checked hypothetical matched outcomes; both depend one-way on
the order model and neither depends on the other.
`trading-fee-policy` owns the existing pure downstream fee/scenario composition under the `trading.fee` package root,
while `trading-risk` owns exact downside, constructive monotone models, boundary-certified maximum sizing, and the
explicit exhaustive fallback. `trading-boundary-codecs` now establishes the pure `trading.codec` artifact above the
encodable domain modules; S-05 delivered its versioned record-family APIs.

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |
| trading-reference-data | `reference-data` | `referenceData` | `trading-reference-data` | `trading.reference` |
| trading-application | `application` | `application` | `trading-application` | `trading.application` |
| trading-runtime | `runtime` | `runtime` | `trading-runtime` | `trading.runtime` |
| trading-instrument-economics | `instrument-economics` | `instrumentEconomics` | `trading-instrument-economics` | `trading.economics.instrument` |
| trading-order-model | `order-model` | `orderModel` | `trading-order-model` | `trading.order` |
| trading-execution-lifecycle | `execution-lifecycle` | `executionLifecycle` | `trading-execution-lifecycle` | `trading.execution` |
| trading-execution-scenario | `execution-scenario` | `executionScenario` | `trading-execution-scenario` | `trading.scenario` |
| trading-fee-policy | `fee-policy` | `feePolicy` | `trading-fee-policy` | `trading.fee` |
| trading-risk | `risk` | `risk` | `trading-risk` | `trading.risk` |
| trading-boundary-codecs | `boundary-codecs` | `boundaryCodecs` | `trading-boundary-codecs` | `trading.codec` |

See the [quantity module guide](quantities/README.md), [reference-data module guide](reference-data/README.md),
[application module guide](application/README.md), [runtime module guide](runtime/README.md),
[instrument economics guide](instrument-economics/README.md),
[order-model guide](order-model/README.md), [actual-execution lifecycle guide](execution-lifecycle/README.md),
[execution-scenario guide](execution-scenario/README.md),
[fee-policy module guide](fee-policy/README.md), [risk module guide](risk/README.md),
[boundary-codec module guide](boundary-codecs/README.md),
[actual-execution evidence map](docs/execution-lifecycle-evidence.md),
[catalog benchmark evidence](docs/catalog-benchmark.md), and [quantity performance notes](quantities/docs/performance.md).

## Architecture charter

The [architecture and functional design charter](docs/design-principles.md) defines responsibility ownership,
dependency direction, algebra-first modeling, semantic type preservation, evidence-producing validation, pure/effect
separation, dependency admission, Scala API ergonomics, and claim-proportional verification. Its
[portfolio audit](docs/architecture-charter-audit.md) records the current module graph, transitional exceptions, and
the proposed target architecture separately.

The catalog, snapshot, application-port, runtime-interpreter, instrument-economics, and shared benchmark
responsibilities now have concrete owners. Order intent, actual-execution evidence, execution-scenario interpretation,
fee-policy composition and scenario-owned attribution, risk, and boundary representation are separate physical pure
boundaries. Actual execution remains a pure leaf with no production consumer in its first Slice; later accepted Slices
may add effect-polymorphic application ports, concrete runtime interpreters, and versioned codecs without moving
domain authority out of `trading-execution-lifecycle`. RFC-0002 S-04 delivered the fee-policy API, while S-05 delivered
the boundary-codec artifact and the removed packing capability's durable replacement. RFC-0002 S-01 delivered the
runtime and future-port admission foundation and its first live-catalog interpreter. Every current module owns a
concrete responsibility rather than existing solely to match the target diagram.

## Build commands

```text
sbt quantities/test
sbt referenceData/test
sbt application/test
sbt runtime/test
sbt instrumentEconomics/test
sbt orderModel/test
sbt executionLifecycle/test
sbt executionScenario/test
sbt feePolicy/test
sbt risk/test
sbt boundaryCodecs/test
sbt benchmarks/Jmh/compile
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
