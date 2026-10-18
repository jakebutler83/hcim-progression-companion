package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerAssignmentWatchTest
{
    private static final String FIRST = "12:0:4:9:49:10:0:0";
    private static final String NEXT = "13:0:5:9:50:10:0:0";
    private static SlayerAssignmentWatch loggedIn(String assignment,int remaining)
    {
        SlayerAssignmentWatch watch=new SlayerAssignmentWatch();
        assertFalse(watch.observe(assignment,remaining));
        assertFalse(watch.observe(assignment,remaining));
        return watch;
    }

    @Test public void loginAndTaskRemindersDoNotStartAnExistingAssignment()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,40);
        for(int i=0;i<10;i++) assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,39));
        assertFalse(watch.observe(FIRST,38));
        watch.reset();
        assertFalse(watch.observe(FIRST,38));
        assertFalse(watch.observe(FIRST,38));
        assertFalse(watch.observe(FIRST,37));
    }

    @Test public void incompleteLoginPacketsOnlyEstablishABaseline()
    {
        SlayerAssignmentWatch watch=new SlayerAssignmentWatch();
        assertFalse(watch.observe("0:0:0:0:0:0:0:0",0));
        assertFalse(watch.observe(FIRST,0));
        assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,39));
    }

    @Test public void receivingFirstAssignmentAutomaticallyStartsOnceAfterSettling()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,0);
        assertFalse(watch.observe(NEXT,150));
        assertTrue(watch.observe(NEXT,150));
        assertFalse(watch.observe(NEXT,150));
        assertFalse(watch.observe(NEXT,149));
    }

    @Test public void completionThenSameMonsterStillStartsANewTracker()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,1);
        assertFalse(watch.observe(FIRST,0));
        assertFalse(watch.observe(FIRST,100));
        assertTrue(watch.observe(FIRST,100));
        assertFalse(watch.observe(FIRST,100));
    }

    @Test public void changedMonsterAreaBossOrStreakCanStartWithoutAnEmptyCountTick()
    {
        for(String next:new String[]{NEXT,"12:1:4:9:49:10:0:0","12:0:5:9:49:10:0:0","12:0:4:9:50:10:0:0"})
        {
            SlayerAssignmentWatch watch=loggedIn(FIRST,20);
            assertFalse(watch.observe(next,100));
            assertTrue(watch.observe(next,99));
            assertFalse(watch.observe(next,98));
        }
    }

    @Test public void transientAssignmentPacketsDoNotStartAnIntermediateTask()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,20);
        assertFalse(watch.observe("13:0:4:9:49:10:0:0",100));
        assertFalse(watch.observe(NEXT,100));
        assertTrue(watch.observe(NEXT,100));
        assertFalse(watch.observe(NEXT,100));
    }

    @Test public void countExtensionsAndStoppedCurrentTasksDoNotRestart()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,20);
        assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,39));
        // Stop keeps the assignment baseline, while the next distinct task starts.
        assertFalse(watch.observe(NEXT,80));
        assertTrue(watch.observe(NEXT,80));
        watch.reset(); // Logout/profile change cannot compare against another character.
        assertFalse(watch.observe(FIRST,40));
        assertFalse(watch.observe(FIRST,40));
    }

    @Test public void cancellationAndRevertedPacketsDoNotResurrectOldTasks()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,20);
        assertFalse(watch.observe(NEXT,100));
        assertFalse(watch.observe(FIRST,20));
        assertFalse(watch.observe(FIRST,20));
        assertFalse(watch.observe(NEXT,100));
        assertFalse(watch.observe(NEXT,0));
        assertFalse(watch.observe(NEXT,0));
        assertFalse(watch.observe(NEXT,80));
        assertTrue(watch.observe(NEXT,80));
    }

    @Test public void newTrackerStartsWithItsOwnCountAndXpThenCanBeSaved()
    {
        SlayerAssignmentWatch watch=loggedIn(FIRST,20);
        SlayerTaskProgress progress=new SlayerTaskProgress();
        progress.start("Old task",20,500000);
        progress.observe(10,1); progress.experience(501000,1);
        assertFalse(watch.observe(NEXT,100));
        assertTrue(watch.observe(NEXT,100));
        progress.start("New task",100,501000);
        assertEquals(0,progress.view(2).done);
        assertEquals(0,progress.view(2).xpGained);
        progress.observe(99,3); progress.experience(501125,3);
        SlayerTaskProgress restored=new SlayerTaskProgress();
        assertTrue(restored.restore(progress.save(3),99,501125));
        assertEquals(1,restored.view(4).done);
        assertEquals(125,restored.view(4).xpGained);
    }
}
