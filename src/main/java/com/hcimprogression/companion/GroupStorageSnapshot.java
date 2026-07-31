package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

public class GroupStorageSnapshot
{
    private String playerName;
    private long timestamp;
    private int occupiedSlots;
    private long totalQuantity;
    private final List<GroupStorageItem> items = new ArrayList<>();

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getOccupiedSlots()
    {
        return occupiedSlots;
    }

    public void setOccupiedSlots(int occupiedSlots)
    {
        this.occupiedSlots = occupiedSlots;
    }

    public long getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalQuantity(long totalQuantity)
    {
        this.totalQuantity = totalQuantity;
    }

    public List<GroupStorageItem> getItems()
    {
        return items;
    }

    public static class GroupStorageItem
    {
        private final int itemId;
        private final int rawItemId;
        private final String name;
        private final int quantity;

        public GroupStorageItem(int itemId, int rawItemId, String name, int quantity)
        {
            this.itemId = itemId;
            this.rawItemId = rawItemId;
            this.name = name;
            this.quantity = quantity;
        }

        public int getItemId()
        {
            return itemId;
        }

        public int getRawItemId()
        {
            return rawItemId;
        }

        public String getName()
        {
            return name;
        }

        public int getQuantity()
        {
            return quantity;
        }
    }
}
