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

final class ApplicationRuntimeBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private val fixturesRoot = Paths.get(getClass.getResource("/application-runtime-boundary").toURI)

  private val applicationClasspath = classpath("/application-boundary.classpath")
  private val runtimeClasspath     = classpath("/runtime-boundary.classpath")

  test("application packages one runtime-neutral port without concrete effect dependencies"):
    val entries        = classpathEntries(applicationClasspath)
    val applicationJar = exactlyOne(entries, "trading-application_3-")
    val internalJars   = entries.filter(_.getFileName.toString.startsWith("trading-"))

    assertEquals(
      internalJars.map(_.getFileName.toString.takeWhile(_ != '_')).toSet,
      Set("trading-quantities", "trading-reference-data", "trading-application")
    )
    assert(!entries.exists(_.getFileName.toString.startsWith("cats-effect")), entries.mkString("\n"))
    assert(!entries.exists(_.getFileName.toString.startsWith("fs2-")), entries.mkString("\n"))

    val jar = new JarFile(applicationJar.toFile)
    try
      val classEntries = jar.entries().asScala.filter(entry => entry.getName.endsWith(".class")).toVector
      assertEquals(classEntries.map(_.getName).toSet, Set("trading/application/LiveCatalog.class"))

      val bytes = classEntries.flatMap: entry =>
        val input = jar.getInputStream(entry)
        try input.readAllBytes().toVector
        finally input.close()
      val symbols = new String(bytes.toArray, StandardCharsets.ISO_8859_1)
      applicationForbiddenSymbols.foreach: symbol =>
        assert(!symbols.contains(symbol), s"application artifact leaked forbidden runtime symbol '$symbol'")
    finally jar.close()

  test("runtime alone owns Cats Effect and keeps FS2 unadmitted"):
    val entries    = classpathEntries(runtimeClasspath)
    val runtimeJar = exactlyOne(entries, "trading-runtime_3-")

    assert(Files.isRegularFile(runtimeJar), runtimeJar.toString)
    assert(entries.exists(_.getFileName.toString.startsWith("cats-effect_3-3.7.0")), entries.mkString("\n"))
    assert(entries.exists(_.getFileName.toString.startsWith("cats-effect-kernel_3-3.7.0")), entries.mkString("\n"))
    assert(!entries.exists(_.getFileName.toString.startsWith("fs2-")), entries.mkString("\n"))

  test("an external client compiles against the completed application artifact without Cats Effect"):
    val result = compile(
      fixturesRoot.resolve("positive/ApplicationOnlyClient.scala"),
      applicationClasspath
    )
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.application.positive.ApplicationOnlyClient$")

  test("the runtime classpath admits Cats Effect only above the application artifact"):
    val result = compile(
      fixturesRoot.resolve("positive/RuntimeDependencyClient.scala"),
      runtimeClasspath
    )
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.application.positive.RuntimeDependencyClient$")

  test("the application-only completed-artifact classpath rejects a concrete effect"):
    val source  = fixturesRoot.resolve("negative/ConcreteEffectLeak.scala")
    val prelude = compilePrelude(source, applicationClasspath)
    assert(prelude.succeeded, s"negative fixture prelude failed:\n${prelude.rendered}")

    val rejected = compile(source, applicationClasspath)
    assert(!rejected.succeeded, "Cats Effect unexpectedly compiled on the application-only classpath")
    assert(rejected.rendered.contains("effect"), rejected.rendered)
    assert(rejected.rendered.contains("cats"), rejected.rendered)
    compilerForbiddenDiagnostics.foreach: diagnostic =>
      assert(!rejected.rendered.contains(diagnostic), rejected.rendered)

  test("runtime callers cannot name or construct the private Ref-backed interpreter"):
    val source  = fixturesRoot.resolve("negative/RuntimeInternalsUnavailable.scala")
    val prelude = compilePrelude(source, runtimeClasspath)
    assert(prelude.succeeded, s"negative fixture prelude failed:\n${prelude.rendered}")

    val rejected = compile(source, runtimeClasspath)
    assert(!rejected.succeeded, "the private Ref-backed interpreter unexpectedly compiled for an external caller")
    assert(rejected.rendered.contains("RefBackedLiveCatalog"), rejected.rendered)
    assert(rejected.rendered.contains("cannot be accessed"), rejected.rendered)
    compilerForbiddenDiagnostics.foreach: diagnostic =>
      assert(!rejected.rendered.contains(diagnostic), rejected.rendered)

  private def classpath(resourceName: String): String =
    val resource = Option(getClass.getResourceAsStream(resourceName)).getOrElse:
      throw new IllegalStateException(s"missing generated classpath $resourceName")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  private def classpathEntries(value: String): List[Path] =
    value.split(File.pathSeparator).toList.map(Paths.get(_))

  private def exactlyOne(entries: List[Path], prefix: String): Path =
    val matches = entries.filter(_.getFileName.toString.startsWith(prefix))
    assertEquals(matches.size, 1, matches.mkString("\n"))
    matches.head

  private def compilePrelude(source: Path, compilationClasspath: String): Compilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)

    val directory = Files.createTempDirectory("application-boundary-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy, compilationClasspath)

  private def compile(source: Path, compilationClasspath: String): Compilation =
    val output   = Files.createTempDirectory("application-runtime-boundary-")
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
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def initializeModule(output: Path, moduleClassName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      assert(moduleClass.getField("MODULE$").get(null) != null, s"$moduleClassName was not initialized")
    catch
      case error: ReflectiveOperationException => fail(s"compiled module could not be loaded: $error")
      case error: LinkageError                 => fail(s"compiled module could not be linked: $error")
    finally loader.close()

end ApplicationRuntimeBoundarySuite

private val applicationForbiddenSymbols = Vector(
  "cats/effect/IO",
  "cats/effect/Ref",
  "cats/effect/Resource",
  "cats/effect/kernel/Fiber",
  "cats/effect/std/Queue",
  "fs2/Stream",
  "java/util/concurrent/locks/Lock",
  "Transaction",
  "Tracer",
  "Meter"
)

private val compilerForbiddenDiagnostics = Vector(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "illegal cyclic type reference",
  "caught cyclic reference",
  "See full stack trace",
  "at dotty.tools"
)
