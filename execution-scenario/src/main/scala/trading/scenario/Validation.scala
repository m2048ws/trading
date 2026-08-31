package trading.scenario

private[scenario] final case class RankedViolation[+E](
  stage: Int,
  rule: Int,
  index: Int,
  value: E)

/** Stable semantic ordering for branch-sensitive scenario validation. */
private[scenario] object Validation:
  def violation[E](stage: Int, rule: Int, index: Int, value: E): RankedViolation[E] =
    RankedViolation(stage, rule, index, value)

  def ordered[E](violations: Vector[RankedViolation[E]]): Vector[E] =
    violations.sortBy(value => (value.stage, value.rule, value.index)).map(_.value)
end Validation
