package trading.codec

import com.networknt.schema.SchemaRegistry
import munit.FunSuite
import org.erdtman.jcs.JsonCanonicalizer

class BoundaryDependencyScopeSuite extends FunSuite:
  test("independent schema and canonicalization mechanisms are available in test scope"):
    assertEquals(classOf[SchemaRegistry].getName, "com.networknt.schema.SchemaRegistry")
    assertEquals(classOf[JsonCanonicalizer].getName, "org.erdtman.jcs.JsonCanonicalizer")
