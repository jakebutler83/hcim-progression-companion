package com.hcimprogression.companion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FarmRunTrackerTest
{
    @Test
    public void herbPredictionUsesRuneLiteStageInsteadOfRestartingTheFullTimer()
    {
        long observedAt = 1_800_000_100L;
        long observedTick = observedAt - observedAt % (20 * 60L);

        FarmRunTracker.GrowthPrediction prediction = FarmRunTracker.predictHerb(
            84, observedAt, observedAt, 0, 0, false);

        assertEquals("Cadantine", prediction.getCrop());
        assertEquals("growing", prediction.getState());
        assertEquals(observedTick + 2 * 20 * 60L, prediction.getReadyAt());
    }

    @Test
    public void herbPredictionRecognizesCurrentHarvestableAndInsertedCrops()
    {
        FarmRunTracker.GrowthPrediction cadantine = FarmRunTracker.predictHerb(
            86, 1_800_000_000L, 1_800_000_000L, 0, 0, false);
        FarmRunTracker.GrowthPrediction huasca = FarmRunTracker.predictHerb(
            60, 1_800_000_000L, 1_800_000_000L, 0, 0, false);

        assertEquals("Cadantine", cadantine.getCrop());
        assertEquals("ready", cadantine.getState());
        assertEquals(0L, cadantine.getReadyAt());
        assertEquals("Huasca", huasca.getCrop());
        assertEquals("growing", huasca.getState());
    }

    @Test
    public void diseasedDeadAndEmptyHerbsNeverScheduleHarvestAlerts()
    {
        for (int rawState : new int[] {0, 67, 128, 158, 170, 176, 201, 221})
        {
            FarmRunTracker.GrowthPrediction prediction = FarmRunTracker.predictHerb(
                rawState, 1_800_000_000L, 1_800_000_000L, 0, 0, false);
            assertEquals("waiting", prediction.getState());
            assertEquals(0L, prediction.getReadyAt());
        }
    }

    @Test
    public void farmTickOffsetMatchesRuneLiteTimeTrackingAlignment()
    {
        long requested = 1_800_000_100L;
        long expected = ((requested + 5 * 60L) - (requested + 5 * 60L) % (20 * 60L)) - 5 * 60L;
        assertEquals(expected, FarmRunTracker.tickTime(20, 0, requested, 40, 5));
    }
}
