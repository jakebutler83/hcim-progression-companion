package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Writes the original keys, so every existing runtime privacy check stays authoritative. */
final class SettingsPresetController
{
    static final String ACCOUNT = "accountTrackingMode";
    static final String HELPERS = "inGameHelpersMode";
    static final String ALERTS = "desktopAlertsMode";
    static final String SOCIAL = "socialSharingMode";
    private static final Map<String, List<String>> GROUPS = groups();
    private final Function<String, String> read;
    private final BiConsumer<String, String> write;
    private final Supplier<String> profile;
    private String activeProfile;
    private boolean updating;

    SettingsPresetController(Function<String, String> read, BiConsumer<String, String> write, Supplier<String> profile)
    {
        this.read = read;
        this.write = write;
        this.profile = profile;
    }

    synchronized boolean updating() { return updating; }

    synchronized void initialize()
    {
        if (updating) return;
        updating = true;
        try
        {
            activeProfile = profile.get();
            // Migration only updates summary selectors. Never enable, disable,
            // rename or delete any existing per-feature setting on upgrade.
            GROUPS.forEach((selector, keys) -> save(selector, mode(keys).name()));
        }
        finally { updating = false; }
    }

    synchronized void changed(String key, String value)
    {
        if (updating) return;
        if (!Objects.equals(activeProfile, profile.get()))
        {
            // Profile-load events look like user edits. Derive the summaries
            // from that profile's flags without applying an old On preset.
            initialize();
            return;
        }
        updating = true;
        try
        {
            List<String> keys = GROUPS.get(key);
            if (keys != null)
            {
                // Ignore stale events queued by a profile/defaults refresh.
                if (!Objects.equals(value, read.apply(key))) return;
                if (SettingsMode.ON.name().equals(value) || SettingsMode.OFF.name().equals(value))
                {
                    String enabled = Boolean.toString(SettingsMode.ON.name().equals(value));
                    keys.forEach(feature -> save(feature, enabled));
                }
                else if (!SettingsMode.CUSTOM.name().equals(value)) save(key, mode(keys).name());
            }
            else
            {
                GROUPS.forEach((selector, features) -> {
                    if (features.contains(key)) save(selector, mode(features).name());
                });
            }
        }
        finally { updating = false; }
    }

    private SettingsMode mode(List<String> keys)
    {
        long enabled = keys.stream().filter(key -> Boolean.parseBoolean(read.apply(key))).count();
        return enabled == 0 ? SettingsMode.OFF : enabled == keys.size() ? SettingsMode.ON : SettingsMode.CUSTOM;
    }

    private void save(String key, String value)
    {
        if (!Objects.equals(value, read.apply(key))) write.accept(key, value);
    }

    private static Map<String, List<String>> groups()
    {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put(ACCOUNT, Arrays.asList("automaticAccountSyncEnabled", "weeklyLootTrackingEnabled",
            "personalBestSyncEnabled", "pinnedGoalEnabled", "routineRoutesSyncEnabled", "skillGoalSyncEnabled", "tcgCollectionSyncEnabled"));
        result.put(HELPERS, Arrays.asList("progressionPopupsEnabled", "routineRemindersEnabled", "routineRoutesEnabled",
            "livePackingChecksEnabled", "sessionSkillGoalsEnabled", "sessionSummaryEnabled"));
        result.put(ALERTS, Arrays.asList("progressionDesktopNotifications", "routineDesktopNotifications", "skillGoalDesktopNotifications"));
        result.put(SOCIAL, Arrays.asList("socialPresenceEnabled", "socialClanSyncEnabled", "clanSkillWeekSyncEnabled", "socialClanEventsSyncEnabled"));
        return Collections.unmodifiableMap(result);
    }
}
