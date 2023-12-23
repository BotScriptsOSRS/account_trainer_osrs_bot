package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

public class LobsterPotFishingStrategy implements TaskStrategy {

    private final int lobsterPotId;
    private final int coinsId;
    private static final int LOBSTER_POT_FISHING_SPOT_ID = 1522;
    private final Area fishingArea = new Area(2922, 3174, 2927, 3181);
    private final Area depositBoxArea = new Area(3043, 3234, 3046, 3237);

    public LobsterPotFishingStrategy(int lobsterPotId, int coinsId) {
        this.lobsterPotId = lobsterPotId;
        this.coinsId = coinsId;
    }

    @Override
    public void execute(Script script) {
        if (!isInFishingArea(script)) {
            walkToFishArea(script);
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

    private void walkToFishArea(Script script) {
        script.log("Walking to lobster pot fishing area");
        script.getWalking().webWalk(fishingArea);
    }

    private void handleFullInventory(Script script) {
        script.log("Inventory full, walking to deposit box");
        script.getWalking().webWalk(depositBoxArea);
        depositItems(script);
    }

    private void depositItems(Script script) {
        new ConditionalSleep(5000) {
            @Override
            public boolean condition() {
                return script.getDepositBox().open();
            }
        }.sleep();

        if (script.getDepositBox().isOpen()) {
            script.getDepositBox().depositAllExcept(lobsterPotId, coinsId);

            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return script.getInventory().contains(lobsterPotId) && script.getInventory().contains(coinsId) && script.getInventory().getEmptySlots() > 0;
                }
            }.sleep();

            script.getDepositBox().close();

            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return !script.getDepositBox().isOpen();
                }
            }.sleep();
        } else {
            script.log("Could not open the deposit box");
        }
    }

    private void startFishing(Script script) {
        NPC fishingSpot = script.getNpcs().closest(LOBSTER_POT_FISHING_SPOT_ID);
        if (fishingSpot != null && !script.myPlayer().isAnimating()) {
            if (fishingSpot.interact("Cage")) {
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
