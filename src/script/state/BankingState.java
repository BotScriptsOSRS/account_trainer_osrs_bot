package script.state;

import script.MainScript;
import script.strategy.TaskStrategy;

public class BankingState implements BotState {
    private final TaskStrategy bankingStrategy;
    private final BotState returnState; // State to return to after banking

    public BankingState(MainScript script, TaskStrategy bankingStrategy, BotState returnState) {
        this.bankingStrategy = bankingStrategy;
        this.returnState = returnState;
        script.log("Entering banking state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        script.log("Executing banking strategy");
        bankingStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        script.log("Returning to previous state after banking");
        return returnState;
    }
}
