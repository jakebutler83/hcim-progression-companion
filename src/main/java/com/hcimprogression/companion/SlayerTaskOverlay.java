package com.hcimprogression.companion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;

/** Compact, movable top-left panel. Only user-started assignment progress. */
final class SlayerTaskOverlay extends OverlayPanel
{
    private final Client client;
    private final SlayerTaskProgress progress;
    SlayerTaskOverlay(HcimProgressionCompanionPlugin plugin, Client client, SlayerTaskProgress progress)
    {
        super(plugin); this.client = client; this.progress = progress;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(185,0));
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;
        SlayerTaskProgress.View view = progress.view(System.currentTimeMillis());
        if (view == null) return null;
        panelComponent.getChildren().clear();
        String title = view.task.length() > 27 ? view.task.substring(0,26) + "…" : view.task;
        panelComponent.getChildren().add(LineComponent.builder().left(title).leftColor(SidebarControls.GOLD).build());
        panelComponent.getChildren().add(LineComponent.builder().left(view.remaining == 0 ? "Task complete" : "Task progress")
            .right(view.done + " / " + view.total).leftColor(Color.LIGHT_GRAY).rightColor(Color.WHITE).build());
        ProgressBarComponent bar = new ProgressBarComponent();
        bar.setMaximum(100); bar.setValue(view.percent);
        bar.setForegroundColor(view.remaining == 0 ? new Color(104,211,145) : SidebarControls.GOLD);
        panelComponent.getChildren().add(bar);
        panelComponent.getChildren().add(LineComponent.builder().left(view.remaining + " remaining").leftColor(Color.LIGHT_GRAY).build());
        return super.render(graphics);
    }
}
