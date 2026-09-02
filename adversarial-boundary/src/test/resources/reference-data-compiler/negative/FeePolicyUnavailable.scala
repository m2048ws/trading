package external.reference.negative

import trading.reference.AssetId

object FeePolicyUnavailable:
  val assetId = AssetId.from("reference-boundary")

  // OFFENDING-BEGIN
  val feePolicy: Class[trading.fee.FeePolicy[?, ?, ?, ?, ?]] = classOf[trading.fee.FeePolicy[?, ?, ?, ?, ?]]
  object MissingBoundaryCodecs:
    import trading.codec.*
  // OFFENDING-END

end FeePolicyUnavailable
