package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;

/**
 * Reads farming state from RuneLite's supported profile configuration and
 * farming transmit varbits. The plugin deliberately avoids accessing private
 * Time Tracking implementation classes.
 */
public class FarmRunTracker
{
    private static final String CONFIG_GROUP = "hcimprogression.farmruns";

    private static final PatchDef[] PATCHES = {
        new PatchDef("herb-catherby", "Catherby", "Herb", VarbitID.FARMING_TRANSMIT_A, 80),
        new PatchDef("herb-falador", "Falador", "Herb", VarbitID.FARMING_TRANSMIT_B, 80),
        new PatchDef("herb-ardougne", "Ardougne", "Herb", VarbitID.FARMING_TRANSMIT_C, 80),
        new PatchDef("herb-hosidius", "Hosidius", "Herb", VarbitID.FARMING_TRANSMIT_D, 80),
        new PatchDef("herb-farming-guild", "Farming Guild", "Herb", VarbitID.FARMING_TRANSMIT_E, 80),
        new PatchDef("allotment-falador", "Falador", "Allotment", VarbitID.FARMING_TRANSMIT_F, 40),
        new PatchDef("allotment-catherby", "Catherby", "Allotment", VarbitID.FARMING_TRANSMIT_G, 40),
        new PatchDef("allotment-hosidius", "Hosidius", "Allotment", VarbitID.FARMING_TRANSMIT_H, 40),
        new PatchDef("tree-lumbridge", "Lumbridge", "Tree", VarbitID.FARMING_TRANSMIT_I, 200),
        new PatchDef("tree-varrock", "Varrock", "Tree", VarbitID.FARMING_TRANSMIT_J, 200),
        new PatchDef("tree-farming-guild", "Farming Guild", "Tree", VarbitID.FARMING_TRANSMIT_K, 200),
        new PatchDef("fruit-tree-catherby", "Catherby", "Fruit tree", VarbitID.FARMING_TRANSMIT_L, 960),
        new PatchDef("fruit-tree-lletya", "Lletya", "Fruit tree", VarbitID.FARMING_TRANSMIT_M, 960),
        new PatchDef("fruit-tree-farming-guild", "Farming Guild", "Fruit tree", VarbitID.FARMING_TRANSMIT_N, 960),
        new PatchDef("flower-hosidius", "Hosidius", "Flower", VarbitID.FARMING_TRANSMIT_O, 40),
        new PatchDef("special-farming-guild", "Farming Guild", "Special", VarbitID.FARMING_TRANSMIT_P, 80)
    };

    /**
     * RuneLite Time Tracking stores patch records as region.varbit profile
     * keys. Keeping the public identifiers here replaces the old reflective
     * walk through RuneLite's package-private FarmingWorld model.
     */
    private static final ProfilePatchDef[] PROFILE_PATCHES = {
        new ProfilePatchDef("Ardougne", "North", "Allotment", 10548, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Ardougne", "South", "Allotment", 10548, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Ardougne", "", "Flower", 10548, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Ardougne", "", "Herb", 10548, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Ardougne", "", "Compost", 10548, VarbitID.FARMING_TRANSMIT_E),

        new ProfilePatchDef("Catherby", "North", "Allotment", 11062, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Catherby", "South", "Allotment", 11062, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Catherby", "", "Flower", 11062, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Catherby", "", "Herb", 11062, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Catherby", "", "Compost", 11062, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Catherby", "", "Fruit tree", 11317, VarbitID.FARMING_TRANSMIT_A),

        new ProfilePatchDef("Civitas illa Fortis", "North", "Allotment", 6192, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Civitas illa Fortis", "South", "Allotment", 6192, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Civitas illa Fortis", "", "Flower", 6192, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Civitas illa Fortis", "", "Herb", 6192, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Civitas illa Fortis", "", "Compost", 6192, VarbitID.FARMING_TRANSMIT_E),

        new ProfilePatchDef("Falador", "North west", "Allotment", 12083, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Falador", "South east", "Allotment", 12083, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Falador", "", "Flower", 12083, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Falador", "", "Herb", 12083, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Falador", "", "Compost", 12083, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Falador", "", "Tree", 11828, VarbitID.FARMING_TRANSMIT_A),

        new ProfilePatchDef("Kourend", "North east", "Allotment", 6967, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Kourend", "South west", "Allotment", 6967, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Kourend", "", "Flower", 6967, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Kourend", "", "Herb", 6967, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Kourend", "", "Compost", 6967, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Kourend", "", "Spirit tree", 6967, VarbitID.FARMING_TRANSMIT_F),

        new ProfilePatchDef("Morytania", "North west", "Allotment", 14391, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Morytania", "South east", "Allotment", 14391, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Morytania", "", "Flower", 14391, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Morytania", "", "Herb", 14391, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Morytania", "", "Compost", 14391, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Morytania", "Mushroom", "Special", 13622, VarbitID.FARMING_TRANSMIT_A),

        new ProfilePatchDef("Harmony", "", "Allotment", 15148, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Harmony", "", "Herb", 15148, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Troll Stronghold", "", "Herb", 11321, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Weiss", "", "Herb", 11325, VarbitID.FARMING_TRANSMIT_A),

        new ProfilePatchDef("Farming Guild", "", "Tree", 4922, VarbitID.FARMING_TRANSMIT_G),
        new ProfilePatchDef("Farming Guild", "", "Herb", 4922, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Farming Guild", "", "Bush", 4922, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Farming Guild", "", "Flower", 4922, VarbitID.FARMING_TRANSMIT_H),
        new ProfilePatchDef("Farming Guild", "North", "Allotment", 4922, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Farming Guild", "South", "Allotment", 4922, VarbitID.FARMING_TRANSMIT_D),
        new ProfilePatchDef("Farming Guild", "", "Compost", 4922, VarbitID.FARMING_TRANSMIT_N),
        new ProfilePatchDef("Farming Guild", "", "Cactus", 4922, VarbitID.FARMING_TRANSMIT_F),
        new ProfilePatchDef("Farming Guild", "", "Spirit tree", 4922, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Farming Guild", "", "Fruit tree", 4922, VarbitID.FARMING_TRANSMIT_K),
        new ProfilePatchDef("Farming Guild", "Anima", "Special", 4922, VarbitID.FARMING_TRANSMIT_M),
        new ProfilePatchDef("Farming Guild", "Celastrus", "Special", 4922, VarbitID.FARMING_TRANSMIT_L),
        new ProfilePatchDef("Farming Guild", "Redwood", "Special", 4922, VarbitID.FARMING_TRANSMIT_I),
        new ProfilePatchDef("Farming Guild", "Hespori", "Special", 5021, VarbitID.FARMING_TRANSMIT_J),

        new ProfilePatchDef("Fossil Island", "East", "Hardwood tree", 14651, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Fossil Island", "Middle", "Hardwood tree", 14651, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Fossil Island", "West", "Hardwood tree", 14651, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Seaweed", "North", "Seaweed", 15008, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Seaweed", "South", "Seaweed", 15008, VarbitID.FARMING_TRANSMIT_B),

        new ProfilePatchDef("Lumbridge", "", "Tree", 12594, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Varrock", "", "Tree", 12854, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Taverley", "", "Tree", 11573, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Gnome Stronghold", "", "Tree", 9781, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Gnome Stronghold", "", "Fruit tree", 9781, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Tree Gnome Village", "", "Fruit tree", 9777, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Lletya", "", "Fruit tree", 9265, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Brimhaven", "", "Fruit tree", 11058, VarbitID.FARMING_TRANSMIT_A),

        new ProfilePatchDef("Prifddinas", "North", "Allotment", 13151, VarbitID.FARMING_TRANSMIT_A),
        new ProfilePatchDef("Prifddinas", "South", "Allotment", 13151, VarbitID.FARMING_TRANSMIT_B),
        new ProfilePatchDef("Prifddinas", "", "Flower", 13151, VarbitID.FARMING_TRANSMIT_C),
        new ProfilePatchDef("Prifddinas", "Crystal tree", "Special", 13151, VarbitID.FARMING_TRANSMIT_E),
        new ProfilePatchDef("Prifddinas", "", "Compost", 13151, VarbitID.FARMING_TRANSMIT_D)
    };

    private final ConfigManager configManager;
    private final Map<Integer, State> states = new LinkedHashMap<>();

    public FarmRunTracker(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public boolean update(Client client)
    {
        boolean changed = false;
        long now = System.currentTimeMillis() / 1000L;
        for (PatchDef patch : PATCHES)
        {
            int value = client.getVarbitValue(patch.varbit);
            State previous = states.get(patch.varbit);
            if (previous == null)
            {
                long stored = readTimestamp(patch.varbit, value);
                long changedAt = stored > 0 ? stored : now;
                states.put(patch.varbit, new State(value, changedAt));
                if (stored <= 0)
                {
                    writeTimestamp(patch.varbit, value, changedAt);
                    changed = true;
                }
            }
            else if (previous.value != value)
            {
                states.put(patch.varbit, new State(value, now));
                writeTimestamp(patch.varbit, value, now);
                changed = true;
            }
        }
        return changed;
    }

    public FarmRunSnapshot snapshot()
    {
        FarmRunSnapshot tracked = timeTrackingSnapshot();
        if (!tracked.getPatches().isEmpty())
        {
            return tracked;
        }

        FarmRunSnapshot result = new FarmRunSnapshot();
        long now = System.currentTimeMillis() / 1000L;
        for (PatchDef patch : PATCHES)
        {
            State state = states.get(patch.varbit);
            if (state == null)
            {
                continue;
            }
            long readyAt = state.value > 0 ? state.changedAt + patch.durationMinutes * 60L : 0;
            String status = state.value <= 0 ? "empty" : readyAt <= now ? "ready" : "growing";
            result.getPatches().add(new FarmRunSnapshot.Patch(
                patch.id,
                patch.location,
                patch.type,
                status,
                state.value,
                state.changedAt,
                readyAt));
        }
        return result;
    }

    private FarmRunSnapshot timeTrackingSnapshot()
    {
        FarmRunSnapshot result = new FarmRunSnapshot();
        long now = System.currentTimeMillis() / 1000L;
        for (ProfilePatchDef patch : PROFILE_PATCHES)
        {
            String key = patch.regionId + "." + patch.varbit;
            String stored = configManager.getRSProfileConfiguration("timetracking", key);
            if (stored == null)
            {
                continue;
            }

            String[] parts = stored.split(":");
            if (parts.length != 2)
            {
                continue;
            }

            try
            {
                int rawState = Integer.parseInt(parts[0]);
                long changedAt = Long.parseLong(parts[1]);
                boolean waiting = isWaitingForPlanting(patch.type, rawState);
                boolean harvestable = isHarvestable(patch.type, rawState);
                long readyAt = waiting || harvestable
                    ? 0
                    : changedAt + durationMinutes(patch.type) * 60L;
                String state = waiting
                    ? "waiting"
                    : harvestable || readyAt <= now ? "ready" : "growing";
                String suffix = patch.name.isEmpty() ? "" : " - " + patch.name;
                String id = "farm-" + slug(patch.location) + "-" + patch.regionId + "-" + patch.varbit;
                result.getPatches().add(new FarmRunSnapshot.Patch(
                    id,
                    patch.location + suffix,
                    patch.type,
                    state,
                    rawState,
                    changedAt,
                    readyAt));
            }
            catch (NumberFormatException ignored)
            {
                // Time Tracking is optional; ignore malformed profile records.
            }
        }
        return result;
    }

    private static boolean isHarvestable(String type, int rawState)
    {
        // RuneLite's herb states use three harvestable values after every four
        // growing values (8-10, 15-17, 22-24, ...).
        if ("Herb".equalsIgnoreCase(type) && rawState >= 8)
        {
            return (rawState - 8) % 7 <= 2;
        }
        return false;
    }

    private static boolean isWaitingForPlanting(String type, int rawState)
    {
        // Herb weed stages are 0-3. Other supported patch types use zero for
        // an empty patch in their Time Tracking profile record.
        if ("Herb".equalsIgnoreCase(type))
        {
            return rawState >= 0 && rawState <= 3;
        }
        return rawState == 0;
    }

    private static int durationMinutes(String type)
    {
        String normalized = type.toLowerCase();
        if (normalized.contains("fruit"))
        {
            return 960;
        }
        if (normalized.contains("tree"))
        {
            return 200;
        }
        if (normalized.contains("allotment") || normalized.contains("flower"))
        {
            return 40;
        }
        return 80;
    }

    private static String slug(String value)
    {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private long readTimestamp(int varbit, int value)
    {
        String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, "patch." + varbit);
        if (stored == null)
        {
            return 0;
        }
        String[] parts = stored.split(":");
        if (parts.length != 2)
        {
            return 0;
        }
        try
        {
            return Integer.parseInt(parts[0]) == value ? Long.parseLong(parts[1]) : 0;
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    private void writeTimestamp(int varbit, int value, long timestamp)
    {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, "patch." + varbit, value + ":" + timestamp);
    }

    private static class PatchDef
    {
        private final String id;
        private final String location;
        private final String type;
        private final int varbit;
        private final int durationMinutes;

        private PatchDef(String id, String location, String type, int varbit, int durationMinutes)
        {
            this.id = id;
            this.location = location;
            this.type = type;
            this.varbit = varbit;
            this.durationMinutes = durationMinutes;
        }
    }

    private static class ProfilePatchDef
    {
        private final String location;
        private final String name;
        private final String type;
        private final int regionId;
        private final int varbit;

        private ProfilePatchDef(String location, String name, String type, int regionId, int varbit)
        {
            this.location = location;
            this.name = name;
            this.type = type;
            this.regionId = regionId;
            this.varbit = varbit;
        }
    }

    private static class State
    {
        private final int value;
        private final long changedAt;

        private State(int value, long changedAt)
        {
            this.value = value;
            this.changedAt = changedAt;
        }
    }
}
