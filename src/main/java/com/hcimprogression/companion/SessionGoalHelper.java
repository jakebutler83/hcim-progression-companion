package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.Locale;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;

/** Entirely local session goals. Swing captures intent; only the client thread reads game state. */
final class SessionGoalHelper
{
    private final Client client;
    private final ClientThread clientThread;
    private final HcimProgressionCompanionConfig config;
    private final Notifier notifier;
    private final SkillGoalSync website;
    private final SkillSessionGoal goal = new SkillSessionGoal();
    private final SessionSummary summary = new SessionSummary();
    private final JPanel panel = SidebarControls.column();
    private final JComboBox<Skill> skill = new JComboBox<>(Arrays.stream(Skill.values())
        .filter(value -> value != Skill.OVERALL).toArray(Skill[]::new));
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Gain XP this session", "Reach a level"});
    private final JTextField amount = new JTextField("10000");
    private final JTextArea status = SidebarControls.text("");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JTextArea sessionStats = SidebarControls.text("");
    private final JTextArea muteStatus = SidebarControls.text("");
    private final JTextArea syncStatus = SidebarControls.text("Website sync off · goals stay local");
    private volatile String session = "";
    private volatile long generation;
    private volatile View view;
    private String label = "";
    private long popupUntil;
    private volatile long muteUntil;
    private long nextSummaryAt;

    SessionGoalHelper(Client client, ClientThread clientThread, HcimProgressionCompanionConfig config, Notifier notifier)
    {
        this(client,clientThread,config,notifier,null);
    }
    SessionGoalHelper(Client client, ClientThread clientThread, HcimProgressionCompanionConfig config, Notifier notifier, SkillGoalSync website)
    {
        this.client = client; this.clientThread = clientThread; this.config = config; this.notifier = notifier; this.website = website;
        SidebarControls.field(skill); SidebarControls.field(mode); SidebarControls.field(amount); SidebarControls.stretch(progress);
        progress.setFont(SidebarControls.FONT.deriveFont(11f));
        skill.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,30)); mode.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,30));
        amount.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,28)); progress.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,22));
        panel.add(SidebarControls.text("Choose skill and target")); panel.add(skill); SidebarControls.gap(panel); panel.add(mode);
        SidebarControls.gap(panel); panel.add(SidebarControls.text("XP to earn / target level")); panel.add(amount); SidebarControls.gap(panel);
        JButton start = SidebarControls.button("Start goal","Start a local skill goal",true), clear = SidebarControls.button("Clear","Clear the current skill goal",false);
        panel.add(SidebarControls.row(start,clear)); SidebarControls.gap(panel); progress.setStringPainted(true); panel.add(progress);
        SidebarControls.gap(panel); panel.add(status); SidebarControls.gap(panel);
        JPanel statsPanel = SidebarControls.column(); statsPanel.add(sessionStats); SidebarControls.gap(statsPanel);
        JButton resetStats = SidebarControls.button("Restart summary","Reset session statistics; keep the active goal",false); statsPanel.add(resetStats);
        panel.add(SidebarControls.fold("Session summary",statsPanel)); SidebarControls.gap(panel);
        JButton mute = SidebarControls.button("Pause 15 min","Mute this plugin's pop-ups and desktop alerts for 15 minutes",false);
        JButton unmute = SidebarControls.button("Resume alerts","Unmute this plugin's alerts",false);
        JPanel alerts = SidebarControls.column(); alerts.add(muteStatus); SidebarControls.gap(alerts); alerts.add(mute); SidebarControls.gap(alerts); alerts.add(unmute);
        panel.add(SidebarControls.fold("Notifications",alerts)); SidebarControls.gap(panel);
        JPanel sync = SidebarControls.column(); sync.add(syncStatus); SidebarControls.gap(sync);
        JButton send = SidebarControls.button("Send current goal","Send or retry the current goal; enable Account tracking or advanced Skill goal website sync",false); sync.add(send);
        send.addActionListener(event -> { if (website != null) clientThread.invokeLater(() -> { website.request(); publish(""); }); });
        panel.add(SidebarControls.fold("Website sync",sync));
        mute.addActionListener(event -> clientThread.invokeLater(() -> { muteUntil = System.currentTimeMillis() + 15 * 60_000L; popupUntil = 0; publish(""); }));
        unmute.addActionListener(event -> clientThread.invokeLater(() -> { muteUntil = 0; publish(""); }));
        resetStats.addActionListener(event -> {
            String expected = session; long revision = generation;
            clientThread.invokeLater(() -> {
                if (current(expected, revision) && config.sessionSummaryEnabled()) { startSummary(); publish(""); }
            });
        });
        start.addActionListener(event -> {
            Skill selected = (Skill) skill.getSelectedItem(); boolean level = mode.getSelectedIndex() == 1;
            String input = amount.getText().trim().replace(",", "");
            String expected = session; long revision = generation;
            clientThread.invokeLater(() -> {
                if (!current(expected, revision)) return;
                if (!config.sessionSkillGoalsEnabled()) { publish("Set In-game helpers to On, or enable Local skill goals under Advanced: helpers."); return; }
                try
                {
                    int xp = client.getSkillExperience(selected);
                    goal.start(selected, xp, SkillSessionGoal.target(xp, level, Integer.parseInt(input)));
                    if (website != null) website.start(selected.getName(),level,level ? Integer.parseInt(input) : 0,xp,SkillSessionGoal.target(xp,level,Integer.parseInt(input)));
                    label = selected.getName() + (level ? " · level " + input : " · +" + format(goal.required()) + " XP");
                    popupUntil = 0; publish("");
                }
                catch (NumberFormatException invalid) { publish("Enter a whole number, such as 10000 or 70."); }
                catch (IllegalArgumentException invalid) { publish(invalid.getMessage()); }
            });
        });
        clear.addActionListener(event -> {
            String expected = session; long revision = generation;
            clientThread.invokeLater(() -> { if (current(expected, revision)) { if (website != null) website.stop(); goal.clear(); view = null; popupUntil = 0; publish(""); } });
        });
        publish("");
    }

    JPanel panel() { return panel; }
    void session(String name)
    {
        String next = name;
        boolean changed = !next.equals(session);
        if (changed) { session = next; generation++; goal.clear(); summary.clear(); view = null; popupUntil = 0; }
        boolean update = changed;
        if (!config.sessionSkillGoalsEnabled() && goal.skill() != null) { goal.clear(); view = null; popupUntil = 0; update = true; }
        if (config.sessionSummaryEnabled() && !session.isEmpty() && !summary.active()) { startSummary(); update = true; }
        else if (!config.sessionSummaryEnabled() && summary.active()) { summary.clear(); update = true; }
        if (update) publish("");
    }
    void reset() { session = ""; generation++; goal.clear(); summary.clear(); view = null; popupUntil = 0; if (website != null) website.reset(); publish(""); }
    void connection(String name,String token,String api) { if (website != null) website.connection(name,token,api); }

    private boolean current(String expected, long revision)
    {
        if (expected.isEmpty() || revision != generation || !expected.equals(session)
            || client.getGameState() != GameState.LOGGED_IN
            || client.getLocalPlayer() == null || !expected.equals(client.getLocalPlayer().getName()))
        {
            if (revision == generation) publish("Log into the character before starting.");
            return false;
        }
        return true;
    }

    void changed(Skill changed, int xp)
    {
        if (!config.sessionSummaryEnabled() && !config.sessionSkillGoalsEnabled()) return;
        if (session.isEmpty() || !current(session, generation)) return;
        boolean summaryChanged = config.sessionSummaryEnabled() && summary.update(changed, xp);
        if (!config.sessionSkillGoalsEnabled() || goal.skill() != changed) { if (summaryChanged) publish(""); return; }
        int previous = goal.gained();
        boolean completed = goal.update(changed, xp);
        if (website != null) { if (goal.skill() == null) website.stop(); else website.progress(xp,completed); }
        if (completed)
        {
            long now = System.currentTimeMillis();
            popupUntil = muted(now) ? 0 : now + Math.max(3, Math.min(20, config.progressionPopupSeconds())) * 1000L;
            summary.goalCompleted();
            if (!muted(now) && config.skillGoalDesktopNotifications()) notifier.notify("Progression Path: " + label + " — goal complete!");
        }
        if (summaryChanged || previous != goal.gained() || completed || goal.skill() == null) publish("");
    }

    private void publish(String error)
    {
        long revision = generation;
        int percent = goal.percent();
        String text = !error.isEmpty() ? error : session.isEmpty()
            ? "Set In-game helpers to On and log in. Goals reset on logout or character/profile change."
            : !config.sessionSkillGoalsEnabled() ? "Set In-game helpers to On. Individual choices are under Advanced: helpers; no website link required."
            : goal.skill() == null ? "Choose a skill and XP gain or target level. Local only; no website link required."
            : label + " — " + format(goal.gained()) + " / " + format(goal.required()) + " XP earned · "
                + (goal.complete() ? "Goal complete!" : format(goal.remaining()) + " XP remaining");
        if (goal.skill() == null) view = null;
        else view = new View(label, format(goal.gained()) + " / " + format(goal.required()) + " XP · " + percent + "%",
            percent, goal.complete(), popupUntil);
        long now = System.currentTimeMillis();
        String stats = !summary.active() ? "Set In-game helpers to On for a local session summary. Resets on logout; nothing is uploaded."
            : String.format(Locale.US, "%,d XP · %,d levels\n%,d skill goals · %d min", summary.xp(), summary.levels(), summary.goals(), summary.minutes(now));
        String muteText = muted(now) ? "Pop-ups muted · " + Math.max(1, (muteUntil - now + 59_999L) / 60_000L) + " min" : "Pop-ups unmuted";
        SwingUtilities.invokeLater(() -> {
            if (revision != generation) return;
            progress.setValue(percent); progress.setString(percent + "%"); message(text);
            sessionStats.setText(stats);
            muteStatus.setText(muteText);
            syncStatus.setText(website == null ? "Website sync unavailable" : website.status());
        });
    }

    private void startSummary()
    {
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill value : Skill.values()) if (value != Skill.OVERALL) xp.put(value, client.getSkillExperience(value));
        summary.start(xp, System.currentTimeMillis());
    }
    void tick(long now)
    {
        if (website != null) website.tick(now);
        if (now >= nextSummaryAt) { nextSummaryAt = now + 10_000L; publish(""); }
    }
    boolean muted(long now) { return now < muteUntil; }

    private void message(String text)
    {
        status.setText(text);
    }
    static String format(int value) { return String.format(Locale.US, "%,d", value); }
    View view() { return config.sessionSkillGoalsEnabled() && !session.isEmpty() ? view : null; }
    static final class View
    {
        final String title, progress;
        final int percent;
        final boolean complete;
        final long popupUntil;
        View(String title, String progress, int percent, boolean complete, long popupUntil)
        { this.title = title; this.progress = progress; this.percent = percent; this.complete = complete; this.popupUntil = popupUntil; }
    }
}
