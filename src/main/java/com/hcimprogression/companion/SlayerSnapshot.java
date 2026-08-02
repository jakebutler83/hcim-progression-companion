package com.hcimprogression.companion;

public class SlayerSnapshot
{
    private final String currentTask;
    private final int remaining;
    private final int points;
    private final int streak;
    private final String master;
    private final int targetId;
    private final long updatedAt;

    public SlayerSnapshot(String currentTask, int remaining, int points, int streak, String master, int targetId, long updatedAt)
    {
        this.currentTask = currentTask;
        this.remaining = remaining;
        this.points = points;
        this.streak = streak;
        this.master = master;
        this.targetId = targetId;
        this.updatedAt = updatedAt;
    }
    public String getCurrentTask() { return currentTask; }
    public int getRemaining() { return remaining; }
    public int getPoints() { return points; }
    public int getStreak() { return streak; }
    public String getMaster() { return master; }
    public int getTargetId() { return targetId; }
    public long getUpdatedAt() { return updatedAt; }
}
