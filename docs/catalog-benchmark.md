# Catalog Snapshot Benchmark Evidence

The non-published `benchmarks` SBT project uses sbt-jmh 0.4.8 (JMH 1.37) and depends on
`trading-reference-data`, `trading-application`, and `trading-runtime`. It is outside root aggregation, so ordinary
`clean test` compiles and tests production modules without running long measurements.

`CatalogSnapshotLookupBenchmark.resolveAsset` measures direct immutable snapshot lookup over 1,024 canonical assets.
The benchmark captures no live capability and contains no registry monitor, lock, atomic reference, grid scan, or
per-record cache. Reader cursor state is thread-local; the snapshot is shared and immutable.

The recorded directional run uses the repository JDK, one fork, three one-second warmups, five one-second measurements,
and separate one-reader and four-reader invocations. Results are evidence about contention shape on this machine, not a
normative throughput threshold.

Recorded on OpenJDK 26.0.2 (Apple Silicon host):

| Readers | Mean aggregate throughput | 99.9% JMH error |
| --- | ---: | ---: |
| 1 | 35,789,553 ops/s | ±891,267 ops/s |
| 4 | 136,622,997 ops/s | ±64,280,259 ops/s |

The four-reader aggregate mean was about 3.82 times the one-reader mean. The four-reader run had substantial variance,
so the useful directional observation is only that immutable readers did not serialize behind the removed shared
monitor; the values are not a portable service-level claim.

## Live catalog runtime paths

`LiveCatalogRuntimeBenchmark` measures the runtime interpreter in four directions. `captureSnapshot` performs one live
snapshot read. `captureOnceAndResolveAll` performs that read once and then resolves 1,024 assets from the immutable
snapshot; it deliberately does not cross the live `Ref` boundary once per asset. `commitUncontended` publishes one asset
into a fresh interpreter. `commitContended` uses four thread-local unique command sequences against one shared
interpreter, requires every operation to publish, and therefore exercises competing atomic state transitions rather
than repeatedly submitting an already-applied idempotent bootstrap. Its score includes command allocation and pure
validation as well as coordinated publication. The shared interpreter is reset at each JMH iteration so warmup growth
does not carry into measurement iterations; it still grows within each one-second iteration as every operation adds a
definition, so the result is directional rather than a steady-state service threshold.

The recorded directional run used OpenJDK 26.0.2 on the same Apple Silicon host, JMH throughput mode, one fork, three
one-second warmups, and five one-second measurements. All cases used one thread except `commitContended`, which used
four. These machine-local results guide investigation and do not establish release thresholds:

| Runtime path | Threads | Mean aggregate throughput | 99.9% JMH error |
| --- | ---: | ---: | ---: |
| Capture one snapshot | 1 | 171,617.667 ops/s | ±46,477.338 ops/s |
| Capture once, resolve 1,024 assets | 1 | 24,257.954 ops/s | ±2,860.145 ops/s |
| Publish uncontended | 1 | 118,339.245 ops/s | ±9,322.987 ops/s |
| Publish under contention | 4 | 3,659.590 ops/s | ±69.860 ops/s |
