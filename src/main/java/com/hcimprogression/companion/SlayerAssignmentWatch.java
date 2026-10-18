package com.hcimprogression.companion;

/** Login establishes a baseline; only a newly observed assignment starts a tracker. */
final class SlayerAssignmentWatch
{
    private String baseline, candidate;
    private boolean noTask;
    private int stableTicks;

    boolean observe(String assignment, int remaining)
    {
        if (baseline == null)
        {
            // Login can deliver an empty task before the real assignment vars.
            // Settle the initial state without treating it as a new assignment.
            String initial = assignment + (remaining > 0 ? ":active" : ":none");
            if (initial.equals(candidate)) stableTicks++;
            else { candidate = initial; stableTicks = 1; }
            if (stableTicks < 2) return false;
            baseline = assignment;
            noTask = remaining <= 0;
            candidate = null;
            stableTicks = 0;
            return false;
        }
        if (remaining <= 0)
        {
            baseline = assignment;
            noTask = true;
            candidate = null;
            stableTicks = 0;
            return false;
        }
        // Count changes, task extensions, point spending and chat reminders alone
        // are not proof of a new assignment. Never restart a stopped current task.
        if (!noTask && baseline.equals(assignment))
        {
            candidate = null;
            stableTicks = 0;
            return false;
        }
        if (assignment.equals(candidate)) stableTicks++;
        else { candidate = assignment; stableTicks = 1; }
        // Assignment vars can arrive separately. Require two settled game ticks.
        if (stableTicks < 2) return false;
        baseline = assignment;
        noTask = false;
        candidate = null;
        stableTicks = 0;
        return true;
    }

    void reset()
    {
        baseline = candidate = null;
        noTask = false;
        stableTicks = 0;
    }
}
