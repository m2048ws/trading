package trading.codec

import trading.reference.*

object CatalogJournalAuthorityEscapesAreUnavailable:
  def retained(
    batch: CatalogBatch,
    published: CatalogCommit.Published
  ): CatalogJournalEntry.V1 =
    CatalogJournalEntry.fromPublished(batch, published)

  // OFFENDING-BEGIN
  def retainUnchanged(batch: CatalogBatch, unchanged: CatalogCommit.Unchanged) =
    CatalogJournalEntry.fromPublished(batch, unchanged)

  def encodeState(state: CatalogState) =
    CatalogJournalEntry.encode(state)

  def replayRoot(root: CatalogRoot, entries: Vector[CatalogJournalEntry.V1]) =
    CatalogReplay.rebuild(root, entries)

  def authority(entry: CatalogJournalEntry.V1) =
    (
      entry.root,
      entry.state,
      entry.snapshot,
      entry.lineage,
      entry.timestamp,
      entry.checkpoint,
      entry.activation,
      entry.delisting
    )

  val repository = CatalogJournalRepository
  val durableCheckpoint = CatalogCheckpoint
  // OFFENDING-END
end CatalogJournalAuthorityEscapesAreUnavailable
