package com.hcimprogression.companion;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Visual marks only, restricted to the normal bank's item layer. */
final class SlayerBankOverlay extends WidgetItemOverlay
{
    private static final Color FILL = new Color(235, 194, 93, 45);
    private static final Color BORDER = new Color(235, 194, 93);
    private final Client client;
    private final SlayerPreparationHelper helper;
    SlayerBankOverlay(Client client, SlayerPreparationHelper helper)
    {
        this.client = client; this.helper = helper;
        drawAfterLayer(InterfaceID.Bankmain.ITEMS);
    }
    @Override public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        // The registered draw layer is Bankmain.ITEMS, not the game-frame root
        // and not Group Storage. A bank is a child interface of the game frame.
        if (client.getGameState() != GameState.LOGGED_IN || widgetItem.getQuantity() <= 0
            || !helper.highlights().contains(itemId)) return;
        Rectangle bounds = widgetItem.getCanvasBounds();
        Color previous = graphics.getColor();
        graphics.setColor(FILL); graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.setColor(BORDER); graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        graphics.setColor(previous);
    }
}
