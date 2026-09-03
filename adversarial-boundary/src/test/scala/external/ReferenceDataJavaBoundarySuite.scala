package external

import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import scala.jdk.CollectionConverters.*

import munit.FunSuite

class ReferenceDataJavaBoundarySuite extends FunSuite:
  private val fixturesRoot = Paths.get(getClass.getResource("/reference-data-java").toURI)
  private val classpath    =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated reference-data Java compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  private final case class Compilation(succeeded: Boolean, diagnostics: String)

  test("completed public artifacts support catalog-issued Java handles"):
    val output = Files.createTempDirectory("reference-data-java-positive")
    val result = compile(
      List(
        fixturesRoot.resolve("SharedReferenceDataJavaSetup.java"),
        fixturesRoot.resolve("positive/CatalogIssuedHandles.java"),
        fixturesRoot.resolve("positive/CatalogErrorConstructionBoundary.java"),
        fixturesRoot.resolve("positive/CatalogOutcomeInspectionBoundary.java"),
        fixturesRoot.resolve("positive/HandleConstructionBoundary.java"),
        fixturesRoot.resolve("positive/GridQuantumConstructionBoundary.java"),
        fixturesRoot.resolve("positive/GridReconciliationAuthority.java")
      ),
      output,
      classpath
    )
    assert(result.succeeded, result.diagnostics)

    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      List(
        "external.referencejava.CatalogIssuedHandles",
        "external.referencejava.CatalogErrorConstructionBoundary",
        "external.referencejava.CatalogOutcomeInspectionBoundary",
        "external.referencejava.HandleConstructionBoundary",
        "external.referencejava.GridQuantumConstructionBoundary",
        "external.referencejava.GridReconciliationAuthority"
      ).foreach: className =>
        val client = Class.forName(className, true, loader)
        val main   = client.getMethod("main", classOf[Array[String]])
        val _      = main.invoke(null, Array.empty[String])
    finally loader.close()

  private def compile(sources: List[Path], output: Path, compileClasspath: String): Compilation =
    val compiler = Option(ToolProvider.getSystemJavaCompiler).getOrElse:
      throw new IllegalStateException("a full JDK is required for Java boundary fixtures")
    val diagnostics = new DiagnosticCollector[JavaFileObject]
    val files       = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)
    try
      val units   = files.getJavaFileObjectsFromFiles(sources.map(_.toFile).asJava)
      val options = List(
        "--release",
        "25",
        "-proc:none",
        "-classpath",
        compileClasspath,
        "-d",
        output.toString
      )
      val succeeded = compiler.getTask(null, files, diagnostics, options.asJava, null, units).call()
      val rendered  = diagnostics.getDiagnostics.asScala
        .map(diagnostic => diagnostic.getMessage(Locale.ROOT))
        .mkString("\n")
      Compilation(succeeded, rendered)
    finally files.close()
  end compile
end ReferenceDataJavaBoundarySuite
