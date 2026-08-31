# trading-risk

Pure position-risk mathematics over typed quantities and instrument economics.

The production artifact depends only on `trading-quantities`, `trading-instrument-economics`, and pure Cats Core
utilities. It does not own order construction, execution scenarios, fee policies, catalogs, application/runtime
effects, persistence, telemetry, or benchmarks.

The current boundary owns exact refined downside measurement plus unforgeable lot-risk assessments and the validated
monotone-model capability. Subsequent S-03 Task Groups add the constructive loss-curve algebra, boundary-certified
maximum sizing, and the explicitly linear arbitrary fallback before the transitional `trading-economics` artifact is
retired.
