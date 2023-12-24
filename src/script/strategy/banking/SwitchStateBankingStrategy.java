package script.strategy.banking;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

import java.util.Map;

public class SwitchStateBankingStrategy implements TaskStrategy {
    private final Map<Integer, Integer> requiredItems;
    private final int SLEEP_DURATION_MS = 5000;

    public SwitchStateBankingStrategy(Map<Integer, Integer> requiredItems) {
        this.requiredItems = requiredItems;
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
        if (!script.getBank().isOpen()) {
            script.log("Bank not open for withdrawal");
            if (!openBankWithRetry(script)) {
                script.log("Unable to open the bank for withdrawal");
                return;
            }
        }
        for (Map.Entry<Integer, Integer> entry : requiredItems.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            if (!withdrawSingleItem(script, itemId, quantity)) {
                Item bankItem = script.getBank().getItem(itemId);
                String itemName = bankItem != null ? bankItem.getName() : String.valueOf(itemId);
                script.log("Failed to withdraw required item with ID: " + itemName + ". Logging out.");
                script.getLogoutTab().logOut();
                script.stop();
                return;
            }
        }
    }

    private boolean withdrawSingleItem(Script script, int itemId, int quantity) {
        Item bankItem = script.getBank().getItem(itemId);
        String itemName = bankItem != null ? bankItem.getName() : String.valueOf(itemId);
        int amountInInventory = (int) script.getInventory().getAmount(itemId);
        int amountToWithdraw = quantity - amountInInventory;

        if (amountToWithdraw <= 0) {
            return true;
        }

        if (!script.getBank().contains(itemId)) {
            script.log("Item with ID: " + itemName + " not found in the bank");
            return false;
        }

        script.log("Withdrawing " + amountToWithdraw + " of " + itemName);
        if (!script.getBank().withdraw(itemId, amountToWithdraw)) {
            script.log("Failed to withdraw item with ID: " + itemName);
            return false;
        }

        return new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return script.getInventory().contains(itemId);
            }
        }.sleep();
    }

    private void equipItems(Script script) {
        for (int itemId : requiredItems.keySet()) {
            equipItemIfPresent(script, itemId);
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
