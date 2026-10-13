package com.hcimprogression.companion;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class PackingChecklistTest
{
    @Test public void carriedSnapshotsCombineInventoryAndEquipmentButIgnoreNotesPlaceholdersAndZeroes()
    {
        Map<String,Long> held = new java.util.HashMap<>();
        CarriedItemSnapshot.supply(held,"Oak logs",3,false,false);
        CarriedItemSnapshot.supply(held,"Oak logs",1,false,false);
        CarriedItemSnapshot.supply(held,"Oak logs",100,true,false);
        CarriedItemSnapshot.supply(held,"Oak logs",100,false,true);
        CarriedItemSnapshot.supply(held,"Oak logs",0,false,false);
        CarriedItemSnapshot.supply(held,null,100,false,false);
        assertEquals(Long.valueOf(4),held.get("oak logs")); assertEquals(1,held.size());
    }
    @Test public void exactNamesAndQuantitiesProduceReadyAndMissingChecks()
    {
        PackingChecklist.Result result = PackingChecklist.check("Hammer\nOak logs x 4\nBarley seed x 40",
            Map.of("hammer", 1L, "oak logs", 3L, "barley seed", 40L), true);
        assertEquals(PackingChecklist.State.READY, result.rows.get(0).state);
        assertEquals("Missing 1 (have 3)", result.rows.get(1).status);
        assertEquals("1 missing · 0 manual · 2 ready", result.summary);
    }

    @Test public void noGuessingOnConditionalAdviceOrGenericIngredients()
    {
        PackingChecklist.Result result = PackingChecklist.check("4 logs matching your levels\nYour chosen teleports\nRake unless weed-free",
            Map.of("logs", 100L, "rake", 1L), true);
        assertTrue(result.rows.stream().allMatch(row -> row.state == PackingChecklist.State.MANUAL));
        assertFalse(result.summary.contains("Packing ready"));
    }

    @Test public void duplicateLinesCannotReuseTheSameSupplies()
    {
        PackingChecklist.Result result = PackingChecklist.check("Oak logs x 3\nOak logs x 3", Map.of("oak logs", 4L), true);
        assertEquals("Ready", result.rows.get(0).status);
        assertEquals("Missing 2 (have 1)", result.rows.get(1).status);
    }

    @Test public void exactChargeVariantsDoNotPretendToVerifyOtherVariants()
    {
        PackingChecklist.Result result = PackingChecklist.check("Digsite pendant(5) x 1", Map.of("digsite pendant(1)", 1L), true);
        assertEquals(PackingChecklist.State.MISSING, result.rows.get(0).state);
    }

    @Test public void missingInventoryIsWaitingNotAFalseReadyOrMissing()
    {
        PackingChecklist.Result result = PackingChecklist.check("Hammer x 1", Collections.emptyMap(), false);
        assertEquals("Waiting for inventory", result.summary);
        assertEquals(PackingChecklist.State.WAITING, result.rows.get(0).state);
    }

    @Test public void invalidQuantitiesAreManualAndEmptyListsAreNeverReady()
    {
        PackingChecklist.Result result = PackingChecklist.check("Hammer x 0\nHammer x 999999999999999999999", Map.of("hammer", 1L), true);
        assertTrue(result.rows.stream().allMatch(row -> row.state == PackingChecklist.State.MANUAL));
        assertEquals("No packing items listed", PackingChecklist.check("", Map.of(), true).summary);
    }

    @Test public void caseSpacesCommasAndMultiplicationSignAreSupported()
    {
        assertEquals("oak logs", PackingChecklist.name(" Oak\u00a0  Logs "));
        assertTrue(PackingChecklist.check(" Barley seed × 1,000 ", Map.of("barley seed", 1000L), true).summary.startsWith("Packing ready"));
    }

    @Test public void snapshotsAndResultsAreNotMutatedByChecks()
    {
        Map<String, Long> held = Map.of("hammer", 1L);
        PackingChecklist.Result result = PackingChecklist.check("Hammer x 1", held, true);
        assertEquals(Long.valueOf(1L), held.get("hammer"));
        try { result.rows.clear(); fail("result should be immutable"); }
        catch (UnsupportedOperationException expected) { }
    }

    @Test public void oversizedListsAndMalformedCommaCountsStayManual()
    {
        assertEquals("Packing list too long · review manually", PackingChecklist.check("x".repeat(2401),Map.of(),true).summary);
        assertEquals(PackingChecklist.State.MANUAL, PackingChecklist.check("Oak logs x 1,00",Map.of("oak logs",100L),true).rows.get(0).state);
    }
}
