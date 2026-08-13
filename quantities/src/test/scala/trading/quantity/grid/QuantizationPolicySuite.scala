package trading.quantity.grid

import munit.FunSuite

import trading.quantity.*

class QuantizationPolicySuite extends FunSuite:
  import QuantizationPolicy.*

  test("directed policies are total for positive, negative, and integral coordinates"):
    assertEquals(
      Floor.roundCoordinate:
        Rational(7, 3)
      ,
      BigInt(2)
    )
    assertEquals(
      Ceiling.roundCoordinate:
        Rational(7, 3)
      ,
      BigInt(3)
    )
    assertEquals(
      TowardZero.roundCoordinate:
        Rational(7, 3)
      ,
      BigInt(2)
    )
    assertEquals(
      AwayFromZero.roundCoordinate:
        Rational(7, 3)
      ,
      BigInt(3)
    )

    assertEquals(
      Floor.roundCoordinate:
        Rational(-7, 3)
      ,
      BigInt:
        -3
    )
    assertEquals(
      Ceiling.roundCoordinate:
        Rational(-7, 3)
      ,
      BigInt:
        -2
    )
    assertEquals(
      TowardZero.roundCoordinate:
        Rational(-7, 3)
      ,
      BigInt:
        -2
    )
    assertEquals(
      AwayFromZero.roundCoordinate:
        Rational(-7, 3)
      ,
      BigInt:
        -3
    )

    List(Floor, Ceiling, TowardZero, AwayFromZero).foreach: policy =>
      assertEquals(
        policy.roundCoordinate:
          Rational:
            -4
        ,
        BigInt:
          -4
      )

  test("nearest policies choose the closer coordinate away from ties"):
    val policies = List(HalfEven, HalfOdd, HalfUp, HalfDown, HalfTowardZero, HalfAwayFromZero)
    policies.foreach: policy =>
      assertEquals(
        policy.roundCoordinate:
          Rational(7, 5)
        ,
        BigInt(1)
      )
      assertEquals(
        policy.roundCoordinate:
          Rational(8, 5)
        ,
        BigInt(2)
      )
      assertEquals(
        policy.roundCoordinate:
          Rational(-7, 5)
        ,
        BigInt:
          -1
      )
      assertEquals(
        policy.roundCoordinate:
          Rational(-8, 5)
        ,
        BigInt:
          -2
      )

  test("nearest tie variants are deterministic for both signs"):
    assertEquals(
      HalfEven.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(2)
    )
    assertEquals(
      HalfEven.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -2
    )
    assertEquals(
      HalfOdd.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(1)
    )
    assertEquals(
      HalfOdd.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -1
    )
    assertEquals(
      HalfUp.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(2)
    )
    assertEquals(
      HalfUp.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -1
    )
    assertEquals(
      HalfDown.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(1)
    )
    assertEquals(
      HalfDown.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -2
    )
    assertEquals(
      HalfTowardZero.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(1)
    )
    assertEquals(
      HalfTowardZero.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -1
    )
    assertEquals(
      HalfAwayFromZero.roundCoordinate:
        Rational(3, 2)
      ,
      BigInt(2)
    )
    assertEquals(
      HalfAwayFromZero.roundCoordinate:
        Rational(-3, 2)
      ,
      BigInt:
        -2
    )

end QuantizationPolicySuite
