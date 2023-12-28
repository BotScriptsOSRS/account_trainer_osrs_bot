package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.SwitchStateBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.fishing.FlyStrategy;
import script.strategy.fishing.LobsterPotStrategy;
import script.strategy.fishing.SmallNetStrategy;
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
            this.strategy = new SmallNetStrategy();
        } else if (fishingLevel < 40) {
            this.strategy = new FlyStrategy();
        } else {
            this.strategy = new LobsterPotStrategy();
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
            requiredItems.add(GameItem.COINS.getId());
        }

        return requiredItems;
    }

    private List<Integer> getFishingItemsToBuy(MainScript script) {
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);
        List<Integer> itemsToBuy = new ArrayList<>();
        if (fishingLevel < 20) {
            itemsToBuy.add(GameItem.SMALL_FISHING_NET.getId());
            itemsToBuy.add(GameItem.FEATHER.getId());
            itemsToBuy.add(GameItem.FLY_FISHING_ROD.getId());
            itemsToBuy.add(GameItem.LOBSTER_POT.getId());
        } else if (fishingLevel < 40) {
            itemsToBuy.add(GameItem.FEATHER.getId());
            itemsToBuy.add(GameItem.FLY_FISHING_ROD.getId());
            itemsToBuy.add(GameItem.LOBSTER_POT.getId());
        } else {
            itemsToBuy.add(GameItem.LOBSTER_POT.getId());
        }
        return itemsToBuy;
    }

    private void switchToBankingStateForFishingEquipment(MainScript script, List<Integer> currentRequiredItems) {
        script.log("Switching to banking state for fishing equipment");
        int fishingLevel = script.getSkills().getStatic(Skill.FISHING);
        int experienceRequired = getExperienceDifference(fishingLevel, 40);

        Random random = new Random();
        int addRandomQuantity = random.nextInt(51) + 10;
        int featherQuantity = experienceRequired / 50 + addRandomQuantity;

        Map<Integer, Integer> requiredItemsForFishing = populateRequiredItems(currentRequiredItems, featherQuantity, random);
        Map<Integer, Integer> futureFishingItemsMap = prepareFutureFishingItems(script, featherQuantity);

        BankingState bankingState = new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForFishing, futureFishingItemsMap, this), this);
        script.setCurrentState(bankingState);
    }

    private Map<Integer, Integer> populateRequiredItems(List<Integer> currentRequiredItems, int featherQuantity, Random random) {
        Map<Integer, Integer> requiredItems = new HashMap<>();
        for (int itemId : currentRequiredItems) {
            int quantity = itemId == GameItem.FEATHER.getId() ? featherQuantity : 1;
            if (itemId == GameItem.COINS.getId()) {
                quantity = random.nextInt(20001) + 30000;
            }
            requiredItems.put(itemId, quantity);
        }
        return requiredItems;
    }

    private Map<Integer, Integer> prepareFutureFishingItems(MainScript script, int featherQuantity) {
        List<Integer> futureFishingItems = getFishingItemsToBuy(script);
        Map<Integer, Integer> futureItemsMap = new HashMap<>();
        for (Integer futureItemId : futureFishingItems) {
            if (!script.getBank().contains(futureItemId) && futureItemId != GameItem.COINS.getId()) {
                futureItemsMap.put(futureItemId, futureItemId == GameItem.FEATHER.getId() ? featherQuantity : 1);
            }
        }
        return futureItemsMap;
    }

    public static int getExperienceDifference(int currentLevel, int targetLevel) {
        return getExperienceForLevel(targetLevel) - getExperienceForLevel(currentLevel);
    }

    public static int getExperienceForLevel(int level) {
        int points = 0;
        int output = 0;

        for (int lvl = 1; lvl <= level; lvl++) {
            points += (int) Math.floor(lvl + 300 * Math.pow(2, lvl / 7.0));
            if (lvl >= level) {
                return output;
            }
            output = (int) Math.floor((double) points / 4);
        }
        return 0;
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
