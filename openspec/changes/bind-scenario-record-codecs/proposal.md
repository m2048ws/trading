## Why

Scenario-record callers currently repeat one instrument and, during reconstruction, one immutable catalog snapshot
across both order and round-trip operations. RFC-0008/S-04 requires owner-local bound contexts that make those calls
concise while preserving the codec boundary's exact dependent types, coherent-snapshot semantics, and stable wire
contract.

## What Changes

- Add one immutable scenario encoder bound only to an exact instrument, covering order- and round-trip-scenario record
  construction plus canonical wire encoding.
- Add one immutable scenario decoder bound to that exact instrument and one immutable `CatalogSnapshot`, covering
  record reconstruction, decode-and-reconstruct, and ordered batch reconstruction for both scenario families.
- Keep parsing, record-only encoding, schemas, decode limits, and record indices explicit and usable without a bound
  context.
- Preserve current V1 records, canonical JSON bytes, error alternatives and ordering, input locations, batch
  atomicity/order, null rejection, and exact reconstructed dependent types by delegating to the existing operations.
- Add focused characterization, golden/malformed/batch, coherent-snapshot, reuse, completed-artifact positive and
  negative compiler, dependency, and full repository regression evidence.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `versioned-boundary-codecs`: Add separate instrument-bound scenario encoding and immutable-snapshot-bound scenario
  reconstruction contexts while retaining all context-free record, parser, schema, limit, wire, and compatibility
  behavior.

## Impact

- Production code: `trading-boundary-codecs`, principally the order- and round-trip-scenario record facade.
- Verification: focused boundary-codec tests and completed-artifact Scala compiler fixtures in the adversarial
  boundary suite.
- Compatibility: existing record types and direct entry points remain available; no wire/schema version, scenario
  semantics, catalog authority, dependency coordinate, runtime effect, persistence boundary, or JDK-25 baseline
  changes.
