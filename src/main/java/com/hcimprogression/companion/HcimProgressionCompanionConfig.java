package com.hcimprogression.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HcimProgressionCompanionConfig.GROUP)
public interface HcimProgressionCompanionConfig extends Config
{
    String GROUP = "hcimprogressioncompanion";

    @ConfigItem(
        keyName = "socialPresenceEnabled",
        name = "Share Social Hub presence",
        description = "Shares online status, world, broad region, activity, combat level, and worn equipment with approved Progression Path friends. Website privacy settings still control each field.",
        position = 0
    )
    default boolean socialPresenceEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "socialClanSyncEnabled",
        name = "Sync clan roster",
        description = "Shares your own RuneScape clan roster, ranks, online members, and worlds with your private Social Hub clan page.",
        position = 1
    )
    default boolean socialClanSyncEnabled()
    {
        return true;
    }


    @ConfigItem(
        keyName = "socialClanEventsSyncEnabled",
        name = "Sync clan events",
        description = "Imports the in-game Clan Settings Events list into the Progression Path clan calendar whenever you open that interface.",
        position = 2
    )
    default boolean socialClanEventsSyncEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "locationSharingEnabled",
        name = "Share exact group location",
        description = "Securely shares your exact location with your HCIM Progression group and allows exact Social Hub pins when enabled on the website.",
        position = 3
    )
    default boolean locationSharingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "groupStorageSyncEnabled",
        name = "Sync Group Storage",
        description = "Uploads the contents of your Group Ironman shared storage to your private Progression Path group whenever you open or change it.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 4
    )
    default boolean groupStorageSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "weeklyLootTrackingEnabled",
        name = "Track NPC loot value",
        description = "Keeps a local running GE-value total for NPC loot. The total is sent to your private Progression Path group only when you press Sync Account.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 5
    )
    default boolean weeklyLootTrackingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "Website API URL",
        description = "Your HCIM Progression website URL followed by /.netlify/functions",
        position = 6
    )
    default String apiBaseUrl()
    {
        return "https://progressionpath.netlify.app/.netlify/functions";
    }
}
