package script.utils;

import org.osbot.rs07.api.GrandExchange;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.MethodProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QuickExchange extends MethodProvider {

    private int WIDGET_ROOT_SEARCHITEM = 162;

    private String WIDGET_TEXT_SEARCHITEM = "Start typing the name of an item to search for it.";

    private String NPC_NAME = "Grand Exchange Clerk";
    private String NPC_ACTION_EXCHANGE = "Exchange";

    private String ITEM_ACTION_OFFER = "Offer";

    private WidgetActionFilter decreaseWidgetFilter = new WidgetActionFilter("-5%");
    private WidgetActionFilter increaseWidgetFilter = new WidgetActionFilter("+5%");

    public QuickExchange(MethodProvider api) {
        exchangeContext(api.getBot());
    }

    public boolean isOpen() {
        return getGrandExchange().isOpen();
    }

    public boolean open() {
        NPC npc = getNpcs().closest(NPC_NAME);

        if (npc == null) {
            log("Unable to locate Grand Exchange NPC");
            return false;
        }

        if (!npc.interact(NPC_ACTION_EXCHANGE)) {
            log("Error interacting with Grand Exchange NPC");
            return false;
        }

        Sleep.until(this::isOpen, 6000, 800);

        return isOpen();
    }

    public boolean hasOpenBox() {
        List<GrandExchange.Box> boxesToCheck;
        // If we're (presumably) a non-mem, we only check the first 3 boxes
        if (getWorlds().isMembersWorld()) {
            boxesToCheck = Arrays.asList(GrandExchange.Box.values());
        } else {
            boxesToCheck = Arrays.asList(GrandExchange.Box.BOX_1, GrandExchange.Box.BOX_2, GrandExchange.Box.BOX_3);
        }

        // Return true if any box is empty
        return boxesToCheck.stream().anyMatch(box -> getGrandExchange().getStatus(box) == GrandExchange.Status.EMPTY);
    }
    
    public boolean quickBuy(String itemName, int amount, boolean withdrawToBank) throws InterruptedException {
        if (itemName == null)
            return false;

        if (!getInventory().contains("Coins")) {
            log("Inventory contains no money to buy item " + itemName);
            return false;
        }

        GrandExchange.Box boxToUse = tryGetOpenBox();
        if (boxToUse == null) {
            log("No GE Boxes available to use to buy item " + itemName);
            return false;
        }

        if (getGrandExchange().isSellOfferOpen()) {
            if (!getGrandExchange().goBack()) {
                log("Error returning to main GE Screen");
                return false;
            }
        }

        if (!getGrandExchange().buyItems(boxToUse)) {
            log("Error interacting with buy-offer widget");
            return false;
        }

        Sleep.until(() -> getGrandExchange().isBuyOfferOpen() && getWidgets().singleFilter(WIDGET_ROOT_SEARCHITEM, w -> w != null && w.getMessage() != null && w.getMessage().equals(WIDGET_TEXT_SEARCHITEM)) != null, 5000, 500);

        getKeyboard().typeString(itemName, false);

        final Wrapper<RS2Widget> searchItemWidget = new Wrapper<>();
        Sleep.until(() -> {
            searchItemWidget.set(getWidgets().singleFilter(WIDGET_ROOT_SEARCHITEM, f -> f != null && f.getMessage() != null && f.getMessage().equals(itemName)));
            return searchItemWidget.isSet();
        }, 6000, 1000);

        if (!searchItemWidget.isSet()) {
            log(String.format("Unable to locate searched item (%s)", itemName));
            return false;
        }

        if (!searchItemWidget.get().interact()) {
            log(String.format("Error interacting with searched item (%s)", itemName));
            return false;
        }

        Sleep.until(() -> getWidgets().singleFilter(getGrandExchange().getInterfaceId(), w -> w != null && w.getMessage() != null && w.getMessage().equals(itemName)) != null, 10000, 1000);

        return finishHandlingItem(itemName, amount, true, withdrawToBank, boxToUse);
    }

    public boolean quickSell(String itemName, int amount) throws InterruptedException {
        if (itemName == null)
            return false;

        if (!getInventory().contains(itemName)) {
            log("Inventory does not contain item indicated to sell");
            return false;
        }

        if (getInventory().getAmount(itemName) < amount) {
            log("Inventory does not contain enough " + itemName + "to sell");
            return false;
        }

        GrandExchange.Box boxToUse = tryGetOpenBox();
        if (boxToUse == null) {
            log("No GE Boxes available to use to sell item " + itemName);
            return false;
        }

        if (getGrandExchange().isBuyOfferOpen()) {
            if (!getGrandExchange().goBack()) {
                log("Error returning to main GE Screen");
                return false;
            }
        }

        if (!getInventory().interact(ITEM_ACTION_OFFER, itemName)) {
            log("Error trying interact with item " + itemName);
            return false;
        }

        Sleep.until(() -> getGrandExchange().isSellOfferOpen(), 2000, 400);

        return finishHandlingItem(itemName, amount, false, true, boxToUse);
    }

    private boolean finishHandlingItem(String itemName, int amount, boolean isBuying, boolean withdrawToBank, GrandExchange.Box boxToUse) throws InterruptedException {
        if (getGrandExchange().getOfferQuantity() != amount) {
            if (!getGrandExchange().setOfferQuantity(amount)) {
                log(String.format("Error setting offer quantity for item %s and amount %d", itemName, amount));
                return false;
            }
        }

        if (isBuying && getGrandExchange().getOfferPrice() > 8 && getGrandExchange().getOfferPrice() < 1000){
            Random rand = new Random(); // Create a Random object
            int randomPrice = rand.nextInt(501) + 2000; // Generate a random number between 2000 and 2500

            if (!getGrandExchange().setOfferPrice(randomPrice)) {
                log("Error setting price to " + randomPrice + " GP for " + itemName);
                return false;
            }
        } else {
            int numberOfPriceAdjustments = 3;
            if (!modifyPricePct(isBuying, numberOfPriceAdjustments)) {
                log("Error modifying price");
                return false;
            }
        }

        if (isBuying) {
            Item coins = getInventory().getItem("Coins");
            if (coins == null) {
                log("No coins in inventory to purchase item " + itemName);
                return false;
            }
            if (coins.getAmount() < getGrandExchange().getOfferPrice()) {
                log(String.format("Insufficient coinage to purchase %s, you need %d coins (had %d)", itemName, getGrandExchange().getOfferPrice(), coins.getAmount()));
                return false;
            }
        }

        if (!getGrandExchange().confirm()) {
            log("Error pressing confirm for item(s) " + itemName);
            return false;
        }

        GrandExchange.Status statusToWaitFor = (isBuying ? GrandExchange.Status.FINISHED_BUY : GrandExchange.Status.FINISHED_SALE);
        Sleep.until(() -> getGrandExchange().getStatus(boxToUse) == statusToWaitFor,
                10000, 1000);

        if (getGrandExchange().getStatus(boxToUse) == statusToWaitFor) {
            if (getInventory().isFull() && !withdrawToBank) {
                log("Inventory was too full to collect bought item(s) " + itemName);
                return false;
            }
            if (getGrandExchange().collect(withdrawToBank)) {
                log(String.format("QuickExchange -- Successfully handled %dx %s at %dGP each.", amount, itemName, getGrandExchange().getItemPrice(boxToUse)));
                return true;
            }
        }

        return false;
    }

    private boolean modifyPricePct(boolean increase, int times) throws InterruptedException {
        RS2Widget widg = getWidgets().singleFilter(getGrandExchange().getInterfaceId(),
                increase ? increaseWidgetFilter : decreaseWidgetFilter);
        String message = increase ? "increase" : "decrease";
        if (widg == null) {
            log("Unable to locate 5% " + message + " filter");
            return false;
        }

        for (int i = 0; i < times; i++) {
            if (!widg.interact()) {
                log("Error interacting with 5% " + message + " widget");
                return false;
            }
            sleep(random(20, 75));
        }

        return true;
    }

    private GrandExchange.Box tryGetOpenBox() throws InterruptedException {
        if (!isOpen()) {
            log("Grand Exchange is not open");
            return null;
        }

        if (!hasOpenBox()) {
            if (getGrandExchange().isOfferScreenOpen()) {
                if (!getGrandExchange().goBack()) {
                    log("Error returning to main GE Screen");
                    return null;
                }
            }
            if (getGrandExchange().collect(true)) {
                sleep(1000);
                if (!hasOpenBox()) {
                    log("No open GrandExchange Boxes, and unable to collect any boxes");
                    return null;
                }
            }
        }

        return Arrays.stream(GrandExchange.Box.values()).filter(f -> getGrandExchange().getStatus(f) == GrandExchange.Status.EMPTY).findFirst().orElse(null);
    }
}