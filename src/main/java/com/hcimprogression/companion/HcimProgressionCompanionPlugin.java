package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.events.NpcLootReceived;
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
    name = "Progression Path Companion",
    description = "Connects RuneLite accounts to Progression Path for progress, weekly gains, bank snapshots, boss and clue history, location, and social features.",
    tags = {"progression", "tracker", "account", "main", "ironman", "hcim", "group ironman", "bank", "group storage", "weekly", "loot", "location", "social", "clan"}
)
public class HcimProgressionCompanionPlugin extends Plugin
{
    private static final Logger logger = LoggerFactory.getLogger(HcimProgressionCompanionPlugin.class);
    private static final String TOKEN_KEY = "deviceToken";
    private static final String DISPLAY_NAME_KEY = "linkedDisplayName";
    private static final String TOKEN_KEY_PREFIX = "deviceToken.";
    private static final String DISPLAY_NAME_KEY_PREFIX = "linkedDisplayName.";
    private static final String ACCOUNT_SNAPSHOT_FINGERPRINT_KEY_PREFIX = "accountSnapshotFingerprint.";
    private static final int BASE_SYNC_TICKS = 5;
    private static final long LIVE_HEARTBEAT_MILLIS = 5 * 60_000L;
    private static final long LIVE_CHANGE_COOLDOWN_MILLIS = 15_000L;
    private static final long CLAN_HEARTBEAT_MILLIS = 30 * 60_000L;
    private static final long CLAN_CHANGE_COOLDOWN_MILLIS = 5 * 60_000L;
    private static final long TEARS_SYNC_COOLDOWN_MILLIS = 60_000L;
    private static final long BIRDHOUSE_SYNC_COOLDOWN_MILLIS = 5 * 60_000L;
    private static final long FARM_RUN_SYNC_COOLDOWN_MILLIS = 60_000L;
    private static final long SLAYER_SYNC_COOLDOWN_MILLIS = 5 * 60_000L;
    private static final long AUTOMATIC_ACCOUNT_SYNC_COOLDOWN_MILLIS = 5 * 60_000L;
    private static final long AUTOMATIC_ACCOUNT_SYNC_HEARTBEAT_MILLIS = 15 * 60_000L;
    private static final int AUTOMATIC_ACCOUNT_SYNC_DEBOUNCE_TICKS = 30;
    private static final Pattern CLUE_SCROLL_PATTERN = Pattern.compile(
        "you have completed [0-9]+ (beginner|easy|medium|hard|elite|master) treasure trails?\\."
    );
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile("Retry after (\\d+) seconds", Pattern.CASE_INSENSITIVE);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private HcimProgressionCompanionConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ItemManager itemManager;
    @Inject private HiscoreClient hiscoreClient;
    @Inject private Gson gson;
    @Inject private EventBus eventBus;

    private final LocationService locationService = new LocationService();
    private final SocialPresenceService socialPresenceService = new SocialPresenceService();
    private final SocialClanService socialClanService = new SocialClanService();
    private final SocialClanEventService socialClanEventService = new SocialClanEventService();
    private final AccountSnapshotService accountSnapshotService = new AccountSnapshotService();
    private final CollectionLogCaptureService collectionLogCaptureService = new CollectionLogCaptureService();
    private final GroupStorageSnapshotService groupStorageSnapshotService = new GroupStorageSnapshotService();
    private final PersonalBankSnapshotService personalBankSnapshotService = new PersonalBankSnapshotService();
    private final WeeklyLootTrackerService weeklyLootTrackerService = new WeeklyLootTrackerService();
    private TcgCollectionSnapshotService tcgCollectionSnapshotService;
    private BirdhouseTracker birdhouseTracker;
    private FarmRunTracker farmRunTracker;
    @Inject private SyncService syncService;
    private HcimProgressionCompanionPanel panel;
    private NavigationButton navigationButton;
    private int tickCounter;
    private volatile long nextLiveSyncAt;
    private volatile long nextClanSyncAt;
    private volatile long lastLiveSuccessAt;
    private volatile String lastLiveFingerprint = "";
    private volatile String lastClanFingerprint = "";
    private volatile boolean liveSyncInFlight;
    private volatile boolean clanSyncInFlight;
    private final SyncBackoff liveBackoff = new SyncBackoff();
    private final SyncBackoff clanBackoff = new SyncBackoff();
    private final SyncCooldown clanChangeCooldown = new SyncCooldown(CLAN_CHANGE_COOLDOWN_MILLIS);
    private volatile boolean clanEventsSyncInFlight;
    private volatile AccountSnapshot latestAccountSnapshot;
    private volatile String lastClanEventsFingerprint = "";
    private boolean clanEventsWidgetOpen;
    private GroupStorageSnapshot pendingGroupStorageSnapshot;
    private boolean groupStorageDirty;
    private int groupStorageDebounceTicks;
    private volatile boolean groupStorageSyncInFlight;
    private PersonalBankSnapshot pendingPersonalBankSnapshot;
    private boolean personalBankDirty;
    private int personalBankDebounceTicks;
    private volatile boolean personalBankSyncInFlight;
    private volatile String activePlayerKey;
    private volatile long lastTearsVisitAt;
    private volatile long lastTearsSyncTriggeredAt;
    private volatile String tearsVisitAccountKey = "";
    private volatile long lastBirdhouseSyncAt;
    private volatile long lastFarmRunSyncAt;
    private volatile long lastSlayerSyncAt;
    private volatile String lastSlayerFingerprint = "";
    private volatile String lastProgressFingerprint = "";
    private volatile boolean automaticAccountSyncDirty;
    private int automaticAccountSyncDebounceTicks;
    private volatile long lastAutomaticAccountSyncAt;
    private volatile boolean accountSyncInFlight;
    private volatile long accountSyncRateLimitedUntil;
    private boolean accountSessionActive;
    private ScheduledExecutorService automaticAccountSyncScheduler;
    private ScheduledFuture<?> automaticAccountSyncTask;
    private String pendingClueRewardSource = "";
    private int pendingClueRewardTicks;

    @Override
    protected void startUp()
    {
        tickCounter = 0;
        long now = System.currentTimeMillis();
        nextLiveSyncAt = now + randomJitter(30_000L);
        nextClanSyncAt = now + randomJitter(60_000L);
        lastLiveSuccessAt = 0L;
        lastLiveFingerprint = "";
        lastClanFingerprint = "";
        liveSyncInFlight = false;
        clanSyncInFlight = false;
        liveBackoff.reset();
        clanBackoff.reset();
        clanChangeCooldown.reset();
        clanEventsSyncInFlight = false;
        lastClanEventsFingerprint = "";
        clanEventsWidgetOpen = false;
        pendingGroupStorageSnapshot = null;
        groupStorageDirty = false;
        groupStorageDebounceTicks = 0;
        groupStorageSyncInFlight = false;
        pendingPersonalBankSnapshot = null;
        personalBankDirty = false;
        personalBankDebounceTicks = 0;
        personalBankSyncInFlight = false;
        activePlayerKey = null;
        lastTearsVisitAt = 0L;
        lastTearsSyncTriggeredAt = 0L;
        tearsVisitAccountKey = "";
        lastBirdhouseSyncAt = 0L;
        lastFarmRunSyncAt = 0L;
        lastSlayerSyncAt = 0L;
        lastSlayerFingerprint = "";
        lastProgressFingerprint = "";
        automaticAccountSyncDirty = false;
        automaticAccountSyncDebounceTicks = 0;
        lastAutomaticAccountSyncAt = 0L;
        accountSyncInFlight = false;
        accountSyncRateLimitedUntil = 0L;
        accountSessionActive = client.getGameState() == GameState.LOGGED_IN;
        panel = new HcimProgressionCompanionPanel(this::linkCompanion, this::syncAccountNow);
        birdhouseTracker = new BirdhouseTracker(configManager);
        farmRunTracker = new FarmRunTracker(configManager);
        tcgCollectionSnapshotService = new TcgCollectionSnapshotService(eventBus);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/hcim-companion-icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("Progression Path Companion")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        updateActiveAccountLink(currentPlayerName());
        if (config.groupStorageSyncEnabled()) panel.showGroupStorageWaiting();
        else panel.showGroupStorageDisabled();
        if (config.personalBankSyncEnabled()) panel.showPersonalBankWaiting();
        else panel.showPersonalBankDisabled();
        panel.showTcgStatus(null, config.tcgCollectionSyncEnabled());
        if (config.tcgCollectionSyncEnabled())
        {
            tcgCollectionSnapshotService.requestSnapshot();
        }

        // Keep the periodic snapshot independent of the game-tick debounce
        // loop. RuneLite can pause or delay ticks while loading worlds, opening
        // interfaces, or sitting at the login screen; the scheduler simply
        // requests a snapshot when the next 15-minute window arrives.
        automaticAccountSyncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "hcim-companion-account-sync");
            thread.setDaemon(true);
            return thread;
        });
        automaticAccountSyncTask = automaticAccountSyncScheduler.scheduleAtFixedRate(
            () -> clientThread.invokeLater(this::runPeriodicAutomaticAccountSync),
            AUTOMATIC_ACCOUNT_SYNC_HEARTBEAT_MILLIS,
            AUTOMATIC_ACCOUNT_SYNC_HEARTBEAT_MILLIS,
            TimeUnit.MILLISECONDS
        );

        logger.info("HCIM Progression Companion started.");
    }

    @Override
    protected void shutDown()
    {
        if (automaticAccountSyncTask != null)
        {
            automaticAccountSyncTask.cancel(false);
            automaticAccountSyncTask = null;
        }
        if (automaticAccountSyncScheduler != null)
        {
            automaticAccountSyncScheduler.shutdownNow();
            automaticAccountSyncScheduler = null;
        }
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
        clanEventsSyncInFlight = false;
        lastClanEventsFingerprint = "";
        clanEventsWidgetOpen = false;
        pendingGroupStorageSnapshot = null;
        groupStorageDirty = false;
        groupStorageSyncInFlight = false;
        pendingPersonalBankSnapshot = null;
        personalBankDirty = false;
        personalBankSyncInFlight = false;
        liveSyncInFlight = false;
        clanSyncInFlight = false;
        accountSyncInFlight = false;
        accountSessionActive = false;
        liveBackoff.reset();
        clanBackoff.reset();
        clanChangeCooldown.reset();
        logger.info("HCIM Progression Companion stopped.");
    }

    private void runPeriodicAutomaticAccountSync()
    {
        if (!config.automaticAccountSyncEnabled() || deviceToken().isEmpty()
            || client.getGameState() != GameState.LOGGED_IN || accountSyncInFlight)
        {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAutomaticAccountSyncAt < AUTOMATIC_ACCOUNT_SYNC_COOLDOWN_MILLIS)
        {
            return;
        }

        lastAutomaticAccountSyncAt = now;
        automaticAccountSyncDirty = false;
        automaticAccountSyncDebounceTicks = 0;
        syncAccountAutomatically();
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
                String key = accountKey(result.getDisplayName());
                if (key.isEmpty())
                {
                    panel.showLinkError("The website did not return a RuneScape account name");
                    return;
                }
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY_PREFIX + key, result.getToken());
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, DISPLAY_NAME_KEY_PREFIX + key, result.getDisplayName());
                activePlayerKey = key;
                panel.showLinked(result.getDisplayName());
            })
        );
    }


    private void syncAccountNow()
    {
        syncAccountNow(false, true);
    }

    private void syncAccountAutomatically()
    {
        syncAccountNow(false, false);
    }

    private void syncAccountAutomatically(boolean allowCachedSnapshot)
    {
        syncAccountNow(allowCachedSnapshot, false);
    }

    /**
     * Sync the most recent in-game snapshot when logout has already changed the
     * client state. RuneLite clears the live game state before the logout event
     * is delivered, but the snapshot captured during the last tick is still a
     * reliable representation of the account and can be enriched with fresh
     * hiscore data.
     */
    private void syncAccountNow(boolean allowCachedSnapshot, boolean forceUpload)
    {
        long now = System.currentTimeMillis();
        if (now < accountSyncRateLimitedUntil)
        {
            if (panel != null)
            {
                long remaining = Math.max(1L, (accountSyncRateLimitedUntil - now + 999L) / 1000L);
                SwingUtilities.invokeLater(() -> panel.showAccountSyncCooldown(remaining));
            }
            return;
        }

        String token = deviceToken();
        if (token.isEmpty())
        {
            if (panel != null) panel.showAccountSyncError("Link the companion first");
            return;
        }

        if (accountSyncInFlight)
        {
            return;
        }
        accountSyncInFlight = true;

        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                panel.showAccountSyncAttempt();
                panel.setAccountSyncing(true);
            });
        }

        clientThread.invokeLater(() ->
        {
            try
            {
                if (client.getGameState() != GameState.LOGGED_IN && (!allowCachedSnapshot || latestAccountSnapshot == null))
                {
                    accountSyncInFlight = false;
                    SwingUtilities.invokeLater(() ->
                    {
                        if (panel != null) panel.showAccountSyncError("Log into the game first");
                    });
                    return;
                }

                AccountSnapshot snapshot = client.getGameState() == GameState.LOGGED_IN
                    ? accountSnapshotService.createSnapshot(client, collectionLogCaptureService, birdhouseTracker, farmRunTracker, itemManager)
                    : latestAccountSnapshot;
                if (snapshot == null)
                {
                    accountSyncInFlight = false;
                    SwingUtilities.invokeLater(() ->
                    {
                        if (panel != null) panel.showAccountSyncError("Could not read account data");
                    });
                    return;
                }
                latestAccountSnapshot = snapshot;
                if (accountKey(snapshot.getPlayerName()).equals(tearsVisitAccountKey))
                {
                    snapshot.setLastTearsVisitAt(lastTearsVisitAt);
                }
                weeklyLootTrackerService.applyTo(
                    snapshot,
                    configManager,
                    gson,
                    config.weeklyLootTrackingEnabled()
                );

                // Clue completion totals are not reliably present in Collection Log widgets.
                // RuneLite itself uses the official hiscores for !clues, so use the same source.
                CompletableFuture<HiscoreResult> hiscoreFuture = hiscoreClient.lookupAsync(snapshot.getPlayerName(), HiscoreEndpoint.NORMAL);
                TcgCollectionSnapshot currentTcgSnapshot = null;
                if (config.tcgCollectionSyncEnabled())
                {
                    tcgCollectionSnapshotService.requestSnapshot();
                    currentTcgSnapshot = tcgCollectionSnapshotService.getLatestSnapshot();
                }
                CompletableFuture<TcgCollectionSnapshot> tcgFuture =
                    CompletableFuture.completedFuture(currentTcgSnapshot);
                CompletableFuture.allOf(hiscoreFuture, tcgFuture).whenComplete((ignored, combinedError) ->
                    {
                        HiscoreResult hiscoreResult = null;
                        TcgCollectionSnapshot tcgSnapshot = null;
                        try
                        {
                            if (!hiscoreFuture.isCompletedExceptionally())
                            {
                                hiscoreResult = hiscoreFuture.getNow(null);
                            }
                            if (!tcgFuture.isCompletedExceptionally())
                            {
                                tcgSnapshot = tcgFuture.getNow(null);
                            }
                        }
                        catch (RuntimeException readError)
                        {
                            logger.debug("Optional hiscore or TCG data was unavailable", readError);
                        }
                        final HiscoreResult resolvedHiscore = hiscoreResult;
                        final TcgCollectionSnapshot resolvedTcgSnapshot = tcgSnapshot;
                        if (resolvedTcgSnapshot != null) snapshot.setTcg(resolvedTcgSnapshot);
                        SwingUtilities.invokeLater(() -> {
                            if (panel != null) panel.showTcgStatus(resolvedTcgSnapshot, config.tcgCollectionSyncEnabled());
                        });
                        if (resolvedHiscore != null)
                        {
                            applyHiscoreClueCounts(snapshot, resolvedHiscore);
                            applyHiscoreBossKillCounts(snapshot, resolvedHiscore);
                        }
                        else
                        {
                            logger.warn("Could not fetch clue totals from hiscores; syncing captured data only", combinedError);
                        }

                        String snapshotFingerprint = AccountSnapshotFingerprint.create(gson, snapshot);
                        String snapshotFingerprintKey = accountSnapshotFingerprintKey(snapshot.getPlayerName(), token);
                        String savedFingerprint = configManager.getConfiguration(
                            HcimProgressionCompanionConfig.GROUP,
                            snapshotFingerprintKey
                        );
                        if (!forceUpload && snapshotFingerprint.equals(savedFingerprint))
                        {
                            accountSyncInFlight = false;
                            SwingUtilities.invokeLater(() ->
                            {
                                if (panel != null) panel.showAccountSyncUnchanged();
                            });
                            return;
                        }

                        syncService.syncAccount(config.apiBaseUrl(), token, snapshot, (result, error) ->
                        {
                            accountSyncInFlight = false;
                            if (error == null && result != null)
                            {
                                configManager.setConfiguration(
                                    HcimProgressionCompanionConfig.GROUP,
                                    snapshotFingerprintKey,
                                    snapshotFingerprint
                                );
                            }
                            SwingUtilities.invokeLater(() ->
                            {
                                if (panel == null) return;
                                if (error != null || result == null)
                                {
                                    long retryAfter = parseRetryAfter(error);
                                    if (retryAfter > 0)
                                    {
                                        accountSyncRateLimitedUntil = System.currentTimeMillis() + retryAfter * 1000L;
                                        panel.showAccountSyncCooldown(retryAfter);
                                    }
                                    else
                                    {
                                        panel.showAccountSyncError(error == null ? "Account sync failed" : error);
                                    }
                                    return;
                                }
                                panel.showAccountSyncSuccess(
                                    result.getQuestUpdates(),
                                    result.getTaskUpdates(),
                                    result.getBossKillCountCount()
                                );
                            });
                        });
                    });
            }
            catch (RuntimeException error)
            {
                accountSyncInFlight = false;
                logger.error("Account sync failed", error);
                SwingUtilities.invokeLater(() ->
                {
                    if (panel != null)
                    {
                        String message = error.getMessage();
                        long retryAfter = parseRetryAfter(message);
                        if (retryAfter > 0)
                        {
                            accountSyncRateLimitedUntil = System.currentTimeMillis() + retryAfter * 1000L;
                            panel.showAccountSyncCooldown(retryAfter);
                        }
                        else
                        {
                            panel.showAccountSyncError(message == null || message.isEmpty()
                                ? "Could not read account data" : message);
                        }
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

    private void applyHiscoreBossKillCounts(AccountSnapshot snapshot, HiscoreResult result)
    {
        putBossKillCount(snapshot, "maggot-king", result.getSkill(HiscoreSkill.MAGGOT_KING));
        putBossKillCount(snapshot, "brutus", result.getSkill(HiscoreSkill.BRUTUS));
        putBossKillCount(snapshot, "bryophyta", result.getSkill(HiscoreSkill.BRYOPHYTA));
        putBossKillCount(snapshot, "obor", result.getSkill(HiscoreSkill.OBOR));
        putBossKillCount(snapshot, "scurrius", result.getSkill(HiscoreSkill.SCURRIUS));
        putBossKillCount(snapshot, "barrows", result.getSkill(HiscoreSkill.BARROWS_CHESTS));
        putBossKillCount(snapshot, "giant-mole", result.getSkill(HiscoreSkill.GIANT_MOLE));
        putBossKillCount(snapshot, "sarachnis", result.getSkill(HiscoreSkill.SARACHNIS));
        putBossKillCount(snapshot, "hespori", result.getSkill(HiscoreSkill.HESPORI));
        putBossKillCount(snapshot, "tempoross", result.getSkill(HiscoreSkill.TEMPOROSS));
        putBossKillCount(snapshot, "wintertodt", result.getSkill(HiscoreSkill.WINTERTODT));
        putBossKillCount(snapshot, "deranged-archaeologist", result.getSkill(HiscoreSkill.DERANGED_ARCHAEOLOGIST));
        putBossKillCount(snapshot, "king-black-dragon", result.getSkill(HiscoreSkill.KING_BLACK_DRAGON));
        putBossKillCount(snapshot, "kalphite-queen", result.getSkill(HiscoreSkill.KALPHITE_QUEEN));
        putBossKillCountSum(snapshot, "dagannoth-kings",
            result.getSkill(HiscoreSkill.DAGANNOTH_PRIME),
            result.getSkill(HiscoreSkill.DAGANNOTH_REX),
            result.getSkill(HiscoreSkill.DAGANNOTH_SUPREME));
        putBossKillCount(snapshot, "skotizo", result.getSkill(HiscoreSkill.SKOTIZO));
        putBossKillCount(snapshot, "kraken", result.getSkill(HiscoreSkill.KRAKEN));
        putBossKillCount(snapshot, "grotesque-guardians", result.getSkill(HiscoreSkill.GROTESQUE_GUARDIANS));
        putBossKillCount(snapshot, "perilous-moons", result.getSkill(HiscoreSkill.LUNAR_CHESTS));
        putBossKillCount(snapshot, "amoxliatl", result.getSkill(HiscoreSkill.AMOXLIATL));
        putBossKillCount(snapshot, "hueycoatl", result.getSkill(HiscoreSkill.THE_HUEYCOATL));
        putBossKillCount(snapshot, "royal-titans", result.getSkill(HiscoreSkill.THE_ROYAL_TITANS));
        putBossKillCount(snapshot, "mimic", result.getSkill(HiscoreSkill.MIMIC));
        putBossKillCount(snapshot, "tzhaar-fight-cave", result.getSkill(HiscoreSkill.TZTOK_JAD));
        putBossKillCount(snapshot, "zulrah", result.getSkill(HiscoreSkill.ZULRAH));
        putBossKillCount(snapshot, "vorkath", result.getSkill(HiscoreSkill.VORKATH));
        putBossKillCount(snapshot, "gauntlet", result.getSkill(HiscoreSkill.THE_GAUNTLET));
        putBossKillCount(snapshot, "corrupted-gauntlet", result.getSkill(HiscoreSkill.THE_CORRUPTED_GAUNTLET));
        putBossKillCount(snapshot, "zalcano", result.getSkill(HiscoreSkill.ZALCANO));
        putBossKillCount(snapshot, "chaos-fanatic", result.getSkill(HiscoreSkill.CHAOS_FANATIC));
        putBossKillCount(snapshot, "crazy-archaeologist", result.getSkill(HiscoreSkill.CRAZY_ARCHAEOLOGIST));
        putBossKillCount(snapshot, "scorpia", result.getSkill(HiscoreSkill.SCORPIA));
        putBossKillCount(snapshot, "chaos-elemental", result.getSkill(HiscoreSkill.CHAOS_ELEMENTAL));
        putBossKillCount(snapshot, "calvarion", result.getSkill(HiscoreSkill.CALVARION));
        putBossKillCount(snapshot, "spindel", result.getSkill(HiscoreSkill.SPINDEL));
        putBossKillCount(snapshot, "artio", result.getSkill(HiscoreSkill.ARTIO));
        putBossKillCount(snapshot, "vetion", result.getSkill(HiscoreSkill.VETION));
        putBossKillCount(snapshot, "venenatis", result.getSkill(HiscoreSkill.VENENATIS));
        putBossKillCount(snapshot, "callisto", result.getSkill(HiscoreSkill.CALLISTO));
        putBossKillCount(snapshot, "abyssal-sire", result.getSkill(HiscoreSkill.ABYSSAL_SIRE));
        putBossKillCount(snapshot, "cerberus", result.getSkill(HiscoreSkill.CERBERUS));
        putBossKillCount(snapshot, "thermy", result.getSkill(HiscoreSkill.THERMONUCLEAR_SMOKE_DEVIL));
        putBossKillCount(snapshot, "alchemical-hydra", result.getSkill(HiscoreSkill.ALCHEMICAL_HYDRA));
        putBossKillCount(snapshot, "araxxor", result.getSkill(HiscoreSkill.ARAXXOR));
        putBossKillCount(snapshot, "commander-zilyana", result.getSkill(HiscoreSkill.COMMANDER_ZILYANA));
        putBossKillCount(snapshot, "general-graardor", result.getSkill(HiscoreSkill.GENERAL_GRAARDOR));
        putBossKillCount(snapshot, "kril", result.getSkill(HiscoreSkill.KRIL_TSUTSAROTH));
        putBossKillCount(snapshot, "kree-arra", result.getSkill(HiscoreSkill.KREEARRA));
        putBossKillCount(snapshot, "phantom-muspah", result.getSkill(HiscoreSkill.PHANTOM_MUSPAH));
        putBossKillCount(snapshot, "vardorvis", result.getSkill(HiscoreSkill.VARDORVIS));
        putBossKillCount(snapshot, "duke-sucellus", result.getSkill(HiscoreSkill.DUKE_SUCELLUS));
        putBossKillCount(snapshot, "leviathan", result.getSkill(HiscoreSkill.THE_LEVIATHAN));
        putBossKillCount(snapshot, "whisperer", result.getSkill(HiscoreSkill.THE_WHISPERER));
        putBossKillCount(snapshot, "nightmare", result.getSkill(HiscoreSkill.NIGHTMARE));
        putBossKillCount(snapshot, "phosanis-nightmare", result.getSkill(HiscoreSkill.PHOSANIS_NIGHTMARE));
        putBossKillCount(snapshot, "corporeal-beast", result.getSkill(HiscoreSkill.CORPOREAL_BEAST));
        putBossKillCount(snapshot, "yama", result.getSkill(HiscoreSkill.YAMA));
        putBossKillCount(snapshot, "doom-of-mokhaiotl", result.getSkill(HiscoreSkill.DOOM_OF_MOKHAIOTL));
        putBossKillCount(snapshot, "shellbane-gryphon", result.getSkill(HiscoreSkill.SHELLBANE_GRYPHON));
        putBossKillCountSum(snapshot, "cox",
            result.getSkill(HiscoreSkill.CHAMBERS_OF_XERIC),
            result.getSkill(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE));
        putBossKillCountSum(snapshot, "tob",
            result.getSkill(HiscoreSkill.THEATRE_OF_BLOOD),
            result.getSkill(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE));
        putBossKillCountSum(snapshot, "toa",
            result.getSkill(HiscoreSkill.TOMBS_OF_AMASCUT),
            result.getSkill(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT));
        putBossKillCount(snapshot, "nex", result.getSkill(HiscoreSkill.NEX));
        putBossKillCount(snapshot, "inferno", result.getSkill(HiscoreSkill.TZKAL_ZUK));
        putBossKillCount(snapshot, "colosseum", result.getSkill(HiscoreSkill.SOL_HEREDIT));
    }

    private void putBossKillCount(AccountSnapshot snapshot, String bossId, Skill skill)
    {
        if (skill != null && skill.getLevel() >= 0)
        {
            snapshot.getBossKillCounts().put(bossId, skill.getLevel());
        }
    }

    private void putBossKillCountSum(AccountSnapshot snapshot, String bossId, Skill... skills)
    {
        int total = 0;
        boolean published = false;
        for (Skill skill : skills)
        {
            if (skill != null && skill.getLevel() >= 0)
            {
                total += skill.getLevel();
                published = true;
            }
        }
        if (published)
        {
            snapshot.getBossKillCounts().put(bossId, total);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessage event)
    {
        if (tcgCollectionSnapshotService == null || !tcgCollectionSnapshotService.handle(event))
        {
            return;
        }

        TcgCollectionSnapshot snapshot = tcgCollectionSnapshotService.getLatestSnapshot();
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.showTcgStatus(snapshot, config.tcgCollectionSyncEnabled());
            }
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState nextState = event.getGameState();
        boolean sessionStarted = nextState == GameState.LOGGED_IN && !accountSessionActive;
        if (sessionStarted)
        {
            accountSessionActive = true;
        }

        // A farm timer can be unchanged while the client is restarted or the
        // account is switched.  In that case no varbit transition fires, so
        // relying only on farm-change events leaves the website showing an
        // empty board.  Upload the current Time Tracking snapshot once after
        // every successful login as well.
        if (sessionStarted
            && config.automaticAccountSyncEnabled()
            && !deviceToken().isEmpty())
        {
            lastFarmRunSyncAt = 0L;
            requestFarmingAccountSync();
        }

        // Capture the final cached snapshot as soon as the player logs out.
        // This mirrors RuneLite's high-score refresh behavior without waiting
        // for the next login, and avoids trying to read cleared client widgets.
        if (isLoggedOutState(nextState) && accountSessionActive)
        {
            accountSessionActive = false;
            // Clear the old character's live card before the next account can
            // publish through the same Firebase user/profile.
            if (!deviceToken().isEmpty())
            {
                clearLivePresenceForLogout();
                if (config.automaticAccountSyncEnabled() && latestAccountSnapshot != null)
                {
                    automaticAccountSyncDirty = false;
                    automaticAccountSyncDebounceTicks = 0;
                    lastAutomaticAccountSyncAt = System.currentTimeMillis();
                    syncAccountAutomatically(true);
                }
            }
        }
    }

    private static boolean isLoggedOutState(GameState state)
    {
        return state == GameState.LOGIN_SCREEN || state == GameState.LOGIN_SCREEN_AUTHENTICATOR;
    }

    private void clearLivePresenceForLogout()
    {
        String token = deviceToken();
        if (token.isEmpty()) return;
        String playerName = latestAccountSnapshot == null ? "" : latestAccountSnapshot.getPlayerName();
        syncService.clearLivePresence(config.apiBaseUrl(), token, playerName, error -> {
            if (error != null) logger.debug("Could not clear live presence on logout: {}", error);
        });
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        String message = event.getMessage();
        if (message == null)
        {
            return;
        }

        String normalized = message.replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
        Matcher clueMatcher = CLUE_SCROLL_PATTERN.matcher(normalized);
        if (clueMatcher.find() && config.weeklyLootTrackingEnabled())
        {
            String tier = clueMatcher.group(1);
            pendingClueRewardSource = "Clue Scroll ("
                + Character.toUpperCase(tier.charAt(0)) + tier.substring(1) + ")";
            // RuneLite fills TRAIL_REWARDINV with the actual casket rewards.
            // Read it immediately like the Loot Tracker plugin, while retaining
            // a short fallback for clients whose container settles afterward.
            pendingClueRewardTicks = 2;
            capturePendingClueReward(client.getItemContainer(InventoryID.TRAIL_REWARDINV));
        }
        boolean collectionLogEvent = normalized.contains("new collection log item")
            || normalized.contains("added to your collection log")
            || normalized.contains("collection log entry");
        if (collectionLogEvent && config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
        {
            // Collection-log/clue rewards often arrive as several chat lines in
            // one game tick. The normal automatic scheduler coalesces them,
            // so one casket batch produces one upload instead of one per item.
            requestAutomaticAccountSync();
        }

        boolean farmingEvent = normalized.contains("you plant")
            || normalized.contains("you harvest")
            || normalized.contains("you clear the patch")
            || normalized.contains("the patch is now empty")
            || normalized.contains("you pick some")
            || normalized.contains("you have successfully grown");
        if (farmingEvent && config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
        {
            requestFarmingAccountSync();
        }

        boolean completedVisit = normalized.contains("tears collected:")
            || (normalized.contains("tears of guthix") && normalized.contains("completed"));
        if (!completedVisit)
        {
            return;
        }

        String playerName = currentPlayerName();
        String accountKey = accountKey(playerName);
        if (accountKey.isEmpty())
        {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTearsSyncTriggeredAt < TEARS_SYNC_COOLDOWN_MILLIS)
        {
            return;
        }
        tearsVisitAccountKey = accountKey;
        lastTearsVisitAt = now;
        lastTearsSyncTriggeredAt = now;
        syncAccountAutomatically();
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        if (!config.weeklyLootTrackingEnabled())
        {
            return;
        }
        SlayerSnapshot slayer = latestAccountSnapshot == null ? null : latestAccountSnapshot.getSlayer();
        String slayerTask = slayer == null ? "" : slayer.getCurrentTask();
        String source = event.getNpc() == null ? "NPC loot" : event.getNpc().getName();
        int npcId = event.getNpc() == null ? 0 : event.getNpc().getId();
        weeklyLootTrackerService.recordNpcLoot(
            source,
            npcId,
            slayerTask,
            event.getItems(),
            itemManager,
            configManager,
            gson
        );
        if (config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
        {
            requestAutomaticAccountSync();
        }
    }

    private void capturePendingClueReward(ItemContainer container)
    {
        if (pendingClueRewardSource.isEmpty() || container == null)
        {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (net.runelite.api.Item item : container.getItems())
        {
            if (item != null && item.getId() > 0 && item.getQuantity() > 0)
            {
                items.add(new ItemStack(item.getId(), item.getQuantity()));
            }
        }
        if (items.isEmpty())
        {
            return;
        }

        String source = pendingClueRewardSource;
        pendingClueRewardSource = "";
        pendingClueRewardTicks = 0;
        weeklyLootTrackerService.recordEventLoot(
            source,
            items,
            itemManager,
            configManager,
            gson
        );
        if (config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
        {
            // Uses the existing account-sync debounce, so several collection
            // log popups from one casket cannot create an upload burst.
            requestAutomaticAccountSync();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        socialPresenceService.updateSkillActivity(event.getSkill(), event.getXp(), System.currentTimeMillis());
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
            if (result != null && config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
            {
                requestAutomaticAccountSync();
            }
        });
    }


    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.TRAIL_REWARDINV && !pendingClueRewardSource.isEmpty())
        {
            capturePendingClueReward(event.getItemContainer());
        }

        if (event.getContainerId() == InventoryID.BANK
            && client.getTopLevelInterfaceId() == InterfaceID.BANKMAIN)
        {
            capturePersonalBank(event.getItemContainer());
        }

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
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.BANKMAIN)
        {
            capturePersonalBank(client.getItemContainer(InventoryID.BANK));
        }

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
            scheduleClanChangeSync();
        }
    }

    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined event)
    {
        scheduleClanChangeSync();
    }

    @Subscribe
    public void onClanMemberLeft(ClanMemberLeft event)
    {
        scheduleClanChangeSync();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            if (panel != null) SwingUtilities.invokeLater(panel::showLoggedOut);
            return;
        }

        if (!pendingClueRewardSource.isEmpty() && pendingClueRewardTicks > 0 && --pendingClueRewardTicks == 0)
        {
            capturePendingClueReward(client.getItemContainer(InventoryID.TRAIL_REWARDINV));
            // Do not allow an empty or closed reward container to leak into the
            // next casket and mislabel its contents.
            pendingClueRewardSource = "";
        }

        boolean birdhousesChanged = birdhouseTracker != null && birdhouseTracker.update(client);
        long birdhouseNow = System.currentTimeMillis();
        if (birdhousesChanged && birdhouseNow - lastBirdhouseSyncAt >= BIRDHOUSE_SYNC_COOLDOWN_MILLIS && !deviceToken().isEmpty())
        {
            lastBirdhouseSyncAt = birdhouseNow;
            requestAutomaticAccountSync();
        }
        boolean farmRunsChanged = farmRunTracker != null && farmRunTracker.update(client);
        if (farmRunsChanged && birdhouseNow - lastFarmRunSyncAt >= FARM_RUN_SYNC_COOLDOWN_MILLIS && !deviceToken().isEmpty())
        {
            lastFarmRunSyncAt = birdhouseNow;
            requestFarmingAccountSync();
        }
        String slayerFingerprint = client.getVarpValue(VarPlayerID.SLAYER_TARGET) + ":"
            + client.getVarpValue(VarPlayerID.SLAYER_COUNT) + ":"
            + client.getVarbitValue(VarbitID.SLAYER_POINTS) + ":"
            + client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED);
        boolean slayerChanged = !slayerFingerprint.equals(lastSlayerFingerprint);
        lastSlayerFingerprint = slayerFingerprint;
        if (slayerChanged && birdhouseNow - lastSlayerSyncAt >= SLAYER_SYNC_COOLDOWN_MILLIS && !deviceToken().isEmpty())
        {
            lastSlayerSyncAt = birdhouseNow;
            requestAutomaticAccountSync();
        }

        if (config.automaticAccountSyncEnabled() && !deviceToken().isEmpty())
        {
            if (tickCounter == 0)
            {
                String progressFingerprint = progressFingerprint();
                if (lastProgressFingerprint.isEmpty())
                {
                    lastProgressFingerprint = progressFingerprint;
                    requestAutomaticAccountSync();
                }
                else if (!progressFingerprint.equals(lastProgressFingerprint))
                {
                    lastProgressFingerprint = progressFingerprint;
                    requestAutomaticAccountSync();
                }
            }

            handleAutomaticAccountSync(birdhouseNow);
        }
        else
        {
            automaticAccountSyncDirty = false;
            automaticAccountSyncDebounceTicks = 0;
            lastProgressFingerprint = "";
        }

        handlePendingGroupStorage();
        handlePendingPersonalBank();

        tickCounter++;
        if (tickCounter < BASE_SYNC_TICKS) return;
        tickCounter = 0;

        PlayerState state = locationService.createPlayerState(client);
        if (state == null) return;
        String playerName = state.getPlayerName() == null ? "Unknown" : state.getPlayerName();
        updateActiveAccountLink(playerName);

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

        long now = System.currentTimeMillis();
        if (now >= nextClanSyncAt && clanBackoff.canAttempt(now))
        {
            syncClanPresence(token);
        }

        syncLivePresence(token, state, now);
    }

    private void requestAutomaticAccountSync()
    {
        if (!config.automaticAccountSyncEnabled())
        {
            return;
        }

        automaticAccountSyncDirty = true;
        automaticAccountSyncDebounceTicks = AUTOMATIC_ACCOUNT_SYNC_DEBOUNCE_TICKS;
    }

    private void requestFarmingAccountSync()
    {
        automaticAccountSyncDirty = true;
        automaticAccountSyncDebounceTicks = 5;
    }

    private void handleAutomaticAccountSync(long now)
    {
        if (!automaticAccountSyncDirty || accountSyncInFlight)
        {
            return;
        }
        if (automaticAccountSyncDebounceTicks > 0)
        {
            automaticAccountSyncDebounceTicks--;
            return;
        }
        if (now - lastAutomaticAccountSyncAt < AUTOMATIC_ACCOUNT_SYNC_COOLDOWN_MILLIS)
        {
            return;
        }

        automaticAccountSyncDirty = false;
        lastAutomaticAccountSyncAt = now;
        syncAccountAutomatically();
    }

    private String accountSnapshotFingerprintKey(String playerName, String token)
    {
        return ACCOUNT_SNAPSHOT_FINGERPRINT_KEY_PREFIX
            + accountKey(playerName)
            + "."
            + AccountSnapshotFingerprint.linkKey(token);
    }

    private static long parseRetryAfter(String message)
    {
        if (message == null) return 0L;
        Matcher matcher = RETRY_AFTER_PATTERN.matcher(message);
        if (!matcher.find()) return 0L;
        try
        {
            return Math.max(1L, Long.parseLong(matcher.group(1)));
        }
        catch (NumberFormatException ignored)
        {
            return 0L;
        }
    }

    private String progressFingerprint()
    {
        StringBuilder fingerprint = new StringBuilder(512);
        for (net.runelite.api.Skill skill : net.runelite.api.Skill.values())
        {
            if (skill == net.runelite.api.Skill.OVERALL)
            {
                continue;
            }
            // XP changes at low levels are frequent and do not need a network
            // snapshot. Track level transitions once a skill is meaningfully
            // progressed; the 15-minute heartbeat still captures exact XP.
            int level = client.getRealSkillLevel(skill);
            // Below 40, the periodic heartbeat is sufficient. Above 40,
            // coalesce level changes into two-level buckets to avoid a
            // network sync on every individual level.
            int bucket = level > 40 ? 40 + ((level - 40) / 2) : 0;
            fingerprint.append(bucket).append('|');
        }

        fingerprint.append(client.getVarpValue(net.runelite.api.VarPlayer.QUEST_POINTS)).append('|')
            .append(client.getVarpValue(net.runelite.api.VarPlayer.CLOG_LOGGED)).append('|')
            .append(client.getVarpValue(net.runelite.api.VarPlayer.CLOG_TOTAL)).append('|');

        for (Quest quest : Quest.values())
        {
            try
            {
                fingerprint.append(quest.getState(client).ordinal());
            }
            catch (RuntimeException ignored)
            {
                fingerprint.append('?');
            }
        }

        int[] diaryVarbits = {
            VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE,
            VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE,
            VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
            VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE,
            VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
            VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE,
            VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT,
            VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE,
            VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
            VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE,
            VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
            VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE,
            VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
            VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE,
            VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
            VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE,
            VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
            VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE,
            VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
            VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE
        };
        for (int varbitId : diaryVarbits)
        {
            fingerprint.append(client.getVarbitValue(varbitId)).append('|');
        }
        return fingerprint.toString();
    }

    private void syncLivePresence(String token, PlayerState state, long now)
    {
        boolean locationEnabled = config.locationSharingEnabled();
        boolean presenceEnabled = config.socialPresenceEnabled();
        if (!presenceEnabled && panel != null)
        {
            SwingUtilities.invokeLater(panel::showSocialPresenceDisabled);
        }
        if (!locationEnabled && !presenceEnabled)
        {
            return;
        }

        SocialPresenceSnapshot presence = presenceEnabled
            ? socialPresenceService.createSnapshot(client, itemManager, locationEnabled)
            : null;
        if (presenceEnabled && presence == null)
        {
            return;
        }

        String fingerprint = liveFingerprint(state, presence, locationEnabled, presenceEnabled);
        boolean changed = !fingerprint.equals(lastLiveFingerprint);
        boolean changeAllowed = changed && now - lastLiveSuccessAt >= LIVE_CHANGE_COOLDOWN_MILLIS;
        if ((!changeAllowed && now < nextLiveSyncAt) || liveSyncInFlight || !liveBackoff.canAttempt(now))
        {
            return;
        }

        liveSyncInFlight = true;
        if (presenceEnabled && panel != null)
        {
            SwingUtilities.invokeLater(() -> panel.showSocialPresenceSyncing(
                presence.getRegionName(), presence.getActivity(),
                presence.getCombatLevel(), presence.getEquipment().size()));
        }

        syncService.syncLive(
            config.apiBaseUrl(), token, state, presence,
            locationEnabled, presenceEnabled, error ->
        {
            liveSyncInFlight = false;
            long completedAt = System.currentTimeMillis();
            if (error == null)
            {
                liveBackoff.recordSuccess();
                lastLiveFingerprint = fingerprint;
                lastLiveSuccessAt = completedAt;
                nextLiveSyncAt = completedAt + LIVE_HEARTBEAT_MILLIS + randomJitter(30_000L);
            }
            else
            {
                nextLiveSyncAt = liveBackoff.recordFailure(completedAt, randomJitter(15_000L));
            }
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (locationEnabled)
                {
                    if (error == null) panel.showSyncSuccess();
                    else panel.showSyncError(error);
                }
                if (presenceEnabled)
                {
                    if (error == null) panel.showSocialPresenceSuccess();
                    else panel.showSocialPresenceError(error);
                }
            });
        });
    }

    private String liveFingerprint(
        PlayerState state,
        SocialPresenceSnapshot presence,
        boolean locationEnabled,
        boolean presenceEnabled)
    {
        StringBuilder value = new StringBuilder()
            .append(locationEnabled).append('|')
            .append(presenceEnabled).append('|')
            .append(state.getPlayerName()).append('|')
            .append(state.getWorld()).append('|')
            .append(state.getRegionId()).append('|')
            .append(state.getPlane());
        if (presence != null)
        {
            value.append('|').append(presence.getRegionName())
                .append('|').append(presence.getCombatLevel())
                .append('|').append(presence.getActivity())
                .append('|').append(presence.isInWilderness())
                .append('|').append(presence.isExactLocationIncluded());
            for (Map.Entry<String, SocialPresenceSnapshot.EquipmentItem> entry
                : presence.getEquipment().entrySet())
            {
                value.append('|').append(entry.getKey())
                    .append(':').append(entry.getValue().getItemId());
            }
        }
        return value.toString();
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

    private void capturePersonalBank(ItemContainer container)
    {
        if (!config.personalBankSyncEnabled())
        {
            pendingPersonalBankSnapshot = null;
            personalBankDirty = false;
            if (panel != null) SwingUtilities.invokeLater(panel::showPersonalBankDisabled);
            return;
        }

        if (deviceToken().isEmpty())
        {
            if (panel != null)
            {
                SwingUtilities.invokeLater(() -> panel.showPersonalBankError("Link companion first"));
            }
            return;
        }

        PersonalBankSnapshot snapshot =
            personalBankSnapshotService.createSnapshot(client, itemManager, container);
        if (snapshot == null)
        {
            return;
        }

        pendingPersonalBankSnapshot = snapshot;
        personalBankDirty = true;
        personalBankDebounceTicks = 2;
    }

    private void handlePendingPersonalBank()
    {
        if (!config.personalBankSyncEnabled())
        {
            pendingPersonalBankSnapshot = null;
            personalBankDirty = false;
            return;
        }

        if (!personalBankDirty || personalBankSyncInFlight)
        {
            return;
        }

        if (personalBankDebounceTicks > 0)
        {
            personalBankDebounceTicks--;
            return;
        }

        String token = deviceToken();
        if (token.isEmpty())
        {
            personalBankDirty = false;
            if (panel != null)
            {
                SwingUtilities.invokeLater(() -> panel.showPersonalBankError("Link companion first"));
            }
            return;
        }

        PersonalBankSnapshot snapshot = pendingPersonalBankSnapshot;
        if (snapshot == null)
        {
            personalBankDirty = false;
            return;
        }

        personalBankDirty = false;
        personalBankSyncInFlight = true;
        int occupiedSlots = snapshot.getOccupiedSlots();
        if (panel != null)
        {
            SwingUtilities.invokeLater(() -> panel.showPersonalBankSyncing(occupiedSlots));
        }

        syncService.syncPersonalBank(config.apiBaseUrl(), token, snapshot, error -> {
            personalBankSyncInFlight = false;
            SwingUtilities.invokeLater(() -> {
                if (panel == null) return;
                if (error == null) panel.showPersonalBankSuccess(occupiedSlots);
                else panel.showPersonalBankError(error);
            });
            if (error != null)
            {
                logger.debug("Personal Bank sync failed: {}", error);
            }
        });
    }

    private void syncClanPresence(String token)
    {
        long now = System.currentTimeMillis();
        if (!config.socialClanSyncEnabled())
        {
            nextClanSyncAt = now + CLAN_HEARTBEAT_MILLIS;
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
            nextClanSyncAt = now + (5 * 60_000L);
            return;
        }

        if (config.clanSkillWeekSyncEnabled() && latestAccountSnapshot != null)
        {
            syncService.syncClanSkillWeek(config.apiBaseUrl(), token, clanSnapshot.getClanName(), latestAccountSnapshot, error -> {
                if (error != null) logger.debug("Clan Skill of the Week sync failed: {}", error);
            });
        }

        String fingerprint = clanFingerprint(clanSnapshot);
        if (fingerprint.equals(lastClanFingerprint))
        {
            nextClanSyncAt = now + CLAN_HEARTBEAT_MILLIS + randomJitter(60_000L);
            return;
        }
        clanChangeCooldown.recordAttempt(now);
        clanSyncInFlight = true;
        syncService.syncSocialClan(config.apiBaseUrl(), token, clanSnapshot, error -> {
            clanSyncInFlight = false;
            long completedAt = System.currentTimeMillis();
            if (error == null)
            {
                clanBackoff.recordSuccess();
                lastClanFingerprint = fingerprint;
                nextClanSyncAt = completedAt + CLAN_HEARTBEAT_MILLIS + randomJitter(60_000L);
            }
            else
            {
                long retryAt = clanBackoff.recordFailure(completedAt, randomJitter(30_000L));
                nextClanSyncAt = clanChangeCooldown.nextAllowedAt(completedAt, retryAt);
            }
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

    private void scheduleClanChangeSync()
    {
        long now = System.currentTimeMillis();
        long scheduledAt = clanChangeCooldown.nextAllowedAt(now, clanBackoff.getRetryAtMillis());
        if (nextClanSyncAt <= 0L || scheduledAt < nextClanSyncAt)
        {
            nextClanSyncAt = scheduledAt;
        }
    }

    private String clanFingerprint(SocialClanSnapshot snapshot)
    {
        StringBuilder value = new StringBuilder()
            .append(snapshot.getClanName()).append('|')
            .append(snapshot.getPlayerRank());
        for (SocialClanSnapshot.ClanMemberSnapshot member : snapshot.getMembers())
        {
            value.append('|').append(member.getName())
                .append(':').append(member.getRank())
                .append(':').append(member.getWorld())
                .append(':').append(member.isOnline());
        }
        return value.toString();
    }

    private long randomJitter(long maximumMillis)
    {
        if (maximumMillis <= 0L)
        {
            return 0L;
        }
        return ThreadLocalRandom.current().nextLong(maximumMillis + 1L);
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
        String key = activePlayerKey == null ? "" : activePlayerKey;
        if (key.isEmpty())
        {
            return "";
        }
        String value = configManager.getConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY_PREFIX + key);
        if (value == null || value.trim().isEmpty())
        {
            String legacyName = configManager.getConfiguration(HcimProgressionCompanionConfig.GROUP, DISPLAY_NAME_KEY);
            String legacyToken = configManager.getConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY);
            if (key.equals(accountKey(legacyName)) && legacyToken != null && !legacyToken.trim().isEmpty())
            {
                value = legacyToken.trim();
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, TOKEN_KEY_PREFIX + key, value);
                configManager.setConfiguration(HcimProgressionCompanionConfig.GROUP, DISPLAY_NAME_KEY_PREFIX + key, legacyName);
            }
        }
        return value == null ? "" : value.trim();
    }

    private String currentPlayerName()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return "";
        }
        String name = client.getLocalPlayer().getName();
        return name == null ? "" : name.trim();
    }

    private String accountKey(String playerName)
    {
        if (playerName == null)
        {
            return "";
        }
        return playerName.replace('\u00a0', ' ')
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private void updateActiveAccountLink(String playerName)
    {
        String key = accountKey(playerName);
        if (key.equals(activePlayerKey))
        {
            return;
        }
        activePlayerKey = key;
        collectionLogCaptureService.reset();
        pendingGroupStorageSnapshot = null;
        groupStorageDirty = false;
        pendingPersonalBankSnapshot = null;
        personalBankDirty = false;
        lastLiveFingerprint = "";
        lastClanFingerprint = "";
        lastClanEventsFingerprint = "";
        liveBackoff.reset();
        clanBackoff.reset();
        if (panel == null)
        {
            return;
        }
        String token = deviceToken();
        HcimProgressionCompanionPanel currentPanel = panel;
        if (token.isEmpty())
        {
            SwingUtilities.invokeLater(() -> {
                if (panel == currentPanel) currentPanel.showUnlinked(playerName);
            });
            return;
        }
        String linkedName = configManager.getConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            DISPLAY_NAME_KEY_PREFIX + key
        );
        String shownName = linkedName == null || linkedName.trim().isEmpty() ? playerName : linkedName;
        SwingUtilities.invokeLater(() -> {
            if (panel == currentPanel) currentPanel.showLinked(shownName);
        });
    }

    @Provides
    HcimProgressionCompanionConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(HcimProgressionCompanionConfig.class);
    }
}
