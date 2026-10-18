package com.hcimprogression.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(HcimProgressionCompanionConfig.GROUP)
public interface HcimProgressionCompanionConfig extends Config
{
    String GROUP = "hcimprogressioncompanion";

    @ConfigSection(name = "Optional sharing", description = "Separate opt-ins for social/clan sharing, bank contents, shared storage and exact location.", position = 10, closedByDefault = true)
    String sharingSection = "sharingSection";

    @ConfigSection(name = "Advanced: sync", description = "Individual saved sync choices. Editing one updates its preset to Custom when needed. Upgrades never change these choices.", position = 20, closedByDefault = true)
    String syncSection = "syncSection";

    @ConfigSection(name = "Advanced: helpers", description = "Individual helpers, alert types and display preferences. Custom choices and muted categories are preserved.", position = 30, closedByDefault = true)
    String helpersSection = "helpersSection";

    @ConfigSection(name = "Connection", description = "Keep the default API address unless your administrator provides another one. Link codes go in the companion sidebar.", position = 40, closedByDefault = true)
    String connectionSection = "connectionSection";

    @ConfigItem(keyName = "accountTrackingMode", name = "Account tracking",
        description = "On enables automatic account/Collection Log snapshots, loot history, personal bests, pinned website tasks, route setup sync, skill-goal sync and TCG collection sync. Bank contents, exact location, Group Storage and social/clan sharing remain separate. Custom keeps your saved individual choices under Advanced: sync.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 0)
    default SettingsMode accountTrackingMode() { return SettingsMode.OFF; }

    @ConfigItem(keyName = "inGameHelpersMode", name = "In-game helpers",
        description = "On enables completion pop-ups, local routine reminders, farm/birdhouse routes, packing checks, skill goals and session summaries. Completion pop-ups need account tracking; this preset does not enable uploads, website requests, desktop alerts or change muted pop-up categories. Custom keeps individual choices under Advanced: helpers.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 1)
    default SettingsMode inGameHelpersMode() { return SettingsMode.OFF; }

    @ConfigItem(keyName = "desktopAlertsMode", name = "Desktop alerts",
        description = "On forwards enabled completion, ready-routine and skill-goal alerts through RuneLite notification settings. Turn off if Time Tracking already notifies you about ready routines. This does not enable helpers or uploads. Custom keeps individual alert choices.", position = 2)
    default SettingsMode desktopAlertsMode() { return SettingsMode.OFF; }

    @ConfigItem(keyName = "socialSharingMode", name = "Social / clan",
        description = "On shares Social Hub presence (online status, world, region, activity, combat level and worn gear), your clan roster/ranks/online worlds, clan Skill of the Week XP, and clan events with Progression Path. Website privacy controls still apply. Exact location, bank and Group Storage are separate. Custom keeps existing individual sharing choices.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 0, section = sharingSection)
    default SettingsMode socialSharingMode() { return SettingsMode.OFF; }

    @ConfigItem(
        keyName = "socialPresenceEnabled",
        section = syncSection,
        name = "Share Social Hub presence",
        description = "Shares online status, world, broad region, activity, combat level, and worn equipment with approved Progression Path friends. Website privacy settings still control each field.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean socialPresenceEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "socialClanSyncEnabled",
        section = syncSection,
        name = "Sync clan roster",
        description = "Shares your own RuneScape clan roster, ranks, online members, and worlds with your private Social Hub clan page.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 1
    )
    default boolean socialClanSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "clanSkillWeekSyncEnabled",
        section = syncSection,
        name = "Share clan Skill of the Week XP",
        description = "Opt in to share your current skill XP with the RuneScape clan Skill of the Week leaderboard. Only your skill totals and RuneScape name are shared.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 2
    )
    default boolean clanSkillWeekSyncEnabled()
    {
        return false;
    }


    @ConfigItem(
        keyName = "socialClanEventsSyncEnabled",
        section = syncSection,
        name = "Sync clan events",
        description = "Imports the in-game Clan Settings Events list into the Progression Path clan calendar whenever you open that interface.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 3
    )
    default boolean socialClanEventsSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "locationSharingEnabled",
        section = sharingSection,
        name = "Share exact group location",
        description = "Securely shares your exact location with your HCIM Progression group and allows exact Social Hub pins when enabled on the website.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 4
    )
    default boolean locationSharingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "groupStorageSyncEnabled",
        section = sharingSection,
        name = "Sync Group Storage",
        description = "Uploads the contents of your Group Ironman shared storage to your private Progression Path group whenever you open or change it.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 5
    )
    default boolean groupStorageSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "personalBankSyncEnabled",
        section = sharingSection,
        name = "Sync Personal Bank",
        description = "Uploads your own bank to your private Progression Path journal whenever you open or change the normal bank interface.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 6
    )
    default boolean personalBankSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "weeklyLootTrackingEnabled",
        section = syncSection,
        name = "Track loot value",
        description = "Keeps a private 30-day NPC and clue-scroll reward history with item quantities and estimated GE value. It is sent only with normal Progression Path account syncs.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 7
    )
    default boolean weeklyLootTrackingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "tcgCollectionSyncEnabled",
        section = syncSection,
        name = "Sync OSRS TCG collection",
        description = "Requests your distinct owned card names from OSRS TCG through RuneLite's read-only PluginMessage API and shares that collection progress with your private Progression Path group.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 8
    )
    default boolean tcgCollectionSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "automaticAccountSyncEnabled",
        section = syncSection,
        name = "Automatic account snapshots",
        description = "Automatically syncs account changes and new Collection Log rewards. Enable the game's Collection Log chat message or popup to capture new items without opening log pages. Old page imports remain history only. Burst rewards are coalesced; the manual Sync Account button remains available.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 9
    )
    default boolean automaticAccountSyncEnabled()
    {
        return false;
    }

    @ConfigItem(keyName = "personalBestSyncEnabled", name = "Personal best sync", section = syncSection,
        description = "Privately syncs this character's RuneLite Chat Commands personal bests and observed quest-speedrun result times. Uses a lightweight upload independent of Hiscores. Existing bests are imported; new records normally queue within a few seconds while logged in. Does not control gameplay.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 10)
    default boolean personalBestSyncEnabled() { return false; }

    @ConfigItem(
        keyName = "progressionPopupsEnabled",
        section = helpersSection,
        name = "Progression completion pop-ups",
        description = "Shows newly completed quests, diary tiers, and supported milestones after an automatic game-event sync. Requires Automatic account snapshots. Login and manual refreshes stay silent.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 11
    )
    default boolean progressionPopupsEnabled() { return false; }

    @ConfigItem(
        keyName = "pinnedGoalEnabled",
        section = syncSection,
        name = "Pinned website goal",
        description = "Shows the task pinned on your Progression Path dashboard. Fetches this character's goal about once a minute while logged in. Does not control gameplay.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 12
    )
    default boolean pinnedGoalEnabled() { return false; }

    @ConfigItem(keyName = "routineRoutesEnabled", name = "Local route helper", section = helpersSection,
        description = "User-stepped packing lists and routes for farm and birdhouse runs. No automatic movement, clicks or timer changes.", position = 61)
    default boolean routineRoutesEnabled() { return false; }

    @ConfigItem(keyName = "routineRoutesSyncEnabled", name = "Route setup sync", section = syncSection,
        description = "Enables manual Load from website and Save to website for this character's farm and birdhouse route setups. Local route editing needs no network access.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 62)
    default boolean routineRoutesSyncEnabled() { return false; }

    @ConfigItem(keyName = "livePackingChecksEnabled", name = "Live packing checks", section = helpersSection,
        description = "Checks exact Item name x quantity entries against inventory and worn gear in Route Helper. Free-form notes need manual review. Does not read your bank, verify charges, or move items. Requires Local route helper.", position = 63)
    default boolean livePackingChecksEnabled() { return false; }

    @ConfigItem(keyName = "sessionSkillGoalsEnabled", name = "Local skill goals", section = helpersSection,
        description = "Live XP-gain or target-level goals with a progress bar and completion message. Saves separately for each character and resumes after login or restarting RuneLite. Clear removes the saved goal. No website link needed.", position = 64)
    default boolean sessionSkillGoalsEnabled() { return false; }

    @ConfigItem(keyName = "skillGoalDesktopNotifications", name = "Also notify for skill goals", section = helpersSection,
        description = "Forwards a newly completed local skill goal through RuneLite notification settings. Requires Local skill goals.", position = 65)
    default boolean skillGoalDesktopNotifications() { return false; }

    @ConfigItem(keyName = "sessionSummaryEnabled", name = "Local session summary", section = helpersSection,
        description = "Counts XP gained, real levels gained and completed local skill goals this session. No uploads or historical backfills. Resets on logout or character/profile change.", position = 66)
    default boolean sessionSummaryEnabled() { return false; }

    @net.runelite.client.config.Range(min = 3, max = 20)
    @ConfigItem(keyName = "progressionPopupSeconds", name = "Pop-up duration (seconds)", section = helpersSection,
        description = "Display new completion, routine and local skill-goal pop-ups for 3–20 seconds. Existing alerts keep their original duration.", position = 67)
    default int progressionPopupSeconds() { return 8; }

    @ConfigItem(keyName = "compactProgressionOverlay", name = "Compact progression display", section = helpersSection,
        description = "Uses a smaller progression overlay and hides extra helper instructions. Does not hide your actual goal or route step.", position = 68)
    default boolean compactProgressionOverlay() { return false; }

    @ConfigItem(keyName = "skillGoalSyncEnabled", name = "Skill goal website sync", section = syncSection,
        description = "Sends your local skill-goal targets and progress to this linked character's private Journey tasks. Starts/completions send promptly; progress is batched about every two minutes. Goals remain on the website after logout.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 69)
    default boolean skillGoalSyncEnabled() { return false; }

    @ConfigItem(
        keyName = "progressionDesktopNotifications",
        section = helpersSection,
        name = "Also notify outside the game",
        description = "Sends completion pop-ups through RuneLite's normal notification settings (sound, tray, and focus preferences). Requires Progression completion pop-ups.",
        position = 13
    )
    default boolean progressionDesktopNotifications() { return false; }

    @ConfigItem(
        keyName = "routineRemindersEnabled",
        section = helpersSection,
        name = "Local farm / birdhouse reminders",
        description = "Shows a small reminder when RuneLite Time Tracking records change from growing to ready. Local only: no website link, upload, or automatic snapshots required. Existing ready timers at login stay silent.",
        position = 14
    )
    default boolean routineRemindersEnabled() { return false; }

    @ConfigItem(
        keyName = "routineDesktopNotifications",
        section = helpersSection,
        name = "Also notify for ready routines",
        description = "Forwards local routine reminders through RuneLite's notification settings. Leave off if Time Tracking already sends these notifications to avoid duplicate sounds.",
        position = 15
    )
    default boolean routineDesktopNotifications() { return false; }

    @ConfigItem(
        keyName = "questCompletionPopups",
        section = helpersSection,
        name = "Quest completion pop-ups",
        description = "Local filter for Progression completion pop-ups. Muted quests still appear in Recent completions while the master option is enabled. Does not enable uploads.",
        position = 16
    )
    default boolean questCompletionPopups() { return true; }

    @ConfigItem(
        keyName = "diaryCompletionPopups",
        section = helpersSection,
        name = "Diary completion pop-ups",
        description = "Local filter for Progression completion pop-ups. Muted diary tiers still appear in Recent completions. Does not enable uploads.",
        position = 17
    )
    default boolean diaryCompletionPopups() { return true; }

    @ConfigItem(
        keyName = "milestoneCompletionPopups",
        section = helpersSection,
        name = "Milestone completion pop-ups",
        description = "Local filter for supported skill, clue and boss milestone pop-ups. Muted milestones still appear in Recent completions. Does not enable uploads.",
        position = 18
    )
    default boolean milestoneCompletionPopups() { return true; }

    @ConfigItem(
        keyName = "apiBaseUrl",
        section = connectionSection,
        name = "Website API URL",
        description = "Progression Path API root. Keep the default unless your website administrator provides a dedicated Cloud Run URL.",
        position = 10
    )
    default String apiBaseUrl()
    {
        return "https://hcim-companion-api-973141269474.us-central1.run.app";
    }
}
