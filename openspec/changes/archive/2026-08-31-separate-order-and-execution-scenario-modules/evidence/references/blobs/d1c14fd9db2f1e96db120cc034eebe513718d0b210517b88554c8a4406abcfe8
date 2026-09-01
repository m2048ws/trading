package external.scenario.boundary

import trading.scenario.*

object ExecutionScenarioHasNoUpstreamMutationOrDownstream:
  val role: LiquidityRole = LiquidityRole.Maker

  // OFFENDING-BEGIN
  object MissingFeePolicy:
    import trading.fee.policy.*

  object MissingRisk:
    import trading.risk.*

  object MissingApplication:
    import trading.application.*

  object MissingRuntime:
    import trading.runtime.*

  def mutateSlice[L, M](slice: LiquiditySlice[L, M], replacement: L): Unit =
    slice.lots = replacement
  // OFFENDING-END
