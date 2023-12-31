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

public class LobsterPotStrategy implements TaskStrategy {

    private static final int lobsterPotFishingSpotId = 1522;
    private static final int npcIdPortKaramja = 3648;
    private static final int[] npcIds = {3644, 3645, 3646};
    private static final Position boatPositionKaramja = new Position(2956,3143,1);
    private static final Position outsideBoatPositionKaramja = new Position(2956,3146,0);
    private static final Position boatPositionPortSarim = new Position(3032,3217,1);
    private static final Position outsideBoatPositionPortSarim= new Position(3029,3217,0);
    private static final Area karamjaPortArea = new Area(2950, 3144, 2961, 3152);
    private final Area fishingArea = new Area(2921, 3175, 2927, 3181);
    private final Area portSarimArea = new Area(3026, 3216, 3029, 3219);
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private final Area depositBoxArea = new Area(3043, 3234, 3046, 3237);

    public LobsterPotStrategy() {
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
        if (karamjaArea.contains(script.myPlayer()) && !script.myPlayer().getPosition().equals(boatPositionKaramja)) {
            script.log("Walking to fishing area");
            script.getWalking().webWalk(fishingArea);
        } else if (!script.myPlayer().getPosition().equals(boatPositionKaramja) && !portSarimArea.contains(script.myPlayer())) {
            script.log("Walking to Port Sarim");
            script.getWalking().webWalk(portSarimArea);
        }
        if (portSarimArea.contains(script.myPlayer())){
            handleDialoguePortSarim(script);
        }
        if (script.myPlayer().getPosition().equals(boatPositionKaramja)){
            crossPlank(script, outsideBoatPositionKaramja);
            script.getWalking().webWalk(fishingArea);
        }
    }

    private void handleDialoguePortSarim(Script script) throws InterruptedException {
        script.log("Handle dialogue Port Sarim");
        NPC closestNpc = getClosestNpc(script);

        if (closestNpc != null && closestNpc.interact("Pay-fare")) {
            waitForDialogue(script);
            completeDialogue(script);
        }
    }

    private NPC getClosestNpc(Script script) {
        return script.getNpcs().closest(npc -> npc != null && Arrays.stream(LobsterPotStrategy.npcIds).anyMatch(id -> id == npc.getId()));
    }

    private void waitForDialogue(Script script) {
        new ConditionalSleep(5000, 500) {
            @Override
            public boolean condition() {
                return script.getDialogues().inDialogue();
            }
        }.sleep();
    }

    private void completeDialogue(Script script) throws InterruptedException {
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue("Yes please.");
        }
        waitForArrivalInKaramja(script);
    }

    private void crossPlank(Script script, Position position){
        script.log("Crossing plank");
        Entity plankForDeposit = script.getObjects().closest("Gangplank");
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            new ConditionalSleep(5000, 500) {
                @Override
                public boolean condition() {
                    return position.equals(script.myPlayer().getPosition());
                }
            }.sleep();
        }
    }

    private void waitForArrivalInKaramja(Script script) {
        script.log("Wait for arrival in Karamja");
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {return script.myPlayer().getPosition() == boatPositionKaramja;}
        }.sleep();
        script.log("Arrived in Karamja");
    }

    private void handleFullInventory(Script script) throws InterruptedException {
        script.log("Inventory full, walking to deposit box");
        if (!karamjaPortArea.contains(script.myPlayer()) && karamjaArea.contains(script.myPlayer())){
            script.getWalking().webWalk(karamjaPortArea);
        }
        if (karamjaPortArea.contains(script.myPlayer())){
            interactWithNpcForDeposit(script);
            waitForArrivalInPortSarim(script);
        }
        if (script.myPlayer().getPosition().equals(boatPositionPortSarim)){
            crossPlank(script, outsideBoatPositionPortSarim);
        }
        if (!depositBoxArea.contains(script.myPlayer()) && !script.myPlayer().getPosition().equals(boatPositionPortSarim)){
            script.getWalking().webWalk(depositBoxArea);
            depositItems(script);
        }
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

    private void waitForArrivalInPortSarim(Script script) {
        script.log("Wait for arrival in Port Sarim");
        new ConditionalSleep(10000, 500) {
            @Override
            public boolean condition() {
                return boatPositionPortSarim.equals(script.myPlayer().getPosition());
            }
        }.sleep();
        script.log("Arrived in Port Sarim");
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
