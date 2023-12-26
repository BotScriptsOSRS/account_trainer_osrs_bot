package script;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;
import script.paint.OSDPainter;
import script.state.BotState;
import script.state.FishingState;
import script.state.WoodcuttingState;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@ScriptManifest(name = "AccountTrainer", author = "Boyd", version = 1.0, info = "", logo = "")
public class MainScript extends Script {

    private static final int npcId = 3648;
    private static final int plankId = 2084;
    private static final Position boatPositionPortSarim = new Position(3032,3217,1);
    private final Area portSarimArea = new Area(3026, 3216, 3029, 3219);
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private static final Area karamjaPortArea = new Area(2950, 3144, 2961, 3152);
    private BotState currentState;
    private boolean stateChanged;
    private OSDPainter osdPainter;
    private Map<Class<? extends BotState>, BotState> stateMap;

    @Override
    public void onStart() {
        osdPainter = new OSDPainter(this);
        stateMap = new HashMap<>();

        // Register states
        registerState(FishingState.class);
        registerState(WoodcuttingState.class);
        // Add other states here

        currentState = pickRandomState(null);
    }

    private void registerState(Class<? extends BotState> stateClass) {
        try {
            stateMap.put(stateClass, stateClass.getDeclaredConstructor(MainScript.class).newInstance(this));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onLoop() throws InterruptedException {
        currentState.execute(this);
        if (!stateChanged) {
            currentState = currentState.nextState(this);
        }
        osdPainter.checkForNewSkills();
        stateChanged = false;
        return random(200, 300);
    }

    public void setCurrentState(BotState newState) {
        this.currentState = newState;
        this.stateChanged = true;
    }

    public BotState pickRandomState(BotState excludeState) {
        Random random = new Random();
        List<Class<? extends BotState>> states = new ArrayList<>(stateMap.keySet());
        BotState newState;

        do {
            Class<? extends BotState> stateClass = states.get(random.nextInt(states.size()));
            newState = stateMap.get(stateClass);
        } while (newState.equals(excludeState));

        // If the new state is not FishingState and the character is on Karamja, leave Karamja
        if (!(newState instanceof FishingState) && karamjaArea.contains(this.myPlayer())) {
            try {
                leaveKaramja(this);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        newState.enterState(this);
        return newState;
    }

    @Override
    public void onPaint(Graphics2D g) {
        osdPainter.onPaint(g);
    }

    private void leaveKaramja(Script script) throws InterruptedException {
        script.log("Leaving Karamja before switching states");
        if (!karamjaPortArea.contains(script.myPlayer())){
            script.getWalking().webWalk(karamjaPortArea);
        }
        interactWithNpc(script);
        crossPlank(script);
    }

    private void interactWithNpc(Script script) throws InterruptedException {
        if (!script.getDialogues().inDialogue()) {
            NPC npcForDeposit = script.getNpcs().closest(npcId);
            if (npcForDeposit != null && npcForDeposit.interact("Pay-fare")) {
                waitForDialogue(script);
                completeDialogueForDeposit(script);
            }
        } else {
            completeDialogueForDeposit(script);
        }
    }

    private void waitForDialogue(Script script) {
        new ConditionalSleep(5000, 500) {
            @Override
            public boolean condition() {
                return script.getDialogues().inDialogue();
            }
        }.sleep();
    }

    private void completeDialogueForDeposit(Script script) throws InterruptedException {
        String[] dialogueOptions = {"Can I journey on this ship?",
                "Search away, I have nothing to hide.", "Ok."};
        if (script.getDialogues().isPendingContinuation()) {
            script.getDialogues().completeDialogue(dialogueOptions);
        }
    }

    private void crossPlank(Script script) {
        waitForArrivalInPortSarim(script);
        Entity plankForDeposit = script.getObjects().closest(plankId);
        if (plankForDeposit != null && plankForDeposit.interact("Cross")) {
            new ConditionalSleep(5000, 500) {
                @Override
                public boolean condition() {
                    return portSarimArea.contains(script.myPlayer());
                }
            }.sleep();
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
}
