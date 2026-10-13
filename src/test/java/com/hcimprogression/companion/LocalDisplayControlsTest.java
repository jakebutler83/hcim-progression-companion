package com.hcimprogression.companion;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalDisplayControlsTest
{
    @Test public void muteDropsVisibleAndQueuedAlertsWithoutReplayingThem()
    {
        ProgressionFeedback feedback = new ProgressionFeedback(); long session = feedback.session("first"); feedback.baseline(session);
        feedback.accept(session,true,List.of(new ProgressionFeedback.Event("quest:a","A"), new ProgressionFeedback.Event("quest:b","B")));
        assertEquals("A",feedback.tick(0)); feedback.muted(true);
        assertEquals("",feedback.view(1).popup); assertEquals("",feedback.tick(9000));
        feedback.accept(session,true,List.of(new ProgressionFeedback.Event("quest:c","C")));
        feedback.routine("ready farm"); assertEquals(3,feedback.recent().size());
        feedback.muted(false); assertEquals("",feedback.tick(20000));
        feedback.accept(session,true,List.of(new ProgressionFeedback.Event("quest:c","C"),new ProgressionFeedback.Event("quest:d","D")));
        assertEquals("D",feedback.tick(21000));
    }

    @Test public void adjustableDurationsAreBoundedAndDontChangeAnExistingPopup()
    {
        ProgressionFeedback feedback = new ProgressionFeedback(); feedback.session("first"); feedback.routine("ready");
        feedback.tick(1000,3); assertEquals("ready",feedback.view(3999).popup); assertEquals("",feedback.view(4000).popup);
        feedback.routine("long"); feedback.tick(5000,999); assertEquals("long",feedback.view(24999).popup);
        assertEquals("",feedback.view(25000).popup);
        feedback.routine("minimum"); feedback.tick(30000,-1); assertEquals("",feedback.view(33000).popup);
    }

    @Test public void mutingDoesNotHideOrDeletePinnedGoals()
    {
        ProgressionFeedback feedback = new ProgressionFeedback(); long session = feedback.session("first");
        feedback.goal(session,new ProgressionFeedback.Goal("Mining",0,1),0); feedback.muted(true);
        assertNotNull(feedback.view(1000).goal);
    }
}
