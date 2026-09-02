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

Task Group 3 adds the public domain-owned structured paths, syntax locations, stage/limit vocabulary, typed violations,
non-empty aggregates, and validated immutable `DecodeLimits`. Its package-private kernel owns the immutable JSON AST,
strict Jackson adapter, restricted RFC 8785-compatible renderer, invariant wire-schema algebra, and Draft 2020-12
interpreter. Operational limits are intentionally absent from generated mathematical schemas.

Concrete exact primitives, envelopes, record families, and public encode/parse/reconstruct operations remain owned by
later Task Groups. Parser, Cats validation, JSON Schema validator, and JCS-oracle types do not appear in this artifact's
public codec foundation.

Build the focused boundary with:

```text
sbt boundaryCodecs/test adversarialBoundary/test
```
