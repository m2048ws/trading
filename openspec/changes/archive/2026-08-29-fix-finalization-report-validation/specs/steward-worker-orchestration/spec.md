## MODIFIED Requirements

### Requirement: Worker reports are mechanically validated
A worker result MUST satisfy the role's structured report schema, assigned-change binding, and workflow-consistency
rules before the steward uses it to transition workflow state. Finalization workflow-consistency rules MUST distinguish
the staged pre-commit state from the clean post-commit state and MUST bind a reported created commit to the reported
repository HEAD. A human-readable native summary alone SHALL NOT authorize a transition.

#### Scenario: Valid native worker result
- **WHEN** a native worker's assigned result passes schema, assigned-change, and workflow-consistency validation
- **THEN** the steward may use the validated report as an input to workflow classification

#### Scenario: Invalid native worker result
- **WHEN** a native result is absent, malformed, schema-invalid, or contradictory to its role transition rules
- **THEN** the steward does not transition and instead requests correction or classifies the failed launch

#### Scenario: Launch context names another change
- **WHEN** a formal native or script worker context names a change different from the change assigned by its launch
- **THEN** preparation rejects the context before launching the worker or acquiring transition authority

#### Scenario: Structured report names another change
- **WHEN** a schema-valid worker report explicitly names an OpenSpec change different from the assigned change
- **THEN** collection rejects transition authority
- **AND** a native writer retains its lease until report correction or refreshed-state classification

#### Scenario: Worker claims successful repository state
- **WHEN** any worker reports successful validation, clean Git state, or completed OpenSpec work
- **THEN** the steward refreshes the load-bearing repository and OpenSpec state before the next transition

#### Scenario: Finalization is commit-ready without a commit
- **WHEN** a finalization report has status `commit_ready`
- **THEN** it is valid only when intended changes remain staged, unstaged and untracked changes are absent, and Git diff checks passed
- **AND** commit authorization and creation are false and the commit hash is empty

#### Scenario: Finalization completed an authorized commit
- **WHEN** a finalization report has status `committed`
- **THEN** it is valid only when staged, unstaged, and untracked changes are absent and Git diff checks passed
- **AND** commit authorization and creation are true, the commit hash is non-empty, and it equals the reported Git HEAD

#### Scenario: Finalization outcome fields contradict the status
- **WHEN** a finalization report combines `commit_ready` with a clean unstaged index or created commit, or combines `committed` with staged changes or a commit hash different from Git HEAD
- **THEN** workflow-consistency validation rejects the report
