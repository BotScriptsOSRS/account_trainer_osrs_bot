package script.strategy.banking;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

import java.util.Set;

public class DepositAllBankingStrategy implements TaskStrategy {
    private final Set<Integer> itemsToKeep;
    private final int SLEEP_DURATION_MS = 2000;

    public DepositAllBankingStrategy(Set<Integer> itemsToKeep) {
        this.itemsToKeep = itemsToKeep;
    }

    private final Area[] BANKS = {
            Banks.LUMBRIDGE_UPPER,
            Banks.VARROCK_WEST,
            Banks.VARROCK_EAST,
            Banks.FALADOR_EAST,
            Banks.FALADOR_WEST,
            Banks.EDGEVILLE,
            Banks.GRAND_EXCHANGE,
            Banks.AL_KHARID,
            Banks.DRAYNOR
    };

    @Override
    public void execute(Script script) throws InterruptedException {
        if (!prepareForBanking(script)) {
            script.log("Failed to prepare for banking.");
            return;
        }

        performBankingActions(script);
    }

    private void performBankingActions(Script script) throws InterruptedException {
        depositAllExceptItemsToKeep(script);
        closeBank(script);
    }

    private boolean prepareForBanking(Script script) throws InterruptedException {
        if (!isAtBank(script)) {
            walkToNearestBank(script);
        }
        return openBankWithRetry(script);
    }

    private void walkToNearestBank(Script script) {
        script.log("Walking to the nearest F2P bank");
        script.getWalking().webWalk(BANKS);
    }

    private boolean openBankWithRetry(Script script) throws InterruptedException {
        int MAX_ATTEMPTS = 3;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (openBank(script)) {
                return true;
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
        return false;
    }

    private boolean openBank(Script script) throws InterruptedException {
        if (script.getBank().isOpen()) {
            return true;
        }
        return attemptToOpenBank(script);
    }

    private boolean isAtBank(Script script) {
        for (Area bank : BANKS) {
            if (bank.contains(script.myPlayer())) {
                return true;
            }
        }
        return false;
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

    private void depositAllExceptItemsToKeep(Script script) throws InterruptedException {
        script.log("Depositing all items except specified items to keep");
        for (Item item : script.getInventory().getItems()) {
            if (item != null && !itemsToKeep.contains(item.getId())) {
                script.getBank().depositAll(item.getId());
                new ConditionalSleep(SLEEP_DURATION_MS) {
                    @Override
                    public boolean condition() {
                        return !script.getInventory().contains(item.getId());
                    }
                }.sleep();
            }
        }
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        new ConditionalSleep(SLEEP_DURATION_MS) {
            @Override
            public boolean condition() {
                return !script.getBank().isOpen();
            }
        }.sleep();
    }
}
