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

  test("packaged artifacts preserve the quantities to reference-data to economics boundary"):
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
    val quantitiesArchive = new JarFile(quantitiesJar.toFile)
    val referenceArchive  = new JarFile(referenceDataJar.toFile)
    try
      val quantityEntries  = quantitiesArchive.entries().asScala.map(_.getName).toSet
      val referenceEntries = referenceArchive.entries().asScala.map(_.getName).toSet
      assert(!quantityEntries.exists(_.startsWith("trading/reference/")))
      assert(!quantityEntries.exists(_.startsWith("trading/economics/")))
      List(
        "AssetId.class",
        "GridId.class",
        "GridVersion.class",
        "GridKey.class",
        "GridIdentity.class",
        "QuantityRegistry.class",
        "Asset.class",
        "DimensionHandle.class",
        "GridHandle.class"
      ).foreach: name =>
        assert(!quantityEntries.contains(s"trading/quantity/$name"), s"quantity JAR retained $name")
        assert(referenceEntries.contains(s"trading/reference/$name"), s"reference-data JAR is missing $name")
      assert(!referenceEntries.exists(_.startsWith("trading/economics/")))
    finally
      quantitiesArchive.close()
      referenceArchive.close()
    end try

  test("economics artifact exposes concise instrument concern classes without stale flat API names"):
    val economicsJar = packagedEconomicsJar
    val jar          = new JarFile(economicsJar.toFile)
    try
      val entries  = jar.entries().asScala.map(_.getName).toSet
      val expected = List(
        "Instrument.class",
        "Prices.class",
        "Market.class",
        "Orders.class",
        "Scenarios.class",
        "Fees.class",
        "Valuation.class",
        "Sizing.class",
        "Mismatch.class"
      ).map(name => s"trading/economics/instrument/$name")
      expected.foreach(entry => assert(entries.contains(entry), s"missing $entry from $economicsJar"))

      val forbidden = entries.filter(entry =>
        entry.startsWith("trading/economics/instrument/") &&
          (entry.contains("OwnerAuthority") || entry.contains("JvmOwnerAuthority") || entry.endsWith("Impl.class"))
      )
      assertEquals(forbidden, Set.empty[String])

      val stale = List(
        "trading/economics/Instrument.class",
        "trading/economics/instrument/InstrumentPrices.class",
        "trading/economics/instrument/InstrumentMarket.class",
        "trading/economics/instrument/InstrumentOrders.class",
        "trading/economics/instrument/InstrumentScenarios.class",
        "trading/economics/instrument/InstrumentFees.class",
        "trading/economics/instrument/InstrumentValuation.class",
        "trading/economics/instrument/InstrumentSizing.class",
        "trading/economics/instrument/InstrumentMismatch.class"
      )
      stale.foreach(entry => assert(!entries.contains(entry), s"stale $entry remains in $economicsJar"))
    finally jar.close()
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
    NegativeFixture("RemovedFlatApi.scala", List("is not a member"), 10, Some(10)),
    NegativeFixture("RemovedOwnerApi.scala", List("is not a member", "Owner"), 5, Some(5)),
    NegativeFixture("RefinementLoss.scala", List("Found:", "Required:"), 5, Some(5)),
    NegativeFixture("DeferredLifecycle.scala", List("is not a member"), 9, Some(9)),
    NegativeFixture("AssociatedEvidenceShapes.scala", List("Found:", "Required:"), 6),
    NegativeFixture("ValidatedDefinitionAuthority.scala", List("cannot be accessed"), 5),
    NegativeFixture("ReversedSettlementRate.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("ConversionDoesNotGrantGrid.scala", List("Found:", "Required:"), 1, Some(1))
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
    compile(copy)

  private def compile(source: Path): Compilation =
    val output    = Files.createTempDirectory("economics-classes-")
    val reporter  = new StoreReporter()
    val arguments = Array(
      "-classpath",
      compilationClasspath,
      "-d",
      output.toString,
      "-Werror",
      "-source:future",
      sharedFixture.toString,
      source.toString
    )
    val _ = Main.process(arguments, reporter)
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def packagedEconomicsJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-economics_3-"))
      .getOrElse(fail("missing packaged economics artifact"))

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

end EconomicsCompilerBoundarySuite

private val economicsForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
