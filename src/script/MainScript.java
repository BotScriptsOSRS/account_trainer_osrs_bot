package script;

import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import script.paint.OSDPainter;
import script.state.BotState;
import script.state.FishingState;
import script.state.WoodcuttingState;
import script.strategy.FishingStrategy;
import script.strategy.TaskStrategy;
import script.strategy.WoodcuttingStrategy;
import script.util.TaskRegistry;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@ScriptManifest(name = "AccountTrainer", author = "Boyd", version = 1.0, info = "", logo = "")
public class MainScript extends Script {

    private BotState currentState;
    private boolean stateChanged;
    private OSDPainter osdPainter;
    private TaskRegistry taskRegistry;
    private Map<Class<? extends TaskStrategy>, Class<? extends BotState>> strategyStateMap;

    @Override
    public void onStart() {
        osdPainter = new OSDPainter(this);
        taskRegistry = new TaskRegistry();
        strategyStateMap = new HashMap<>();

        // Register strategies and corresponding states
        registerStrategyWithState(WoodcuttingStrategy.class, WoodcuttingState.class);
        registerStrategyWithState(FishingStrategy.class, FishingState.class);
        // Add other strategies and states here

        // Randomly pick initial state
        currentState = pickRandomState(null);
    }

    private void registerStrategyWithState(Class<? extends TaskStrategy> strategyClass,
                                           Class<? extends BotState> stateClass) {
        try {
            taskRegistry.registerTask(strategyClass.newInstance());
            strategyStateMap.put(strategyClass, stateClass);
        } catch (InstantiationException | IllegalAccessException e) {
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
        List<Class<? extends TaskStrategy>> strategies = new ArrayList<>(strategyStateMap.keySet());
        BotState newState;
        do {
            Class<? extends TaskStrategy> strategyClass = strategies.get(random.nextInt(strategies.size()));
            Class<? extends BotState> stateClass = strategyStateMap.get(strategyClass);
            newState = createState(strategyClass, stateClass);
        } while (newState.equals(excludeState));
        return newState;
    }

    private BotState createState(Class<? extends TaskStrategy> strategyClass, Class<? extends BotState> stateClass) {
        try {
            Constructor<? extends BotState> constructor = stateClass.getConstructor(MainScript.class, TaskStrategy.class);
            TaskStrategy strategy = taskRegistry.getTask(strategyClass);
            return constructor.newInstance(this, strategy);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        osdPainter.onPaint(g);
    }
}
