# Trading Application

`trading-application` owns effect-polymorphic capabilities whose execution genuinely varies by interpreter. Its initial
surface is only `LiveCatalog[F]`: capture one immutable `CatalogSnapshot`, or atomically commit one explicit
`CatalogBatch`.

All identity, validation, revision, delta, and canonical-handle semantics remain pure in `trading-reference-data` under
`CatalogModel.commit`. The port has no point lookup, reset, stream, clock, telemetry, persistence, transaction handle,
or concrete effect runtime. The runtime foundation Slice supplies the first interpreter and its lifecycle/concurrency
mechanics.

Application workflows capture a snapshot once per coherent input boundary and perform all asset, dimension, and grid
resolution directly against that snapshot.

The reusable `LiveCatalogContract[F]` lives in application test sources and describes observable behavior without
depending on MUnit, Cats Effect, or a synchronous effect runner. Interpreter suites supply construction, sequencing,
and concurrency for their own effect. The shared cases cover bootstrap, lookup, publication, idempotence, typed and
ordered failures, revision/delta conservation, coherent snapshots, and lineage rules. Each interpreter project adds
mechanism-specific resource, cancellation, contention, integration, and performance evidence beside those cases.
