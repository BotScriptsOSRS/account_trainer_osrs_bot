package script.state;

import script.MainScript;
import script.strategy.TaskStrategy;

public class GrandExchangeState implements BotState {
    private final TaskStrategy grandExchangeStrategy;
    private final BotState returnState; // State to return to after buying or selling items

    public GrandExchangeState(MainScript script, TaskStrategy grandExchangeStrategy, BotState returnState) {
        this.grandExchangeStrategy = grandExchangeStrategy;
        this.returnState = returnState;
        script.log("Entering grand exchange state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        script.log("Executing buying strategy");
        grandExchangeStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        script.log("Returning to previous state after buying items");
        return returnState;
    }
}