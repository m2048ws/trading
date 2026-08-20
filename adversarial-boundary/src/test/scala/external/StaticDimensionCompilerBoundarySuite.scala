package external

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import dotty.tools.dotc.Main
import dotty.tools.dotc.reporting.StoreReporter
import munit.FunSuite

class StaticDimensionCompilerBoundarySuite extends FunSuite:

  private final case class Compilation(errors: List[String], warnings: List[String]):
    def succeeded: Boolean = errors.isEmpty && warnings.isEmpty
    def rendered: String   = (errors ++ warnings).mkString("\n")

  private final case class NegativeFixture(
    file: String,
    expected: List[String],
    minimumErrors: Int = 1,
    forbidden: List[String] = commonForbiddenDiagnostics)

  private val fixturesRoot =
    Paths.get(getClass.getResource("/static-dimension").toURI)

  private val compilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated static-dimension compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("standalone compiler classpath contains completed immutable products only"):
    val entries             = compilationClasspath.split(File.pathSeparator).toList.map(Paths.get(_))
    val quantitiesArtifacts = entries.filter: entry =>
      entry.getFileName.toString.startsWith("trading-quantities_3-")
    val compilerArtifacts = entries.filter: entry =>
      entry.getFileName.toString.startsWith("scala3-compiler_3-")

    assertEquals(quantitiesArtifacts.size, 1)
    assertEquals(compilerArtifacts.size, 1)
    assert(entries.forall(path => Files.isRegularFile(path)), entries.mkString("\n"))
    assert(quantitiesArtifacts.head.getFileName.toString.endsWith(".jar"))

  private val positiveFixtures = List(
    "AssociationIndependentOrder.scala",
    "CanonicalKeyMatrix.scala",
    "ClosedDimensionCaller.scala",
    "DirectCrossRate.scala",
    "ExplicitEquivalentArithmetic.scala",
    "GenericCapabilitySeparation.scala",
    "GenericDimensionPreserving.scala",
    "NormalizeGeneric.scala",
    "NormalizedUninhabitedZero.scala",
    "OpaqueRuntimeBigInt.scala",
    "SingletonDimensionKeys.scala",
    "StaticEquivalenceRetagging.scala"
  )

  positiveFixtures.foreach: fixture =>
    test(s"positive downstream static-dimension fixture compiles $fixture"):
      val result = compile(fixturesRoot.resolve("positive").resolve(fixture))
      assert(
        result.succeeded,
        s"expected positive fixture $fixture to compile without warnings or errors:\n${result.rendered}"
      )

  private val negativeFixtures = List(
    NegativeFixture(
      "ClosedMalformedDimension.scala",
      List("zero exponents", "keys must be unique", "must be a Power"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "UnresolvedSingletonKeys.scala",
      List("contextual Normalize evidence")
    ),
    NegativeFixture(
      "InvalidCanonicalKeys.scala",
      List("a Power key must be a concrete stable singleton identity"),
      minimumErrors = 6
    ),
    NegativeFixture(
      "WidenedLiteralAtom.scala",
      List("Cannot statically normalize the requested dimension", "WidenedLiteralAtom.K"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "WidenedNominalAtom.scala",
      List("WidenedNominalAtom.firstKey :", "WidenedNominalAtom.secondKey :", "WidenedNominalAtom.D"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "NormalizationDoesNotInhabit.scala",
      List("DimRef", "StaticOnlyKey"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "DimRefDoesNotNormalize.scala",
      List("Cannot statically normalize the requested dimension", "contextual Normalize evidence"),
      minimumErrors = 3
    ),
    NegativeFixture(
      "StaticEquivalenceNoRuntimeAuthority.scala",
      List("key", "DimRef"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "MalformedDimensionPreservingArithmetic.scala",
      List("Cannot statically normalize the requested dimension", "bad"),
      minimumErrors = 12
    ),
    NegativeFixture(
      "MalformedRefinementArithmetic.scala",
      List("Cannot statically normalize the requested dimension", "refined:bad"),
      minimumErrors = 7
    ),
    NegativeFixture(
      "MalformedGridBoundaryArithmetic.scala",
      List("Cannot statically normalize the requested dimension", "grid-boundary:bad"),
      minimumErrors = 6
    ),
    NegativeFixture(
      "MalformedDimensionAlgebra.scala",
      List("Normalize", "algebra:bad"),
      minimumErrors = 4
    ),
    NegativeFixture(
      "StaticExponentOverflow.scala",
      List("outside the singleton Int range"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "StaticEvidenceForgery.scala",
      List("sealed trait Normalize")
    ),
    NegativeFixture(
      "SameDimensionForgery.scala",
      List("sealed trait SameDimension")
    ),
    NegativeFixture(
      "DimRefForgery.scala",
      List("sealed trait DimRef")
    ),
    NegativeFixture(
      "RuntimeOpaquePackageSpoof.scala",
      List("runtimeOpaque")
    ),
    NegativeFixture(
      "ContradictoryNominalAuthority.scala",
      List("None of the overloaded alternatives of method atom", "match arguments"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "MalformedEndpointArithmetic.scala",
      List("Cannot statically normalize the requested dimension", "endpoint:bad"),
      minimumErrors = 4
    ),
    NegativeFixture(
      "UnresolvedTupleTail.scala",
      List("concrete Tuple")
    ),
    NegativeFixture(
      "AbstractExponent.scala",
      List("nonzero singleton Int literal"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "UnsupportedDimensionShapes.scala",
      List("outside the closed static grammar"),
      minimumErrors = 4
    ),
    NegativeFixture(
      "StaticExponentUnderflow.scala",
      List("static exponent -2147483649 is outside the singleton Int range")
    ),
    NegativeFixture(
      "RemovedLegacyStaticApi.scala",
      List(
        "Natural",
        "Powers",
        "NormalizedPowers",
        "DimensionProduct",
        "DimensionInverse",
        "DimensionQuotient",
        "DimensionAlignment"
      ),
      minimumErrors = 7
    ),
    NegativeFixture(
      "IncorrectStaticSameDimension.scala",
      List("The requested dimensions are not equivalent; provide checked SameDimension evidence")
    ),
    NegativeFixture(
      "ImplicitEquivalentArithmetic.scala",
      List("Found:", "Required:", "ImplicitEquivalentArithmetic.a", "ImplicitEquivalentArithmetic.b", "rightGrid"),
      minimumErrors = 6
    ),
    NegativeFixture(
      "UnequalGridComparison.scala",
      List("The requested dimensions are not equivalent; provide checked SameDimension evidence"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "RemovedAsDimension.scala",
      List("asDimension", "is not a member"),
      minimumErrors = 2
    ),
    NegativeFixture(
      "MissingAlignmentEvidence.scala",
      List("The requested dimensions are not equivalent; provide checked SameDimension evidence"),
      minimumErrors = 2
    )
  )

  negativeFixtures.foreach: fixture =>
    test(s"negative downstream fixture rejects ${fixture.file}"):
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
      fixture.forbidden.foreach: fragment =>
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

    val directory = Files.createTempDirectory("static-dimension-prelude-")
    val copy      = directory.resolve(source.getFileName)
    val _         = Files.write(copy, filtered, StandardCharsets.UTF_8)
    compile(copy)

  private def compile(source: Path): Compilation =
    val output    = Files.createTempDirectory("static-dimension-classes-")
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
    Compilation(
      reporter.allErrors.map(_.message),
      reporter.allWarnings.map(_.message)
    )

end StaticDimensionCompilerBoundarySuite

private val commonForbiddenDiagnostics = List(
  "Exception occurred while executing macro expansion",
  "CyclicReference",
  "See full stack trace",
  "at dotty.tools"
)
