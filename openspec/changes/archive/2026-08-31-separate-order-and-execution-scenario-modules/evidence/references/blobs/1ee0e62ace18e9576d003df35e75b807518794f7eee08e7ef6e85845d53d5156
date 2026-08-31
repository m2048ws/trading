package external

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import scala.jdk.CollectionConverters.*

import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.StoreReporter
import munit.FunSuite

class EconomicsCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class NegativeFixture(
    file: String,
    expected: List[String],
    minimumErrors: Int,
    exactErrors: Option[Int] = None)

  private val fixturesRoot         = Paths.get(getClass.getResource("/economics-compiler").toURI)
  private val sharedFixture        = fixturesRoot.resolve("SharedEconomicsSetup.scala")
  private val compilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated external compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()
  private val coreCompilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/instrument-economics-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated instrument-economics compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("completed pure JAR compiles and runs a concrete core-only client with generic helpers"):
    val entries = coreCompilationClasspath.split(File.pathSeparator).map(Paths.get(_)).map(_.getFileName.toString)
    assert(entries.exists(_.startsWith("trading-instrument-economics_3-")))
    assert(!entries.exists(_.startsWith("trading-economics_3-")))
    assert(!entries.exists(_.startsWith("trading-application_3-")))
    val result = compileCore(Paths.get(getClass.getResource("/economics-core-compiler/PureCoreClient.scala").toURI))
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.economics.core.PureCoreClient$", "run")

  test("completed pure JAR preserves retained denomination definition and provenance in equality"):
    val result = compileCore(
      Paths.get(getClass.getResource("/economics-core-compiler/RetainedDenominationEqualityClient.scala").toURI)
    )
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.economics.core.RetainedDenominationEqualityClient$", "run")

  test("completed pure JAR cannot import downstream packages"):
    val source  = Paths.get(getClass.getResource("/economics-core-compiler/CoreHasNoDownstream.scala").toURI)
    val prelude = compileCorePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileCore(source)
    assert(rejected.errors.size >= 4, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("packaged artifacts preserve quantities, reference-data, and pure instrument-economics boundaries"):
    val quantitiesJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-quantities_3-"))
      .getOrElse(fail("missing packaged quantities artifact"))
    val referenceDataJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-reference-data_3-"))
      .getOrElse(fail("missing packaged reference-data artifact"))
    val instrumentJar     = packagedInstrumentEconomicsJar
    val quantitiesArchive = new JarFile(quantitiesJar.toFile)
    val referenceArchive  = new JarFile(referenceDataJar.toFile)
    val instrumentArchive = new JarFile(instrumentJar.toFile)
    try
      val quantityEntries   = quantitiesArchive.entries().asScala.map(_.getName).toSet
      val referenceEntries  = referenceArchive.entries().asScala.map(_.getName).toSet
      val instrumentEntries = instrumentArchive.entries().asScala.map(_.getName).toSet
      assert(!quantityEntries.exists(_.startsWith("trading/reference/")))
      assert(!quantityEntries.exists(_.startsWith("trading/economics/")))
      List(
        "AssetId.class",
        "GridId.class",
        "GridVersion.class",
        "GridKey.class",
        "GridIdentity.class",
        "CatalogRoot.class",
        "CatalogState.class",
        "CatalogSnapshot.class",
        "CatalogBatch.class",
        "CatalogCommand.class",
        "CatalogModel.class",
        "Asset.class",
        "DimensionHandle.class",
        "GridHandle.class"
      ).foreach: name =>
        assert(!quantityEntries.contains(s"trading/quantity/$name"), s"quantity JAR retained $name")
        assert(referenceEntries.contains(s"trading/reference/$name"), s"reference-data JAR is missing $name")
      assert(!referenceEntries.exists(_.startsWith("trading/economics/")))
      assert(instrumentEntries.exists(_ == "trading/economics/instrument/Instrument.class"))
      List("trading/order/", "trading/scenario/", "trading/fee/policy/", "trading/risk/", "trading/application/")
        .foreach(prefix => assert(!instrumentEntries.exists(_.startsWith(prefix)), s"pure JAR retained $prefix"))
    finally
      quantitiesArchive.close()
      referenceArchive.close()
      instrumentArchive.close()
    end try

  test("pure and transitional JARs expose their one-way owned classes"):
    val instrumentJar = packagedInstrumentEconomicsJar
    val economicsJar  = packagedEconomicsJar
    val core          = new JarFile(instrumentJar.toFile)
    val downstream    = new JarFile(economicsJar.toFile)
    try
      val coreEntries       = core.entries().asScala.map(_.getName).toSet
      val downstreamEntries = downstream.entries().asScala.map(_.getName).toSet
      val expectedCore      = List(
        "InstrumentDefinition.class",
        "InstrumentAssembler.class",
        "InstrumentSpec.class",
        "Instrument.class",
        "Lots.class",
        "PositionLots.class",
        "Price.class",
        "SettlementConversion.class",
        "MarketState.class",
        "FeeDenomination.class",
        "Fee.class",
        "PricePnl.class",
        "SettledFeeContribution.class",
        "Pnl.class",
        "Valuation.class"
      ).map(name => s"trading/economics/instrument/$name")
      expectedCore.foreach(entry => assert(coreEntries.contains(entry), s"missing $entry from $instrumentJar"))
      assert(!downstreamEntries.exists(_.startsWith("trading/economics/instrument/")))
      List(
        "trading/order/Orders.class",
        "trading/scenario/Scenarios.class",
        "trading/fee/policy/FeePolicy.class",
        "trading/risk/Risk.class"
      ).foreach(entry => assert(downstreamEntries.contains(entry), s"missing $entry from $economicsJar"))

      val staleCore = List(
        "trading/economics/instrument/Prices.class",
        "trading/economics/instrument/Market.class",
        "trading/economics/instrument/Fees.class",
        "trading/economics/instrument/Sizing.class",
        "trading/economics/instrument/EconomicsError.class",
        "trading/economics/instrument/ForeignRegistry.class"
      )
      staleCore.foreach(entry => assert(!coreEntries.contains(entry), s"stale $entry remains in $instrumentJar"))
    finally
      core.close()
      downstream.close()
    end try

  test("positive downstream economics fixture compiles without warnings and runs"):
    val result = compile(fixturesRoot.resolve("positive/CompleteEconomicsClient.scala"))
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.economics.positive.CompleteEconomicsClient$")

  test("same-shape replay fixture compiles against immutable JARs and enforces captured semantics"):
    val result = compile(fixturesRoot.resolve("positive/SameShapeReplayClient.scala"))
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.economics.positive.SameShapeReplayClient$")

  private val negativeFixtures = List(
    NegativeFixture("InstrumentSpecAuthority.scala", List("Cannot extend sealed trait"), 1),
    NegativeFixture("PackageSpoofInstrumentSpec.scala", List("Cannot extend sealed trait"), 1),
    NegativeFixture("RawDefinitionShape.scala", List("cannot be accessed", "Found:", "Required:"), 6),
    NegativeFixture("RawInstrumentConstruction.scala", List("Found:", "Required:", "is not a member"), 3),
    NegativeFixture("ReversedPayoffEndpoint.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("SpecAuthorityExtraction.scala", List("is not a member"), 4, Some(4)),
    NegativeFixture("ConversionDoesNotGrantGrid.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("AssociatedEvidenceShapes.scala", List("Found:", "Required:"), 6, Some(6)),
    NegativeFixture("RemovedCapabilityPaths.scala", List("is not a member"), 7, Some(7)),
    NegativeFixture("DeferredLifecycle.scala", List("is not a member"), 9, Some(9)),
    NegativeFixture("RemovedFlatApi.scala", List("is not a member"), 10, Some(10)),
    NegativeFixture("RemovedOwnerApi.scala", List("is not a member", "Not found"), 4, Some(4)),
    NegativeFixture("ReversedPriceRate.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("ReversedSettlementRate.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("RefinementLoss.scala", List("Found:", "Required:"), 4, Some(4)),
    NegativeFixture("CoreSideAbsent.scala", List("is not a member"), 2, Some(2)),
    NegativeFixture("UnlawfulAlgebra.scala", List("No given instance"), 2, Some(2)),
    NegativeFixture("RawCoreConstruction.scala", List("cannot be accessed"), 3, Some(3))
  )

  negativeFixtures.foreach: fixture =>
    test(s"negative downstream economics fixture rejects ${fixture.file}"):
      val source  = fixturesRoot.resolve("negative").resolve(fixture.file)
      val prelude = compilePrelude(source)
      assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")

      val rejected = compile(source)
      assert(rejected.errors.size >= fixture.minimumErrors, rejected.rendered)
      fixture.exactErrors.foreach(count => assertEquals(rejected.errors.size, count, rejected.rendered))
      fixture.expected.foreach(fragment => assert(rejected.rendered.contains(fragment), rejected.rendered))
      economicsForbiddenDiagnostics.foreach(fragment =>
        assert(!rejected.rendered.contains(fragment), rejected.rendered)
      )

  private def compilePrelude(source: Path): Compilation =
    compileFilteredPrelude(source, compile)

  private def compileCorePrelude(source: Path): Compilation =
    compileFilteredPrelude(source, compileCore)

  private def compileFilteredPrelude(source: Path, compileFile: Path => Compilation): Compilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)

    val directory = Files.createTempDirectory("economics-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compileFile(copy)

  private def compile(source: Path): Compilation =
    compileWith(source, compilationClasspath, Some(sharedFixture))

  private def compileCore(source: Path): Compilation =
    compileWith(source, coreCompilationClasspath, None)

  private def compileWith(source: Path, classpath: String, shared: Option[Path]): Compilation =
    val output        = Files.createTempDirectory("economics-classes-")
    val reporter      = new StoreReporter()
    val baseArguments = Array(
      "-classpath",
      classpath,
      "-d",
      output.toString,
      "-Werror",
      "-source:future"
    )
    val arguments = baseArguments ++ shared.toVector.map(_.toString) :+ source.toString
    val _         = Main.process(arguments, reporter)
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def packagedEconomicsJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-economics_3-"))
      .getOrElse(fail("missing packaged economics artifact"))

  private def packagedInstrumentEconomicsJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-instrument-economics_3-"))
      .getOrElse(fail("missing packaged instrument-economics artifact"))

  private def initializeModule(output: Path, moduleClassName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      assert(moduleClass.getField("MODULE$").get(null) != null, s"$moduleClassName was not initialized")
    catch
      case error: ExceptionInInitializerError =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled positive economics client failed during module initialization: $cause")
      case error: ReflectiveOperationException =>
        fail(s"compiled positive economics client module could not be loaded: $error")
      case error: LinkageError =>
        fail(s"compiled positive economics client module could not be linked: $error")
    finally loader.close()

  private def runModule(output: Path, moduleClassName: String, methodName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      val module      = moduleClass.getField("MODULE$").get(null)
      val _           = moduleClass.getMethod(methodName).invoke(module)
    catch
      case error: java.lang.reflect.InvocationTargetException =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled economics client $moduleClassName.$methodName failed: $cause")
      case error: ExceptionInInitializerError =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled economics client $moduleClassName failed during initialization: $cause")
      case error: ReflectiveOperationException =>
        fail(s"compiled economics client $moduleClassName could not be invoked: $error")
      case error: LinkageError =>
        fail(s"compiled economics client $moduleClassName could not be linked: $error")
    finally loader.close()

end EconomicsCompilerBoundarySuite

private val economicsForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
