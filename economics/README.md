# trading-economics

This is the transitional downstream aggregate for order, execution-scenario, fee-policy, and risk behavior. It depends
on `trading-instrument-economics`; it introduces no concrete effect runtime or production I/O.

## Current ownership

The implemented package roots currently contain:

- `trading.order`: immutable order instructions consuming an explicit instrument;
- `trading.scenario`: hypothetical checked execution scenarios;
- `trading.fee.policy`: fee calculation, schedule composition, attribution, and PnL orchestration;
- `trading.risk`: exact downside and discrete-lot sizing.

The pure dependency supplies the trust transition:

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

This aggregate reflects implementation order, not the final physical graph. It contains no production
`trading.economics.instrument` classes; those are packaged only by `trading-instrument-economics`. Later proposals move
each downstream package into its final artifact and remove this aggregate.

## Proposed migrations

The active architecture portfolio assigns the aggregate's responsibilities as follows:

| Current concern | Proposed owner | Proposal |
| --- | --- | --- |
| Snapshot-based stable-ID instrument assembly | `trading-instrument-economics` (implemented) | 3–4 |
| Instrument meaning, lots, prices, valuation, economic fee values, P&L | `trading-instrument-economics` (implemented) | 4, `establish-pure-instrument-economics` |
| Order instructions | `trading-order-model` | 5, `separate-order-and-execution-scenario-modules` |
| Hypothetical execution evidence | `trading-execution-scenario` | 5 |
| Venue/account/tier fee policy | `trading-fee-policy` | 6, `introduce-pure-fee-policy-module` |
| Downside and sizing procedures | `trading-risk` | 7, `introduce-pure-risk-module` |

The instrument-economics artifact is implemented by Proposal 4. Proposals 5–7 own the remaining physical moves and
remove this aggregate only after its final responsibility has moved.

The [architecture and functional design charter](../docs/design-principles.md) governs the migrations. The
[portfolio audit](../docs/architecture-charter-audit.md) records dependency direction, boundary names, transition
ownership, and implementation order.
