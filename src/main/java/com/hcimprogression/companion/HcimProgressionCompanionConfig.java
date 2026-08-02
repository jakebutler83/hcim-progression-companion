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
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean socialPresenceEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "socialClanSyncEnabled",
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
        keyName = "socialClanEventsSyncEnabled",
        name = "Sync clan events",
        description = "Imports the in-game Clan Settings Events list into the Progression Path clan calendar whenever you open that interface.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 2
    )
    default boolean socialClanEventsSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "locationSharingEnabled",
        name = "Share exact group location",
        description = "Securely shares your exact location with your HCIM Progression group and allows exact Social Hub pins when enabled on the website.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
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
        keyName = "personalBankSyncEnabled",
        name = "Sync Personal Bank",
        description = "Uploads your own bank to your private Progression Path journal whenever you open or change the normal bank interface.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 5
    )
    default boolean personalBankSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "weeklyLootTrackingEnabled",
        name = "Track NPC loot value",
        description = "Keeps a local running GE-value total for NPC loot. The total is sent to your private Progression Path group only when you press Sync Account.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 6
    )
    default boolean weeklyLootTrackingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "tcgCollectionSyncEnabled",
        name = "Sync OSRS TCG collection",
        description = "Reads your local OSRS TCG save and shares card names, quantities, and collection progress with your private Progression Path group.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 7
    )
    default boolean tcgCollectionSyncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "Website API URL",
        description = "Progression Path API root. Keep the default unless your website administrator provides a dedicated Cloud Run URL.",
        position = 8
    )
    default String apiBaseUrl()
    {
        return "https://hcim-companion-api-973141269474.us-central1.run.app";
    }
}
