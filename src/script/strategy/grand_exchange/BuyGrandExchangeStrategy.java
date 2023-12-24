package script.strategy.grand_exchange;

import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.QuickExchange;

import java.util.Map;

public class BuyGrandExchangeStrategy implements TaskStrategy {

    private final Map<String, Integer> requiredItems;
    private static final int ITEM_INTERACT_WAIT_MS = 5000;
    private static final int SLEEP_DURATION_MS = 5000;
    private static final int MAX_BANK_ATTEMPTS = 3;

    public BuyGrandExchangeStrategy(Map<String, Integer> requiredItems) {
        this.requiredItems = requiredItems;
    }

    @Override
    public void execute(Script script) throws InterruptedException {
        if (ensureAtGrandExchange(script)) {
            performExchangeActions(script);
        } else {
            script.log("Failed to reach the Grand Exchange.");
        }
    }

    private boolean ensureAtGrandExchange(Script script) {
        if (!isInGrandExchangeArea(script)) {
            walkToGrandExchangeArea(script);
        }
        return isInGrandExchangeArea(script);
    }

    private void performExchangeActions(Script script) throws InterruptedException {
        if (openBankAndWithdrawCoins(script)) {
            performPurchases(script);
        }
    }

    private boolean openBankAndWithdrawCoins(Script script) throws InterruptedException {
        if (openBankWithRetry(script)) {
            withdrawAllCoins(script);
            script.getBank().close();
            return true;
        }
        return false;
    }

    private void performPurchases(Script script) throws InterruptedException {
        QuickExchange quickExchange = new QuickExchange(script);
        quickExchange.open();

        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            quickBuyItem(script, quickExchange, entry.getKey(), entry.getValue());
        }
    }

    private void quickBuyItem(Script script, QuickExchange quickExchange, String itemName, int quantity) throws InterruptedException {
        quickExchange.quickBuy(itemName, quantity, false);
        new ConditionalSleep(60000, 500) {
            @Override
            public boolean condition() {
                return script.getInventory().contains(itemName);
            }
        }.sleep();
    }

    private boolean openBankWithRetry(Script script) throws InterruptedException {
        for (int attempt = 0; attempt < MAX_BANK_ATTEMPTS; attempt++) {
            if (tryToOpenBank(script)) {
                return true;
            }
            script.log("Attempt " + (attempt + 1) + " to open bank failed, retrying...");
            MethodProvider.sleep(SLEEP_DURATION_MS);
        }
        script.log("Failed to open bank after multiple attempts");
        return false;
    }

    private boolean tryToOpenBank(Script script) throws InterruptedException {
        return script.getBank().open() && waitForBankToOpen(script);
    }

    private boolean waitForBankToOpen(Script script) {
        return new ConditionalSleep(10000, 1000) {
            @Override
            public boolean condition() {
                return script.getBank().isOpen();
            }
        }.sleep();
    }

    private void withdrawAllCoins(Script script) {
        if (script.getBank().isOpen()) {
            script.log("Withdrawing all coins");
            script.getBank().withdrawAll("Coins");
            waitForCoinsInInventory(script);
        }
    }

    private void waitForCoinsInInventory(Script script) {
        new ConditionalSleep(ITEM_INTERACT_WAIT_MS) {
            @Override
            public boolean condition() {
                return script.getInventory().contains("Coins");
            }
        }.sleep();
    }

    private boolean isInGrandExchangeArea(Script script) {
        return Banks.GRAND_EXCHANGE.contains(script.myPlayer());
    }

    private void walkToGrandExchangeArea(Script script) {
        script.log("Walking to the Grand Exchange");
        script.getWalking().webWalk(Banks.GRAND_EXCHANGE);
    }

}
