package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.util.Arrays;

public class LobsterPotFishingStrategy implements TaskStrategy {

    private static final int lobsterPotFishingSpotId = 1522;
    private static final int plankIdPortKaramja = 2082;
    private static final int plankIdPortSarim = 2084;
    private static final int npcIdPortKaramja = 3648;
    private static final int[] npcIds = {3644, 3645, 3646};
    private static final Position boatPositionKaramja = new Position(2956,3143,1);
    private static final Position boatPositionPortSarim = new Position(3032,3217,1);
    private static final Area karamjaPortArea = new Area(2950, 3144, 2961, 3152);
    private final Area fishingArea = new Area(2921, 3175, 2927, 3181);
    private final Area portSarimArea = new Area(3026, 3216, 3029, 3219);
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private final Area depositBoxArea = new Area(3043, 3234, 3046, 3237);

    public LobsterPotFishingStrategy() {
    }

    @Override
    public void execute(Script script) throws InterruptedException {
        if (!isInFishingArea(script) && !script.getInventory().isFull()) {
            walkToFishingArea(script);
        }

        if (script.getInventory().isFull()) {
            handleFullInventory(script);
        } else if (fishingArea.contains(script.myPlayer())) {
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
            script.getWalking().webWalk(fishingArea);
        }
    }

    private void handleDialoguePortSarim(Script script) throws InterruptedException {
        NPC closestNpc = getClosestNpc(script);

        if (closestNpc != null && closestNpc.interact("Pay-fare")) {
            waitForDialogue(script);
            completeDialogueAndCrossPlank(script);
        }
    }

    private NPC getClosestNpc(Script script) {
        return script.getNpcs().closest(npc -> npc != null && Arrays.stream(LobsterPotFishingStrategy.npcIds).anyMatch(id -> id == npc.getId()));
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
        }
        waitForArrivalInKaramja(script);
        Entity plank = script.getObjects().closest(plankIdPortKaramja);
        if (plank != null && plank.interact("Cross")) {
            new ConditionalSleep(5000, 500) {
                @Override
                public boolean condition() {
                    return karamjaPortArea.contains(script.myPlayer());
                }
            }.sleep();
        }
    }

    private void waitForArrivalInKaramja(Script script) {
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return boatPositionKaramja.equals(script.myPlayer().getPosition());
            }
        }.sleep();

        script.getWalking().webWalk(fishingArea);
    }

    private void handleFullInventory(Script script) throws InterruptedException {
        script.log("Inventory full, walking to deposit box");
        if (!karamjaPortArea.contains(script.myPlayer())){
            script.getWalking().webWalk(karamjaPortArea);
        }
        interactWithNpcForDeposit(script);
        crossPlankAndDepositItems(script);
    }

    private void interactWithNpcForDeposit(Script script) throws InterruptedException {
        if (!script.getDialogues().inDialogue()) {
            NPC npcForDeposit = script.getNpcs().closest(npcIdPortKaramja);
            if (npcForDeposit != null && npcForDeposit.interact("Pay-fare")) {
                waitForDialogue(script);
                completeDialogueForDeposit(script);
            }
        } else {
            completeDialogueForDeposit(script);
        }
    }
    private void completeDialogueForDeposit(Script script) throws InterruptedException {
        String[] dialogueOptions = {"Can I journey on this ship?",
                "Search away, I have nothing to hide.", "Ok."};
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue(dialogueOptions);
        }
    }

    private void crossPlankAndDepositItems(Script script) {
        waitForArrivalInPortSarim(script);
        Entity plankForDeposit = script.getObjects().closest(plankIdPortSarim);
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            new ConditionalSleep(5000, 500) {
                @Override
                public boolean condition() {
                    return portSarimArea.contains(script.myPlayer());
                }
            }.sleep();
            script.getWalking().webWalk(depositBoxArea);
            depositItems(script);
        }
    }

    private void waitForArrivalInPortSarim(Script script) {
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return boatPositionPortSarim.equals(script.myPlayer().getPosition());
            }
        }.sleep();
    }

    private void depositItems(Script script) {
        new ConditionalSleep(5000) {
            @Override
            public boolean condition() {
                return script.getDepositBox().open();
            }
        }.sleep();

        if (script.getDepositBox().isOpen()) {
            script.getDepositBox().depositAllExcept(GameItem.LOBSTER_POT.getId(), GameItem.COINS.getId());

            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return script.getInventory().contains(GameItem.LOBSTER_POT.getId()) && script.getInventory().contains(GameItem.COINS.getId()) && script.getInventory().getEmptySlots() > 0;
                }
            }.sleep();

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
        NPC fishingSpot = script.getNpcs().closest(lobsterPotFishingSpotId);
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
