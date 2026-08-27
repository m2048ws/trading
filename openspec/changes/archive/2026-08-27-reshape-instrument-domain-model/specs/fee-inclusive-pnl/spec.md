## MODIFIED Requirements

### Requirement: Contextual instrument-bound fee schedules
A fee schedule SHALL remain bound to one stable instrument owner and SHALL remain a separate contextual value rather than immutable instrument metadata. A schedule MAY capture venue, account, tier, effective version, and other policy inputs. It SHALL calculate zero or more fees from a complete order scenario and SHALL be able to inspect the order's explicit activation and execution alternatives and every liquidity slice.

The instrument-owned `fees` capability SHALL construct a validated `FeeDenomination` from an explicit registered asset, matching registered grid, and quantization policy. A denomination SHALL retain the dependent relationship between its asset and grid, validate registry and dimension coherence once, and provide subsequent fee construction without requiring callers to repeat or manually align those witnesses. The capability SHALL also provide checked fee-line attribution, the no-fee schedule, and direct schedule composition.

A fee schedule SHALL return its zero, one, or many checked fee lines directly as an immutable vector. Schedule composition SHALL accept component schedules directly as an immutable vector. The public schedule contract SHALL continue to permit percentage, flat, tiered, minimum, order-type-sensitive, visibility-sensitive, and multi-component policy without product-family branches in `Instrument`.

#### Scenario: Reuse a validated denomination
- **WHEN** a schedule charges several components in one asset, grid, and quantization policy
- **THEN** it validates that denomination once and uses it for each fee calculation without repeating existential grid parameters

#### Scenario: Reject an incoherent denomination
- **WHEN** a fee asset and grid have different dimensions or registry provenance
- **THEN** denomination construction fails before a fee amount is calculated

#### Scenario: Apply different maker and taker rates
- **WHEN** a mixed-liquidity scenario contains maker and taker slices under a schedule with distinct rates
- **THEN** the schedule evaluates the applicable quantities under their respective rates

#### Scenario: Capture account-specific terms outside Instrument
- **WHEN** two accounts have different fee tiers for the same stable instrument
- **THEN** they use distinct fee schedules and may reuse or select different validated denominations without constructing different instruments

#### Scenario: Inspect order mechanics when required
- **WHEN** venue policy assigns special treatment to an execution instruction or visibility alternative
- **THEN** its schedule can inspect that explicit alternative in addition to slice liquidity role

#### Scenario: Compose zero or many schedule components directly
- **WHEN** a caller has an immutable vector of component fee schedules
- **THEN** `fees` combines their checked fee-line vectors without a pass-through collection wrapper

### Requirement: Signed grid-constrained fees
A calculated `Fee` SHALL retain its semantic kind, original registered asset, validated fee denomination, signed grid amount, exact quantization residual, and unrounded exact amount. These intrinsic observations SHALL be available directly from the fee value rather than through functions requiring the fee to be passed back to its instrument. Fee amount sign SHALL remain from the account perspective: a charge is negative and a rebate is positive. A quoted percentage `FeeRate` SHALL retain the policy convention that a positive rate is a charge and a negative rate is a rebate.

Every exact fee basis calculation SHALL remain rational until the selected validated denomination explicitly quantizes it. Percentage fee construction SHALL accept the semantic kind, nonnegative basis in the denomination asset, and fee rate; exact quantization SHALL accept kind and unrounded amount in that asset. Both SHALL reuse the denomination's already checked asset, grid, and policy. The result SHALL conserve the exact unrounded amount as grid amount plus residual. A fee asset or grid SHALL NOT be inferred solely from instrument settlement.

#### Scenario: Calculate a taker charge
- **WHEN** a positive taker rate is applied to a nonnegative basis through a validated denomination
- **THEN** the resulting fee contribution is negative in that denomination's asset

#### Scenario: Calculate a maker rebate
- **WHEN** a negative maker rate is applied to a nonnegative basis through a validated denomination
- **THEN** the resulting fee contribution is positive in that denomination's asset

#### Scenario: Preserve fee rounding residual
- **WHEN** an exact fee is not representable on the denomination grid
- **THEN** the fee directly exposes the quantized amount, exact residual, and unrounded amount that satisfy conservation

#### Scenario: Charge a third asset
- **WHEN** a policy constructs a denomination for an asset other than base, quote, or settle
- **THEN** the fee retains that asset and its own checked grid rather than being relabeled as settlement currency

#### Scenario: Keep denomination ownership coherent
- **WHEN** a fee denomination belongs to one instrument owner
- **THEN** it cannot construct fees or schedules typed as belonging to another instrument owner
