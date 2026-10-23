package com.hcimprogression.companion;

import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/** A separate toast, so a quest celebration never shifts pinned goals or skill bars. */
final class QuestCompletionOverlay extends Overlay
{
    private final Client client;
    private final ProgressionFeedback feedback;
    private final HcimProgressionCompanionConfig config;
    private final SessionGoalHelper skills;
    private final QuestCompletionBanner banner = new QuestCompletionBanner();

    QuestCompletionOverlay(HcimProgressionCompanionPlugin plugin, Client client, ProgressionFeedback feedback,
        HcimProgressionCompanionConfig config, SessionGoalHelper skills)
    {
        super(plugin);
        this.client = client;
        this.feedback = feedback;
        this.config = config;
        this.skills = skills;
        setPosition(OverlayPosition.TOP_CENTER);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        long now = System.currentTimeMillis();
        ProgressionFeedback.View view = feedback.view(now);
        if (!visible(view, client.getGameState() == GameState.LOGGED_IN,
            config.progressionPopupsEnabled(), config.questCompletionPopups(), skills != null && skills.muted(now))) return null;
        int width = Math.min(340, client.getViewportWidth() - 20);
        if (width < QuestCompletionBanner.MIN_WIDTH) return null;
        return banner.render(graphics, view.popup, width, view.popupStarted, view.popupUntil, now);
    }

    static boolean visible(ProgressionFeedback.View view, boolean loggedIn, boolean enabled, boolean quests, boolean muted)
    {
        return loggedIn && enabled && quests && !muted && view.questPopup();
    }
}
