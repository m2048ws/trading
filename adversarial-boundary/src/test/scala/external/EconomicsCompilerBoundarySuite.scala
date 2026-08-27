package external

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.tools.asm.ClassReader
import scala.tools.asm.ClassVisitor
import scala.tools.asm.MethodVisitor
import scala.tools.asm.Opcodes

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

  private final case class JavaCompilation(exitCode: Int, rendered: String):
    def succeeded: Boolean = exitCode == 0

  private final case class JavaNegativeFixture(file: String, expected: List[String])

  private final case class ClassInfo(version: Int, access: Int, constructors: List[(Int, String)])

  private val fixturesRoot         = Paths.get(getClass.getResource("/economics-compiler").toURI)
  private val javaFixturesRoot     = Paths.get(getClass.getResource("/economics-java").toURI)
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

  test("economics artifact contains the public instrument and focused capability engines"):
    val economicsJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-economics_3-"))
      .getOrElse(fail("missing packaged economics artifact"))
    val jar = new JarFile(economicsJar.toFile)
    try
      val entries  = jar.entries().asScala.map(_.getName).toSet
      val expected = List(
        "Instrument.class",
        "InstrumentPricesImpl.class",
        "InstrumentMarketImpl.class",
        "InstrumentOrdersImpl.class",
        "InstrumentScenariosImpl.class",
        "InstrumentFeesImpl.class",
        "InstrumentValuationImpl.class",
        "InstrumentSizingImpl.class"
      ).map(name => s"trading/economics/$name")
      expected.foreach(entry => assert(entries.contains(entry), s"missing $entry from $economicsJar"))

      val removed = List(
        "ActivationKind.class",
        "PriceInstructionKind.class",
        "VisibilityKind.class",
        "InstrumentOrders$OrderPlan.class",
        "InstrumentScenarios$OrderView.class"
      ).map(name => s"trading/economics/$name")
      removed.foreach(entry => assert(!entries.contains(entry), s"superseded $entry remains in $economicsJar"))
    finally jar.close()
    end try

  test("economics JVM artifact closes authority and every trusted carrier constructor"):
    val economicsJar = packagedEconomicsJar
    val jar          = new JarFile(economicsJar.toFile)
    try
      val entries          = jar.entries().asScala.map(_.getName).toSet
      val authorityEntries = entries.filter(_.contains("OwnerAuthority"))
      assertEquals(
        authorityEntries,
        Set(
          "trading/economics/JvmOwnerAuthority.class",
          "trading/economics/Instrument$OwnerAuthority.class"
        )
      )

      val authority = classInfo(jar, "trading/economics/JvmOwnerAuthority.class")
      assertEquals(authority.access & Opcodes.ACC_PUBLIC, 0)
      assert(authority.constructors.nonEmpty, "missing JVM authority constructor")
      authority.constructors.foreach: (access, _) =>
        assert((access & Opcodes.ACC_PRIVATE) != 0, s"JVM authority constructor is not private in $economicsJar")

      val authorityDescriptor = "Ltrading/economics/JvmOwnerAuthority;"
      val scalaAuthority      = classInfo(jar, "trading/economics/Instrument$OwnerAuthority.class")
      assert((scalaAuthority.access & Opcodes.ACC_FINAL) != 0, "Scala owner authority must be final on the JVM")
      scalaAuthority.constructors.foreach: (_, descriptor) =>
        assert(descriptor.contains(authorityDescriptor), s"Scala authority constructor lacks JVM gate: $descriptor")

      val trustedTopLevelCarriers = List(
        "InstrumentLots",
        "InstrumentPosition",
        "InstrumentPrice",
        "InstrumentSettlementConversion",
        "InstrumentMarketState",
        "OrderActivation",
        "ImmediateActivation",
        "FixedActivation",
        "TrailingActivation",
        "OrderPricing",
        "LimitPricing",
        "PeggedPricing",
        "PricedVisibility",
        "DisplayedVisibility",
        "HiddenVisibility",
        "IcebergVisibility",
        "OrderExecution",
        "MarketExecution",
        "PricedExecution",
        "OrderIntent",
        "InstrumentOrder",
        "TriggerEvidence",
        "FixedTriggerEvidence",
        "TrailingTriggerEvidence",
        "ActivationAssumption",
        "ImmediateAssumption",
        "TriggeredAssumption",
        "PegResolution",
        "PricingAssumption",
        "DirectPricingAssumption",
        "ResolvedPegAssumption",
        "InstrumentLiquiditySlice",
        "ScenarioAssumptions",
        "InstrumentOrderScenario",
        "InstrumentRoundTripScenario",
        "InstrumentFeeDenomination",
        "InstrumentFee",
        "InstrumentFeeLine",
        "InstrumentConvertedFeeLine",
        "InstrumentPnl"
      ).map(name => s"trading/economics/$name.class")
      trustedTopLevelCarriers.foreach: entry =>
        val constructors = classInfo(jar, entry).constructors
        assert(constructors.nonEmpty, s"missing constructor in $entry")
        constructors.foreach: (_, descriptor) =>
          assert(descriptor.contains(authorityDescriptor), s"$entry constructor lacks JVM authority: $descriptor")

      val trustedNested = entries.filter: entry =>
        entry.startsWith("trading/economics/Instrument$") && entry.endsWith("Impl.class")
      assert(trustedNested.size >= 30, s"unexpected trusted implementation set: ${trustedNested.toList.sorted}")
      val scalaAuthorityDescriptor = "Ltrading/economics/Instrument$OwnerAuthority;"
      trustedNested.foreach: entry =>
        val constructors = classInfo(jar, entry).constructors
        assert(constructors.nonEmpty, s"missing constructor in $entry")
        constructors.foreach: (_, descriptor) =>
          assert(
            descriptor.contains(authorityDescriptor) || descriptor.contains(scalaAuthorityDescriptor),
            s"$entry constructor lacks JVM authority: $descriptor"
          )

      val instrumentConstructors = classInfo(jar, "trading/economics/Instrument.class").constructors
      assert(instrumentConstructors.nonEmpty, "missing Instrument constructor")
      instrumentConstructors.foreach: (_, descriptor) =>
        assert(descriptor.contains(authorityDescriptor), s"Instrument constructor lacks JVM authority: $descriptor")

      val capabilityImplementations = List(
        "InstrumentPricesImpl",
        "InstrumentMarketImpl",
        "InstrumentOrdersImpl",
        "InstrumentScenariosImpl",
        "InstrumentFeesImpl",
        "InstrumentValuationImpl",
        "InstrumentSizingImpl"
      )
      capabilityImplementations.foreach: name =>
        val entry        = s"trading/economics/$name.class"
        val constructors = classInfo(jar, entry).constructors
        assert(constructors.nonEmpty, s"missing constructor in $entry")
        constructors.foreach: (_, descriptor) =>
          assert(descriptor.contains(scalaAuthorityDescriptor), s"$entry constructor lacks JVM authority: $descriptor")
    finally jar.close()
    end try

  test("economics JVM artifact consistently targets Java 17"):
    val economicsJar = packagedEconomicsJar
    val jar          = new JarFile(economicsJar.toFile)
    try
      val versions = jar.entries().asScala
        .map(_.getName)
        .filter(entry => entry.startsWith("trading/economics/") && entry.endsWith(".class"))
        .map(entry => entry -> classInfo(jar, entry).version)
        .toList
      assert(versions.nonEmpty, s"missing economics classes from $economicsJar")
      val mismatches = versions.filter(_._2 != Opcodes.V17)
      assertEquals(
        mismatches,
        Nil,
        s"economics classes must target Java 17 (class-file major ${Opcodes.V17})"
      )
    finally jar.close()
    end try

  test("positive downstream economics fixture compiles without warnings and runs"):
    val result = compile(fixturesRoot.resolve("positive/CompleteEconomicsClient.scala"))
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.economics.positive.CompleteEconomicsClient$")

  test("positive ordinary Java inspection fixture compiles with strict warnings"):
    val result = compileJava(javaFixturesRoot.resolve("positive/JavaInspectionClient.java"))
    assert(result.succeeded, result.rendered)

  private val javaNegativeFixtures = List(
    JavaNegativeFixture(
      "ExternalAuthorityAccess.java",
      List("OwnerAuthority", "actual and formal argument lists differ in length")
    ),
    JavaNegativeFixture(
      "SamePackageAuthorityAccess.java",
      List("JvmOwnerAuthority() has private access")
    ),
    JavaNegativeFixture("TrustedCarrierImplementation.java", List("constructor InstrumentLots", "cannot be applied")),
    JavaNegativeFixture("TrustedAggregateImplementation.java", List("constructor InstrumentOrder", "cannot be applied"))
  )

  javaNegativeFixtures.foreach: fixture =>
    test(s"negative ordinary Java fixture rejects ${fixture.file}"):
      val source  = javaFixturesRoot.resolve("negative").resolve(fixture.file)
      val prelude = compileJavaPrelude(source)
      assert(prelude.succeeded, s"Java fixture prelude must compile independently:\n${prelude.rendered}")

      val rejected = compileJava(source)
      assert(!rejected.succeeded, s"Java fixture unexpectedly compiled:\n${rejected.rendered}")
      fixture.expected.foreach(fragment => assert(rejected.rendered.contains(fragment), rejected.rendered))
      economicsForbiddenDiagnostics.foreach(fragment =>
        assert(!rejected.rendered.contains(fragment), rejected.rendered)
      )

  private val negativeFixtures = List(
    NegativeFixture("CrossInstrumentMixing.scala", List("Required:", "first"), 6),
    NegativeFixture("PrivateConstruction.scala", List("Cannot extend sealed class", "Found:", "Required:"), 4),
    NegativeFixture("RemovedFlatApi.scala", List("is not a member"), 10, Some(10)),
    NegativeFixture(
      "PackageSpoofConstruction.scala",
      List("cannot be accessed as a member", "OwnerAuthorityImpl", "InstrumentImpl"),
      13,
      Some(13)
    ),
    NegativeFixture("RefinementLoss.scala", List("Found:", "Required:"), 5, Some(5)),
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

  private def compileJavaPrelude(source: Path): JavaCompilation =
    val lines    = Files.readAllLines(source, StandardCharsets.UTF_8)
    val filtered = new java.util.ArrayList[String]()
    var dropping = false
    lines.forEach: line =>
      if line.contains("OFFENDING-BEGIN") then dropping = true
      else if line.contains("OFFENDING-END") then dropping = false
      else if !dropping then
        val _ = filtered.add(line)

    val directory = Files.createTempDirectory("economics-java-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compileJava(copy)

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
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def compileJava(source: Path): JavaCompilation =
    val output  = Files.createTempDirectory("economics-java-classes-")
    val process = new ProcessBuilder(
      "javac",
      "-Xlint:all",
      "-Werror",
      "-classpath",
      compilationClasspath,
      "-d",
      output.toString,
      source.toString
    ).redirectErrorStream(true).start()
    val rendered = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
    JavaCompilation(process.waitFor(), rendered)

  private def packagedEconomicsJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-economics_3-"))
      .getOrElse(fail("missing packaged economics artifact"))

  private def classInfo(jar: JarFile, entryName: String): ClassInfo =
    val entry = Option(jar.getJarEntry(entryName)).getOrElse(fail(s"missing $entryName from ${jar.getName}"))
    val input = jar.getInputStream(entry)
    try
      var classVersion = 0
      var classAccess  = 0
      val constructors = ListBuffer.empty[(Int, String)]
      val visitor      = new ClassVisitor(Opcodes.ASM9):
        override def visit(
          version: Int,
          access: Int,
          name: String,
          signature: String,
          superName: String,
          interfaces: Array[String]
        ): Unit =
          classVersion = version
          classAccess = access

        override def visitMethod(
          access: Int,
          name: String,
          descriptor: String,
          signature: String,
          exceptions: Array[String]
        ): MethodVisitor =
          if name == "<init>" then constructors += access -> descriptor
          null

      new ClassReader(input).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
      ClassInfo(classVersion, classAccess, constructors.toList)
    finally input.close()
    end try
  end classInfo

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
