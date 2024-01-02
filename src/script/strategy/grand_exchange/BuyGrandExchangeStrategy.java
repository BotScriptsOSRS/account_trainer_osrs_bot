package script.strategy.grand_exchange;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.BankingUtils;
import script.utils.GameItem;
import script.utils.Sleep;

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
        Sleep.sleepUntil(()-> !script.getGrandExchange().isOpen(), SLEEP_DURATION_MS);
    }

//    private void buyAndCollectItems(Script script) {
//        final int MAX_RETRIES = 5;
//        final double PRICE_INCREMENT_FACTOR = 1.1; // 10% increase for each retry
//
//        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
//            int itemId = GameItem.getIdByName(entry.getKey());
//            int itemPrice = 100; // script.getGrandExchange().getOverallPrice(itemId);
//
//            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
//                int adjustedPrice = itemPrice; //adjustPriceBasedOnTier(itemPrice, entry.getValue());
//                script.log("Attempt " + (attempt + 1) + ": Buying " + entry.getValue() + " of " + entry.getKey() + " for " + entry.getValue() * adjustedPrice);
//                if (script.getGrandExchange().buyItem(itemId, entry.getKey(), adjustedPrice, entry.getValue())){
//                    script.log("waiting to see widget");
//                    Sleep.sleepUntil(() -> isWidgetWorking(script), 5000);
//                }
//                // Wait for a set period or until item is bought
//                if (script.getGrandExchange().collect(true)) {
//                    script.log("Item successfully bought and collected.");
//                    break; // Exit the loop if the purchase was successful
//                } else {
//                    script.log("Bid not successful, retrying...");
//                    cancelOffer(script);
//                    itemPrice = (int) (itemPrice * PRICE_INCREMENT_FACTOR); // Increase the price for the next attempt
//                }
//            }
//        }
//    }

    private void cancelOffer(Script script) {
        if (isWidgetWorking(script) && abortOfferWidget(script).interact("Abort offer")) {
            Sleep.sleepUntil(() -> script.getGrandExchange().collect(), SLEEP_DURATION_MS);
        }
    }

    private boolean isWidgetWorking(Script script) {
        RS2Widget abortOfferWidget = abortOfferWidget(script);
        return abortOfferWidget != null && abortOfferWidget.isVisible();
    }

    private RS2Widget abortOfferWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.hasAction("Abort offer"));
    }


    private void buyAndCollectItems(Script script) {
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            int itemId = GameItem.getIdByName(entry.getKey());
            int itemPrice = script.getGrandExchange().getOverallPrice(itemId);
            // Adjusted pricing strategy
            int adjustedPrice = adjustPriceBasedOnTier(itemPrice, entry.getValue());

            final int finalPrice = adjustedPrice;
            script.log("Buying "+ entry.getValue() + " of " + entry.getKey() + " for " + entry.getValue()*adjustedPrice);
            Sleep.sleepUntil(()-> script.getGrandExchange().buyItem(itemId, entry.getKey(), finalPrice, entry.getValue()), SLEEP_DURATION_MS);
            Sleep.sleepUntil(()->  script.getGrandExchange().collect(true), SLEEP_DURATION_MS);
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
            Sleep.sleepUntil(()-> script.getGrandExchange().isOpen(), SLEEP_DURATION_MS);
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
        BankingUtils.openBankWithRetry(script);
        if (withdrawCoins(script)){
            closeBank(script);
        } else {
            sellStrategy.execute(script);
        }
    }

    private boolean withdrawCoins(Script script) {
        if (script.getBank().contains(GameItem.COINS.getId())){
            script.getBank().withdrawAll(GameItem.COINS.getId());
            waitForItemInInventory(script, GameItem.COINS.getName());
            return true;
        }
        return false;
    }

    private void waitForItemInInventory(Script script, String itemName) {
        Sleep.sleepUntil(()->  script.getInventory().contains(itemName), SLEEP_DURATION_MS);
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        waitForBankToClose(script);
    }

    private void waitForBankToClose(Script script) {
        Sleep.sleepUntil(()->  !script.getBank().isOpen(), SLEEP_DURATION_MS);
    }
}
