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
        bankingStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        // Return to the state that required banking
        return returnState;
    }
}
