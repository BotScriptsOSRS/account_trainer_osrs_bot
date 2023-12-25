package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;

import java.util.Arrays;

public class LobsterPotFishingStrategy implements TaskStrategy {

    private final int lobsterPotId;
    private final int coinsId;
    private static final int LOBSTER_POT_FISHING_SPOT_ID = 1522;
    private static final int SLEEP_MIN_MS = 8500;
    private static final int SLEEP_MAX_MS = 10000;
    private static final int PLANK_ID = 2082;
    private static final int NPC_ID_FOR_DEPOSIT = 3648;
    private static final int PLANK_ID_FOR_DEPOSIT = 2084;
    private static final int[] NPC_IDS = {3644, 3645, 3646};
    private static final Area FULL_INVENTORY_AREA = new Area(2950, 3144, 2961, 3152);
    private final Area fishingArea = new Area(2921, 3175, 2927, 3181);

    private final Area portSarimArea = new Area(3026, 3216, 3029, 3219);
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private final Area depositBoxArea = new Area(3043, 3234, 3046, 3237);

    public LobsterPotFishingStrategy(int lobsterPotId, int coinsId) {
        this.lobsterPotId = lobsterPotId;
        this.coinsId = coinsId;
    }

    @Override
    public void execute(Script script) throws InterruptedException {
        if (!isInFishingArea(script) && !script.getInventory().isFull()) {
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

    private void walkToFishingArea(Script script) throws InterruptedException {
        script.log("Walking to lobster pot fishing area");
        if (karamjaArea.contains(script.myPlayer())) {
            script.getWalking().webWalk(fishingArea);
        } else {
            script.getWalking().webWalk(portSarimArea);
            handleDialoguePortSarim(script);
        }
    }

    private void handleDialoguePortSarim(Script script) throws InterruptedException {
        script.log("Handling dialogue in Port Sarim");
        NPC closestNpc = getClosestNpc(script);

        if (closestNpc != null && closestNpc.interact("Pay-fare")) {
            waitForDialogue(script);
            completeDialogueAndCrossPlank(script);
        }
    }

    private NPC getClosestNpc(Script script) {
        return script.getNpcs().closest(npc -> npc != null && Arrays.stream(LobsterPotFishingStrategy.NPC_IDS).anyMatch(id -> id == npc.getId()));
    }

    private void waitForDialogue(Script script) {
        new ConditionalSleep(5000, 500) {
            @Override
            public boolean condition() {
                return script.getDialogues().inDialogue();
            }
        }.sleep();
    }

    private void completeDialogueAndCrossPlank(Script script) throws InterruptedException {
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue("Yes please.");
            MethodProvider.sleep(random());
            Entity plank = script.getObjects().closest(PLANK_ID);
            if (plank != null && plank.interact("Cross")) {
                waitForArrivalInKaramja(script);
            }
        }
    }

    private void waitForArrivalInKaramja(Script script) {
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return karamjaArea.contains(script.myPlayer());
            }
        }.sleep();

        script.getWalking().webWalk(fishingArea);
    }

    private int random() {
        return (int) (LobsterPotFishingStrategy.SLEEP_MIN_MS + Math.random() * (LobsterPotFishingStrategy.SLEEP_MAX_MS - LobsterPotFishingStrategy.SLEEP_MIN_MS + 1));
    }

    private void handleFullInventory(Script script) throws InterruptedException {
        script.log("Inventory full, walking to deposit box");
        if (!FULL_INVENTORY_AREA.contains(script.myPlayer())){
            script.getWalking().webWalk(FULL_INVENTORY_AREA);
        }
        interactWithNpcForDeposit(script);
        crossPlankAndDepositItems(script);
    }

    private void interactWithNpcForDeposit(Script script) throws InterruptedException {
        if (!script.getDialogues().inDialogue()) {
            NPC npcForDeposit = script.getNpcs().closest(NPC_ID_FOR_DEPOSIT);
            if (npcForDeposit != null && npcForDeposit.interact("Pay-fare")) {
                waitForDialogue(script);
                completeDialogueForDeposit(script);
            }
        } else {
            completeDialogueAndCrossPlank(script);
        }
    }
    private void completeDialogueForDeposit(Script script) throws InterruptedException {
        String[] dialogueOptions = {"Can I journey on this ship?",
                "Search away, I have nothing to hide.", "Ok."};
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue(dialogueOptions);
            MethodProvider.sleep(random());
        }
    }

    private void crossPlankAndDepositItems(Script script) {
        Entity plankForDeposit = script.getObjects().closest(PLANK_ID_FOR_DEPOSIT);
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            waitForArrivalInPortSarim(script);
            script.getWalking().webWalk(depositBoxArea);
            depositItems(script);
        }
    }

    private void waitForArrivalInPortSarim(Script script) {
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return portSarimArea.contains(script.myPlayer());
            }
        }.sleep();
    }

    private void depositItems(Script script) {
        script.log("Attempting to open deposit box");
        new ConditionalSleep(5000) {
            @Override
            public boolean condition() {
                return script.getDepositBox().open();
            }
        }.sleep();

        if (script.getDepositBox().isOpen()) {
            script.log("Deposit box is open, depositing items");
            script.getDepositBox().depositAllExcept(lobsterPotId, coinsId);

            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return script.getInventory().contains(lobsterPotId) && script.getInventory().contains(coinsId) && script.getInventory().getEmptySlots() > 0;
                }
            }.sleep();

            script.log("Items deposited, closing deposit box");
            script.getDepositBox().close();

            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return !script.getDepositBox().isOpen();
                }
            }.sleep();

            script.log("Deposit box closed, returning to fishing");
        } else {
            script.log("Failed to open deposit box");
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
