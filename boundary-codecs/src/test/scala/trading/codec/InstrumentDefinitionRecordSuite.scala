package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

class InstrumentDefinitionRecordSuite extends FunSuite:
  test("frozen V1 products structurally preserve exact linear inverse and quanto definitions"):
    val fixture     = Fixture("exact")
    val definitions = Vector(
      fixture.definition("linear", fixture.quoteId, Rational.one, Rational.zero),
      fixture.definition("inverse", fixture.baseId, Rational.zero, Rational(-100)),
      fixture.definition("quanto", fixture.settleId, Rational(7, 13), Rational(-19, 23))
    )

    definitions.foreach: definition =>
      val record  = InstrumentDefinitionRecord.fromDefinition(definition)
      val encoded = InstrumentDefinitionRecord.encode(record).toOption.get
      assertEquals(InstrumentDefinitionRecord.parse(encoded), Right(record))
      assertEquals(InstrumentDefinitionRecord.decode(encoded), Right(definition))
      assertEquals(InstrumentDefinitionRecord.toDefinition(record), definition)
      assert(encoded.contains(definition.payoff.basePerPosition.numerator.toString))
      rejectSerialization(record)

    val fields = classOf[InstrumentDefinitionRecord.V1].getDeclaredFields.map(_.getName).toSet
    assertEquals(fields, Set("identity", "roles", "listing", "payoff"))
    val schema = InstrumentDefinitionRecord.schema().toOption.get
    Vector(
      "instrumentId",
      "underlyingId",
      "baseAssetId",
      "quoteAssetId",
      "positionAssetId",
      "settleAssetId",
      "positionLotGridIdentity",
      "priceGridIdentity",
      "basePerPosition",
      "quotePerPosition"
    ).foreach(field => assert(schema.contains(field)))
    Vector("revision", "lineage", "handle", "InstrumentSpec", "market", "venue", "productFamily").foreach(forbidden =>
      assert(!schema.contains(forbidden))
    )

  test("structural decoding accumulates independent identifiers and exact coefficients without catalog lookup"):
    val fixture   = Fixture("malformed")
    val valid     = fixture.wire(fixture.definition("valid", fixture.quoteId, Rational(2, 3), Rational.zero))
    val malformed = valid
      .replace("\"instrumentId\":\"malformed-valid\"", "\"instrumentId\":\"\"")
      .replace("\"underlyingId\":\"malformed-underlying\"", "\"underlyingId\":\" \"")
      .replace(s"\"baseAssetId\":\"${fixture.baseId.value}\"", "\"baseAssetId\":\"\"")
      .replace(
        "\"basePerPosition\":{\"denominator\":\"3\",\"numerator\":\"2\"}",
        "\"basePerPosition\":{\"denominator\":\"2\",\"numerator\":\"2\"}"
      )
      .replace(
        "\"quotePerPosition\":{\"denominator\":\"1\",\"numerator\":\"0\"}",
        "\"quotePerPosition\":{\"denominator\":\"2\",\"numerator\":\"0\"}"
      )

    val failures = InstrumentDefinitionRecord.decode(malformed).left.toOption.get.toVector
    assertEquals(
      failures.map(_.path.render),
      Vector(
        "$.payload.identity.instrumentId",
        "$.payload.identity.underlyingId",
        "$.payload.payoff.basePerPosition",
        "$.payload.payoff.quotePerPosition",
        "$.payload.roles.baseAssetId"
      )
    )
    assertEquals(failures.map(_.recordIndex).distinct, Vector(0))
    InstrumentDefinitionRecord.decodeAndAssemble(malformed, CatalogRoot.create().initialState.snapshot) match
      case Left(InstrumentDefinitionReconstructionFailure.Codec(violations)) =>
        assertEquals(violations, WireViolations.orderedDecode(failures))
      case other => fail(s"structural failures must suppress assembly, got $other")

  test("reconstruction delegates to canonical assembly and selects exact historical grids"):
    val fixture     = Fixture("assembly")
    val definitions = Vector(
      fixture.definition("linear", fixture.quoteId, Rational.one, Rational.zero),
      fixture.definition("inverse", fixture.baseId, Rational.zero, Rational(-100)),
      fixture.definition("quanto", fixture.settleId, Rational(7, 13), Rational(-19, 23))
    )

    definitions.foreach: definition =>
      val wire          = fixture.wire(definition)
      val decoded       = InstrumentDefinitionRecord.decode(wire).toOption.get
      val direct        = Instrument.fromSpec(InstrumentAssembler.assemble(decoded, fixture.snapshot).toOption.get)
      val reconstructed = InstrumentDefinitionRecord
        .decodeAndAssemble(wire, fixture.snapshot)
        .toOption
        .get
      assertEquivalent(reconstructed, direct)
      assertEquals(reconstructed.identity, definition.identity)
      assertEquals(reconstructed.basePerPosition.coefficient, definition.payoff.basePerPosition)
      assertEquals(reconstructed.quotePerPosition.coefficient, definition.payoff.quotePerPosition)
      assertEquals(reconstructed.positionLotGrid.identity, fixture.positionGridV1.identity)
      assertEquals(reconstructed.priceGrid.identity, fixture.priceGridV1.identity)
      assert(GridHandle.reconcile(reconstructed.positionLotGrid, fixture.positionGridV2).isLeft)
      assert(GridHandle.reconcile(reconstructed.priceGrid, fixture.priceGridV2).isLeft)

  test("assembly retains ordered missing conflicting role grid and payoff diagnostics"):
    val fixture       = Fixture("failures")
    val emptyAndEqual = fixture.definition(
      "empty-equal",
      fixture.baseId,
      Rational.zero,
      Rational.zero,
      roles = Some(AssetRoleIds(fixture.baseId, fixture.baseId, fixture.positionId, fixture.settleId))
    )
    InstrumentDefinitionRecord
      .decodeAndAssemble(fixture.wire(emptyAndEqual), CatalogRoot.create().initialState.snapshot) match
      case Left(InstrumentDefinitionReconstructionFailure.Assembly(errors)) =>
        assertEquals(errors.violations.size, 8)
        assert(errors.violations(0).isInstanceOf[InstrumentAssemblyViolation.EqualBaseAndQuote])
        assert(errors.violations(1).isInstanceOf[InstrumentAssemblyViolation.EmptyPayoff])
        assertEquals(
          errors.violations.collect { case InstrumentAssemblyViolation.AssetResolution(_, role, _, _, _) => role },
          Vector(AssetRole.Base, AssetRole.Quote, AssetRole.Position, AssetRole.Settle)
        )
        assertEquals(
          errors.violations.collect { case InstrumentAssemblyViolation.GridResolution(_, role, _, _, _) => role },
          Vector(ListingGridRole.PositionLot, ListingGridRole.Price)
        )
      case other => fail(s"expected complete ordered assembly failures, got $other")

    val wrongDimensions = fixture.definition(
      "wrong-dimensions",
      fixture.quoteId,
      Rational.one,
      Rational.zero,
      listing = Some(ListingDefinition(fixture.priceGridV1.identity, fixture.positionGridV1.identity))
    )
    InstrumentDefinitionRecord.decodeAndAssemble(fixture.wire(wrongDimensions), fixture.snapshot) match
      case Left(InstrumentDefinitionReconstructionFailure.Assembly(errors)) =>
        assertEquals(
          errors.violations.collect { case value: InstrumentAssemblyViolation.GridDimension => value.role },
          Vector(ListingGridRole.PositionLot, ListingGridRole.Price)
        )
      case other => fail(s"expected both grid-dimension conflicts, got $other")

  test("batch reconstruction is coherent ordered and all-valid-or-indexed-errors"):
    val fixture = Fixture("batch")
    val first   = fixture.definition("first", fixture.quoteId, Rational.one, Rational.zero)
    val second  = fixture.definition("second", fixture.baseId, Rational.zero, Rational(-100))
    val third   = fixture.definition("third", fixture.settleId, Rational(5, 7), Rational(-11, 13))
    val valid   = Vector(first, second, third).map(fixture.wire)

    val success = InstrumentDefinitionRecord.reconstructBatch(valid, fixture.snapshot).toOption.get
    assertEquals(success.map(_.identity.id), Vector(first, second, third).map(_.identity.id))

    val malformed       = valid.head.replace("\"instrumentId\":\"batch-first\"", "\"instrumentId\":\"\"")
    val assemblyFailure = fixture.wire(
      fixture.definition(
        "assembly-failure",
        fixture.baseId,
        Rational.zero,
        Rational.zero,
        roles = Some(AssetRoleIds(fixture.baseId, fixture.baseId, fixture.positionId, fixture.settleId))
      )
    )
    val inputs   = Vector(valid(0), malformed, valid(1), assemblyFailure, valid(2))
    val failures = InstrumentDefinitionRecord
      .reconstructBatch(inputs, fixture.snapshot)
      .left
      .toOption
      .get
      .toVector
    assertEquals(failures.map(_.recordIndex), Vector(1, 3))
    assert(failures.head.failure.isInstanceOf[InstrumentDefinitionReconstructionFailure.Codec])
    assert(failures.last.failure.isInstanceOf[InstrumentDefinitionReconstructionFailure.Assembly])

    val oneRecord = limits(maxBatchRecords = 1)
    InstrumentDefinitionRecord.reconstructBatch(valid.take(2), fixture.snapshot, oneRecord) match
      case Left(errors) =>
        errors.head.failure match
          case InstrumentDefinitionReconstructionFailure.Codec(violations) =>
            violations.head match
              case WireDecodeViolation.Limit(limit) =>
                assertEquals(limit.limit, DecodeLimit.BatchRecords)
                assertEquals(limit.actual, 2L)
              case other => fail(s"expected batch limit, got $other")
          case other => fail(s"expected codec-stage batch limit, got $other")
      case other => fail(s"oversized batch must fail once before record work, got $other")

  test("equal records reassemble stable meaning under distinct fresh lineages without revision claims"):
    val firstFixture  = Fixture("lineage")
    val secondFixture = Fixture("lineage")
    val definition    = firstFixture.definition(
      "shared",
      firstFixture.quoteId,
      Rational.one,
      Rational.zero
    )
    val wire   = firstFixture.wire(definition)
    val first  = InstrumentDefinitionRecord.decodeAndAssemble(wire, firstFixture.snapshot).toOption.get
    val second = InstrumentDefinitionRecord.decodeAndAssemble(wire, secondFixture.snapshot).toOption.get

    assertEquals(first.identity, second.identity)
    assertEquals(first.positionLotGrid.identity, second.positionLotGrid.identity)
    assertEquals(first.priceGrid.identity, second.priceGrid.identity)
    assert(Asset.reconcile(first.roles.base, second.roles.base).isLeft)
    assert(GridHandle.reconcile(first.positionLotGrid, second.positionLotGrid).isLeft)
    val encoded = InstrumentDefinitionRecord.encodeDefinition(definition).toOption.get
    Vector("revision", "lineage", "snapshot", "handle", "market", "venue").foreach(forbidden =>
      assert(!encoded.contains(s"\"$forbidden\""))
    )

  private def assertEquivalent(left: Instrument, right: Instrument): Unit =
    assertEquals(left.identity, right.identity)
    assertEquals(left.roles.base.id, right.roles.base.id)
    assertEquals(left.roles.quote.id, right.roles.quote.id)
    assertEquals(left.roles.position.id, right.roles.position.id)
    assertEquals(left.roles.settle.id, right.roles.settle.id)
    assertEquals(left.positionLotGrid.identity, right.positionLotGrid.identity)
    assertEquals(left.priceGrid.identity, right.priceGrid.identity)
    assertEquals(left.basePerPosition.coefficient, right.basePerPosition.coefficient)
    assertEquals(left.quotePerPosition.coefficient, right.quotePerPosition.coefficient)

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  private def limits(maxBatchRecords: Int): DecodeLimits =
    DecodeLimits
      .create(
        maxPayloadCharacters = 1_000_000,
        maxPayloadUtf8Bytes = 4_000_000,
        maxNestingDepth = 32,
        maxBatchRecords = maxBatchRecords,
        maxObjectMembers = 128,
        maxArrayEntries = 10_000,
        maxStringCharacters = 4_096,
        maxIntegerDigits = 4_096,
        maxDimensionFactors = 256,
        maxCatalogCommands = 10_000,
        maxScenarioSlices = 10_000,
        maxMarketConversions = 1_024
      )
      .toOption
      .get
end InstrumentDefinitionRecordSuite

private final class Fixture(prefix: String):
  private val baseDefinition     = asset("base")
  private val quoteDefinition    = asset("quote")
  private val positionDefinition = asset("position")
  private val settleDefinition   = asset("settle")

  val baseId: AssetId     = baseDefinition.id
  val quoteId: AssetId    = quoteDefinition.id
  val positionId: AssetId = positionDefinition.id
  val settleId: AssetId   = settleDefinition.id

  private val positionDimension = DimKey.atom(positionDefinition.dimensionAtom)
  private val baseDimension     = DimKey.atom(baseDefinition.dimensionAtom)
  private val quoteDimension    = DimKey.atom(quoteDefinition.dimensionAtom)
  private val priceDimension    = DimKey.multiply(quoteDimension, DimKey.inverse(baseDimension))

  private val positionV1Definition = grid(positionDimension, "position-lots", 1, Rational(1, 1_000))
  private val positionV2Definition = grid(positionDimension, "position-lots", 2, Rational(1, 10_000))
  private val priceV1Definition    = grid(priceDimension, "quote-per-base", 1, Rational(1, 2))
  private val priceV2Definition    = grid(priceDimension, "quote-per-base", 2, Rational(1, 4))

  private val batch = CatalogBatch.of(
    CatalogCommand.RegisterAsset(baseDefinition),
    CatalogCommand.RegisterAsset(quoteDefinition),
    CatalogCommand.RegisterAsset(positionDefinition),
    CatalogCommand.RegisterAsset(settleDefinition),
    CatalogCommand.RegisterDimension(priceDimension),
    CatalogCommand.RegisterGrid(positionV1Definition),
    CatalogCommand.RegisterGrid(positionV2Definition),
    CatalogCommand.RegisterGrid(priceV1Definition),
    CatalogCommand.RegisterGrid(priceV2Definition)
  )
  private val state             = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get.state
  val snapshot: CatalogSnapshot = state.snapshot

  val positionAsset                               = snapshot.resolveAsset(positionId).toOption.get
  val priceHandle                                 = snapshot.resolveDimension(priceDimension).toOption.get
  val positionGridV1: GridHandle[positionAsset.D] =
    snapshot.resolveGrid(positionAsset.dimension)(positionV1Definition.key).toOption.get
  val positionGridV2: GridHandle[positionAsset.D] =
    snapshot.resolveGrid(positionAsset.dimension)(positionV2Definition.key).toOption.get
  val priceGridV1: GridHandle[priceHandle.D] =
    snapshot.resolveGrid(priceHandle)(priceV1Definition.key).toOption.get
  val priceGridV2: GridHandle[priceHandle.D] =
    snapshot.resolveGrid(priceHandle)(priceV2Definition.key).toOption.get

  def definition(
    name: String,
    settle: AssetId,
    baseCoefficient: Rational,
    quoteCoefficient: Rational,
    roles: Option[AssetRoleIds] = None,
    listing: Option[ListingDefinition] = None
  ): InstrumentDefinition =
    InstrumentDefinition(
      InstrumentIdentity(
        InstrumentId.from(s"$prefix-$name").toOption.get,
        UnderlyingId.from(s"$prefix-underlying").toOption.get
      ),
      roles.getOrElse(AssetRoleIds(baseId, quoteId, positionId, settle)),
      listing.getOrElse(ListingDefinition(positionGridV1.identity, priceGridV1.identity)),
      PayoffDefinition(baseCoefficient, quoteCoefficient)
    )

  def wire(definition: InstrumentDefinition): String =
    InstrumentDefinitionRecord.encodeDefinition(definition).toOption.get

  private def asset(name: String): AssetDefinition =
    AssetDefinition(
      AssetId.from(s"$prefix-$name").toOption.get,
      AtomId(s"$prefix:$name")
    )

  private def grid(dimension: DimKey, id: String, version: Long, quantum: Rational): GridDefinition =
    GridDefinition(
      GridIdentity(
        dimension,
        GridKey(
          GridId.from(s"$prefix-$id").toOption.get,
          GridVersion.from(version).toOption.get
        )
      ),
      PositiveRational(quantum).toOption.get
    )
end Fixture
