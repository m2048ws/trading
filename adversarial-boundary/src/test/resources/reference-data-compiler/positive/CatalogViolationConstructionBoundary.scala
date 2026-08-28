package external.reference.positive

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.*
import trading.reference.*

object CatalogViolationConstructionBoundary:
  private def rejectsMalformed(attempt: => Any): Unit =
    try
      val retained = attempt
      throw new AssertionError(s"malformed catalog violation was retained: $retained")
    catch case _: IllegalArgumentException => ()

  private val firstAsset  = assetDefinition("violation-first")
  private val secondAsset = AssetDefinition(firstAsset.id, AtomId("reference:violation-second"))
  private val thirdAsset  = AssetDefinition(firstAsset.id, AtomId("reference:violation-third"))
  private val otherAsset  = assetDefinition("violation-other")
  private val dimension   = DimKey.atom(firstAsset.dimensionAtom)
  private val firstGrid   = gridDefinition(dimension, "violation-grid")
  private val secondGrid  = gridDefinition(dimension, "violation-grid", Rational(1, 1000))
  private val thirdGrid   = gridDefinition(dimension, "violation-grid", Rational(1, 10000))
  private val otherGrid   = gridDefinition(dimension, "violation-other-grid")
  private val otherGridConflict = gridDefinition(dimension, "violation-other-grid", Rational(1, 1000))

  val validViolations: Vector[CatalogViolation] = Vector(
    CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, secondAsset)),
    CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 1), Vector(firstGrid, secondGrid)),
    CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, secondAsset),
    CatalogViolation.ImmutableGridConflict(
      firstGrid.identity,
      firstGrid.quantum.unrefined,
      secondGrid.quantum.unrefined
    ),
    CatalogViolation.AssetDimensionAlreadyBound(dimension, firstAsset.id, otherAsset.id),
    CatalogViolation.MissingGridDimension(firstGrid.identity)
  )

  assert(validViolations.size == 6)

  rejectsMalformed(
    CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 0), Vector(firstAsset, secondAsset))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(3, 0), Vector(firstAsset, secondAsset))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(otherAsset, firstAsset))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateAssetProposal(
      firstAsset.id,
      Vector(0, 1, 2),
      Vector(firstAsset, secondAsset, firstAsset)
    )
  )
  rejectsMalformed(
    CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, secondAsset, thirdAsset))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 0), Vector(firstGrid, secondGrid))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(3, 0), Vector(firstGrid, secondGrid))
  )
  rejectsMalformed(
    CatalogViolation.DuplicateGridProposal(
      firstGrid.identity,
      Vector(0, 1),
      Vector(otherGrid, otherGridConflict)
    )
  )
  rejectsMalformed(
    CatalogViolation.DuplicateGridProposal(
      firstGrid.identity,
      Vector(0, 1, 2),
      Vector(firstGrid, secondGrid, firstGrid)
    )
  )
  rejectsMalformed(
    CatalogViolation.DuplicateGridProposal(
      firstGrid.identity,
      Vector(0, 1),
      Vector(firstGrid, secondGrid, thirdGrid)
    )
  )
  rejectsMalformed(CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, firstAsset))
  rejectsMalformed(CatalogViolation.ImmutableAssetConflict(firstAsset.id, otherAsset, secondAsset))
  rejectsMalformed(
    CatalogViolation.ImmutableGridConflict(
      firstGrid.identity,
      firstGrid.quantum.unrefined,
      firstGrid.quantum.unrefined
    )
  )
  rejectsMalformed(CatalogViolation.ImmutableGridConflict(firstGrid.identity, Rational.zero, Rational.one))
  rejectsMalformed(CatalogViolation.ImmutableGridConflict(firstGrid.identity, Rational.one, -Rational.one))
  rejectsMalformed(CatalogViolation.AssetDimensionAlreadyBound(dimension, firstAsset.id, firstAsset.id))
  rejectsMalformed(CatalogViolation.AssetDimensionAlreadyBound(DimKey.one, firstAsset.id, otherAsset.id))
  rejectsMalformed(
    CatalogViolation.AssetDimensionAlreadyBound(
      DimKey.multiply(dimension, DimKey.atom(otherAsset.dimensionAtom)),
      firstAsset.id,
      otherAsset.id
    )
  )

  private val duplicate = CatalogViolation.DuplicateAssetProposal(
    firstAsset.id,
    Vector(0, 2),
    Vector(firstAsset, secondAsset)
  )
  rejectsMalformed(IndexedCatalogViolation(9, 0, duplicate))
  rejectsMalformed(IndexedCatalogViolation(0, 99, CatalogViolation.MissingGridDimension(firstGrid.identity)))

  val validIndexedViolations: Vector[IndexedCatalogViolation] = Vector(
    IndexedCatalogViolation(0, 0, duplicate),
    IndexedCatalogViolation(1, 1, CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, secondAsset)),
    IndexedCatalogViolation(
      2,
      2,
      CatalogViolation.AssetDimensionAlreadyBound(dimension, firstAsset.id, otherAsset.id)
    ),
    IndexedCatalogViolation(3, 3, CatalogViolation.MissingGridDimension(firstGrid.identity))
  )
  assert(validIndexedViolations.map(_.ruleOrdinal) == Vector(0, 1, 2, 3))
end CatalogViolationConstructionBoundary
