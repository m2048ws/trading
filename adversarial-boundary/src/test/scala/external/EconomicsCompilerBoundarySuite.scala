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

class EconomicsCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class NegativeFixture(
    file: String,
    expected: List[String],
    minimumErrors: Int,
    exactErrors: Option[Int] = None)

  private val fixturesRoot         = Paths.get(getClass.getResource("/economics-compiler").toURI)
  private val compilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated external compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("quantities artifact remains free of economics classes"):
    val quantitiesJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-quantities_3-"))
      .getOrElse(fail("missing packaged quantities artifact"))
    val jar = new JarFile(quantitiesJar.toFile)
    try
      assert(!jar.entries().asScala.exists(_.getName.startsWith("trading/economics/")))
    finally jar.close()

  test("positive downstream economics fixture compiles without warnings"):
    val result = compile(fixturesRoot.resolve("positive/CompleteEconomicsClient.scala"))
    assert(result.succeeded, result.rendered)

  private val negativeFixtures = List(
    NegativeFixture("CrossInstrumentMixing.scala", List("Required:", "first"), 6),
    NegativeFixture("PrivateConstruction.scala", List("sealed trait", "Found:", "Required:"), 4),
    NegativeFixture(
      "PackageSpoofConstruction.scala",
      List("Not found", "cannot be accessed as a member", "InstrumentImpl", "SettlementConversionImpl"),
      4,
      Some(4)
    ),
    NegativeFixture("DeferredLifecycle.scala", List("is not a member"), 9)
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
      source.toString
    )
    val _ = Main.process(arguments, reporter)
    Compilation(reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

end EconomicsCompilerBoundarySuite

private val economicsForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
