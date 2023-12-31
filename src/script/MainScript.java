package script;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import script.paint.OSDPainter;
import script.state.*;
import script.strategy.fishing.KaramjaStrategy;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@ScriptManifest(name = "AccountTrainer", author = "BotScriptsOSRS", version = 1.0, info = "", logo = "")
public class MainScript extends Script {

    private BotState currentState;
    private boolean stateChanged;
    private OSDPainter osdPainter;
    private Map<Class<? extends BotState>, BotState> stateMap;
    private final Area karamjaArea = new Area(2962, 3145, 2912, 3182);
    private KaramjaStrategy karamjaStrategy;

    @Override
    public void onStart() {
        osdPainter = new OSDPainter(this);
        stateMap = new HashMap<>();
        karamjaStrategy = new KaramjaStrategy("N/A");

        // Register states
        registerState(FishingState.class);
        registerState(WoodcuttingState.class);
        registerState(CraftingState.class);
        // Add other states here

        currentState = pickRandomState(null);
    }

    @Override
    public int onLoop() throws InterruptedException {
        if (shouldLeaveKaramja()) {
            karamjaStrategy.leaveKaramja(this);
        } else {
            currentState.execute(this);
            if (!stateChanged) {
                currentState = currentState.nextState(this);
            }
        }
        osdPainter.checkForNewSkills();
        stateChanged = false;
        return random(200, 300);
    }

    @Override
    public void onPaint(Graphics2D g) {
        osdPainter.onPaint(g);
    }

    @Override
    public void onExit(){
        log("Thank you for using Account Trainer");
        stop();
    }

    private void registerState(Class<? extends BotState> stateClass) {
        try {
            stateMap.put(stateClass, stateClass.getDeclaredConstructor(MainScript.class).newInstance(this));
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

        newState.enterState(this);
        return newState;
    }

    private boolean shouldLeaveKaramja() {
        return karamjaArea.contains(this.myPlayer()) && !(currentState instanceof FishingState);
    }
}
