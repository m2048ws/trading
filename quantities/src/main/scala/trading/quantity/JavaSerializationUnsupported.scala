package trading.quantity

import java.io.NotSerializableException
import java.io.ObjectStreamException

/**
 * Fail-closed hook for public carriers whose Scala case-class shape must not become a persistence boundary.
 *
 * Project-owned logical packed models remain ordinary in-memory values. Java ObjectOutputStream/ObjectInputStream is
 * not a supported codec for them or for any other invariant-bearing carrier.
 */
abstract class JavaSerializationUnsupported extends Serializable:

  @throws[ObjectStreamException]
  protected final def writeReplace(): AnyRef = throw JavaSerializationUnsupported.failure(this)

  @throws[ObjectStreamException]
  protected final def readResolve(): AnyRef = throw JavaSerializationUnsupported.failure(this)

/** Shared failure construction for carriers that explicitly reject Java serialization. */
object JavaSerializationUnsupported:

  def failure(v: AnyRef): NotSerializableException =
    new NotSerializableException(s"Java object serialization is unsupported for ${v.getClass.getName}")
