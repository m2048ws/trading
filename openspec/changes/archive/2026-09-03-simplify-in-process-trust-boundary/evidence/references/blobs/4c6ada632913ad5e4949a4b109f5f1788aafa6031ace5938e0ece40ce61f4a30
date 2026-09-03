package external.execution.positive;

import scala.util.Either;
import trading.execution.ApplicationCommandId;
import trading.execution.ExecutionAccountId;
import trading.execution.ExecutionSourceId;
import trading.execution.ExecutionTarget;
import trading.execution.NativeFillId;
import trading.execution.SourceSequence;

public final class ExecutionFactoryClient {
  private ExecutionFactoryClient() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static boolean checkedFactoriesPreserveSemantics() {
    Either command = ApplicationCommandId.from("command-1");
    Either source = ExecutionSourceId.from("source-1");
    Either account = ExecutionAccountId.from("account-1");
    Either fill = NativeFillId.from("fill-1");
    Either sequence = SourceSequence.from(scala.math.BigInt.apply(1));
    ExecutionTarget target =
        (ExecutionTarget)
            ExecutionTarget.create(
                    (ExecutionSourceId) source.toOption().get(),
                    (ExecutionAccountId) account.toOption().get())
                .toOption()
                .get();

    return command.isRight()
        && source.isRight()
        && account.isRight()
        && fill.isRight()
        && sequence.isRight()
        && target.source().equals(source.toOption().get())
        && target.account().equals(account.toOption().get());
  }
}
