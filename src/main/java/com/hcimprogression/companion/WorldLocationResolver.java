package com.hcimprogression.companion;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

final class WorldLocationResolver
{
    private WorldLocationResolver()
    {
    }

    static WorldPoint resolve(Client client, Player player)
    {
        WorldPoint worldPoint = player == null ? null : player.getWorldLocation();
        if (player == null || !client.isInInstancedRegion())
        {
            return worldPoint;
        }

        LocalPoint localPoint = player.getLocalLocation();
        if (localPoint == null)
        {
            return worldPoint;
        }

        WorldPoint templatePoint = WorldPoint.fromLocalInstance(client, localPoint);
        return templatePoint == null ? worldPoint : templatePoint;
    }
}
