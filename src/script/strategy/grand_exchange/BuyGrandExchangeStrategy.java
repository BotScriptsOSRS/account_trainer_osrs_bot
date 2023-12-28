package script.strategy.grand_exchange;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.util.Map;

public class BuyGrandExchangeStrategy implements TaskStrategy {

    private final Map<String, Integer> requiredItems;
    private final int SLEEP_DURATION_MS = 5000;
    private final Area grandExchangeArea = new Area(3160, 3486, 3168, 3493);
    private final SellGrandExchangeStrategy sellStrategy;

    public BuyGrandExchangeStrategy(Map<String, Integer> requiredItems) {
        this.requiredItems = requiredItems;
        sellStrategy = new SellGrandExchangeStrategy();
    }
    @Override
    public void execute(Script script) throws InterruptedException {
        if (!isInGrandExchangeArea(script)) {
            script.log("Walking to GrandExchange");
            walkToGrandExchangeArea(script);
        }
        if (!script.getInventory().contains(GameItem.COINS.getId())) {
            script.log("Checking bank for coins");
            checkBankForCoins(script);
        }
        if (!script.getGrandExchange().isOpen()) {
            script.log("Opening GrandExchange");
            openGrandExchange(script);
        }
        if (script.getGrandExchange().isOpen()) {
            buyAndCollectItems(script);
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

    private void buyAndCollectItems(Script script) {
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            int itemId = GameItem.getIdByName(entry.getKey());
            int itemPrice = script.getGrandExchange().getOverallPrice(itemId);

            // Adjusted pricing strategy
            int adjustedPrice = adjustPriceBasedOnTier(itemPrice, entry.getValue());

            final int finalPrice = adjustedPrice;
            script.log("Buying item: " + entry.getKey() + " for price: " + adjustedPrice);
            new ConditionalSleep(SLEEP_DURATION_MS, 500) {
                @Override
                public boolean condition() {
                    return script.getGrandExchange().buyItem(itemId, entry.getKey(), finalPrice, entry.getValue());
                }
            }.sleep();

            new ConditionalSleep(SLEEP_DURATION_MS, 500) {
                @Override
                public boolean condition() {
                    return script.getGrandExchange().collect();
                }
            }.sleep();
        }
    }

    private int adjustPriceBasedOnTier(int itemPrice, int itemQuantity) {
        if (itemPrice <= 10) {
            // For very cheap items, add a fixed amount
            return itemPrice + 5;
        } else if (itemPrice <= 1000 && itemQuantity < 10) {
            // For moderately priced items, add a smaller percentage
            return 3000;
        } else if (itemPrice <= 1000) {
            // For moderately priced items, add a smaller percentage
            return (int) (itemPrice * 1.3);
        } else {
            // For expensive items, use the existing 30% increase
            return (int) (itemPrice * 1.2);
        }
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

    private boolean isInGrandExchangeArea(Script script) {
        return Banks.GRAND_EXCHANGE.contains(script.myPlayer());
    }

    private void walkToGrandExchangeArea(Script script) {
        script.log("Walking to the Grand Exchange");
        script.getWalking().webWalk(grandExchangeArea);
    }

    private void checkBankForCoins(Script script) throws InterruptedException {
        openBankWithRetry(script);
        if (withdrawCoins(script)){
            closeBank(script);
        } else {
            sellStrategy.execute(script);
        }
    }

    private void openBankWithRetry(Script script) throws InterruptedException {
        int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (openBank(script)) {
                return;
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

    private boolean withdrawCoins(Script script) {
        int MINIMUM_COINS = 50000;
        if (script.getBank().contains(GameItem.COINS.getId()) && script.getBank().getAmount(GameItem.COINS.getId()) > MINIMUM_COINS){
            script.getBank().withdrawAll(GameItem.COINS.getId());
            waitForItemInInventory(script, GameItem.COINS.getName());
            return true;
        }
        return false;
    }

    private void waitForItemInInventory(Script script, String itemName) {
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return script.getInventory().contains(itemName);
            }
        }.sleep();
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        waitForBankToClose(script);
    }

    private void waitForBankToClose(Script script) {
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return !script.getBank().isOpen();
            }
        }.sleep();
    }
}
