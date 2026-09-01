# Boundary-codec golden vectors

These canonical JSON and JSON Lines fixtures cover all seven V1 families and every closed tagged alternative. The
compatibility suite regenerates them byte-for-byte, round-trips them through the owning codecs, and compares every line
with the independent test-only JCS oracle. The catalog history preserves publication order; scenario resources remain
hypothetical and contain no execution, fee, PnL, lifecycle, or storage facts.

The adjacent `invalid/` directory contains JSON-valid unknown-version fixtures. Test oracles and fixtures verify the
production contract but do not define it.
