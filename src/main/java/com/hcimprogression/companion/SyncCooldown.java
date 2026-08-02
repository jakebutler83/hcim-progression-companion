package com.hcimprogression.companion;

final class SyncCooldown
{
    private final long cooldownMillis;
    private long lastSuccessAtMillis;

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
        lastSuccessAtMillis = 0L;
    }

    synchronized void recordSuccess(long nowMillis)
    {
        lastSuccessAtMillis = nowMillis;
    }

    synchronized long nextAllowedAt(long nowMillis, long retryAtMillis)
    {
        long cooldownAt = lastSuccessAtMillis > 0L
                ? lastSuccessAtMillis + cooldownMillis
                : nowMillis;
        return Math.max(nowMillis, Math.max(cooldownAt, retryAtMillis));
    }
}
