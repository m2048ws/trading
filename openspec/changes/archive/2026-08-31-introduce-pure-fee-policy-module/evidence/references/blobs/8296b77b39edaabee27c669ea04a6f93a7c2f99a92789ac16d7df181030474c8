package external.reference.negative

import trading.reference.AssetId

object FeePolicyUnavailable:
  val assetId = AssetId.from("reference-boundary")

  // OFFENDING-BEGIN
  val feePolicy: Class[trading.fee.policy.FeePolicy[?]] = classOf[trading.fee.policy.FeePolicy[?]]
  // OFFENDING-END

end FeePolicyUnavailable
