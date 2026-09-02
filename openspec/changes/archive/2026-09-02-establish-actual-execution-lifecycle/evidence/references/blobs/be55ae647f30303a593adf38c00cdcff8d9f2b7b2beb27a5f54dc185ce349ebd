# Actual execution lifecycle

`trading-execution-lifecycle` is the pure domain owner for authoritative facts about actual order execution. It is
deliberately separate from immutable order intent and from hypothetical execution scenarios.

The module may depend on instrument economics, order model, quantities available through those boundaries, and pure
support libraries. It does not own transport, codecs, effects, streams, clients, persistence, telemetry, venue SDKs,
fees, risk, or application/runtime coordination.

Identity authority is explicit:

- the application supplies command, logical execution-order, and lineage identifiers;
- an execution source and account qualify native source event, order, fill, stream, and sequence identifiers;
- checked constructors validate representation only—they do not generate identifiers or infer authority from hashes,
  timestamps, payload equality, receipt order, or delivery attempts.

All public values in this module reject Java object serialization. Later lifecycle values retain these identities and
their source qualification rather than replacing them with locally generated receipts.
