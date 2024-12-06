package script.strategy.woodcutting;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.Sleep;

public class TreeStrategy implements TaskStrategy {

    private final int bestAxeId;
    private final Area woodcuttingArea = new Area(3154, 3206, 3206, 3262);
    private final Position safePosition = new Position(3194, 3241, 0);

    public TreeStrategy(int bestAxeId) {
        this.bestAxeId = bestAxeId;
    }
    @Override
    public void execute(Script script) {
        if (isUnderAttack(script)) {
            moveToSafePosition(script);
        } else if (!isInWoodcuttingArea(script)) {
            walkToWoodcuttingArea(script);
        } else if (script.getInventory().isFull()) {
            handleFullInventory(script);
        } else {
            startWoodcutting(script);
        }
    }

    private boolean isUnderAttack(Script script) {
        return script.myPlayer().isUnderAttack();
    }

    private void moveToSafePosition(Script script) {
        script.log("Under attack, moving to safe position");
        script.getWalking().webWalk(safePosition);
    }

    private boolean isInWoodcuttingArea(Script script) {
        return woodcuttingArea.contains(script.myPlayer());
    }

    private void walkToWoodcuttingArea(Script script) {
        script.log("Walking to woodcutting area");
        script.getWalking().webWalk(woodcuttingArea);
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, dropping logs");
        script.getInventory().dropAll("Logs"); // Adjust the item name if needed
    }

    private void startWoodcutting(Script script) {
        if (!hasAppropriateAxe(script)) {
            script.log("No appropriate axe found, unable to cut trees");
            return;
        }

        if (script.myPlayer().isAnimating()) {
            return;
        }

        Entity tree = script.getObjects().closest(woodcuttingArea, "Tree");
        if (tree != null && tree.interact("Chop down")) {
            waitForWoodcuttingToStart(script);
        }
    }

    private boolean hasAppropriateAxe(Script script) {
        return script.getInventory().contains(bestAxeId) || script.getEquipment().contains(bestAxeId);
    }

    private void waitForWoodcuttingToStart(Script script) {
        Sleep.sleepUntil(()-> script.myPlayer().isAnimating(), 8000);
    }
}
