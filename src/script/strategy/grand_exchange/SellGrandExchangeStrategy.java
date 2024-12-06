package script.strategy.grand_exchange;

import org.osbot.rs07.api.Bank;
import org.osbot.rs07.api.Chatbox;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.BankingUtils;
import script.utils.SellableItems;
import script.utils.Sleep;

import java.util.List;


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
        final int MAX_RETRIES = 5;
        final double PRICE_INCREMENT_FACTOR = 0.9; // 10% decrease for each retry
        boolean isAccountRestricted = false;

        // Iterating through each item in the inventory
        for (Item item : script.getInventory().getItems()) {
            if (item != null && item.getDefinition().isNoted()) {
                if (SellableItems.isSellable(item.getName())) {

                    // Skip selling if account is restricted and item is not a gold or emerald ring
                    if (isAccountRestricted && !isAllowedItem(item.getName())) {
                        continue;
                    }

                    int itemId = SellableItems.getIdByName(item.getName());
                    int basePrice = determineSellPrice(script, itemId);

                    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                        int finalPrice = (int) (basePrice * Math.pow(PRICE_INCREMENT_FACTOR, attempt));
                        script.log("Selling item: " + item.getName() + " for price: " + item.getAmount()*finalPrice);

                        if (script.getGrandExchange().sellItem(item.getId(), finalPrice, item.getAmount())) {
                            script.log("waiting to see widget");
                            Sleep.sleepUntil(() -> isCollectWidgetWorking(script), 3000);
                        }
                        if (script.getGrandExchange().collect(true)) {
                            script.log("Item successfully sold and collected.");
                            break;
                        } else {
                            script.log("Bid not successful, retrying...");
                            cancelOffer(script);
                            if (!isAccountRestricted) {
                                isAccountRestricted = checkForTradeRestriction(script);
                                if (isAccountRestricted){
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkForTradeRestriction(Script script) {
        List<String> messages = script.getChatbox().getMessages(Chatbox.MessageType.GAME);
        for (String message : messages) {
            if (message.contains("Your account is currently restricted for trading")) {
                script.log(message);
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedItem(String itemName) {
        return itemName.equalsIgnoreCase(SellableItems.GOLD_RINGS.getName()) || itemName.equalsIgnoreCase(SellableItems.EMERALD_RINGS.getName());
    }


    private void cancelOffer(Script script) {
        if (isWidgetWorking(script) && abortOfferWidget(script).interact("Abort offer")) {
            Sleep.sleepUntil(() -> script.getGrandExchange().collect(), SLEEP_DURATION_MS);
            Sleep.sleepUntil(() -> !isCollectWidgetWorking(script), SLEEP_DURATION_MS);
        }
    }

    private boolean isWidgetWorking(Script script) {
        RS2Widget abortOfferWidget = abortOfferWidget(script);
        return abortOfferWidget != null && abortOfferWidget.isVisible();
    }

    private boolean isCollectWidgetWorking(Script script) {
        RS2Widget collectWidget = collectWidget(script);
        return collectWidget != null && collectWidget.isVisible();
    }

    private RS2Widget abortOfferWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.hasAction("Abort offer"));
    }

    private RS2Widget collectWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.hasAction("Collect to bank"));
    }
    private int determineSellPrice(Script script, int itemId) {
        int marketPrice = script.getGrandExchange().getOverallPrice(itemId);
        return (int) (marketPrice * 0.8);
    }

    private void openGrandExchange(Script script) {
        NPC npc = script.getNpcs().closest("Grand Exchange Clerk");
        if (npc != null && npc.interact("Exchange")) {
            Sleep.sleepUntil(()-> script.getGrandExchange().isOpen(), SLEEP_DURATION_MS);
        }
    }

    private void retrieveItemsFromBank(Script script) throws InterruptedException {
        boolean itemWithdrawn = false; // Track if any item is withdrawn

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
                itemWithdrawn = true; // Set to true as an item has been withdrawn
            }
        }

        closeBank(script);

        if (!itemWithdrawn) {
            script.log("No sellable items in the bank. Stopping the script.");
            script.stop();
        }
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
