package com.hcimprogression.companion;

/** Read-only availability for one of RuneLite's built-in daily task checks. */
public class DailyTaskSnapshot
{
    private final String id;
    private final String name;
    private final String location;
    private final boolean unlocked;
    private final boolean available;
    private final int claimed;
    private final int maximum;

    public DailyTaskSnapshot(String id, String name, String location, boolean unlocked,
        boolean available, int claimed, int maximum)
    {
        this.id = id;
        this.name = name;
        this.location = location;
        this.unlocked = unlocked;
        this.available = available;
        this.claimed = claimed;
        this.maximum = maximum;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isAvailable() { return available; }
    public int getClaimed() { return claimed; }
    public int getMaximum() { return maximum; }
}
