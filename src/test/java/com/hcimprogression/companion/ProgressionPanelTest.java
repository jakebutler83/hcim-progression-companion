package com.hcimprogression.companion;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** Component-only tests: never launches or interacts with RuneScape. */
public class ProgressionPanelTest
{
    @Test public void localGoalPanelExposesControlsWithoutAnyWebsiteConnection() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SessionGoalHelper helper = new SessionGoalHelper(null,null,new HcimProgressionCompanionConfig() {},null);
            HcimProgressionCompanionPanel panel = new HcimProgressionCompanionPanel(code -> {}, () -> {}, () -> {},null,helper.panel());
            List<Component> values = components(panel);
            for (String title : new String[]{"Start goal","Clear","Restart summary","Pause 15 min","Resume alerts"})
                assertTrue(values.stream().filter(JButton.class::isInstance).map(JButton.class::cast).anyMatch(button -> title.equals(button.getText())));
            assertNull(helper.view()); assertFalse(helper.muted(0));
        });
    }
    @Test public void recentCompletionsEscapeMarkupAndClearOnSessionChange() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            HcimProgressionCompanionPanel panel = new HcimProgressionCompanionPanel(code -> {}, () -> {}, () -> {});
            showDetails(panel);
            ProgressionFeedback feedback = new ProgressionFeedback();
            long session = feedback.session("hp");
            feedback.baseline(session);
            feedback.accept(session, true, java.util.Collections.singletonList(
                new ProgressionFeedback.Event("quest:test", "<img> A & B")));
            panel.showRecentCompletions(feedback.recent());
            assertTrue(components(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText() != null && label.getText().contains("A &amp; B")));
            assertFalse(components(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText() != null && label.getText().contains("<img>")));
            feedback.session("sw");
            panel.showRecentCompletions(feedback.recent());
            assertFalse(components(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText() != null && label.getText().contains("A &amp; B")));
        });
    }

    @Test public void notificationSubfiltersDoNotEnableNetworkFeaturesByDefault()
    {
        HcimProgressionCompanionConfig config = new HcimProgressionCompanionConfig() {};
        assertTrue(config.questCompletionPopups());
        assertTrue(config.diaryCompletionPopups());
        assertTrue(config.milestoneCompletionPopups());
        assertFalse(config.progressionPopupsEnabled());
        assertFalse(config.automaticAccountSyncEnabled());
        assertFalse(config.routineRemindersEnabled());
    }

    @Test public void previewRequiresEnabledLocalRemindersAndAUserLogin() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger previews = new AtomicInteger();
            HcimProgressionCompanionPanel panel = new HcimProgressionCompanionPanel(code -> {}, () -> {}, previews::incrementAndGet);
            showDetails(panel);
            JButton button = components(panel).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(value -> "Preview local reminder".equals(value.getText())).findFirst().get();
            assertFalse(button.isEnabled());
            panel.showProgressionDisplays(false, false, false, true, true, "Local only");
            assertTrue(button.isEnabled());
            button.doClick(0);
            assertEquals(1, previews.get());
            panel.showProgressionDisplays(false, false, false, true, false, "Logged out");
            button.doClick(0);
            assertEquals(1, previews.get());
        });
    }

    @Test public void panelExplainsMissingAutoSyncAndEscapesWebsiteGoalText() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            HcimProgressionCompanionPanel panel = new HcimProgressionCompanionPanel(code -> {}, () -> {}, () -> {});
            showDetails(panel);
            panel.showProgressionDisplays(true, false, true, false, true, "<img src='untrusted'> & goal");
            List<Component> values = components(panel);
            assertTrue(values.stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> "Needs auto sync".equals(label.getText())));
            assertTrue(values.stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .anyMatch(label -> label.getText().contains("& goal") && !label.getText().contains("<img")));
            assertFalse(values.stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> label.getText() != null && label.getText().contains("<img")));
        });
    }

    private static void showDetails(Container panel)
    {
        components(panel).stream().filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast)
            .filter(button -> "Details".equals(button.getText())).findFirst().get().doClick(0);
    }

    private static List<Component> components(Container parent)
    {
        List<Component> output = new ArrayList<>();
        for (Component component : parent.getComponents())
        {
            output.add(component);
            if (component instanceof Container) output.addAll(components((Container) component));
        }
        return output;
    }
}
