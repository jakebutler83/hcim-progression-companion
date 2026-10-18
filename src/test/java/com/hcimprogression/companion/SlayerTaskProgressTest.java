package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerTaskProgressTest
{
    @Test public void startsAtZeroFromTheRemainingAssignmentNotLifetimeKc()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress();
        assertNull(progress.view(0)); progress.start("Greater demons",124);
        assertEquals(124,progress.view(0).total); assertEquals(0,progress.view(0).done); assertEquals(0,progress.view(0).percent);
        progress.observe(100,1000); assertEquals(24,progress.view(1000).done); assertEquals(100,progress.view(1000).remaining);
    }
    @Test public void countCreditsHandleExpeditiousAndSlaughterWithoutInventingKills()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Greater demons",100);
        progress.observe(100,1); assertEquals(0,progress.view(1).done);
        progress.observe(98,2); assertEquals(2,progress.view(2).done); assertEquals(2,progress.view(2).percent);
    }
    @Test public void extensionsPreserveProgressAndCannotMakeNegativePercent()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",10);
        progress.observe(5,0); progress.observe(15,1);
        assertEquals(20,progress.view(1).total); assertEquals(5,progress.view(1).done); assertEquals(25,progress.view(1).percent);
    }
    @Test public void completionDisplaysBrieflyThenExpiresWithoutMutatingOnRender()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",5); progress.observe(0,1000);
        assertEquals(100,progress.view(1000).percent); assertEquals(5,progress.view(1000).done);
        progress.observe(0,5000); assertNull(progress.view(9000));
    }
    @Test public void stopOrRestartNeverCarriesThePreviousTaskCount()
    {
        SlayerTaskProgress progress = new SlayerTaskProgress(); progress.start("Bloodveld",30); progress.observe(10,0);
        progress.stop(); assertNull(progress.view(0)); progress.start("Dust devil",15);
        assertEquals(0,progress.view(0).done); assertEquals("Dust devil",progress.view(0).task);
        progress.start("None",0); assertNull(progress.view(0));
    }
}
