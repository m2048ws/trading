package external

import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.jar.JarFile
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import scala.jdk.CollectionConverters.*

import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.StoreReporter
import munit.FunSuite

class ExecutionLifecycleCompilerBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class JavaCompilation(output: Path, succeeded: Boolean, diagnostics: String)

  private val fixturesRoot       = Paths.get(getClass.getResource("/execution-lifecycle-compiler").toURI)
  private val javaRoot           = Paths.get(getClass.getResource("/execution-lifecycle-java").toURI)
  private val executionClasspath = loadClasspath("execution-lifecycle-compiler.classpath")

  test("completed execution-lifecycle classpath contains only its admitted pure dependency cone"):
    val entries = names(executionClasspath)
    List(
      "trading-quantities_3-",
      "trading-reference-data_3-",
      "trading-instrument-economics_3-",
      "trading-order-model_3-",
      "trading-execution-lifecycle_3-",
      "cats-core_3-"
    ).foreach(prefix => assert(entries.exists(_.startsWith(prefix)), s"missing $prefix from ${entries.mkString(", ")}"))
    List(
      "trading-execution-scenario_3-",
      "trading-fee-policy_3-",
      "trading-risk_3-",
      "trading-application_3-",
      "trading-runtime_3-",
      "cats-effect_3-",
      "fs2-core_3-",
      "circe-core_3-",
      "doobie-core_3-",
      "opentelemetry-",
      "jmh-core-"
    ).foreach(prefix => assert(!entries.exists(_.startsWith(prefix)), s"execution classpath retained $prefix"))

  test("completed execution-lifecycle JAR owns checked identities without forbidden references"):
    val archive = new JarFile(packagedExecutionJar.toFile)
    try
      val entries = archive.entries().asScala.toList
      val names   = entries.map(_.getName).toSet
      List(
        "ApplicationCommandId.class",
        "ExecutionOrderId.class",
        "OrderLineageId.class",
        "ExecutionSourceId.class",
        "ExecutionAccountId.class",
        "NativeSourceEventId.class",
        "NativeSourceOrderId.class",
        "NativeFillId.class",
        "SourceStreamId.class",
        "SourceSequence.class"
      ).foreach(name => assert(names.contains(s"trading/execution/$name"), s"missing $name"))

      val classBytes = entries
        .filter(entry => !entry.isDirectory && entry.getName.endsWith(".class"))
        .map: entry =>
          val stream = archive.getInputStream(entry)
          try new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1)
          finally stream.close()
        .mkString
      List(
        "trading/scenario/",
        "trading/fee/",
        "trading/risk/",
        "trading/application/",
        "trading/runtime/",
        "cats/effect/",
        "fs2/",
        "io/circe/",
        "doobie/",
        "java/sql/",
        "io/opentelemetry/",
        "org/openjdk/jmh/",
        "com/binance/"
      ).foreach(fragment => assert(!classBytes.contains(fragment), s"execution JAR retained reference $fragment"))
    finally archive.close()
    end try

  test("completed execution-lifecycle JAR compiles and runs a checked identity client"):
    val result = compile(fixturesRoot.resolve("positive/ExecutionIdentityBoundaryClient.scala"), executionClasspath)
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.execution.positive.ExecutionIdentityBoundaryClient$", "run")

  test("completed execution-lifecycle JAR compiles and runs authority, lifecycle, and closed command transitions"):
    val result = compile(
      List(
        fixturesRoot.resolve("ExecutionLifecycleSetup.scala"),
        fixturesRoot.resolve("positive/ExecutionAuthorityBoundaryClient.scala")
      ),
      executionClasspath
    )
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.execution.positive.ExecutionAuthorityBoundaryClient$", "run")

  test("execution-lifecycle classpath rejects downstream mechanisms"):
    val source  = fixturesRoot.resolve("negative/ExecutionLifecycleHasNoDownstream.scala")
    val prelude = compilePrelude(source, executionClasspath)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compile(source, executionClasspath)
    assert(rejected.errors.size >= 11, rejected.rendered)
    assert(rejected.rendered.contains("is not a member") || rejected.rendered.contains("Not found"), rejected.rendered)

  test("established upstream and sibling classpaths cannot import actual execution"):
    val source = fixturesRoot.resolve("negative/EstablishedOwnerCannotImportExecution.scala")
    List(
      "quantities"           -> "quantities-compiler.classpath",
      "instrument economics" -> "instrument-economics-compiler.classpath",
      "order model"          -> "order-model-compiler.classpath",
      "execution scenario"   -> "execution-scenario-compiler.classpath",
      "fee policy"           -> "fee-policy-compiler.classpath",
      "risk"                 -> "risk-compiler.classpath"
    ).foreach: (owner, resource) =>
      val ownerClasspath = loadClasspath(resource)
      assert(!names(ownerClasspath).exists(_.startsWith("trading-execution-lifecycle_3-")), clues(owner, resource))
      val rejected = compile(source, ownerClasspath)
      assert(!rejected.succeeded, s"$owner imported actual execution")
      assert(
        rejected.rendered.contains("trading.execution") ||
          rejected.rendered.contains("execution is not a member of trading") ||
          rejected.rendered.contains("ApplicationCommandId"),
        clues(owner, rejected.rendered)
      )

  test("completed JAR rejects same-package constructors, copies, and unknown alternatives"):
    val source  = fixturesRoot.resolve("negative/PackageSpoofExecutionAuthority.scala")
    val prelude = compilePrelude(source, executionClasspath)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compile(source, executionClasspath)
    assert(rejected.errors.size >= 11, rejected.rendered)
    assert(rejected.rendered.contains("cannot be accessed"), rejected.rendered)
    assert(rejected.rendered.contains("value copy is not a member"), rejected.rendered)

  test("remaining migration representations are final with JVM-private constructors"):
    List(
      Class.forName("trading.execution.ExecutionLifecycle"),
      Class.forName("trading.execution.SourceFactViolations"),
      Class.forName("trading.execution.OrderAccepted"),
      Class.forName("trading.execution.OrderRejected"),
      Class.forName("trading.execution.ExecutionFill"),
      Class.forName("trading.execution.FillCorrected"),
      Class.forName("trading.execution.FillBusted"),
      Class.forName("trading.execution.CancellationEffective"),
      Class.forName("trading.execution.ReconciliationCheckpoint"),
      Class.forName("trading.execution.SourceOrderCompleted"),
      Class.forName("trading.execution.SourceOrderAbsent"),
      Class.forName("trading.execution.SourceFactClassifications"),
      Class.forName("trading.execution.SourceFactConflict"),
      Class.forName("trading.execution.FillIdentityConflict"),
      Class.forName("trading.execution.StreamPositionConflict"),
      Class.forName("trading.execution.UnresolvedFillReference"),
      Class.forName("trading.execution.SourceFactRecorded"),
      Class.forName("trading.execution.SourceFactRejected"),
      Class.forName("trading.execution.SourceEvidenceState"),
      Class.forName("trading.execution.ModifierAmbiguity"),
      Class.forName("trading.execution.ActiveEffectiveFill"),
      Class.forName("trading.execution.BustedEffectiveFill"),
      Class.forName("trading.execution.AmbiguousEffectiveFill"),
      Class.forName("trading.execution.ConflictingEffectiveFill"),
      Class.forName("trading.execution.EffectiveFillLedger"),
      Class.forName("trading.execution.ExecutionState"),
      Class.forName("trading.execution.LifecycleAccepted"),
      Class.forName("trading.execution.LifecycleRejected"),
      Class.forName("trading.execution.LifecycleDiagnostics"),
      Class.forName("trading.execution.LifecycleObservation"),
      Class.forName("trading.execution.LifecycleReplayResult")
    ).foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} is not final")
      assert(
        representation.getDeclaredConstructors.forall(constructor => Modifier.isPrivate(constructor.getModifiers)),
        s"${representation.getName} exposes a non-private JVM constructor"
      )

  test("completed JAR rejects remaining unknown Java execution alternatives at construction"):
    val result = compileJava(javaRoot.resolve("positive/RejectedExecutionAlternatives.java"))
    assert(result.succeeded, result.diagnostics)
    val loader = new URLClassLoader(Array(result.output.toUri.toURL), getClass.getClassLoader)
    try
      val fixture = Class.forName("external.execution.positive.RejectedExecutionAlternatives", true, loader)
      assertEquals(
        fixture.getMethod("guardsRejectUnknownAlternatives").invoke(null),
        java.lang.Boolean.TRUE
      )
    finally loader.close()

    val rejected = compileJava(javaRoot.resolve("negative/RejectedExecutionIdentityConstruction.java"))
    assert(!rejected.succeeded, "same-package Java unexpectedly forged execution identities")
    assert(rejected.diagnostics.toLowerCase(Locale.ROOT).contains("private"), rejected.diagnostics)

    val representationHelper = Class.forName("trading.execution.IdentityRepresentation$")
    List("constructor", "construct").foreach: forbidden =>
      assert(
        !representationHelper.getMethods.exists(_.getName == forbidden),
        s"identity representation helper exposed public JVM method $forbidden"
      )

  private def loadClasspath(resourceName: String): String =
    val resource = Option(getClass.getResourceAsStream(s"/$resourceName")).getOrElse:
      throw new IllegalStateException(s"missing generated $resourceName")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  private def names(classpath: String): List[String] =
    classpath.split(File.pathSeparator).map(entry => Paths.get(entry).getFileName.toString).toList

  private def compilePrelude(source: Path, classpath: String): Compilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)
    val directory = Files.createTempDirectory("execution-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy, classpath)

  private def compile(source: Path, classpath: String): Compilation =
    compile(List(source), classpath)

  private def compile(sources: List[Path], classpath: String): Compilation =
    val output    = Files.createTempDirectory("execution-classes-")
    val reporter  = new StoreReporter()
    val arguments =
      List("-classpath", classpath, "-d", output.toString, "-Werror", "-source:future") ++ sources.map(_.toString)
    val _ = Main.process(arguments.toArray, reporter)
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def compileJava(source: Path): JavaCompilation =
    val compiler = Option(ToolProvider.getSystemJavaCompiler).getOrElse:
      throw new IllegalStateException("a full JDK is required for Java boundary fixtures")
    val diagnostics = new DiagnosticCollector[JavaFileObject]
    val files       = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)
    val output      = Files.createTempDirectory("execution-java-")
    try
      val units   = files.getJavaFileObjects(source.toFile)
      val options = List(
        "--release",
        "25",
        "-proc:none",
        "-classpath",
        executionClasspath,
        "-d",
        output.toString
      )
      val succeeded = compiler.getTask(null, files, diagnostics, options.asJava, null, units).call()
      val rendered  = diagnostics.getDiagnostics.asScala
        .map(diagnostic => diagnostic.getMessage(Locale.ROOT))
        .mkString("\n")
      JavaCompilation(output, succeeded, rendered)
    finally files.close()
  end compileJava

  private def runModule(output: Path, moduleName: String, method: String): Unit =
    val urls = (output +: executionClasspath.split(File.pathSeparator).map(Paths.get(_)).toSeq)
      .map(_.toUri.toURL)
      .toArray
    val loader = new URLClassLoader(urls, null)
    try
      val moduleClass = Class.forName(moduleName, true, loader)
      val module      = moduleClass.getField("MODULE$").get(null)
      val _           = moduleClass.getMethod(method).invoke(module)
    finally loader.close()

  private def packagedExecutionJar: Path =
    executionClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-execution-lifecycle_3-"))
      .getOrElse(fail("missing packaged execution-lifecycle artifact"))

end ExecutionLifecycleCompilerBoundarySuite
