package script.strategy.banking;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.BankingUtils;
import script.utils.Sleep;

import java.util.Set;

public class DepositAllBankingStrategy implements TaskStrategy {
    private final Set<Integer> itemsToKeep;
    private final int SLEEP_DURATION_MS = 5000;

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

    private void performBankingActions(Script script) {
        depositAllExceptItemsToKeep(script);
        closeBank(script);
    }

    private boolean prepareForBanking(Script script) throws InterruptedException {
        if (!isAtBank(script)) {
            walkToNearestBank(script);
        }
        return BankingUtils.openBankWithRetry(script);
    }

    private void walkToNearestBank(Script script) {
        script.log("Walking to the nearest F2P bank");
        script.getWalking().webWalk(BANKS);
    }

    private boolean isAtBank(Script script) {
        for (Area bank : BANKS) {
            if (bank.contains(script.myPlayer())) {
                return true;
            }
        }
        return false;
    }

    private void depositAllExceptItemsToKeep(Script script) {
        script.log("Depositing all items except specified items to keep");
        for (Item item : script.getInventory().getItems()) {
            if (item != null && !itemsToKeep.contains(item.getId())) {
                script.getBank().depositAll(item.getId());
                Sleep.sleepUntil(()-> !script.getInventory().contains(item.getId()), SLEEP_DURATION_MS);
            }
        }
    }

    private void closeBank(Script script) {
        script.log("Closing the bank");
        script.getBank().close();
        Sleep.sleepUntil(()-> !script.getBank().isOpen(), SLEEP_DURATION_MS);
    }
}
