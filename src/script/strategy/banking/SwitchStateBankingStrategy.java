package script.strategy.banking;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.MainScript;
import script.state.BotState;
import script.state.GrandExchangeState;
import script.strategy.TaskStrategy;
import script.strategy.grand_exchange.BuyGrandExchangeStrategy;
import script.utils.GameItem;

import java.util.HashMap;
import java.util.Map;

import static org.osbot.rs07.script.MethodProvider.random;

public class SwitchStateBankingStrategy implements TaskStrategy {
    private final Map<String, Integer> itemsToBuyInAdvance;
    private final int SLEEP_DURATION_MS = 5000;
    private final BotState returnState;
    private final Map<String, Integer> requiredBankItems;

    public SwitchStateBankingStrategy(Map<Integer, Integer> requiredBankItems, Map<Integer, Integer> itemsToBuyInAdvance, BotState returnState) {
        this.requiredBankItems = new HashMap<>();
        this.itemsToBuyInAdvance = new HashMap<>();

        // Processing required items from the bank
        requiredBankItems.forEach((id, quantity) -> {
            String itemName = GameItem.getNameById(id);
            if (itemName != null) {
                this.requiredBankItems.put(itemName, quantity);
            }
        });

        // Processing items to buy
        itemsToBuyInAdvance.forEach((id, quantity) -> {
            String itemName = GameItem.getNameById(id);
            if (itemName != null) {
                this.itemsToBuyInAdvance.put(itemName, quantity);
            }
        });

        this.returnState = returnState;
    }


    private final Area[] BANKS = {
            Banks.LUMBRIDGE_UPPER,
            Banks.VARROCK_WEST,
            Banks.VARROCK_EAST,
            Banks.FALADOR_EAST,
            Banks.FALADOR_WEST,
            Banks.EDGEVILLE,
            Banks.GRAND_EXCHANGE,
            Banks.AL_KHARID,
            Banks.DRAYNOR
    };

    @Override
    public void execute(Script script) throws InterruptedException {
        if (!prepareForBanking(script)) {
            script.log("Failed to prepare for banking.");
            return;
        }

        if (!areAllItemsAvailable(script)) {
            script.log("Not all required items are available in the bank.");
            handleMissingItems(script);
            return;
        }

        performBankingActions(script);
        equipItemsIfNeeded(script);
    }

    private void performBankingActions(Script script) throws InterruptedException {
        depositInventoryAndEquipment(script);
        withdrawRequiredItems(script);  
        closeBank(script);
    }

    private void equipItemsIfNeeded(Script script) {
        equipItems(script);
    }

    private boolean prepareForBanking(Script script) throws InterruptedException {
        if (!isAtBank(script)) {
            walkToNearestBank(script);
        }
        return openBankWithRetry(script);
    }

    private void walkToNearestBank(Script script) {
        script.log("Walking to the nearest F2P bank");
        script.getWalking().webWalk(BANKS);
    }

    private boolean openBankWithRetry(Script script) throws InterruptedException {
        int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (openBank(script)) {
                return true;
            }
            script.log("Attempt to open bank failed, retrying...");
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getBank().isOpen();
                }
            }.sleep();
        }
        script.log("Failed to open bank after multiple attempts");
        return false;
    }

    private boolean areAllItemsAvailable(Script script) throws InterruptedException {
        for (Map.Entry<String, Integer> entry : requiredBankItems.entrySet()) {
            String itemName = entry.getKey();
            int requiredQuantity = entry.getValue();

            MethodProvider.sleep(random(1000, 1500));

            int amountInInventory = (int) script.getInventory().getAmount(itemName);
            int amountEquipped = (int) script.getEquipment().getAmount(itemName);
            int totalAmount = amountInInventory + amountEquipped;

            if (totalAmount < requiredQuantity) {
                Item bankItem = script.getBank().getItem(itemName);
                if (bankItem == null || bankItem.getAmount() < (requiredQuantity - totalAmount)) {
                    return false; // Not enough items in bank, inventory, or equipment
                }
            }
        }
        return true;
    }

    private boolean openBank(Script script) throws InterruptedException {
        if (script.getBank().isOpen()) {
            return true;
        }
        return attemptToOpenBank(script);
    }

    private boolean isAtBank(Script script) {
        for (Area bank : BANKS) {
            if (bank.contains(script.myPlayer())) {
                return true;
            }
        }
        return false;
    }

    private boolean attemptToOpenBank(Script script) throws InterruptedException {
        if (script.getBank().open()) {
            return new ConditionalSleep(10000, 1000) {
                @Override
                public boolean condition() {
                    return script.getBank().isOpen();
                }
            }.sleep();
        }
        script.log("Failed to open the bank");
        return false;
    }

    private void depositInventoryAndEquipment(Script script) {
        if (!script.getInventory().isEmpty()) {
            script.log("Depositing inventory");
            script.getBank().depositAll();
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getInventory().isEmpty();
                }
            }.sleep();
        }
        if (!script.getEquipment().isEmpty()) {
            script.log("Depositing equipment");
            script.getBank().depositWornItems();
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getEquipment().isEmpty();
                }
            }.sleep();
        }
    }

    private void withdrawRequiredItems(Script script) throws InterruptedException {
        if (!openBankIfNeeded(script)) {
            return;
        }

        for (Map.Entry<String, Integer> entry : requiredBankItems.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            int itemId = GameItem.getIdByName(itemName); // Convert name to ID
            if (itemId != -1) {
                withdrawSingleItem(script, itemId, quantity);
            }
        }
    }

    private boolean openBankIfNeeded(Script script) throws InterruptedException {
        if (!script.getBank().isOpen()) {
            script.log("Bank not open for withdrawal");
            return openBankWithRetry(script);
        }
        return true;
    }

    private void handleMissingItems(Script script) {
        Map<String, Integer> itemsToBuy = new HashMap<>();

        for (Map.Entry<String, Integer> entry : itemsToBuyInAdvance.entrySet()) {
            String itemName = entry.getKey();
            int requiredQuantity = entry.getValue();

            int amountInInventory = (int) script.getInventory().getAmount(itemName);
            int amountEquipped = (int) script.getEquipment().getAmount(itemName);
            Item bankItem = script.getBank().getItem(itemName);
            int amountInBank = bankItem != null ? bankItem.getAmount() : 0;

            int totalAmount = amountInInventory + amountEquipped + amountInBank;

            if (totalAmount < requiredQuantity) {
                int amountNeeded = requiredQuantity - totalAmount;
                itemsToBuy.put(itemName, amountNeeded);
            }
        }

        if (!itemsToBuy.isEmpty()) {
            withdrawAllCoins(script);
            closeBank(script);
            script.log("Transitioning to Grand Exchange State to buy missing items.");
            TaskStrategy grandExchangeStrategy = new BuyGrandExchangeStrategy(itemsToBuy);
            ((MainScript) script).setCurrentState(new GrandExchangeState((MainScript) script, grandExchangeStrategy, returnState));
        }
    }


    private void withdrawAllCoins(Script script) {
        if (script.getBank().isOpen()) {
            script.log("Withdrawing all coins");
            script.getBank().withdrawAll("Coins");
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getInventory().contains("Coins");
                }
            }.sleep();
        }
    }

    private void withdrawSingleItem(Script script, int itemId, int quantity) {
        Item bankItem = script.getBank().getItem(itemId);
        String itemName = bankItem != null ? bankItem.getName() : String.valueOf(itemId);
        int amountInInventory = (int) script.getInventory().getAmount(itemId);
        int amountToWithdraw = quantity - amountInInventory;

        if (amountToWithdraw <= 0) {
            return;
        }

        if (!script.getBank().contains(itemId)) {
            script.log("Item with ID: " + itemId + " not found in bank. Skipping withdrawal.");
            return;
        }

        script.log("Withdrawing " + amountToWithdraw + " of " + itemName);
        if (script.getBank().withdraw(itemId, amountToWithdraw)) {
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getInventory().contains(itemId);
                }
            }.sleep();
        }
    }

    private void equipItems(Script script) {
        for (String itemName : requiredBankItems.keySet()) {
            int itemId = GameItem.getIdByName(itemName); // Convert name to ID
            if (itemId != -1) {
                equipItemIfPresent(script, itemId);
            }
        }
    }

    private void equipItemIfPresent(Script script, int itemId) {
        Item item = script.getInventory().getItem(itemId);
        if (item != null && item.hasAction("Wield")) {
            String itemName = item.getName();
            int requiredLevel = getRequiredLevelForItem(itemName);

            if (script.getSkills().getDynamic(Skill.ATTACK) >= requiredLevel) {
                script.log("Equipping " + itemName);
                item.interact("Wield");
                new ConditionalSleep(SLEEP_DURATION_MS) {
                    @Override
                    public boolean condition() {
                        return script.getEquipment().contains(itemId);
                    }
                }.sleep();
            } else {
                script.log("Attack level not high enough to wield " + itemName);
            }
        }
    }


    private int getRequiredLevelForItem(String itemName) {
        itemName = itemName.toLowerCase();
        if (itemName.contains("rune")) {
            return 40;
        } else if (itemName.contains("adamant")) {
            return 30;
        } else if (itemName.contains("mithril")) {
            return 20;
        } else if (itemName.contains("black")) {
            return 10;
        } else if (itemName.contains("steel")) {
            return 5;
        }
        return 0; // Default level if no specific type is found
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return !script.getBank().isOpen();
            }
        }.sleep();
    }
}
