package com.hcimprogression.companion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarrowsRewardTrackerTest
{
    @Test
    public void reopeningAndPartialWithdrawalsCannotRecordTheSameChestAgain()
    {
        AtomicReference<Integer> saved = new AtomicReference<>();
        AtomicInteger drops = new AtomicInteger();
        BarrowsRewardTracker tracker = new BarrowsRewardTracker(saved::get, saved::set);
        assertTrue(tracker.capture(13, drops::incrementAndGet));
        assertFalse(tracker.capture(13, drops::incrementAndGet));
        assertFalse(tracker.capture(12, drops::incrementAndGet));
        // Recreating the tracker simulates a restart, with no time-based reset.
        tracker = new BarrowsRewardTracker(saved::get, saved::set);
        assertFalse(tracker.capture(13, drops::incrementAndGet));
        assertTrue(tracker.capture(14, drops::incrementAndGet));
        assertEquals(2, drops.get());
    }

    @Test
    public void chestCountsAreIndependentForEachRuneScapeProfile()
    {
        Map<String, Integer> profiles = new HashMap<>();
        AtomicReference<String> profile = new AtomicReference<>("main");
        BarrowsRewardTracker tracker = new BarrowsRewardTracker(
            () -> profiles.get(profile.get()), chest -> profiles.put(profile.get(), chest));
        assertTrue(tracker.capture(1300, () -> {}));
        profile.set("gim");
        assertTrue(tracker.capture(13, () -> {}));
        assertFalse(tracker.capture(13, () -> {}));
        profile.set("main");
        assertFalse(tracker.capture(1300, () -> {}));
        assertTrue(tracker.capture(1301, () -> {}));
    }

    @Test
    public void uninitializedCountsAndFailedCapturesDoNotConsumeAChest()
    {
        AtomicReference<Integer> saved = new AtomicReference<>();
        BarrowsRewardTracker tracker = new BarrowsRewardTracker(saved::get, saved::set);
        assertFalse(tracker.capture(0, () -> fail("Uninitialized count")));
        assertFalse(tracker.capture(-1, () -> fail("Invalid count")));
        try
        {
            tracker.capture(13, () -> { throw new IllegalStateException("Failed recorder"); });
            fail("Expected recorder failure");
        }
        catch (IllegalStateException expected) { assertNull(saved.get()); }
        assertTrue(tracker.capture(13, () -> {}));
    }
}
