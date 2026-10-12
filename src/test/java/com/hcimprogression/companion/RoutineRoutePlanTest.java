package com.hcimprogression.companion;

import org.junit.Test;
import static org.junit.Assert.*;

public class RoutineRoutePlanTest
{
    @Test public void starterRoutesAreValidBoundedPackingLists()
    {
        assertTrue(RoutineRoutePlan.defaults("farm").valid());
        RoutineRoutePlan bird = RoutineRoutePlan.defaults("birdhouse");
        assertTrue(bird.valid()); assertEquals(4, RoutineRoutePlan.lines(bird.stops).size());
        assertTrue(bird.items.contains("40 low-level hop seeds"));
    }
    @Test public void oversizedUntrustedAndEmptyRoutesAreRejected()
    {
        assertFalse(new RoutineRoutePlan("boss", "food", "step", 0).valid());
        assertFalse(new RoutineRoutePlan("farm", "food", "", 0).valid());
        assertFalse(new RoutineRoutePlan("farm", "food", "x".repeat(161), 0).valid());
        assertFalse(new RoutineRoutePlan("farm", "food", "step\n".repeat(25), 0).valid());
        assertFalse(new RoutineRoutePlan("farm", "food\n".repeat(33), "step", 0).valid());
        assertFalse(new RoutineRoutePlan("farm", "food", "step", -1).valid());
    }
    @Test public void routeTextIsPlainTextNotHtml()
    {
        assertEquals("b Spade /b", RoutineRoutePlan.lines("<b>Spade</b>\n\n").get(0));
    }
    @Test public void runStartsWithPackingThenManuallyVisitsEveryStop()
    {
        RoutineRouteRun run = new RoutineRouteRun(); run.start(RoutineRoutePlan.defaults("birdhouse"));
        assertEquals(-1, run.position()); assertTrue(run.title().contains("packing"));
        for (int i = 0; i < 4; i++) {run.next(); assertEquals(i, run.position()); assertTrue(run.active());}
        run.next(); assertFalse(run.active()); assertEquals("", run.text());
    }
    @Test public void backStopAndRestartAreBoundedAndNeverWrap()
    {
        RoutineRouteRun run = new RoutineRouteRun(); run.start(RoutineRoutePlan.defaults("farm"));
        run.back(); assertEquals(-1, run.position()); run.next(); run.next(); run.back(); assertEquals(0,run.position());
        run.stop(); run.next(); assertFalse(run.active());
        run.start(RoutineRoutePlan.defaults("birdhouse")); assertEquals(-1,run.position());
    }
    @Test public void editsCannotChangeAnAlreadyRunningRoute()
    {
        RoutineRoutePlan plan = RoutineRoutePlan.defaults("farm"); RoutineRouteRun run = new RoutineRouteRun();
        run.start(plan); plan.stops = "Different account's route"; run.next();
        assertTrue(run.text().startsWith("Falador"));
        assertFalse(run.start(new RoutineRoutePlan("farm","","",0)));
    }
    @Test public void activityPinsAreNotMisreportedAsCompletedTasks()
    {
        ProgressionFeedback feedback = new ProgressionFeedback(); long session = feedback.session("character");
        ProgressionFeedback.Goal goal = new ProgressionFeedback.Goal("Fight Barrows",0,0); goal.activity = true;
        feedback.goal(session,goal,0); assertNotNull(feedback.view(1).goal); assertTrue(feedback.view(1).goal.activity);
        feedback.session("different-character"); assertNull(feedback.view(2).goal);
    }
}
