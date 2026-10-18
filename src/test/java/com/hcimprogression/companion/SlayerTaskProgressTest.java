package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerTaskProgressTest
{
    @Test public void startsAtZeroFromTheRemainingAssignmentNotLifetimeKc()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress();
        assertNull(progress.view(0)); progress.start("Greater demons",124,1000);
        assertEquals(124,progress.view(0).total); assertEquals(0,progress.view(0).done); assertEquals(0.0,progress.view(0).percent,0.00001);
        progress.observe(100,1000); assertEquals(24,progress.view(1000).done); assertEquals(100,progress.view(1000).remaining);
    }
    @Test public void countCreditsHandleExpeditiousAndSlaughterWithoutInventingKills()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Greater demons",100,1000);
        progress.observe(100,1); assertEquals(0,progress.view(1).done);
        progress.observe(98,2); assertEquals(2,progress.view(2).done); assertEquals(2.0,progress.view(2).percent,0.00001);
    }
    @Test public void extensionsPreserveProgressAndCannotMakeNegativePercent()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",10,1000);
        progress.observe(5,0); progress.observe(15,1);
        assertEquals(20,progress.view(1).total); assertEquals(5,progress.view(1).done); assertEquals(25.0,progress.view(1).percent,0.00001);
    }
    @Test public void completionDisplaysBrieflyThenExpiresWithoutMutatingOnRender()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",5,1000); progress.observe(0,1000);
        assertEquals(100.0,progress.view(1000).percent,0.00001); assertEquals(5,progress.view(1000).done);
        progress.observe(0,5000); assertNull(progress.view(9000));
    }
    @Test public void slayerXpUsesTheStartedBaselineNotKillsOrLifetimeExperience()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Greater demons",100,500000);
        assertEquals(0,progress.view(0).xpGained);
        progress.experience(501234,1); assertEquals(1234,progress.view(1).xpGained);
        assertEquals(0,progress.view(1).done);
        assertEquals("0.0% · +1,234 XP",progress.view(1).barLabel);
        progress.observe(98,2); assertEquals(1234,progress.view(2).xpGained);
        progress.observe(99,3); assertEquals(1234,progress.view(3).xpGained);
        progress.experience(501234,4); assertEquals(1234,progress.view(4).xpGained);
    }

    @Test public void finalXpCanArriveAfterTheCountWithoutExtendingTheCompletionPopup()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",1,1000);
        progress.observe(0,1000); progress.experience(1100,1100);
        assertEquals(100,progress.view(1100).xpGained); assertEquals(100.0,progress.view(1100).percent,0.00001);
        progress.experience(1200,9000); assertNull(progress.view(9000));
    }

    @Test public void stopRestartAndFallingXpDiscardThePreviousBaseline()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",10,1000);
        progress.experience(1200,1); progress.stop(); progress.experience(2000,2); assertNull(progress.view(2));
        progress.start("Dust devils",10,2000); assertEquals(0,progress.view(2).xpGained);
        progress.experience(2100,3); assertEquals(100,progress.view(3).xpGained);
        progress.experience(1000,4); assertNull(progress.view(4));
        progress.start("Unknown XP",10,-1); assertNull(progress.view(4));
    }

    @Test public void taskCountPercentageAlsoKeepsTenthsAndDoesNotRoundUpToComplete()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Dust devils",3,1000);
        progress.observe(2,1); progress.experience(1110,2);
        assertEquals("33.3% · +110 XP",progress.view(2).barLabel);
        progress.start("Large task",10000,0); progress.observe(1,3);
        assertEquals("99.9% · +0 XP",progress.view(3).barLabel);
    }

    @Test public void stopOrRestartNeverCarriesThePreviousTaskCount()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",30,1000); progress.observe(10,0);
        progress.stop(); assertNull(progress.view(0)); progress.start("Dust devil",15,1000);
        assertEquals(0,progress.view(0).done); assertEquals("Dust devil",progress.view(0).task);
        progress.start("None",0,1000); assertNull(progress.view(0));
    }

    @Test public void restoredTrackerRetainsCreditAndXpAcrossLogins()
    {
        SlayerTaskProgress first=new SlayerTaskProgress(); first.start("Greater demons",40,500000);
        first.observe(38,1); first.experience(500250,2);
        SlayerTaskProgress restored=new SlayerTaskProgress(); assertTrue(restored.restore(first.save(2),38,500250));
        assertEquals(2,restored.view(3).done); assertEquals(250,restored.view(3).xpGained);
        restored.experience(500375,4); restored.observe(37,4);
        assertEquals(3,restored.view(4).done); assertEquals(375,restored.view(4).xpGained);
    }

    @Test public void unrelatedXpBetweenClientsIsNotInventedAsTrackedTaskXp()
    {
        SlayerTaskProgress first=new SlayerTaskProgress(); first.start("Greater demons",40,500000);
        first.observe(38,1); first.experience(500250,2);
        SlayerTaskProgress restored=new SlayerTaskProgress(); assertTrue(restored.restore(first.save(2),35,510000));
        assertEquals(5,restored.view(3).done); assertEquals(250,restored.view(3).xpGained);
        restored.experience(510125,4); assertEquals(375,restored.view(4).xpGained);
    }

    @Test public void invalidCompletedOrUnsettledRestoresAreNotShown()
    {
        SlayerTaskProgress first=new SlayerTaskProgress(); first.start("Greater demons",40,500000);
        first.experience(500250,1); SlayerTaskProgress.Saved checkpoint=first.save(1);
        SlayerTaskProgress restored=new SlayerTaskProgress();
        assertFalse(restored.restore(checkpoint,40,0)); assertNull(restored.view(2));
        assertFalse(restored.restore(checkpoint,41,500250)); assertNull(restored.view(2));
        assertFalse(restored.restore(checkpoint,0,500250));
        first.observe(0,2); assertNull(first.save(2));
    }
}
