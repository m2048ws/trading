package trading.codec

import trading.reference.*

object CatalogJournalAuthorityEscapesAreUnavailable:
  def retained(
    batch: CatalogBatch,
    published: CatalogCommit.Published
  ): CatalogJournalEntry.V1 =
    CatalogJournalEntry.fromPublished(batch, published)

  // OFFENDING-BEGIN
  def forge(revision: CatalogRevision, batch: CatalogBatch) =
    new CatalogJournalEntry.V1(revision, batch)

  def packageForge(revision: CatalogRevision, batch: CatalogBatch) =
    CatalogJournalEntry.construct(revision, batch)

  def retainUnchanged(batch: CatalogBatch, unchanged: CatalogCommit.Unchanged) =
    CatalogJournalEntry.fromPublished(batch, unchanged)

  def encodeState(state: CatalogState) =
    CatalogJournalEntry.encode(state)

  def replayRoot(root: CatalogRoot, entries: Vector[CatalogJournalEntry.V1]) =
    CatalogReplay.rebuild(root, entries)

  def forgeResult(state: CatalogState) =
    new CatalogReplayResult(state)

  def packageForgeResult(state: CatalogState) =
    CatalogReplayResult.from(state)

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
