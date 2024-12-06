package script.paint;

import org.osbot.rs07.api.ui.Skill;

public class SkillTracker {
    private final Skill skill;
    private int gainedXPPerHour;
    private int gainedLevels;
    private long timeToNextLevel;

    public SkillTracker(Skill skill) {
        this.skill = skill;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setGainedXPPerHour(int xpPerHour) {
        this.gainedXPPerHour = xpPerHour;
    }

    public int getGainedXPPerHour() {
        return gainedXPPerHour;
    }

    public void setGainedLevels(int levels) {
        this.gainedLevels = levels;
    }

    public int getGainedLevels() {
        return gainedLevels;
    }

    public void setTimeToNextLevel(long time) {
        this.timeToNextLevel = time;
    }

    public long getTimeToNextLevel() {
        return timeToNextLevel;
    }
}
