package external

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.jar.JarFile
import scala.jdk.CollectionConverters.*

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

  private val fixturesRoot          = Paths.get(getClass.getResource("/economics-compiler").toURI)
  private val orderFixturesRoot     = Paths.get(getClass.getResource("/order-model-compiler").toURI)
  private val scenarioFixturesRoot  = Paths.get(getClass.getResource("/execution-scenario-compiler").toURI)
  private val feePolicyFixturesRoot = Paths.get(getClass.getResource("/fee-policy-compiler").toURI)
  private val sharedFixture         = fixturesRoot.resolve("SharedEconomicsSetup.scala")
  private val compilationClasspath  =
    val resource = Option(getClass.getResourceAsStream("/static-dimension-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated external compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()
  private val coreCompilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/instrument-economics-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated instrument-economics compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()
  private val orderCompilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/order-model-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated order-model compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()
  private val scenarioCompilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/execution-scenario-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated execution-scenario compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()
  private val feePolicyCompilationClasspath =
    val resource = Option(getClass.getResourceAsStream("/fee-policy-compiler.classpath")).getOrElse:
      throw new IllegalStateException("missing generated fee-policy compiler classpath")
    try new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim
    finally resource.close()

  test("completed pure JAR compiles and runs a concrete core-only client with generic helpers"):
    val entries = coreCompilationClasspath.split(File.pathSeparator).map(Paths.get(_)).map(_.getFileName.toString)
    assert(entries.exists(_.startsWith("trading-instrument-economics_3-")))
    assert(!entries.exists(_.startsWith("trading-fee-policy_3-")))
    assert(!entries.exists(_.startsWith("trading-risk_3-")))
    assert(!entries.exists(_.startsWith("trading-application_3-")))
    val result = compileCore(Paths.get(getClass.getResource("/economics-core-compiler/PureCoreClient.scala").toURI))
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.economics.core.PureCoreClient$", "run")

  test("completed pure JAR preserves retained denomination definition and provenance in equality"):
    val result = compileCore(
      Paths.get(getClass.getResource("/economics-core-compiler/RetainedDenominationEqualityClient.scala").toURI)
    )
    assert(result.succeeded, result.rendered)
    runModule(result.output, "external.economics.core.RetainedDenominationEqualityClient$", "run")

  test("completed pure JAR cannot import downstream packages"):
    val source  = Paths.get(getClass.getResource("/economics-core-compiler/CoreHasNoDownstream.scala").toURI)
    val prelude = compileCorePrelude(source)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileCore(source)
    assert(rejected.errors.size >= 5, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    assert(rejected.rendered.contains("fee is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("packaged artifacts preserve quantities, reference-data, and pure instrument-economics boundaries"):
    val quantitiesJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-quantities_3-"))
      .getOrElse(fail("missing packaged quantities artifact"))
    val referenceDataJar = compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-reference-data_3-"))
      .getOrElse(fail("missing packaged reference-data artifact"))
    val instrumentJar     = packagedInstrumentEconomicsJar
    val quantitiesArchive = new JarFile(quantitiesJar.toFile)
    val referenceArchive  = new JarFile(referenceDataJar.toFile)
    val instrumentArchive = new JarFile(instrumentJar.toFile)
    try
      val quantityEntries   = quantitiesArchive.entries().asScala.map(_.getName).toSet
      val referenceEntries  = referenceArchive.entries().asScala.map(_.getName).toSet
      val instrumentEntries = instrumentArchive.entries().asScala.map(_.getName).toSet
      assert(!quantityEntries.exists(_.startsWith("trading/reference/")))
      assert(!quantityEntries.exists(_.startsWith("trading/economics/")))
      assert(!quantityEntries.exists(_.startsWith("trading/risk/")))
      List(
        "AssetId.class",
        "GridId.class",
        "GridVersion.class",
        "GridKey.class",
        "GridIdentity.class",
        "CatalogRoot.class",
        "CatalogState.class",
        "CatalogSnapshot.class",
        "CatalogBatch.class",
        "CatalogCommand.class",
        "CatalogModel.class",
        "Asset.class",
        "DimensionHandle.class",
        "GridHandle.class"
      ).foreach: name =>
        assert(!quantityEntries.contains(s"trading/quantity/$name"), s"quantity JAR retained $name")
        assert(referenceEntries.contains(s"trading/reference/$name"), s"reference-data JAR is missing $name")
      assert(!referenceEntries.exists(_.startsWith("trading/economics/")))
      assert(!referenceEntries.exists(_.startsWith("trading/risk/")))
      assert(instrumentEntries.exists(_ == "trading/economics/instrument/Instrument.class"))
      List("trading/order/", "trading/scenario/", "trading/fee/", "trading/risk/", "trading/application/")
        .foreach(prefix => assert(!instrumentEntries.exists(_.startsWith(prefix)), s"pure JAR retained $prefix"))
    finally
      quantitiesArchive.close()
      referenceArchive.close()
      instrumentArchive.close()
    end try

  test("pure order, scenario, fee-policy, and risk JARs expose only their one-way owned classes"):
    assert(
      !compilationClasspath
        .split(File.pathSeparator)
        .exists(entry => Paths.get(entry).getFileName.toString.startsWith("trading-economics_3-")),
      "retired trading-economics artifact remains on the completed-product classpath"
    )
    val instrumentJar = packagedInstrumentEconomicsJar
    val orderJar      = packagedOrderModelJar
    val scenarioJar   = packagedExecutionScenarioJar
    val feePolicyJar  = packagedFeePolicyJar
    val riskJar       = packagedRiskJar
    val core          = new JarFile(instrumentJar.toFile)
    val order         = new JarFile(orderJar.toFile)
    val scenario      = new JarFile(scenarioJar.toFile)
    val feePolicy     = new JarFile(feePolicyJar.toFile)
    val risk          = new JarFile(riskJar.toFile)
    try
      val coreEntries      = core.entries().asScala.map(_.getName).toSet
      val orderEntries     = order.entries().asScala.map(_.getName).toSet
      val scenarioEntries  = scenario.entries().asScala.map(_.getName).toSet
      val feePolicyEntries = feePolicy.entries().asScala.map(_.getName).toSet
      val riskEntries      = risk.entries().asScala.map(_.getName).toSet
      val expectedCore     = List(
        "InstrumentDefinition.class",
        "InstrumentAssembler.class",
        "InstrumentSpec.class",
        "Instrument.class",
        "Lots.class",
        "PositionLots.class",
        "Price.class",
        "SettlementConversion.class",
        "MarketState.class",
        "FeeDenomination.class",
        "Fee.class",
        "PricePnl.class",
        "SettledFeeContribution.class",
        "Pnl.class",
        "Valuation.class"
      ).map(name => s"trading/economics/instrument/$name")
      expectedCore.foreach(entry => assert(coreEntries.contains(entry), s"missing $entry from $instrumentJar"))
      List(
        "trading/order/Order.class",
        "trading/order/OrderIntent.class",
        "trading/order/OrderViolations.class",
        "trading/order/Side.class"
      ).foreach(entry => assert(orderEntries.contains(entry), s"missing $entry from $orderJar"))
      assert(!orderEntries.contains("trading/order/Orders.class"), s"order JAR retained removed Orders service")
      List(
        "trading/scenario/LiquidityRole.class",
        "trading/scenario/LiquiditySlice.class",
        "trading/scenario/MatchedSlices.class",
        "trading/scenario/ScenarioAssumptions.class",
        "trading/scenario/OrderScenario.class",
        "trading/scenario/ScenarioSliceComponent.class",
        "trading/scenario/ScenarioLocation.class",
        "trading/scenario/ScenarioViolation.class",
        "trading/scenario/ScenarioViolations.class",
        "trading/scenario/RoundTripComponent.class",
        "trading/scenario/RoundTripLeg.class",
        "trading/scenario/RoundTripViolation.class",
        "trading/scenario/RoundTripScenario.class",
        "trading/scenario/ScenarioValuationError.class",
        "trading/scenario/ScenarioValuation$.class"
      ).foreach(entry => assert(scenarioEntries.contains(entry), s"missing $entry from $scenarioJar"))
      assert(!scenarioEntries.contains("trading/scenario/Scenarios.class"), s"scenario JAR retained Scenarios service")
      assert(!scenarioEntries.contains("trading/scenario/ScenarioLeg.class"), s"scenario JAR retained old ScenarioLeg")
      List(
        "trading/scenario/ScenarioError.class",
        "trading/scenario/ScenarioFailureReason.class",
        "trading/scenario/InvalidScenario.class",
        "trading/scenario/InvalidScenarioDiagnostics.class"
      ).foreach(entry => assert(!scenarioEntries.contains(entry), s"scenario JAR retained obsolete $entry"))
      val assumptionsEntry = scenario.getJarEntry("trading/scenario/ScenarioAssumptions.class")
      val assumptionsBytes = scenario.getInputStream(assumptionsEntry).readAllBytes()
      assert(
        !new String(assumptionsBytes, StandardCharsets.ISO_8859_1).contains("cats/data/NonEmptyVector"),
        s"scenario assumptions leaked Cats NonEmptyVector in $scenarioJar"
      )
      val valuationEntries = scenarioEntries.toList.sorted.filter: entry =>
        entry.startsWith("trading/scenario/ScenarioValuation") && entry.endsWith(".class")
      val valuationBytes = valuationEntries.map: entry =>
        val stream = scenario.getInputStream(scenario.getJarEntry(entry))
        try new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1)
        finally stream.close()
      .mkString
        .toLowerCase(Locale.ROOT)
      List(
        "trading/fee/",
        "quantiz",
        "trading/reference/catalog",
        "cats/effect/",
        "fs2/",
        "average",
        "coefficient"
      ).foreach(fragment => assert(!valuationBytes.contains(fragment), s"scenario valuation leaked $fragment"))
      List(
        "trading/fee/FeeCalculation$package.tasty",
        "trading/fee/FeeCalculation$.class",
        "trading/fee/PolicyErrors.class",
        "trading/fee/SliceIndex.class",
        "trading/fee/FeeDirective.class",
        "trading/fee/FeePolicy.class",
        "trading/fee/FeeAssessmentErrors.class",
        "trading/fee/AssessedFee.class",
        "trading/fee/ScenarioFees.class",
        "trading/fee/FeeAssessment$.class",
        "trading/fee/RoundTripFeePolicies.class",
        "trading/fee/FeeInclusivePnlViolation.class",
        "trading/fee/FeeInclusivePnlErrors.class",
        "trading/fee/AttributedFeeContribution.class",
        "trading/fee/FeeInclusivePnl.class",
        "trading/fee/FeeInclusivePnl$.class"
      ).foreach(entry => assert(feePolicyEntries.contains(entry), s"missing $entry from $feePolicyJar"))
      List(
        "trading/fee/FeeSchedule.class",
        "trading/fee/FeeLine.class",
        "trading/fee/FeePolicyError.class",
        "trading/fee/FeeOrchestration.class"
      )
        .foreach(entry => assert(!feePolicyEntries.contains(entry), s"fee-policy JAR retained removed $entry"))
      assert(
        !feePolicyEntries.exists(_.startsWith("trading/fee/policy/")),
        s"fee-policy JAR retained provisional policy subpackage"
      )
      List(
        "trading/fee/FeePolicyAcquisition",
        "trading/fee/FeePolicyProvider",
        "trading/fee/FeePolicySelector",
        "trading/fee/AccountFeePolicy",
        "trading/fee/FeeTierSelection",
        "trading/fee/FeePolicyVersion",
        "trading/fee/FeeAuditEnvelope",
        "trading/fee/FeeExecutionReport"
      ).foreach(prefix =>
        assert(!feePolicyEntries.exists(_.startsWith(prefix)), s"fee-policy JAR retained deferred $prefix")
      )
      val publicPolicyEntries = feePolicyEntries.toList.sorted.filter: entry =>
        List(
          "trading/fee/FeePolicy",
          "trading/fee/FeeDirective",
          "trading/fee/AssessedFee",
          "trading/fee/ScenarioFees",
          "trading/fee/FeeAssessment",
          "trading/fee/RoundTripFeePolicies",
          "trading/fee/FeeInclusive",
          "trading/fee/AttributedFeeContribution"
        ).exists(entry.startsWith) && entry.endsWith(".class")
      val publicPolicyBytes = publicPolicyEntries
        .map: entry =>
          val stream = feePolicy.getInputStream(feePolicy.getJarEntry(entry))
          try new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1)
          finally stream.close()
        .mkString
      List("cats/data/", "cats/kernel/Monoid", "cats/effect/", "fs2/", "java/time/Clock")
        .foreach(fragment => assert(!publicPolicyBytes.contains(fragment), s"public fee policy leaked $fragment"))
      List(
        "trading/risk/Risk.class",
        "trading/risk/MonotoneLotRisk.class",
        "trading/risk/MaxAffordableLots.class",
        "trading/risk/ExhaustiveLotSizing.class"
      ).foreach(entry => assert(riskEntries.contains(entry), s"missing $entry from $riskJar"))

      List(
        "trading/economics/instrument/MarketState.class",
        "trading/scenario/",
        "trading/fee/",
        "trading/risk/",
        "trading/application/",
        "trading/runtime/"
      ).foreach(entry => assert(!orderEntries.exists(_.startsWith(entry)), s"order JAR retained $entry"))
      List(
        "trading/economics/instrument/",
        "trading/order/",
        "trading/fee/",
        "trading/risk/",
        "trading/application/",
        "trading/runtime/"
      ).foreach(entry => assert(!scenarioEntries.exists(_.startsWith(entry)), s"scenario JAR retained $entry"))
      List("trading/economics/instrument/", "trading/order/", "trading/scenario/", "trading/risk/")
        .foreach(entry => assert(!feePolicyEntries.exists(_.startsWith(entry)), s"fee-policy JAR retained $entry"))
      List("trading/order/", "trading/scenario/", "trading/fee/")
        .foreach(entry => assert(!riskEntries.exists(_.startsWith(entry)), s"risk JAR retained $entry"))

      val staleCore = List(
        "trading/economics/instrument/Prices.class",
        "trading/economics/instrument/Market.class",
        "trading/economics/instrument/Fees.class",
        "trading/economics/instrument/Sizing.class",
        "trading/economics/instrument/EconomicsError.class",
        "trading/economics/instrument/ForeignRegistry.class"
      )
      staleCore.foreach(entry => assert(!coreEntries.contains(entry), s"stale $entry remains in $instrumentJar"))
    finally
      core.close()
      order.close()
      scenario.close()
      feePolicy.close()
      risk.close()
    end try

  test("completed order-model classpath cannot compile downstream concerns"):
    val source  = Paths.get(getClass.getResource("/order-model-compiler/OrderModelHasNoDownstream.scala").toURI)
    val prelude = compileFilteredPrelude(source, compileOrder)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileOrder(source)
    assert(rejected.errors.size >= 7, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    assert(rejected.rendered.contains("fee is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed fee-policy classpath contains exactly its pure one-way production graph"):
    val entries = feePolicyCompilationClasspath.split(File.pathSeparator).toList.map(Paths.get(_))
    List(
      "trading-quantities_3-",
      "trading-reference-data_3-",
      "trading-instrument-economics_3-",
      "trading-order-model_3-",
      "trading-execution-scenario_3-",
      "trading-fee-policy_3-"
    ).foreach: prefix =>
      assertEquals(entries.count(_.getFileName.toString.startsWith(prefix)), 1, entries.mkString("\n"))

    List(
      "trading-risk_3-",
      "trading-application_3-",
      "trading-runtime_3-",
      "trading-economics_3-",
      "cats-effect_3-",
      "fs2-core_3-",
      "circe-core_3-",
      "doobie-core_3-",
      "opentelemetry-api-",
      "jmh-core-"
    ).foreach: prefix =>
      assert(!entries.exists(_.getFileName.toString.startsWith(prefix)), s"fee-policy classpath retained $prefix")
    assert(entries.forall(Files.isRegularFile(_)), entries.mkString("\n"))

  test("completed fee-policy classpath compiles and runs a policy-only client"):
    val source = feePolicyFixturesRoot.resolve("positive/FeePolicyBoundaryClient.scala")
    val result = compileFeePolicy(source)
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.fee.positive.FeePolicyBoundaryClient$")

  test("completed fee-policy formulas require a refined basis and nominal rate"):
    val source  = feePolicyFixturesRoot.resolve("negative/RawFeeBasis.scala")
    val prelude = compileFilteredPrelude(source, compileFeePolicy)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileFeePolicy(source)
    assert(rejected.errors.size >= 2, rejected.rendered)
    assert(rejected.rendered.contains("Found:"), rejected.rendered)
    assert(rejected.rendered.contains("Required:"), rejected.rendered)
    assert(rejected.rendered.contains("NonNegative"), rejected.rendered)
    assert(rejected.rendered.contains("FeeRate"), rejected.rendered)

  test(
    "completed fee-policy API rejects retired capabilities, unlawful algebra, effects, error erasure, and source markets"
  ):
    val source  = feePolicyFixturesRoot.resolve("negative/UnlawfulPolicyApi.scala")
    val prelude = compileFilteredPrelude(source, compileFeePolicy)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileFeePolicy(source)
    assert(rejected.errors.size >= 9, rejected.rendered)
    List(
      "FeeSchedule",
      "FeeLine",
      "FeeOrchestration",
      "FeePolicyError",
      "Monoid",
      "Type argument",
      "Throwable",
      "String",
      "sourceMarket"
    )
      .foreach(fragment => assert(rejected.rendered.contains(fragment), rejected.rendered))

  test("completed fee-policy classpath cannot access downstream or effect concerns"):
    val source  = feePolicyFixturesRoot.resolve("negative/FeePolicyHasNoDownstream.scala")
    val prelude = compileFilteredPrelude(source, compileFeePolicy)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileFeePolicy(source)
    assert(rejected.errors.size >= 10, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed order-model classpath compiles every supported instruction shape exhaustively"):
    val source = orderFixturesRoot.resolve("positive/InstructionAlgebra.scala")
    val result = compileOrder(source)
    assert(result.succeeded, result.rendered)

  test("completed order-model classpath rejects impossible instruction and evidence shapes"):
    val source  = orderFixturesRoot.resolve("negative/ImpossibleInstructionShapes.scala")
    val prelude = compileFilteredPrelude(source, compileOrder)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileOrder(source)
    assert(rejected.errors.size >= 7, rejected.rendered)
    assert(rejected.rendered.contains("Found:"), rejected.rendered)
    assert(rejected.rendered.contains("Required:"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed execution-scenario classpath cannot compile downstream concerns or mutation"):
    val source = Paths.get(
      getClass
        .getResource("/execution-scenario-compiler/ExecutionScenarioHasNoUpstreamMutationOrDownstream.scala")
        .toURI
    )
    val prelude = compileFilteredPrelude(source, compileScenario)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileScenario(source)
    assert(rejected.errors.size >= 9, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    assert(rejected.rendered.contains("fee is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.contains("codec is not a member of trading"), rejected.rendered)
    assert(rejected.rendered.toLowerCase.contains("reassignment to val"), rejected.rendered)
    economicsForbiddenDiagnostics.foreach(fragment => assert(!rejected.rendered.contains(fragment), rejected.rendered))

  test("completed execution-scenario classpath calculates exact price PnL without fee policy"):
    val source = scenarioFixturesRoot.resolve("positive/ScenarioValuationClient.scala")
    val result = compileScenario(source)
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.scenario.positive.ScenarioValuationClient$")

  test("completed scenario API hides raw side signs and removes the old leg name"):
    val source  = scenarioFixturesRoot.resolve("negative/RemovedScenarioValuationApi.scala")
    val prelude = compileFilteredPrelude(source, compileScenario)
    assert(prelude.succeeded, s"fixture prelude must compile independently:\n${prelude.rendered}")
    val rejected = compileScenario(source)
    assert(rejected.errors.size >= 2, rejected.rendered)
    assert(rejected.rendered.contains("is not a member"), rejected.rendered)
    assert(rejected.rendered.contains("ScenarioLeg"), rejected.rendered)

  test("positive downstream composition fixture compiles without warnings and runs"):
    val result = compile(fixturesRoot.resolve("positive/CompleteCompositionClient.scala"))
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.economics.positive.CompleteCompositionClient$")

  test("same-shape replay fixture compiles against immutable JARs and enforces captured semantics"):
    val result = compile(fixturesRoot.resolve("positive/SameShapeReplayClient.scala"))
    assert(result.succeeded, result.rendered)
    initializeModule(result.output, "external.economics.positive.SameShapeReplayClient$")

  private val negativeFixtures = List(
    NegativeFixture("RawDefinitionShape.scala", List("Found:", "Required:"), 4),
    NegativeFixture("RawInstrumentConstruction.scala", List("Found:", "Required:", "is not a member"), 3),
    NegativeFixture("ReversedPayoffEndpoint.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("SpecAuthorityExtraction.scala", List("is not a member"), 4, Some(4)),
    NegativeFixture("ConversionDoesNotGrantGrid.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("AssociatedEvidenceShapes.scala", List("Found:", "Required:"), 8, Some(8)),
    NegativeFixture("RemovedCapabilityPaths.scala", List("is not a member"), 7, Some(7)),
    NegativeFixture("DeferredLifecycle.scala", List("is not a member"), 9, Some(9)),
    NegativeFixture("RemovedFlatApi.scala", List("is not a member", "Not found", "Found:", "Required:"), 18, Some(18)),
    NegativeFixture("RemovedOwnerApi.scala", List("is not a member", "Not found"), 4, Some(4)),
    NegativeFixture("ReversedPriceRate.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("ReversedSettlementRate.scala", List("Found:", "Required:"), 1, Some(1)),
    NegativeFixture("RefinementLoss.scala", List("Found:", "Required:"), 4, Some(4)),
    NegativeFixture("CoreSideAbsent.scala", List("is not a member"), 2, Some(2)),
    NegativeFixture("UnlawfulAlgebra.scala", List("No given instance"), 2, Some(2))
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
    compileFilteredPrelude(source, compile)

  private def compileCorePrelude(source: Path): Compilation =
    compileFilteredPrelude(source, compileCore)

  private def compileFilteredPrelude(source: Path, compileFile: Path => Compilation): Compilation =
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
    compileFile(copy)

  private def compile(source: Path): Compilation =
    compileWith(source, compilationClasspath, Some(sharedFixture))

  private def compileCore(source: Path): Compilation =
    compileWith(source, coreCompilationClasspath, None)

  private def compileOrder(source: Path): Compilation =
    compileWith(source, orderCompilationClasspath, None)

  private def compileScenario(source: Path): Compilation =
    compileWith(source, scenarioCompilationClasspath, None)

  private def compileFeePolicy(source: Path): Compilation =
    compileWith(source, feePolicyCompilationClasspath, Some(sharedFixture))

  private def compileWith(source: Path, classpath: String, shared: Option[Path]): Compilation =
    val output        = Files.createTempDirectory("economics-classes-")
    val reporter      = new StoreReporter()
    val baseArguments = Array(
      "-classpath",
      classpath,
      "-d",
      output.toString,
      "-Werror",
      "-source:future"
    )
    val arguments = baseArguments ++ shared.toVector.map(_.toString) :+ source.toString
    val _         = Main.process(arguments, reporter)
    Compilation(output, reporter.allErrors.map(_.message), reporter.allWarnings.map(_.message))

  private def packagedFeePolicyJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-fee-policy_3-"))
      .getOrElse(fail("missing packaged fee-policy artifact"))

  private def packagedRiskJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-risk_3-"))
      .getOrElse(fail("missing packaged risk artifact"))

  private def packagedInstrumentEconomicsJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-instrument-economics_3-"))
      .getOrElse(fail("missing packaged instrument-economics artifact"))

  private def packagedOrderModelJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-order-model_3-"))
      .getOrElse(fail("missing packaged order-model artifact"))

  private def packagedExecutionScenarioJar: Path =
    compilationClasspath
      .split(File.pathSeparator)
      .map(Paths.get(_))
      .find(_.getFileName.toString.startsWith("trading-execution-scenario_3-"))
      .getOrElse(fail("missing packaged execution-scenario artifact"))

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

  private def runModule(output: Path, moduleClassName: String, methodName: String): Unit =
    val loader = new URLClassLoader(Array(output.toUri.toURL), getClass.getClassLoader)
    try
      val moduleClass = Class.forName(moduleClassName, true, loader)
      val module      = moduleClass.getField("MODULE$").get(null)
      val _           = moduleClass.getMethod(methodName).invoke(module)
    catch
      case error: java.lang.reflect.InvocationTargetException =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled economics client $moduleClassName.$methodName failed: $cause")
      case error: ExceptionInInitializerError =>
        val cause = Option(error.getCause).fold(error.toString)(_.toString)
        fail(s"compiled economics client $moduleClassName failed during initialization: $cause")
      case error: ReflectiveOperationException =>
        fail(s"compiled economics client $moduleClassName could not be invoked: $error")
      case error: LinkageError =>
        fail(s"compiled economics client $moduleClassName could not be linked: $error")
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
