package script.state;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
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
    private static final int npcId = 3648;
    private static final int plankId = 2084;
    private static final Position boatPositionPortSarim = new Position(3032,3217,1);
    private final Area portSarimArea = new Area(3026, 3216, 3029, 3219);
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private static final Area karamjaPortArea = new Area(2950, 3144, 2961, 3152);

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
            this.strategy = new SmallNetFishingStrategy();
        } else if (fishingLevel < 40) {
            this.strategy = new FlyFishingStrategy();
        } else {
            this.strategy = new LobsterPotFishingStrategy();
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

        // Set a random quantity for feathers, used for both current and future needs
        int featherQuantity = new Random().nextInt(201) + 800; // Random quantity between 800 and 1000

        Map<Integer, Integer> requiredItemsForFishing = new HashMap<>();
        Map<Integer, Integer> futureFishingItemsMap = new HashMap<>();

        // Add currently required items for fishing
        for (int itemId : currentRequiredItems) {
            int quantity = itemId == GameItem.FEATHER.getId() ? featherQuantity : 1; // Default quantity for most items

            if (itemId == GameItem.COINS.getId()) {
                quantity = new Random().nextInt(20001) + 30000; // Random quantity between 30,000 and 50,000 for coins
            }

            requiredItemsForFishing.put(itemId, quantity);
        }

        // Prepare future fishing items with appropriate quantities
        List<Integer> futureFishingItems = getFishingItemsToBuy();
        for (Integer futureItemId : futureFishingItems) {
            int quantity = futureItemId == GameItem.FEATHER.getId() ? featherQuantity : 1; // Use the same feather quantity

            if (!script.getBank().contains(futureItemId) && futureItemId != GameItem.COINS.getId()) {
                futureFishingItemsMap.put(futureItemId, quantity);
            }
        }

        // Switch to banking state with the strategy to handle the required items
        BankingState returnBankingState = new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForFishing, futureFishingItemsMap, this), this);
        script.setCurrentState(new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForFishing, futureFishingItemsMap, returnBankingState), this));
    }

    @Override
    public BotState nextState(MainScript script) {
        if (shouldSwitchToAnotherState()) {
            if (karamjaArea.contains(script.myPlayer())){
                try {
                    leaveKaramaja(script);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return script.pickRandomState(this);
        }
        return this;
    }


    private boolean shouldSwitchToAnotherState() {
        return System.currentTimeMillis() > switchTime;
    }

    private void leaveKaramaja(Script script) throws InterruptedException {
        script.log("Inventory full, walking to deposit box");
        if (!karamjaPortArea.contains(script.myPlayer())){
            script.getWalking().webWalk(karamjaPortArea);
        }
        interactWithNpc(script);
        crossPlank(script);
    }

    private void interactWithNpc(Script script) throws InterruptedException {
        if (!script.getDialogues().inDialogue()) {
            NPC npcForDeposit = script.getNpcs().closest(npcId);
            if (npcForDeposit != null && npcForDeposit.interact("Pay-fare")) {
                waitForDialogue(script);
                completeDialogueForDeposit(script);
            }
        } else {
            completeDialogueForDeposit(script);
        }
    }

    private void waitForDialogue(Script script) {
        new ConditionalSleep(5000, 500) {
            @Override
            public boolean condition() {
                return script.getDialogues().inDialogue();
            }
        }.sleep();
    }

    private void completeDialogueForDeposit(Script script) throws InterruptedException {
        String[] dialogueOptions = {"Can I journey on this ship?",
                "Search away, I have nothing to hide.", "Ok."};
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue(dialogueOptions);
        }
    }

    private void crossPlank(Script script) {
        waitForArrivalInPortSarim(script);
        Entity plankForDeposit = script.getObjects().closest(plankId);
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            new ConditionalSleep(5000, 500) {
                @Override
                public boolean condition() {
                    return portSarimArea.contains(script.myPlayer());
                }
            }.sleep();
        }
    }

    private void waitForArrivalInPortSarim(Script script) {
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return boatPositionPortSarim.equals(script.myPlayer().getPosition());
            }
        }.sleep();
    }
}
