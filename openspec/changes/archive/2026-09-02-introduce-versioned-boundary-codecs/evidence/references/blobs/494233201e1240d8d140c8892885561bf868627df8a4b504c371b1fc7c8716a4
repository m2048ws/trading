package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

class GridCoordinateRecordSuite extends FunSuite:
  private val compoundDimension = DimKey(
    Vector(
      AtomId("asset:eur")   -> BigInt(1),
      AtomId("time:second") -> BigInt(-1)
    )
  )
  private val compoundIdentity = gridIdentity(compoundDimension, "compound-rate", 7)

  test("V1 records contain only stable identity and exact coordinate data and reject Java serialization"):
    val general = GeneralGridCoordinateRecord.V1(compoundIdentity, BigInt(-17))
    val asset   = AssetGridCoordinateRecord.V1(assetId("EUR"), compoundIdentity, BigInt(19))

    assertEquals(general.productElementNames.toVector, Vector("gridIdentity", "coordinate"))
    assertEquals(asset.productElementNames.toVector, Vector("assetId", "gridIdentity", "coordinate"))
    assertEquals(
      classOf[GeneralGridCoordinateRecord.V1].getDeclaredFields.map(_.getName).toSet,
      Set("gridIdentity", "coordinate")
    )
    assertEquals(
      classOf[AssetGridCoordinateRecord.V1].getDeclaredFields.map(_.getName).toSet,
      Set("assetId", "gridIdentity", "coordinate")
    )
    assertEquals(GeneralGridCoordinateRecord.recordType.value, "trading.general-grid-coordinate")
    assertEquals(AssetGridCoordinateRecord.recordType.value, "trading.asset-grid-coordinate")
    assertEquals(GeneralGridCoordinateRecord.schemaVersion, SchemaVersion.one)
    assertEquals(AssetGridCoordinateRecord.schemaVersion, SchemaVersion.one)
    rejectSerialization(general)
    rejectSerialization(asset)

  test("general records canonically encode signed huge coordinates and reconstruct a compound dimension"):
    val snapshot = published(
      CatalogCommand.RegisterDimension(compoundDimension),
      CatalogCommand.RegisterGrid(gridDefinition(compoundIdentity, 1, 10_000))
    )
    val grid       = snapshot.resolveGrid(compoundIdentity).toOption.get
    val coordinate = -(BigInt(10).pow(600) + 37)
    val record     = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(coordinate))
    val encoded    = GeneralGridCoordinateRecord.encode(record).toOption.get
    val golden     =
      s"""{"payload":{"coordinate":"$coordinate","gridIdentity":{"dimension":[{"atom":"asset:eur","power":"1"},{"atom":"time:second","power":"-1"}],"gridId":"compound-rate","gridVersion":"7"}},"recordType":"trading.general-grid-coordinate","schemaVersion":1}"""

    assertEquals(record, GeneralGridCoordinateRecord.V1(compoundIdentity, coordinate))
    assertEquals(encoded, golden)
    assertEquals(GeneralGridCoordinateRecord.parse(encoded), Right(record))
    val reordered =
      s"""{
         | "schemaVersion": 1,
         | "payload": {
         |   "gridIdentity": {
         |     "gridVersion": "7",
         |     "gridId": "compound-rate",
         |     "dimension": [
         |       {"power":"1","atom":"asset:eur"},
         |       {"power":"-1","atom":"time:second"}
         |     ]
         |   },
         |   "coordinate": "$coordinate"
         | },
         | "recordType": "trading.general-grid-coordinate"
         |}""".stripMargin
    assertEquals(GeneralGridCoordinateRecord.parse(reordered).flatMap(GeneralGridCoordinateRecord.encode),
      Right(golden))

    val decoded = GeneralGridCoordinateRecord.reconstruct(record, snapshot).toOption.get
    assertEquals(decoded.dimension.key, compoundDimension)
    assertEquals(decoded.grid.identity, compoundIdentity)
    assertEquals(decoded.grid.dimension.key, decoded.dimension.key)
    assertEquals(decoded.grid.coordinate(decoded.value), coordinate)
    assertEquals(
      decoded.grid.asQuantity(decoded.value).coefficient,
      Rational(coordinate, BigInt(10_000))
    )
    rejectSerialization(decoded)

  test("asset records restore the exact historical version and retain dependent asset-grid coherence"):
    val definition = AssetDefinition(assetId("USD"), AtomId("asset:usd"))
    val dimension  = DimKey.atom(definition.dimensionAtom)
    val first      = gridIdentity(dimension, "usd-price", 1)
    val second     = gridIdentity(dimension, "usd-price", 2)
    val snapshot   = published(
      CatalogCommand.RegisterAsset(definition),
      CatalogCommand.RegisterGrid(gridDefinition(first, 1, 100)),
      CatalogCommand.RegisterGrid(gridDefinition(second, 1, 1_000))
    )
    val asset      = snapshot.resolveAsset(definition.id).toOption.get
    val firstGrid  = snapshot.resolveGrid(asset.dimension)(first.key).toOption.get
    val secondGrid = snapshot.resolveGrid(asset.dimension)(second.key).toOption.get
    val coordinate = BigInt(10).pow(500) + 123
    val record     = AssetGridCoordinateRecord.pack(asset)(firstGrid)(firstGrid.fromCoordinate(coordinate))

    assertEquals(record, AssetGridCoordinateRecord.V1(asset.id, first, coordinate))
    assertEquals(AssetGridCoordinateRecord.parse(AssetGridCoordinateRecord.encode(record).toOption.get), Right(record))
    val decoded = AssetGridCoordinateRecord.reconstruct(record, snapshot).toOption.get
    assert(decoded.asset.eq(asset))
    assert(decoded.grid.eq(firstGrid))
    assert(!decoded.grid.eq(secondGrid))
    assertEquals(decoded.grid.version, gridVersion(1))
    assertEquals(decoded.grid.quantum.unrefined, Rational(1, 100))
    assertEquals(decoded.grid.coordinate(decoded.value), coordinate)
    assertEquals(decoded.grid.asQuantity(decoded.value).coefficient, Rational(coordinate, BigInt(100)))
    rejectSerialization(decoded)

  test("snapshot reconstruction reports dimension, asset, mismatch, and full-grid stages without fallback"):
    val usdDefinition = AssetDefinition(assetId("USD-failures"), AtomId("asset:usd-failures"))
    val usdDimension  = DimKey.atom(usdDefinition.dimensionAtom)
    val retained      = gridIdentity(usdDimension, "retained", 1)
    val snapshot      = published(
      CatalogCommand.RegisterAsset(usdDefinition),
      CatalogCommand.RegisterGrid(gridDefinition(retained, 1, 100))
    )
    val missingDimension = DimKey.atom(AtomId("asset:missing"))
    val missingIdentity  = gridIdentity(missingDimension, "missing", 1)
    val missingVersion   = gridIdentity(usdDimension, "retained", 99)

    assertEquals(
      GeneralGridCoordinateRecord.reconstruct(
        GeneralGridCoordinateRecord.V1(missingIdentity, BigInt(1)),
        snapshot
      ),
      Left(GridCoordinateReconstructionFailure.UnknownDimension(missingDimension))
    )
    assertEquals(
      GeneralGridCoordinateRecord.reconstruct(
        GeneralGridCoordinateRecord.V1(missingVersion, BigInt(1)),
        snapshot
      ),
      Left(GridCoordinateReconstructionFailure.UnknownGrid(missingVersion))
    )
    val unknownAsset = assetId("unknown-asset")
    assertEquals(
      AssetGridCoordinateRecord.reconstruct(
        AssetGridCoordinateRecord.V1(unknownAsset, missingIdentity, BigInt(1)),
        snapshot
      ),
      Left(GridCoordinateReconstructionFailure.UnknownAsset(unknownAsset))
    )
    assertEquals(
      AssetGridCoordinateRecord.reconstruct(
        AssetGridCoordinateRecord.V1(usdDefinition.id, missingIdentity, BigInt(1)),
        snapshot
      ),
      Left(
        GridCoordinateReconstructionFailure.AssetDimensionMismatch(
          usdDefinition.id,
          missingDimension,
          usdDimension
        )
      )
    )

  test("batch reconstruction accumulates indexed failures and preserves all-success input order"):
    val knownDimension = DimKey.atom(AtomId("batch:known"))
    val knownIdentity  = gridIdentity(knownDimension, "batch-grid", 3)
    val snapshot       = published(
      CatalogCommand.RegisterDimension(knownDimension),
      CatalogCommand.RegisterGrid(gridDefinition(knownIdentity, 3, 7))
    )
    val grid                = snapshot.resolveGrid(knownIdentity).toOption.get
    val first               = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(BigInt(-3)))
    val second              = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(BigInt(0)))
    val third               = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(BigInt(11)))
    val successful          = GeneralGridCoordinateRecord.reconstructBatch(Vector(first, second, third), snapshot)
    val restored            = successful.toOption.get
    val restoredCoordinates = restored.map(value => value.grid.coordinate(value.value))
    assertEquals(restoredCoordinates, Vector(BigInt(-3), BigInt(0), BigInt(11)))

    val absentDimension = DimKey.atom(AtomId("batch:absent"))
    val absentIdentity  = gridIdentity(absentDimension, "absent", 1)
    val absentGrid      = gridIdentity(knownDimension, "batch-grid", 999)
    val failures        = GeneralGridCoordinateRecord
      .reconstructBatch(
        Vector(
          first,
          GeneralGridCoordinateRecord.V1(absentIdentity, BigInt(1)),
          GeneralGridCoordinateRecord.V1(absentGrid, BigInt(2)),
          third
        ),
        snapshot
      )
      .left
      .toOption
      .get
    assertEquals(
      failures.toVector,
      Vector(
        IndexedGridCoordinateReconstructionFailure(
          1,
          GridCoordinateReconstructionFailure.UnknownDimension(absentDimension)
        ),
        IndexedGridCoordinateReconstructionFailure(
          2,
          GridCoordinateReconstructionFailure.UnknownGrid(absentGrid)
        )
      )
    )
    failures.toVector.foreach(rejectSerialization)

  test("family codecs retain typed field paths, reject cross-family envelopes, and omit copied quantum"):
    val missingCoordinate =
      """{"payload":{"gridIdentity":{"dimension":[],"gridId":"g","gridVersion":"1"}},"recordType":"trading.general-grid-coordinate","schemaVersion":1}"""
    GeneralGridCoordinateRecord.parse(missingCoordinate).left.toOption.get.head match
      case WireDecodeViolation.MissingField(path, "coordinate", 0) =>
        assertEquals(path.render, "$.payload.coordinate")
      case other => fail(s"unexpected missing-coordinate failure: $other")

    val assetWire = AssetGridCoordinateRecord.encode(
      AssetGridCoordinateRecord.V1(assetId("wire-asset"), gridIdentity(DimKey.one, "wire-grid", 1), BigInt(2))
    ).toOption.get
    GeneralGridCoordinateRecord.parse(assetWire).left.toOption.get.head match
      case WireDecodeViolation.Envelope(_, EnvelopeProblem.RecordTypeMismatch(expected, supplied), 0) =>
        assertEquals(expected, GeneralGridCoordinateRecord.recordType)
        assertEquals(supplied, AssetGridCoordinateRecord.recordType)
      case other => fail(s"unexpected cross-family failure: $other")

    val generalSchema = GeneralGridCoordinateRecord.schema().toOption.get
    val assetSchema   = AssetGridCoordinateRecord.schema().toOption.get
    assert(generalSchema.contains("gridIdentity"))
    assert(assetSchema.contains("assetId"))
    assert(!generalSchema.contains("quantum"))
    assert(!assetSchema.contains("quantum"))

  test("decoded dependent-package constructors are private and retired compatibility names stay absent"):
    Vector(classOf[DecodedGridQuantity], classOf[DecodedAssetGridQuantity]).foreach: owner =>
      assert(owner.getDeclaredConstructors.forall(constructor => Modifier.isPrivate(constructor.getModifiers)))

    val publicNames = Vector(
      classOf[DecodedGridQuantity].getName,
      classOf[DecodedAssetGridQuantity].getName,
      GeneralGridCoordinateRecord.getClass.getName,
      AssetGridCoordinateRecord.getClass.getName
    ).mkString(" ")
    Vector("PackedGridQuantity", "ResolvedGridQuantity", "QuantityRegistry").foreach: retired =>
      assert(!publicNames.contains(retired))

  test("reconstruction failures reject null payloads and false dimension mismatches"):
    val dimension = DimKey.atom(AtomId("failure:dimension"))
    val id        = assetId("failure-asset")
    val _         = intercept[NullPointerException](GridCoordinateReconstructionFailure.UnknownDimension(null))
    val _         = intercept[NullPointerException](GridCoordinateReconstructionFailure.UnknownAsset(null))
    val _         = intercept[NullPointerException](GridCoordinateReconstructionFailure.UnknownGrid(null))
    val _ = intercept[NullPointerException](GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(null))
    val _ = intercept[IllegalArgumentException](
      GridCoordinateReconstructionFailure.AssetDimensionMismatch(id, dimension, dimension)
    )

  private def published(commands: CatalogCommand*): CatalogSnapshot =
    CatalogModel
      .commit(CatalogRoot.create().initialState, CatalogBatch.of(commands.head, commands.tail*))
      .fold(errors => fail(s"expected published catalog snapshot, got $errors"), _.state.snapshot)

  private def assetId(value: String): AssetId =
    AssetId.from(value).fold(error => fail(s"invalid test asset ID: $error"), identity)

  private def gridId(value: String): GridId =
    GridId.from(value).fold(error => fail(s"invalid test grid ID: $error"), identity)

  private def gridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => fail(s"invalid test grid version: $error"), identity)

  private def gridIdentity(dimension: DimKey, id: String, version: Long): GridIdentity =
    GridIdentity(dimension, GridKey(gridId(id), gridVersion(version)))

  private def gridDefinition(
    identity: GridIdentity,
    numerator: BigInt,
    denominator: BigInt
  ): GridDefinition =
    val quantum = PositiveRational.exact(numerator, denominator).fold(
      error => fail(s"invalid test grid quantum: $error"),
      value => value
    )
    GridDefinition(identity, quantum)

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()
end GridCoordinateRecordSuite
