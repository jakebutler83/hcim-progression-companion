package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProgressionFeedbackTest
{
    private final ProgressionFeedback feedback = new ProgressionFeedback();
    private ProgressionFeedback.Event event(String id) { return new ProgressionFeedback.Event(id, "Cook's Assistant"); }

    @Test public void allEightCategoryCombinationsOnlyDisplayEnabledCategories()
    {
        for (int mask = 0; mask < 8; mask++)
        {
            ProgressionFeedback state = new ProgressionFeedback();
            long session = state.session("character:" + mask);
            state.baseline(session);
            state.categories((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            state.accept(session, true, Arrays.asList(
                new ProgressionFeedback.Event("quest:new", "Quest title"),
                new ProgressionFeedback.Event("diary:new", "Diary title"),
                new ProgressionFeedback.Event("task:new", "Milestone title")));
            java.util.List<String> expected = new java.util.ArrayList<>();
            if ((mask & 1) != 0) expected.add("Quest title");
            if ((mask & 2) != 0) expected.add("Diary title");
            if ((mask & 4) != 0) expected.add("Milestone title");
            java.util.List<String> actual = new java.util.ArrayList<>();
            for (int step = 0; step < 4; step++)
            {
                String popup = state.tick(1 + step * ProgressionFeedback.POPUP_MILLIS);
                if (!popup.isEmpty()) actual.add(popup);
            }
            assertEquals("Category mask " + mask, expected, actual);
            assertEquals(3, state.recent().size());
            state.categories(true, true, true);
            assertEquals("Re-enable must not replay mask " + mask, "", state.tick(100_000));
        }
    }

    @Test public void popupsExpireAtEightSecondsAndAreOnlyEmittedOnce()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        feedback.accept(session, true, Collections.singletonList(event("quest:duration")));
        assertEquals("Cook's Assistant", feedback.tick(100));
        assertEquals("Quest", feedback.view(101).popupCategory);
        assertEquals("", feedback.tick(101));
        assertEquals("Cook's Assistant", feedback.view(8_099).popup);
        assertEquals("", feedback.view(8_100).popup);
        assertEquals("", feedback.tick(8_100));
        assertEquals("", feedback.tick(8_101));
        assertEquals(1, feedback.recent().size());
    }

    @Test public void recentListIsNewestFirstAndOldSnapshotsCannotBeChangedByLaterEvents()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        for (int i = 0; i < 12; i++) feedback.accept(session, true, Collections.singletonList(
            new ProgressionFeedback.Event("task:" + i, "Milestone " + i)));
        java.util.List<ProgressionFeedback.Recent> snapshot = feedback.recent();
        assertEquals(10, snapshot.size());
        assertEquals("Milestone 11", snapshot.get(0).title);
        assertEquals("Milestone 2", snapshot.get(9).title);
        feedback.routine("Local routine reminder");
        assertEquals(10, feedback.recent().size());
        feedback.session("sw");
        assertEquals(10, snapshot.size());
        assertTrue(feedback.recent().isEmpty());
    }

    @Test public void deadlineTrackerAndFeedbackDeliverOneBatchedLocalReminderPerCycle()
    {
        RoutineReminderTracker timers = new RoutineReminderTracker();
        BirdhouseSnapshot birds = new BirdhouseSnapshot();
        birds.getHouses().add(new BirdhouseSnapshot.House("Valley", "growing", 100, 200));
        birds.getHouses().add(new BirdhouseSnapshot.House("Meadow", "growing", 100, 200));
        feedback.session("unlinked-character");
        for (String message : timers.observe(birds, null, 100)) feedback.routine(message);
        assertEquals("", feedback.tick(100_000));
        for (String message : timers.observe(birds, null, 200)) feedback.routine(message);
        assertTrue(feedback.tick(200_000).startsWith("2 birdhouses"));
        assertTrue(feedback.view(200_001).routinePopup);
        for (String message : timers.observe(birds, null, 210)) feedback.routine(message);
        assertEquals("", feedback.tick(210_000));
        assertTrue(feedback.recent().isEmpty());
        timers.reset();
        feedback.session("other-character");
        for (String message : timers.observe(birds, null, 220)) feedback.routine(message);
        assertEquals("", feedback.tick(220_000));
    }

    @Test public void localRoutinesWorkWithoutAWebsiteBaselineAndKeepTheirOwnHeading()
    {
        feedback.session("unlinked-character");
        feedback.routine("Birdhouses ready to check");
        assertEquals("Birdhouses ready to check", feedback.tick(1));
        assertTrue(feedback.view(2).routinePopup);
        long revision = feedback.session("linked-character");
        feedback.baseline(revision);
        feedback.accept(revision, true, Collections.singletonList(event("new")));
        assertEquals("Cook's Assistant", feedback.tick(3));
        assertFalse(feedback.view(4).routinePopup);
    }

    @Test public void localRoutinesAreDroppedOnLogoutAndWhenSettingsChange()
    {
        feedback.session("hp:routines-on");
        feedback.routine("Old routine");
        feedback.session("hp:routines-off");
        assertEquals("", feedback.tick(1));
        feedback.session("");
        feedback.routine("Logged-out routine");
        assertEquals("", feedback.tick(2));
    }

    @Test public void firstSyncIsSilentEvenWhenMarkedAsGameEvent()
    {
        long session = feedback.session("hp");
        feedback.accept(session, true, Collections.singletonList(event("old")));
        assertTrue(feedback.recent().isEmpty());
        assertEquals("", feedback.tick(1));
        feedback.accept(session, true, Collections.singletonList(event("new")));
        assertEquals("Cook's Assistant", feedback.tick(2));
    }

    @Test public void categoryFiltersMutePopupsButKeepNewCompletionsWithoutReplay()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        feedback.categories(false, false, true);
        feedback.accept(session, true, Arrays.asList(event("quest:one"), event("diary:one"), event("task:one")));
        assertEquals(3, feedback.recent().size());
        assertEquals("Milestone", feedback.recent().get(0).category);
        assertEquals("Diary", feedback.recent().get(1).category);
        assertEquals("Quest", feedback.recent().get(2).category);
        assertEquals("Cook's Assistant", feedback.tick(1));
        feedback.categories(true, true, false);
        assertEquals("", feedback.view(2).popup);
        feedback.accept(session, true, Arrays.asList(event("quest:one"), event("diary:one"), event("task:one")));
        assertEquals("", feedback.tick(3));
        assertEquals(3, feedback.recent().size());
    }

    @Test public void disablingCategoriesRemovesQueuedPopupsWithoutAffectingRoutines()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        feedback.accept(session, true, Arrays.asList(event("quest:one"), event("diary:one")));
        feedback.routine("Birdhouses ready");
        feedback.categories(false, false, false);
        assertEquals("Birdhouses ready", feedback.tick(1));
        assertTrue(feedback.view(2).routinePopup);
        assertEquals(2, feedback.recent().size());
        assertEquals("", feedback.tick(10_000));
    }

    @Test public void recentHistoryIsBoundedDeduplicatedAndSessionPrivate()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        feedback.accept(session, false, Collections.singletonList(event("quest:refresh")));
        for (int i = 0; i < 40; i++) feedback.accept(session, true, Collections.singletonList(event("quest:" + i)));
        assertEquals(10, feedback.recent().size());
        assertTrue(feedback.recent().get(0).completedAt > 0);
        long revision = feedback.recentRevision();
        feedback.accept(session, true, Collections.singletonList(event("quest:39")));
        assertEquals(revision, feedback.recentRevision());
        try { feedback.recent().clear(); fail("History must be immutable"); }
        catch (UnsupportedOperationException expected) { }
        feedback.session("sw");
        assertTrue(feedback.recent().isEmpty());
        feedback.accept(session, true, Collections.singletonList(event("quest:late")));
        assertTrue(feedback.recent().isEmpty());
        long sw = feedback.session("sw");
        feedback.baseline(sw);
        feedback.accept(sw, true, Collections.singletonList(event("quest:new")));
        feedback.session("");
        assertTrue(feedback.recent().isEmpty());
    }

    @Test public void refreshesAndRepeatedResponsesDoNotReplay()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        feedback.accept(session, false, Collections.singletonList(event("refresh")));
        feedback.accept(session, true, Arrays.asList(event("refresh"), event("new"), event("new")));
        assertEquals("Cook's Assistant", feedback.tick(1));
        assertEquals("", feedback.tick(10_000));
        feedback.accept(session, true, Collections.singletonList(event("new")));
        assertEquals("", feedback.tick(20_000));
    }

    @Test public void switchingAccountRejectsCallbacksAndClearsVisibleState()
    {
        long old = feedback.session("hp");
        feedback.baseline(old);
        feedback.goal(old, new ProgressionFeedback.Goal("HP goal", 0, 1), 1);
        feedback.accept(old, true, Collections.singletonList(event("quest")));
        feedback.tick(2);
        long next = feedback.session("sw");
        assertNotEquals(old, next);
        feedback.accept(old, true, Collections.singletonList(event("late")));
        feedback.goal(old, new ProgressionFeedback.Goal("Wrong account", 0, 1), 3);
        assertNull(feedback.view(4).goal);
        assertEquals("", feedback.view(4).popup);
        assertEquals("", feedback.tick(4));
    }

    @Test public void logoutAndReEnableCreateNewBaselines()
    {
        long old = feedback.session("hp:on");
        feedback.baseline(old);
        feedback.session("");
        assertFalse(feedback.current(old));
        long next = feedback.session("hp:on");
        feedback.accept(next, true, Collections.singletonList(event("old")));
        assertEquals("", feedback.tick(1));
        feedback.session("hp:off");
        assertFalse(feedback.current(next));
    }

    @Test public void goalsExpireAndFailuresClearThem()
    {
        long session = feedback.session("hp");
        feedback.goal(session, new ProgressionFeedback.Goal("Goal", 20, 1), 1);
        assertEquals(1, feedback.view(2).goal.done);
        assertNull(feedback.view(90_001).goal);
        feedback.goal(session, new ProgressionFeedback.Goal("Goal", 0, 1), 100_000);
        feedback.goal(session, null, 100_001);
        assertNull(feedback.view(100_002).goal);
        feedback.goal(session, new ProgressionFeedback.Goal("Bad goal", 0, 1000), 100_003);
        assertNull(feedback.view(100_004).goal);
    }

    @Test public void popupQueueIsBoundedAndMarkupIsNeutralized()
    {
        long session = feedback.session("hp");
        feedback.baseline(session);
        for (int i = 0; i < 100; i++) feedback.accept(session, true, Collections.singletonList(event("id" + i)));
        int displayed = 0;
        for (int i = 1; i <= 100; i++) if (!feedback.tick(i * 10_000L).isEmpty()) displayed++;
        assertEquals(8, displayed);
        assertFalse(ProgressionFeedback.clean("<col=red>hello\n").contains("<"));
        for (String line : ProgressionFeedback.wrap(String.join("", Collections.nCopies(100, "x")), 27)) assertTrue(line.length() <= 27);
    }
}
