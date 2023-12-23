package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.DepositAllBankingStrategy;
import script.strategy.banking.SwitchStateOrEquipmentBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.woodcutting.OakWoodcuttingStrategy;
import script.strategy.woodcutting.TreeWoodcuttingStrategy;
import script.strategy.woodcutting.YewWoodcuttingStrategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WoodcuttingState implements BotState {
    private TaskStrategy strategy;
    private final long switchTime;

    // Axe item IDs
    private static final int BRONZE_AXE_ID = 1351;
    private static final int STEEL_AXE_ID = 1353;
    private static final int BLACK_AXE_ID = 1361;
    private static final int MITHRIL_AXE_ID = 1355;
    private static final int ADAMANT_AXE_ID = 1357;
    private static final int RUNE_AXE_ID = 1359;

    public WoodcuttingState(MainScript script) {
        updateStrategy(script);
        long startTime = System.currentTimeMillis();
        this.switchTime = startTime + (long) (3600000 / 6 + Math.random() * 3600000 / 6);
        script.log("Entering woodcutting state");
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkWoodcuttingEquipment(script)) {
            return;
        }
        updateStrategy(script);
        strategy.execute(script);
    }

    private void updateStrategy(MainScript script) {
        int woodcuttingLevel = script.getSkills().getStatic(Skill.WOODCUTTING);
        int bestAxeId = getBestAxeId(woodcuttingLevel);

        if (woodcuttingLevel < 15) {
            this.strategy = new TreeWoodcuttingStrategy(bestAxeId);
        } else if (woodcuttingLevel < 60) {
            this.strategy = new OakWoodcuttingStrategy(script, bestAxeId, this);
        } else {
            this.strategy = new YewWoodcuttingStrategy(script, bestAxeId, this);
        }
    }

    private boolean checkWoodcuttingEquipment(MainScript script) {
        int woodcuttingLevel = script.getSkills().getStatic(Skill.WOODCUTTING);
        int bestAxeId = getBestAxeId(woodcuttingLevel);
        if (!script.getInventory().contains(bestAxeId) && !script.getEquipment().contains(bestAxeId)) {
            switchToBankingStateForWoodcuttingEquipment(script, bestAxeId);
            return false;
        }
        return true;
    }

    private int getBestAxeId(int woodcuttingLevel) {
        if (woodcuttingLevel >= 41) return RUNE_AXE_ID;
        else if (woodcuttingLevel >= 31) return ADAMANT_AXE_ID;
        else if (woodcuttingLevel >= 21) return MITHRIL_AXE_ID;
        else if (woodcuttingLevel >= 11) return BLACK_AXE_ID;
        else if (woodcuttingLevel >= 6) return STEEL_AXE_ID;
        else return BRONZE_AXE_ID;
    }

    private void switchToBankingStateForWoodcuttingEquipment(MainScript script, int axeId) {
        script.log("Switching to banking state for woodcutting equipment");
        Map<Integer, Integer> requiredItemsForWoodcutting = new HashMap<>();
        requiredItemsForWoodcutting.put(axeId, 1);
        script.setCurrentState(new BankingState(script, new SwitchStateOrEquipmentBankingStrategy(requiredItemsForWoodcutting), this));
    }

    public void switchToBankingState(MainScript script) {
        script.log("Switching to banking state");
        Set<Integer> itemsToKeep = new HashSet<>(); // Add items to keep if any
        itemsToKeep.add(getBestAxeId(script.getSkills().getStatic(Skill.WOODCUTTING)));
        BankingState bankingState = new BankingState(script, new DepositAllBankingStrategy(itemsToKeep), this);
        script.setCurrentState(bankingState);
    }

    @Override
    public BotState nextState(MainScript script) {
        if (shouldSwitchToAnotherState()) {
            return script.pickRandomState(this);
        }
        return this;
    }

    private boolean shouldSwitchToAnotherState() {
        return System.currentTimeMillis() > switchTime;
    }
}
