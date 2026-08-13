# trading

`trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a
non-published aggregator: production code belongs to independently named modules rather than the repository root.

The current implementation is the `trading-quantities` module. It provides exact quantities, uniform-grid arithmetic
and projection, checked refinements, runtime identity and logical reconstruction, plus optional Typelevel Algebra
integration. See the [quantity module guide](quantities/README.md) and its [performance notes](quantities/docs/performance.md).

| Module | Directory | SBT ID | Artifact | Package |
| --- | --- | --- | --- | --- |
| trading-quantities | `quantities` | `quantities` | `trading-quantities` | `trading.quantity` |

Future trading-domain modules—such as instruments, orders, positions, ledger, and wallets—are deliberately deferred.
No domain model is implied by the current quantity foundation.

## Build commands

```text
sbt quantities/test
sbt quantities/doc
sbt adversarialBoundary/test
sbt test
```
