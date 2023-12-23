package script.state;

import script.MainScript;
import script.strategy.banking.SwitchStateOrEquipmentBankingStrategy;
import script.strategy.TaskStrategy;

import java.util.HashMap;
import java.util.Map;

public class FishingState implements BotState {
    private final TaskStrategy strategy;
    private static final int SMALL_FISHING_NET_ID = 303;

    private final long switchTime; // Time to switch to another state

    public FishingState(MainScript script, TaskStrategy strategy) {
        this.strategy = strategy;
        long startTime = System.currentTimeMillis();
        this.switchTime = startTime + (long) (3600000/6 + Math.random() * 3600000/6); // 1 to 2 hours from startTime
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
        script.setCurrentState(new BankingState(script, new SwitchStateOrEquipmentBankingStrategy(requiredItemsForFishing), this));
    }

    private void executeFishingStrategy(MainScript script) throws InterruptedException {
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
