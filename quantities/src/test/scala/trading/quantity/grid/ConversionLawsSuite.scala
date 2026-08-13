package trading.quantity.grid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.*

class ConversionLawsSuite extends ScalaCheckSuite:
  private val usd =
    trading.quantity.testkit.TestAsset
      .runtime:
        AssetId:
          "USD-conversion-laws"
  private val cents =
    UniformGrid.create[usd.D](
      GridId:
        "USD-cent-conversion-laws"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val equivalentCents =
    UniformGrid.create[usd.D](
      GridId:
        "USD-cent-conversion-laws"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val alternateCents =
    UniformGrid.create[usd.D](
      GridId:
        "USD-alternate-cent-conversion-laws"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 100).toOption.get
    )
  private val mills =
    UniformGrid.create[usd.D](
      GridId:
        "USD-mill-conversion-laws"
      ,
      GridVersion(1),
      usd.dimension,
      PositiveRational.exact(1, 1000).toOption.get
    )

  property("grid interpretation and exact narrowing round-trip"):
    forAll(coordinate): coordinate =>
      val original =
        cents.fromCoordinate:
          coordinate
      val mathematical =
        cents.asQuantity:
          original
      val restored =
        mathematical
          .narrowExactlyTo:
            cents
          .toOption
          .get

      assertEquals(mathematical.coefficient, Rational(coordinate, 100))
      assertEquals(
        cents.coordinate:
          restored
        ,
        coordinate
      )

  property("same-grid evidence preserves coordinates and mathematical value"):
    val evidence = SameGrid.between(cents, equivalentCents).toOption.get
    forAll(coordinate): coordinate =>
      val original =
        cents.fromCoordinate:
          coordinate
      val converted =
        evidence.retype:
          original

      assertEquals(
        equivalentCents.coordinate:
          converted
        ,
        coordinate
      )
      assertEquals(
        equivalentCents
          .asQuantity:
            converted
          .coefficient,
        cents
          .asQuantity:
            original
          .coefficient
      )

  property("same-quantum conversion preserves value without claiming grid identity"):
    val evidence = SameQuantum.between(cents, alternateCents).toOption.get
    forAll(coordinate): coordinate =>
      val original =
        cents.fromCoordinate:
          coordinate
      val converted =
        evidence.convert:
          original

      assertEquals(
        alternateCents.coordinate:
          converted
        ,
        coordinate
      )
      assertEquals(
        alternateCents
          .asQuantity:
            converted
          .coefficient,
        cents
          .asQuantity:
            original
          .coefficient
      )

  property("grid embedding preserves exact mathematical value"):
    val embedding = Embedding.between(cents, mills).toOption.get
    forAll(coordinate): coordinate =>
      val original =
        cents.fromCoordinate:
          coordinate
      val widened =
        embedding.widenTo:
          original

      assertEquals(
        mills.coordinate:
          widened
        ,
        coordinate * 10
      )
      assertEquals(
        mills
          .asQuantity:
            widened
          .coefficient,
        cents
          .asQuantity:
            original
          .coefficient
      )

end ConversionLawsSuite
