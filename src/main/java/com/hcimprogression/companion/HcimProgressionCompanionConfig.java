package com.hcimprogression.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hcimprogressioncompanion")
public interface HcimProgressionCompanionConfig extends Config
{
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
            keyName = "connectionKey",
            name = "Connection key",
            description = "Paste the private RuneLite connection key shown on the HCIM Progression website.",
            secret = true,
            position = 1
    )
    default String connectionKey()
    {
        return "";
    }
}