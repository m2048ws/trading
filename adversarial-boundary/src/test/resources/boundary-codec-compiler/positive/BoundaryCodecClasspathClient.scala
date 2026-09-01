package external.codec.positive

import cats.data.Validated
import tools.jackson.core.json.JsonFactory

import trading.economics.instrument.Instrument
import trading.order.Side
import trading.quantity.Rational
import trading.reference.AssetId
import trading.scenario.LiquidityRole

object BoundaryCodecClasspathClient:
  val exactType: Class[Rational]          = classOf[Rational]
  val referenceType: Class[AssetId]       = classOf[AssetId]
  val instrumentType: Class[Instrument]   = classOf[Instrument]
  val side: Side                          = Side.Buy
  val role: LiquidityRole                 = LiquidityRole.Maker
  val parserMechanism: Class[JsonFactory] = classOf[JsonFactory]
  val pureValidation: Validated[String, Side] = Validated.valid(side)
