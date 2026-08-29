# Trading Application

`trading-application` owns effect-polymorphic capabilities whose execution genuinely varies by interpreter. Its initial
surface is only `LiveCatalog[F]`: capture one immutable `CatalogSnapshot`, or atomically commit one explicit
`CatalogBatch`.

All identity, validation, revision, delta, and canonical-handle semantics remain pure in `trading-reference-data` under
`CatalogModel.commit`. The port has no point lookup, reset, stream, clock, telemetry, persistence, transaction handle,
or concrete effect runtime. Proposal 8 supplies the first interpreter and its lifecycle/concurrency mechanics.

Application workflows capture a snapshot once per coherent input boundary and perform all asset, dimension, and grid
resolution directly against that snapshot.
