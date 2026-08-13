# Exact coordinate backing

`GridQuantity[D, G]` is backed directly by an opaque `BigInt` coordinate. It provides arbitrary-precision exactness using the Scala standard library while keeping the backing inaccessible to supported callers.

No production workload benchmark is claimed by this foundational change. There is no competing coordinate backing to select between, and exact arbitrary-precision semantics take precedence over speculative optimization.

If profiling later identifies coordinate arithmetic as a bottleneck, benchmark alternative private backing strategies against representative deployment workloads before changing the carrier.
