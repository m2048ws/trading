package trading.quantity.runtime

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*

class JavaSerializationPolicySuite extends FunSuite:

  private def assertExplicitlyRejected(name: String, value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val failure = intercept[NotSerializableException](output.writeObject(value))
      assertEquals(
        failure.getMessage,
        s"Java object serialization is unsupported for ${value.getClass.getName}",
        clues(name)
      )
    finally output.close()

  test("quantity-owned invariant carriers fail Java serialization closed"):
    val atom      = AtomId("serialization-atom")
    val dimension = DimRef.atomic(atom)
    val quantum   = PositiveRational.exact(1, 100).toOption.get
    val grid      = UniformGrid.create(dimension.dimension, quantum)
    val value     = Quantity(dimension.dimension, Rational(1, 3))
    val offGrid   = value.narrowExactlyTo(grid).swap.toOption.get
    val quantized = value.quantizeTo(grid, QuantizationPolicy.HalfEven)
    val gridError = SameGrid
      .between(grid, UniformGrid.create(dimension.dimension, quantum))
      .swap
      .toOption
      .get

    val inventory: List[(String, JavaSerializationUnsupported)] =
      List(
        "AtomId"             -> atom,
        "NotOnGrid"          -> offGrid,
        "GridError"          -> gridError,
        "Quantization"       -> quantized,
        "QuantizationPolicy" -> QuantizationPolicy.HalfEven
      )

    inventory.foreach((name, carrier) => assertExplicitlyRejected(name, carrier))

end JavaSerializationPolicySuite
