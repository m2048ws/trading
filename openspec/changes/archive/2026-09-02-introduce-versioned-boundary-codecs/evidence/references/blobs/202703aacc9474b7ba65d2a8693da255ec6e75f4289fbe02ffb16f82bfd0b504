package trading.codec

import java.util.Objects

/** Shared all-valid-or-indexed-errors traversal for encoded family batches. */
private[codec] object AllOrErrorsBatch:
  def decode[A, E, I](
    inputs: Vector[String],
    limits: DecodeLimits,
    familyName: String
  )(
    codecFailure: WireViolations[WireDecodeViolation] => E,
    indexedFailure: (Int, E) => I
  )(
    decodeRecord: (String, Int) => Either[E, A]
  ): Either[WireViolations[I], Vector[A]] =
    val checkedInputs = Objects.requireNonNull(inputs, s"$familyName inputs")
    val checkedLimits = Objects.requireNonNull(limits, s"$familyName decode limits")
    if checkedInputs.size > checkedLimits.maxBatchRecords then
      val violation = WireDecodeViolation.Limit(
        WireLimitViolation(
          DecodeLimit.BatchRecords,
          checkedInputs.size.toLong,
          checkedLimits.maxBatchRecords,
          WirePath.root,
          0
        )
      )
      Left(WireViolations.one(indexedFailure(0, codecFailure(WireViolations.one(violation)))))
    else
      val results = checkedInputs.zipWithIndex.map: (input, index) =>
        decodeRecord(Objects.requireNonNull(input, s"$familyName input $index"), index)
          .left
          .map(failure => indexedFailure(index, failure))
      val failures = results.collect:
        case Left(failure) => failure
      WireViolations.fromVector(failures) match
        case Some(errors) => Left(errors)
        case None         => Right(results.collect { case Right(value) => value })
    end if
  end decode
end AllOrErrorsBatch
