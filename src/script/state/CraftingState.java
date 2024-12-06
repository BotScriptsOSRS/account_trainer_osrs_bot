package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.SwitchStateBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.crafting.EmeraldRingStrategy;
import script.strategy.crafting.GoldRingStrategy;
import script.strategy.crafting.LeatherStrategy;
import script.utils.GameItem;

import java.util.*;

public class CraftingState implements BotState {
    private TaskStrategy strategy;
    private long switchTime;

    public CraftingState(MainScript script) {
        updateStrategy(script);
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkCraftingEquipment(script)) {
            return;
        }
        updateStrategy(script);
        strategy.execute(script);
    }

    @Override
    public void enterState(MainScript script) {
        script.log("Entering crafting state");
        this.switchTime = System.currentTimeMillis() + (long) (3600000 + Math.random() * 3600000); // 1 to 2 hours
    }

    private void updateStrategy(MainScript script) {
        int craftingLevel = script.getSkills().getStatic(Skill.CRAFTING);

        if (craftingLevel < 5) {
            this.strategy = new LeatherStrategy();
        } else if (craftingLevel < 27) {
            this.strategy = new GoldRingStrategy();
        } else {
            this.strategy = new EmeraldRingStrategy();
        }
    }

    private List<Integer> getRequiredCraftingItems(int craftingLevel) {
        List<Integer> requiredItems = new ArrayList<>();

        if (craftingLevel < 5) {
            requiredItems.add(GameItem.NEEDLE.getId());
            requiredItems.add(GameItem.THREAD.getId());
            requiredItems.add(GameItem.SOFT_LEATHER.getId());
        } else if (craftingLevel < 27) {
            requiredItems.add(GameItem.GOLD_BAR.getId());
            requiredItems.add(GameItem.RING_MOULD.getId());
        } else {
            requiredItems.add(GameItem.GOLD_BAR.getId());
            requiredItems.add(GameItem.EMERALD.getId());
            requiredItems.add(GameItem.RING_MOULD.getId());
        }
        return requiredItems;
    }

    private boolean checkCraftingEquipment(MainScript script) {
        int craftingLevel = script.getSkills().getStatic(Skill.CRAFTING);
        List<Integer> requiredItemIds = getRequiredCraftingItems(craftingLevel);

        for (int itemId : requiredItemIds) {
            if (!script.getInventory().contains(itemId)) {
                switchToBankingStateForCraftingEquipment(script, requiredItemIds);
                return false;
            }
        }
        return true;
    }

    private List<Integer> getCraftingItemsToBuy(MainScript script) {
        int craftingLevel = script.getSkills().getStatic(Skill.CRAFTING);
        List<Integer> itemsToBuy = new ArrayList<>();
        if (craftingLevel < 5) {
            itemsToBuy.add(GameItem.NEEDLE.getId());
            itemsToBuy.add(GameItem.THREAD.getId());
            itemsToBuy.add(GameItem.SOFT_LEATHER.getId());
        } else if (craftingLevel < 27) {
            itemsToBuy.add(GameItem.GOLD_BAR.getId());
            itemsToBuy.add(GameItem.RING_MOULD.getId());
        }
        if (craftingLevel >= 27){
            itemsToBuy.add(GameItem.GOLD_BAR.getId());
            itemsToBuy.add(GameItem.EMERALD.getId());
            itemsToBuy.add(GameItem.RING_MOULD.getId());
        }
        return itemsToBuy;
    }

    private void switchToBankingStateForCraftingEquipment(MainScript script, List<Integer> currentRequiredItems) {
        script.log("Switching to banking state for crafting equipment");
        int craftingLevel = script.getSkills().getStatic(Skill.CRAFTING);

        int goldBarsAndEmeraldQuantity = calculateGoldBarsAndEmeraldQuantity(craftingLevel);

        Map<Integer, Integer> requiredItemsForCrafting = calculateRequiredItemsForCrafting(currentRequiredItems, craftingLevel);

        Map<Integer, Integer> futureCraftingItemsMap = craftingItemsToBuy(script, goldBarsAndEmeraldQuantity, craftingLevel);

        List<String> depositExceptionNames;

        if (craftingLevel < 5) {
            depositExceptionNames = Arrays.asList(GameItem.THREAD.getName(), GameItem.NEEDLE.getName());
        } else {
            depositExceptionNames = Collections.singletonList(GameItem.RING_MOULD.getName());
        }

        BankingState bankingState = new BankingState(
                script,
                new SwitchStateBankingStrategy(requiredItemsForCrafting, futureCraftingItemsMap, this, depositExceptionNames),
                this
        );

        script.setCurrentState(bankingState);
    }

    private int calculateGoldBarsAndEmeraldQuantity(int craftingLevel) {
        Random random = new Random();
        if (craftingLevel < 27) {
            int experienceRequired = getExperienceDifference(craftingLevel, 27);
            return experienceRequired / 15 + random.nextInt(51) + 10;
        } else {
            return random.nextInt(51) + 200; // Random quantity between 200 and 250
        }
    }

    private Map<Integer, Integer> calculateRequiredItemsForCrafting(List<Integer> currentRequiredItems, int craftingLevel) {
        Map<Integer, Integer> requiredItems = new HashMap<>();
        for (int itemId : currentRequiredItems) {
            int quantity = 1;
            if (itemId == GameItem.EMERALD.getId()) {
                quantity = 13;
            } else if (itemId == GameItem.GOLD_BAR.getId()) {
                quantity = craftingLevel < 27 ? 27 : 13;
            } else if (itemId == GameItem.THREAD.getId()) {
                quantity = 10;
            } else if (itemId == GameItem.SOFT_LEATHER.getId()) {
                quantity = 26;
            }
            requiredItems.put(itemId, quantity);
        }
        return requiredItems;
    }

    private Map<Integer, Integer> craftingItemsToBuy(MainScript script, int goldBarsAndEmeraldQuantity, int craftingLevel) {
        List<Integer> futureCraftingItems = getCraftingItemsToBuy(script);
        Map<Integer, Integer> futureCraftingItemsMap = new HashMap<>();
        for (Integer futureItemId : futureCraftingItems) {
            int quantity = 1;
            if (futureItemId == GameItem.GOLD_BAR.getId() || futureItemId == GameItem.EMERALD.getId()) {
                quantity = goldBarsAndEmeraldQuantity;
            } else if (futureItemId == GameItem.THREAD.getId() && craftingLevel == 1) {
                quantity = 10;
            } else if (futureItemId == GameItem.SOFT_LEATHER.getId() && craftingLevel == 1) {
                quantity = 50;
            } else if (futureItemId == GameItem.THREAD.getId() || futureItemId == GameItem.SOFT_LEATHER.getId()){
                quantity = 10;
            }
            if (!script.getBank().contains(futureItemId)) {
                futureCraftingItemsMap.put(futureItemId, quantity);
            }
        }
        return futureCraftingItemsMap;
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
            output = (int) (double) (points / 4);
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
