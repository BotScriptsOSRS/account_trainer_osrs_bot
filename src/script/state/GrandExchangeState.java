package script.state;

import script.MainScript;
import script.strategy.TaskStrategy;

public class GrandExchangeState implements BotState {
    private final TaskStrategy grandExchangeStrategy;
    private final BotState returnState; // State to return to after buying or selling items

    public GrandExchangeState(MainScript script, TaskStrategy grandExchangeStrategy, BotState returnState) {
        script.log("Entering grand exchange state");
        this.grandExchangeStrategy = grandExchangeStrategy;
        this.returnState = returnState;
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        grandExchangeStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        script.log("Returning to previous state");
        return returnState;
    }
}
