package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;

public class SocialPresenceSnapshot
{
    private String playerName;
    private int world;
    private int regionId;
    private String regionName;
    private int x;
    private int y;
    private int plane;
    private boolean exactLocationIncluded;
    private boolean inWilderness;
    private int combatLevel;
    private String activity;
    private long timestamp;
    private final Map<String, EquipmentItem> equipment = new LinkedHashMap<>();

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getWorld() { return world; }
    public void setWorld(int world) { this.world = world; }
    public int getRegionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getPlane() { return plane; }
    public void setPlane(int plane) { this.plane = plane; }
    public boolean isExactLocationIncluded() { return exactLocationIncluded; }
    public void setExactLocationIncluded(boolean exactLocationIncluded) { this.exactLocationIncluded = exactLocationIncluded; }
    public boolean isInWilderness() { return inWilderness; }
    public void setInWilderness(boolean inWilderness) { this.inWilderness = inWilderness; }
    public int getCombatLevel() { return combatLevel; }
    public void setCombatLevel(int combatLevel) { this.combatLevel = combatLevel; }
    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Map<String, EquipmentItem> getEquipment() { return equipment; }

    public static class EquipmentItem
    {
        private final int itemId;
        private final int rawItemId;
        private final String name;

        public EquipmentItem(int itemId, int rawItemId, String name)
        {
            this.itemId = itemId;
            this.rawItemId = rawItemId;
            this.name = name;
        }

        public int getItemId() { return itemId; }
        public int getRawItemId() { return rawItemId; }
        public String getName() { return name; }
    }
}
