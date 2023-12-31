package script.strategy.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.GameItem;
import script.utils.Sleep;

import java.util.Arrays;

public class KaramjaStrategy implements TaskStrategy {

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

    private final String fishingAction;

    public KaramjaStrategy(String fishingAction) {
        this.fishingAction = fishingAction;
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
        return script.getNpcs().closest(npc -> npc != null && Arrays.stream(KaramjaStrategy.npcIds).anyMatch(id -> id == npc.getId()));
    }

    private void waitForDialogue(Script script) {
        Sleep.sleepUntil(()-> script.getDialogues().inDialogue(), 5000);
    }

    private void completeDialogue(Script script) throws InterruptedException {
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue("Yes please.");
        }
        waitForArrivalAtPosition(script, boatPositionKaramja);
    }

    private void crossPlank(Script script, Position position){
        script.log("Crossing plank");
        Entity plankForDeposit = script.getObjects().closest("Gangplank");
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            Sleep.sleepUntil(()-> position.equals(script.myPlayer().getPosition()), 5000);
        }
    }

    private void waitForArrivalAtPosition(Script script, Position position) {
        script.log("Wait for arrival in Karamja");
        Sleep.sleepUntil(()-> script.myPlayer().getPosition() == position, 10000);
        script.log("Arrived in Karamja");
    }

    private void handleFullInventory(Script script) throws InterruptedException {
        script.log("Inventory full, walking to deposit box");
        leaveKaramja(script);
        if (!depositBoxArea.contains(script.myPlayer()) && !script.myPlayer().getPosition().equals(boatPositionPortSarim)){
            script.getWalking().webWalk(depositBoxArea);
            depositItems(script);
        }
    }

    public void leaveKaramja(Script script) throws InterruptedException {
        script.log("Leaving Karamja");
        if (!karamjaPortArea.contains(script.myPlayer())){
            script.getWalking().webWalk(karamjaPortArea);
        }
        if (karamjaPortArea.contains(script.myPlayer())){
            interactWithNpcForDeposit(script);
            waitForArrivalAtPosition(script, boatPositionPortSarim);
        }
        if (script.myPlayer().getPosition().equals(boatPositionPortSarim)){
            crossPlank(script, outsideBoatPositionPortSarim);
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

    private void depositItems(Script script) {
        Sleep.sleepUntil(()-> script.getDepositBox().open(), 5000);
        if (script.getDepositBox().isOpen()) {
            script.getDepositBox().depositAllExcept(GameItem.LOBSTER_POT.getId(), GameItem.COINS.getId());

            Sleep.sleepUntil(()-> script.getInventory().contains(GameItem.LOBSTER_POT.getId())
                    && script.getInventory().contains(GameItem.COINS.getId())
                    && script.getInventory().getEmptySlots() > 0, 5000);

            script.getDepositBox().close();

            Sleep.sleepUntil(()-> !script.getDepositBox().isOpen(), 5000);

            script.log("Deposit box closed, returning to fishing");
        } else {
            script.log("Failed to open deposit box");
        }
    }

    private void startFishing(Script script) {
        NPC fishingSpot = script.getNpcs().closest(n -> (n.hasAction(fishingAction)));
        if (fishingSpot != null && !script.myPlayer().isAnimating()) {
            if (fishingSpot.interact(fishingAction)) {
                waitForFishingAnimation(script);
            }
        }
    }

    private void waitForFishingAnimation(Script script) {
        Sleep.sleepUntil(()-> script.myPlayer().isAnimating(), 5000);
    }
}
