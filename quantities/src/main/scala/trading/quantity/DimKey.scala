package trading.quantity

import java.util.Objects

/**
 * Canonical value used to identify a dimension at runtime.
 *
 * Atom powers are normalized, so algebraically equal dimensions have equal keys. A key is runtime data rather than type
 * evidence; use a [[DimRef]] or [[trading.quantity.runtime.QuantityRegistry]] to bring it into type-safe operations.
 */
final class DimKey private (val powers: Vector[(AtomId, BigInt)]):
  powers.foreach: (atom, power) =>
    val _ = Objects.requireNonNull(atom, "dimension key atom ID")
    val _ = Objects.requireNonNull(power, "dimension key power")

  require(powers.forall(_._2 != 0), "dimension key cannot contain zero powers")
  require(powers == powers.sortBy(_._1.value), "dimension key powers must be sorted")
  require(powers.map(_._1).distinct.size == powers.size, "dimension key cannot contain duplicate atoms")

  override def equals(o: Any): Boolean =
    o match
      case k: DimKey => powers == k.powers
      case _         => false

  override def hashCode: Int =
    powers.hashCode

  override def toString: String =
    s"DimKey($powers)"

end DimKey

/** Normalizing constructors and free-abelian-group operations for [[DimKey]]. */
object DimKey:
  val one: DimKey = new DimKey(Vector.empty)

  def atom(id: AtomId): DimKey =
    new DimKey(Vector(id -> BigInt(1)))

  def apply(powers: Iterable[(AtomId, BigInt)]): DimKey =
    val raw = powers.toVector
    raw.foreach: (atom, power) =>
      val _ = Objects.requireNonNull(atom, "dimension key atom ID")
      val _ = Objects.requireNonNull(power, "dimension key power")

    val normalized = raw.groupMapReduce(_._1)(_._2)(_ + _).filter(_._2 != 0).toVector.sortBy(_._1.value)

    new DimKey(normalized)

  def multiply(l: DimKey, r: DimKey): DimKey =
    apply(l.powers ++ r.powers)

  def inverse(v: DimKey): DimKey =
    apply(v.powers.map((id, power) => id -> -power))

end DimKey
