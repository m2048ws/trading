package trading.economics.instrument

import java.util.Objects

import trading.quantity.*
import trading.reference.*

/** Closed, contextual failures produced by the pure instrument-assembly boundary. */
enum InstrumentAssemblyViolation extends JavaSerializationUnsupported:
  case AssetResolution(
    instrumentId: InstrumentId,
    role: AssetRole,
    requested: AssetId,
    revision: CatalogRevision,
    cause: CatalogLookupError)
  case GridResolution(
    instrumentId: InstrumentId,
    role: ListingGridRole,
    requested: GridIdentity,
    revision: CatalogRevision,
    cause: CatalogLookupError)
  case EqualBaseAndQuote(instrumentId: InstrumentId, assetId: AssetId)
  case EmptyPayoff(instrumentId: InstrumentId)
  case GridDimension(
    instrumentId: InstrumentId,
    role: ListingGridRole,
    grid: GridIdentity,
    expected: DimKey,
    supplied: DimKey)
end InstrumentAssemblyViolation

/** An attempted empty assembly-error aggregate. */
case object EmptyInstrumentAssemblyErrors extends JavaSerializationUnsupported

/** Domain-owned, immutable, non-empty, deterministically ordered assembly diagnostics. */
final class InstrumentAssemblyErrors private (
  val head: InstrumentAssemblyViolation,
  val tail: Vector[InstrumentAssemblyViolation])
  extends JavaSerializationUnsupported:
  Objects.requireNonNull(head, "assembly error head")
  Objects.requireNonNull(tail, "assembly error tail").foreach(value =>
    Objects.requireNonNull(value, "assembly error")
  )

  val violations: Vector[InstrumentAssemblyViolation] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: InstrumentAssemblyErrors => violations == that.violations
      case _                              => false

  override def hashCode: Int    = violations.hashCode
  override def toString: String = violations.mkString("InstrumentAssemblyErrors(", ",", ")")
end InstrumentAssemblyErrors

object InstrumentAssemblyErrors:
  def one(head: InstrumentAssemblyViolation): InstrumentAssemblyErrors =
    new InstrumentAssemblyErrors(Objects.requireNonNull(head, "assembly error"), Vector.empty)

  def of(
    head: InstrumentAssemblyViolation,
    tail: InstrumentAssemblyViolation*
  ): InstrumentAssemblyErrors =
    new InstrumentAssemblyErrors(Objects.requireNonNull(head, "assembly error"), tail.toVector)

  def from(
    violations: Vector[InstrumentAssemblyViolation]
  ): Either[EmptyInstrumentAssemblyErrors.type, InstrumentAssemblyErrors] =
    Objects.requireNonNull(violations, "assembly errors") match
      case head +: tail => Right(new InstrumentAssemblyErrors(head, tail))
      case _            => Left(EmptyInstrumentAssemblyErrors)
end InstrumentAssemblyErrors

/** Trusted assets retained by an assembled specification. */
sealed trait InstrumentRoles:
  val base: Asset
  val quote: Asset
  val position: Asset
  val settle: Asset

/** The only proof-carrying instrument-definition value. */
sealed trait InstrumentSpec extends JavaSerializationUnsupported:
  val identity: InstrumentIdentity
  val roles: InstrumentRoles

  val positionLotGrid: GridHandle[roles.position.D]
  val priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]]
  val basePerPosition: Rate[roles.position.D, roles.base.D]
  val quotePerPosition: Rate[roles.position.D, roles.quote.D]

  final def sourceId: InstrumentId          = identity.id
  final def underlyingId: UnderlyingId      = identity.underlying
  final def positionLotGridId: GridIdentity = positionLotGrid.identity
  final def priceGridId: GridIdentity       = priceGrid.identity
end InstrumentSpec

object InstrumentSpec

/** Pure resolution and staged validation of stable instrument definitions. */
object InstrumentAssembler:
  private final class ResolvedRoles(
    val base: Asset,
    val quote: Asset,
    val position: Asset,
    val settle: Asset)
    extends InstrumentRoles:
    Objects.requireNonNull(base, "base asset")
    Objects.requireNonNull(quote, "quote asset")
    Objects.requireNonNull(position, "position asset")
    Objects.requireNonNull(settle, "settle asset")

  private final class Assembled(
    val identity: InstrumentIdentity,
    val rolesValue: InstrumentRoles,
    positionGridValue: GridHandle[rolesValue.position.D],
    priceGridValue: GridHandle[Divide[rolesValue.quote.D, rolesValue.base.D]],
    baseRateValue: Rate[rolesValue.position.D, rolesValue.base.D],
    quoteRateValue: Rate[rolesValue.position.D, rolesValue.quote.D])
    extends InstrumentSpec:

    Objects.requireNonNull(identity, "instrument identity")
    val roles: rolesValue.type                                     = rolesValue
    val positionLotGrid: GridHandle[roles.position.D]              = positionGridValue
    val priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]] = priceGridValue
    val basePerPosition: Rate[roles.position.D, roles.base.D]      = baseRateValue
    val quotePerPosition: Rate[roles.position.D, roles.quote.D]    = quoteRateValue

  private def assembled(
    identity: InstrumentIdentity,
    roles: InstrumentRoles,
    positionGrid: GridHandle[roles.position.D],
    priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]],
    baseRate: Rate[roles.position.D, roles.base.D],
    quoteRate: Rate[roles.position.D, roles.quote.D]
  ): InstrumentSpec =
    new Assembled(identity, roles, positionGrid, priceGrid, baseRate, quoteRate)
  def assemble(
    definition: InstrumentDefinition,
    snapshot: CatalogSnapshot
  ): Either[InstrumentAssemblyErrors, InstrumentSpec] =
    val checkedDefinition = Objects.requireNonNull(definition, "instrument definition")
    val checkedSnapshot   = Objects.requireNonNull(snapshot, "catalog snapshot")
    val id                = checkedDefinition.identity.id

    val structural = Vector(
      Option.when(checkedDefinition.roles.base == checkedDefinition.roles.quote)(
        InstrumentAssemblyViolation.EqualBaseAndQuote(id, checkedDefinition.roles.base)
      ),
      Option.when(
        checkedDefinition.payoff.basePerPosition.isZero && checkedDefinition.payoff.quotePerPosition.isZero
      )(InstrumentAssemblyViolation.EmptyPayoff(id))
    ).flatten

    def asset(role: AssetRole, requested: AssetId): Either[InstrumentAssemblyViolation, Asset] =
      checkedSnapshot
        .resolveAsset(requested)
        .left
        .map(cause =>
          InstrumentAssemblyViolation.AssetResolution(id, role, requested, checkedSnapshot.revision, cause)
        )

    def grid(
      role: ListingGridRole,
      requested: GridIdentity
    ): Either[InstrumentAssemblyViolation, GridHandle[? <: Dim]] =
      checkedSnapshot
        .resolveGrid(requested)
        .left
        .map(cause =>
          InstrumentAssemblyViolation.GridResolution(id, role, requested, checkedSnapshot.revision, cause)
        )

    val base         = asset(AssetRole.Base, checkedDefinition.roles.base)
    val quote        = asset(AssetRole.Quote, checkedDefinition.roles.quote)
    val position     = asset(AssetRole.Position, checkedDefinition.roles.position)
    val settle       = asset(AssetRole.Settle, checkedDefinition.roles.settle)
    val positionGrid = grid(ListingGridRole.PositionLot, checkedDefinition.listing.positionLotGrid)
    val priceGrid    = grid(ListingGridRole.Price, checkedDefinition.listing.priceGrid)

    val lookup = Vector(
      base.left.toOption,
      quote.left.toOption,
      position.left.toOption,
      settle.left.toOption,
      positionGrid.left.toOption,
      priceGrid.left.toOption
    ).flatten

    val positionDimension = (position.toOption, positionGrid.toOption) match
      case (Some(asset), Some(resolvedGrid)) if resolvedGrid.dimension.key != asset.dimension.key =>
        Vector(
          InstrumentAssemblyViolation.GridDimension(
            id,
            ListingGridRole.PositionLot,
            resolvedGrid.identity,
            asset.dimension.key,
            resolvedGrid.dimension.key
          )
        )
      case _ => Vector.empty

    val priceDimension = (base.toOption, quote.toOption, priceGrid.toOption) match
      case (Some(baseAsset), Some(quoteAsset), Some(resolvedGrid))
        if checkedDefinition.roles.base != checkedDefinition.roles.quote =>
        val expected = DimRef.divide(quoteAsset.dimension.ref, baseAsset.dimension.ref).key
        Option
          .when(resolvedGrid.dimension.key != expected)(
            InstrumentAssemblyViolation.GridDimension(
              id,
              ListingGridRole.Price,
              resolvedGrid.identity,
              expected,
              resolvedGrid.dimension.key
            )
          )
          .toVector
      case _ => Vector.empty

    val violations = structural ++ lookup ++ positionDimension ++ priceDimension

    InstrumentAssemblyErrors.from(violations) match
      case Right(errors) => Left(errors)
      case Left(_)       =>
        for
          baseAsset     <- base.left.map(InstrumentAssemblyErrors.one)
          quoteAsset    <- quote.left.map(InstrumentAssemblyErrors.one)
          positionAsset <- position.left.map(InstrumentAssemblyErrors.one)
          settleAsset   <- settle.left.map(InstrumentAssemblyErrors.one)
          lots          <- positionGrid.left.map(InstrumentAssemblyErrors.one)
          price         <- priceGrid.left.map(InstrumentAssemblyErrors.one)
        yield
          val roles = new ResolvedRoles(baseAsset, quoteAsset, positionAsset, settleAsset)
          // The immediately preceding runtime-key checks establish these dependent result types. The casts are
          // lexical to this constructor and no retagging evidence escapes.
          val checkedLots  = lots.asInstanceOf[GridHandle[roles.position.D]]
          val checkedPrice = price.asInstanceOf[GridHandle[Divide[roles.quote.D, roles.base.D]]]
          val baseRate     =
            Rate(roles.position.dimension.ref, roles.base.dimension.ref, checkedDefinition.payoff.basePerPosition)
          val quoteRate =
            Rate(roles.position.dimension.ref, roles.quote.dimension.ref, checkedDefinition.payoff.quotePerPosition)
          assembled(
            checkedDefinition.identity,
            roles,
            checkedLots,
            checkedPrice,
            baseRate,
            quoteRate
          )
    end match
  end assemble

  /** Deterministic head projection of [[assemble]]; no validation rule is duplicated. */
  def assembleFirst(
    definition: InstrumentDefinition,
    snapshot: CatalogSnapshot
  ): Either[InstrumentAssemblyViolation, InstrumentSpec] =
    assemble(definition, snapshot).left.map(_.head)
end InstrumentAssembler
