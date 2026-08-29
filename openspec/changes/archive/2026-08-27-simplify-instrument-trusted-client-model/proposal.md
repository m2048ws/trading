## Why

The economics model still pays a large readability and maintenance cost to resist hostile callers: generative owner types, JVM issuance gates, forwarding authorities, abstract-carrier/private-implementation pairs, and adversarial bytecode fixtures dominate otherwise straightforward domain code. The economics artifact is unreleased and is expected to have one benign client for the foreseeable future, so it should optimize for clear domain modeling and accidental-error prevention rather than anti-forgery hardening.

## What Changes

- **BREAKING** Adopt an explicit trusted-client boundary for the `economics` artifact: ordinary Scala or Java callers are assumed not to forge constructors, subclass implementation types, use hostile bytecode, or bypass constructors through deserialization.
- **BREAKING** Remove `Instrument.Owner`, the owner type parameter carried across economics values, and compile-time path-dependent cross-instrument rejection.
- **BREAKING** Remove `JvmOwnerAuthority`, `Instrument.OwnerAuthority`, authority forwarding, gated carrier constructors, and abstract-carrier/private-implementation pairs used only to make values non-forgeable.
- Replace those carrier pairs with direct, closed domain data types and small smart constructors. Keep focused concern APIs where they own policy or multi-value operations, but let ordinary domain alternatives and validated values be represented directly.
- Preserve simple runtime instrument identity on instrument-dependent values where it prevents accidental cross-instrument aggregation. Validate that identity at aggregate boundaries; it is a correctness diagnostic, not a security boundary, and trusted callers can construct matching data directly.
- Remove economics-specific hostile-client, same-package-spoof, Java-access, authority-bytecode, and constructor-bypassing serialization requirements and tests. This does not weaken the separate construction, authority, provenance, or serialization requirements of the `quantities` artifact.
- Retain positive/refined lots and prices, explicit activation/pricing/visibility/scenario alternatives, exact arithmetic, contextual grids, registry provenance, instrument-definition coherence, market/conversion checks, order/scenario conservation checks, fee denomination checks, sizing completeness, and typed failures.
- Reorganize the implementation around direct domain types, aggregate validators, and cohesive concern modules so `Instrument.scala` no longer acts as an issuance kernel or a catalogue of every implementation class.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `instrument-economics`: Replace non-forgeable generative ownership with a trusted-client model and simple runtime instrument coherence while preserving economic and provenance validation.
- `order-scenarios`: Remove owner-indexed order/scenario types and compile-time cross-instrument rejection, retaining direct closed alternatives and runtime aggregate coherence checks.
- `fee-inclusive-pnl`: Remove owner-indexed fee types and anti-forgery construction while retaining validated denominations, scenario attribution, conversion, quantization, and exact PnL.

## Impact

- Affects the pre-release public API and implementation of `economics`, its Scala and Java boundary fixtures, tests, examples, and the three canonical economics specifications.
- Expected removals include the Java authority gate, owner authority forwarding, owner type parameters and aliases, private implementation class families, economics authority audits, and economics use of constructor-bypassing serialization hardening.
- Expected additions are direct closed data types, compact smart constructors, explicit runtime instrument identity where aggregation needs it, and focused tests for accidental mixing and retained economic invariants.
- Does not change the public API or trust model of `quantities`, exact formulas, grid provenance, registry ownership, venue parsing, execution lifecycle scope, accounts, or ledgers.
