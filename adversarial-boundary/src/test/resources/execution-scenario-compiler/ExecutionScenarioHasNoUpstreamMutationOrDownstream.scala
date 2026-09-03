package external.scenario.boundary

import trading.economics.instrument.PositionLots
import trading.quantity.Dim
import trading.scenario.*

object ExecutionScenarioHasNoUpstreamMutationOrDownstream:
  val role: LiquidityRole = LiquidityRole.Maker

  // OFFENDING-BEGIN
  object MissingFeePolicy:
    import trading.fee.*

  object MissingRisk:
    import trading.risk.*

  object MissingApplication:
    import trading.application.*

  object MissingRuntime:
    import trading.runtime.*

  object MissingBoundaryCodecs:
    import trading.codec.*

  def mutateSlice[L, M](slice: LiquiditySlice[L, M], replacement: L): Unit =
    slice.lots = replacement
    val copied = slice.copy(lots = replacement)

  def forgeScenario[D <: Dim, B <: Dim, Q <: Dim, M](
    scenario: OrderScenario[D, B, Q, M],
    replacement: PositionLots[D]
  ): Unit =
    scenario.positionChange = replacement
    val copied = scenario.copy(positionChange = replacement)
  // OFFENDING-END
