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

class ReferenceDataCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class NegativeFixture(
    file: String,
    expected: List[String],
    minimumErrors: Int = 1)

  private val fixturesRoot         = Paths.get(getClass.getResource("/reference-data-compiler").toURI)
  private val sharedFixture        = fixturesRoot.resolve("SharedReferenceDataSetup.scala")
  private val compilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated reference-data compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("reference-data fixtures consume completed immutable public artifacts"):
    val entries       = compilationClasspath.split(File.pathSeparator).toList.map(Paths.get(_))
    val quantities    = entries.filter(_.getFileName.toString.startsWith("trading-quantities_3-"))
    val referenceData = entries.filter(_.getFileName.toString.startsWith("trading-reference-data_3-"))
    val application   = entries.filter(_.getFileName.toString.startsWith("trading-application_3-"))
    val feePolicy     = entries.filter(_.getFileName.toString.startsWith("trading-fee-policy_3-"))

    assertEquals(quantities.size, 1)
    assertEquals(referenceData.size, 1)
    assertEquals(application.size, 1)
    assertEquals(feePolicy.size, 1)
    assert(entries.forall(Files.isRegularFile(_)), entries.mkString("\n"))
    assert(quantities.head.getFileName.toString.endsWith(".jar"))
    assert(referenceData.head.getFileName.toString.endsWith(".jar"))
    assert(application.head.getFileName.toString.endsWith(".jar"))

    (quantities ++ referenceData ++ feePolicy).foreach: artifact =>
      val jar = new JarFile(artifact.toFile)
      try
        val names = jar.entries().asScala.map(_.getName).toVector
        assert(!names.exists(_.startsWith("trading/application/")), artifact.toString)
      finally jar.close()

    val applicationJar = new JarFile(application.head.toFile)
    try
      val names = applicationJar.entries().asScala.map(_.getName).toSet
      assert(names.contains("trading/application/LiveCatalog.class"))
      assertEquals(
        names.filter(name => name.startsWith("trading/application/") && name.endsWith(".class")),
        Set("trading/application/LiveCatalog.class")
      )
    finally applicationJar.close()

  private val positiveFixtures = List(
    "ConcreteReferenceDataClient.scala"          -> "external.reference.positive.ConcreteReferenceDataClient$",
    "GenericReferenceDataClient.scala"           -> "external.reference.positive.GenericReferenceDataClient$",
    "ApplicationPortClient.scala"                -> "external.reference.positive.ApplicationPortClient$",
    "CatalogViolationConstructionBoundary.scala" ->
      "external.reference.positive.CatalogViolationConstructionBoundary$",
    "CatalogOutcomeInspectionBoundary.scala" ->
      "external.reference.positive.CatalogOutcomeInspectionBoundary$"
  )

  positiveFixtures.foreach: (file, moduleClass) =>
    test(s"positive packaged reference-data fixture compiles and runs $file"):
      val result = compile(fixturesRoot.resolve("positive").resolve(file))
      assert(result.succeeded, result.rendered)
      initializeModule(result.output, moduleClass)

  private val negativeFixtures = List(
    NegativeFixture(
      "TrustedHandleImplementation.scala",
      List("cannot be accessed", "DimensionHandle", "Asset", "GridHandle"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "InternalImplementationAccess.scala",
      List("cannot be accessed", "handlePermit", "lineage", "CatalogState", "CatalogSnapshot", "Reconciliation"),
      minimumErrors = 5
    ),
    NegativeFixture(
      "AnonymousGridStablePromotion.scala",
      List("Found:", "Required:", "GridHandle")
    ),
    NegativeFixture(
      "RemovedStableGridFactory.scala",
      List("method create in object UniformGrid", "too many arguments")
    ),
    NegativeFixture(
      "StableIdentityConstructors.scala",
      List("cannot be accessed", "AssetId", "GridId", "GridVersion"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "StableIdentityApply.scala",
      List("does not take parameters", "AssetId", "GridId", "GridVersion"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "StableIdentityCopy.scala",
      List("copy", "is not a member", "AssetId", "GridId", "GridVersion"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "GridDefinitionProductBypass.scala",
      List("Found:", "Positive", "copy", "fromProduct", "is not a member"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "CatalogGuardedConstruction.scala",
      List("cannot be accessed", "CatalogBatch", "CatalogRevision", "CatalogDelta", "CatalogViolations"),
      minimumErrors = 4
    ),
    NegativeFixture(
      "CatalogOutcomeConstruction.scala",
      List("cannot be accessed", "CatalogTransition", "Published", "Unchanged"),
      minimumErrors = 3
    )
  )

  negativeFixtures.foreach: fixture =>
    test(s"negative packaged reference-data fixture rejects ${fixture.file}"):
      val source  = fixturesRoot.resolve("negative").resolve(fixture.file)
      val prelude = compilePrelude(source)
      assert(
        prelude.succeeded,
        s"fixture prelude must compile independently for ${fixture.file}:\n${prelude.rendered}"
      )

      val rejected = compile(source)
      assert(
        rejected.errors.size >= fixture.minimumErrors,
        s"expected ${fixture.file} to have at least ${fixture.minimumErrors} errors:\n${rejected.rendered}"
      )
      fixture.expected.foreach: fragment =>
        assert(
          rejected.rendered.contains(fragment),
          s"expected ${fixture.file} diagnostic to contain '$fragment':\n${rejected.rendered}"
        )
      referenceDataForbiddenDiagnostics.foreach: fragment =>
        assert(
          !rejected.rendered.contains(fragment),
          s"unexpected internal compiler diagnostic '$fragment' for ${fixture.file}:\n${rejected.rendered}"
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

    val directory = Files.createTempDirectory("reference-data-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy)

  private def compile(source: Path): Compilation =
    val output   = Files.createTempDirectory("reference-data-classes-")
    val reporter = new StoreReporter()
    val _        = Main.process(
      Array(
        "-classpath",
        compilationClasspath,
        "-d",
        output.toString,
        "-Werror",
        "-source:future",
        sharedFixture.toString,
        source.toString
      ),
      reporter
    )
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def initializeModule(output: Path, moduleClassName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      assert(moduleClass.getField("MODULE$").get(null) != null, s"$moduleClassName was not initialized")
    catch
      case error: ExceptionInInitializerError =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled positive reference-data client failed during module initialization: $cause")
      case error: ReflectiveOperationException =>
        fail(s"compiled positive reference-data client module could not be loaded: $error")
      case error: LinkageError =>
        fail(s"compiled positive reference-data client module could not be linked: $error")
    finally loader.close()

end ReferenceDataCompilerBoundarySuite

private val referenceDataForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
