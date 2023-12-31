package script.strategy.grand_exchange;

import org.osbot.rs07.api.Bank;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.BankingUtils;
import script.utils.SellableItems;
import script.utils.Sleep;


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
        Sleep.sleepUntil(()->!script.getGrandExchange().isOpen(), SLEEP_DURATION_MS);
    }

    private void sellAndCollectItems(Script script) {
        // Iterating through each item in the inventory
        for (Item item : script.getInventory().getItems()) {
            if (item != null && item.getDefinition().isNoted()) {
                if (SellableItems.isSellable(item.getName())) {
                    int itemId = SellableItems.getIdByName(item.getName());
                    final int finalPrice = determineSellPrice(script, itemId);
                    script.log("Selling item: " + item.getName() + " for price: " + finalPrice);
                    // Initiate the selling process
                    Sleep.sleepUntil(()->script.getGrandExchange().sellItem(item.getId(), finalPrice, item.getAmount()), SLEEP_DURATION_MS);
                    // Collect coins or any unsold items
                    Sleep.sleepUntil(()->script.getGrandExchange().collect(true), SLEEP_DURATION_MS);
                }
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
            Sleep.sleepUntil(()-> script.getGrandExchange().isOpen(), SLEEP_DURATION_MS);
        }
    }

    private void retrieveItemsFromBank(Script script) throws InterruptedException {
        if (!script.getBank().isOpen()){
            BankingUtils.openBankWithRetry(script);
        }
        if (!script.getBank().isBankModeEnabled(Bank.BankMode.WITHDRAW_NOTE) && script.getBank().enableMode(Bank.BankMode.WITHDRAW_NOTE)){
            Sleep.sleepUntil(()-> script.getBank().isBankModeEnabled(Bank.BankMode.WITHDRAW_NOTE), SLEEP_DURATION_MS);
        }
        for (SellableItems item : SellableItems.values()) {
            if (script.getBank().contains(item.getName()) && script.getBank().withdrawAll(item.getName())) {
                script.log("Withdrawing " + item.getName());
                Sleep.sleepUntil(()-> !script.getBank().contains(item.getName()), SLEEP_DURATION_MS);
            }
        }
        closeBank(script);
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        Sleep.sleepUntil(()-> !script.getBank().isOpen(), SLEEP_DURATION_MS);
    }

    private boolean isInGrandExchangeArea(Script script) {
        return Banks.GRAND_EXCHANGE.contains(script.myPlayer());
    }

    private void walkToGrandExchangeArea(Script script) {
        script.log("Walking to the Grand Exchange");
        script.getWalking().webWalk(grandExchangeArea);
    }
}
