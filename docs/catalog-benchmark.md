# Catalog Snapshot Benchmark Evidence

The non-published `benchmarks` SBT project uses sbt-jmh 0.4.8 (JMH 1.37) and depends only on
`trading-reference-data`. It is outside root aggregation, so ordinary `clean test` compiles and tests production modules
without running long measurements.

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
