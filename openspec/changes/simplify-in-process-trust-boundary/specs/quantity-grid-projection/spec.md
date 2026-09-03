## ADDED Requirements

### Requirement: Grid provenance is semantic rather than constructor-secret

Documented grid construction and projection SHALL preserve a matching dimension witness, positive quantum, generative
coordinate namespace, refinement evidence, and exact residuals. A stronger stable grid identity SHALL still require a
reference-data handle and checked reconciliation. These guarantees SHALL prevent ordinary type and input mistakes but
SHALL NOT be advertised as resistance to deliberate same-JVM visibility or bytecode bypass.

#### Scenario: Construct and project on a supported grid

- **WHEN** a caller uses a matching mathematical grid witness to construct, quantize, narrow, or embed a value
- **THEN** dimension, grid namespace, coordinate, exact value, and residual semantics remain coherent

#### Scenario: Reconstruct a packed coordinate

- **WHEN** an external packed record supplies stable dimension/grid identity and a coordinate
- **THEN** checked reference-data resolution establishes the handle and coordinate relationship before returning the
  dependent value

#### Scenario: Possess only a grid value

- **WHEN** in-process code possesses a `GridQuantity` without its witness or stable handle
- **THEN** the value supplies no mathematical witness, stable identity, lineage, or encoding authority

