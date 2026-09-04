package trading.reference

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*

class CatalogPropertiesSuite extends ScalaCheckSuite:

  private def assetId(index: Int): AssetId =
    AssetId.from(s"property-asset-$index").toOption.get

  private def definition(index: Int): AssetDefinition =
    AssetDefinition(assetId(index), AtomId(s"property:asset:$index"))

  property("successful additions are append-only, advance once, and retry idempotently"):
    forAll { (rawSize: Byte) =>
      val size        = Math.floorMod(rawSize.toInt, 20) + 1
      val definitions = Vector.tabulate(size)(definition)
      val batch       = CatalogBatch.from(definitions.map(value => CatalogCommand.RegisterAsset(value))).toOption.get
      val initial     = CatalogRoot.create().initialState
      val first       = CatalogModel.commit(initial, batch).toOption.get
      val retry       = CatalogModel.commit(first.state, batch).toOption.get

      assertEquals(initial.revision.value, BigInt(0))
      assertEquals(initial.assetCount, 0)
      assertEquals(first.state.revision.value, BigInt(1))
      assertEquals(first.state.assetCount, size)
      assertEquals(first.state.dimensionCount, size)
      assertEquals(retry.state.revision.value, BigInt(1))
      assert(retry.outcome.isInstanceOf[CatalogCommit.Unchanged])
      definitions.foreach(value => assert(first.state.snapshot.resolveAsset(value.id).isRight))
    }

  property("valid command permutations preserve definitions, revision, and lineage semantics"):
    forAll { (seed: Int) =>
      val first    = definition(Math.floorMod(seed, 10000))
      val second   = definition(Math.floorMod(seed, 10000) + 10001)
      val commands = Vector(CatalogCommand.RegisterAsset(first), CatalogCommand.RegisterAsset(second))
      val root     = CatalogRoot.create()
      val forward  = CatalogModel.commit(root.initialState, CatalogBatch.from(commands).toOption.get).toOption.get
      val reverse  =
        CatalogModel.commit(root.initialState, CatalogBatch.from(commands.reverse).toOption.get).toOption.get

      assertEquals(forward.state.revision, reverse.state.revision)
      assertEquals(forward.state.assetCount, reverse.state.assetCount)
      val forwardFirst = forward.state.snapshot.resolveAsset(first.id).toOption.get
      val reverseFirst = reverse.state.snapshot.resolveAsset(first.id).toOption.get
      assert(Asset.reconcile(forwardFirst, reverseFirst).isRight)
    }
end CatalogPropertiesSuite
