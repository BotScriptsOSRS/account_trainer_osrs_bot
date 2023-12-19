package script.state;

import org.osbot.rs07.script.Script;
import script.MainScript;
import script.strategy.BankingStrategy;
import script.strategy.FishingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.WoodcuttingStrategy;

import java.util.HashMap;
import java.util.Map;

public class WoodcuttingState implements BotState {
    private final TaskStrategy strategy;
    private final long switchTime;
    private static final int SMALL_FISHING_NET_ID = 303;
    private static final int BRONZE_AXE_ID = 1351; // Example item ID for a bronze axe

    public WoodcuttingState(Script script, TaskStrategy strategy) {
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
        script.setCurrentState(new BankingState(script, new BankingStrategy(requiredItemsForWoodcutting),
                new WoodcuttingState(script, new WoodcuttingStrategy())));
    }

    private void executeWoodcuttingStrategy(MainScript script) throws InterruptedException {
        strategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) { // Include script parameter if needed
        if (shouldSwitchToFishing()) {
            return switchToFishingState(script);
        }
        return this;
    }

    private boolean shouldSwitchToFishing() {
        return System.currentTimeMillis() > switchTime;
    }

    private BotState switchToFishingState(MainScript script) {
        script.log("Switching to fishing");
        // Required items for fishing
        Map<Integer, Integer> requiredItemsForFishing = new HashMap<>();
        requiredItemsForFishing.put(SMALL_FISHING_NET_ID, 1);
        // Return a new BankingState instance with the required items and new FishingState as the next state
        return new BankingState(script, new BankingStrategy(requiredItemsForFishing), new FishingState(script, new FishingStrategy()));
    }
}
