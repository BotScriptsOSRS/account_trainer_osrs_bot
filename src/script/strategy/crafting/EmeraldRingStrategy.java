package script.strategy.crafting;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.utility.ConditionalSleep;
import script.strategy.TaskStrategy;
import script.utils.GameItem;

import java.awt.*;
import java.util.function.Supplier;

public class EmeraldRingStrategy implements TaskStrategy {

    private static final Area FURNACE_AREA = new Area(3110, 3497, 3106, 3500);
    private static final Position FURNACE_POSITION = new Position(3109,3499,0);
    private static final int EMERALD_RING_WIDGET_X = 141;
    private static final int EMERALD_RING_WIDGET_Y = 87;
    private static final int SMELT_SLEEP_TIME_MS = 5000;
    private static final int CRAFT_SLEEP_TIME_MS = 120000;

    @Override
    public void execute(Script script) throws InterruptedException {
        if (script.getInventory().contains(GameItem.GOLD_BAR.getId()) && script.getInventory().contains(GameItem.RING_MOULD.getId()) && script.getInventory().contains(GameItem.EMERALD.getId())) {
            moveToAreaIfNeeded(script);
            smeltEmeraldRing(script);
            craftEmeraldRings(script);
        }
    }

    private void moveToAreaIfNeeded(Script script) {
        if (!FURNACE_AREA.contains(script.myPlayer())) {
            script.getWalking().walk(FURNACE_POSITION);
        }
    }

    private void smeltEmeraldRing(Script script) {
        Entity furnace = script.getObjects().closest("Furnace");
        if (furnace != null && furnace.interact("Smelt") && !script.myPlayer().isAnimating()) {
            sleepUntil(() -> isEmeraldRingWidgetWorking(script), SMELT_SLEEP_TIME_MS);
        }
    }

    private void craftEmeraldRings(Script script) {
        if (getEmeraldRingWidget(script) != null && getEmeraldRingWidget(script).interact()) {
            sleepUntil(() -> !script.getInventory().contains(GameItem.GOLD_BAR.getId()) || isLevelUpWidgetWorking(script), CRAFT_SLEEP_TIME_MS);
        }
    }

    private RS2Widget getEmeraldRingWidget(Script script) {
        return script.getWidgets().singleFilter(
                script.getWidgets().getAll(),
                widget -> widget.isVisible() && widget.getItemId() == 1639 && widget.getPosition().equals(new Point(EMERALD_RING_WIDGET_X,EMERALD_RING_WIDGET_Y)));

    }

    private boolean isEmeraldRingWidgetWorking(Script script) {
        RS2Widget emeraldRingWidget = getEmeraldRingWidget(script);
        return emeraldRingWidget != null && emeraldRingWidget.isVisible();
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
