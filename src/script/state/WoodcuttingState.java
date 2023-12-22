package script.state;

import script.MainScript;
import script.strategy.BankingStrategy;
import script.strategy.TaskStrategy;

import java.util.HashMap;
import java.util.Map;

public class WoodcuttingState implements BotState {
    private final TaskStrategy strategy;
    private final long switchTime;
    private static final int BRONZE_AXE_ID = 1351; // Example item ID for a bronze axe

    public WoodcuttingState(MainScript script, TaskStrategy strategy) {
        this.strategy = strategy;
        long startTime = System.currentTimeMillis();
        this.switchTime = startTime + (long) (3600000/6 + Math.random() * 3600000/6); // 1 to 2 hours from startTime
        script.log("Entering woodcutting state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkWoodcuttingEquipment(script)) {
            return; // Exit if the state has been switched to BankingState
        }
        executeWoodcuttingStrategy(script);
    }

    private boolean checkWoodcuttingEquipment(MainScript script) {
        if (!hasBronzeAxe(script)) {
            switchToBankingStateForWoodcuttingEquipment(script);
            return false;
        }
        return true;
    }

    private boolean hasBronzeAxe(MainScript script) {
        return script.getInventory().contains(BRONZE_AXE_ID) || script.getEquipment().contains(BRONZE_AXE_ID);
    }

    private void switchToBankingStateForWoodcuttingEquipment(MainScript script) {
        script.log("Switching to banking state for woodcutting equipment");
        Map<Integer, Integer> requiredItemsForWoodcutting = new HashMap<>();
        requiredItemsForWoodcutting.put(BRONZE_AXE_ID, 1);
        script.setCurrentState(new BankingState(script, new BankingStrategy(requiredItemsForWoodcutting), this));
    }

    private void executeWoodcuttingStrategy(MainScript script) throws InterruptedException {
        strategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        if (shouldSwitchToAnotherState()) {
            return script.pickRandomState(this);
        }
        return this;
    }

    private boolean shouldSwitchToAnotherState() {
        return System.currentTimeMillis() > switchTime;
    }
}
