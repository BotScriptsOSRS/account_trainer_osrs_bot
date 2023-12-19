package script.state;

import org.osbot.rs07.script.Script;
import script.MainScript;
import script.strategy.TaskStrategy;

public class BankingState implements BotState {
    private Script script;
    private TaskStrategy bankingStrategy;
    private BotState nextState;

    public BankingState(Script script, TaskStrategy bankingStrategy, BotState nextState) {
        this.bankingStrategy = bankingStrategy;
        this.nextState = nextState;
        script.log("Entering banking state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        bankingStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        return nextState;
    }
}

