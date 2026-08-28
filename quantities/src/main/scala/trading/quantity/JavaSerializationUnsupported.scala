package trading.quantity

import java.io.NotSerializableException
import java.io.ObjectStreamException

/**
 * Fail-closed hook for public carriers whose Scala case-class shape must not become a persistence boundary.
 *
 * Java ObjectOutputStream/ObjectInputStream is not a supported durable format for mathematical values or
 * authority-bearing handles.
 */
trait JavaSerializationUnsupported extends Serializable:

  @throws[ObjectStreamException]
  protected final def writeReplace(): AnyRef = throw JavaSerializationUnsupported.failure(this)

  @throws[ObjectStreamException]
  protected final def readResolve(): AnyRef = throw JavaSerializationUnsupported.failure(this)

/** Shared failure construction for carriers that explicitly reject Java serialization. */
object JavaSerializationUnsupported:

  def failure(v: AnyRef): NotSerializableException =
    new NotSerializableException(s"Java object serialization is unsupported for ${v.getClass.getName}")
