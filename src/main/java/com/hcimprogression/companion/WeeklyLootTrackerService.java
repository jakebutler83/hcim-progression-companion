package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/**
 * Keeps monotonic, RuneScape-profile-scoped NPC and clue loot totals plus a bounded
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
    private static final Map<String, List<String>> SLAYER_SOURCE_ALIASES = createSlayerSourceAliases();

    public void recordNpcLoot(
        String source,
        int npcId,
        String slayerTask,
        Collection<ItemStack> items,
        ItemManager itemManager,
        ConfigManager configManager,
        Gson gson)
    {
        String verifiedTask = sourceMatchesSlayerTask(source, slayerTask) ? slayerTask : "";
        recordLoot(source, npcId, verifiedTask, items, itemManager, configManager, gson);
    }

    public void recordEventLoot(
        String source,
        Collection<ItemStack> items,
        ItemManager itemManager,
        ConfigManager configManager,
        Gson gson)
    {
        recordLoot(source, 0, "", items, itemManager, configManager, gson);
    }

    private void recordLoot(
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

    static boolean sourceMatchesSlayerTask(String source, String slayerTask)
    {
        String actual = normalizeSlayerName(source);
        String target = normalizeSlayerName(slayerTask);
        if (actual.isEmpty() || target.isEmpty() || target.startsWith("no active task"))
        {
            return false;
        }
        if (actual.equals(target) || (target.endsWith("s") && actual.equals(target.substring(0, target.length() - 1))))
        {
            return true;
        }
        return SLAYER_SOURCE_ALIASES.getOrDefault(target, Collections.emptyList()).contains(actual);
    }

    private static String normalizeSlayerName(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("\\b(task|assignment)\\b", " ")
            .replaceAll("['’]", "")
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }

    private static Map<String, List<String>> createSlayerSourceAliases()
    {
        Map<String, List<String>> aliases = new HashMap<>();
        aliases.put("abyssal demons", Arrays.asList("abyssal demon", "abyssal sire"));
        aliases.put("aviansies", Arrays.asList("aviansie", "k ril tsutsaroth", "flight kilisa", "flockleader geerin", "wingman skree"));
        aliases.put("black demons", Arrays.asList("black demon", "demonic gorilla", "tortured gorilla", "skotizo"));
        aliases.put("black dragons", Arrays.asList("black dragon", "baby black dragon", "brutal black dragon", "king black dragon"));
        aliases.put("blue dragons", Arrays.asList("blue dragon", "baby blue dragon", "brutal blue dragon"));
        aliases.put("dogs", Arrays.asList("guard dog", "wild dog"));
        aliases.put("elves", Arrays.asList("elf", "iorwerth warrior", "iorwerth archer"));
        aliases.put("fossil island wyverns", Arrays.asList("spitting wyvern", "taloned wyvern", "long tailed wyvern", "ancient wyvern"));
        aliases.put("gargoyles", Arrays.asList("gargoyle", "dusk", "dawn"));
        aliases.put("greater demons", Arrays.asList("greater demon", "k ril tsutsaroth"));
        aliases.put("hellhounds", Arrays.asList("hellhound", "cerberus"));
        aliases.put("hydras", Arrays.asList("hydra", "alchemical hydra"));
        aliases.put("kraken", Arrays.asList("cave kraken", "kraken"));
        aliases.put("lizardmen", Arrays.asList("lizardman", "lizardman brute", "lizardman shaman"));
        aliases.put("red dragons", Arrays.asList("red dragon", "brutal red dragon"));
        aliases.put("shades", Arrays.asList("loar shade", "phrin shade", "riyl shade", "asyn shade", "fiyr shade", "urium shade"));
        aliases.put("smoke devils", Arrays.asList("smoke devil", "thermonuclear smoke devil"));
        aliases.put("spiritual creatures", Arrays.asList("spiritual ranger", "spiritual warrior", "spiritual mage"));
        aliases.put("tzhaar", Arrays.asList("tzhaar ket", "tzhaar xil", "tzhaar mej", "tztok jad", "tzkal zuk"));
        aliases.put("wolves", Arrays.asList("wolf", "white wolf", "dire wolf"));
        aliases.put("zombies", Arrays.asList("zombie", "undead one"));
        return aliases;
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
