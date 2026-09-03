package external

import java.io.File
import java.lang.reflect.Modifier
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

class RiskCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private val fixturesRoot = Paths.get(getClass.getResource("/risk-compiler").toURI)
  private val classpath    =
    val resource = Option(getClass.getResourceAsStream("/risk-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated risk compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("completed risk classpath contains only its pure upstream module graph"):
    val entries = classpath.split(File.pathSeparator).map(Paths.get(_).getFileName.toString).toList
    List(
      "trading-quantities_3-",
      "trading-reference-data_3-",
      "trading-instrument-economics_3-",
      "trading-risk_3-"
    ).foreach(prefix => assert(entries.exists(_.startsWith(prefix)), s"missing $prefix from ${entries.mkString(", ")}"))
    List(
      "trading-order-model_3-",
      "trading-execution-scenario_3-",
      "trading-fee-policy_3-",
      "trading-application_3-",
      "trading-runtime_3-",
      "cats-effect_3-",
      "fs2-core_3-",
      "circe-core_3-",
      "doobie-core_3-",
      "opentelemetry-",
      "jmh-core-"
    ).foreach(prefix => assert(!entries.exists(_.startsWith(prefix)), s"risk classpath retained $prefix"))

  test("completed risk JAR owns its non-empty identity-error boundary without forbidden references"):
    val archive = new JarFile(packagedRiskJar.toFile)
    try
      val entries = archive.entries().asScala.toList
      val names   = entries.map(_.getName).toSet
      assert(names.contains("trading/risk/RiskIdentityError.class"))
      assert(names.contains("trading/risk/DownsideInstrumentMismatch.class"))
      val classBytes = entries
        .filter(entry => !entry.isDirectory && entry.getName.endsWith(".class"))
        .map: entry =>
          val stream = archive.getInputStream(entry)
          try new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1)
          finally stream.close()
        .mkString
      List(
        "trading/order/",
        "trading/scenario/",
        "trading/fee/",
        "trading/application/",
        "trading/runtime/",
        "cats/effect/",
        "fs2/",
        "io/circe/",
        "doobie/",
        "java/time/Clock",
        "java/sql/",
        "io/opentelemetry/",
        "org/openjdk/jmh/"
      ).foreach(fragment => assert(!classBytes.contains(fragment), s"risk JAR retained reference $fragment"))
    finally archive.close()
    end try

  test("completed risk JAR compiles and runs a risk-only client"):
    val result = compile(fixturesRoot.resolve("positive/RiskBoundaryClient.scala"))
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.risk.positive.RiskBoundaryClient$", "run")

  test("completed risk classpath rejects downstream, effect, codec, persistence, telemetry, and benchmark concerns"):
    val source  = fixturesRoot.resolve("negative/RiskHasNoDownstream.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compile(source)
    assert(rejected.errors.size >= 12, rejected.rendered)
    assert(rejected.rendered.contains("is not a member") || rejected.rendered.contains("Not found"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    forbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed risk API rejects raw refinements, dimension mismatches, and arbitrary monotonicity promises"):
    val source  = fixturesRoot.resolve("negative/InvalidRiskInputs.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compile(source)
    assert(rejected.errors.size >= 4, rejected.rendered)
    assert(rejected.rendered.contains("Found:"), rejected.rendered)
    assert(rejected.rendered.contains("Required:"), rejected.rendered)
    forbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("assessment and model representations stay final without exposing arbitrary certification"):
    List(
      Class.forName("trading.risk.LotRiskAssessment"),
      Class.forName("trading.risk.ModelViolations"),
      Class.forName("trading.risk.MonotoneLotRisk")
    ).foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} is not final")
    val model = Class.forName("trading.risk.MonotoneLotRisk")
    assert(!model.getMethods.exists(_.getName == "makeLots"), "model exposes its total lot constructor to Java")
    assert(
      !model.getMethods.exists(_.getParameterTypes.contains(classOf[Function1[?, ?]])),
      "model exposes a public arbitrary-function certification path"
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
    val directory = Files.createTempDirectory("risk-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy)

  private def compile(source: Path): Compilation =
    val output   = Files.createTempDirectory("risk-classes-")
    val reporter = new StoreReporter()
    val _        = Main.process(
      Array("-classpath", classpath, "-d", output.toString, "-Werror", "-source:future", source.toString),
      reporter
    )
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def packagedRiskJar: Path =
    classpath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-risk_3-"))
      .getOrElse(fail("missing packaged risk artifact"))

  private def runModule(output: Path, moduleClassName: String, methodName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      val module      = moduleClass.getField("MODULE$").get(null)
      val _           = moduleClass.getMethod(methodName).invoke(module)
    catch
      case error: java.lang.reflect.InvocationTargetException =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled risk client $moduleClassName.$methodName failed: $cause")
      case error: ReflectiveOperationException =>
        fail(s"compiled risk client $moduleClassName.$methodName could not be invoked: $error")
      case error: LinkageError =>
        fail(s"compiled risk client $moduleClassName.$methodName could not be linked: $error")
    finally loader.close()
end RiskCompilerBoundarySuite

private val forbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
