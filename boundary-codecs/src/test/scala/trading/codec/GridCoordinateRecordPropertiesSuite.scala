package trading.codec

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.quantity.refinement.PositiveRational
import trading.reference.*

class GridCoordinateRecordPropertiesSuite extends ScalaCheckSuite:
  private val dimension = DimKey(
    Vector(
      AtomId("property:numerator")   -> BigInt(3),
      AtomId("property:denominator") -> BigInt(-2)
    )
  )
  private val key      = GridKey(GridId.from("property-grid").toOption.get, GridVersion.from(17).toOption.get)
  private val identity = GridIdentity(dimension, key)
  private val quantum  = PositiveRational.exact(7, 19).toOption.get
  private val snapshot = CatalogModel
    .commit(
      CatalogRoot.create().initialState,
      CatalogBatch.of(
        CatalogCommand.RegisterDimension(dimension),
        CatalogCommand.RegisterGrid(GridDefinition(identity, quantum))
      )
    )
    .toOption
    .get
    .state
    .snapshot
  private val grid = snapshot.resolveGrid(identity).toOption.get

  property("every signed exact coordinate survives pack, envelope round trip, and dependent reconstruction"):
    forAll: (coordinate: BigInt) =>
      val record = GeneralGridCoordinateRecord.pack(grid)(grid.fromCoordinate(coordinate))
      val parsed = GeneralGridCoordinateRecord.encode(record).toOption.flatMap(encoded =>
        GeneralGridCoordinateRecord.parse(encoded).toOption
      )
      val decoded = parsed.flatMap(value => GeneralGridCoordinateRecord.reconstruct(value, snapshot).toOption)
      parsed.contains(record) &&
      decoded.exists(value =>
        value.grid.identity == identity &&
          value.grid.coordinate(value.value) == coordinate &&
          value.grid.asQuantity(value.value).coefficient == Rational(7 * coordinate, 19)
      )
end GridCoordinateRecordPropertiesSuite
