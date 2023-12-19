package script.state;

import org.osbot.rs07.script.Script;
import script.MainScript;
import script.strategy.BankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.WoodcuttingStrategy;
import script.strategy.FishingStrategy;

import java.util.HashMap;
import java.util.Map;

public class FishingState implements BotState {
    private final TaskStrategy strategy;
    private static final int BRONZE_AXE_ID = 1351;
    private static final int SMALL_FISHING_NET_ID = 303;

    private final long switchTime; // Time to switch to woodcutting

    public FishingState(Script script, TaskStrategy strategy) {
        this.strategy = strategy;
        long startTime = System.currentTimeMillis();
        this.switchTime = startTime + (long) (3600000/20 + Math.random() * 3600000/1000); // 1 to 2 hours from startTime
        script.log("Entering fishing state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkFishingEquipment(script)) {
            return; // Exit if the state has been switched to BankingState
        }
        executeFishingStrategy(script);
    }

    private boolean checkFishingEquipment(MainScript script) {
        if (!hasFishingNet(script)) {
            switchToBankingStateForFishingEquipment(script);
            return false;
        }
        return true;
    }

    private boolean hasFishingNet(MainScript script) {
        return script.getInventory().contains(SMALL_FISHING_NET_ID);
    }

    private void switchToBankingStateForFishingEquipment(MainScript script) {
        script.log("Switching to banking state for fishing equipment");
        Map<Integer, Integer> requiredItemsForFishing = new HashMap<>();
        requiredItemsForFishing.put(SMALL_FISHING_NET_ID, 1);
        script.setCurrentState(new BankingState(script, new BankingStrategy(requiredItemsForFishing),
                new FishingState(script, new FishingStrategy())));
    }

    private void executeFishingStrategy(MainScript script) throws InterruptedException {
        strategy.execute(script);
    }

    @Override
    public BotState nextState(MainScript script) {
        if (shouldSwitchToWoodcutting()) {
            return switchToWoodcuttingState(script);
        }
        return this;
    }

    private boolean shouldSwitchToWoodcutting() {
        return System.currentTimeMillis() > switchTime;
    }

    private BotState switchToWoodcuttingState(MainScript script) {
        script.log("Switching to woodcutting");
        Map<Integer, Integer> requiredItemsForWoodcutting = new HashMap<>();
        requiredItemsForWoodcutting.put(BRONZE_AXE_ID, 1);
        return new BankingState(script, new BankingStrategy(requiredItemsForWoodcutting),
                new WoodcuttingState(script, new WoodcuttingStrategy()));
    }
}
