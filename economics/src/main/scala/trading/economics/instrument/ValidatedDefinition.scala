package trading.economics.instrument

import cats.syntax.all.*

import trading.quantity.*
import trading.quantity.runtime.*

/** A raw definition together with the checked evidence needed for total instrument construction. */
sealed abstract class ValidatedDefinition private (val raw: Definition):
  private[instrument] def positionGrid: RegisteredGridRef[raw.roles.position.D]
  private[instrument] def priceGrid: RegisteredGridRef[Divide[raw.roles.quote.D, raw.roles.base.D]]
  private[instrument] def basePerPosition: Rate[raw.roles.position.D, raw.roles.base.D]
  private[instrument] def quotePerPosition: Rate[raw.roles.position.D, raw.roles.quote.D]

private[instrument] object ValidatedDefinition:
  private final class CheckedDefinition(
    definition: Definition,
    checkedPositionGrid: RegisteredGridRef[? <: Dim],
    checkedPriceGrid: RegisteredGridRef[? <: Dim])
    extends ValidatedDefinition(definition):

    val positionGrid: RegisteredGridRef[raw.roles.position.D] =
      checkedPositionGrid.asInstanceOf[RegisteredGridRef[raw.roles.position.D]]

    val priceGrid: RegisteredGridRef[Divide[raw.roles.quote.D, raw.roles.base.D]] =
      checkedPriceGrid.asInstanceOf[RegisteredGridRef[Divide[raw.roles.quote.D, raw.roles.base.D]]]

    val basePerPosition: Rate[raw.roles.position.D, raw.roles.base.D] =
      raw.contractPayoff.basePerPosition.asInstanceOf[Rate[raw.roles.position.D, raw.roles.base.D]]

    val quotePerPosition: Rate[raw.roles.position.D, raw.roles.quote.D] =
      raw.contractPayoff.quotePerPosition.asInstanceOf[Rate[raw.roles.position.D, raw.roles.quote.D]]

  def validate(definition: Definition): Either[InvalidDefinition, ValidatedDefinition] =
    import Validation.*

    val roles   = definition.roles
    val listing = definition.listingRules
    val payoff  = definition.contractPayoff
    val id      = definition.identity.id

    val structural = (
      ensure(0, roles.base.dimension.sharesRegistryWith(roles.quote.dimension))(
        DefinitionViolation.Registry("quote", roles.base.dimension.key, roles.quote.dimension.key)
      ),
      ensure(1, roles.base.dimension.sharesRegistryWith(roles.position.dimension))(
        DefinitionViolation.Registry("position", roles.base.dimension.key, roles.position.dimension.key)
      ),
      ensure(2, roles.base.dimension.sharesRegistryWith(roles.settle.dimension))(
        DefinitionViolation.Registry("settle", roles.base.dimension.key, roles.settle.dimension.key)
      ),
      ensure(3, listing.roles.eq(roles))(
        DefinitionViolation.ComponentRoles(id, Contradiction.ListingRolesDiffer)
      ),
      ensure(4, payoff.roles.eq(roles))(
        DefinitionViolation.ComponentRoles(id, Contradiction.PayoffRolesDiffer)
      ),
      ensure(5, roles.base.id != roles.quote.id)(
        DefinitionViolation.ComponentRoles(id, Contradiction.BaseEqualsQuote)
      ),
      ensure(10, !(payoff.basePerPosition.coefficient.isZero && payoff.quotePerPosition.coefficient.isZero))(
        DefinitionViolation.EmptyPayoff(id)
      )
    ).mapN((_, _, _, _, _, _, _) => ())

    val positionRegistry = ensure(
      6,
      listing.positionLotGrid.dimension.sharesRegistryWith(roles.position.dimension)
    )(
      DefinitionViolation.Registry(
        "position grid",
        roles.position.dimension.key,
        listing.positionLotGrid.dimension.key
      )
    )
    val positionBranch = stage(positionRegistry): _ =>
      ensure(7, listing.positionLotGrid.dimension.key == roles.position.dimension.key)(
        DefinitionViolation.GridDimension(
          "position grid",
          listing.positionLotGrid.key,
          roles.position.dimension.key,
          listing.positionLotGrid.dimension.key
        )
      )

    val expectedPrice = DimRef.divide(roles.quote.dimension.ref, roles.base.dimension.ref).key
    val priceRegistry = ensure(
      8,
      listing.priceGrid.dimension.sharesRegistryWith(roles.base.dimension)
    )(
      DefinitionViolation.Registry("price grid", expectedPrice, listing.priceGrid.dimension.key)
    )
    val priceBranch = stage(priceRegistry): _ =>
      ensure(9, listing.priceGrid.dimension.key == expectedPrice)(
        DefinitionViolation.GridDimension(
          "price grid",
          listing.priceGrid.key,
          expectedPrice,
          listing.priceGrid.dimension.key
        )
      )

    ordered((structural, positionBranch, priceBranch).mapN((_, _, _) => ()))
      .left
      .map(violations => InvalidDefinition(violations.head, violations.tail))
      .map(_ => new CheckedDefinition(definition, listing.positionLotGrid, listing.priceGrid))
  end validate

end ValidatedDefinition
