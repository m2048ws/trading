package external.reference.negative

import trading.reference.AssetId

object FeePolicyUnavailable:
  val assetId = AssetId.from("reference-boundary")

  // OFFENDING-BEGIN
  val feePolicy: Class[trading.fee.FeePolicy[?, ?, ?, ?, ?]] = classOf[trading.fee.FeePolicy[?, ?, ?, ?, ?]]
  // OFFENDING-END

end FeePolicyUnavailable
