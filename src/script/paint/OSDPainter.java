package script.paint;

import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.util.ExperienceTracker;
import org.osbot.rs07.script.Script;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSDPainter {
    private final Script script;
    private final Map<Skill, SkillTracker> skillTrackers;
    private final Map<Skill, Integer> previousExp;
    private final long scriptStartTime;
    private final ExperienceTracker experienceTracker; // Instance variable for ExperienceTracker

    public OSDPainter(Script script) {
        this.script = script;
        this.skillTrackers = new HashMap<>();
        this.previousExp = new HashMap<>();
        this.scriptStartTime = System.currentTimeMillis();
        this.experienceTracker = script.getExperienceTracker(); // Initialize ExperienceTracker
        this.experienceTracker.startAll(); // Start tracking all skills
    }

    public void checkForNewSkills() {
        for (Skill skill : Skill.values()) {
            int currentExp = script.getSkills().getExperience(skill);
            previousExp.putIfAbsent(skill, 0); // Initialize with 0 if not present
            if (currentExp > previousExp.get(skill)) {
                skillTrackers.putIfAbsent(skill, new SkillTracker(skill));
                previousExp.put(skill, currentExp); // Update the experience
            }
        }
    }


    public void onPaint(Graphics2D g) {
        // Update skill trackers with the latest data from ExperienceTracker
        updateSkillTrackers();

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

    private void updateSkillTrackers() {
        for (SkillTracker tracker : skillTrackers.values()) {
            Skill skill = tracker.getSkill();
            tracker.setGainedXPPerHour(experienceTracker.getGainedXPPerHour(skill));
            tracker.setGainedLevels(experienceTracker.getGainedLevels(skill));
            tracker.setTimeToNextLevel(experienceTracker.getTimeToLevel(skill));
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

        // Retrieve the current level of the skill
        int currentLevel = script.getSkills().getStatic(tracker.getSkill());

        // Skill name, level, and levels gained
        g.drawString(tracker.getSkill().name() + " Lvl: " + currentLevel +
                " (+" + tracker.getGainedLevels() + ")", 10, y);

        // Experience per hour
        g.drawString("Exp/Hr: " + tracker.getGainedXPPerHour(), 10, y + 15);

        // Time to next level
        g.drawString("Time to Next Lvl: " + formatDuration(tracker.getTimeToNextLevel()), 10, y + 30);
    }

    private List<SkillTracker> getDisplayedTrackers() {
        List<SkillTracker> displayedTrackers = new ArrayList<>();
        for (SkillTracker tracker : skillTrackers.values()) {
            Skill skill = tracker.getSkill();
            if (experienceTracker.getGainedXP(skill) > 0) {
                displayedTrackers.add(tracker);
            }
        }
        return displayedTrackers;
    }

    private String formatTime(long time) {
        long s = time / 1000, m = s / 60, h = m / 60;
        s %= 60;
        m %= 60;
        h %= 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    private String formatDuration(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
