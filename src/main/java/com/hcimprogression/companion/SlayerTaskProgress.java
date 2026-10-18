package com.hcimprogression.companion;

import java.util.Locale;
import net.runelite.api.Experience;

/** Assignment-count progress, not lifetime KC or a count of attack animations. */
final class SlayerTaskProgress
{
    static final class Saved
    {
        final String task;
        final int total,remaining,xpGained,lastXp;
        Saved(String task,int total,int remaining,int xpGained,int lastXp)
        { this.task=task; this.total=total; this.remaining=remaining; this.xpGained=xpGained; this.lastXp=lastXp; }
        boolean valid()
        {
            return task!=null && !task.isEmpty() && task.length()<=120 && task.chars().noneMatch(Character::isISOControl)
                && total>0 && total<=1_000_000 && remaining>0 && remaining<=total
                && xpGained>=0 && lastXp>=xpGained && lastXp<=Experience.MAX_SKILL_XP;
        }
    }
    static final class View
    {
        final String task;
        final int total, remaining, done, xpGained;
        final double percent;
        final String barLabel;
        final long until;
        View(String task, int total, int remaining, int xpGained, long until)
        {
            this.task = task; this.total = total; this.remaining = remaining; this.until = until;
            done = Math.max(0,total - remaining);
            percent = total <= 0 ? 0 : Math.min(100.0,100.0 * done / total);
            this.xpGained = xpGained;
            double rounded = remaining == 0 ? 100.0 : Math.min(99.9,Math.round(percent * 10.0) / 10.0);
            barLabel = String.format(Locale.US,"%.1f%% · +%,d XP",rounded,xpGained);
        }
    }
    private volatile View view;
    private int startXp, currentXp;
    void start(String task, int remaining, int slayerXp)
    {
        startXp = currentXp = slayerXp;
        view = remaining > 0 && slayerXp >= 0 ? new View(ProgressionFeedback.clean(task),remaining,remaining,0,Long.MAX_VALUE) : null;
    }
    void experience(int slayerXp, long now)
    {
        View previous = view(now);
        if (previous == null) return;
        if (slayerXp < currentXp) { stop(); return; }
        if (slayerXp == currentXp) return;
        currentXp = slayerXp;
        view = new View(previous.task,previous.total,previous.remaining,Math.max(0,currentXp-startXp),previous.until);
    }
    void observe(int count, long now)
    {
        View previous = view;
        if (previous == null || previous.remaining == 0) return;
        int remaining = Math.max(0,count);
        // If a task is extended, preserve progress already credited and enlarge
        // its target instead of letting the bar become negative or over 100%.
        int total = previous.total + Math.max(0,remaining - previous.remaining);
        view = new View(previous.task,total,remaining,previous.xpGained,remaining == 0 ? now + 8000L : Long.MAX_VALUE);
    }
    View view(long now) { View current = view; return current != null && now < current.until ? current : null; }
    Saved save(long now)
    {
        View current=view(now);
        return current==null || current.remaining<=0 ? null : new Saved(current.task,current.total,current.remaining,current.xpGained,currentXp);
    }
    boolean restore(Saved saved,int liveRemaining,int liveXp)
    {
        if(saved==null || !saved.valid() || liveRemaining<=0 || liveRemaining>saved.remaining
            || liveXp<saved.lastXp || liveXp>Experience.MAX_SKILL_XP) return false;
        // Preserve proven tracked XP; unrelated/other-client XP while absent is not backfilled.
        startXp=liveXp-saved.xpGained; currentXp=liveXp;
        view=new View(saved.task,saved.total,liveRemaining,saved.xpGained,Long.MAX_VALUE);
        return true;
    }
    void stop() { view = null; startXp = currentXp = 0; }
}
