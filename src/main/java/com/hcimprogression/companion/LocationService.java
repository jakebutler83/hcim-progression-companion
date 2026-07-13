package com.hcimprogression.companion;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class LocationService
{
    public PlayerState createPlayerState(Client client)
    {
        Player player = client.getLocalPlayer();

        if (player == null)
        {
            return null;
        }

        WorldPoint point = player.getWorldLocation();

        if (point == null)
        {
            return null;
        }

        PlayerState state = new PlayerState();

        state.setPlayerName(player.getName());
        state.setWorld(client.getWorld());
        state.setRegionId(point.getRegionID());
        state.setX(point.getX());
        state.setY(point.getY());
        state.setPlane(point.getPlane());
        state.setTimestamp(System.currentTimeMillis());

        return state;
    }
}