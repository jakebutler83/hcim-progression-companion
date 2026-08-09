package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
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
    public FarmRunTracker(ConfigManager configManager) { this.configManager = configManager; }

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
    private long readTimestamp(int varbit, int value) { String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, "patch." + varbit); if (stored == null) return 0; String[] parts=stored.split(":"); if(parts.length!=2)return 0; try{return Integer.parseInt(parts[0])==value?Long.parseLong(parts[1]):0;}catch(NumberFormatException ignored){return 0;} }
    private void writeTimestamp(int varbit,int value,long timestamp){configManager.setRSProfileConfiguration(CONFIG_GROUP,"patch."+varbit,value+":"+timestamp);}
    private static class PatchDef { final String id,location,type; final int varbit,durationMinutes; PatchDef(String id,String location,String type,int varbit,int durationMinutes){this.id=id;this.location=location;this.type=type;this.varbit=varbit;this.durationMinutes=durationMinutes;} }
    private static class State { final int value; final long changedAt; State(int value,long changedAt){this.value=value;this.changedAt=changedAt;} }
}
