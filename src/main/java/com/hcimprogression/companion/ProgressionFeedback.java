package com.hcimprogression.companion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Bounded, session-scoped presentation state. No game or network work here. */
final class ProgressionFeedback
{
    static final long POPUP_MILLIS = 8_000L;
    private String session = "";
    private long generation;
    private boolean baseline;
    private final Set<String> seen = new LinkedHashSet<>();
    private final ArrayDeque<Popup> pending = new ArrayDeque<>();
    private final Set<String> localQuests = new LinkedHashSet<>();
    private final Set<String> announcedServerQuests = new LinkedHashSet<>();
    private final ArrayDeque<Recent> recent = new ArrayDeque<>();
    private long recentRevision;
    private boolean quests = true, diaries = true, milestones = true;
    private String popup = "";
    private String popupCategory = "";
    private long popupStarted;
    private long popupUntil;
    private boolean routinePopup;
    private boolean muted;
    private Goal goal;
    private long goalUntil;

    synchronized long session(String next)
    {
        if (!session.equals(next))
        {
            session = next;
            generation++;
            baseline = false;
            seen.clear();
            localQuests.clear();
            announcedServerQuests.clear();
            pending.clear();
            recent.clear();
            recentRevision++;
            popup = "";
            goal = null;
        }
        return generation;
    }

    synchronized boolean current(long revision)
    {
        return !session.isEmpty() && revision == generation;
    }

    synchronized void baseline(long revision)
    {
        if (current(revision)) baseline = true;
    }

    synchronized void accept(long revision, boolean eligible, List<Event> events)
    {
        if (!current(revision)) return;
        boolean announce = baseline && eligible;
        baseline = true;
        if (events == null) return;
        int count = 0;
        for (Event event : events)
        {
            if (++count > 20) break;
            if (event == null || event.id == null || event.id.length() > 160) continue;
            String title = clean(event.title);
            if (title.isEmpty() || !seen.add(event.id)) continue;
            if (seen.size() > 512) seen.remove(seen.iterator().next());
            String category = category(event.id);
            // A local quest already celebrated (or intentionally muted) must
            // not replay when its normal, rate-limited website upload returns.
            if ("Quest".equals(category) && localQuests.contains(QuestCompletionCapture.key(title))) continue;
            if (announce)
            {
                if ("Quest".equals(category)) remember(announcedServerQuests, QuestCompletionCapture.key(title));
                recent(title, category, System.currentTimeMillis());
                if (!muted && enabled(category) && pending.size() < 8) pending.add(new Popup(title, false, category));
            }
        }
    }

    /** Proven local game event: no website baseline, upload or polling dependency. */
    synchronized String localQuest(long revision, String title, boolean eligible, long now, int seconds)
    {
        if (!current(revision)) return "";
        title = clean(title);
        if (title.isEmpty()) return "";
        String key = QuestCompletionCapture.key(title);
        if (localQuests.contains(key)) return "";
        remember(localQuests, key);
        boolean alreadyAnnounced = announcedServerQuests.contains(key);
        if (eligible && !alreadyAnnounced) recent(title, "Quest", now);
        if (!eligible || muted || !quests) return "";

        // Handle even the unusual server-first race without a second history
        // entry: promote its pending banner, or keep its already-visible one.
        boolean wasPending = pending.removeIf(value -> "Quest".equals(value.category)
            && QuestCompletionCapture.key(value.text).equals(key));
        if (alreadyAnnounced && !wasPending) return "";
        Popup next = new Popup(title, false, "Quest");
        if (!popup.isEmpty() && now < popupUntil)
        {
            if ("Quest".equals(popupCategory))
            {
                ArrayDeque<Popup> prioritized = new ArrayDeque<>();
                while (!pending.isEmpty() && "Quest".equals(pending.peek().category)) prioritized.add(pending.poll());
                prioritized.add(next);
                prioritized.addAll(pending);
                pending.clear();
                pending.addAll(prioritized);
                if (pending.size() > 8) pending.removeLast();
                return "";
            }
            // Quest celebrations get immediate priority, but an interrupted
            // routine/milestone resumes without a second desktop notification.
            if (pending.size() >= 8) pending.removeLast();
            pending.addFirst(new Popup(popup, routinePopup, popupCategory, false));
        }
        show(next, now, seconds);
        return popup;
    }

    private static void remember(Set<String> values, String value)
    {
        values.add(value);
        if (values.size() > 512) values.remove(values.iterator().next());
    }

    private void recent(String title, String category, long now)
    {
        recent.addFirst(new Recent(title, category, now));
        if (recent.size() > 10) recent.removeLast();
        recentRevision++;
    }

    /** Local presentation filters; changing them never replays or resets completion history. */
    synchronized void categories(boolean quests, boolean diaries, boolean milestones)
    {
        this.quests = quests;
        this.diaries = diaries;
        this.milestones = milestones;
        pending.removeIf(value -> !value.routine && !enabled(value.category));
        if (!routinePopup && !enabled(popupCategory)) popup = "";
    }

    private boolean enabled(String category)
    {
        return "Quest".equals(category) ? quests : "Diary".equals(category) ? diaries : milestones;
    }

    private static String category(String id)
    {
        return id.startsWith("quest:") ? "Quest" : id.startsWith("diary:") ? "Diary" : "Milestone";
    }

    synchronized List<Recent> recent() { return Collections.unmodifiableList(new ArrayList<>(recent)); }
    synchronized long recentRevision() { return recentRevision; }

    synchronized void routine(String message)
    {
        String text = clean(message);
        if (!muted && !session.isEmpty() && !text.isEmpty() && pending.size() < 8) pending.add(new Popup(text, true, "Routine"));
    }

    synchronized void muted(boolean next)
    {
        muted = next;
        if (muted) { pending.clear(); popup = ""; }
    }

    synchronized void goal(long revision, Goal next, long now)
    {
        if (!current(revision)) return;
        if (next == null || clean(next.title).isEmpty() || (!next.activity && next.total < 1) || next.total < 0 || next.total > 40)
        {
            goal = null;
            return;
        }
        goal = new Goal(clean(next.title), Math.max(0, Math.min(next.done, next.total)), next.total);
        goal.activity = next.activity;
        goalUntil = now + 90_000L;
    }

    /** Advances only on game ticks; the overlay just reads an immutable view. */
    synchronized String tick(long now)
    {
        return tick(now, 8);
    }

    synchronized String tick(long now, int seconds)
    {
        if (muted) return "";
        if (now < popupUntil && !popup.isEmpty()) return "";
        Popup next = pending.poll();
        show(next, now, seconds);
        return next != null && next.notify ? popup : "";
    }

    private void show(Popup next, long now, int seconds)
    {
        popup = next == null ? "" : next.text;
        popupCategory = next == null ? "" : next.category;
        routinePopup = next != null && next.routine;
        popupStarted = now;
        popupUntil = now + Math.max(3, Math.min(20, seconds)) * 1000L;
    }

    synchronized View view(long now)
    {
        return new View(now < goalUntil ? goal : null, now < popupUntil ? popup : "", routinePopup, popupCategory,
            popupStarted, popupUntil);
    }

    static String clean(String value)
    {
        if (value == null) return "";
        String text = value.replaceAll("[<>\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return text.substring(0, Math.min(text.length(), 100));
    }

    static final class Event
    {
        String id;
        String title;
        Event(String id, String title) { this.id = id; this.title = title; }
    }

    static final class Goal
    {
        String title;
        int done;
        int total;
        boolean activity;
        Goal(String title, int done, int total) { this.title = title; this.done = done; this.total = total; }
    }

    static final class Response
    {
        boolean ok;
        Goal goal;
        List<Event> completions = Collections.emptyList();
    }

    static final class View
    {
        final Goal goal;
        final String popup;
        final boolean routinePopup;
        final String popupCategory;
        final long popupStarted;
        final long popupUntil;
        View(Goal goal, String popup, boolean routinePopup, String popupCategory, long popupStarted, long popupUntil)
        {
            this.goal = goal;
            this.popup = popup;
            this.routinePopup = routinePopup;
            this.popupCategory = popupCategory;
            this.popupStarted = popupStarted;
            this.popupUntil = popupUntil;
        }

        boolean questPopup() { return !routinePopup && "Quest".equals(popupCategory) && !popup.isEmpty(); }
    }

    private static final class Popup
    {
        final String text;
        final boolean routine;
        final String category;
        final boolean notify;
        Popup(String text, boolean routine, String category) { this(text, routine, category, true); }
        Popup(String text, boolean routine, String category, boolean notify)
        { this.text = text; this.routine = routine; this.category = category; this.notify = notify; }
    }

    static final class Recent
    {
        final String title;
        final String category;
        final long completedAt;
        Recent(String title, String category, long completedAt)
        {
            this.title = title;
            this.category = category;
            this.completedAt = completedAt;
        }
    }

    static List<String> wrap(String text, int columns)
    {
        List<String> lines = new ArrayList<>();
        String remaining = clean(text);
        while (remaining.length() > columns)
        {
            int end = remaining.lastIndexOf(' ', columns);
            if (end <= 0) end = columns;
            lines.add(remaining.substring(0, end));
            remaining = remaining.substring(end).trim();
        }
        if (!remaining.isEmpty()) lines.add(remaining);
        return lines;
    }
}
