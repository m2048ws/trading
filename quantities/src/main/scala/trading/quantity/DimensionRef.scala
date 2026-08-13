package trading.quantity

/**
 * Type-carrying evidence that dimension `D` has a particular canonical runtime key.
 *
 * This is the bridge between compile-time dimensional safety and dimensions assembled or resolved at runtime.
 *
 * @tparam D the represented dimension
 */
sealed trait DimRef[D <: Dimension]:
  def key: DimensionKey

/**
 * A dimension resolved at runtime together with its fresh, path-dependent type.
 *
 * Keeping the witness and type together allows callers to retain dimensional safety when the concrete dimension was not
 * known at compile time.
 */
sealed trait SomeDimensionRef:
  /** Fresh static dimension represented by this runtime witness. */
  type D <: Dimension
  def dimension: DimRef[D]

/** A generative witness that gives one runtime atom identity a fresh compile-time dimension type. */
sealed trait AtomicDimensionRef extends SomeDimensionRef:
  def atomId: AtomId

/**
 * Creates dimension witnesses and lifts the dimension algebra's identity, product, inverse, and quotient operations to
 * them.
 *
 * Each operation constructs both the corresponding type expression and its normalized [[DimensionKey]]. Algebraically
 * equal expressions can remain different Scala types; [[SameDimension]] witnesses their equality after their canonical
 * keys agree.
 */
object DimRef:

  private final class Canonical[D <: Dimension](val key: DimensionKey) extends DimRef[D]

  private def canonical[D <: Dimension](k: DimensionKey): DimRef[D] =
    new Canonical[D](k)

  private final class Fresh(canonicalKey: DimensionKey) extends SomeDimensionRef:
    sealed trait FreshDimension extends Dimension

    type D = FreshDimension

    val dimension: DimRef[D] = canonical[D](canonicalKey)

  private final class Atomic(val atomId: AtomId) extends AtomicDimensionRef:

    sealed trait AtomicDimension extends Dimension

    type D = AtomicDimension

    val dimension: DimRef[D] = canonical[D](DimensionKey.atom(atomId))

  /** Creates a fresh path-dependent type for a checked canonical runtime key. */
  def fresh(k: DimensionKey): SomeDimensionRef =
    new Fresh(k)

  /** Creates an atomic witness whose associated type cannot be selected independently of its runtime identity. */
  def atomic(id: AtomId): AtomicDimensionRef =
    new Atomic(id)

  def one: DimRef[One] =
    canonical(DimensionKey.one)

  def times[A <: Dimension, B <: Dimension](l: DimRef[A], r: DimRef[B]): DimRef[Times[A, B]] =
    canonical(DimensionKey.multiply(l.key, r.key))

  def inverse[A <: Dimension](v: DimRef[A]): DimRef[Inverse[A]] =
    canonical(DimensionKey.inverse(v.key))

  def divide[A <: Dimension, B <: Dimension](n: DimRef[A], d: DimRef[B]): DimRef[Divide[A, B]] =
    times(n, inverse(d))

end DimRef

/**
 * Evidence that dimension types `A` and `B` have the same canonical runtime identity.
 *
 * The evidence permits safe coercion between otherwise distinct static types and can only be obtained after their
 * dimension keys agree.
 */
final class SameDimension[A <: Dimension, B <: Dimension] private ():
  def coerceQuantity(v: Quantity[A]): Quantity[B] =
    v.asInstanceOf[Quantity[B]]

  def coerceGrid[G](v: GridQuantity[A, G]): GridQuantity[B, G] =
    v.asInstanceOf[GridQuantity[B, G]]

/** Recovers [[SameDimension]] evidence after comparing canonical runtime keys. */
object SameDimension:
  /** Recover type evidence only after the canonical runtime identities agree. */
  def between[A <: Dimension, B <: Dimension](l: DimRef[A], r: DimRef[B]): Option[SameDimension[A, B]] =
    Option.when(l.key == r.key):
      new SameDimension()

end SameDimension
