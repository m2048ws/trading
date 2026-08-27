package trading.quantity.runtime

import trading.quantity.Dim
import trading.quantity.DimKey
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.SameDimension
import trading.quantity.grid.GridError
import trading.quantity.grid.SameGrid

/** Closed hierarchy of failures while recovering typed evidence from registry-owned runtime identities. */
sealed abstract class RuntimeEvidenceError extends JavaSerializationUnsupported with Product with Serializable

/** The compared witnesses represent different canonical dimensions. */
final case class DimensionEvidenceMismatch(left: DimKey, right: DimKey) extends RuntimeEvidenceError

/** The compared grid witnesses failed a checked grid-compatibility comparison. */
final case class GridEvidenceMismatch(cause: GridError) extends RuntimeEvidenceError

/** The compared witnesses were issued by different registries. */
final case class ForeignRegistryEvidence(left: DimKey, right: DimKey) extends RuntimeEvidenceError

/**
 * Recovers dimension and grid equality evidence for values whose identities were resolved at runtime.
 *
 * Evidence is issued only when both witnesses belong to the same registry and their canonical definitions agree.
 */
object RuntimeEvidence:

  def sameDimension(l: DimensionWitness, r: DimensionWitness): Either[RuntimeEvidenceError, SameDimension[l.D, r.D]] =
    if !l.dimension.sharesRegistryWith(r.dimension) then
      Left(ForeignRegistryEvidence(l.dimension.key, r.dimension.key))
    else
      SameDimension
        .between(l.dimension.ref, r.dimension.ref)
        .toRight(DimensionEvidenceMismatch(l.dimension.key, r.dimension.key))

  def sameGrid[A <: Dim, B <: Dim](
    l: RegisteredGridRef[A],
    r: RegisteredGridRef[B]
  ): Either[RuntimeEvidenceError, SameGrid[A, l.G, B, r.G]] =
    if !l.dimension.sharesRegistryWith(r.dimension) then
      Left(ForeignRegistryEvidence(l.dimension.key, r.dimension.key))
    else
      SameGrid
        .between(l.asGridRef, r.asGridRef)
        .left
        .map(GridEvidenceMismatch(_))

end RuntimeEvidence
