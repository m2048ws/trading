package trading.quantity.testkit

import scala.compiletime.testing.Error
import scala.compiletime.testing.typeCheckErrors

/** Inline helpers for positive and negative public-API compile checks. */
object CompileAssertions:

  inline def assertCompiles(inline code: String): Unit =
    val errors =
      typeCheckErrors:
        code
    if errors.nonEmpty then
      throw new AssertionError(s"Expected code to compile, but got:\n${render(errors)}")

  inline def assertDoesNotCompile(inline code: String): Unit =
    val errors =
      typeCheckErrors:
        code
    if errors.isEmpty then
      throw new AssertionError("Expected code not to compile")

  inline def assertDoesNotCompileContaining(inline code: String, expected: String): Unit =
    val errors =
      typeCheckErrors:
        code
    if errors.isEmpty then
      throw new AssertionError("Expected code not to compile")
    if !errors.exists(_.message.contains(expected)) then
      throw new AssertionError(s"Expected a compile error containing '$expected', but got:\n${render(errors)}")

  def assertSameType[A, B](using A =:= B): Unit = ()

  private def render(es: List[Error]): String =
    es
      .map: e =>
        s"- ${e.message}"
      .mkString:
        "\n"

end CompileAssertions
