# trading-boundary-codecs

This artifact is the pure boundary representation owner under `trading.codec`. It sits above quantities, reference
data, instrument economics, the order model, and execution scenarios; none of those lower artifacts depends back on
codecs.

The production classpath is intentionally limited to those five domain artifacts, Cats Core for internal pure
validation/traversal, and Jackson Core 3.x for the later package-private strict streaming adapter. It contains no fee
policy, risk, application, runtime, effect, stream, persistence, network-client, clock, transaction, tracing, metrics,
Jackson Databind/Scala module, Circe, schema-validator, or JCS-oracle dependency.

NetworkNT JSON Schema Validator 3.x and the RFC 8785-listed Java JSON Canonicalization implementation are test-only
independent checks. Schemas belong under `src/main/resources/trading/codec/schema`; canonical golden vectors belong
under `src/test/resources/trading/codec/golden`.

Task Group 2 establishes only this physical artifact, package root, dependency direction, resources, and completed-JAR
verification. Concrete paths, errors, limits, schema algebra, record families, and public encode/parse/reconstruct
operations are introduced by their later owning Task Groups; this module does not claim those APIs yet.

Build the focused boundary with:

```text
sbt boundaryCodecs/test adversarialBoundary/test
```
