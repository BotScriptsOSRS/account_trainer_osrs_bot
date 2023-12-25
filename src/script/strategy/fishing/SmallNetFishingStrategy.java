package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

public class SmallNetFishingStrategy implements TaskStrategy {

    private static final int NET_FISHING_SPOT_ID = 1530;
    private final Area fishingArea = new Area(3237, 3139, 3249, 3162);

    public SmallNetFishingStrategy() {
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
        script.log("Walking to fishing area");
        script.getWalking().webWalk(fishingArea);
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, dropping fish");
        script.getInventory().dropAll("Raw shrimps", "Raw anchovies");
    }

    private void startFishing(Script script) {
        NPC fishingSpot = script.getNpcs().closest(NET_FISHING_SPOT_ID);
        if (fishingSpot != null && !script.myPlayer().isAnimating()) {
            if (fishingSpot.interact("Net")) {
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
