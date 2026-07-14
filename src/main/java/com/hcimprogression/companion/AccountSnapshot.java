package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountSnapshot
{
    private String playerName;
    private int questPoints;
    private final Map<String, SkillSnapshot> skills = new LinkedHashMap<>();
    private List<String> completedQuests;
    private final Map<String, Integer> clueCounts = new LinkedHashMap<>();
    private final Map<String, Integer> collectionLog = new LinkedHashMap<>();
    private final List<CollectionLogItemSnapshot> collectionLogItems = new ArrayList<>();

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getQuestPoints() { return questPoints; }
    public void setQuestPoints(int questPoints) { this.questPoints = questPoints; }
    public Map<String, SkillSnapshot> getSkills() { return skills; }
    public List<String> getCompletedQuests() { return completedQuests; }
    public void setCompletedQuests(List<String> completedQuests) { this.completedQuests = completedQuests; }
    public Map<String, Integer> getClueCounts() { return clueCounts; }
    public Map<String, Integer> getCollectionLog() { return collectionLog; }
    public List<CollectionLogItemSnapshot> getCollectionLogItems() { return collectionLogItems; }

    public static class CollectionLogItemSnapshot
    {
        private final int itemId;
        private final String name;
        private final String category;
        private final int quantity;

        public CollectionLogItemSnapshot(int itemId, String name, String category, int quantity)
        {
            this.itemId = itemId;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
    }

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
