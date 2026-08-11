package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/**
 * Keeps monotonic, RuneScape-profile-scoped NPC loot totals plus a bounded
 * local history. The history is uploaded only during normal account syncs, so
 * no per-drop network request is required and gameplay never waits on the site.
 */
public class WeeklyLootTrackerService
{
    private static final String VALUE_KEY = "weeklyLootValueTotal";
    private static final String DROP_COUNT_KEY = "weeklyLootDropCountTotal";
    private static final String TRACKED_SINCE_KEY = "weeklyLootTrackedSince";
    private static final String DROP_HISTORY_KEY = "weeklyLootDropHistory";
    private static final int MAX_STORED_DROPS = 250;
    private static final long DROP_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    public void recordNpcLoot(
        String source,
        int npcId,
        String slayerTask,
        Collection<ItemStack> items,
        ItemManager itemManager,
        ConfigManager configManager,
        Gson gson)
    {
        if (items == null || items.isEmpty())
        {
            return;
        }

        long eventValue = 0L;
        List<LootDropSnapshot.LootItemSnapshot> lootItems = new ArrayList<>();
        for (ItemStack item : items)
        {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            int canonicalId = itemManager.canonicalize(item.getId());
            int price = Math.max(0, itemManager.getItemPrice(canonicalId));
            long itemValue = safeMultiply(price, item.getQuantity());
            eventValue = safeAdd(eventValue, itemValue);
            String itemName = itemManager.getItemComposition(canonicalId).getName();
            if (itemName == null || itemName.trim().isEmpty())
            {
                itemName = "Item " + canonicalId;
            }
            lootItems.add(new LootDropSnapshot.LootItemSnapshot(
                canonicalId,
                itemName,
                item.getQuantity(),
                price,
                itemValue
            ));
        }
        if (lootItems.isEmpty())
        {
            return;
        }

        long now = System.currentTimeMillis();
        long nextDropCount = safeAdd(readLong(configManager, DROP_COUNT_KEY), 1L);
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
            nextDropCount
        );

        List<LootDropSnapshot> history = readDrops(configManager, gson, now);
        history.add(0, new LootDropSnapshot(
            now + "-" + nextDropCount,
            now,
            cleanLabel(source, "NPC loot"),
            Math.max(0, npcId),
            cleanSlayerTask(slayerTask),
            eventValue,
            lootItems
        ));
        if (history.size() > MAX_STORED_DROPS)
        {
            history = new ArrayList<>(history.subList(0, MAX_STORED_DROPS));
        }
        configManager.setRSProfileConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            DROP_HISTORY_KEY,
            gson.toJson(history)
        );
    }

    public void applyTo(AccountSnapshot snapshot, ConfigManager configManager, Gson gson, boolean enabled)
    {
        snapshot.setLootTrackingEnabled(enabled);
        if (!enabled)
        {
            return;
        }
        snapshot.setLootValueTotal(readLong(configManager, VALUE_KEY));
        snapshot.setLootDropCountTotal(readLong(configManager, DROP_COUNT_KEY));
        snapshot.setLootTrackedSince(readLong(configManager, TRACKED_SINCE_KEY));
        snapshot.getLootDrops().addAll(readDrops(configManager, gson, System.currentTimeMillis()));
    }

    private List<LootDropSnapshot> readDrops(ConfigManager configManager, Gson gson, long now)
    {
        String value = configManager.getRSProfileConfiguration(
            HcimProgressionCompanionConfig.GROUP,
            DROP_HISTORY_KEY,
            String.class
        );
        if (value == null || value.trim().isEmpty())
        {
            return new ArrayList<>();
        }
        try
        {
            LootDropSnapshot[] decoded = gson.fromJson(value, LootDropSnapshot[].class);
            if (decoded == null)
            {
                return new ArrayList<>();
            }
            List<LootDropSnapshot> retained = new ArrayList<>();
            long cutoff = now - DROP_RETENTION_MILLIS;
            for (LootDropSnapshot drop : Arrays.asList(decoded))
            {
                if (drop == null || drop.getDropId() == null || drop.getOccurredAt() < cutoff)
                {
                    continue;
                }
                retained.add(drop);
                if (retained.size() >= MAX_STORED_DROPS)
                {
                    break;
                }
            }
            return retained;
        }
        catch (JsonSyntaxException ignored)
        {
            return new ArrayList<>();
        }
    }

    private String cleanLabel(String value, String fallback)
    {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty())
        {
            return fallback;
        }
        return clean.length() > 80 ? clean.substring(0, 80) : clean;
    }

    private String cleanSlayerTask(String value)
    {
        String clean = cleanLabel(value, "");
        return clean.toLowerCase().startsWith("no active task") ? "" : clean;
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
