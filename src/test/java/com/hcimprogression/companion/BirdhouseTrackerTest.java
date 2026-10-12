package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

public class BirdhouseTrackerTest
{
    @Test public void timeTrackingRecordKeepsItsOriginalDeadlineAcrossRestart()
    {
        BirdhouseSnapshot.House house = BirdhouseTracker.fromTimeTracking("Valley", "3:1000", 2000);
        assertEquals(4000, house.getReadyAt());
        assertEquals("growing", house.getState());
        assertEquals("ready", BirdhouseTracker.fromTimeTracking("Valley", "3:1000", 4000).getState());
    }
    @Test public void emptyAndUnseededHousesDoNotHaveGrowthDeadlines()
    {
        assertEquals("empty", BirdhouseTracker.fromTimeTracking("Valley", "0:1000", 2000).getState());
        BirdhouseSnapshot.House built = BirdhouseTracker.fromTimeTracking("Valley", "2:1000", 2000);
        assertEquals("needs-seeds", built.getState());
        assertEquals(0, built.getReadyAt());
    }
    @Test public void malformedUnknownAndFutureRecordsAreIgnored()
    {
        for (String stored : new String[] {null, "", "garbage", "3:bad", "-1:1000", "301:1000", "3:-1", "3:10000", "3:1000:extra"})
            assertNull(BirdhouseTracker.fromTimeTracking("Valley", stored, 2000));
    }
}
