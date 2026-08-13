package trading.quantity.laws

/** The single project test base that integrates Discipline RuleSets with MUnit. */
trait TradingDisciplineSuite extends munit.DisciplineSuite:
  override protected def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(100)
      .withWorkers(1)
