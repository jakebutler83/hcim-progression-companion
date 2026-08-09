package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;

/**
 * Reads the farming transmit varbits used by RuneLite's farming/time-tracking
 * plugins. The raw state is retained so the web client can continue to improve
 * labels without requiring a plugin update.
 */
public class FarmRunTracker
{
    private static final Logger LOG = Logger.getLogger(FarmRunTracker.class.getName());
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
    private final ConfigManager configManager;
    private final Map<Integer, State> states = new LinkedHashMap<>();
    private final Object farmingWorld;
    public FarmRunTracker(ConfigManager configManager)
    {
        this.configManager = configManager;
        this.farmingWorld = createFarmingWorld();
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
        FarmRunSnapshot known = knownCatherbySnapshot();
        if (!known.getPatches().isEmpty())
        {
            for (FarmRunSnapshot.Patch patch : known.getPatches())
            {
                boolean present = tracked.getPatches().stream().anyMatch(existing -> existing.getId().equals(patch.getId()));
                if (!present) tracked.getPatches().add(patch);
            }
        }
        if (!tracked.getPatches().isEmpty())
        {
            return tracked;
        }
        FarmRunSnapshot result = new FarmRunSnapshot();
        long now = System.currentTimeMillis() / 1000L;
        for (PatchDef patch : PATCHES)
        {
            State state = states.get(patch.varbit);
            if (state == null) continue;
            long readyAt = state.value > 0 ? state.changedAt + patch.durationMinutes * 60L : 0;
            String status = state.value <= 0 ? "empty" : readyAt <= now ? "ready" : "growing";
            result.getPatches().add(new FarmRunSnapshot.Patch(patch.id, patch.location, patch.type, status, state.value, state.changedAt, readyAt));
        }
        return result;
    }

    private FarmRunSnapshot knownCatherbySnapshot()
    {
        FarmRunSnapshot result = new FarmRunSnapshot();
        long now = System.currentTimeMillis() / 1000L;
        String[] names = {"Allotment north", "Allotment south", "Flower", "Herb", "Compost"};
        String[] types = {"Allotment", "Allotment", "Flower", "Herb", "Compost"};
        int[] varbits = {4771, 4772, 4773, 4774, 4775};
        for (int i = 0; i < varbits.length; i++)
        {
            String stored = configManager.getRSProfileConfiguration("timetracking", "11062." + varbits[i]);
            if (stored == null) continue;
            String[] parts = stored.split(":");
            if (parts.length != 2) continue;
            try
            {
                int raw = Integer.parseInt(parts[0]);
                long changedAt = Long.parseLong(parts[1]);
                long readyAt = raw <= 0 ? 0 : changedAt + durationMinutes(types[i]) * 60L;
                String state = raw <= 0 ? "empty" : readyAt <= now ? "ready" : "growing";
                result.getPatches().add(new FarmRunSnapshot.Patch("farm-catherby-" + varbits[i], "Catherby · " + names[i], types[i], state, raw, changedAt, readyAt));
            }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }
    private FarmRunSnapshot timeTrackingSnapshot()
    {
        FarmRunSnapshot result = new FarmRunSnapshot();
        if (farmingWorld == null)
        {
            return result;
        }
        try
        {
            Method getTabs = accessible(farmingWorld.getClass(), "getTabs");
            Object tabsValue = getTabs.invoke(farmingWorld);
            if (!(tabsValue instanceof Map)) return result;
            long now = System.currentTimeMillis() / 1000L;
            for (Object entryObject : ((Map<?, ?>) tabsValue).entrySet())
            {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                String type = titleCase(String.valueOf(entry.getKey()));
                Object patches = entry.getValue();
                if (!(patches instanceof Iterable)) continue;
                for (Object patch : (Iterable<?>) patches)
                {
                    Method getRegion = accessible(patch.getClass(), "getRegion");
                    Method getName = accessible(patch.getClass(), "getName");
                    Method getVarbit = accessible(patch.getClass(), "getVarbit");
                    Object region = getRegion.invoke(patch);
                    if (region == null) continue;
                    Method getRegionName = accessible(region.getClass(), "getName");
                    Method getRegionId = accessible(region.getClass(), "getRegionID");
                    String location = String.valueOf(getRegionName.invoke(region));
                    int regionId = ((Number) getRegionId.invoke(region)).intValue();
                    int varbit = ((Number) getVarbit.invoke(patch)).intValue();
                    String stored = configManager.getRSProfileConfiguration("timetracking", regionId + "." + varbit);
                    if (stored == null) continue;
                    String[] parts = stored.split(":");
                    if (parts.length != 2) continue;
                    int rawState;
                    long changedAt;
                    try
                    {
                        rawState = Integer.parseInt(parts[0]);
                        changedAt = Long.parseLong(parts[1]);
                    }
                    catch (NumberFormatException ignored)
                    {
                        continue;
                    }
                    long readyAt = rawState <= 0 ? 0 : changedAt + durationMinutes(type) * 60L;
                    String state = rawState <= 0 ? "empty" : readyAt <= now ? "ready" : "growing";
                    String name = String.valueOf(getName.invoke(patch));
                    String id = "farm-" + slug(location) + "-" + varbit;
                    result.getPatches().add(new FarmRunSnapshot.Patch(id, location + " · " + name, type, state, rawState, changedAt, readyAt));
                }
            }
        }
        catch (ReflectiveOperationException | RuntimeException error)
        {
            // Time Tracking is optional. The raw transmit fallback remains available.
            LOG.log(Level.FINE, "Unable to read RuneLite Time Tracking farming records", error);
        }
        return result;
    }
    private static Object createFarmingWorld()
    {
        try
        {
            Class<?> type = Class.forName("net.runelite.client.plugins.timetracking.farming.FarmingWorld");
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
        catch (ReflectiveOperationException | RuntimeException ignored)
        {
            return null;
        }
    }
    private static Method accessible(Class<?> type, String name) throws NoSuchMethodException
    {
        Method method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }
    private static int durationMinutes(String type)
    {
        String normalized = type.toLowerCase();
        if (normalized.contains("fruit")) return 960;
        if (normalized.contains("tree")) return 200;
        if (normalized.contains("allotment") || normalized.contains("flower")) return 40;
        return 80;
    }
    private static String titleCase(String value)
    {
        String lower = value.toLowerCase().replace('_', ' ');
        return lower.isEmpty() ? "Other" : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
    private static String slug(String value)
    {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
    private long readTimestamp(int varbit, int value) { String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, "patch." + varbit); if (stored == null) return 0; String[] parts=stored.split(":"); if(parts.length!=2)return 0; try{return Integer.parseInt(parts[0])==value?Long.parseLong(parts[1]):0;}catch(NumberFormatException ignored){return 0;} }
    private void writeTimestamp(int varbit,int value,long timestamp){configManager.setRSProfileConfiguration(CONFIG_GROUP,"patch."+varbit,value+":"+timestamp);}
    private static class PatchDef { final String id,location,type; final int varbit,durationMinutes; PatchDef(String id,String location,String type,int varbit,int durationMinutes){this.id=id;this.location=location;this.type=type;this.varbit=varbit;this.durationMinutes=durationMinutes;} }
    private static class State { final int value; final long changedAt; State(int value,long changedAt){this.value=value;this.changedAt=changedAt;} }
}
