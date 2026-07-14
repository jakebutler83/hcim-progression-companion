package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountSnapshot
{
    private String playerName;
    private int questPoints;
    private final Map<String, SkillSnapshot> skills = new LinkedHashMap<>();
    private List<String> completedQuests;

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getQuestPoints() { return questPoints; }
    public void setQuestPoints(int questPoints) { this.questPoints = questPoints; }
    public Map<String, SkillSnapshot> getSkills() { return skills; }
    public List<String> getCompletedQuests() { return completedQuests; }
    public void setCompletedQuests(List<String> completedQuests) { this.completedQuests = completedQuests; }

    public static class SkillSnapshot
    {
        private final int level;
        private final int xp;

        public SkillSnapshot(int level, int xp)
        {
            this.level = level;
            this.xp = xp;
        }

        public int getLevel() { return level; }
        public int getXp() { return xp; }
    }
}
