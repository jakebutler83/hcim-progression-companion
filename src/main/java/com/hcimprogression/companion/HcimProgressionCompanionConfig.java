package com.hcimprogression.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HcimProgressionCompanionConfig.GROUP)
public interface HcimProgressionCompanionConfig extends Config
{
    String GROUP = "hcimprogressioncompanion";

    @ConfigItem(
        keyName = "locationSharingEnabled",
        name = "Share location",
        description = "Securely shares your location with your HCIM Progression group.",
        position = 0
    )
    default boolean locationSharingEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "Website API URL",
        description = "Your HCIM Progression website URL followed by /.netlify/functions",
        position = 1
    )
    default String apiBaseUrl()
    {
        return "https://YOUR-SITE.netlify.app/.netlify/functions";
    }
}
