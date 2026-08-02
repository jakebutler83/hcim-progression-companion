package com.hcimprogression.companion;

final class SyncBackoff
{
    private static final long BASE_DELAY_MILLIS = 30_000L;
    private static final long MAX_DELAY_MILLIS = 15 * 60_000L;

    private int consecutiveFailures;
    private long retryAtMillis;

    synchronized boolean canAttempt(long nowMillis)
    {
        return nowMillis >= retryAtMillis;
    }

    synchronized long recordFailure(long nowMillis, long jitterMillis)
    {
        consecutiveFailures = Math.min(consecutiveFailures + 1, 10);
        int shift = Math.min(consecutiveFailures - 1, 5);
        long delay = Math.min(MAX_DELAY_MILLIS, BASE_DELAY_MILLIS << shift);
        retryAtMillis = nowMillis + delay + Math.max(0L, jitterMillis);
        return retryAtMillis;
    }

    synchronized void recordSuccess()
    {
        consecutiveFailures = 0;
        retryAtMillis = 0L;
    }

    synchronized void reset()
    {
        recordSuccess();
    }

    synchronized int getConsecutiveFailures()
    {
        return consecutiveFailures;
    }

    synchronized long getRetryAtMillis()
    {
        return retryAtMillis;
    }
}
