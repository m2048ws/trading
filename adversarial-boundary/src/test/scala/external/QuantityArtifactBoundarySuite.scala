package external

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.StoreReporter
import munit.FunSuite

class QuantityArtifactBoundarySuite extends FunSuite:
  private final case class Compilation(errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private val fixturesRoot = Paths.get(getClass.getResource("/quantity-artifact-boundary").toURI)

  private val quantitiesOnlyClasspath =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated compiler classpath")
    val complete =
      try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
      finally resource.close()
    complete
      .split(File.pathSeparator)
      .filterNot: entry =>
        val name = Paths.get(entry).getFileName.toString
        name.startsWith("trading-reference-data_3-") ||
        name.startsWith("trading-risk_3-") ||
        name.startsWith("trading-fee-policy_3-") ||
        name.startsWith("trading-boundary-codecs_3-")
      .mkString(File.pathSeparator)

  test("real downstream source compiles against the completed quantities JAR without reference data"):
    val result = compile(fixturesRoot.resolve("positive/QuantitiesOnlyClient.scala"))
    assert(result.succeeded, result.rendered)

  test("the quantities-only classpath rejects stable identity, catalog, and packed-record ownership"):
    val source  = fixturesRoot.resolve("negative/StableIdentityUnavailable.scala")
    val prelude = compilePrelude(source)
    assert(prelude.succeeded, s"negative fixture prelude failed independently:\n${prelude.rendered}")

    val rejected = compile(source)
    assert(rejected.errors.size >= 8, rejected.rendered)
    assert(rejected.rendered.contains("reference is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("risk is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("fee is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("runtime is not a member of trading.quantity"), rejected.rendered)
    artifactForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  private def compilePrelude(source: Path): Compilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)

    val directory = Files.createTempDirectory("quantities-only-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy)

  private def compile(source: Path): Compilation =
    val output   = Files.createTempDirectory("quantities-only-classes-")
    val reporter = new StoreReporter()
    val _        = Main.process(
      Array(
        "-classpath",
        quantitiesOnlyClasspath,
        "-d",
        output.toString,
        "-Werror",
        "-source:future",
        source.toString
      ),
      reporter
    )
    Compilation(reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

end QuantityArtifactBoundarySuite

private val artifactForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "See full stack trace",
  "at dotty.tools"
)
