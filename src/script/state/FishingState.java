package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.SwitchStateOrEquipmentBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.fishing.FlyFishingStrategy;
import script.strategy.fishing.LobsterPotFishingStrategy;
import script.strategy.fishing.SmallNetFishingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FishingState implements BotState {
    private TaskStrategy strategy;
    private final long switchTime;

    // Fishing item IDs
    private static final int SMALL_FISHING_NET_ID = 303;
    private static final int FLY_FISHING_ROD_ID = 309;
    private static final int FEATHER_ID = 314;
    private static final int LOBSTER_POT_ID = 301;

    private static final int COINS_ID = 995;

    private final Random random = new Random();

    public FishingState(MainScript script) {
        updateStrategy(script);
        long startTime = System.currentTimeMillis();
        this.switchTime = startTime + (long) (3600000 + Math.random() * 3600000);
        script.log("Entering fishing state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkFishingEquipment(script)) {
            return;
        }
        updateStrategy(script);
        strategy.execute(script);
    }

    private void updateStrategy(MainScript script) {
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);
        if (fishingLevel < 20) {
            this.strategy = new SmallNetFishingStrategy(SMALL_FISHING_NET_ID);
        } else if (fishingLevel < 40) {
            this.strategy = new FlyFishingStrategy(FLY_FISHING_ROD_ID, FEATHER_ID);
        } else {
            this.strategy = new LobsterPotFishingStrategy(LOBSTER_POT_ID, COINS_ID);
        }
    }

    private boolean checkFishingEquipment(MainScript script) {
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);

        if (fishingLevel < 20 && !script.getInventory().contains(SMALL_FISHING_NET_ID)) {
            switchToBankingStateForFishingEquipment(script, SMALL_FISHING_NET_ID);
            return false;
        } else if (fishingLevel < 40 && (!script.getInventory().contains(FLY_FISHING_ROD_ID) || !script.getInventory().contains(FEATHER_ID))) {
            switchToBankingStateForFishingEquipment(script, FLY_FISHING_ROD_ID, FEATHER_ID) ;
            return false;
        } else if (fishingLevel >= 40 && (!script.getInventory().contains(LOBSTER_POT_ID) || !script.getInventory().contains(COINS_ID))) {
            switchToBankingStateForFishingEquipment(script, LOBSTER_POT_ID, COINS_ID);
            return false;
        }
        return true;
    }

    private void switchToBankingStateForFishingEquipment(MainScript script, int... itemIds) {
        script.log("Switching to banking state for fishing equipment");
        Map<Integer, Integer> requiredItemsForFishing = new HashMap<>();
        for (int itemId : itemIds) {
            int quantity = 1; // Default quantity for most items

            // Apply random quantity only to feathers and coins
            if (itemId == FEATHER_ID || itemId == COINS_ID) {
                // Generate a random quantity between 100,000 and 1,000,000
                quantity = random.nextInt(900001) + 100000; // (max - min + 1) + min
            }

            requiredItemsForFishing.put(itemId, quantity);
        }
        script.setCurrentState(new BankingState(script, new SwitchStateOrEquipmentBankingStrategy(requiredItemsForFishing), this));
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
