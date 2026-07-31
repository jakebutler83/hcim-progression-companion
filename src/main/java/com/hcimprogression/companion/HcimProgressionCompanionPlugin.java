package com.hcimprogression.companion;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
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
    description = "Connects RuneLite to Progression Path for progress, Group Storage, location, Social Hub presence, clan rosters, and clan events.",
    tags = {"hcim", "group ironman", "progression", "group storage", "bank", "location", "social", "friends", "equipment", "clan", "events"}
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
    private final SocialPresenceService socialPresenceService = new SocialPresenceService();
    private final SocialClanService socialClanService = new SocialClanService();
    private final SocialClanEventService socialClanEventService = new SocialClanEventService();
    private final AccountSnapshotService accountSnapshotService = new AccountSnapshotService();
    private final CollectionLogCaptureService collectionLogCaptureService = new CollectionLogCaptureService();
    private final GroupStorageSnapshotService groupStorageSnapshotService = new GroupStorageSnapshotService();
    @Inject private SyncService syncService;
    private HcimProgressionCompanionPanel panel;
    private NavigationButton navigationButton;
    private int tickCounter;
    private int presenceCycleCounter;
    private int clanCycleCounter;
    private boolean syncInFlight;
    private boolean presenceSyncInFlight;
    private boolean clanSyncInFlight;
    private volatile boolean clanEventsSyncInFlight;
    private volatile String lastClanEventsFingerprint = "";
    private boolean clanEventsWidgetOpen;
    private GroupStorageSnapshot pendingGroupStorageSnapshot;
    private boolean groupStorageDirty;
    private int groupStorageDebounceTicks;
    private volatile boolean groupStorageSyncInFlight;

    @Override
    protected void startUp()
    {
        tickCounter = 0;
        presenceCycleCounter = 0;
        clanCycleCounter = 10;
        syncInFlight = false;
        presenceSyncInFlight = false;
        clanSyncInFlight = false;
        clanEventsSyncInFlight = false;
        lastClanEventsFingerprint = "";
        clanEventsWidgetOpen = false;
        pendingGroupStorageSnapshot = null;
        groupStorageDirty = false;
        groupStorageDebounceTicks = 0;
        groupStorageSyncInFlight = false;
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
        if (config.groupStorageSyncEnabled()) panel.showGroupStorageWaiting();
        else panel.showGroupStorageDisabled();

        logger.info("HCIM Progression Companion started.");
    }

    @Override
    protected void shutDown()
    {
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
        clanEventsSyncInFlight = false;
        lastClanEventsFingerprint = "";
        clanEventsWidgetOpen = false;
        pendingGroupStorageSnapshot = null;
        groupStorageDirty = false;
        groupStorageSyncInFlight = false;
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
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.INV_GROUP_TEMP
            && client.getTopLevelInterfaceId() == InterfaceID.SHARED_BANK)
        {
            captureGroupStorage(event.getItemContainer());
        }

        if (event.getContainerId() != InventoryID.WORN)
        {
            return;
        }

        socialPresenceService.updateWornEquipment(event.getItemContainer());
        // Request a fresh presence snapshot on the next eligible game tick.
        presenceCycleCounter = 2;
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.SHARED_BANK)
        {
            captureGroupStorage(client.getItemContainer(InventoryID.INV_GROUP_TEMP));
        }
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event)
    {
        if (!event.isGuest())
        {
            clanCycleCounter = 10;
        }
    }

    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined event)
    {
        clanCycleCounter = 10;
    }

    @Subscribe
    public void onClanMemberLeft(ClanMemberLeft event)
    {
        clanCycleCounter = 10;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            if (panel != null) SwingUtilities.invokeLater(panel::showLoggedOut);
            return;
        }

        handlePendingGroupStorage();

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
        if (token.isEmpty()) return;

        syncClanEventsIfVisible(token);

        clanCycleCounter++;
        if (clanCycleCounter >= 10)
        {
            clanCycleCounter = 0;
            syncClanPresence(token);
        }

        if (config.locationSharingEnabled() && !syncInFlight)
        {
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

        presenceCycleCounter++;
        if (presenceCycleCounter < 2) return;
        presenceCycleCounter = 0;

        if (!config.socialPresenceEnabled())
        {
            if (panel != null) SwingUtilities.invokeLater(panel::showSocialPresenceDisabled);
            return;
        }

        if (presenceSyncInFlight) return;
        SocialPresenceSnapshot presence = socialPresenceService.createSnapshot(
            client,
            itemManager,
            config.locationSharingEnabled()
        );
        if (presence == null) return;

        presenceSyncInFlight = true;
        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> panel.showSocialPresenceSyncing(
                presence.getRegionName(),
                presence.getActivity(),
                presence.getCombatLevel(),
                presence.getEquipment().size()
            ));
        }
        syncService.syncSocialPresence(config.apiBaseUrl(), token, presence, error -> {
            presenceSyncInFlight = false;
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null) panel.showSocialPresenceSuccess();
                else panel.showSocialPresenceError(error);
            });
        });
    }

    private void captureGroupStorage(ItemContainer container)
    {
        if (!config.groupStorageSyncEnabled())
        {
            pendingGroupStorageSnapshot = null;
            groupStorageDirty = false;
            if (panel != null) SwingUtilities.invokeLater(panel::showGroupStorageDisabled);
            return;
        }

        if (deviceToken().isEmpty())
        {
            if (panel != null)
            {
                SwingUtilities.invokeLater(() -> panel.showGroupStorageError("Link companion first"));
            }
            return;
        }

        GroupStorageSnapshot snapshot =
            groupStorageSnapshotService.createSnapshot(client, itemManager, container);
        if (snapshot == null)
        {
            return;
        }

        pendingGroupStorageSnapshot = snapshot;
        groupStorageDirty = true;
        groupStorageDebounceTicks = 2;
    }

    private void handlePendingGroupStorage()
    {
        if (!config.groupStorageSyncEnabled())
        {
            pendingGroupStorageSnapshot = null;
            groupStorageDirty = false;
            return;
        }

        if (!groupStorageDirty || groupStorageSyncInFlight)
        {
            return;
        }

        if (groupStorageDebounceTicks > 0)
        {
            groupStorageDebounceTicks--;
            return;
        }

        String token = deviceToken();
        if (token.isEmpty())
        {
            groupStorageDirty = false;
            if (panel != null)
            {
                SwingUtilities.invokeLater(() -> panel.showGroupStorageError("Link companion first"));
            }
            return;
        }

        GroupStorageSnapshot snapshot = pendingGroupStorageSnapshot;
        if (snapshot == null)
        {
            groupStorageDirty = false;
            return;
        }

        groupStorageDirty = false;
        groupStorageSyncInFlight = true;
        int occupiedSlots = snapshot.getOccupiedSlots();
        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> panel.showGroupStorageSyncing(occupiedSlots));
        }

        syncService.syncGroupStorage(config.apiBaseUrl(), token, snapshot, error -> {
            groupStorageSyncInFlight = false;
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null) panel.showGroupStorageSuccess(occupiedSlots);
                else panel.showGroupStorageError(error);
            });
            if (error != null)
            {
                logger.debug("Group Storage sync failed: {}", error);
            }
        });
    }

    private void syncClanPresence(String token)
    {
        if (!config.socialClanSyncEnabled())
        {
            if (panel != null) SwingUtilities.invokeLater(panel::showClanRosterDisabled);
            return;
        }
        if (clanSyncInFlight)
        {
            return;
        }

        SocialClanSnapshot clanSnapshot = socialClanService.createSnapshot(client);
        if (clanSnapshot == null)
        {
            return;
        }

        clanSyncInFlight = true;
        syncService.syncSocialClan(config.apiBaseUrl(), token, clanSnapshot, error -> {
            clanSyncInFlight = false;
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null)
                {
                    panel.showClanRosterSuccess(clanSnapshot.getMembers().size());
                }
                else
                {
                    panel.showClanRosterError(error);
                }
            });
            if (error != null)
            {
                logger.debug("Social clan roster sync failed: {}", error);
            }
        });
    }

    private void syncClanEventsIfVisible(String token)
    {
        if (!config.socialClanEventsSyncEnabled())
        {
            clanEventsWidgetOpen = false;
            lastClanEventsFingerprint = "";
            if (panel != null) SwingUtilities.invokeLater(panel::showClanEventsDisabled);
            return;
        }

        SocialClanEventSnapshot snapshot = socialClanEventService.createSnapshot(client);
        if (snapshot == null)
        {
            if (clanEventsWidgetOpen)
            {
                clanEventsWidgetOpen = false;
                // Reopening the interface should always force one fresh import,
                // even when the visible rows have not changed.
                lastClanEventsFingerprint = "";
            }
            return;
        }

        boolean newlyOpened = !clanEventsWidgetOpen;
        clanEventsWidgetOpen = true;
        String fingerprint = snapshot.fingerprint();
        if (clanEventsSyncInFlight || (!newlyOpened && fingerprint.equals(lastClanEventsFingerprint)))
        {
            return;
        }

        clanEventsSyncInFlight = true;
        int eventCount = snapshot.getEvents().size();
        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> panel.showClanEventsSyncing(eventCount));
        }

        syncService.syncClanEvents(config.apiBaseUrl(), token, snapshot, error -> {
            clanEventsSyncInFlight = false;
            if (error == null)
            {
                lastClanEventsFingerprint = fingerprint;
            }
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null)
                {
                    panel.showClanEventsSuccess(eventCount);
                }
                else
                {
                    panel.showClanEventsError(error);
                }
            });
            if (error != null)
            {
                logger.debug("Clan event import failed: {}", error);
            }
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
