package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.SwitchStateBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.fishing.FlyFishingStrategy;
import script.strategy.fishing.LobsterPotFishingStrategy;
import script.strategy.fishing.SmallNetFishingStrategy;
import script.utils.GameItem;

import java.util.*;

public class FishingState implements BotState {
    private TaskStrategy strategy;
    private long switchTime;

    public FishingState(MainScript script) {
        updateStrategy(script);
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkFishingEquipment(script)) {
            return;
        }
        updateStrategy(script);
        strategy.execute(script);
    }

    @Override
    public void enterState(MainScript script) {
        script.log("Entering fishing state");
        this.switchTime = System.currentTimeMillis() + (long) (3600000 + Math.random() * 3600000); // 1 to 2 hours
    }

    private void updateStrategy(MainScript script) {
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);
        if (fishingLevel < 20) {
            this.strategy = new SmallNetFishingStrategy(GameItem.SMALL_FISHING_NET.getId());
        } else if (fishingLevel < 40) {
            this.strategy = new FlyFishingStrategy(GameItem.FLY_FISHING_ROD.getId(), GameItem.FEATHER.getId());
        } else {
            this.strategy = new LobsterPotFishingStrategy(GameItem.LOBSTER_POT.getId(), GameItem.COINS.getId());
        }
    }

    private boolean checkFishingEquipment(MainScript script) {
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);
        List<Integer> requiredItemIds = getRequiredFishingItemId(fishingLevel);

        for (int itemId : requiredItemIds) {
            if (!script.getInventory().contains(itemId)) {
                switchToBankingStateForFishingEquipment(script, requiredItemIds);
                return false;
            }
        }
        return true;
    }

    private List<Integer> getRequiredFishingItemId(int fishingLevel) {
        List<Integer> requiredItems = new ArrayList<>();

        if (fishingLevel < 20) {
            requiredItems.add(GameItem.SMALL_FISHING_NET.getId());
        } else if (fishingLevel < 40) {
            requiredItems.add(GameItem.FLY_FISHING_ROD.getId());
            requiredItems.add(GameItem.FEATHER.getId());
        } else {
            requiredItems.add(GameItem.LOBSTER_POT.getId());
        }

        return requiredItems;
    }

    private List<Integer> getFishingItemsToBuy() {
        List<Integer> itemsToBuy = new ArrayList<>();
        itemsToBuy.add(GameItem.SMALL_FISHING_NET.getId());
        itemsToBuy.add(GameItem.FEATHER.getId());
        itemsToBuy.add(GameItem.FLY_FISHING_ROD.getId());
        itemsToBuy.add(GameItem.LOBSTER_POT.getId());
        return itemsToBuy;
    }

    private void switchToBankingStateForFishingEquipment(MainScript script, List<Integer> currentRequiredItems) {
        script.log("Switching to banking state for fishing equipment");

        Map<Integer, Integer> requiredItemsForFishing = new HashMap<>();
        Map<Integer, Integer> futureFishingItemsMap = new HashMap<>();

        // Add currently required items for fishing
        for (int itemId : currentRequiredItems) {
            requiredItemsForFishing.put(itemId, 1); // Assuming a quantity of 1 for each required item
        }

        // Prepare future fishing items based on fishing level
        List<Integer> futureFishingItems = getFishingItemsToBuy();
        for (Integer futureItemId : futureFishingItems) {
            if (!script.getBank().contains(futureItemId)) {
                futureFishingItemsMap.put(futureItemId, 1); // Add missing future items
            }
        }

        // Switch to banking state with the strategy to handle the required items
        BankingState returnBankingState = new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForFishing, futureFishingItemsMap, this), this);
        script.setCurrentState(new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForFishing, futureFishingItemsMap, returnBankingState), this));
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
