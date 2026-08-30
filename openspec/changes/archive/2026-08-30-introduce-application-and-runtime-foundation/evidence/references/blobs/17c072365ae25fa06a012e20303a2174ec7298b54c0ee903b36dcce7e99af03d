# Trading Runtime

`trading-runtime` is the concrete effect boundary above `trading-application`. It owns the admitted Cats Effect
dependency and is the only place where resource and concurrency mechanisms, concrete clients, streams, telemetry
decorators, and application-port interpreters may be implemented. Lower modules never depend on this artifact.

The module depends only on `trading-application` and `trading-reference-data` plus Cats Effect 3. Cats Effect TestKit
and the current Cats Effect 3-only `munit-cats-effect` artifact are test-only; FS2 is not admitted by this change. This
boundary commit intentionally adds no
speculative runtime API; the in-memory `LiveCatalog` implementation arrives in the next implementation group.

Callers depend on the application-facing `LiveCatalog[F]` capability. Runtime coordination classes and mutable state
will remain implementation details and do not appear in application signatures.
