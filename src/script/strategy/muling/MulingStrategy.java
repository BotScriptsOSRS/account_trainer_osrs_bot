package script.strategy.muling;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.util.concurrent.Callable;

public class MulingStrategy implements TaskStrategy {

    private final int SLEEP_DURATION_MS = 5000;
    private static final Area mulingArea = new Area(2969, 3353, 2967, 3351).setPlane(1);
    private static final Position mulingPosition = new Position(2969,3351,1);

    @Override
    public void execute(Script script) throws InterruptedException {
        handleCoinsInInventory(script);
        hopToWorld(script);
        walkToMulingArea(script);
        handleTrading(script);
        hopToRandomWorld(script);
    }

    private void hopToRandomWorld(Script script) {
        if (!script.getInventory().contains(GameItem.COINS.getId()) && script.getWorlds().getCurrentWorld()==456){
            waitForCondition(() -> script.getWorlds().hopToF2PWorld());
        }
    }

    private void handleCoinsInInventory(Script script) throws InterruptedException {
        if (!script.getInventory().contains(GameItem.COINS.getId())) {
            walkToFaladorBank(script);
            checkBankForCoins(script);
        }
    }

    private void walkToFaladorBank(Script script) {
        if (!Banks.FALADOR_WEST.contains(script.myPlayer())) {
            script.getWalking().webWalk(Banks.FALADOR_WEST);
        }
    }

    private void hopToWorld(Script script) {
        if (script.getWorlds().getCurrentWorld() != 456) {
            waitForCondition(() -> script.getWorlds().hop(456));
        }
    }

    private void walkToMulingArea(Script script) {
        if (!mulingArea.contains(script.myPlayer())) {
            script.log("Try to walk to muling position");
            script.log(mulingPosition.getArea(2));
            script.getWalking().webWalk(mulingPosition);
        }
    }

    private void handleTrading(Script script) {
        if (shouldInitiateTrade(script)) {
            initiateTrade(script);
        }
        if (shouldOfferCoins(script)) {
            offerCoins(script);
        }
        if (shouldAcceptFirstTradeInterface(script)) {
            acceptTrade(script);
        }
        if (script.getTrade().isSecondInterfaceOpen()) {
            acceptTrade(script);
        }
    }

    private boolean shouldInitiateTrade(Script script) {
        return script.getPlayers().closest("Faegrotto13") != null && !script.getTrade().isCurrentlyTrading();
    }

    private void initiateTrade(Script script) {
        waitForCondition(() -> script.getPlayers().closest("Faegrotto13").interact("Trade with"));
        waitForCondition(() -> script.getTrade().didOtherAcceptTrade());
    }

    private boolean shouldOfferCoins(Script script) {
        return script.getTrade().isCurrentlyTrading() && !script.getTrade().getOurOffers().contains(GameItem.COINS.getId());
    }

    private void offerCoins(Script script) {
        waitForCondition(() -> script.getTrade().offerAll(GameItem.COINS.getId()));
    }

    private boolean shouldAcceptFirstTradeInterface(Script script) {
        return script.getTrade().isFirstInterfaceOpen() && script.getTrade().getOurOffers().contains(GameItem.COINS.getId());
    }

    private void acceptTrade(Script script) {
        waitForCondition(() -> script.getTrade().acceptTrade());
    }

    private void waitForCondition(Callable<Boolean> condition) {
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                try {
                    return condition.call();
                } catch (Exception e) {
                    return false;
                }
            }
        }.sleep();
    }

    private void checkBankForCoins(Script script) throws InterruptedException {
        openBankWithRetry(script);
        if (withdrawCoins(script)){
            closeBank(script);
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
        script.log("Withdrawing all coins");
        if (script.getBank().contains(GameItem.COINS.getId())){
            long amountOfCoinsInBank = script.getBank().getAmount(GameItem.COINS.getId());
            long amountToWithdraw = amountOfCoinsInBank - 450000;
            script.getBank().withdraw(GameItem.COINS.getId(), (int) amountToWithdraw);
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
