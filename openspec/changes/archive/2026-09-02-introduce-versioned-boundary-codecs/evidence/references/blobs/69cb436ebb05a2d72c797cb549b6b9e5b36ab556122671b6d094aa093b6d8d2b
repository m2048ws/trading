package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*
import trading.quantity.*
import trading.reference.*

class InstrumentDefinitionRecordPropertiesSuite extends ScalaCheckSuite:
  property("arbitrary exact coefficients and stable definition products survive V1 structural decoding"):
    forAll: (seed: Long, second: Long) =>
      val magnitude   = BigInt(seed).abs + 1
      val other       = BigInt(second)
      val suffix      = s"$magnitude-${other.abs}"
      val positionDim = DimKey.atom(AtomId(s"property:position:$suffix"))
      val priceDim    = DimKey(Vector(
        AtomId(s"property:base:$suffix")  -> BigInt(-1),
        AtomId(s"property:quote:$suffix") -> BigInt(1)
      ))
      val definition = InstrumentDefinition(
        InstrumentIdentity(
          InstrumentId.from(s"property-instrument-$suffix").toOption.get,
          UnderlyingId.from(s"property-underlying-$suffix").toOption.get
        ),
        AssetRoleIds(
          AssetId.from(s"property-base-$suffix").toOption.get,
          AssetId.from(s"property-quote-$suffix").toOption.get,
          AssetId.from(s"property-position-$suffix").toOption.get,
          AssetId.from(s"property-settle-$suffix").toOption.get
        ),
        ListingDefinition(
          GridIdentity(
            positionDim,
            GridKey(
              GridId.from(s"property-lots-$suffix").toOption.get,
              GridVersion.from((magnitude % BigInt(Long.MaxValue - 1) + 1).toLong).toOption.get
            )
          ),
          GridIdentity(
            priceDim,
            GridKey(
              GridId.from(s"property-price-$suffix").toOption.get,
              GridVersion.from((magnitude % BigInt(Long.MaxValue - 1) + 1).toLong).toOption.get
            )
          )
        ),
        PayoffDefinition(
          Rational(other, magnitude),
          Rational(-magnitude * magnitude, magnitude + 1)
        )
      )
      val encoded = InstrumentDefinitionRecord.encodeDefinition(definition).toOption
      encoded.flatMap(InstrumentDefinitionRecord.decode(_).toOption).contains(definition)
end InstrumentDefinitionRecordPropertiesSuite
