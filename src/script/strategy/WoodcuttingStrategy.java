package script.strategy;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;

public class WoodcuttingStrategy implements TaskStrategy {

    private static final int BRONZE_AXE_ID = 1351;
    private final Area woodcuttingArea = new Area(3154, 3206, 3206, 3262);
    private final Position safePosition = new Position(3194, 3241, 0);

    @Override
    public void execute(Script script) throws InterruptedException {
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
        script.log("Starting woodcutting");
    }

    private boolean isInWoodcuttingArea(Script script) {
        return woodcuttingArea.contains(script.myPlayer());
    }

    private void walkToWoodcuttingArea(Script script) {
        script.log("Walking to woodcutting area");
        script.getWalking().webWalk(woodcuttingArea.getRandomPosition());
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, dropping logs");
        script.getInventory().dropAll("Logs"); // Adjust the item name if needed
    }

    private void startWoodcutting(Script script) {
        if (!hasBronzeAxe(script)) {
            script.log("No bronze axe found, unable to cut trees");
            return;
        }

        if (script.myPlayer().isAnimating()) {
            return;
        }

        Entity tree = script.getObjects().closest("Tree");
        if (tree != null && tree.interact("Chop down")) {
            script.log("Start woodcutting");
            waitForWoodcuttingToStart(script);
        }
    }


    private boolean hasBronzeAxe(Script script) {
        return script.getInventory().contains(BRONZE_AXE_ID) || script.getEquipment().contains(BRONZE_AXE_ID);
    }

    private void waitForWoodcuttingToStart(Script script) {
        new ConditionalSleep(8000, 1500) {
            @Override
            public boolean condition() throws InterruptedException {
                return script.myPlayer().isAnimating();
            }
        }.sleep();
    }
}
