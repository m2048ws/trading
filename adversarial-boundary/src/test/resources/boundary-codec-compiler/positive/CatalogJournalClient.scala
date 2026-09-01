package external.codec.positive

import trading.codec.*
import trading.reference.*

object CatalogJournalClient:
  def retain(
    batch: CatalogBatch,
    published: CatalogCommit.Published
  ): CatalogJournalEntry.V1 =
    CatalogJournalEntry.fromPublished(batch, published)

  def decode(
    inputs: Vector[String]
  ): Either[WireViolations[WireDecodeViolation], Vector[CatalogJournalEntry.V1]] =
    CatalogJournalEntry.parseHistory(inputs)

  def replay(
    fresh: CatalogState,
    entries: Vector[CatalogJournalEntry.V1]
  ): Either[CatalogReplayFailure, CatalogReplayResult] =
    CatalogReplay.rebuild(fresh, entries)

  def projections(result: CatalogReplayResult): (CatalogRevision, Int) =
    (result.revision, result.snapshot.gridCount)
end CatalogJournalClient
