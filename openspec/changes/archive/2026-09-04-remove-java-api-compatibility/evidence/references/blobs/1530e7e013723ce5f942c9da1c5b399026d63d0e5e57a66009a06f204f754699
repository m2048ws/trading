package external

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import scala.jdk.CollectionConverters.*

import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.StoreReporter
import munit.FunSuite

class BoundaryCodecCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private val fixturesRoot         = Paths.get(getClass.getResource("/boundary-codec-compiler").toURI)
  private val compilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/boundary-codec-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated boundary-codec compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  private val entries = compilationClasspath.split(File.pathSeparator).toList.map(Paths.get(_))

  test("completed boundary-codec classpath contains exactly its one-way production graph"):
    List(
      "trading-quantities_3-",
      "trading-reference-data_3-",
      "trading-instrument-economics_3-",
      "trading-order-model_3-",
      "trading-execution-scenario_3-",
      "trading-boundary-codecs_3-"
    ).foreach: prefix =>
      assertEquals(entries.count(_.getFileName.toString.startsWith(prefix)), 1, entries.mkString("\n"))

    List(
      "trading-fee-policy_3-",
      "trading-risk_3-",
      "trading-application_3-",
      "trading-runtime_3-",
      "cats-effect_3-",
      "fs2-core_3-",
      "circe-core_3-",
      "doobie-core_3-",
      "opentelemetry-api-",
      "jackson-databind-",
      "jackson-module-scala_3-",
      "json-schema-validator-",
      "java-json-canonicalization-",
      "jmh-core-"
    ).foreach: prefix =>
      assert(!entries.exists(_.getFileName.toString.startsWith(prefix)), s"production classpath retained $prefix")

    assert(entries.exists(_.getFileName.toString.startsWith("cats-core_3-")), entries.mkString("\n"))
    assert(entries.exists(_.getFileName.toString.startsWith("jackson-core-3.")), entries.mkString("\n"))
    assert(entries.forall(Files.isRegularFile(_)), entries.mkString("\n"))

  test("completed boundary-codec JAR contains the public foundation and confined parser/schema internals"):
    val artifact = exactlyOne("trading-boundary-codecs_3-")
    val jar      = new JarFile(artifact.toFile)
    try
      val names = jar.entries().asScala.map(_.getName).toSet
      assert(names.contains("trading/codec/package.class"), names.toList.sorted.mkString("\n"))
      assert(names.contains("trading/codec/schema/README.md"), names.toList.sorted.mkString("\n"))
      val schemaResources = names.filter(name =>
        name.startsWith("trading/codec/schema/") && name.endsWith(".schema.json")
      )
      assertEquals(
        schemaResources,
        Set(
          "trading/codec/schema/general-grid-coordinate-v1.schema.json",
          "trading/codec/schema/asset-grid-coordinate-v1.schema.json",
          "trading/codec/schema/catalog-journal-entry-v1.schema.json",
          "trading/codec/schema/instrument-definition-v1.schema.json",
          "trading/codec/schema/order-v1.schema.json",
          "trading/codec/schema/order-scenario-v1.schema.json",
          "trading/codec/schema/round-trip-scenario-v1.schema.json"
        )
      )
      schemaResources.foreach: entry =>
        val input = jar.getInputStream(jar.getJarEntry(entry))
        try
          val schema = new String(input.readAllBytes(), StandardCharsets.UTF_8)
          List(
            "feePolicy",
            "feeAssessment",
            "pricePnl",
            "netPnl",
            "accountId",
            "fillId",
            "venue",
            "catalogRevision",
            "snapshot",
            "checkpoint",
            "repository"
          ).foreach(fragment => assert(!schema.contains(fragment), s"$entry contains out-of-scope $fragment"))
        finally input.close()
      val codecClasses = names.filter(name => name.startsWith("trading/codec/") && name.endsWith(".class"))
      List("Packed", "GridCoordinateEncoding", "LiveCatalog", "Repository", "Checkpoint").foreach: fragment =>
        assert(!codecClasses.exists(_.contains(fragment)), s"codec JAR retained out-of-scope name $fragment")
      List(
        "trading/codec/DecodeLimits.class",
        "trading/codec/DecodedAssetGridQuantity.class",
        "trading/codec/DecodedGridQuantity.class",
        "trading/codec/GridCoordinateRecordReconstructionFailure.class",
        "trading/codec/IndexedGridCoordinateRecordReconstructionFailure.class",
        "trading/codec/GeneralGridCoordinateRecord$.class",
        "trading/codec/AssetGridCoordinateRecord$.class",
        "trading/codec/CatalogJournalEntry$.class",
        "trading/codec/CatalogJournalEntry$V1.class",
        "trading/codec/CatalogReplay$.class",
        "trading/codec/CatalogReplayFailure.class",
        "trading/codec/CatalogReplayResult.class",
        "trading/codec/InstrumentDefinitionRecord$.class",
        "trading/codec/InstrumentDefinitionRecord$V1.class",
        "trading/codec/InstrumentDefinitionReconstructionFailure.class",
        "trading/codec/IndexedInstrumentDefinitionReconstructionFailure.class",
        "trading/codec/OrderRecord$.class",
        "trading/codec/OrderRecord$V1.class",
        "trading/codec/OrderReconstructionFailure.class",
        "trading/codec/OrderRefinementFailure.class",
        "trading/codec/OrderRefinementFailures.class",
        "trading/codec/IndexedOrderReconstructionFailure.class",
        "trading/codec/OrderScenarioRecord$.class",
        "trading/codec/OrderScenarioRecord$V1.class",
        "trading/codec/OrderScenarioReconstructionFailure.class",
        "trading/codec/IndexedOrderScenarioReconstructionFailure.class",
        "trading/codec/ScenarioPreparationFailure.class",
        "trading/codec/ScenarioPreparationFailures.class",
        "trading/codec/RoundTripScenarioRecord$.class",
        "trading/codec/RoundTripScenarioRecord$V1.class",
        "trading/codec/RoundTripScenarioReconstructionFailure.class",
        "trading/codec/IndexedRoundTripScenarioReconstructionFailure.class",
        "trading/codec/WirePath.class",
        "trading/codec/WireViolations.class",
        "trading/codec/StrictJson$.class",
        "trading/codec/WireSchema.class",
        "trading/codec/JsonSchemaDocument$.class"
      ).foreach(name => assert(codecClasses.contains(name), names.toList.sorted.mkString("\n")))

      val forbiddenImplementationFragments = List(
        "com/networknt/",
        "org/erdtman/",
        "cats/effect/",
        "fs2/",
        "trading/application/",
        "trading/runtime/",
        "java/util/concurrent/atomic/",
        "java/lang/reflect/"
      )
      codecClasses.foreach: entry =>
        val input = jar.getInputStream(jar.getJarEntry(entry))
        try
          val bytecode = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1)
          forbiddenImplementationFragments.foreach: fragment =>
            assert(!bytecode.contains(fragment), s"$entry exposes $fragment")
        finally input.close()

      val intendedPublicSurface = Set(
        "trading/codec/DecodeLimits.class",
        "trading/codec/WirePath.class",
        "trading/codec/WirePathSegment.class",
        "trading/codec/WireViolations.class",
        "trading/codec/WireDecodeViolation.class",
        "trading/codec/WireEncodeViolation.class",
        "trading/codec/WireLimitViolation.class",
        "trading/codec/RecordType.class",
        "trading/codec/SchemaVersion.class",
        "trading/codec/GridCoordinateRecordReconstructionFailure.class",
        "trading/codec/IndexedGridCoordinateRecordReconstructionFailure.class",
        "trading/codec/CatalogJournalEntry$V1.class",
        "trading/codec/CatalogJournalRebuildFailure.class",
        "trading/codec/CatalogReplayFailure.class",
        "trading/codec/CatalogReplayResult.class",
        "trading/codec/InstrumentDefinitionRecord$V1.class",
        "trading/codec/InstrumentDefinitionReconstructionFailure.class",
        "trading/codec/IndexedInstrumentDefinitionReconstructionFailure.class",
        "trading/codec/OrderRecord$V1.class",
        "trading/codec/OrderReconstructionFailure.class",
        "trading/codec/OrderRefinementFailure.class",
        "trading/codec/OrderRefinementFailures.class",
        "trading/codec/IndexedOrderReconstructionFailure.class",
        "trading/codec/OrderScenarioRecord$V1.class",
        "trading/codec/OrderScenarioReconstructionFailure.class",
        "trading/codec/IndexedOrderScenarioReconstructionFailure.class",
        "trading/codec/ScenarioPreparationFailure.class",
        "trading/codec/ScenarioPreparationFailures.class",
        "trading/codec/RoundTripScenarioRecord$V1.class",
        "trading/codec/RoundTripScenarioReconstructionFailure.class",
        "trading/codec/IndexedRoundTripScenarioReconstructionFailure.class",
        "trading/codec/ExactNumberProblem.class",
        "trading/codec/StableIdentifierProblem.class",
        "trading/codec/DimensionProblem.class",
        "trading/codec/EnvelopeProblem.class"
      )
      intendedPublicSurface.foreach: entry =>
        val input = jar.getInputStream(jar.getJarEntry(entry))
        try
          val bytecode = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1)
          List("tools/jackson/", "cats/data/", "com/networknt/", "org/erdtman/").foreach: fragment =>
            assert(!bytecode.contains(fragment), s"$entry leaks $fragment")
        finally input.close()

      val parserInput = jar.getInputStream(jar.getJarEntry("trading/codec/StrictJson$.class"))
      try
        val parserBytecode = new String(parserInput.readAllBytes(), StandardCharsets.ISO_8859_1)
        assert(parserBytecode.contains("tools/jackson/"), "strict adapter does not bind Jackson Core")
      finally parserInput.close()
      List(
        "PackedAssetGridQuantity",
        "PackedGridQuantity",
        "ResolvedAssetGridQuantity",
        "ResolvedGridQuantity",
        "QuantityRegistry",
        "CatalogJournalRepository",
        "CatalogCheckpoint"
      ).foreach: retired =>
        assert(!names.exists(_.contains(retired)), s"completed codec JAR restored retired name $retired")
      assert(!names.exists(_.startsWith("trading/fee/")))
      assert(!names.exists(_.startsWith("trading/risk/")))
      assert(!names.exists(_.startsWith("trading/application/")))
      assert(!names.exists(_.startsWith("trading/runtime/")))
    finally jar.close()
    end try

  test("completed boundary-codec classpath compiles a direct-domain and Jackson-Core client"):
    val result = compile(fixturesRoot.resolve("positive/BoundaryCodecClasspathClient.scala"))
    assert(result.succeeded, result.rendered)

  test("completed boundary-codec JAR compiles the domain-owned public codec foundation"):
    val result = compile(fixturesRoot.resolve("positive/BoundaryCodecFoundationClient.scala"))
    assert(result.succeeded, result.rendered)

  test("completed boundary-codec JAR compiles exact grid packing and dependent reconstruction clients"):
    val result = compile(fixturesRoot.resolve("positive/GridCoordinateRecordClient.scala"))
    assert(result.succeeded, result.rendered)

  test("catalog journal clients can retain published batches and replay only from fresh state"):
    val result = compile(fixturesRoot.resolve("positive/CatalogJournalClient.scala"))
    assert(result.succeeded, result.rendered)

  test("instrument-definition clients can decode stable data and assemble through one explicit snapshot"):
    val result = compile(fixturesRoot.resolve("positive/InstrumentDefinitionRecordClient.scala"))
    assert(result.succeeded, result.rendered)

  test("immutable-order clients can retain stable records and reconstruct through one explicit instrument"):
    val result = compile(fixturesRoot.resolve("positive/OrderRecordClient.scala"))
    assert(result.succeeded, result.rendered)

  test("hypothetical-scenario clients reconstruct associated evidence through one instrument and snapshot"):
    val result = compile(fixturesRoot.resolve("positive/ScenarioRecordClient.scala"))
    assert(result.succeeded, result.rendered)

  test("completed boundary-codec classpath rejects downstream, effect, mapping, and test-oracle concerns"):
    val source  = fixturesRoot.resolve("negative/CodecHasNoForbiddenDependencies.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 13, rejected.rendered)
    List("fee", "risk", "application", "runtime", "effect", "fs2", "doobie", "opentelemetry", "databind", "circe",
      "networknt", "erdtman").foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed boundary-codec JAR hides parser, AST, and schema-algebra internals"):
    val source  = fixturesRoot.resolve("negative/CodecInternalsAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 10, rejected.rendered)
    List(
      "StrictJson",
      "CanonicalJson",
      "JsonNode",
      "WireSchema",
      "JsonSchemaDocument",
      "DecodeContext",
      "ExactWire",
      "EnvelopeCodec",
      "EnvelopeHeader",
      "CanonicalRationalRecord"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("grid-coordinate families reject off-grid and cross-grid values and retired names"):
    val source  = fixturesRoot.resolve("negative/GridCoordinateEscapesAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 7, rejected.rendered)
    List(
      "GridQuantity",
      "PackedGridQuantity",
      "ResolvedGridQuantity",
      "QuantityRegistry"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("catalog journal rejects broader outcomes, authority leaks, and durability concerns"):
    val source  = fixturesRoot.resolve("negative/CatalogJournalAuthorityEscapesAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 10, rejected.rendered)
    List(
      "Published",
      "CatalogState",
      "root",
      "lineage",
      "timestamp",
      "checkpoint",
      "activation",
      "delisting",
      "CatalogJournalRepository",
      "CatalogCheckpoint"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("instrument-definition records reject trusted authority revision market and live-context escapes"):
    val source  = fixturesRoot.resolve("negative/InstrumentDefinitionAuthorityEscapesAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 13, rejected.rendered)
    List(
      "Instrument",
      "InstrumentSpec",
      "CatalogSnapshot",
      "catalogRevision",
      "lineage",
      "snapshot",
      "positionLotGridHandle",
      "priceGridHandle",
      "market",
      "venue",
      "productFamily"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("order records reject trusted authority derived state execution facts and untyped decoding"):
    val source  = fixturesRoot.resolve("negative/OrderRecordAuthorityEscapesAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 17, rejected.rendered)
    List(
      "Instrument",
      "CatalogRoot",
      "CatalogSnapshot",
      "decode",
      "positionChange",
      "componentInstrumentIds",
      "scenarios",
      "venueLifecycle",
      "fills",
      "reportedFees",
      "accountState",
      "catalogRevision",
      "lineage",
      "snapshot",
      "lotGridHandle",
      "priceGridHandle"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("scenario records reject execution facts fees valuation lifecycle targets and untyped evidence"):
    val source  = fixturesRoot.resolve("negative/ScenarioRecordAuthorityEscapesAreUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 17, rejected.rendered)
    List(
      "decode",
      "actualExecution",
      "fillId",
      "venue",
      "feePolicy",
      "fees",
      "pnl",
      "lifecycle",
      "catalogRevision",
      "targetAssetId",
      "heldPosition",
      "pricePnl",
      "netPnl",
      "reconstruct",
      "evidence"
    ).foreach: fragment =>
      assert(rejected.rendered.contains(fragment), s"missing '$fragment' rejection:\n${rejected.rendered}")
    boundaryForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  private def exactlyOne(prefix: String): Path =
    entries.filter(_.getFileName.toString.startsWith(prefix)) match
      case artifact :: Nil => artifact
      case other           => fail(s"expected one $prefix artifact, found:\n${other.mkString("\n")}")

  private def compilePrelude(source: Path): Compilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)

    val directory = Files.createTempDirectory("boundary-codec-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy)

  private def compile(source: Path): Compilation =
    val output   = Files.createTempDirectory("boundary-codec-classes-")
    val reporter = new StoreReporter()
    val _        = Main.process(
      Array(
        "-classpath",
        compilationClasspath,
        "-d",
        output.toString,
        "-Werror",
        "-source:future",
        source.toString
      ),
      reporter
    )
    Compilation(reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

end BoundaryCodecCompilerBoundarySuite

private val boundaryForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "See full stack trace",
  "at dotty.tools"
)
