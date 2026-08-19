package trading.quantity

/**
 * Authoritative public association between an inhabited dimension type `D` and one canonical runtime key.
 *
 * Runtime authority is a single-valued partial-domain mapping: all supported public witnesses for the same exact atom
 * type have the same [[DimensionKey]], while a statically available [[Normalize]] does not imply that a `DimRef`
 * exists. Conversely, generic code possessing a `DimRef[D]` must still accept and forward `Normalize[D]` when static
 * arithmetic requires it.
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
  type D = Atom[this.type]
  def dimension: DimRef[D]

/** A generative witness that gives one runtime atom identity a fresh compile-time dimension type. */
sealed trait AtomicDimensionRef extends SomeDimensionRef:
  def atomId: AtomId

/**
 * Creates authoritative dimension witnesses and lifts the dimension algebra's identity, product, inverse, and quotient
 * operations to them.
 *
 * Root constructors bind the static type and runtime identity at one boundary; callers cannot select them
 * independently. Each algebra operation uses one complete [[Normalize]] result for its static output and the matching
 * [[DimensionKey]] operation on authoritative inputs. Algebraically equal expressions can remain different Scala types;
 * [[SameDimension]] witnesses their equality after their canonical keys agree.
 */
object DimRef:

  /**
   * Base class for a stable nominal singleton key with one authoritative runtime atom identity.
   *
   * Extend this class with an `object`, then pass that object to [[DimRef.atom]]. The final constructor-owned
   * [[AtomId]] prevents the same singleton type from being associated with contradictory runtime keys.
   */
  abstract class NominalAtom protected (final val atomId: AtomId) extends JavaSerializationUnsupported

  private final class Canonical[D <: Dimension](val key: DimensionKey) extends DimRef[D]

  private def canonical[D <: Dimension](k: DimensionKey): DimRef[D] =
    new Canonical[D](k)

  private final class Fresh(canonicalKey: DimensionKey) extends SomeDimensionRef:
    val dimension: DimRef[D] = canonical[D](canonicalKey)

  private final class Atomic(val atomId: AtomId) extends AtomicDimensionRef:
    val dimension: DimRef[D] = canonical[D](DimensionKey.atom(atomId))

  /** Creates a fresh path-dependent type for a checked canonical runtime key. */
  def fresh(k: DimensionKey): SomeDimensionRef =
    new Fresh(k)

  /** Creates an atomic witness whose associated type cannot be selected independently of its runtime identity. */
  def atomic(id: AtomId): AtomicDimensionRef =
    new Atomic(id)

  /**
   * Creates an authoritative atom whose compile-time and runtime identities are the same validated literal string.
   *
   * `Normalize[Atom[K]]` rejects caller-selected broad singleton types even when a caller supplies `ValueOf[K]`.
   */
  def atom[K <: String & Singleton](
    using
    key: ValueOf[K],
    valid: Normalize[Atom[K]]
  ): DimRef[Atom[K]] =
    val _ = valid
    canonical(DimensionKey.atom(AtomId(key.value)))

  /** Creates an authoritative atom keyed by the supplied stable nominal value's exact singleton identity. */
  def atom(
    key: NominalAtom & Singleton
  )(using valid: Normalize[Atom[key.type]]
  ): DimRef[Atom[key.type]] =
    val _ = valid
    canonical(DimensionKey.atom(key.atomId))

  def one: DimRef[One] =
    canonical(DimensionKey.one)

  def times[A <: Dimension, B <: Dimension](
    l: DimRef[A],
    r: DimRef[B]
  )(using
    operation: Normalize[Times[A, B]]
  ): DimRef[operation.Out] =
    val _ = operation
    canonical[operation.Out](DimensionKey.multiply(l.key, r.key))

  def inverse[A <: Dimension](
    v: DimRef[A]
  )(using operation: Normalize[Inverse[A]]
  ): DimRef[operation.Out] =
    val _ = operation
    canonical[operation.Out](DimensionKey.inverse(v.key))

  def divide[A <: Dimension, B <: Dimension](
    n: DimRef[A],
    d: DimRef[B]
  )(using
    operation: Normalize[Divide[A, B]]
  ): DimRef[operation.Out] =
    val _ = operation
    canonical[operation.Out](DimensionKey.multiply(n.key, DimensionKey.inverse(d.key)))

end DimRef
