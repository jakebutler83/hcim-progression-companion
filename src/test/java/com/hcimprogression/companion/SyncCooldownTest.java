package com.hcimprogression.companion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SyncCooldownTest
{
    @Test
    public void delaysChangesUntilFiveMinutesAfterSuccess()
    {
        SyncCooldown cooldown = new SyncCooldown(300_000L);

        assertEquals(1_000L, cooldown.nextAllowedAt(1_000L, 0L));

        cooldown.recordSuccess(10_000L);
        assertEquals(310_000L, cooldown.nextAllowedAt(20_000L, 0L));
        assertEquals(310_000L, cooldown.nextAllowedAt(200_000L, 0L));
        assertEquals(350_000L, cooldown.nextAllowedAt(350_000L, 0L));
    }

    @Test
    public void preservesNetworkBackoffAndCanReset()
    {
        SyncCooldown cooldown = new SyncCooldown(300_000L);
        cooldown.recordSuccess(10_000L);

        assertEquals(500_000L, cooldown.nextAllowedAt(20_000L, 500_000L));

        cooldown.reset();
        assertEquals(20_000L, cooldown.nextAllowedAt(20_000L, 0L));
    }
}
