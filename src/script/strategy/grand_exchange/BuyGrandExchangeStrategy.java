package script.strategy.grand_exchange;

import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.QuickExchange;

import java.util.Map;

public class BuyGrandExchangeStrategy implements TaskStrategy {

    private final Map<String, Integer> requiredItems;

    public BuyGrandExchangeStrategy(Map<String, Integer> requiredItems) {
        this.requiredItems = requiredItems;
    }
    @Override
    public void execute(Script script) throws InterruptedException {
        if (!isInGrandExchangeArea(script)) {
            walkToGrandExchangeArea(script);
            return;
        }

        QuickExchange quickExchange = new QuickExchange(script);

        quickExchange.open();

        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            quickExchange.quickBuy(itemName, quantity, false);
            new ConditionalSleep(60000, 500) { // Sleep for up to 60 seconds, checking every 500 milliseconds
                @Override
                public boolean condition() {
                    return script.getInventory().contains(itemName);
                }
            }.sleep();
        }
    }

    private boolean isInGrandExchangeArea(Script script) {
        return Banks.GRAND_EXCHANGE.contains(script.myPlayer());
    }

    private void walkToGrandExchangeArea(Script script) {
        script.log("Walking to the Grand Exchange");
        script.getWalking().webWalk(Banks.GRAND_EXCHANGE);
    }

}
