package com.hcimprogression.companion;

import java.util.Collection;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/**
 * Keeps monotonic, RuneScape-profile-scoped NPC loot totals. The website stores
 * only the difference between account syncs, so no per-drop network request is
 * required and normal gameplay never waits on Progression Path.
 */
public class WeeklyLootTrackerService
{
    private static final String VALUE_KEY = "weeklyLootValueTotal";
    private static final String DROP_COUNT_KEY = "weeklyLootDropCountTotal";
    private static final String TRACKED_SINCE_KEY = "weeklyLootTrackedSince";

    public void recordNpcLoot(
        Collection<ItemStack> items,
        ItemManager itemManager,
        ConfigManager configManager)
    {
        if (items == null || items.isEmpty())
        {
            return;
        }

        long eventValue = 0L;
        for (ItemStack item : items)
        {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            int canonicalId = itemManager.canonicalize(item.getId());
            int price = Math.max(0, itemManager.getItemPrice(canonicalId));
            eventValue = safeAdd(eventValue, safeMultiply(price, item.getQuantity()));
        }

        long now = System.currentTimeMillis();
        long trackedSince = readLong(configManager, TRACKED_SINCE_KEY);
        if (trackedSince <= 0L)
        {
            configManager.setRSProfileConfiguration(
                HcimProgressionCompanionConfig.GROUP,
                TRACKED_SINCE_KEY,
                now
            );
        }
        configManager.setRSProfileConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            VALUE_KEY,
            safeAdd(readLong(configManager, VALUE_KEY), eventValue)
        );
        configManager.setRSProfileConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            DROP_COUNT_KEY,
            safeAdd(readLong(configManager, DROP_COUNT_KEY), 1L)
        );
    }

    public void applyTo(AccountSnapshot snapshot, ConfigManager configManager, boolean enabled)
    {
        snapshot.setLootTrackingEnabled(enabled);
        if (!enabled)
        {
            return;
        }
        snapshot.setLootValueTotal(readLong(configManager, VALUE_KEY));
        snapshot.setLootDropCountTotal(readLong(configManager, DROP_COUNT_KEY));
        snapshot.setLootTrackedSince(readLong(configManager, TRACKED_SINCE_KEY));
    }

    private long readLong(ConfigManager configManager, String key)
    {
        Long value = configManager.getRSProfileConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            key,
            Long.class
        );
        return value == null || value < 0L ? 0L : value;
    }

    private long safeMultiply(long left, long right)
    {
        if (left <= 0L || right <= 0L)
        {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right)
        {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private long safeAdd(long left, long right)
    {
        if (right > 0L && left > Long.MAX_VALUE - right)
        {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }
}
