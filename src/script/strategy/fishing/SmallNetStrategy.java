package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.Sleep;

public class SmallNetStrategy implements TaskStrategy {

    private final Area fishingArea = new Area(3237, 3139, 3249, 3162);

    public SmallNetStrategy() {
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
        NPC fishingSpot = script.getNpcs().closest(n -> (n.hasAction("Net")));
        if (fishingSpot != null && !script.myPlayer().isAnimating()) {
            if (fishingSpot.interact("Net")) {
                waitForFishingAnimation(script);
            }
        }
    }

    private void waitForFishingAnimation(Script script) {
        Sleep.sleepUntil(() -> script.myPlayer().isAnimating(), 5000);
    }
}
