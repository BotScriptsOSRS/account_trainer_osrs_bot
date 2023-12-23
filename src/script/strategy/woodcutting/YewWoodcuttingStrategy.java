package script.strategy.woodcutting;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.MainScript;
import script.state.WoodcuttingState;
import script.strategy.TaskStrategy;

public class YewWoodcuttingStrategy implements TaskStrategy {

    private final int bestAxeId;
    private final MainScript mainScript;
    private final Area woodcuttingArea = new Area(3085, 3482, 3089, 3468);
    private final WoodcuttingState woodcuttingState;

    public YewWoodcuttingStrategy(MainScript mainScript, int bestAxeId, WoodcuttingState woodcuttingState) {
        this.mainScript = mainScript;
        this.bestAxeId = bestAxeId;
        this.woodcuttingState = woodcuttingState;
    }
    @Override
    public void execute(Script script) {
        if (!isInWoodcuttingArea(script)) {
            walkToWoodcuttingArea(script);
        } else if (script.getInventory().isFull()) {
            handleFullInventory(script);
        } else {
            startWoodcutting(script);
        }
    }

    private boolean isInWoodcuttingArea(Script script) {
        return woodcuttingArea.contains(script.myPlayer());
    }

    private void walkToWoodcuttingArea(Script script) {
        script.log("Walking to woodcutting area");
        script.getWalking().webWalk(woodcuttingArea.getRandomPosition());
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, switching to banking state");
        woodcuttingState.switchToBankingState(mainScript);
    }
    private void startWoodcutting(Script script) {
        if (!hasAppropriateAxe(script)) {
            script.log("No appropriate axe found, unable to cut trees");
            return;
        }

        if (script.myPlayer().isAnimating()) {
            return;
        }

        Entity tree = script.getObjects().closest(woodcuttingArea, "Yew tree");
        if (tree != null && tree.interact("Chop down")) {
            script.log("Start woodcutting");
            waitForWoodcuttingToStart(script);
        } else {
            script.log("No Yew trees found in the area");
        }
    }

    private boolean hasAppropriateAxe(Script script) {
        return script.getInventory().contains(bestAxeId) || script.getEquipment().contains(bestAxeId);
    }

    private void waitForWoodcuttingToStart(Script script) {
        new ConditionalSleep(8000, 1500) {
            @Override
            public boolean condition() {
                return script.myPlayer().isAnimating();
            }
        }.sleep();
    }
}
