## Context

See [proposal.md](proposal.md) for motivation. The current `Instrument` trait owns the correct generative types and checked construction authority, but also exposes more than fifty operations in one flat sequence. Price callers normally receive text representing an actual market price, yet exact construction currently requires them to manufacture `Rate[base.D, quote.D]` by reopening dimension references already held by the instrument. Market-state construction repeats that pattern and uses names whose differences are difficult to recover at a call site.

The change is an API and implementation-organization refactor inside the existing `economics` module. The four delta specs preserve the already accepted exact formulas, grid and registry checks, path-dependent ownership, contextual fees, complete-scenario semantics, and exhaustive sizing behavior. `Instrument` and its owned value hierarchies must remain sealed against external and same-package-spoof construction.

## Goals / Non-Goals

**Goals:**

- Make the ordinary call path read in domain terms without requiring callers to reconstruct known dimensions.
- Make related operations discoverable through a small number of stable capability values while retaining `instrument.Price`, `instrument.Order`, and the other direct path-dependent type names.
- Reduce the amount of validation and calculation logic that must be understood in `Instrument.scala` at once without moving trusted construction authority to a spoofable package boundary.
- Establish a migration map and downstream compiler fixtures that make accidental API duplication or ownership weakening visible.
- Encode the fresh-independent-review gate in the implementation task list from the start.

**Non-Goals:**

- Change any valuation, conversion, fee, PnL, risk, or sizing formula.
- Add venue/product-family branches, BitMEX wire models, execution lifecycle state, persistence schemas, or network behavior.
- Add a `String` decoder to the economics core or prescribe one number grammar for every venue.
- Introduce collection wrappers whose only purpose is to contain a `Vector`.
- Preserve source compatibility with the unreleased flat API.
- Reopen the settled quantities/domain module boundary.

## Decisions

### 1. Keep owned types on `Instrument`; move operations into owned capabilities

`Instrument` remains the generative aggregate. Identity, asset roles, grid/contract metadata, `Lots`, `PositionLots`, `Price`, and the remaining owned domain types stay path-dependent on the stable instrument value. Lot and signed-position primitives remain at the root because they are the aggregate's minimal exposure representation. The root adds seven stable, read-only capability values:

```scala
sealed trait Instrument extends JavaSerializationUnsupported:
  val id: InstrumentId
  val underlying: UnderlyingId
  val base: AssetRef
  val quote: AssetRef
  val position: AssetRef
  val settle: AssetRef

  type Lots
  type PositionLots
  type Price
  // Existing instrument-owned domain types remain here.

  // Existing lot/position primitives remain here.
  def lots(count: BigInt): Either[EconomicsError, Lots]
  def lotCount(value: Lots): BigInt
  def lotsQuantity(value: Lots): Quantity[position.D]
  def positionLots(side: Side, value: Lots): PositionLots
  def positionLotCount(value: PositionLots): BigInt
  def positionQuantity(value: PositionLots): Quantity[position.D]
  def flatPosition: PositionLots

  val prices: Prices
  val market: Market
  val orders: Orders
  val scenarios: Scenarios
  val fees: Fees
  val valuation: Valuation
  val sizing: Sizing
```

Each capability is an instrument-owned sealed interface. Its implementation is a stable object/value created by the private `InstrumentImpl`; it is not another domain wrapper and carries no independently supplied state. All signatures continue to mention the outer instrument's member types, so `other.prices` cannot construct or consume `instrument.Price`.

Top-level service objects parameterized by a structural instrument type were rejected because they make dependent call sites harder to read and create more opportunities to lose a stable path. Moving owned types under capabilities, such as `instrument.prices.Price`, was rejected because it adds a second path level without improving ownership. Adding more root methods with naming prefixes was rejected because it preserves the original discoverability problem.

### 2. Apply one rule for scalar coefficients versus typed rates

The public boundary follows this rule:

1. When the receiver fixes both endpoints, the ordinary method accepts `Rational`.
2. When explicit authoritative `AssetRef` values in the same call fix both endpoints, the ordinary method accepts `Rational` and checks registry identity at runtime.
3. When a caller already has a rate produced by typed arithmetic, a parallel method accepts that endpoint-typed `Rate` directly.
4. A bare coefficient is never accepted when its endpoint meaning would otherwise be ambiguous.

This removes ceremony, not validation. For example, `instrument.prices.exact(coefficient)` can only mean quote per base for that instrument. Internally it creates the same typed rate using the instrument's authoritative dimensions and follows the same exact grid-narrowing path as the typed form.

The price capability is shaped as:

```scala
sealed trait Prices extends JavaSerializationUnsupported:
  def exact(coefficient: Rational): Either[EconomicsError, Price]
  def fromRate(value: Rate[base.D, quote.D]): Either[EconomicsError, Price]

  // Deliberately low-level reconstruction from a grid coordinate.
  def fromTicks(ticks: PositiveWhole): Price

  def quantize(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])]

  def quantizeRate(
    value: Rate[base.D, quote.D],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])]

  def ticks(value: Price): BigInt
  def coefficient(value: Price): Rational
  def rate(value: Price): Rate[base.D, quote.D]
```

`fromTicks` takes `PositiveWhole` so positivity is visible before reconstructing a trusted grid coordinate. It is intended for persisted coordinates or venue tick data, not ordinary market-price input. Exact and quantizing scalar methods are the normal adapter path.

The same endpoint rule updates explicit additional conversions:

```scala
SettlementConversion.positive(source, target, coefficient)
SettlementConversion.fromRate(source, target)(typedRate)
```

Accepting only `Rate` was rejected because it forces callers to manufacture evidence already present on the receiver or explicit asset references. Accepting only `Rational` was rejected because it would throw away useful static endpoint evidence from derived typed arithmetic. An implicit conversion from `Rational` to an arbitrary `Rate` was rejected because its endpoints would be hidden and potentially inferred in surprising contexts.

### 3. Name market-state constructors by the fact each one establishes

The primary scalar API is:

```scala
sealed trait Market extends JavaSerializationUnsupported:
  def quoteSettled(
    price: Price,
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def baseSettled(
    price: Price,
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def fromQuoteAnchor(
    price: Price,
    quoteToSettle: Rational,
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def fromBaseAnchor(
    price: Price,
    baseToSettle: Rational,
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def fromAnchors(
    price: Price,
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  // Advanced forms preserve caller-derived endpoint types.
  def fromQuoteRate(
    price: Price,
    quoteToSettle: Rate[quote.D, settle.D],
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def fromBaseRate(
    price: Price,
    baseToSettle: Rate[base.D, settle.D],
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def fromRates(
    price: Price,
    baseToSettle: Rate[base.D, settle.D],
    quoteToSettle: Rate[quote.D, settle.D],
    additionalConversions: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def convertToSettle(source: AssetRef, conversions: SettlementConversions)(
    value: Quantity[source.D]
  ): Either[EconomicsError, Quantity[settle.D]]
```

The scalar methods construct typed rates and delegate to the typed methods; all paths converge on one private checked implementation. `quoteSettled` means quote actually is settlement. `fromQuoteAnchor` means quote is converted into a distinct or equal settlement role using the supplied anchor. `fromAnchors` means both independently supplied facts are checked against price coherence. The typed forms use distinct names rather than overloaded default arguments, avoiding Scala's overloaded-default ambiguity and keeping error messages legible.

A single constructor with optional base and quote anchors was rejected because combinations of `None` encode several modes and make invalid absence/duplication states possible. A public `quote is settlement` Boolean or product-family enum was rejected because the registered role identities already establish that fact. Keeping the old `For`, `From`, and `Checked` naming family was rejected because those words do not identify the economic fact at a call site.

### 4. Keep text parsing in the adapter that owns the grammar

The intended flow from typical market data is:

```scala
def decodePrice(instrument: Instrument)(raw: String): Either[AdapterError, instrument.Price] =
  for
    coefficient <- VenueDecimal.parseExact(raw)
    price <- instrument.prices.exact(coefficient).left.map(AdapterError.InvalidPrice.apply)
  yield price
```

`VenueDecimal.parseExact` is illustrative adapter code, not a new economics API. A venue can reject fractions, accept or reject exponent notation, constrain scale, and retain its own diagnostics before producing a `Rational`. Applications whose grammar matches the quantities parser may use `Rational.parse`; the economics module does not silently adopt that broader grammar as a venue contract.

A core `price(String)` method was rejected because parsing policy and economic grid membership are separate failures owned by different boundaries. A floating-point intermediate was rejected because it would discard the exact decimal value before grid validation.

### 5. Use capability-local verbs and direct plural collections

The remaining public operations move as follows:

| Current flat area | Capability-oriented area |
|---|---|
| `price`, `priceExactly`, `quantizePrice`, price observers | `prices.fromTicks`, `prices.exact`/`fromRate`, `prices.quantize`/`quantizeRate`, `prices` observers |
| `marketStateForQuote`, `marketStateForBase` | `market.quoteSettled`, `market.baseSettled` |
| `marketStateFromQuote`, `marketStateFromBase`, `marketStateChecked` | `market.fromQuoteAnchor`, `market.fromBaseAnchor`, `market.fromAnchors` plus typed-rate forms |
| settlement conversion lookup | `market.convertToSettle` |
| activation, visibility, price instruction, and order constructors | `orders` with local verbs such as `market`, `limit`, `stopMarket`, and `stopLimit` |
| evidence, peg, slice, order-scenario, and round-trip constructors | `scenarios` with `order` and `roundTrip` as the aggregate checks |
| minimum, quantization, percentage, fee-line, and schedule helpers | `fees` with `none` and `combine` for schedule values |
| settle-per-position, position value, price PnL, and fee-inclusive PnL | `valuation` with `settlePerPosition`, `positionValue`, `pricePnl`, and `pnl` |
| downside risk and position sizing | `sizing.downsideRisk` and `sizing.maxLots` |

Representative capability shapes are:

```scala
sealed trait Orders extends JavaSerializationUnsupported:
  def checked(/* independent mechanics */): Either[EconomicsError, Order]
  def market(/* side, lots, position effect */): Either[EconomicsError, Order]
  def limit(/* side, lots, limit, mechanics */): Either[EconomicsError, Order]
  def stopMarket(/* side, lots, trigger, mechanics */): Either[EconomicsError, Order]
  def stopLimit(/* side, lots, trigger, limit, mechanics */): Either[EconomicsError, Order]
  // Activation, price-instruction, and visibility constructors live here too.

sealed trait Scenarios extends JavaSerializationUnsupported:
  def slice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice
  def order(
    order: Order,
    matchedSlices: Vector[LiquiditySlice],
    activationEvidence: Option[ActivationEvidence] = None,
    pegResolution: Option[PegResolution] = None
  ): Either[EconomicsError, OrderScenario]
  def roundTrip(entry: OrderScenario, exit: OrderScenario): Either[EconomicsError, RoundTripScenario]

sealed trait Fees extends JavaSerializationUnsupported:
  def minimumCharge(/* explicit asset and quantities */): Either[EconomicsError, /* quantity */]
  def quantize(/* explicit asset, grid, kind, amount, policy */): Either[EconomicsError, Fee]
  def percentage(/* explicit asset, grid, kind, basis, rate, policy */): Either[EconomicsError, Fee]
  def line(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine]
  def none: FeeSchedule
  def combine(componentSchedules: Vector[FeeSchedule]): FeeSchedule

sealed trait Valuation extends JavaSerializationUnsupported:
  def settlePerPosition(state: MarketState): Rate[position.D, settle.D]
  def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D]
  def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D]
  def pnl(roundTrip: RoundTripScenario, feeSchedule: FeeSchedule): Either[EconomicsError, Pnl]

sealed trait Sizing extends JavaSerializationUnsupported:
  def downsideRisk(pnl: Pnl): Quantity[settle.D]
  def maxLots(
    riskBudget: Quantity[settle.D],
    cap: PositiveWhole,
    feeSchedule: FeeSchedule
  )(
    scenarioFor: Lots => Either[EconomicsError, RoundTripScenario]
  ): Either[EconomicsError, Option[Lots]]
```

The exact full signatures are derived by moving the existing parameters under these capability-local names; this change does not remove mechanics or policy inputs.

Vectors remain visible where plurality is semantic:

- `additionalConversions`: zero or many extra source assets;
- `matchedSlices`: one or many complete matched-price/liquidity assumptions;
- `FeeSchedule.assess`: zero or many attributed fee components;
- `componentSchedules`: zero or many policies to compose;
- `convertedFeeLines`: zero or many explainable PnL components.

Non-emptiness, uniqueness, lot conservation, attribution, and conversion coherence are checked by the aggregate that owns those invariants. `LiquidityPlan`, `FeeAssessment`, and `AdditionalConversions` pass-through wrappers are therefore not introduced. A future wrapper is justified only if it acquires a stable domain identity, construction authority, or reusable invariant that cannot honestly remain at the aggregate boundary.

### 6. Extract only authority-free rules from `Instrument.scala`

The public capability interfaces and the private implementations of sealed, path-owned values remain in the same compilation unit where Scala sealing and private construction can enforce authority. The private `InstrumentImpl` wires each stable capability value to the same instrument-owned grids and witnesses.

Pure checks and calculations that neither construct trusted public values nor cast existential witnesses may move to concern-named package files, for example price-grid checks, market coefficient coherence, order compatibility predicates, trigger comparisons, lot-sum checks, fee sign arithmetic, PnL sums, and candidate traversal. Such helpers operate on exact scalar data, enums, and explicit callbacks. They do not expose constructors and do not become public domain APIs.

Splitting every nested implementation into separate package-visible classes was rejected because `private[economics]` is reachable by source that declares the same package and would fail the existing package-spoof boundary. Leaving all rules inline was rejected because capability namespaces alone would improve call sites but leave the maintenance readability problem largely unchanged. A mandatory one-file-per-capability rule was rejected because some small rules are clearer together and line-count targets encourage mechanical fragmentation.

### 7. Make the public boundary and review gate prevent regression

Behavioral tests are reorganized by capability, but existing exact examples and laws remain. One downstream positive fixture demonstrates the normal adapter flow from parsed rational price through market construction, orders, scenarios, fees, valuation, and sizing. Negative compiler fixtures continue to prove cross-instrument rejection and private construction, and add checks that the superseded flat names are absent.

Every scalar/rate pair receives equivalence tests over successful values and matching typed failures. The package-spoof fixture is rerun after rule extraction. Review checks the root API against a simple placement rule: a new operation stays at the root only when it is identity, immutable instrument metadata, an owned type declaration, or a primitive lot/position operation; all other operations belong to the narrowest existing capability.

The implementation task list ends with a fresh independent-review task. Implementation and remediation workers may make and test changes but may not mark that final gate complete themselves. This prevents a fully checked task list from being mistaken for independent approval.

Snapshotting the entire textual API was rejected because it would make harmless signature formatting noisy. Relying only on same-module tests was rejected because they cannot demonstrate the public artifact boundary or package-spoof resistance.

### 8. Make the quantities main-to-test compiler boundary immutable

The completed implementation exposed two failures in `quantities / Test / compileIncremental`: the test compiler first could not read `GridConstraint.tasty`, then an isolated single-process clean run could not read `DivisionByZero.tasty`. In both cases quantities main compilation had completed and the test compiler was consuming the ordinary `quantities` `Compile / classes` directory. The second failure occurred with no competing SBT/JVM process targeting the checkout, and the named TASTy file existed again after the failed compiler transaction. The defect is therefore a transient observation at the mutable main-output-to-test-compiler boundary, not evidence for the earlier external-process hypothesis. Live task inspection also proves that the packaged quantities/economics artifact diamond occurs only later under the adversarial boundary, so an edge added there cannot causally repair either recorded failure.

Only `quantities / Test / internalDependencyClasspath` changes: it consumes the completed `quantities / Compile / packageBin` JAR instead of `Compile / classes`. Packaging depends on successful main compilation and materializes the classfiles and TASTy files as one completed immutable compiler input before test compilation begins. Test external dependencies remain on the ordinary external classpath. This does not enable `Compile / exportJars`, change `Compile / exportedProducts`, alter economics or adversarial dependency classpaths, or change quantities source, public API, lifecycle semantics, or packaged artifact contents. A disposable snapshot with this session-only setting compiled all 27 quantities main sources, showed only the completed quantities JAR on the test internal dependency classpath, and then compiled all 61 quantities test sources.

SBT target directories remain mutable build-process state, so the supported validation model for one checkout is still one active SBT invocation at a time; concurrent automation uses separate checkouts or independently configured target trees. That process contract is no longer claimed as the remediation. The worker runs one unretried `sbt -batch clean test` on the corrected wiring, then focused gates sequentially. Retry, sleep, filesystem polling, hard-coded target paths, publication, global `exportJars`, and repository-wide build serialization remain rejected. Any failure in the corrected authoritative run remains a blocker.

## Risks / Trade-offs

- [Capability views add seven small interface values] → Implement them as stable instrument-owned values with no caller-supplied state and verify that their only role is namespacing and access to the existing owner.
- [Scalar inputs could appear less dimension-safe] → Permit them only when endpoints are fixed by the receiver or explicit asset references, retain typed-rate forms, and test both paths for observational equivalence.
- [Renaming many calls creates migration churn] → Make the break before the first release, publish the mapping above, and update all internal and downstream examples atomically.
- [Moving methods may only relocate a large implementation] → Extract authority-free rules by concern and review the remaining private aggregate for construction/ownership responsibilities only.
- [Rule extraction could leak construction authority] → Keep sealed implementations and witness casts private in the trusted compilation unit and rerun same-package-spoof compiler tests.
- [`PositiveWhole` adds ceremony for coordinate reconstruction] → Reserve `fromTicks` for low-level coordinate data; ordinary price strings parse to `Rational` and use `prices.exact`.
- [Distinct scalar and typed method names enlarge each capability] → Prefer explicit `fromQuoteRate`/`fromRates` names over overloaded defaults; keep both paths delegating to one check.
- [Direct vectors permit temporarily invalid collections] → Validate all collection-wide invariants atomically at the market-state, scenario, fee-line, and PnL boundaries before exposing valid aggregates.
- [A same-project test compiler can observe a transiently unavailable TASTy file in mutable main output] → Make only the quantities test internal classpath consume its completed main JAR, audit that the JAR contains the expected TASTy files, and leave ordinary dependent-project classpaths unchanged.
- [Overlapping SBT processes can mutate one checkout's target tree concurrently] → Continue to support one active invocation per checkout and require separate checkouts or target trees for concurrent automation; do not mistake that process contract for the main-to-test remediation.

## Migration Plan

This is a pre-release source migration with no stored-data, wire-format, or deployment migration.

1. Introduce the sealed capability interfaces and private stable capability implementations while preserving the existing calculation functions as private delegates.
2. Add scalar price/conversion entry points and prove them equivalent to the typed-rate paths.
3. Move public order, scenario, fee, valuation, and sizing operations to their capability-local names; remove the superseded flat public methods in the same change.
4. Extract authority-free rules only after ownership and package-spoof tests remain green.
5. Migrate module tests, examples, and downstream compiler fixtures using the mapping in this design.
6. Inspect both missing-TASTy failures and the current task graph, remove the non-causal later-consumer workaround, and make only quantities tests consume the completed quantities main JAR instead of mutable main classes.
7. With no competing SBT process targeting the checkout, run one authoritative unretried clean validation, audit the packaged quantities artifact and test classpath, then run focused economics tests, downstream adversarial/compiler tests, and formatting sequentially.
8. Obtain fresh independent approval before archive or commit-readiness finalization.

Rollback reverts this change to the committed initial economics API. Because the artifact is unreleased and no persistence representation changes, rollback requires no compatibility shim or data transformation.
