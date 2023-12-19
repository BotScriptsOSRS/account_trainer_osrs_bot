package script.strategy;

import org.osbot.rs07.script.Script;

public interface TaskStrategy {
    void execute(Script script) throws InterruptedException;
}

