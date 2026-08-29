# Native Orchestration Handoff Measurements

Measured on 2026-08-28 against the workflow-only
`adopt-native-subagent-orchestration` implementation at HEAD
`cf8a1a2035d1701c8992156750ded2367c61e6bc`.

The representative apply context named the active change, the relevant workflow
and charter invariants, and the change-specific guard obligations. The worker
report was a small schema-valid apply report. All generated artifacts lived
under ignored `.agent/reports/` paths.

These payload measurements exercise the retained broker protocol as an internal
diagnostic fixture. They do not establish native-writer eligibility. Fresh
review showed that the executable decision closure remains worker-writable and
the broker-owned reservation ends with broker-process death, so current policy
selects the script backend for apply, remediation, and finalization regardless
of a capability file. Bounded read-only exploration remains native-eligible.

| Payload | Bytes |
| --- | ---: |
| complete rendered prompt/context | 10,162 |
| native spawn assignment | 289 |
| broker preparation result | 937 |
| worker-readable manifest | 1,786 |
| non-authorizing lease evidence | 263 |
| assigned structured report | 508 |
| complete refreshed steward state | 68,686 |
| detailed collection result | 71,946 |
| compact collection stdout | 1,627 |
| collection diagnostics | 268 |

The prompt-file assignment was about 97.2% smaller than inlining the rendered
prompt in the native spawn exchange. Compact collection was about 97.6% smaller
than returning just the complete report plus complete refreshed state, while
stable paths, SHA-256 digests, and byte sizes kept those artifacts available for
targeted inspection. The broker preparation result contains only bounded launch
identity and handoff locations. The full launch tuple and random one-shot
capability remain in protected broker memory and are therefore absent from all
measured worker-readable and persisted payloads.

Observed local guard-plane elapsed time was approximately 0.31 seconds for
preparation and 4.87 seconds for schema/logical validation, full state refresh,
evidence persistence, one-shot consumption, writer release, and compact
collection. The refreshed OpenSpec portfolio dominates local collection work in
this repository. These single-machine observations are directional rather than
a latency guarantee.

These figures demonstrate possible primary-thread payload reduction, not lower
total model usage, monetary cost, or safe native-writer authority. Future native
and script workers would perform comparable model/tool work from the same
canonical prompt, and real elapsed worker time depends on the assignment. The
script regression suite uses deterministic stub workers and the dormant broker
fixture is useful for protocol diagnostics, not for comparing live model
latency. No total-token conclusion is supported by these measurements.
