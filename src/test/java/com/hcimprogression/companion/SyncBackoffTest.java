package com.hcimprogression.companion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SyncBackoffTest
{
    @Test
    public void appliesExponentialDelayAndResetsAfterSuccess()
    {
        SyncBackoff backoff = new SyncBackoff();
        assertTrue(backoff.canAttempt(1_000L));

        assertEquals(31_500L, backoff.recordFailure(1_000L, 500L));
        assertFalse(backoff.canAttempt(31_499L));
        assertTrue(backoff.canAttempt(31_500L));

        assertEquals(91_500L, backoff.recordFailure(31_500L, 0L));
        assertEquals(2, backoff.getConsecutiveFailures());

        backoff.recordSuccess();
        assertEquals(0, backoff.getConsecutiveFailures());
        assertTrue(backoff.canAttempt(0L));
    }

    @Test
    public void capsTheRetryWindow()
    {
        SyncBackoff backoff = new SyncBackoff();
        long now = 0L;
        for (int index = 0; index < 10; index++)
        {
            now = backoff.recordFailure(now, 0L);
        }
        assertTrue(backoff.getRetryAtMillis() - (now - 900_000L) <= 900_000L);
    }
}
