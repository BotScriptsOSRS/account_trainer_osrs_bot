package script.state;

import script.MainScript;

public interface BotState {
    void execute(MainScript script) throws InterruptedException;
    BotState nextState(MainScript script);
}
