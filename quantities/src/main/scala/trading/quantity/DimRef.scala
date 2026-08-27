package trading.quantity

import java.util.Objects
import scala.quoted.*

/**
 * Authoritative public association between an inhabited dimension type `D` and one canonical runtime key.
 *
 * Runtime authority is a single-valued partial-domain mapping: all supported public witnesses for the same exact atom
 * type have the same [[DimKey]]. Private static interpretation does not imply that a `DimRef` exists.
 *
 * @tparam D the represented dimension
 */
sealed trait DimRef[D <: Dim]:
  def key: DimKey

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
 * independently. Algebra operations preserve their public expression types and apply the matching [[DimKey]] operation
 * to authoritative inputs. Algebraically equal expressions can remain different Scala types; [[SameDimension]]
 * witnesses their equality.
 */
object DimRef:

  /**
   * Base class for a stable nominal singleton key with one authoritative runtime atom identity.
   *
   * Extend this class with an `object`, then pass that object to [[DimRef.atom]]. The final constructor-owned
   * [[AtomId]] prevents the same singleton type from being associated with contradictory runtime keys.
   */
  abstract class NominalAtom protected (id: AtomId) extends JavaSerializationUnsupported:
    final val atomId: AtomId = Objects.requireNonNull(id, "nominal atom ID")

  private final class Canonical[D <: Dim](val key: DimKey) extends DimRef[D]

  private def canonical[D <: Dim](k: DimKey): DimRef[D] =
    new Canonical[D](Objects.requireNonNull(k, "dimension key"))

  private final class Fresh(canonicalKey: DimKey) extends SomeDimensionRef:
    val dimension: DimRef[D] = canonical[D](canonicalKey)

  private final class Atomic(val atomId: AtomId) extends AtomicDimensionRef:
    val dimension: DimRef[D] = canonical[D](DimKey.atom(atomId))

  /** Creates a fresh path-dependent type for a checked canonical runtime key. */
  def fresh(k: DimKey): SomeDimensionRef =
    new Fresh(k)

  /** Creates an atomic witness whose associated type cannot be selected independently of its runtime identity. */
  def atomic(id: AtomId): AtomicDimensionRef =
    new Atomic(id)

  /**
   * Creates an authoritative atom whose compile-time and runtime identities are the same validated literal string.
   *
   * The inline constructor derives the runtime atom ID from the same concrete literal type and rejects caller-selected
   * broad singleton types even when a caller supplies `ValueOf[K]`.
   */
  transparent inline def atom[K <: String & Singleton](using inline key: ValueOf[K]): DimRef[Atom[K]] =
    ${ literalAtom[K]('key) }

  private def literalAtom[K <: String & Singleton: Type](
    key: Expr[ValueOf[K]]
  )(using
    quotes: Quotes
  ): Expr[DimRef[Atom[K]]] =
    import quotes.reflect.*

    TypeRepr.of[K].dealias match
      case ConstantType(StringConstant(value)) =>
        '{
          val _ = $key
          canonical[Atom[K]](DimKey.atom(AtomId(${ Expr(value) })))
        }
      case _ =>
        report.errorAndAbort(
          "Invalid canonical static dimension: a literal atom key must be a concrete string singleton identity"
        )

  /** Creates an authoritative atom keyed by the supplied stable nominal value's exact singleton identity. */
  def atom(
    key: NominalAtom & Singleton
  ): DimRef[Atom[key.type]] =
    canonical(DimKey.atom(key.atomId))

  def one: DimRef[One] =
    canonical(DimKey.one)

  def times[A <: Dim, B <: Dim](
    l: DimRef[A],
    r: DimRef[B]
  ): DimRef[Times[A, B]] =
    canonical[Times[A, B]](DimKey.multiply(l.key, r.key))

  def inverse[A <: Dim](
    v: DimRef[A]
  ): DimRef[Inverse[A]] =
    canonical[Inverse[A]](DimKey.inverse(v.key))

  def divide[A <: Dim, B <: Dim](
    n: DimRef[A],
    d: DimRef[B]
  ): DimRef[Divide[A, B]] =
    canonical[Divide[A, B]](DimKey.multiply(n.key, DimKey.inverse(d.key)))

end DimRef
