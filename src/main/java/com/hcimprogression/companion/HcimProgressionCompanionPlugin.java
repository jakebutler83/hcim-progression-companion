package com.hcimprogression.companion;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

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

	@Inject
	private ClientToolbar clientToolbar;

	private HcimProgressionCompanionPanel panel;
	private NavigationButton navigationButton;
	private int tickCounter;
	private final LocationService locationService = new LocationService();
	@Override
	protected void startUp()
	{
		tickCounter = 0;

		panel = new HcimProgressionCompanionPanel();

		BufferedImage icon = ImageUtil.loadImageResource(
				getClass(),
				"/hcim-companion-icon.png"
		);

		navigationButton = NavigationButton.builder()
				.tooltip("HCIM Progression Companion")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);

		logger.info("HCIM Progression Companion started.");
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}

		panel = null;
		navigationButton = null;

		logger.info("HCIM Progression Companion stopped.");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			if (panel != null)
			{
				SwingUtilities.invokeLater(panel::showLoggedOut);
			}

			return;
		}

		tickCounter++;

		// Update roughly every three seconds.
		if (tickCounter < 5)
		{
			return;
		}

		tickCounter = 0;

		PlayerState state = locationService.createPlayerState(client);

		if (state == null)
		{
			return;
		}

		String playerName = state.getPlayerName() == null
				? "Unknown"
				: state.getPlayerName();

		logger.info(
				"Player: {} | World: {} | X: {} | Y: {} | Plane: {} | Region ID: {}",
				playerName,
				state.getWorld(),
				state.getX(),
				state.getY(),
				state.getPlane(),
				state.getRegionId()
		);

		if (panel != null)
		{
			SwingUtilities.invokeLater(() ->
					panel.updatePlayerInformation(
							playerName,
							state.getWorld(),
							state.getRegionId(),
							state.getX(),
							state.getY(),
							state.getPlane()
					)
			);
		}
	}
	@Provides
	HcimProgressionCompanionConfig provideConfig(
			ConfigManager configManager)
	{
		return configManager.getConfig(
				HcimProgressionCompanionConfig.class
		);
	}
}