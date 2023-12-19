package script.strategy;

import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;

import java.util.Map;

public class BankingStrategy implements TaskStrategy {
    private Map<Integer, Integer> requiredItems; // Map of Item ID to Quantity

    public BankingStrategy(Map<Integer, Integer> requiredItems) {
        this.requiredItems = requiredItems;
    }

    @Override
    public void execute(Script script) throws InterruptedException {
        if (openBank(script)) {
            depositInventoryAndEquipment(script);
            withdrawItems(script);
            closeBank(script); // Close the bank after all operations are completed
        }
        equipItems(script); // Equip items after closing the bank
    }

    private boolean openBank(Script script) throws InterruptedException {
        if (script.getBank().isOpen()) {
            return true;
        }

        if (!isAtBank(script)) {
            script.log("Walking to the nearest bank");
            script.getWalking().webWalk(Banks.LUMBRIDGE_UPPER);
        }

        return attemptToOpenBank(script);
    }

    private boolean isAtBank(Script script) {
        return Banks.LUMBRIDGE_UPPER.contains(script.myPlayer());
        // Add other bank areas if needed
    }

    private boolean attemptToOpenBank(Script script) throws InterruptedException {
        if (script.getBank().open()) {
            new ConditionalSleep(10000, 1000) {
                @Override
                public boolean condition() throws InterruptedException {
                    return script.getBank().isOpen();
                }
            }.sleep();
            return true;
        }

        script.log("Failed to open the bank");
        return false;
    }

    private void depositInventoryAndEquipment(Script script) throws InterruptedException {
        if (!script.getInventory().isEmpty()) {
            script.log("Depositing inventory");
            script.getBank().depositAll();
        }
        if (!script.getEquipment().isEmpty()) {
            script.log("Depositing equipment");
            script.getBank().depositWornItems();
        }
    }

    private void withdrawItems(Script script) throws InterruptedException {
        if (!script.getBank().isOpen()) {
            script.log("Bank not open for withdrawal");
            if (!attemptToOpenBank(script)) {
                script.log("Unable to open the bank for withdrawal");
                // Handle the case where the bank cannot be opened, e.g., stop the script or switch strategies.
                return;
            }
        }

        for (Map.Entry<Integer, Integer> entry : requiredItems.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            withdrawSingleItem(script, itemId, quantity);
        }
    }

    private void withdrawSingleItem(Script script, int itemId, int quantity) throws InterruptedException {
        int amountInInventory = (int) script.getInventory().getAmount(itemId);
        int amountToWithdraw = quantity - amountInInventory;
        if (amountToWithdraw <= 0) {
            return;
        }

        if (!script.getBank().contains(itemId)) {
            script.log("Item ID: " + itemId + " not found in the bank");
            // Handle the case where the item is not in the bank. E.g., stop the script or switch strategies.
            return;
        }

        script.log("Withdrawing " + amountToWithdraw + " of item ID: " + itemId);
        boolean success = script.getBank().withdraw(itemId, amountToWithdraw);

        if (!success) {
            script.log("Failed to withdraw item ID: " + itemId);
            return;
        }

        new ConditionalSleep(2000) {
            @Override
            public boolean condition() throws InterruptedException {
                return script.getInventory().contains(itemId);
            }
        }.sleep();
    }

    private void equipItems(Script script) throws InterruptedException {
        for (int itemId : requiredItems.keySet()) {
            equipItemIfPresent(script, itemId);
        }
    }

    private void equipItemIfPresent(Script script, int itemId) throws InterruptedException {
        Item item = script.getInventory().getItem(itemId);
        if (item != null && item.hasAction("Wield")) {
            script.log("Equipping item ID: " + itemId);
            item.interact("Wield");
            waitForEquip(script, itemId);
        }
    }

    private void waitForEquip(Script script, int itemId) throws InterruptedException {
        new ConditionalSleep(5000, 1000) {
            @Override
            public boolean condition() throws InterruptedException {
                return script.getEquipment().contains(itemId);
            }
        }.sleep();
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
    }
}
