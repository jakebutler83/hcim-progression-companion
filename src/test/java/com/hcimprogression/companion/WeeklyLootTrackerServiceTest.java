package com.hcimprogression.companion;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeeklyLootTrackerServiceTest
{
    @Test
    public void onlyAttributesLootToMatchingSlayerSources()
    {
        assertTrue(WeeklyLootTrackerService.sourceMatchesSlayerTask("Fire giant", "Fire giants"));
        assertTrue(WeeklyLootTrackerService.sourceMatchesSlayerTask("Baby blue dragon", "Blue dragons"));
        assertTrue(WeeklyLootTrackerService.sourceMatchesSlayerTask("TzHaar-Ket", "TzHaar"));

        assertFalse(WeeklyLootTrackerService.sourceMatchesSlayerTask("TzHaar-Ket", "Fire giants"));
        assertFalse(WeeklyLootTrackerService.sourceMatchesSlayerTask("Aberrant spectre", "Shades"));
        assertFalse(WeeklyLootTrackerService.sourceMatchesSlayerTask("NPC loot", "Fire giants"));
    }
}
