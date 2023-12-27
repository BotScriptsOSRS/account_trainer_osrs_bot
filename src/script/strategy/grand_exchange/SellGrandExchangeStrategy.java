package script.strategy.grand_exchange;

import org.osbot.rs07.api.Bank;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.SellableItems;


public class SellGrandExchangeStrategy implements TaskStrategy {

    private final int SLEEP_DURATION_MS = 5000;
    private final Area grandExchangeArea = new Area(3160, 3486, 3168, 3493);

    @Override
    public void execute(Script script) throws InterruptedException {
        if (!isInGrandExchangeArea(script)) {
            script.log("Walking to GrandExchange");
            walkToGrandExchangeArea(script);
        }
        retrieveItemsFromBank(script);
        if (!script.getGrandExchange().isOpen()) {
            script.log("Opening GrandExchange");
            openGrandExchange(script);
        }
        if (script.getGrandExchange().isOpen()) {
            sellAndCollectItems(script);
            closeGrandExchange(script);
        }
    }

    private void closeGrandExchange(Script script) {
        script.log("Closing the GrandExchange");
        script.getGrandExchange().close();
        waitForGrandExchangeToClose(script);
    }

    private void waitForGrandExchangeToClose(Script script) {
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return !script.getGrandExchange().isOpen();
            }
        }.sleep();
    }

    private void sellAndCollectItems(Script script) {
        // Iterating through each item in the inventory
        for (Item item : script.getInventory().getItems()) {
            if (item != null && item.getDefinition().isNoted()) {  // Check if the item is noted
                int itemId = SellableItems.getIdByName(item.getName());
                final int finalPrice = determineSellPrice(script, itemId);
                script.log("Selling item: " + item.getName() + " for price: " + finalPrice);
                // Initiate the selling process
                new ConditionalSleep(SLEEP_DURATION_MS, 500) {
                    @Override
                    public boolean condition() {
                        return script.getGrandExchange().sellItem(item.getId(), finalPrice, item.getAmount());
                    }
                }.sleep();

                // Collect coins or any unsold items
                new ConditionalSleep(SLEEP_DURATION_MS, 500) {
                    @Override
                    public boolean condition() {
                        return script.getGrandExchange().collect();
                    }
                }.sleep();
            }
        }
    }

    private int determineSellPrice(Script script, int itemId) {
        int marketPrice = script.getGrandExchange().getOverallPrice(itemId);
        return (int) (marketPrice * 0.75);
    }

    private void openGrandExchange(Script script) {
        NPC npc = script.getNpcs().closest("Grand Exchange Clerk");
        if (npc != null && npc.interact("Exchange")) {
            new ConditionalSleep(SLEEP_DURATION_MS, 500) {
                @Override
                public boolean condition() {
                    return script.getGrandExchange().isOpen();
                }
            }.sleep();
        }
    }
    private void retrieveItemsFromBank(Script script) throws InterruptedException {
        if (!script.getBank().isOpen()){
            openBankWithRetry(script);
        }
        if (!script.getBank().isBankModeEnabled(Bank.BankMode.WITHDRAW_NOTE)){
            new ConditionalSleep(SLEEP_DURATION_MS) {
                @Override
                public boolean condition() {
                    return script.getBank().enableMode(Bank.BankMode.WITHDRAW_NOTE);
                }
            }.sleep();
        }

        for (SellableItems item : SellableItems.values()) {
            if (script.getBank().contains(item.getName())) {
                script.log("Withdrawing " + item.getName());
                new ConditionalSleep(SLEEP_DURATION_MS) {
                    @Override
                    public boolean condition() {
                        return script.getBank().withdrawAll(item.getName());
                    }
                }.sleep();
            }
        }
        closeBank(script);
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

    private boolean isInGrandExchangeArea(Script script) {
        return Banks.GRAND_EXCHANGE.contains(script.myPlayer());
    }

    private void walkToGrandExchangeArea(Script script) {
        script.log("Walking to the Grand Exchange");
        script.getWalking().webWalk(grandExchangeArea);
    }
}
