package script;

import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
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

    private BotState currentState;
    private boolean stateChanged;
    private OSDPainter osdPainter;
    private Map<Class<? extends BotState>, BotState> stateMap;


    @Override
    public void onStart() {
        osdPainter = new OSDPainter(this);
        stateMap = new HashMap<>();

        // Register states
        registerState(WoodcuttingState.class);
        registerState(FishingState.class);
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
        return newState;
    }

    @Override
    public void onPaint(Graphics2D g) {
        osdPainter.onPaint(g);
    }
}
