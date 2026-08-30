package external

import java.io.DataInputStream
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
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

final class ApplicationRuntimeBoundarySuite extends FunSuite:
  private final case class Compilation(output: Path, errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class JavaCompilation(succeeded: Boolean, diagnostics: String)

  private val fixturesRoot = Paths.get(getClass.getResource("/application-runtime-boundary").toURI)

  private val applicationClasspath = classpath("/application-boundary.classpath")
  private val runtimeClasspath     = classpath("/runtime-boundary.classpath")

  test("application packages one narrow runtime-neutral port without architecture containers or concrete effects"):
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
        assert(!symbols.contains(symbol), s"application artifact leaked forbidden architecture symbol '$symbol'")
    finally jar.close()

    val publicMethods = classOf[trading.application.LiveCatalog[?]].getDeclaredMethods.map(_.getName).toSet
    assertEquals(publicMethods, Set("snapshot", "commit"))

  test("runtime alone owns Cats Effect and keeps FS2 unadmitted"):
    val entries    = classpathEntries(runtimeClasspath)
    val runtimeJar = exactlyOne(entries, "trading-runtime_3-")

    assert(Files.isRegularFile(runtimeJar), runtimeJar.toString)
    assert(entries.exists(_.getFileName.toString.startsWith("cats-effect_3-3.7.0")), entries.mkString("\n"))
    assert(entries.exists(_.getFileName.toString.startsWith("cats-effect-kernel_3-3.7.0")), entries.mkString("\n"))
    assert(!entries.exists(_.getFileName.toString.startsWith("fs2-")), entries.mkString("\n"))

    val jar = new JarFile(runtimeJar.toFile)
    try
      val classEntries = jar.entries().asScala.filter(entry => entry.getName.endsWith(".class")).map(_.getName).toSet
      val bridgeEntry  = "trading/runtime/LiveCatalogBridge.class"
      val implementationEntry = "trading/runtime/LiveCatalogBridge$RefBackedLiveCatalog.class"
      assert(classEntries.contains(bridgeEntry), classEntries.mkString("\n"))
      assert(classEntries.contains(implementationEntry), classEntries.mkString("\n"))
      assert(!classEntries.exists(_.contains("InMemoryLiveCatalog$$anon$")), classEntries.mkString("\n"))

      assertEquals(classFileMajor(jar, bridgeEntry), 61)
      assertEquals(classFileMajor(jar, implementationEntry), 61)

      val bridge = Class.forName(bridgeEntry.stripSuffix(".class").replace('/', '.'))
      assert(!Modifier.isPublic(bridge.getModifiers), bridge.toString)
      val factoryField = bridge.getDeclaredField("FACTORY_CLASS")
      factoryField.setAccessible(true)
      val guardedFactory = factoryField.get(null).asInstanceOf[Class[?]]
      val actualFactory  = Class.forName("trading.runtime.InMemoryLiveCatalog$", false, bridge.getClassLoader)
      assert(guardedFactory.eq(actualFactory))
      assertEquals(
        guardedFactory.getProtectionDomain.getCodeSource,
        bridge.getProtectionDomain.getCodeSource
      )
      val create = bridge.getDeclaredMethods.find(_.getName == "create").getOrElse(fail("missing bridge create"))
      assert(!Modifier.isPublic(create.getModifiers), create.toString)
      create.setAccessible(true)
      val rejected = intercept[InvocationTargetException](create.invoke(null, null, null))
      assert(rejected.getCause.isInstanceOf[SecurityException], rejected.getCause)

      val implementation = Class.forName(implementationEntry.stripSuffix(".class").replace('/', '.'))
      assert(Modifier.isPrivate(implementation.getModifiers), implementation.toString)
      val constructors = implementation.getDeclaredConstructors.toList
      assertEquals(constructors.size, 1)
      val constructor = constructors.head
      assert(Modifier.isPrivate(constructor.getModifiers), constructor.toString)
      val parameterTypes = constructor.getParameterTypes.map(_.getName).toSet
      assert(!parameterTypes.contains("cats.effect.kernel.Ref"), constructor.toString)
      assert(!parameterTypes.contains("cats.effect.kernel.Sync"), constructor.toString)
      constructor.setAccessible(true)
      val reflectiveConstruction = intercept[InvocationTargetException](constructor.newInstance(null, null))
      assert(reflectiveConstruction.getCause.isInstanceOf[SecurityException], reflectiveConstruction.getCause)

      val privateLookup     = MethodHandles.privateLookupIn(implementation, MethodHandles.lookup())
      val constructorType   = MethodType.methodType(java.lang.Void.TYPE, constructor.getParameterTypes.toList.asJava)
      val constructorHandle = privateLookup.findConstructor(implementation, constructorType)
      val _                 = intercept[SecurityException]:
        constructorHandle.invokeWithArguments(List[Object](null, null).asJava)
      implementation.getDeclaredFields.foreach: field =>
        assert(!field.getType.getName.contains("cats.effect.kernel.Ref"), field.toString)
        assert(!field.getType.getName.contains("cats.effect.kernel.Sync"), field.toString)
    finally jar.close()
    end try

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

  test("Scala runtime callers cannot name or construct the private Ref-backed interpreter"):
    val source  = fixturesRoot.resolve("negative/RuntimeInternalsUnavailable.scala")
    val prelude = compilePrelude(source, runtimeClasspath)
    assert(prelude.succeeded, s"negative fixture prelude failed:\n${prelude.rendered}")

    val rejected = compile(source, runtimeClasspath)
    assert(!rejected.succeeded, "the private Ref-backed interpreter unexpectedly compiled for an external caller")
    assert(rejected.rendered.contains("RefBackedLiveCatalog"), rejected.rendered)
    compilerForbiddenDiagnostics.foreach: diagnostic =>
      assert(!rejected.rendered.contains(diagnostic), rejected.rendered)

  test("Java runtime callers cannot name or invoke the private interpreter implementation"):
    val source = fixturesRoot.resolve("negative/RuntimeInternalsUnavailable.java")
    val output = Files.createTempDirectory("application-runtime-java-negative-")
    val result = compileJava(source, output, runtimeClasspath)

    assert(!result.succeeded, "the Ref-backed interpreter unexpectedly compiled for a Java caller")
    assert(result.diagnostics.contains("RefBackedLiveCatalog"), result.diagnostics)
    assert(
      result.diagnostics.contains("has private access") || result.diagnostics.contains("cannot be accessed"),
      result.diagnostics
    )

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

  private def classFileMajor(jar: JarFile, entryName: String): Int =
    val input = new DataInputStream(jar.getInputStream(jar.getJarEntry(entryName)))
    try
      assertEquals(input.readInt(), 0xcafebabe)
      val _ = input.readUnsignedShort()
      input.readUnsignedShort()
    finally input.close()

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

  private def compileJava(source: Path, output: Path, compilationClasspath: String): JavaCompilation =
    val compiler = Option(ToolProvider.getSystemJavaCompiler).getOrElse:
      throw new IllegalStateException("a full JDK is required for Java boundary fixtures")
    val diagnostics = new DiagnosticCollector[JavaFileObject]
    val files       = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)
    try
      val units   = files.getJavaFileObjectsFromFiles(List(source.toFile).asJava)
      val options = List(
        "--release",
        "17",
        "-proc:none",
        "-classpath",
        compilationClasspath,
        "-d",
        output.toString
      )
      val succeeded = compiler.getTask(null, files, diagnostics, options.asJava, null, units).call()
      val rendered  = diagnostics.getDiagnostics.asScala
        .map(diagnostic => diagnostic.getMessage(Locale.ROOT))
        .mkString("\n")
      JavaCompilation(succeeded, rendered)
    finally files.close()
  end compileJava

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
  "cats/effect/kernel/Clock",
  "cats/effect/kernel/Temporal",
  "cats/effect/std/Queue",
  "cats/free/Free",
  "fs2/Stream",
  "scala/concurrent/ExecutionContext",
  "java/util/concurrent",
  "java/util/concurrent/locks/Lock",
  "ApplicationEnvironment",
  "ApplicationEnv",
  "ServiceLocator",
  "CapabilityRegistry",
  "ApplicationError",
  "UniversalError",
  "CallbackRegistry",
  "Scheduler",
  "MarketData",
  "TradeStore",
  "OrderExecution",
  "ValuationService",
  "PnlService",
  "Transaction",
  "Telemetry",
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
