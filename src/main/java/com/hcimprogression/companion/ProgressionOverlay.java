package com.hcimprogression.companion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class ProgressionOverlay extends OverlayPanel
{
    private final ProgressionFeedback feedback;
    private final HcimProgressionCompanionConfig config;
    private final Client client;
    private static final Color GOLD = new Color(235, 194, 93);

    ProgressionOverlay(HcimProgressionCompanionPlugin plugin, ProgressionFeedback feedback,
        HcimProgressionCompanionConfig config, Client client)
    {
        super(plugin);
        this.feedback = feedback;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(240, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;
        ProgressionFeedback.View view = feedback.view(System.currentTimeMillis());
        boolean popup = !view.popup.isEmpty() && (view.routinePopup ? config.routineRemindersEnabled() : config.progressionPopupsEnabled());
        boolean goal = config.pinnedGoalEnabled() && view.goal != null;
        if (!popup && !goal) return null;
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("Progression Path").color(GOLD).build());
        if (popup)
        {
            line(view.routinePopup ? "Routine ready to check" : view.popupCategory + " complete!", Color.GREEN);
            wrapped(view.popup);
        }
        if (goal)
        {
            line(view.goal.done == view.goal.total ? "Pinned task complete" : "Your pinned task", GOLD);
            wrapped(view.goal.title);
            line(view.goal.done + " / " + view.goal.total + " steps", Color.LIGHT_GRAY);
        }
        return super.render(graphics);
    }

    private void wrapped(String text)
    {
        for (String part : ProgressionFeedback.wrap(text, 27)) line(part, Color.WHITE);
    }

    private void line(String text, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(text).leftColor(color).build());
    }
}
