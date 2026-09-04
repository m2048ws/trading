# Trading Runtime

`trading-runtime` is the concrete effect boundary above `trading-application`. It owns the admitted Cats Effect
dependency and is the only place where resource and concurrency mechanisms, concrete clients, streams, telemetry
decorators, and application-port interpreters may be implemented. Lower modules never depend on this artifact.

The module depends only on `trading-application` and `trading-reference-data` plus Cats Effect 3. Cats Effect TestKit
and the current Cats Effect 3-only `munit-cats-effect` artifact are test-only; FS2 is not admitted by this change.

`InMemoryLiveCatalog.create[F: Sync](bootstrap)` delays allocation of a fresh catalog root, evaluates an optional
bootstrap through `CatalogModel.commit`, and returns typed violations or only the `LiveCatalog[F]` capability. Its one
private `Ref` publishes complete immutable states. The interpreter owns no releasable resource, so construction returns
`F[...]`; callers may lift it into a larger `Resource` graph without adding a meaningless finalizer.

Snapshot capture performs one atomic state read and returns an immutable `CatalogSnapshot`; all point resolution after
capture is pure. Commit performs one atomic modification whose retryable function invokes only `CatalogModel.commit`.
Failures and idempotent retries retain the current state, while a valid non-empty transition publishes its complete
successor revision and delta together.

Callers depend on the application-facing `LiveCatalog[F]` capability. Runtime coordination classes and mutable state
remain implementation details and do not appear in application signatures.

Runtime tests instantiate the application test-source `LiveCatalogContract[IO]` directly with the public in-memory
constructor. Those shared, framework-neutral cases compare observable results with the pure catalog model and prove
that equal independently constructed catalogs are structurally equivalent but retain distinct lineages. Cats Effect
TestKit cancellation checks, real-runtime race/stress checks, packaged dependency/JDK scans, and JMH measurements remain
interpreter-specific coverage. Future database, actor, simulation, backtest, or venue interpreters should reuse the
shared cases and add only the lifecycle and integration evidence required by their mechanism.
