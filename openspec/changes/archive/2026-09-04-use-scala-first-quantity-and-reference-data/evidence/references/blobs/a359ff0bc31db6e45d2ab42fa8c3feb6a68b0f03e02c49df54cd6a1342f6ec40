package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.quantity.refinement.PositiveRational
import trading.reference.*

class CatalogJournalPropertiesSuite extends ScalaCheckSuite:
  property("published exact definitions survive envelope decoding and fresh pure replay"):
    forAll: (seed: Long) =>
      val magnitude   = BigInt(seed).abs + 1
      val suffix      = magnitude.toString
      val dimension   = DimKey.atom(AtomId(s"journal:$suffix"))
      val gridId      = GridId.from(s"journal-grid-$suffix").toOption.get
      val gridVersion = GridVersion.from((magnitude % BigInt(Long.MaxValue - 1) + 1).toLong).toOption.get
      val identity    = GridIdentity(dimension, GridKey(gridId, gridVersion))
      val numerator   = magnitude * magnitude + 1
      val denominator = numerator + magnitude
      val quantum     = PositiveRational.exact(numerator, denominator).toOption.get
      val batch       = CatalogBatch.of(
        CatalogCommand.RegisterDimension(dimension),
        CatalogCommand.RegisterGrid(GridDefinition(identity, quantum))
      )
      val transition = CatalogModel.commit(CatalogRoot.create().initialState, batch).toOption.get
      val published  = transition.outcome.asInstanceOf[CatalogCommit.Published]
      val entry      = CatalogJournalEntry.fromPublished(batch, published)
      val decoded    = CatalogJournalEntry.encode(entry).toOption.flatMap(encoded =>
        CatalogJournalEntry.parse(encoded).toOption
      )
      val replayed = decoded.flatMap(value =>
        CatalogReplay.rebuild(CatalogRoot.create().initialState, Vector(value)).toOption
      )

      decoded.contains(entry) && replayed.exists(result =>
        result.revision == CatalogRevision.from(BigInt(1)).toOption.get &&
          result.snapshot.resolveGrid(identity).exists(_.quantum.unrefined == Rational(numerator, denominator))
      )
end CatalogJournalPropertiesSuite
