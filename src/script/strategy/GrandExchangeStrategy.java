//package script.strategy;
//
//import org.osbot.rs07.api.GrandExchange;
//import org.osbot.rs07.api.map.constants.Banks;
//import org.osbot.rs07.script.Script;
//import org.osbot.rs07.utility.ConditionalSleep;
//
//import java.util.Map;
//
//public class GrandExchangeStrategy implements TaskStrategy {
//
//    private Map<Integer, Integer> itemsToBuy; // Map of Item ID to Quantity
//
//    public GrandExchangeStrategy(Map<Integer, Integer> itemsToBuy) {
//        this.itemsToBuy = itemsToBuy;
//    }
//
//    @Override
//    public void execute(Script script) throws InterruptedException {
//        if (!isAtGrandExchange(script)) {
//            script.log("Walking to the Grand Exchange");
//            walkToGrandExchange(script);
//        }
//
//        for (Map.Entry<Integer, Integer> entry : itemsToBuy.entrySet()) {
//            int itemId = entry.getKey();
//            int quantity = entry.getValue();
//            buyItem(script, itemId, quantity);
//        }
//
//        // Optionally wait for completion of all buy orders
//        waitForBuyOrdersToComplete(script);
//    }
//
//    private boolean isAtGrandExchange(Script script) {
//        return Banks.GRAND_EXCHANGE.contains(script.myPosition());
//    }
//
//    private void walkToGrandExchange(Script script) throws InterruptedException {
//        script.getWalking().webWalk(Banks.GRAND_EXCHANGE);
//    }
//
//    private void buyItem(Script script, int itemId, int quantity) throws InterruptedException {
//        script.log("Initiating buy offer for item ID: " + itemId);
//
//        // Open the Grand Exchange
//        if (!script.getGrandExchange().isOpen()) {
//            script.getGrandExchange().openGE();
//        }
//
//        // Set up the buy offer
//        script.getGrandExchange().buyItem(itemId, "Item Name", quantity, /*pricePerItem*/);
//        boolean boughtSuccessfully = new ConditionalSleep(10000) {
//            @Override
//            public boolean condition() throws InterruptedException {
//                return script.getGrandExchange().getStatus(GrandExchange.Box.BOX_1) == GrandExchange.Status.FINISHED_BUY;
//            }
//        }.sleep();
//
//        if (boughtSuccessfully) {
//            script.log("Buy offer for item ID: " + itemId + " completed.");
//            collectItems(script);
//        } else {
//            script.log("Buy offer for item ID: " + itemId + " timed out or failed.");
//        }
//    }
//
//    private void collectItems(Script script) throws InterruptedException {
//        if (script.getGrandExchange().isOpen()) {
//            script.log("Collecting items from Grand Exchange.");
//            script.getGrandExchange().collect();
//        } else {
//            script.log("Grand Exchange is not open, cannot collect items.");
//        }
//    }
//
//
//    private void waitForBuyOrdersToComplete(Script script) throws InterruptedException {
//        // Logic to wait for all buy orders to complete
//        // This might involve checking the status of each Grand Exchange box
//    }
//}
