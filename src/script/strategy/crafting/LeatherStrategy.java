package script.strategy.crafting;

import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.awt.*;
import java.util.function.Supplier;

public class LeatherStrategy implements TaskStrategy {

    private static final int SLEEP_DURATION_MS = 5000;
    private static final int CRAFT_SLEEP_TIME_MS = 60000;
    private static final int CRAFT_SLEEP_INTERVAL_MS = 1000;

    public LeatherStrategy() {
    }

    @Override
    public void execute(Script script) {
        if (hasRequiredItems(script)) {
            selectNeedleIfNotSelected(script);
            interactWithLeatherIfItemSelected(script);
            useGloveWidgetIfAvailable(script);
            waitToCraftGloves(script);
        }
    }

    private boolean hasRequiredItems(Script script) {
        return script.getInventory().contains(GameItem.SOFT_LEATHER.getId())
                && script.getInventory().contains(GameItem.NEEDLE.getId())
                && script.getInventory().contains(GameItem.THREAD.getId());
    }

    private void selectNeedleIfNotSelected(Script script) {
        if (!script.getInventory().isItemSelected()) {
            script.log("Select needle");
            script.getInventory().interact("Use", "Needle");
            sleepUntil(() -> script.getInventory().isItemSelected());
        }
    }

    private void interactWithLeatherIfItemSelected(Script script) {
        if (script.getInventory().isItemSelected()) {
            script.log("Trying to interact with leather");
            script.getInventory().getItem(GameItem.SOFT_LEATHER.getId()).interact();
            sleepUntil(() -> isGloveWidgetWorking(script));
        }
    }

    private void useGloveWidgetIfAvailable(Script script) {
        if (isGloveWidgetWorking(script)) {
            script.log("Use widget");
            getGloveWidget(script).interact();
            sleepUntil(() -> script.myPlayer().isAnimating());
        }
    }

    private void waitToCraftGloves(Script script) {
        script.log("Waiting to make gloves");
        sleepUntil(() -> !script.getInventory().contains(GameItem.SOFT_LEATHER.getId()) || isLevelUpWidgetWorking(script), CRAFT_SLEEP_TIME_MS, CRAFT_SLEEP_INTERVAL_MS);
    }

    private void sleepUntil(Supplier<Boolean> condition) {
        sleepUntil(condition, SLEEP_DURATION_MS, 500);
    }

    private void sleepUntil(Supplier<Boolean> condition, int duration, int interval) {
        new ConditionalSleep(duration, interval) {
            @Override
            public boolean condition() {
                return condition.get();
            }
        }.sleep();
    }

    private RS2Widget getGloveWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() &&
                        widget.getSpellName().contains("Leather gloves") &&
                        widget.getPosition().equals(new Point(11, 389))
        );
    }

    private boolean isGloveWidgetWorking(Script script) {
        RS2Widget gloveWidget = getGloveWidget(script);
        return gloveWidget != null && gloveWidget.isVisible();
    }

    private boolean isLevelUpWidgetWorking(Script script) {
        RS2Widget levelUpWidget = getLevelUpWidget(script);
        return levelUpWidget != null && levelUpWidget.isVisible();
    }

    private RS2Widget getLevelUpWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() &&
                        widget.getMessage().contains("Click here to continue")
        );
    }
}
