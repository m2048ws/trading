# Boundary-codec schemas

These seven human-readable JSON Schema Draft 2020-12 documents are generated from the same internal algebra as their V1
codecs. Each has a stable `urn:trading:codec:schema:*:v1` identifier, uses only local `#/$defs` references, closes every
record and tagged alternative, and deliberately omits operational decode limits.

`BoundaryCodecCompatibilitySuite` checks byte-for-byte regeneration, validates every document against the Draft
2020-12 meta-schema, and uses the test-only NetworkNT validator with remote fetching disabled. The schemas describe
wire structure; exact canonical spelling, catalog coherence, refinements, and dependent reconstruction remain codec and
domain responsibilities.
