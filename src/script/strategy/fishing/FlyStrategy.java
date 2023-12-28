package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

public class FlyStrategy implements TaskStrategy {

    private static final int FLY_FISHING_SPOT_ID = 1526;
    private final Area fishingArea = new Area(3100, 3423, 3110, 3436);

    public FlyStrategy() {
    }

    @Override
    public void execute(Script script) {
        if (!isInFishingArea(script)) {
            walkToFishingArea(script);
        }

        if (script.getInventory().isFull()) {
            handleFullInventory(script);
        } else {
            startFishing(script);
        }
    }

    private boolean isInFishingArea(Script script) {
        return fishingArea.contains(script.myPlayer());
    }

    private void walkToFishingArea(Script script) {
        script.log("Walking to fly fishing area");
        script.getWalking().webWalk(fishingArea);
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, dropping fish");
        script.getInventory().dropAll("Raw trout", "Raw salmon");
    }

    private void startFishing(Script script) {
        NPC fishingSpot = script.getNpcs().closest(FLY_FISHING_SPOT_ID);
        if (fishingSpot != null && !script.myPlayer().isAnimating()) {
            if (fishingSpot.interact("Lure")) { // Assuming "Lure" is the correct action for fly fishing
                waitForFishingAnimation(script);
            }
        }
    }

    private void waitForFishingAnimation(Script script) {
        new ConditionalSleep(5000, 1000) {
            @Override
            public boolean condition() {
                return script.myPlayer().isAnimating();
            }
        }.sleep();
    }
}
