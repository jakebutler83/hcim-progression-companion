package com.hcimprogression.companion;

final class SyncCooldown
{
    private final long cooldownMillis;
    private long lastAttemptAtMillis;

    SyncCooldown(long cooldownMillis)
    {
        if (cooldownMillis < 0L)
        {
            throw new IllegalArgumentException("cooldownMillis must not be negative");
        }
        this.cooldownMillis = cooldownMillis;
    }

    synchronized void reset()
    {
        lastAttemptAtMillis = 0L;
    }

    synchronized void recordAttempt(long nowMillis)
    {
        lastAttemptAtMillis = nowMillis;
    }

    synchronized long nextAllowedAt(long nowMillis, long retryAtMillis)
    {
        long cooldownAt = lastAttemptAtMillis > 0L
                ? lastAttemptAtMillis + cooldownMillis
                : nowMillis;
        return Math.max(nowMillis, Math.max(cooldownAt, retryAtMillis));
    }
}
