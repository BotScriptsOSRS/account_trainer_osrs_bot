package script.strategy.crafting;

import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import script.strategy.TaskStrategy;
import script.utils.GameItem;
import script.utils.Sleep;

import java.awt.*;

public class LeatherStrategy implements TaskStrategy {

    private static final int SLEEP_DURATION_MS = 5000;
    private static final int CRAFT_SLEEP_TIME_MS = 60000;

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
            Sleep.sleepUntil(() -> script.getInventory().isItemSelected(), SLEEP_DURATION_MS);
        }
    }

    private void interactWithLeatherIfItemSelected(Script script) {
        if (script.getInventory().isItemSelected()) {
            script.log("Trying to interact with leather");
            script.getInventory().getItem(GameItem.SOFT_LEATHER.getId()).interact();
            Sleep.sleepUntil(() -> isGloveWidgetWorking(script), SLEEP_DURATION_MS);
        }
    }

    private void useGloveWidgetIfAvailable(Script script) {
        if (isGloveWidgetWorking(script)) {
            script.log("Use widget");
            getGloveWidget(script).interact();
            Sleep.sleepUntil(() -> script.myPlayer().isAnimating(), SLEEP_DURATION_MS);
        }
    }

    private void waitToCraftGloves(Script script) {
        script.log("Waiting to make gloves");
        Sleep.sleepUntil(() -> !script.getInventory().contains(GameItem.SOFT_LEATHER.getId()) || isLevelUpWidgetWorking(script), CRAFT_SLEEP_TIME_MS);
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
