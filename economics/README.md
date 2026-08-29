# trading-economics

This is the current production aggregate for pure instrument, order, execution-scenario, fee, valuation, P&L, and
risk-sizing behavior. It depends on `trading-quantities` and `trading-reference-data`; it introduces no concrete effect
runtime or production I/O.

## Current ownership

The implemented package `trading.economics.instrument` currently contains:

- stable-ID instrument definitions, pure snapshot-based assembly, proof-carrying specifications, and total trusted
  construction;
- prices, lots, positions, market state, and valuation;
- immutable order and hypothetical execution-scenario models;
- fee denomination/policy and fee-inclusive P&L;
- isolated-instrument risk sizing.

Instrument construction follows one explicit trust transition:

```text
InstrumentDefinition + CatalogSnapshot
  -> InstrumentAssembler.assemble / assembleFirst
  -> InstrumentSpec
  -> Instrument.fromSpec
```

`InstrumentDefinition` contains guarded stable IDs, full grid identities, and exact payoff coefficients only. Assembly
is pure, resolves every role through exactly the supplied immutable snapshot, accumulates independent failures in
deterministic stage/role order, and retains only the resolved handles and typed rates in `InstrumentSpec`. The final
`Instrument.fromSpec` step is total and performs no lookup, recasting, validation, or quantization. Adapters and the
future boundary-codec artifact own parsing and durable versioned records; neither `InstrumentSpec` nor `Instrument` is
a Java-serialization format.

This aggregate reflects implementation history. It is not the proposed final ownership graph. It consumes immutable
`Asset`, `DimensionHandle`, `GridHandle`, and `CatalogSnapshot` capabilities without receiving live catalog or runtime
coordination. It must not depend on future application, runtime, codec, persistence, network, or telemetry layers.
Quantities and reference data must not depend back on economics. The adversarial-boundary project consumes completed
packaged artifacts for tests.

## Proposed migrations

The active architecture portfolio assigns the aggregate's responsibilities as follows:

| Current concern | Proposed owner | Proposal |
| --- | --- | --- |
| Snapshot-based stable-ID instrument assembly | focused assembly boundary in this artifact | 3, `introduce-instrument-assembly-boundary` |
| Instrument meaning, lots, prices, valuation, economic fee values, P&L | `trading-instrument-economics` | 4, `establish-pure-instrument-economics` |
| Order instructions | `trading-order-model` | 5, `separate-order-and-execution-scenario-modules` |
| Hypothetical execution evidence | `trading-execution-scenario` | 5 |
| Venue/account/tier fee policy | `trading-fee-policy` | 6, `introduce-pure-fee-policy-module` |
| Downside and sizing procedures | `trading-risk` | 7, `introduce-pure-risk-module` |

These artifacts and APIs are proposed, not currently available. Proposals 3–7 own the physical moves and must keep
each intermediate repository state buildable. Proposal 7 removes this aggregate only after its final responsibility has
moved.

The [architecture and functional design charter](../docs/design-principles.md) governs the migrations. The
[portfolio audit](../docs/architecture-charter-audit.md) records dependency direction, boundary names, transition
ownership, and implementation order.
