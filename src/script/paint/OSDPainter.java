package script.paint;

import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class OSDPainter {
    private final Script script;
    private final Map<Skill, SkillTracker> skillTrackers;
    private final Map<Skill, Integer> previousExp;
    private final long scriptStartTime;

    public OSDPainter(Script script) {
        this.script = script;
        this.skillTrackers = new HashMap<>();
        this.previousExp = new HashMap<>();
        this.scriptStartTime = System.currentTimeMillis();
    }

    public void checkForNewSkills() {
        for (Skill skill : Skill.values()) {
            int currentExp = script.getSkills().getExperience(skill);
            if (!previousExp.containsKey(skill)) {
                // Initialize the previous experience for all skills to 0 or current experience
                    previousExp.put(skill, currentExp);
            } else if (currentExp > previousExp.get(skill)) {
                // Add a tracker only if the skill's experience has increased from its previous value
                skillTrackers.putIfAbsent(skill, new SkillTracker(script, skill));
                previousExp.put(skill, currentExp);
            }
        }
    }

    public void onPaint(Graphics2D g) {
        // Draw background and header
        drawBackground(g);
        drawHeader(g);

        // Display skill trackers
        int y = 70; // Adjust starting Y position as needed
        for (SkillTracker tracker : getDisplayedTrackers()) {
            drawTracker(g, tracker, y);
            y += 60; // Increase spacing to prevent overlap
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180)); // Dark, semi-transparent background
        g.fillRect(5, 5, 250, 500); // Slightly wider background to fit more content
    }

    private void drawHeader(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 215, 0)); // Gold color for header
        g.drawString("Account Trainer V1", 10, 30);
        g.drawString("Run Time: " + formatTime(System.currentTimeMillis() - scriptStartTime), 10, 55);
    }

    private void drawTracker(Graphics2D g, SkillTracker tracker, int y) {
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.WHITE);

        // Skill name, level, and levels gained
        g.drawString(tracker.getSkill().name() + " Lvl: " + script.getSkills().getStatic(tracker.getSkill()) +
                " (+" + tracker.getGainedLevels() + ")", 10, y);

        // Experience per hour
        g.drawString("Exp/Hr: " + tracker.getExpPerHour(), 10, y + 15);

        // Time to next level
        g.drawString("Time to Next Lvl: " + tracker.getTimeToNextLevel(), 10, y + 30);

    }

    private List<SkillTracker> getDisplayedTrackers() {
        return new ArrayList<>(skillTrackers.values());
    }

    private String formatTime(long time) {
        long s = time / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

}