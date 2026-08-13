package trading.quantity.refinement

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import munit.FunSuite

class RefinementClosureBytecodeSuite extends FunSuite:

  private val checkedMarkers = List(
    "Sign.signum",
    "fromChecked:",
    "Refinement$package$NonNegative$.apply",
    "Refinement$package$NonZero$.apply",
    "Refinement$package$Positive$.apply"
  )

  private def disassemble(className: String): String =
    val javap     = Paths.get(System.getProperty("java.home"), "bin", "javap").toString
    val classPath =
      Paths
        .get(classOf[Sign[?]].getProtectionDomain.getCodeSource.getLocation.toURI)
        .toString
    val process =
      new ProcessBuilder(javap, "-classpath", classPath, "-c", "-p", className)
        .redirectErrorStream(true)
        .start()
    val output = new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
    val exit   = process.waitFor()

    assertEquals(exit, 0, output)
    output

  private def methodBody(disassembly: String, methodName: String): String =
    val header = s" $methodName("
    val start  = disassembly.indexOf(header)
    assert(start >= 0, s"missing method $methodName in:\n$disassembly")

    val codeStart = disassembly.indexOf("\n    Code:", start)
    assert(codeStart >= 0, s"missing bytecode for $methodName in:\n$disassembly")

    val tail       = disassembly.substring(codeStart + 1)
    val nextMethod = "(?m)^  (?:public|private|protected) ".r.findFirstMatchIn(tail).map(_.start)
    tail.substring(0, nextMethod.getOrElse(tail.length))

  private def assertClosedMethodsAvoidChecks(disassembly: String, methodNames: List[String]): Unit =
    methodNames.foreach: methodName =>
      val body = methodBody(disassembly, methodName)
      checkedMarkers.foreach: marker =>
        assert(!body.contains(marker), s"$methodName unexpectedly reached checked refinement path '$marker':\n$body")

  test("checked constructors alone invoke the factored sign predicate path"):
    val companions = List(
      "trading.quantity.refinement.Refinement$package$NonNegative$",
      "trading.quantity.refinement.Refinement$package$NonZero$",
      "trading.quantity.refinement.Refinement$package$Positive$"
    )

    companions.foreach: className =>
      val bytecode = disassemble(className)
      assert(methodBody(bytecode, "apply").contains("fromChecked:"))
      assert(methodBody(bytecode, "fromChecked").contains("Sign.signum"))

  test("mathematically closed operations do not invoke checked reconstruction"):
    assertClosedMethodsAvoidChecks(
      disassemble("trading.quantity.refinement.Refinement$package$NonNegative$"),
      List("quantityZero", "gridQuantityZero", "quotRemNonNegativeGrid")
    )
    assertClosedMethodsAvoidChecks(
      disassemble("trading.quantity.refinement.Refinement$package$NonZero$"),
      List("rationalOne", "multiply", "reciprocal", "quotRemNonZeroGrid")
    )
    assertClosedMethodsAvoidChecks(
      disassemble("trading.quantity.refinement.Refinement$package$Positive$"),
      List("asNonNegative", "asNonZero", "toPositiveWhole", "quotRemPositiveGrid")
    )
    assertClosedMethodsAvoidChecks(
      disassemble("trading.quantity.refinement.Refinement$package$"),
      List(
        "addPositiveGrid",
        "exactDividePositiveGrid",
        "addPositiveQuantity",
        "exactDividePositiveQuantity",
        "addNonNegativeGrid",
        "exactDivideNonNegativeGrid",
        "addNonNegativeQuantity",
        "exactDivideNonNegativeQuantity",
        "addNonZeroGrid",
        "exactDivideNonZeroGrid",
        "addNonZeroQuantity",
        "exactDivideNonZeroQuantity"
      )
    )

end RefinementClosureBytecodeSuite
