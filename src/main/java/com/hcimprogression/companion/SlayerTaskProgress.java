package com.hcimprogression.companion;

/** Assignment-count progress, not lifetime KC or a count of attack animations. */
final class SlayerTaskProgress
{
    static final class View
    {
        final String task;
        final int total, remaining, done, percent;
        final long until;
        View(String task, int total, int remaining, long until)
        {
            this.task = task; this.total = total; this.remaining = remaining; this.until = until;
            done = Math.max(0,total - remaining);
            percent = total <= 0 ? 0 : (int)Math.min(100,100L * done / total);
        }
    }
    private volatile View view;
    void start(String task, int remaining)
    {
        view = remaining > 0 ? new View(ProgressionFeedback.clean(task),remaining,remaining,Long.MAX_VALUE) : null;
    }
    void observe(int count, long now)
    {
        View previous = view;
        if (previous == null || previous.remaining == 0) return;
        int remaining = Math.max(0,count);
        // If a task is extended, preserve progress already credited and enlarge
        // its target instead of letting the bar become negative or over 100%.
        int total = previous.total + Math.max(0,remaining - previous.remaining);
        view = new View(previous.task,total,remaining,remaining == 0 ? now + 8000L : Long.MAX_VALUE);
    }
    View view(long now) { View current = view; return current != null && now < current.until ? current : null; }
    void stop() { view = null; }
}
