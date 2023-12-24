package script.state;

import org.osbot.rs07.api.ui.Skill;
import script.MainScript;
import script.strategy.banking.DepositAllBankingStrategy;
import script.strategy.banking.SwitchStateBankingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.woodcutting.OakWoodcuttingStrategy;
import script.strategy.woodcutting.TreeWoodcuttingStrategy;
import script.strategy.woodcutting.YewWoodcuttingStrategy;
import script.utils.GameItem;

import java.util.*;

public class WoodcuttingState implements BotState {
    private TaskStrategy strategy;
    private long switchTime;

    public WoodcuttingState(MainScript script) {
        updateStrategy(script);
    }

    @Override
    public void execute(MainScript script) throws InterruptedException {
        if (!checkWoodcuttingEquipment(script)) {
            return;
        }
        updateStrategy(script);
        strategy.execute(script);
    }

    @Override
    public void enterState(MainScript script) {
        script.log("Entering woodcutting state");
        this.switchTime = System.currentTimeMillis() + (long) (3600000 + Math.random() * 3600000); // 1 to 2 hours
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

    private int getBestAxeId(int woodcuttingLevel) {
        if (woodcuttingLevel >= 41) return GameItem.RUNE_AXE.getId();
        else if (woodcuttingLevel >= 31) return GameItem.ADAMANT_AXE.getId();
        else if (woodcuttingLevel >= 21) return GameItem.MITHRIL_AXE.getId();
        else if (woodcuttingLevel >= 11) return GameItem.BLACK_AXE.getId();
        else if (woodcuttingLevel >= 6) return GameItem.STEEL_AXE.getId();
        else return GameItem.BRONZE_AXE.getId();
    }

    private boolean checkWoodcuttingEquipment(MainScript script) {
        int woodcuttingLevel = script.getSkills().getStatic(Skill.WOODCUTTING);
        List<Integer> axesToBuy = getAxesToBuy();
        int currentAxeId = getBestAxeId(woodcuttingLevel);

        if (!script.getInventory().contains(currentAxeId) && !script.getEquipment().contains(currentAxeId)) {
            switchToBankingStateForWoodcuttingEquipment(script, currentAxeId,axesToBuy);
            return false;
        }
        return true;
    }

    private List<Integer> getAxesToBuy() {
        return new ArrayList<>(Arrays.asList(GameItem.BRONZE_AXE.getId(), GameItem.STEEL_AXE.getId(), GameItem.BLACK_AXE.getId(), GameItem.MITHRIL_AXE.getId(), GameItem.ADAMANT_AXE.getId(), GameItem.RUNE_AXE.getId()));
    }

    private void switchToBankingStateForWoodcuttingEquipment(MainScript script, int currentAxeId, List<Integer> futureAxeIds) {
        script.log("Switching to banking state for woodcutting equipment");

        Map<Integer, Integer> requiredItemsForWoodcutting = new HashMap<>();
        Map<Integer, Integer> futureAxeIdsMap = new HashMap<>();
        // Add the current axe needed for woodcutting
        requiredItemsForWoodcutting.put(currentAxeId, 1);

        // Check for future axes in the bank and add them to the required items if missing
        for (Integer futureAxeId : futureAxeIds) {
            if (!script.getBank().contains(futureAxeId)) {
                futureAxeIdsMap.put(futureAxeId, 1);
            }
        }

        // Switch to banking state with the strategy to handle the required items
        BankingState returnBankingState = new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForWoodcutting, futureAxeIdsMap, this), this);
        script.setCurrentState(new BankingState(script, new SwitchStateBankingStrategy(requiredItemsForWoodcutting, futureAxeIdsMap, returnBankingState), this));
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
