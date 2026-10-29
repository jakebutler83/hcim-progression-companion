package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

/** Simulated server-message/state inputs only; never controls the game. */
public class LocalQuestCompletionTest
{
    private static final String QUEST = "Tale of the Righteous";
    private static final String MESSAGE = "Congratulations, you've completed a quest: <col=081190>" + QUEST + "</col>";

    @Test public void acceptsOnlyGameSuppliedQuestCompletionMessages()
    {
        for (ChatMessageType type : ChatMessageType.values())
            assertEquals(type.name(), type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM ? QUEST : "",
                QuestCompletionCapture.chatName(type, MESSAGE));
        assertEquals("", QuestCompletionCapture.chatName(null, MESSAGE));
    }

    @Test public void handlesMarkupPunctuationApostrophesAndNewQuestNames()
    {
        assertEquals("Cook's Assistant", parse("Cook's Assistant."));
        assertEquals("Cook's Assistant", parse("<col=081190>Cook's Assistant</col>!"));
        assertEquals("A Brand New Quest", parse("A\u00a0Brand  New Quest"));
        assertEquals("Recipe for Disaster - Another Cook's Quest", parse("Recipe for Disaster - Another Cook's Quest"));
        assertEquals(QuestCompletionCapture.key("Cook's Assistant"), QuestCompletionCapture.key(" COOK\u2019S   ASSISTANT "));
    }

    @Test public void rejectsUnrelatedSpoofedEmptyAndOversizedMessages()
    {
        for (String message : Arrays.asList(null, "", "You've started a new quest: " + QUEST,
            "Someone says: Congratulations, you've completed a quest: " + QUEST,
            "Congratulations, you've completed a quest:", "Congratulations, you've completed a quest: !",
            "Congratulations, you've completed a quest: " + "A".repeat(101),
            "Congratulations, you've completed a quest: A\nB", "Congratulations, you've completed a quest: <bad"))
            assertEquals("", QuestCompletionCapture.chatName(ChatMessageType.GAMEMESSAGE, message));
    }

    @Test public void startsOnTheLocalEventWithoutATickLinkUploadOrWebsiteBaseline()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("unlinked-character");
        assertEquals(QUEST, state.localQuest(revision, QuestCompletionCapture.chatName(ChatMessageType.GAMEMESSAGE, MESSAGE), true, 1000, 12));
        assertTrue(state.view(1001).questPopup());
        assertEquals(1000, state.view(1001).popupStarted);
        assertEquals(13000, state.view(1001).popupUntil);
        assertEquals(1, state.recent().size());
        assertEquals("", state.tick(1001)); // No repeated desktop notification on the next tick.
        assertFalse(state.view(13000).questPopup());
    }

    @Test public void localEventDoesNotTurnHistoricalFirstSyncIntoAnAnnouncement()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.localQuest(revision, QUEST, true, 1000, 3);
        state.accept(revision, true, Arrays.asList(event(QUEST), event("An old completed quest")));
        assertEquals(1, state.recent().size());
        assertEquals("", state.tick(4000));
    }

    @Test public void chatDuplicatesAndLaterServerRepliesCannotReplayOrRestart()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.baseline(revision);
        state.localQuest(revision, QUEST, true, 1000, 8);
        assertEquals("", state.localQuest(revision, QUEST, true, 2000, 20));
        assertEquals(9000, state.view(2001).popupUntil);
        state.accept(revision, true, Collections.singletonList(new ProgressionFeedback.Event("quest:another-id", "TALE OF THE RIGHTEOUS")));
        assertEquals(1, state.recent().size());
        assertEquals("", state.tick(300000));
        state.accept(revision, true, Collections.singletonList(event(QUEST)));
        assertEquals("", state.tick(600000));
    }

    @Test public void localQuestImmediatelyPreemptsRoutineButResumeDoesNotNotifyTwice()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.routine("Birdhouses ready");
        assertEquals("Birdhouses ready", state.tick(1000));
        assertEquals(QUEST, state.localQuest(revision, QUEST, true, 1001, 3));
        assertEquals(QUEST, state.view(1002).popup);
        assertEquals("", state.tick(4001));
        assertEquals("Birdhouses ready", state.view(4002).popup);
        assertTrue(state.view(4002).routinePopup);
        assertEquals("", state.tick(13000));
    }

    @Test public void multipleRealQuestsArePrioritizedInArrivalOrderWithABoundedQueue()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.routine("Routine reminder");
        state.tick(1000);
        state.localQuest(revision, "Quest A", true, 1001, 3);
        state.localQuest(revision, "Quest B", true, 1002, 3);
        state.localQuest(revision, "Quest C", true, 1003, 3);
        assertEquals("Quest B", state.tick(4001, 3));
        assertEquals("Quest C", state.tick(7001, 3));
        assertEquals("", state.tick(10001, 3));
        assertEquals("Routine reminder", state.view(10002).popup);
        for (int i = 0; i < 20; i++) state.localQuest(revision, "Extra " + i, true, 10003 + i, 3);
        int emitted = 0;
        for (int i = 0; i < 30; i++) if (!state.tick(20000 + 3000L * i, 3).isEmpty()) emitted++;
        assertEquals(8, emitted);
    }

    @Test public void mutedDisabledAndPausedQuestsAreNotReplayedByWebsiteSync()
    {
        for (int mode = 0; mode < 3; mode++)
        {
            ProgressionFeedback state = new ProgressionFeedback();
            long revision = state.session("one");
            state.baseline(revision);
            if (mode == 0) state.categories(false, true, true);
            if (mode == 1) state.muted(true);
            assertEquals("", state.localQuest(revision, QUEST, mode != 2, 1000, 8));
            assertEquals(mode == 2 ? 0 : 1, state.recent().size());
            state.categories(true, true, true);
            state.muted(false);
            state.accept(revision, true, Collections.singletonList(event(QUEST)));
            assertEquals("", state.tick(300000));
            assertEquals(mode == 2 ? 0 : 1, state.recent().size());
        }
    }

    @Test public void logoutAndCharacterSwitchClearPopupAndRejectOldEvents()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long old = state.session("one");
        state.localQuest(old, QUEST, true, 1000, 8);
        long next = state.session("two");
        assertEquals("", state.view(1001).popup);
        assertTrue(state.recent().isEmpty());
        assertEquals("", state.localQuest(old, QUEST, true, 1002, 8));
        assertEquals(QUEST, state.localQuest(next, QUEST, true, 1003, 8));
        state.session("");
        assertEquals("", state.view(1004).popup);
        assertEquals("", state.localQuest(next, QUEST, true, 1005, 8));
    }

    @Test public void serverFirstPendingQuestIsPromotedWithoutDuplicateHistory()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.baseline(revision);
        state.routine("Routine");
        state.tick(1000);
        state.accept(revision, true, Collections.singletonList(event(QUEST)));
        assertEquals(QUEST, state.localQuest(revision, QUEST, true, 1001, 3));
        assertEquals(1, state.recent().size());
        assertEquals("", state.tick(4001, 3));
        assertEquals("Routine", state.view(4002).popup);
        assertEquals("", state.tick(7001, 3));
    }

    @Test public void serverFirstAlreadyVisibleQuestDoesNotReplayOrExtendItsDuration()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.baseline(revision);
        state.accept(revision, true, Collections.singletonList(event(QUEST)));
        state.tick(1000, 3);
        assertEquals("", state.localQuest(revision, QUEST, true, 1001, 20));
        assertEquals(4000, state.view(1002).popupUntil);
        assertEquals(1, state.recent().size());
        assertEquals("", state.tick(4000));
    }

    @Test public void localQuestDoesNotMuteOtherServerCompletionCategories()
    {
        ProgressionFeedback state = new ProgressionFeedback();
        long revision = state.session("one");
        state.baseline(revision);
        state.localQuest(revision, QUEST, true, 1000, 3);
        state.accept(revision, true, Arrays.asList(event(QUEST), new ProgressionFeedback.Event("diary:one", "Diary"),
            new ProgressionFeedback.Event("task:one", "Milestone")));
        assertEquals("Diary", state.tick(4000, 3));
        assertEquals("Milestone", state.tick(7000, 3));
        assertEquals(3, state.recent().size());
    }

    private static String parse(String title)
    {
        return QuestCompletionCapture.chatName(ChatMessageType.GAMEMESSAGE, "Congratulations, you've completed a quest: " + title);
    }

    private static ProgressionFeedback.Event event(String title)
    {
        return new ProgressionFeedback.Event("quest:" + title, title);
    }
}
