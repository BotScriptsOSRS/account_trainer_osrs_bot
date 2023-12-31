package script.strategy.muling;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.BankingUtils;
import script.utils.GameItem;
import script.utils.Sleep;

import java.util.concurrent.Callable;

public class MulingStrategy implements TaskStrategy {

    private final int SLEEP_DURATION_MS = 20000;
    private static final Area mulingArea = Banks.GRAND_EXCHANGE;
    private static final Area mulingWalkTo = new Area(3162, 3487, 3167, 3487);

    @Override
    public void execute(Script script) throws InterruptedException {
        handleCoinsInInventory(script);
        hopToWorld(script);
        walkToMulingArea(script);
        handleTrading(script);
        hopToRandomWorld(script);
    }

    private void hopToRandomWorld(Script script) {
        if (!script.getInventory().contains(GameItem.COINS.getId())
                && script.getWorlds().getCurrentWorld()==456
                && !script.getTrade().isFirstInterfaceOpen()
                && !script.getTrade().isSecondInterfaceOpen()){
            script.log("Hop to random world");
            waitForCondition(() -> script.getWorlds().hopToF2PWorld());
        }
    }

    private void handleCoinsInInventory(Script script) throws InterruptedException {
        if (!script.getInventory().contains(GameItem.COINS.getId())) {
            script.log("Withdrawing coins from bank");
            walkToMulingArea(script);
            checkBankForCoins(script);
        }
        if (script.getBank().isOpen()){
            closeBank(script);
        }
    }

    private void hopToWorld(Script script) {
        if (script.getWorlds().getCurrentWorld() != 456) {
            script.log("Hop to world 456");
            waitForCondition(() -> script.getWorlds().hop(456));
            waitForCondition(() -> script.getWorlds().getCurrentWorld() == 456);
        }
    }

    private void walkToMulingArea(Script script) {
        if (!mulingArea.contains(script.myPlayer())) {
            script.log("Walk to muling area");
            script.getWalking().webWalk(mulingWalkTo);
        }
    }

    private void handleTrading(Script script) throws InterruptedException {
        if (shouldInitiateTrade(script)) {
            initiateTrade(script);
        }
        if (shouldOfferCoins(script)) {
            offerCoins(script);
        }
        if (shouldAcceptFirstTradeInterface(script)) {
            MethodProvider.sleep(4000);
            script.log("Accept trade (first interface)");
            acceptTradeOne(script);
        }
        if (script.getTrade().isSecondInterfaceOpen()) {
            script.log("Accept trade (second interface)");
            acceptTradeTwo(script);
        }
    }

    private boolean shouldInitiateTrade(Script script) {
        return script.getPlayers().closest(script.getParameters()) != null && !script.getTrade().isCurrentlyTrading();
    }

    private void initiateTrade(Script script) {
        script.log("Initiating trade");
        waitForCondition(() -> script.getPlayers().closest(script.getParameters()).interact("Trade with"));
        waitForCondition(() -> script.getTrade().isFirstInterfaceOpen());
    }

    private boolean shouldOfferCoins(Script script) {
        return script.getTrade().isCurrentlyTrading() && !script.getTrade().getOurOffers().contains(GameItem.COINS.getId());
    }

    private void offerCoins(Script script) {
        script.log("Offer coins");
        waitForCondition(() -> script.getTrade().offerAll(GameItem.COINS.getId()));
        waitForCondition(() -> script.getTrade().getOurOffers().contains(GameItem.COINS.getId()));
    }

    private boolean shouldAcceptFirstTradeInterface(Script script) {
        return script.getTrade().isFirstInterfaceOpen() && script.getTrade().getOurOffers().contains(GameItem.COINS.getId());
    }

    private void acceptTradeOne(Script script) {
        script.getTrade().acceptTrade();
        waitForCondition(() -> script.getTrade().isSecondInterfaceOpen());
    }
    private void acceptTradeTwo(Script script) {
        script.getTrade().acceptTrade();
        waitForCondition(() -> !script.getTrade().isSecondInterfaceOpen());
    }

    private void waitForCondition(Callable<Boolean> condition) {
        Sleep.sleepUntil(()-> {
            try {
                return condition.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, SLEEP_DURATION_MS);
    }

    private void checkBankForCoins(Script script) throws InterruptedException {
        BankingUtils.openBankWithRetry(script);
        if (withdrawCoins(script)){
            closeBank(script);
        }
        if (script.getBank().isOpen()){
            closeBank(script);
        }
    }

    private boolean withdrawCoins(Script script) {
        script.log("Withdrawing coins");
        if (script.getBank().contains(GameItem.COINS.getId())){
            long amountOfCoinsInBank = script.getBank().getAmount(GameItem.COINS.getId());
            long amountToWithdraw = amountOfCoinsInBank - 100000;
            script.getBank().withdraw(GameItem.COINS.getId(), (int) amountToWithdraw);
            waitForItemInInventory(script, GameItem.COINS.getName());
            return true;
        }
        return false;
    }

    private void waitForItemInInventory(Script script, String itemName) {
        Sleep.sleepUntil(()-> script.getInventory().contains(itemName), SLEEP_DURATION_MS);
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        waitForBankToClose(script);
    }

    private void waitForBankToClose(Script script) {
        Sleep.sleepUntil(()-> !script.getBank().isOpen(), SLEEP_DURATION_MS);
    }
}
