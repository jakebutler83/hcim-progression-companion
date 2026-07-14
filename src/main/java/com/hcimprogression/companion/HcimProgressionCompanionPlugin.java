package com.hcimprogression.companion;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.callback.ClientThread;

@PluginDescriptor(
    name = "HCIM Progression Companion",
    description = "Connects RuneLite to HCIM Progression.",
    tags = {"hcim", "group ironman", "progression", "location"}
)
public class HcimProgressionCompanionPlugin extends Plugin
{
    private static final Logger logger = LoggerFactory.getLogger(HcimProgressionCompanionPlugin.class);
    private static final String TOKEN_KEY = "deviceToken";
    private static final String DISPLAY_NAME_KEY = "linkedDisplayName";

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private HcimProgressionCompanionConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ItemManager itemManager;
    @Inject private HiscoreClient hiscoreClient;

    private final LocationService locationService = new LocationService();
    private final AccountSnapshotService accountSnapshotService = new AccountSnapshotService();
    private final CollectionLogCaptureService collectionLogCaptureService = new CollectionLogCaptureService();
    private final SyncService syncService = new SyncService();
    private HcimProgressionCompanionPanel panel;
    private NavigationButton navigationButton;
    private int tickCounter;
    private boolean syncInFlight;

    @Override
    protected void startUp()
    {
        tickCounter = 0;
        syncInFlight = false;
        panel = new HcimProgressionCompanionPanel(this::linkCompanion, this::syncAccountNow);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/hcim-companion-icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("HCIM Progression Companion")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        String token = deviceToken();
        if (token.isEmpty()) panel.showUnlinked();
        else panel.showLinked(configManager.getConfiguration(HcimProgressionCompanionConfig.GROUP, DISPLAY_NAME_KEY));

        logger.info("HCIM Progression Companion started.");
    }

    @Override
    protected void shutDown()
    {
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
        logger.info("HCIM Progression Companion stopped.");
    }

    private void linkCompanion(String code)
    {
        if (panel != null) panel.setLinking(true);
        syncService.exchangeCode(config.apiBaseUrl(), code, (result, error) ->
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error != null || result == null) {
                    panel.showLinkError(error);
                    return;
                }
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY, result.getToken());
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, DISPLAY_NAME_KEY, result.getDisplayName());
                panel.showLinked(result.getDisplayName());
            })
        );
    }


    private void syncAccountNow()
    {
        String token = deviceToken();
        if (token.isEmpty())
        {
            if (panel != null) panel.showAccountSyncError("Link the companion first");
            return;
        }

        if (panel != null) panel.setAccountSyncing(true);

        clientThread.invokeLater(() ->
        {
            try
            {
                if (client.getGameState() != GameState.LOGGED_IN)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if (panel != null) panel.showAccountSyncError("Log into the game first");
                    });
                    return;
                }

                AccountSnapshot snapshot = accountSnapshotService.createSnapshot(client, collectionLogCaptureService);
                if (snapshot == null)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        if (panel != null) panel.showAccountSyncError("Could not read account data");
                    });
                    return;
                }

                // Clue completion totals are not reliably present in Collection Log widgets.
                // RuneLite itself uses the official hiscores for !clues, so use the same source.
                hiscoreClient.lookupAsync(snapshot.getPlayerName(), HiscoreEndpoint.NORMAL)
                    .whenComplete((hiscoreResult, hiscoreError) ->
                    {
                        if (hiscoreResult != null)
                        {
                            applyHiscoreClueCounts(snapshot, hiscoreResult);
                        }
                        else
                        {
                            logger.warn("Could not fetch clue totals from hiscores; syncing captured data only", hiscoreError);
                        }

                        syncService.syncAccount(config.apiBaseUrl(), token, snapshot, (result, error) ->
                            SwingUtilities.invokeLater(() ->
                            {
                                if (panel == null) return;
                                if (error != null || result == null)
                                {
                                    panel.showAccountSyncError(error == null ? "Account sync failed" : error);
                                    return;
                                }
                                panel.showAccountSyncSuccess(result.getQuestUpdates(), result.getTaskUpdates());
                            })
                        );
                    });
            }
            catch (RuntimeException error)
            {
                logger.error("Account sync failed", error);
                SwingUtilities.invokeLater(() ->
                {
                    if (panel != null)
                    {
                        String message = error.getMessage();
                        panel.showAccountSyncError(message == null || message.isEmpty()
                            ? "Could not read account data" : message);
                    }
                });
            }
        });
    }

    private void applyHiscoreClueCounts(AccountSnapshot snapshot, HiscoreResult result)
    {
        putClueCount(snapshot, "beginner", result.getSkill(HiscoreSkill.CLUE_SCROLL_BEGINNER));
        putClueCount(snapshot, "easy", result.getSkill(HiscoreSkill.CLUE_SCROLL_EASY));
        putClueCount(snapshot, "medium", result.getSkill(HiscoreSkill.CLUE_SCROLL_MEDIUM));
        putClueCount(snapshot, "hard", result.getSkill(HiscoreSkill.CLUE_SCROLL_HARD));
        putClueCount(snapshot, "elite", result.getSkill(HiscoreSkill.CLUE_SCROLL_ELITE));
        putClueCount(snapshot, "master", result.getSkill(HiscoreSkill.CLUE_SCROLL_MASTER));
    }

    private void putClueCount(AccountSnapshot snapshot, String tier, Skill skill)
    {
        int count = skill == null ? 0 : skill.getLevel();
        snapshot.getClueCounts().put(tier, Math.max(0, count));
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST)
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            CollectionLogCaptureService.CaptureResult result =
                collectionLogCaptureService.captureCurrentPage(client, itemManager);
            if (result != null && panel != null)
            {
                SwingUtilities.invokeLater(() -> panel.showCollectionLogCapture(
                    result.getPageTitle(),
                    result.getPageItems(),
                    result.getTotalPages(),
                    result.getTotalItems(),
                    collectionLogCaptureService.getClueCounts()
                ));
            }
        });
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            if (panel != null) SwingUtilities.invokeLater(panel::showLoggedOut);
            return;
        }

        tickCounter++;
        if (tickCounter < 5) return;
        tickCounter = 0;

        PlayerState state = locationService.createPlayerState(client);
        if (state == null) return;
        String playerName = state.getPlayerName() == null ? "Unknown" : state.getPlayerName();

        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> {
                if (config.locationSharingEnabled()) panel.showSharingEnabled();
                else panel.showSharingDisabled();
                panel.updatePlayerInformation(
                    playerName, state.getWorld(), state.getRegionId(),
                    state.getX(), state.getY(), state.getPlane()
                );
            });
        }

        String token = deviceToken();
        if (!config.locationSharingEnabled() || token.isEmpty() || syncInFlight) return;

        syncInFlight = true;
        syncService.syncLocation(config.apiBaseUrl(), token, state, error -> {
            syncInFlight = false;
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null) panel.showSyncSuccess();
                else panel.showSyncError(error);
            });
        });
    }

    private String deviceToken()
    {
        String value = configManager.getConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY);
        return value == null ? "" : value.trim();
    }

    @Provides
    HcimProgressionCompanionConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(HcimProgressionCompanionConfig.class);
    }
}
