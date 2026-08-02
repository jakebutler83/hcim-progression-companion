package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

public class BirdhouseSnapshot
{
    private final List<House> houses = new ArrayList<>();
    private int trackedCount;
    private int seededCount;
    private int readyCount;
    private long nextReadyAt;

    public List<House> getHouses() { return houses; }
    public int getTrackedCount() { return trackedCount; }
    public void setTrackedCount(int value) { trackedCount = value; }
    public int getSeededCount() { return seededCount; }
    public void setSeededCount(int value) { seededCount = value; }
    public int getReadyCount() { return readyCount; }
    public void setReadyCount(int value) { readyCount = value; }
    public long getNextReadyAt() { return nextReadyAt; }
    public void setNextReadyAt(long value) { nextReadyAt = value; }

    public static class House
    {
        private final String name;
        private final String state;
        private final long lastChangedAt;
        private final long readyAt;

        public House(String name, String state, long lastChangedAt, long readyAt)
        {
            this.name = name;
            this.state = state;
            this.lastChangedAt = lastChangedAt;
            this.readyAt = readyAt;
        }

        public String getName() { return name; }
        public String getState() { return state; }
        public long getLastChangedAt() { return lastChangedAt; }
        public long getReadyAt() { return readyAt; }
    }
}
