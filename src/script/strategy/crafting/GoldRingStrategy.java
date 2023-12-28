package script.strategy.crafting;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.awt.*;
import java.util.function.Supplier;

public class GoldRingStrategy implements TaskStrategy {

    private static final Area FURNACE_AREA = new Area(3110, 3497, 3106, 3500);
    private static final int GOLD_RING_WIDGET_X = 51;
    private static final int GOLD_RING_WIDGET_Y = 87;
    private static final int SMELT_SLEEP_TIME_MS = 5000;
    private static final int CRAFT_SLEEP_TIME_MS = 120000;

    @Override
    public void execute(Script script) throws InterruptedException {
        if (script.getInventory().contains(GameItem.GOLD_BAR.getId()) && script.getInventory().contains(GameItem.RING_MOULD.getId())) {
            moveToAreaIfNeeded(script);
            smeltGoldRing(script);
            craftGoldRings(script);
        }
    }

    private void moveToAreaIfNeeded(Script script) {
        if (!FURNACE_AREA.contains(script.myPlayer())) {
            script.getWalking().webWalk(FURNACE_AREA);
        }
    }

    private void smeltGoldRing(Script script) {
        Entity furnace = script.getObjects().closest("Furnace");
        if (furnace != null && furnace.interact("Smelt") && !script.myPlayer().isAnimating()) {
            sleepUntil(() -> isGoldRingWidgetWorking(script), SMELT_SLEEP_TIME_MS);
        }
    }

    private void craftGoldRings(Script script) {
        if (getGoldRingWidget(script).interact()) {
            sleepUntil(() -> !script.getInventory().contains(GameItem.GOLD_BAR.getId()) || isLevelUpWidgetWorking(script), CRAFT_SLEEP_TIME_MS);
        }
    }

    private RS2Widget getGoldRingWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.getPosition().equals(new Point(GOLD_RING_WIDGET_X, GOLD_RING_WIDGET_Y))
        );
    }

    private boolean isGoldRingWidgetWorking(Script script) {
        RS2Widget goldRingWidget = getGoldRingWidget(script);
        return goldRingWidget != null && goldRingWidget.isVisible();
    }

    private boolean isLevelUpWidgetWorking(Script script) {
        RS2Widget levelUpWidget = getLevelUpWidget(script);
        return levelUpWidget != null && levelUpWidget.isVisible();
    }

    private RS2Widget getLevelUpWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.getMessage().contains("Click here to continue")
        );
    }

    private void sleepUntil(Supplier<Boolean> condition, int duration) {
        new ConditionalSleep(duration, 1000) {
            @Override
            public boolean condition() {
                return condition.get();
            }
        }.sleep();
    }
}