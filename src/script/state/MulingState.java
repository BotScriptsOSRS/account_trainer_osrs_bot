package script.state;

import script.MainScript;
import script.strategy.TaskStrategy;

public class MulingState implements BotState{
    private final TaskStrategy mulingStrategy;
    private final BotState returnState;

    public MulingState(MainScript script, TaskStrategy mulingStrategy, BotState returnState) {
        script.log("Entering muling state");
        this.mulingStrategy = mulingStrategy;
        this.returnState = returnState;
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        script.log("Executing muling strategy");
        mulingStrategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        script.log("Returning to previous state");
        return returnState;
    }
}
