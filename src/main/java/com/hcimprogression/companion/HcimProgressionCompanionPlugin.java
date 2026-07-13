package com.hcimprogression.companion;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
		name = "HCIM Progression Companion",
		description = "Reads player information for HCIM Progression.",
		tags = {"hcim", "group ironman", "progression", "location"}
)
public class HcimProgressionCompanionPlugin extends Plugin
{
	private static final Logger logger =
			LoggerFactory.getLogger(HcimProgressionCompanionPlugin.class);

	@Inject
	private Client client;

	private int tickCounter;

	@Override
	protected void startUp()
	{
		tickCounter = 0;
		logger.info("HCIM Progression Companion started.");
	}

	@Override
	protected void shutDown()
	{
		logger.info("HCIM Progression Companion stopped.");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player player = client.getLocalPlayer();

		if (player == null)
		{
			return;
		}

		tickCounter++;

		if (tickCounter < 5)
		{
			return;
		}

		tickCounter = 0;

		WorldPoint location = player.getWorldLocation();

		if (location == null)
		{
			return;
		}

		logger.info(
				"Player: {} | World: {} | X: {} | Y: {} | Plane: {} | Region ID: {}",
				player.getName(),
				client.getWorld(),
				location.getX(),
				location.getY(),
				location.getPlane(),
				location.getRegionID()
		);
	}

	@Provides
	HcimProgressionCompanionConfig provideConfig(
			ConfigManager configManager)
	{
		return configManager.getConfig(
				HcimProgressionCompanionConfig.class);
	}
}