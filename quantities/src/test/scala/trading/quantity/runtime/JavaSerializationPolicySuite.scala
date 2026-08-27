package trading.quantity.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.NotSerializableException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

class JavaSerializationPolicySuite extends FunSuite:

  private def serializationFailure(v: AnyRef, name: String): NotSerializableException =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      output.writeObject(v)
      fail(s"$name serialized successfully")
    catch case failure: NotSerializableException => failure
    finally output.close()

  private def assertExplicitlyRejected(name: String, v: AnyRef): Unit =
    val failure         = serializationFailure(v, name)
    val expectedMessage =
      s"Java object serialization is unsupported for ${v.getClass.getName}"
    assert(
      failure.getMessage == expectedMessage,
      s"$name used an incidental failure instead of the fail-closed hook: ${failure.getMessage}"
    )

  private def craftedGridIdStream(v: String): Array[Byte] =
    val bytes = new ByteArrayOutputStream
    val data  = new DataOutputStream(bytes)
    data.writeShort(0xaced) // STREAM_MAGIC
    data.writeShort(5)      // STREAM_VERSION
    data.writeByte(0x73)    // TC_OBJECT
    data.writeByte(0x72)    // TC_CLASSDESC
    data.writeUTF(classOf[GridId].getName)
    data.writeLong(ObjectStreamClass.lookup(classOf[GridId]).getSerialVersionUID)
    data.writeByte(0x02) // SC_SERIALIZABLE
    data.writeShort(1)
    data.writeByte('L')
    data.writeUTF("value")
    data.writeByte(0x74) // TC_STRING for the field signature
    data.writeUTF("Ljava/lang/String;")
    data.writeByte(0x78) // TC_ENDBLOCKDATA
    data.writeByte(0x70) // TC_NULL superclass descriptor
    data.writeByte(0x74) // TC_STRING field value
    data.writeUTF(v)
    data.close()
    bytes.toByteArray

  end craftedGridIdStream

  test("invariant-bearing public nominal carriers reject Java object serialization"):
    val atom        = AtomId("serialization-atom")
    val dimension   = DimKey.atom(atom)
    val gridId      = GridId("serialization-grid")
    val gridVersion = GridVersion(1)
    val quantum     = PositiveRational.exact(1, 100).toOption.get
    val assetId     = AssetId("serialization-asset")
    val registry    = new QuantityRegistry
    val asset       =
      registry
        .registerAsset(AssetDefinition(assetId, atom))
        .toOption
        .get
    val gridDefinition = GridDefinition(dimension, gridId, gridVersion, quantum)
    val grid           =
      registry
        .registerGrid(asset)(gridDefinition)
        .toOption
        .get
    val gridValue      = grid.fromCoordinate(BigInt(123))
    val packedAsset    = PackedAssetGridQuantity.pack(asset)(grid)(gridValue)
    val packedQuantity = PackedGridQuantity.pack(grid)(gridValue)
    val resolvedAsset  =
      PackedAssetGridQuantity
        .decode(packedAsset, registry)
        .toOption
        .get
    val resolvedGrid =
      PackedGridQuantity
        .decode(packedQuantity, registry)
        .toOption
        .get
    val resolvedExact =
      HeterogeneousQuantity
        .addExact(HeterogeneousQuantity.generalize(resolvedAsset), resolvedGrid)
        .toOption
        .get

    val offGrid      = Quantity(asset.dimension.ref, Rational(1, 3))
    val quantization = offGrid.quantizeTo(grid.asGridRef, QuantizationPolicy.HalfEven)
    val allocation   =
      gridValue.allocateEvenly(PositiveInt(3).toOption.get, RemainderOrder.FirstToLast, grid.asGridRef)
    val quotientRemainder        = gridValue.quotRemBy(PositiveWhole(3).toOption.get, grid.asGridRef)
    val refinedQuotientRemainder =
      Positive(gridValue).toOption.get.quotRemBy(PositiveWhole(3).toOption.get, grid.asGridRef)
    val refinedQuantization =
      Positive(offGrid).toOption.get.quantizeTo(grid.asGridRef, QuantizationPolicy.HalfEven)
    val gridEncoding =
      ConstrainedGridEncoding
        .encodeExact(grid.asGridRef)(grid.asQuantity(gridValue))
        .toOption
        .get

    val numericError: NumericError =
      PositiveRational.exact(1, 0) match
        case Left(ZeroRationalDenominator) => ZeroRationalDenominator
        case result                        => fail(s"expected a zero-denominator error, got $result")
    val exactDecimalError: ExactDecimalError =
      Quantity(DimRef.one, new java.math.BigDecimal(java.math.BigInteger.ONE, Int.MinValue)).swap.toOption.get
    val refinementError: RefinementError = PositiveInt(0).swap.toOption.get
    val narrowingError                   = offGrid.narrowExactlyTo(grid.asGridRef).swap.toOption.get

    val comparisonGrid =
      UniformGrid.create(
        GridId("serialization-comparison-grid"),
        gridVersion,
        asset.dimension.ref,
        quantum
      )
    val gridRelationshipError: GridError =
      SameGrid.between(grid.asGridRef, comparisonGrid).swap.toOption.get

    val registryError: RegistryError =
      PackedAssetGridQuantity
        .decode(
          PackedAssetGridQuantity(asset.id, DimKey.one, grid.id, grid.version, BigInt(123)),
          registry
        )
        .swap
        .toOption
        .get

    val foreignRegistry = new QuantityRegistry
    val foreignAsset    =
      foreignRegistry
        .registerAsset(AssetDefinition(assetId, atom))
        .toOption
        .get
    val runtimeEvidenceError: RuntimeEvidenceError =
      RuntimeEvidence.sameDimension(asset, foreignAsset).swap.toOption.get

    val otherAsset =
      registry
        .registerAsset:
          AssetDefinition(AssetId("serialization-other-asset"), AtomId("serialization-other-atom"))
        .toOption
        .get
    val otherGrid =
      registry
        .registerGrid(otherAsset):
          GridDefinition(
            otherAsset.dimension.key,
            GridId("serialization-other-grid"),
            GridVersion(1),
            quantum
          )
        .toOption
        .get
    val otherPacked =
      PackedAssetGridQuantity.pack(otherAsset)(otherGrid)(otherGrid.fromCoordinate(BigInt(1)))
    val otherResolved = PackedAssetGridQuantity.decode(otherPacked, registry).toOption.get
    val heterogeneousError: HeterogeneousOperationError =
      HeterogeneousQuantity
        .addExact(
          HeterogeneousQuantity.generalize(resolvedAsset),
          HeterogeneousQuantity.generalize(otherResolved)
        )
        .swap
        .toOption
        .get

    assertEquals(resolvedAsset.grid.coordinate(resolvedAsset.value), BigInt(123))

    val inventory: List[(String, JavaSerializationUnsupported)] =
      List(
        "GridId"                      -> gridId,
        "AssetId"                     -> assetId,
        "AtomId"                      -> atom,
        "GridVersion"                 -> gridVersion,
        "GridKey"                     -> GridKey(gridId, gridVersion),
        "NumericError"                -> numericError,
        "ExactDecimalError"           -> exactDecimalError,
        "RefinementError"             -> refinementError,
        "GridError"                   -> gridRelationshipError,
        "NotOnGrid"                   -> narrowingError,
        "GridCoordinateEncoding"      -> gridEncoding,
        "QuotRem"                     -> quotientRemainder,
        "Allocation"                  -> allocation,
        "Quantization"                -> quantization,
        "RefinedQuotRem"              -> refinedQuotientRemainder,
        "RefinedQuantization"         -> refinedQuantization,
        "AssetDefinition"             -> AssetDefinition(assetId, atom),
        "GridDefinition"              -> gridDefinition,
        "PackedAssetGridQuantity"     -> packedAsset,
        "PackedGridQuantity"          -> packedQuantity,
        "ResolvedAssetGridQuantity"   -> resolvedAsset,
        "ResolvedGridQuantity"        -> resolvedGrid,
        "ResolvedExactQuantity"       -> resolvedExact,
        "RegistryError"               -> registryError,
        "RuntimeEvidenceError"        -> runtimeEvidenceError,
        "HeterogeneousOperationError" -> heterogeneousError,
        "QuantizationPolicy"          -> QuantizationPolicy.HalfEven
      )

    inventory.foreach:
      case (name, value) => assertExplicitlyRejected(name, value)

  test("the remainder-order enum uses its explicit fail-closed hooks"):
    val hookNames = classOf[RemainderOrder].getDeclaredMethods.map(_.getName).toSet
    assert(hookNames.contains("writeReplace"))
    assert(hookNames.contains("readResolve"))
    assertExplicitlyRejected("RemainderOrder", RemainderOrder.FirstToLast)

  test("mathematical values without Java serialization support remain rejected"):
    val naturallyUnsupported: List[(String, AnyRef)] =
      List(
        "Rational" -> Rational.one,
        "DimKey"   -> DimKey.one
      )

    naturallyUnsupported.foreach:
      case (name, value) => val _ = serializationFailure(value, name)

  test("crafted default deserialization cannot return a constructor-bypassing identifier"):
    val input = new ObjectInputStream(new ByteArrayInputStream(craftedGridIdStream("")))
    try
      val failure =
        intercept[NotSerializableException]:
          input.readObject()
      assert(failure.getMessage.startsWith("Java object serialization is unsupported for "))
    finally input.close()

  test("project-owned logical packing and checked decoding remain unaffected"):
    val registry = new QuantityRegistry
    val asset    =
      registry
        .registerAsset:
          AssetDefinition(AssetId("logical-pack-asset"), AtomId("logical-pack-atom"))
        .toOption
        .get
    val definition =
      GridDefinition(
        asset.dimension.key,
        GridId("logical-pack-grid"),
        GridVersion(1),
        PositiveRational.exact(1, 100).toOption.get
      )
    val grid    = registry.registerGrid(asset)(definition).toOption.get
    val packed  = PackedAssetGridQuantity.pack(asset)(grid)(grid.fromCoordinate(321))
    val decoded = PackedAssetGridQuantity.decode(packed, registry).toOption.get

    assertEquals(decoded.grid.coordinate(decoded.value), BigInt(321))

end JavaSerializationPolicySuite
