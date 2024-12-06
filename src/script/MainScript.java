package script;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import script.paint.OSDPainter;
import script.state.*;
import script.strategy.banking.SwitchStateBankingStrategy;
import script.strategy.fishing.KaramjaStrategy;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalTime;
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
    private boolean tooManyCoins = false;
    LocalTime currentTime = LocalTime.now();
    LocalTime startTime = LocalTime.of(21, 0);
    LocalTime endTime = LocalTime.of(21, 30);

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
        } else if (shouldLeaveKaramja() && tooManyCoins && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)){
            karamjaStrategy.leaveKaramja(this);
            setBankingState();
        } else if (tooManyCoins && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)){
            setBankingState();
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

    public void setTooManyCoins(boolean tooManyCoins) {
        this.tooManyCoins = tooManyCoins;
    }
    public void setBankingState() {
        Map<Integer, Integer> defaultRequiredBankItems = new HashMap<>();
        Map<Integer, Integer> defaultItemsToBuyInAdvance = new HashMap<>();

        SwitchStateBankingStrategy bankingStrategy = new SwitchStateBankingStrategy(
                defaultRequiredBankItems, defaultItemsToBuyInAdvance, this.currentState
        );
        BankingState bankingState = new BankingState(this, bankingStrategy, this.currentState);
        this.setCurrentState(bankingState);
    }

}
