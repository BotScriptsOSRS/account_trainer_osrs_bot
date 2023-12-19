package script.paint;

import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;

public class SkillTracker {
    private Script script;
    private Skill skill;
    private long activeStartTime; // Start time of the current active period
    private long totalActiveTime; // Total active time for this skill
    private int startExp;
    private int startLevel;


    public SkillTracker(Script script, Skill skill) {
        this.script = script;
        this.skill = skill;
        this.activeStartTime = System.currentTimeMillis();
        this.totalActiveTime = 0;
        this.startExp = script.getSkills().getExperience(skill);
        this.startLevel = script.getSkills().getStatic(skill);
    }

    // Start tracking time for this skill
    public void startTracking() {
        this.activeStartTime = System.currentTimeMillis();
    }

    // Pause tracking time for this skill
    public void pauseTracking() {
        this.totalActiveTime += System.currentTimeMillis() - this.activeStartTime;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getGainedExp() {
        return script.getSkills().getExperience(skill) - startExp;
    }

    public int getGainedLevels() {
        return script.getSkills().getStatic(skill) - startLevel;
    }

    public int getExpPerHour() {
        long timeElapsed = totalActiveTime + (System.currentTimeMillis() - activeStartTime);
        if (timeElapsed == 0) return 0;
        return (int) ((getGainedExp() * 3600000D) / timeElapsed);
    }

    public String getTimeToNextLevel() {
        int expToNextLevel = script.getSkills().getExperienceForLevel(script.getSkills().getStatic(skill) + 1) - script.getSkills().getExperience(skill);
        if (getExpPerHour() == 0) return "N/A";
        long timeToNextLevel = (long) (expToNextLevel / (getExpPerHour() / 3600000D));
        return formatTime(timeToNextLevel);
    }

    // Get total active time in a formatted string
    public String getSkillRunTime() {
        long totalMillis = this.totalActiveTime + (System.currentTimeMillis() - this.activeStartTime);
        long hours = totalMillis / 3600000;
        long mins = (totalMillis % 3600000) / 60000;
        long secs = (totalMillis % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private String formatTime(long millis) {
        long hours = millis / 3600000;
        long mins = (millis % 3600000) / 60000;
        long secs = (millis % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }
}
