package script;

import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import script.paint.OSDPainter;
import script.state.BotState;
import script.state.FishingState;
import script.state.WoodcuttingState;
import script.strategy.FishingStrategy;
import script.strategy.WoodcuttingStrategy;
import script.util.TaskRegistry;

import java.awt.*;
import java.util.Random;

@ScriptManifest(name = "AccountTrainer", author = "Boyd", version = 1.0, info = "", logo = "")
public class MainScript extends Script {

    private BotState currentState;
    private boolean stateChanged;
    private TaskRegistry taskRegistry;
    private OSDPainter osdPainter;

    @Override
    public void onStart() throws InterruptedException {
        osdPainter = new OSDPainter(this);
        taskRegistry = new TaskRegistry();

        // Register tasks
        taskRegistry.registerTask(new WoodcuttingStrategy());
        taskRegistry.registerTask(new FishingStrategy());

        // Randomly pick initial state
        if (new Random().nextBoolean()) {
            currentState = new WoodcuttingState(this, taskRegistry.getTask(WoodcuttingStrategy.class));
        } else {
            currentState = new FishingState(this, taskRegistry.getTask(FishingStrategy.class));
        }
        osdPainter.startSkillTracker(Skill.WOODCUTTING);
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

    public void setCurrentState(BotState newState, Skill newSkill) {
        // Pause all skill trackers
        osdPainter.pauseAllSkillTrackers();
        // Start tracking the new skill
        osdPainter.startSkillTracker(newSkill);

        this.currentState = newState;
        this.stateChanged = true;
    }


    @Override
    public void onPaint(Graphics2D g) {
        osdPainter.onPaint(g);
    }
}
