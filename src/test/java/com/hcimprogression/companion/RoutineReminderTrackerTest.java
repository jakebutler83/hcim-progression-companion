package com.hcimprogression.companion;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RoutineReminderTrackerTest
{
    private final RoutineReminderTracker tracker = new RoutineReminderTracker();
    private BirdhouseSnapshot bird(String state, long cycle, long due)
    {
        BirdhouseSnapshot result = new BirdhouseSnapshot();
        result.getHouses().add(new BirdhouseSnapshot.House("Valley", state, cycle, due));
        return result;
    }
    private FarmRunSnapshot farm(String state, long cycle, long due)
    {
        FarmRunSnapshot result = new FarmRunSnapshot();
        result.getPatches().add(new FarmRunSnapshot.Patch("catherby", "Catherby", "Herb", "Ranarr", state, 1, cycle, due));
        return result;
    }

    @Test public void loginWithAlreadyReadyRoutinesIsSilent()
    {
        assertTrue(tracker.observe(bird("ready", 1, 10), farm("ready", 1, 0), 100).isEmpty());
        assertTrue(tracker.observe(bird("ready", 1, 10), farm("ready", 1, 0), 110).isEmpty());
    }
    @Test public void eachGrowingCycleNotifiesOnceAndReplantingArmsAnotherCycle()
    {
        assertTrue(tracker.observe(bird("growing", 10, 200), farm("growing", 10, 200), 100).isEmpty());
        assertEquals(2, tracker.observe(bird("ready", 10, 200), farm("ready", 10, 0), 200).size());
        assertTrue(tracker.observe(bird("ready", 10, 200), farm("ready", 10, 0), 210).isEmpty());
        tracker.observe(bird("growing", 220, 300), farm("growing", 220, 300), 220);
        assertEquals(2, tracker.observe(bird("ready", 220, 300), farm("ready", 220, 0), 300).size());
    }
    @Test public void waitingDiseasedDeadAndUnseededObservationsCancelArmedReminders()
    {
        for (String state : new String[] {"waiting", "empty", "diseased", "dead", "needs-seeds", "unknown"})
        {
            tracker.reset();
            tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 100);
            assertTrue(tracker.observe(bird(state, 1, 0), farm(state, 1, 0), 120).isEmpty());
            assertTrue(tracker.observe(bird("ready", 1, 200), farm("ready", 1, 0), 200).isEmpty());
        }
    }
    @Test public void accountSwitchLogoutAndReEnableNeverReplayReadyTimers()
    {
        tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 100);
        tracker.reset();
        assertTrue(tracker.observe(bird("ready", 1, 200), farm("ready", 1, 0), 200).isEmpty());
    }
    @Test public void missingTrackingDataCancelsTimersInsteadOfInventingReadyState()
    {
        tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 100);
        assertTrue(tracker.observe(null, null, 200).isEmpty());
        assertTrue(tracker.observe(bird("ready", 1, 200), farm("ready", 1, 0), 220).isEmpty());
    }
    @Test public void naturalDeadlineCrossingWorksWithoutNewVarbitEvents()
    {
        tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 100);
        assertEquals(2, tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 205).size());
        assertTrue(tracker.observe(bird("growing", 1, 200), farm("growing", 1, 200), 215).isEmpty());
    }
    @Test public void predictionCorrectionsDoNotReplayTheSameCycle()
    {
        tracker.observe(bird("growing", 1, 200), null, 100);
        assertEquals(1, tracker.observe(bird("ready", 1, 200), null, 200).size());
        tracker.observe(bird("growing", 1, 250), null, 210);
        assertTrue(tracker.observe(bird("ready", 1, 250), null, 250).isEmpty());
    }
    @Test public void simultaneousHousesAreBatchedIntoOneMessage()
    {
        BirdhouseSnapshot growing = bird("growing", 1, 200), ready = bird("ready", 1, 200);
        growing.getHouses().add(new BirdhouseSnapshot.House("Meadow", "growing", 1, 200));
        ready.getHouses().add(new BirdhouseSnapshot.House("Meadow", "ready", 1, 200));
        tracker.observe(growing, null, 100);
        List<String> messages = tracker.observe(ready, null, 200);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).startsWith("2 birdhouses"));
    }
    @Test public void invalidTimestampsAreNotUsed()
    {
        assertTrue(tracker.observe(bird("growing", 10000, 20000), farm("growing", -1, 200), 100).isEmpty());
        assertTrue(tracker.observe(bird("ready", 10000, 20000), farm("ready", -1, 200), 200).isEmpty());
    }
}
