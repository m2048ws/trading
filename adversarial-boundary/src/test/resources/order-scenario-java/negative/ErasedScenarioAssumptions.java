package external.scenario.negative;

import scala.util.Either;
import trading.scenario.ScenarioAssumptions;
import trading.scenario.ScenarioViolation;

public final class ErasedScenarioAssumptions {
  public static Either<ScenarioViolation, ScenarioAssumptions> attempt(
      ScenarioAssumptions valid) {
    return ScenarioAssumptions.create(
        valid.order(), new Object(), new Object(), valid.matchedSlices());
  }
}
